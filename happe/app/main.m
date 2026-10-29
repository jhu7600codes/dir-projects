// Happe: paste a vless:// link, connect. plain uikit, ios 10.

#import <UIKit/UIKit.h>
#import <CFNetwork/CFNetwork.h>
#include <spawn.h>
#include <sys/wait.h>
#include <sys/stat.h>
#import "VLESSConfig.h"

#define HELPER      "/usr/libexec/happe/happectl"
#define CONFIG_DIR  @"/var/mobile/Library/Happe"
#define CONFIG_PATH @"/var/mobile/Library/Happe/config.json"

static NSString *const kManualProxyHint =
    @"Set it manually: Settings > Wi-Fi > (i) > HTTP Proxy > Manual, server 127.0.0.1, port 8080.";

// runs the setuid helper, returns its exit status and output
static int runHelper(NSString *command, NSString **output) {
    int fds[2];
    if (pipe(fds) != 0) {
        if (output) *output = @"pipe failed";
        return -1;
    }
    posix_spawn_file_actions_t fa;
    posix_spawn_file_actions_init(&fa);
    posix_spawn_file_actions_adddup2(&fa, fds[1], 1);
    posix_spawn_file_actions_adddup2(&fa, fds[1], 2);
    posix_spawn_file_actions_addclose(&fa, fds[0]);
    posix_spawn_file_actions_addclose(&fa, fds[1]);
    const char *argv[] = { HELPER, command.UTF8String, NULL };
    char *const env[] = { "PATH=/usr/bin:/bin:/usr/sbin:/sbin", NULL };
    pid_t pid;
    int rc = posix_spawn(&pid, HELPER, &fa, NULL, (char *const *)argv, env);
    posix_spawn_file_actions_destroy(&fa);
    close(fds[1]);
    NSMutableData *buf = [NSMutableData data];
    if (rc == 0) {
        char tmp[4096];
        ssize_t n;
        while ((n = read(fds[0], tmp, sizeof tmp)) > 0) [buf appendBytes:tmp length:(NSUInteger)n];
    }
    close(fds[0]);
    if (rc != 0) {
        if (output) *output = [NSString stringWithFormat:@"cannot run %s: %s (is the .deb installed?)", HELPER, strerror(rc)];
        return -1;
    }
    int st = 0;
    while (waitpid(pid, &st, 0) < 0 && errno == EINTR) {}
    NSString *s = [[NSString alloc] initWithData:buf encoding:NSUTF8StringEncoding] ?: @"";
    if (output) *output = [s stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    return WIFEXITED(st) ? WEXITSTATUS(st) : -1;
}

// what the system actually uses right now (public api, works without root)
static BOOL systemProxyIsOurs(void) {
    NSDictionary *s = CFBridgingRelease(CFNetworkCopySystemProxySettings());
    return [s[@"HTTPEnable"] intValue] == 1 && [s[@"HTTPProxy"] isEqual:@"127.0.0.1"] && [s[@"HTTPPort"] intValue] == 8080;
}

typedef NS_ENUM(NSInteger, VPState) {
    VPStateUnknown,
    VPStateDisconnected,
    VPStateWorking,
    VPStateConnected,
    VPStateError,
};

static UIColor *VPRGB(uint32_t hex) {
    return [UIColor colorWithRed:((hex >> 16) & 0xff) / 255.0 green:((hex >> 8) & 0xff) / 255.0 blue:(hex & 0xff) / 255.0 alpha:1];
}

// insets the text of the link field so it lines up with the other cards
@interface VPInsetTextField : UITextField
@end

@implementation VPInsetTextField
- (CGRect)textRectForBounds:(CGRect)bounds {
    return CGRectInset([super textRectForBounds:bounds], 14, 0);
}
- (CGRect)editingRectForBounds:(CGRect)bounds {
    return CGRectInset([super editingRectForBounds:bounds], 14, 0);
}
- (CGRect)placeholderRectForBounds:(CGRect)bounds {
    return CGRectInset([super placeholderRectForBounds:bounds], 14, 0);
}
@end

@interface VPViewController : UIViewController <UITextFieldDelegate>
@property (nonatomic, strong) UITextField *linkField;
@property (nonatomic, strong) UIButton *actionButton;
@property (nonatomic, strong) UIActivityIndicatorView *buttonSpinner;
@property (nonatomic, strong) UIView *statusDot;
@property (nonatomic, strong) UILabel *statusTitle;
@property (nonatomic, strong) UILabel *statusDetail;
@property (nonatomic, strong) UILabel *footerLabel;
@property (nonatomic, assign) VPState state;
@end

@implementation VPViewController

- (UIView *)card {
    UIView *v = [[UIView alloc] init];
    v.backgroundColor = [UIColor whiteColor];
    v.layer.cornerRadius = 12;
    return v;
}

- (UILabel *)labelWithFont:(UIFont *)font color:(UIColor *)color {
    UILabel *l = [[UILabel alloc] init];
    l.font = font;
    l.textColor = color;
    l.numberOfLines = 0;
    return l;
}

- (UIView *)spacer:(CGFloat)height {
    UIView *v = [[UIView alloc] init];
    [v.heightAnchor constraintEqualToConstant:height].active = YES;
    return v;
}

// section headers/footers sit indented like in settings, cards don't
- (UIView *)inset:(UIView *)content {
    UIView *wrap = [[UIView alloc] init];
    content.translatesAutoresizingMaskIntoConstraints = NO;
    [wrap addSubview:content];
    [NSLayoutConstraint activateConstraints:@[
        [content.leadingAnchor constraintEqualToAnchor:wrap.leadingAnchor constant:16],
        [content.trailingAnchor constraintEqualToAnchor:wrap.trailingAnchor constant:-16],
        [content.topAnchor constraintEqualToAnchor:wrap.topAnchor],
        [content.bottomAnchor constraintEqualToAnchor:wrap.bottomAnchor],
    ]];
    return wrap;
}

- (UILabel *)sectionHeader:(NSString *)text {
    UILabel *l = [self labelWithFont:[UIFont systemFontOfSize:13] color:VPRGB(0x6d6d72)];
    l.text = text.uppercaseString;
    return l;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = VPRGB(0xefeff4);

    UILabel *title = [self labelWithFont:[UIFont systemFontOfSize:34 weight:UIFontWeightBold] color:[UIColor blackColor]];
    title.text = @"Happe";

    // status card: dot + title, detail underneath
    UIView *statusCard = [self card];
    self.statusDot = [[UIView alloc] init];
    self.statusDot.layer.cornerRadius = 5;
    self.statusTitle = [self labelWithFont:[UIFont systemFontOfSize:17 weight:UIFontWeightSemibold] color:[UIColor blackColor]];
    self.statusDetail = [self labelWithFont:[UIFont systemFontOfSize:13] color:VPRGB(0x8e8e93)];
    for (UIView *v in @[ self.statusDot, self.statusTitle, self.statusDetail ]) {
        v.translatesAutoresizingMaskIntoConstraints = NO;
        [statusCard addSubview:v];
    }

    self.linkField = [[VPInsetTextField alloc] init];
    self.linkField.backgroundColor = [UIColor whiteColor];
    self.linkField.layer.cornerRadius = 12;
    self.linkField.placeholder = @"vless://…";
    self.linkField.font = [UIFont fontWithName:@"Menlo-Regular" size:14] ?: [UIFont systemFontOfSize:14];
    self.linkField.autocorrectionType = UITextAutocorrectionTypeNo;
    self.linkField.autocapitalizationType = UITextAutocapitalizationTypeNone;
    self.linkField.spellCheckingType = UITextSpellCheckingTypeNo;
    self.linkField.keyboardType = UIKeyboardTypeURL;
    self.linkField.clearButtonMode = UITextFieldViewModeWhileEditing;
    self.linkField.returnKeyType = UIReturnKeyDone;
    self.linkField.delegate = self;

    self.actionButton = [UIButton buttonWithType:UIButtonTypeCustom];
    self.actionButton.layer.cornerRadius = 12;
    self.actionButton.titleLabel.font = [UIFont systemFontOfSize:17 weight:UIFontWeightSemibold];
    [self.actionButton setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
    [self.actionButton setTitleColor:[UIColor colorWithWhite:1 alpha:0.6] forState:UIControlStateHighlighted];
    [self.actionButton addTarget:self action:@selector(toggle) forControlEvents:UIControlEventTouchUpInside];
    self.buttonSpinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhite];
    self.buttonSpinner.hidesWhenStopped = YES;
    self.buttonSpinner.translatesAutoresizingMaskIntoConstraints = NO;
    [self.actionButton addSubview:self.buttonSpinner];

    self.footerLabel = [self labelWithFont:[UIFont systemFontOfSize:13] color:VPRGB(0x6d6d72)];

    // no setCustomSpacing: that is ios 11+. fixed-height spacers instead.
    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[
        title, [self spacer:16], statusCard, [self spacer:28],
        [self inset:[self sectionHeader:@"Server link"]], [self spacer:8], self.linkField, [self spacer:16],
        self.actionButton, [self spacer:10], [self inset:self.footerLabel]
    ]];
    stack.axis = UILayoutConstraintAxisVertical;
    stack.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:stack];

    [NSLayoutConstraint activateConstraints:@[
        [stack.topAnchor constraintEqualToAnchor:self.topLayoutGuide.bottomAnchor constant:12],
        [stack.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor constant:16],
        [stack.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor constant:-16],

        [self.statusDot.widthAnchor constraintEqualToConstant:10],
        [self.statusDot.heightAnchor constraintEqualToConstant:10],
        [self.statusDot.leadingAnchor constraintEqualToAnchor:statusCard.leadingAnchor constant:16],
        [self.statusDot.centerYAnchor constraintEqualToAnchor:self.statusTitle.centerYAnchor],
        [self.statusTitle.leadingAnchor constraintEqualToAnchor:self.statusDot.trailingAnchor constant:10],
        [self.statusTitle.trailingAnchor constraintEqualToAnchor:statusCard.trailingAnchor constant:-16],
        [self.statusTitle.topAnchor constraintEqualToAnchor:statusCard.topAnchor constant:14],
        [self.statusDetail.leadingAnchor constraintEqualToAnchor:self.statusTitle.leadingAnchor],
        [self.statusDetail.trailingAnchor constraintEqualToAnchor:self.statusTitle.trailingAnchor],
        [self.statusDetail.topAnchor constraintEqualToAnchor:self.statusTitle.bottomAnchor constant:3],
        [self.statusDetail.bottomAnchor constraintEqualToAnchor:statusCard.bottomAnchor constant:-14],

        [self.linkField.heightAnchor constraintEqualToConstant:48],
        [self.actionButton.heightAnchor constraintEqualToConstant:50],
        [self.buttonSpinner.centerYAnchor constraintEqualToAnchor:self.actionButton.centerYAnchor],
        [self.buttonSpinner.trailingAnchor constraintEqualToAnchor:self.actionButton.trailingAnchor constant:-16],
    ]];

    [self.view addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self.view action:@selector(endEditing:)]];
    [self setState:VPStateUnknown title:@"Checking…" detail:@" " footer:nil];
    [self refreshStatus];
}

