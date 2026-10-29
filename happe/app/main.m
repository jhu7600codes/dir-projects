// Happe: subscriptions + vless:// links, pick a server, connect. plain uikit, ios 10.

#import <UIKit/UIKit.h>
#import <CFNetwork/CFNetwork.h>
#include <spawn.h>
#include <sys/wait.h>
#include <sys/stat.h>
#import "VLESSConfig.h"
#import "HPStore.h"
#import "HPScanner.h"

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

static const CGFloat kSide = 16;    // card inset from the screen edge
static const CGFloat kInner = 16;   // text inset inside a card
static const CGFloat kRadius = 12;

#pragma mark - cells

typedef NS_ENUM(NSInteger, HPCardPosition) {
    HPCardSingle,
    HPCardTop,
    HPCardMiddle,
    HPCardBottom,
};

// a grouped cell drawn as part of a rounded, inset card (ios 10 has no
// insetGrouped style, so the cell insets its own frame and masks its corners)
@interface HPCardCell : UITableViewCell
@property (nonatomic, assign) HPCardPosition position;
@property (nonatomic, strong) UIView *separator;
@end

@implementation HPCardCell

- (instancetype)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier {
    if ((self = [super initWithStyle:style reuseIdentifier:reuseIdentifier])) {
        self.backgroundColor = [UIColor whiteColor];
        self.separator = [[UIView alloc] init];
        self.separator.backgroundColor = VPRGB(0xc8c7cc);
        [self addSubview:self.separator];
        UIView *sel = [[UIView alloc] init];
        sel.backgroundColor = VPRGB(0xd9d9d9);
        self.selectedBackgroundView = sel;
    }
    return self;
}

- (void)setFrame:(CGRect)frame {
    if (self.superview && frame.size.width > 2 * kSide + 100) {
        frame.origin.x = kSide;
        frame.size.width = self.superview.bounds.size.width - 2 * kSide;
    }
    [super setFrame:frame];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    UIRectCorner corners = 0;
    if (self.position == HPCardSingle || self.position == HPCardTop) corners |= UIRectCornerTopLeft | UIRectCornerTopRight;
    if (self.position == HPCardSingle || self.position == HPCardBottom) corners |= UIRectCornerBottomLeft | UIRectCornerBottomRight;
    CAShapeLayer *mask = [CAShapeLayer layer];
    mask.path = [UIBezierPath bezierPathWithRoundedRect:self.bounds byRoundingCorners:corners
                                            cornerRadii:CGSizeMake(kRadius, kRadius)].CGPath;
    self.layer.mask = mask;

    CGFloat px = 1.0 / [UIScreen mainScreen].scale;
    self.separator.hidden = self.position == HPCardSingle || self.position == HPCardBottom;
    self.separator.frame = CGRectMake(kInner, self.bounds.size.height - px, self.bounds.size.width - kInner, px);
    [self bringSubviewToFront:self.separator];

    // keep labels on the same inner margin as everything else
    for (UILabel *l in @[ self.textLabel ?: [UILabel new], self.detailTextLabel ?: [UILabel new] ]) {
        CGRect f = l.frame;
        CGFloat right = CGRectGetMaxX(f);
        f.origin.x = kInner;
        f.size.width = MAX(0, right - kInner);
        l.frame = f;
    }
}

@end

@interface HPStatusCell : HPCardCell
@property (nonatomic, strong) UIView *dot;
@property (nonatomic, strong) UILabel *title;
@property (nonatomic, strong) UILabel *detail;
@end

@implementation HPStatusCell

