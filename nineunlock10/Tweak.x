// NineUnlock10 - ios 9 slide to unlock on ios 10
// backport of NineUnlock by Minh Ton (MIT, https://github.com/Minh-Ton/NineUnlock)
// the ios 13/14 CoverSheet (CS*) hooks were rewritten for the ios 10 SBDashBoard* classes.

#import "NineUnlock10.h"

#define PREFS_ID CFSTR("com.jhu7600.nineunlock10")

// ---------------------------------------------------------------- prefs

static BOOL enabled, lockSound;
static BOOL stuEnabled, stuChevron;
static NSString *stuText, *stuColor;
static CGFloat stuSize;

static id pref(NSString *key, id fallback) {
    id v = (__bridge_transfer id)CFPreferencesCopyAppValue((__bridge CFStringRef)key, PREFS_ID);
    return v ?: fallback;
}

static void loadPrefs(void) {
    CFPreferencesAppSynchronize(PREFS_ID);
    enabled             = [pref(@"enabled", @YES) boolValue];
    lockSound           = [pref(@"lockSound", @YES) boolValue];
    stuEnabled          = [pref(@"stuEnabled", @YES) boolValue];
    stuText             = pref(@"stuText", @"slide to unlock");
    stuColor            = pref(@"stuColor", @"#FFFFFF");
    stuSize             = [pref(@"stuSize", @25) floatValue];
    stuChevron          = [pref(@"stuChevron", @YES) boolValue];
}

static UIColor *colorFromHex(NSString *hex) {
    unsigned rgb = 0;
    NSScanner *s = [NSScanner scannerWithString:hex ?: @"#FFFFFF"];
    if ([hex hasPrefix:@"#"]) [s setScanLocation:1];
    [s scanHexInt:&rgb];
    return [UIColor colorWithRed:((rgb >> 16) & 0xFF) / 255.0
                           green:((rgb >> 8) & 0xFF) / 255.0
                            blue:(rgb & 0xFF) / 255.0 alpha:1.0];
}

// ---------------------------------------------------------------- shared state

static BOOL isOnLockscreen = YES;
static __weak SBDashBoardMainPageView *mainPageView;
static __weak SBPagedScrollView *pagedScrollView;
static __weak SBDashBoardTodayPageViewController *todayPage;
static __weak SBDashBoardFixedFooterViewController *fixedFooter;
static __weak UIViewController *dashBoard;

// these only exist when their group got hooked, so always go through these
static void updateSTU(void)   { if ([mainPageView respondsToSelector:@selector(nls_updateSTU)]) [mainPageView nls_updateSTU]; }

static void requestUnlock(void) {
    SBLockScreenManager *m = [%c(SBLockScreenManager) sharedInstance];
    if ([m respondsToSelector:@selector(lockScreenViewControllerRequestsUnlock)]) {
        [m lockScreenViewControllerRequestsUnlock];
    } else if ([m respondsToSelector:@selector(unlockUIFromSource:withOptions:)]) {
        [m unlockUIFromSource:0 withOptions:nil];
    }
}

static void scrollToMainPage(void) {
    if (pagedScrollView) [pagedScrollView scrollToPageAtIndex:1 animated:YES];
}

static void updateTodayPage(void) {
    if (!todayPage.isViewLoaded) return;
    BOOL hide = enabled && stuEnabled && isOnLockscreen;
    todayPage.view.alpha = hide ? 0.0 : 1.0;
}

static void updateFooter(void) {
    if (!fixedFooter.isViewLoaded) return;
    fixedFooter.view.hidden = enabled && stuEnabled; // page dots
}

static void updateAll(void) {
    updateSTU();
    updateTodayPage();
    updateFooter();
}

static void setOnLockscreen(BOOL v) {
    isOnLockscreen = v;
    updateAll();
}

// ================================================================ slide to unlock

%group SlideToUnlock

%hook SBDashBoardMainPageView
%property (nonatomic, retain) _UIGlintyStringView *stuView;

- (void)layoutSubviews {
    %orig;
    mainPageView = self;
    if (!self.stuView) {
        self.stuView = [[%c(_UIGlintyStringView) alloc] initWithText:stuText andFont:[UIFont systemFontOfSize:stuSize]];
    }
    [self nls_updateSTU];
}

%new
- (void)nls_updateSTU {
    _UIGlintyStringView *stu = self.stuView;
    if (!stu) return;

    if (enabled && stuEnabled && isOnLockscreen) {
        if (stu.superview != self) [self addSubview:stu];
        stu.frame = CGRectMake(0, self.bounds.size.height - 150, self.bounds.size.width, 150);
        [self sendSubviewToBack:stu];
        [stu setChevronStyle:stuChevron ? 1 : 0];

        // the shimmer color lives on a sublayer, guard it so a layout change can't crash springboard
        NSArray<CALayer *> *outer = stu.layer.sublayers;
        NSArray<CALayer *> *inner = outer.count > 0 ? outer[0].sublayers : nil;
        if (inner.count > 2) {
            inner[2].backgroundColor = [colorFromHex(stuColor) colorWithAlphaComponent:0.65].CGColor;
        }

        [stu hide];
        [stu show];
    } else {
        [stu hide];
        [stu removeFromSuperview];
    }
}
%end