- (UIStatusBarStyle)preferredStatusBarStyle {
    return UIStatusBarStyleDefault;
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField resignFirstResponder];
    return YES;
}

- (void)setState:(VPState)state title:(NSString *)title detail:(NSString *)detail footer:(NSString *)footer {
    self.state = state;
    self.statusTitle.text = title;
    self.statusDetail.text = detail.length ? detail : @" ";
    self.footerLabel.text = footer;
    self.footerLabel.superview.hidden = footer.length == 0;

    UIColor *dot = VPRGB(0xc7c7cc);
    NSString *button = @"Connect";
    UIColor *buttonColor = VPRGB(0x007aff);
    BOOL busy = NO;
    switch (state) {
        case VPStateConnected:
            dot = VPRGB(0x34c759);
            button = @"Disconnect";
            buttonColor = VPRGB(0xff3b30);
            break;
        case VPStateWorking:
        case VPStateUnknown:
            dot = VPRGB(0xff9500);
            busy = YES;
            break;
        case VPStateError:
            dot = VPRGB(0xff3b30);
            break;
        case VPStateDisconnected:
            break;
    }
    self.statusDot.backgroundColor = dot;
    [UIView performWithoutAnimation:^{
        [self.actionButton setTitle:button forState:UIControlStateNormal];
        [self.actionButton layoutIfNeeded];
    }];
    self.actionButton.backgroundColor = busy ? [buttonColor colorWithAlphaComponent:0.5] : buttonColor;
    self.actionButton.enabled = !busy;
    self.linkField.enabled = state != VPStateConnected && !busy;
    self.linkField.textColor = self.linkField.enabled ? [UIColor blackColor] : VPRGB(0x8e8e93);
    if (busy) [self.buttonSpinner startAnimating];
    else [self.buttonSpinner stopAnimating];
}