- (instancetype)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier {
    if ((self = [super initWithStyle:style reuseIdentifier:reuseIdentifier])) {
        self.selectionStyle = UITableViewCellSelectionStyleNone;
        self.dot = [[UIView alloc] init];
        self.dot.layer.cornerRadius = 5;
        self.title = [[UILabel alloc] init];
        self.title.font = [UIFont systemFontOfSize:17 weight:UIFontWeightSemibold];
        self.detail = [[UILabel alloc] init];
        self.detail.font = [UIFont systemFontOfSize:13];
        self.detail.textColor = VPRGB(0x8e8e93);
        self.detail.numberOfLines = 0;
        for (UIView *v in @[ self.dot, self.title, self.detail ]) {
            v.translatesAutoresizingMaskIntoConstraints = NO;
            [self.contentView addSubview:v];
        }
        UIView *c = self.contentView;
        [NSLayoutConstraint activateConstraints:@[
            [self.dot.widthAnchor constraintEqualToConstant:10],
            [self.dot.heightAnchor constraintEqualToConstant:10],
            [self.dot.leadingAnchor constraintEqualToAnchor:c.leadingAnchor constant:kInner],
            [self.dot.centerYAnchor constraintEqualToAnchor:self.title.centerYAnchor],
            [self.title.leadingAnchor constraintEqualToAnchor:self.dot.trailingAnchor constant:10],
            [self.title.trailingAnchor constraintEqualToAnchor:c.trailingAnchor constant:-kInner],
            [self.title.topAnchor constraintEqualToAnchor:c.topAnchor constant:14],
            [self.detail.leadingAnchor constraintEqualToAnchor:self.title.leadingAnchor],
            [self.detail.trailingAnchor constraintEqualToAnchor:self.title.trailingAnchor],
            [self.detail.topAnchor constraintEqualToAnchor:self.title.bottomAnchor constant:3],
            [self.detail.bottomAnchor constraintEqualToAnchor:c.bottomAnchor constant:-14],
        ]];
    }
    return self;
}

@end

@interface HPButtonCell : HPCardCell
@property (nonatomic, strong) UILabel *label;
@property (nonatomic, strong) UIActivityIndicatorView *spinner;
@end

@implementation HPButtonCell

- (instancetype)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier {
    if ((self = [super initWithStyle:style reuseIdentifier:reuseIdentifier])) {
        self.label = [[UILabel alloc] init];
        self.label.font = [UIFont systemFontOfSize:17 weight:UIFontWeightSemibold];
        self.label.textColor = [UIColor whiteColor];
        self.label.textAlignment = NSTextAlignmentCenter;
        self.spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhite];
        self.spinner.hidesWhenStopped = YES;
        for (UIView *v in @[ self.label, self.spinner ]) {
            v.translatesAutoresizingMaskIntoConstraints = NO;
            [self.contentView addSubview:v];
        }
        UIView *c = self.contentView;
        [NSLayoutConstraint activateConstraints:@[
            [self.label.centerXAnchor constraintEqualToAnchor:c.centerXAnchor],
            [self.label.centerYAnchor constraintEqualToAnchor:c.centerYAnchor],
            [self.spinner.centerYAnchor constraintEqualToAnchor:c.centerYAnchor],
            [self.spinner.trailingAnchor constraintEqualToAnchor:c.trailingAnchor constant:-kInner],
        ]];
    }
    return self;
}

- (void)setColor:(UIColor *)color {
    self.backgroundColor = color;
    UIView *sel = [[UIView alloc] init];
    CGFloat r, g, b, a;
    [color getRed:&r green:&g blue:&b alpha:&a];
    sel.backgroundColor = [UIColor colorWithRed:r * 0.85 green:g * 0.85 blue:b * 0.85 alpha:a];
    self.selectedBackgroundView = sel;
}

@end

// section header / footer text, lined up with the text inside the cards
@interface HPSectionLabel : UITableViewHeaderFooterView
@property (nonatomic, strong) UILabel *label;
@property (nonatomic, strong) NSLayoutConstraint *top;
@property (nonatomic, strong) NSLayoutConstraint *bottom;
@end

@implementation HPSectionLabel

- (instancetype)initWithReuseIdentifier:(NSString *)reuseIdentifier {
    if ((self = [super initWithReuseIdentifier:reuseIdentifier])) {
        self.label = [[UILabel alloc] init];
        self.label.font = [UIFont systemFontOfSize:13];
        self.label.textColor = VPRGB(0x6d6d72);
        self.label.numberOfLines = 0;
        self.label.translatesAutoresizingMaskIntoConstraints = NO;
        [self.contentView addSubview:self.label];
        UIView *c = self.contentView;
        self.top = [self.label.topAnchor constraintEqualToAnchor:c.topAnchor];
        self.bottom = [self.label.bottomAnchor constraintEqualToAnchor:c.bottomAnchor];
        [NSLayoutConstraint activateConstraints:@[
            [self.label.leadingAnchor constraintEqualToAnchor:c.leadingAnchor constant:kSide + kInner],
            [self.label.trailingAnchor constraintEqualToAnchor:c.trailingAnchor constant:-(kSide + kInner)],
            self.top, self.bottom,
        ]];
        self.backgroundView = [[UIView alloc] init];
    }
    return self;
}

@end

#pragma mark - main screen

