// vless proxy: one text field, connect, disconnect. plain uikit, ios 10.

#import <UIKit/UIKit.h>
#import <CFNetwork/CFNetwork.h>
#include <spawn.h>
#include <sys/wait.h>
#include <sys/stat.h>
#import "VLESSConfig.h"

#define HELPER      "/usr/libexec/vlessproxy/vlessctl"
#define CONFIG_DIR  @"/var/mobile/Library/VLESSProxy"
#define CONFIG_PATH @"/var/mobile/Library/VLESSProxy/config.json"

static NSString *const kManualProxyHint =
    @"set it by hand: settings > wi-fi > (i) > http proxy > manual > server 127.0.0.1, port 8080";

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

@interface VPViewController : UIViewController <UITextFieldDelegate>
@property (nonatomic, strong) UITextField *linkField;
@property (nonatomic, strong) UIButton *connectButton;
@property (nonatomic, strong) UIButton *disconnectButton;
@property (nonatomic, strong) UILabel *statusLabel;
@property (nonatomic, strong) UILabel *hintLabel;
@property (nonatomic, strong) UIActivityIndicatorView *spinner;
@end

@implementation VPViewController

- (UIButton *)buttonWithTitle:(NSString *)title action:(SEL)action {
    UIButton *b = [UIButton buttonWithType:UIButtonTypeSystem];
    [b setTitle:title forState:UIControlStateNormal];
    b.titleLabel.font = [UIFont systemFontOfSize:18 weight:UIFontWeightMedium];
    b.layer.cornerRadius = 8;
    b.layer.borderWidth = 1;
    b.layer.borderColor = b.tintColor.CGColor;
    [b addTarget:self action:action forControlEvents:UIControlEventTouchUpInside];
    return b;
}

- (UILabel *)multilineLabel:(CGFloat)size {
    UILabel *l = [[UILabel alloc] init];
    l.numberOfLines = 0;
    l.font = [UIFont systemFontOfSize:size];
    l.textAlignment = NSTextAlignmentCenter;
    return l;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor whiteColor];

    UILabel *title = [self multilineLabel:22];
    title.text = @"vless proxy";
    title.font = [UIFont boldSystemFontOfSize:22];

    self.linkField = [[UITextField alloc] init];
    self.linkField.placeholder = @"paste vless:// link";
    self.linkField.borderStyle = UITextBorderStyleRoundedRect;
    self.linkField.autocorrectionType = UITextAutocorrectionTypeNo;
    self.linkField.autocapitalizationType = UITextAutocapitalizationTypeNone;
    self.linkField.spellCheckingType = UITextSpellCheckingTypeNo;
    self.linkField.keyboardType = UIKeyboardTypeURL;
    self.linkField.clearButtonMode = UITextFieldViewModeWhileEditing;
    self.linkField.returnKeyType = UIReturnKeyDone;
    self.linkField.font = [UIFont systemFontOfSize:14];
    self.linkField.delegate = self;

    self.connectButton = [self buttonWithTitle:@"connect" action:@selector(connect)];
    self.disconnectButton = [self buttonWithTitle:@"disconnect" action:@selector(disconnect)];

    self.statusLabel = [self multilineLabel:15];
    self.hintLabel = [self multilineLabel:13];
    self.hintLabel.textColor = [UIColor darkGrayColor];

    self.spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
    self.spinner.hidesWhenStopped = YES;

    UIStackView *buttons = [[UIStackView alloc] initWithArrangedSubviews:@[ self.connectButton, self.disconnectButton ]];
    buttons.axis = UILayoutConstraintAxisHorizontal;
    buttons.distribution = UIStackViewDistributionFillEqually;
    buttons.spacing = 12;

    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[ title, self.linkField, buttons, self.spinner, self.statusLabel, self.hintLabel ]];
    stack.axis = UILayoutConstraintAxisVertical;
    stack.spacing = 16;
    stack.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:stack];

    [NSLayoutConstraint activateConstraints:@[
        [stack.topAnchor constraintEqualToAnchor:self.topLayoutGuide.bottomAnchor constant:24],
        [stack.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor constant:16],
        [stack.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor constant:-16],
        [self.linkField.heightAnchor constraintEqualToConstant:40],
        [buttons.heightAnchor constraintEqualToConstant:44],
    ]];

    [self.view addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self.view action:@selector(endEditing:)]];
    [self refreshStatus];
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField resignFirstResponder];
    return YES;
}