- (NSString *)proxyFooter:(BOOL)proxyOK {
    return proxyOK ? @"Wi-Fi proxy is set to 127.0.0.1:8080. SOCKS5 is on 127.0.0.1:1080." : kManualProxyHint;
}

- (void)refreshStatus {
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        NSString *out = nil;
        int rc = runHelper(@"status", &out);
        BOOL proxyOK = systemProxyIsOurs();
        dispatch_async(dispatch_get_main_queue(), ^{
            if (rc == 0) {
                [self setState:VPStateConnected title:@"Connected" detail:[@"xray " stringByAppendingString:out]
                        footer:[self proxyFooter:proxyOK]];
            } else if (rc == 3) {
                [self setState:VPStateDisconnected title:@"Not connected" detail:@"Paste a link and tap Connect." footer:nil];
            } else {
                [self setState:VPStateError title:@"Helper unavailable" detail:out footer:nil];
            }
        });
    });
}

- (void)toggle {
    if (self.state == VPStateConnected) [self disconnect];
    else [self connect];
}

- (void)connect {
    [self.view endEditing:YES];
    NSString *link = self.linkField.text ?: @"";
    NSString *err = nil;
    NSData *json = [VLESSConfig configJSONFromLink:link error:&err];
    if (!json) {
        [self setState:VPStateError title:@"Invalid link" detail:err footer:nil];
        return;
    }
    NSString *summary = [VLESSConfig summaryForLink:link];

    [[NSFileManager defaultManager] createDirectoryAtPath:CONFIG_DIR withIntermediateDirectories:YES
                                               attributes:@{ NSFilePosixPermissions: @0700 } error:nil];
    NSError *werr = nil;
    if (![json writeToFile:CONFIG_PATH options:NSDataWritingAtomic error:&werr]) {
        [self setState:VPStateError title:@"Couldn't save config" detail:werr.localizedDescription footer:nil];
        return;
    }
    chmod(CONFIG_PATH.fileSystemRepresentation, 0600);

    [self setState:VPStateWorking title:@"Connecting…" detail:summary footer:nil];
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        NSString *startOut = nil, *proxyOut = nil;
        int rc = runHelper(@"start", &startOut);
        int prc = rc == 0 ? runHelper(@"proxy-on", &proxyOut) : -1;
        // give configd a moment to publish the new settings
        BOOL proxyOK = NO;
        for (int i = 0; rc == 0 && i < 10 && !(proxyOK = systemProxyIsOurs()); i++) usleep(200 * 1000);
        dispatch_async(dispatch_get_main_queue(), ^{
            if (rc != 0) {
                [self setState:VPStateError title:@"Couldn't connect" detail:startOut footer:nil];
                return;
            }
            NSString *footer = [self proxyFooter:proxyOK];
            if (!proxyOK && prc != 0 && proxyOut.length)
                footer = [NSString stringWithFormat:@"Couldn't set the Wi-Fi proxy (%@). %@", proxyOut, kManualProxyHint];
            [self setState:VPStateConnected title:@"Connected" detail:summary footer:footer];
        });
    });
}