@interface VPViewController : UITableViewController <UIImagePickerControllerDelegate, UINavigationControllerDelegate>
@property (nonatomic, assign) VPState state;
@property (nonatomic, copy) NSString *statusTitle;
@property (nonatomic, copy) NSString *statusDetail;
@property (nonatomic, copy) NSString *proxyNote;
@property (nonatomic, copy) NSString *connectedLink;
@end

@implementation VPViewController

- (instancetype)init {
    return [super initWithStyle:UITableViewStyleGrouped];
}

- (HPStore *)store {
    return [HPStore shared];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    UITableView *tv = self.tableView;
    tv.backgroundColor = VPRGB(0xefeff4);
    tv.separatorStyle = UITableViewCellSeparatorStyleNone;
    tv.estimatedRowHeight = 56;
    tv.rowHeight = UITableViewAutomaticDimension;
    tv.estimatedSectionHeaderHeight = 30;
    tv.sectionHeaderHeight = UITableViewAutomaticDimension;
    tv.estimatedSectionFooterHeight = 20;
    tv.sectionFooterHeight = UITableViewAutomaticDimension;
    tv.tintColor = VPRGB(0x007aff);
    [tv registerClass:[HPSectionLabel class] forHeaderFooterViewReuseIdentifier:@"label"];

    UIView *header = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 320, 64)];
    UILabel *title = [[UILabel alloc] initWithFrame:CGRectMake(kSide, 16, 280, 41)];
    title.font = [UIFont systemFontOfSize:34 weight:UIFontWeightBold];
    title.text = @"Happe";
    [header addSubview:title];
    tv.tableHeaderView = header;

    UIRefreshControl *rc = [[UIRefreshControl alloc] init];
    [rc addTarget:self action:@selector(pullToRefresh) forControlEvents:UIControlEventValueChanged];
    tv.refreshControl = rc;

    self.state = VPStateUnknown;
    self.statusTitle = @"Checking…";
    [self refreshStatus];
    [self refreshStaleSubscriptions];
}

#pragma mark state

- (void)setState:(VPState)state title:(NSString *)title detail:(NSString *)detail note:(NSString *)note {
    self.state = state;
    self.statusTitle = title;
    self.statusDetail = detail;
    self.proxyNote = note;
    [self.tableView reloadData];
}

- (BOOL)busy {
    return self.state == VPStateWorking || self.state == VPStateUnknown;
}

- (NSString *)idleDetail {
    HPServer *s = self.store.selectedServer;
    if (s) return s.name;
    return self.store.allServers.count ? @"Pick a server below." : @"Add a subscription or a vless:// link.";
}

// helper output looks like "running 123 socks 10808 (direct)"
static int socksPortFrom(NSString *helperOut) {
    NSRange r = [helperOut rangeOfString:@"socks "];
    int port = r.location == NSNotFound ? 0 : [[helperOut substringFromIndex:NSMaxRange(r)] intValue];
    return port > 0 ? port : 1080;
}

- (NSString *)proxyNoteFor:(BOOL)proxyOK socks:(int)socks {
    NSString *socksNote = socks == 1080 ? @"SOCKS5 is on 127.0.0.1:1080."
        : [NSString stringWithFormat:@"SOCKS5 is on 127.0.0.1:%d (1080 is taken by another tweak).", socks];
    return proxyOK ? [@"Wi-Fi proxy is set to 127.0.0.1:8080. " stringByAppendingString:socksNote]
                   : [NSString stringWithFormat:@"%@ %@", kManualProxyHint, socksNote];
}

- (void)refreshStatus {
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        NSString *out = nil;
        int rc = runHelper(@"status", &out);
        BOOL proxyOK = systemProxyIsOurs();
        dispatch_async(dispatch_get_main_queue(), ^{
            if (rc == 0) {
                self.connectedLink = self.store.selectedLink;
                HPServer *s = self.store.selectedServer;
                [self setState:VPStateConnected title:@"Connected" detail:s ? s.name : [@"xray " stringByAppendingString:out]
                          note:[self proxyNoteFor:proxyOK socks:socksPortFrom(out)]];
            } else if (rc == 3) {
                [self setState:VPStateDisconnected title:@"Not connected" detail:[self idleDetail] note:nil];
            } else {
                [self setState:VPStateError title:@"Helper unavailable" detail:out note:nil];
            }
        });
    });
}

#pragma mark connect / disconnect