- (void)setBusy:(BOOL)busy {
    self.connectButton.enabled = !busy;
    self.disconnectButton.enabled = !busy;
    if (busy) [self.spinner startAnimating];
    else [self.spinner stopAnimating];
}

- (void)showStatus:(NSString *)status hint:(NSString *)hint {
    self.statusLabel.text = status;
    self.hintLabel.text = hint ?: @"";
}

- (void)refreshStatus {
    [self setBusy:YES];
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        NSString *out = nil;
        int rc = runHelper(@"status", &out);
        BOOL proxyOK = systemProxyIsOurs();
        dispatch_async(dispatch_get_main_queue(), ^{
            [self setBusy:NO];
            if (rc == 0) {
                [self showStatus:[NSString stringWithFormat:@"connected - xray %@", out]
                            hint:proxyOK ? @"wifi proxy -> 127.0.0.1:8080" : kManualProxyHint];
            } else if (rc == 3) {
                [self showStatus:@"disconnected" hint:@"socks 127.0.0.1:1080, http 127.0.0.1:8080 while connected"];
            } else {
                [self showStatus:out hint:nil];
            }
        });
    });
}

- (void)connect {
    [self.view endEditing:YES];
    NSString *link = self.linkField.text ?: @"";
    NSString *err = nil;
    NSData *json = [VLESSConfig configJSONFromLink:link error:&err];
    if (!json) {
        [self showStatus:[NSString stringWithFormat:@"bad link: %@", err] hint:nil];
        return;
    }
    NSString *summary = [VLESSConfig summaryForLink:link];

    [[NSFileManager defaultManager] createDirectoryAtPath:CONFIG_DIR withIntermediateDirectories:YES
                                               attributes:@{ NSFilePosixPermissions: @0700 } error:nil];
    NSError *werr = nil;
    if (![json writeToFile:CONFIG_PATH options:NSDataWritingAtomic error:&werr]) {
        [self showStatus:[NSString stringWithFormat:@"cannot write config: %@", werr.localizedDescription] hint:nil];
        return;
    }
    chmod(CONFIG_PATH.fileSystemRepresentation, 0600);

    [self setBusy:YES];
    [self showStatus:[NSString stringWithFormat:@"connecting to %@...", summary] hint:nil];
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        NSString *startOut = nil, *proxyOut = nil;
        int rc = runHelper(@"start", &startOut);
        int prc = rc == 0 ? runHelper(@"proxy-on", &proxyOut) : -1;
        // give configd a moment to publish the new settings
        BOOL proxyOK = NO;
        for (int i = 0; rc == 0 && i < 10 && !(proxyOK = systemProxyIsOurs()); i++) usleep(200 * 1000);
        dispatch_async(dispatch_get_main_queue(), ^{
            [self setBusy:NO];
            if (rc != 0) {
                [self showStatus:startOut hint:nil];
                return;
            }
            NSString *hint = proxyOK ? @"wifi proxy -> 127.0.0.1:8080"
                                     : [NSString stringWithFormat:@"couldn't set the wifi proxy (%@). %@", prc == 0 ? @"not applied" : proxyOut, kManualProxyHint];
            [self showStatus:[NSString stringWithFormat:@"connected: %@\nxray %@", summary, startOut] hint:hint];
        });
    });
}

- (void)disconnect {
    [self.view endEditing:YES];
    [self setBusy:YES];
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        NSString *proxyOut = nil, *stopOut = nil;
        int prc = runHelper(@"proxy-off", &proxyOut);
        int rc = runHelper(@"stop", &stopOut);
        BOOL stillOurs = systemProxyIsOurs();
        dispatch_async(dispatch_get_main_queue(), ^{
            [self setBusy:NO];
            NSString *hint = nil;
            if (stillOurs || prc != 0)
                hint = @"turn the proxy off by hand: settings > wi-fi > (i) > http proxy > off";
            [self showStatus:rc == 0 ? @"disconnected" : stopOut hint:hint];
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