- (void)disconnect {
    [self.view endEditing:YES];
    [self setState:VPStateWorking title:@"Disconnecting…" detail:self.statusDetail.text footer:nil];
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        NSString *proxyOut = nil, *stopOut = nil;
        int prc = runHelper(@"proxy-off", &proxyOut);
        int rc = runHelper(@"stop", &stopOut);
        BOOL stillOurs = systemProxyIsOurs();
        dispatch_async(dispatch_get_main_queue(), ^{
            NSString *footer = (stillOurs || prc != 0)
                ? @"Turn the proxy off manually: Settings > Wi-Fi > (i) > HTTP Proxy > Off." : nil;
            if (rc == 0) [self setState:VPStateDisconnected title:@"Not connected" detail:@"Paste a link and tap Connect." footer:footer];
            else [self setState:VPStateError title:@"Couldn't stop xray" detail:stopOut footer:footer];
        });
    });
}

@end

@interface VPAppDelegate : UIResponder <UIApplicationDelegate>
@property (nonatomic, strong) UIWindow *window;
@end

@implementation VPAppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    self.window.rootViewController = [[VPViewController alloc] init];
    [self.window makeKeyAndVisible];
    return YES;
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    [(VPViewController *)self.window.rootViewController refreshStatus];
}

@end

int main(int argc, char *argv[]) {
    @autoreleasepool {
        return UIApplicationMain(argc, argv, nil, NSStringFromClass([VPAppDelegate class]));
    }
}