- (void)connect {
    HPServer *server = self.store.selectedServer;
    if (!server) {
        [self setState:VPStateError title:@"No server selected" detail:[self idleDetail] note:nil];
        return;
    }
    NSString *err = nil;
    NSData *json = [VLESSConfig configJSONFromLink:server.link error:&err];
    if (!json) {
        [self setState:VPStateError title:@"Invalid link" detail:err note:nil];
        return;
    }
    [[NSFileManager defaultManager] createDirectoryAtPath:CONFIG_DIR withIntermediateDirectories:YES
                                               attributes:@{ NSFilePosixPermissions: @0700 } error:nil];
    NSError *werr = nil;
    if (![json writeToFile:CONFIG_PATH options:NSDataWritingAtomic error:&werr]) {
        [self setState:VPStateError title:@"Couldn't save config" detail:werr.localizedDescription note:nil];
        return;
    }
    chmod(CONFIG_PATH.fileSystemRepresentation, 0600);

    [self setState:VPStateWorking title:@"Connecting…" detail:server.name note:nil];
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        NSString *startOut = nil, *proxyOut = nil;
        int rc = runHelper(@"start", &startOut);
        int prc = rc == 0 ? runHelper(@"proxy-on", &proxyOut) : -1;
        // give configd a moment to publish the new settings
        BOOL proxyOK = NO;
        for (int i = 0; rc == 0 && i < 10 && !(proxyOK = systemProxyIsOurs()); i++) usleep(200 * 1000);
        dispatch_async(dispatch_get_main_queue(), ^{
            if (rc != 0) {
                [self setState:VPStateError title:@"Couldn't connect" detail:startOut note:nil];
                return;
            }
            self.connectedLink = server.link;
            NSString *note = [self proxyNoteFor:proxyOK socks:socksPortFrom(startOut)];
            if (!proxyOK && prc != 0 && proxyOut.length)
                note = [NSString stringWithFormat:@"Couldn't set the Wi-Fi proxy (%@). %@", proxyOut, note];
            [self setState:VPStateConnected title:@"Connected" detail:server.name note:note];
        });
    });
}

- (void)disconnect {
    [self setState:VPStateWorking title:@"Disconnecting…" detail:self.statusDetail note:nil];
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        NSString *proxyOut = nil, *stopOut = nil;
        int prc = runHelper(@"proxy-off", &proxyOut);
        int rc = runHelper(@"stop", &stopOut);
        BOOL stillOurs = systemProxyIsOurs();
        dispatch_async(dispatch_get_main_queue(), ^{
            self.connectedLink = nil;
            NSString *note = (stillOurs || prc != 0)
                ? @"Turn the proxy off manually: Settings > Wi-Fi > (i) > HTTP Proxy > Off." : nil;
            if (rc == 0) [self setState:VPStateDisconnected title:@"Not connected" detail:[self idleDetail] note:note];
            else [self setState:VPStateError title:@"Couldn't stop xray" detail:stopOut note:note];
        });
    });
}

#pragma mark subscriptions

- (void)refreshSubs:(NSArray<HPSub *> *)subs done:(void (^)(void))done {
    if (!subs.count) {
        if (done) done();
        return;
    }
    __block NSUInteger left = subs.count;
    for (HPSub *s in subs) {
        [self.store refresh:s done:^{
            [self.tableView reloadData];
            if (--left == 0 && done) done();
        }];
    }
    [self.tableView reloadData];
}

- (void)refreshStaleSubscriptions {
    NSMutableArray *stale = [NSMutableArray array];
    for (HPSub *s in self.store.subs)
        if (!s.updated || -[s.updated timeIntervalSinceNow] > 6 * 3600) [stale addObject:s];
    [self refreshSubs:stale done:nil];
}

- (void)pullToRefresh {
    [self refreshSubs:self.store.subs done:^{
        [self.refreshControl endRefreshing];
    }];
    if (!self.store.subs.count) [self.refreshControl endRefreshing];
}

- (void)showAlert:(NSString *)title message:(NSString *)message {
    UIAlertController *info = [UIAlertController alertControllerWithTitle:title message:message
                                                            preferredStyle:UIAlertControllerStyleAlert];
    [info addAction:[UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:nil]];
    [self presentViewController:info animated:YES completion:nil];
}

// everything that adds servers ends up here: the paste alert, qr codes and
// happ:// / vless:// links opened from safari
- (void)addText:(NSString *)text {
    NSArray *newSubs = nil;
    NSString *msg = nil;
    NSUInteger n = [self.store addFromText:text ?: @"" newSubs:&newSubs message:&msg];
    if (self.state != VPStateConnected && !self.busy) self.statusDetail = [self idleDetail];
    [self.tableView reloadData];
    [self refreshSubs:newSubs done:^{
        if (self.state == VPStateDisconnected) {
            self.statusDetail = [self idleDetail];
            [self.tableView reloadData];
        }
    }];
    if (n == 0 || msg)
        [self showAlert:n ? @"Added" : @"Nothing Added"
                message:msg ?: @"Use a subscription URL (https://…), a vless:// link or a happ://add/… link."];
}