// hide "press home to unlock"
%hook SBUICallToActionLabel
- (void)setText:(NSString *)text forLanguage:(NSString *)lang animated:(BOOL)animated {
    %orig((enabled && stuEnabled) ? @"" : text, lang, animated);
}
%end

// the page to the left (today view) is emptied out. landing on it = unlock.
%hook SBPagedScrollView
- (void)layoutSubviews {
    %orig;
    if (!pagedScrollView && [self.superview isKindOfClass:%c(SBDashBoardView)]) pagedScrollView = self;
}

- (void)setContentOffset:(CGPoint)offset {
    %orig;
    if (self != pagedScrollView || !(enabled && stuEnabled && isOnLockscreen)) return;

    static BOOL fired = NO;
    CGFloat w = self.bounds.size.width;
    if (offset.x > w * 0.5) { fired = NO; return; }

    // wait until the finger is up and the deceleration settled on page 0
    if (offset.x <= 1.0 && !fired) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.05 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            if (fired || self.isTracking || self.isDecelerating || self.contentOffset.x > 1.0) return;
            fired = YES;
            requestUnlock();
        });
    }
}
%end

%hook SBDashBoardTodayPageViewController
- (void)viewDidLayoutSubviews {
    %orig;
    todayPage = self;
    updateTodayPage();
}
%end

// cancel on the passcode keypad -> back to the lock screen
%hook SBUIPasscodeLockNumberPad
- (void)_cancelButtonHit {
    %orig;
    if (enabled && stuEnabled) scrollToMainPage();
}
%end

%hook SBDashBoardPasscodeViewController
- (void)viewDidDisappear:(BOOL)animated {
    %orig;
    if (enabled && stuEnabled) scrollToMainPage();
}
%end

%end // SlideToUnlock

// page dots, only if the class is there
%group FixedFooter
%hook SBDashBoardFixedFooterViewController
- (void)viewDidLayoutSubviews {
    %orig;
    fixedFooter = self;
    updateFooter();
}
%end
%end

// ================================================================ lock screen state

%group LockState
%hook SBDashBoardViewController
- (void)viewDidLoad {
    %orig;
    dashBoard = self;
}

- (void)viewWillAppear:(BOOL)animated {
    %orig;
    BOOL authed = [self respondsToSelector:@selector(isAuthenticated)] ? [self isAuthenticated] : NO;
    setOnLockscreen(!authed);
}
%end
%end

// ================================================================ ios 9 lock sound

%group LockSound
%hook SBLockScreenManager
- (void)_playLockSound {
    if (!(enabled && lockSound)) { %orig; return; }
    static SystemSoundID sound = 0;
    if (!sound) {
        NSURL *url = [NSURL fileURLWithPath:@"/Library/Application Support/NineUnlock10/lock.caf"];
        AudioServicesCreateSystemSoundID((__bridge CFURLRef)url, &sound);
    }
    AudioServicesPlaySystemSound(sound);
}
%end
%end

// ================================================================ init

static void prefsChanged(CFNotificationCenterRef c, void *o, CFStringRef n, const void *obj, CFDictionaryRef info) {
    loadPrefs();
    if ([mainPageView respondsToSelector:@selector(stuView)]) {
        _UIGlintyStringView *stu = mainPageView.stuView;
        [stu setText:stuText];
        [stu setNeedsTextUpdate:YES];
        [stu updateText];
    }
    updateAll();
}

static void displayChanged(CFNotificationCenterRef c, void *o, CFStringRef n, const void *obj, CFDictionaryRef info) {
    setOnLockscreen(YES);
}

static BOOL has(const char *cls) { return objc_getClass(cls) != Nil; }

%ctor {
    loadPrefs();

    // only hook what actually exists on this ios 10 build, so a missing class can't take springboard down
    if (has("SBDashBoardViewController")) %init(LockState);
    if (has("SBDashBoardMainPageView") && has("SBPagedScrollView") && has("SBDashBoardTodayPageViewController")) {
        %init(SlideToUnlock);
    }
    if (has("SBDashBoardFixedFooterViewController")) %init(FixedFooter);
    if ([%c(SBLockScreenManager) instancesRespondToSelector:@selector(_playLockSound)]) %init(LockSound);

    CFNotificationCenterRef darwin = CFNotificationCenterGetDarwinNotifyCenter();
    CFNotificationCenterAddObserver(darwin, NULL, prefsChanged, CFSTR("com.jhu7600.nineunlock10/prefsupdated"), NULL, CFNotificationSuspensionBehaviorCoalesce);
    CFNotificationCenterAddObserver(darwin, NULL, displayChanged, CFSTR("com.apple.iokit.hid.displayStatus"), NULL, CFNotificationSuspensionBehaviorDeliverImmediately);
}