- (void)showAdd {
    UIAlertController *sheet = [UIAlertController alertControllerWithTitle:nil message:nil
                                                            preferredStyle:UIAlertControllerStyleActionSheet];
    [sheet addAction:[UIAlertAction actionWithTitle:@"Paste Link or URL" style:UIAlertActionStyleDefault handler:^(UIAlertAction *a) {
        [self showPasteAlert];
    }]];
    [sheet addAction:[UIAlertAction actionWithTitle:@"Scan QR Code" style:UIAlertActionStyleDefault handler:^(UIAlertAction *a) {
        [self showScanner];
    }]];
    [sheet addAction:[UIAlertAction actionWithTitle:@"QR Code from Photos" style:UIAlertActionStyleDefault handler:^(UIAlertAction *a) {
        [self showPhotoPicker];
    }]];
    [sheet addAction:[UIAlertAction actionWithTitle:@"Cancel" style:UIAlertActionStyleCancel handler:nil]];
    [self presentViewController:sheet animated:YES completion:nil];
}

- (void)showPasteAlert {
    UIAlertController *a = [UIAlertController alertControllerWithTitle:@"Add Servers"
        message:@"Paste subscription URLs, vless:// or happ://add/ links. Several at once are fine." preferredStyle:UIAlertControllerStyleAlert];
    NSString *clip = [UIPasteboard generalPasteboard].string;
    [a addTextFieldWithConfigurationHandler:^(UITextField *f) {
        f.placeholder = @"https://… or vless://…";
        f.autocorrectionType = UITextAutocorrectionTypeNo;
        f.autocapitalizationType = UITextAutocapitalizationTypeNone;
        f.keyboardType = UIKeyboardTypeURL;
        f.clearButtonMode = UITextFieldViewModeWhileEditing;
        if ([clip rangeOfString:@"://"].location != NSNotFound) f.text = clip;
    }];
    [a addAction:[UIAlertAction actionWithTitle:@"Cancel" style:UIAlertActionStyleCancel handler:nil]];
    __weak UIAlertController *wa = a;
    [a addAction:[UIAlertAction actionWithTitle:@"Add" style:UIAlertActionStyleDefault handler:^(UIAlertAction *act) {
        [self addText:wa.textFields.firstObject.text];
    }]];
    [self presentViewController:a animated:YES completion:nil];
}

- (void)showScanner {
    HPScanViewController *scan = [[HPScanViewController alloc] initWithCompletion:^(NSString *text) {
        [self dismissViewControllerAnimated:YES completion:^{
            if (text) [self addText:text];
        }];
    }];
    [self presentViewController:scan animated:YES completion:nil];
}

- (void)showPhotoPicker {
    UIImagePickerController *picker = [[UIImagePickerController alloc] init];
    picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    picker.delegate = self;
    [self presentViewController:picker animated:YES completion:nil];
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info {
    UIImage *image = info[UIImagePickerControllerOriginalImage];
    [self dismissViewControllerAnimated:YES completion:^{
        NSString *text = image ? HPQRTextInImage(image) : nil;
        if (text) [self addText:text];
        else [self showAlert:@"No QR Code Found" message:@"Pick a photo or screenshot with a clearly visible QR code."];
    }];
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (NSString *)versionLine {
    NSString *app = [NSBundle mainBundle].infoDictionary[@"CFBundleShortVersionString"] ?: @"?";
    NSString *xray = [[NSString stringWithContentsOfFile:@"/usr/share/happe/xray.version" encoding:NSUTF8StringEncoding error:nil]
        stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    NSArray *parts = [xray componentsSeparatedByString:@" "];
    NSString *xv = parts.count ? [parts[0] stringByReplacingOccurrencesOfString:@"v" withString:@""] : nil;
    if (!xv.length) return [NSString stringWithFormat:@"Happe %@", app];
    return parts.count > 1 ? [NSString stringWithFormat:@"Happe %@ · Xray %@ (%@)", app, xv, parts[1]]
                           : [NSString stringWithFormat:@"Happe %@ · Xray %@", app, xv];
}

- (void)confirmDeleteSub:(HPSub *)sub {
    UIAlertController *a = [UIAlertController alertControllerWithTitle:[NSString stringWithFormat:@"Delete “%@”?", sub.displayTitle]
        message:@"Its servers are removed from the list." preferredStyle:UIAlertControllerStyleAlert];
    [a addAction:[UIAlertAction actionWithTitle:@"Cancel" style:UIAlertActionStyleCancel handler:nil]];
    [a addAction:[UIAlertAction actionWithTitle:@"Delete" style:UIAlertActionStyleDestructive handler:^(UIAlertAction *act) {
        [self.store removeSub:sub];
        if (self.state == VPStateDisconnected) self.statusDetail = [self idleDetail];
        [self.tableView reloadData];
    }]];
    [self presentViewController:a animated:YES completion:nil];
}

#pragma mark table layout
// sections: status, button, one per subscription, manual links (if any), add

- (NSInteger)subBase {
    return 2;
}

- (BOOL)hasManual {
    return self.store.manual.count > 0;
}

- (NSInteger)manualSection {
    return self.hasManual ? self.subBase + (NSInteger)self.store.subs.count : -1;
}

- (NSInteger)addSection {
    return self.subBase + (NSInteger)self.store.subs.count + (self.hasManual ? 1 : 0);
}

- (HPSub *)subForSection:(NSInteger)section {
    NSInteger i = section - self.subBase;
    return (i >= 0 && i < (NSInteger)self.store.subs.count) ? self.store.subs[(NSUInteger)i] : nil;
}

- (HPServer *)serverAt:(NSIndexPath *)ip {
    HPSub *sub = [self subForSection:ip.section];
    if (sub) return (NSUInteger)ip.row < sub.servers.count ? sub.servers[(NSUInteger)ip.row] : nil;
    if (ip.section == self.manualSection) return self.store.manual[(NSUInteger)ip.row];
    return nil;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return self.addSection + 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    HPSub *sub = [self subForSection:section];
    if (sub) return MAX((NSInteger)sub.servers.count, 1);
    if (section == self.manualSection) return (NSInteger)self.store.manual.count;
    return 1;
}

- (HPCardPosition)positionFor:(NSIndexPath *)ip {
    NSInteger n = [self tableView:self.tableView numberOfRowsInSection:ip.section];
    if (n == 1) return HPCardSingle;
    if (ip.row == 0) return HPCardTop;
    return ip.row == n - 1 ? HPCardBottom : HPCardMiddle;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)ip {
    if (ip.section == 0) {
        HPStatusCell *c = [tableView dequeueReusableCellWithIdentifier:@"status"]
            ?: [[HPStatusCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"status"];
        c.position = HPCardSingle;
        c.title.text = self.statusTitle;
        c.detail.text = self.statusDetail.length ? self.statusDetail : @" ";
        UIColor *dot = VPRGB(0xc7c7cc);
        if (self.state == VPStateConnected) dot = VPRGB(0x34c759);
        if (self.busy) dot = VPRGB(0xff9500);
        if (self.state == VPStateError) dot = VPRGB(0xff3b30);
        c.dot.backgroundColor = dot;
        return c;
    }
    if (ip.section == 1) {
        HPButtonCell *c = [tableView dequeueReusableCellWithIdentifier:@"button"]
            ?: [[HPButtonCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"button"];
        c.position = HPCardSingle;
        // while reconnecting/disconnecting the button keeps its red "Disconnect" look
        BOOL connected = self.state == VPStateConnected || (self.busy && self.connectedLink);
        UIColor *color = connected ? VPRGB(0xff3b30) : VPRGB(0x007aff);
        [c setColor:self.busy ? [color colorWithAlphaComponent:0.5] : color];
        c.label.text = connected ? @"Disconnect" : @"Connect";
        if (self.busy) [c.spinner startAnimating];
        else [c.spinner stopAnimating];
        return c;
    }
    if (ip.section == self.addSection) {
        HPCardCell *c = [tableView dequeueReusableCellWithIdentifier:@"add"]
            ?: [[HPCardCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"add"];
        c.position = HPCardSingle;
        c.textLabel.text = @"Add Subscription or Link…";
        c.textLabel.textColor = VPRGB(0x007aff);
        c.textLabel.font = [UIFont systemFontOfSize:17];
        return c;
    }

    HPCardCell *c = [tableView dequeueReusableCellWithIdentifier:@"server"]
        ?: [[HPCardCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"server"];
    c.position = [self positionFor:ip];
    c.textLabel.font = [UIFont systemFontOfSize:16];
    c.detailTextLabel.font = [UIFont systemFontOfSize:12];
    c.detailTextLabel.textColor = VPRGB(0x8e8e93);
    HPServer *s = [self serverAt:ip];
    if (!s) {
        HPSub *sub = [self subForSection:ip.section];
        c.textLabel.text = sub.refreshing ? @"Loading…" : @"No servers";
        c.textLabel.textColor = VPRGB(0x8e8e93);
        c.detailTextLabel.text = nil;
        c.accessoryType = UITableViewCellAccessoryNone;
        c.selectionStyle = UITableViewCellSelectionStyleNone;
        return c;
    }
    c.textLabel.text = s.name;
    c.textLabel.textColor = [UIColor blackColor];
    c.detailTextLabel.text = s.detail;
    c.selectionStyle = UITableViewCellSelectionStyleDefault;
    c.accessoryType = [s.link isEqualToString:self.store.selectedServer.link] ? UITableViewCellAccessoryCheckmark
                                                                                : UITableViewCellAccessoryNone;
    return c;
}

- (NSString *)footerForSub:(HPSub *)sub {
    if (sub.refreshing) return @"Updating…";
    NSMutableArray *bits = [NSMutableArray array];
    if (sub.error) [bits addObject:[@"Couldn't update: " stringByAppendingString:sub.error]];
    NSDictionary *u = sub.userinfo;
    if (u[@"total"] || u[@"download"]) {
        long long used = [u[@"upload"] longLongValue] + [u[@"download"] longLongValue];
        long long total = [u[@"total"] longLongValue];
        NSString *usedStr = [NSByteCountFormatter stringFromByteCount:used countStyle:NSByteCountFormatterCountStyleFile];
        [bits addObject:total > 0 ? [NSString stringWithFormat:@"%@ of %@ used", usedStr,
                                     [NSByteCountFormatter stringFromByteCount:total countStyle:NSByteCountFormatterCountStyleFile]]
                                  : [usedStr stringByAppendingString:@" used"]];
    }
    if ([u[@"expire"] longLongValue] > 0) {
        NSDateFormatter *f = [[NSDateFormatter alloc] init];
        f.dateStyle = NSDateFormatterMediumStyle;
        f.timeStyle = NSDateFormatterNoStyle;
        [bits addObject:[@"expires " stringByAppendingString:[f stringFromDate:[NSDate dateWithTimeIntervalSince1970:[u[@"expire"] doubleValue]]]]];
    }
    if (sub.skipped) [bits addObject:[NSString stringWithFormat:@"%lu non-vless hidden", (unsigned long)sub.skipped]];
    if (sub.updated) {
        NSTimeInterval ago = -[sub.updated timeIntervalSinceNow];
        NSString *when = ago < 60 ? @"just now"
            : ago < 3600 ? [NSString stringWithFormat:@"%d min ago", (int)(ago / 60)]
            : ago < 86400 ? [NSString stringWithFormat:@"%d h ago", (int)(ago / 3600)]
            : [NSString stringWithFormat:@"%d d ago", (int)(ago / 86400)];
        [bits addObject:[@"updated " stringByAppendingString:when]];
    }
    return [bits componentsJoinedByString:@" · "];
}

- (UIView *)labelView:(NSString *)text header:(BOOL)header {
    HPSectionLabel *v = [self.tableView dequeueReusableHeaderFooterViewWithIdentifier:@"label"];
    v.label.text = text;
    v.top.constant = header ? 8 : 8;
    v.bottom.constant = header ? -8 : -4;
    return v;
}

- (UIView *)tableView:(UITableView *)tableView viewForHeaderInSection:(NSInteger)section {
    HPSub *sub = [self subForSection:section];
    if (sub) return [self labelView:sub.displayTitle.uppercaseString header:YES];
    if (section == self.manualSection) return [self labelView:@"LINKS" header:YES];
    return nil;
}

- (CGFloat)tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section {
    if ([self subForSection:section] || section == self.manualSection) return UITableViewAutomaticDimension;
    if (section == 0) return 8;
    return section == 1 ? 12 : 24;
}

- (UIView *)tableView:(UITableView *)tableView viewForFooterInSection:(NSInteger)section {
    NSString *text = nil;
    HPSub *sub = [self subForSection:section];
    if (sub) text = [self footerForSub:sub];
    else if (section == 1) text = self.proxyNote;
    else if (section == self.addSection)
        text = self.store.subs.count ? [NSString stringWithFormat:@"Pull down to update subscriptions. Swipe a server for more.\n\n%@", self.versionLine]
                                     : self.versionLine;
    return text.length ? [self labelView:text header:NO] : nil;
}

- (CGFloat)tableView:(UITableView *)tableView heightForFooterInSection:(NSInteger)section {
    HPSub *sub = [self subForSection:section];
    BOOL has = (sub && [self footerForSub:sub].length) || (section == 1 && self.proxyNote.length)
        || section == self.addSection;
    return has ? UITableViewAutomaticDimension : 4;
}

#pragma mark table actions

- (BOOL)tableView:(UITableView *)tableView shouldHighlightRowAtIndexPath:(NSIndexPath *)ip {
    if (ip.section == 0) return NO;
    if (ip.section == 1) return !self.busy;
    if (ip.section == self.addSection) return YES;
    return [self serverAt:ip] != nil;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)ip {
    [tableView deselectRowAtIndexPath:ip animated:YES];
    if (ip.section == 1) {
        if (self.busy) return;
        if (self.state == VPStateConnected) [self disconnect];
        else [self connect];
        return;
    }
    if (ip.section == self.addSection) {
        [self showAdd];
        return;
    }
    HPServer *s = [self serverAt:ip];
    if (!s || self.busy) return;
    self.store.selectedLink = s.link;
    [self.store save];
    if (self.state == VPStateConnected) {
        // switching servers while connected reconnects right away
        if (![s.link isEqualToString:self.connectedLink]) [self connect];
        else [tableView reloadData];
    } else {
        [self setState:VPStateDisconnected title:@"Not connected" detail:s.name note:nil];
    }
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)ip {
    return [self subForSection:ip.section] != nil || ip.section == self.manualSection;
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)style forRowAtIndexPath:(NSIndexPath *)ip {
}

- (NSArray<UITableViewRowAction *> *)tableView:(UITableView *)tableView editActionsForRowAtIndexPath:(NSIndexPath *)ip {
    HPSub *sub = [self subForSection:ip.section];
    if (sub) {
        UITableViewRowAction *del = [UITableViewRowAction rowActionWithStyle:UITableViewRowActionStyleDestructive
            title:@"Delete Sub" handler:^(UITableViewRowAction *a, NSIndexPath *p) {
                [tableView setEditing:NO animated:YES];
                [self confirmDeleteSub:sub];
            }];
        UITableViewRowAction *upd = [UITableViewRowAction rowActionWithStyle:UITableViewRowActionStyleNormal
            title:@"Update" handler:^(UITableViewRowAction *a, NSIndexPath *p) {
                [tableView setEditing:NO animated:YES];
                [self refreshSubs:@[ sub ] done:nil];
            }];
        upd.backgroundColor = VPRGB(0x8e8e93);
        return @[ del, upd ];
    }
    HPServer *s = [self serverAt:ip];
    return @[ [UITableViewRowAction rowActionWithStyle:UITableViewRowActionStyleDestructive title:@"Delete"
        handler:^(UITableViewRowAction *a, NSIndexPath *p) {
            [self.store removeManual:s];
            if (self.state == VPStateDisconnected) self.statusDetail = [self idleDetail];
            [tableView reloadData];
        }] ];
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
    // the list scrolls under the transparent status bar; give the bar a backdrop
    UIView *bar = [[UIView alloc] initWithFrame:[UIApplication sharedApplication].statusBarFrame];
    bar.backgroundColor = [VPRGB(0xefeff4) colorWithAlphaComponent:0.96];
    bar.autoresizingMask = UIViewAutoresizingFlexibleWidth;
    [self.window addSubview:bar];
    return YES;
}

// happ://add/…, vless://… and happe://… links from safari or a qr scan in another app
- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary *)options {
    VPViewController *vc = (VPViewController *)self.window.rootViewController;
    NSString *text = url.absoluteString;
    if ([url.scheme.lowercaseString isEqualToString:@"happe"]) {
        // happe://add/<url> works like happ://add/<url>
        text = [@"happ://" stringByAppendingString:[text substringFromIndex:8]];
    }
    [vc addText:text];
    return YES;
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    VPViewController *vc = (VPViewController *)self.window.rootViewController;
    [vc refreshStatus];
    [vc refreshStaleSubscriptions];
}

@end

int main(int argc, char *argv[]) {
    @autoreleasepool {
        return UIApplicationMain(argc, argv, nil, NSStringFromClass([VPAppDelegate class]));
    }
}
