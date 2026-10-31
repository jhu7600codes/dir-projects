// NineLS10 - the ios 9 lock screen on ios 10
// backport of NineLS by Minh Ton (MIT, https://github.com/Minh-Ton/NineLS)
// the ios 13/14 CoverSheet (CS*) hooks were rewritten for the ios 10 SBDashBoard* classes.

#import "NineLS10.h"

#define PREFS_ID CFSTR("com.jhu7600.ninels10")

// ---------------------------------------------------------------- prefs

static BOOL enabled, lockSound;
static BOOL stuEnabled, stuChevron, stuAutoHide;
static NSString *stuText, *stuColor;
static CGFloat stuSize;
static BOOL notifEnabled, notifSeparators;
static CGFloat notifSeparatorAlpha;
static BOOL musicEnabled;

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
    stuAutoHide         = [pref(@"stuAutoHide", @YES) boolValue];
    notifEnabled        = [pref(@"notifEnabled", @YES) boolValue];
    notifSeparators     = [pref(@"notifSeparators", @YES) boolValue];
    notifSeparatorAlpha = [pref(@"notifSeparatorAlpha", @0.5) floatValue];
    musicEnabled        = [pref(@"musicEnabled", @YES) boolValue];
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
static BOOL musicActive = NO;
static BOOL hasNotifications = NO;
static __weak SBDashBoardMainPageView *mainPageView;
static __weak SBPagedScrollView *pagedScrollView;
static __weak SBDashBoardTodayPageViewController *todayPage;
static __weak SBDashBoardFixedFooterViewController *fixedFooter;
static __weak UIViewController *dashBoard;
static UIVisualEffectView *notifBlur;

// these only exist when their group got hooked, so always go through these
static void updateSTU(void)   { if ([mainPageView respondsToSelector:@selector(nls_updateSTU)]) [mainPageView nls_updateSTU]; }
static void updateMusic(void) { if ([mainPageView respondsToSelector:@selector(nls_updateMusic)]) [mainPageView nls_updateMusic]; }
static UIImageView *artworkView(void) { return [mainPageView respondsToSelector:@selector(nlsArtwork)] ? mainPageView.nlsArtwork : nil; }

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
    updateMusic();
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

    BOOL hiddenByContent = stuAutoHide && (musicActive || hasNotifications);
    if (enabled && stuEnabled && isOnLockscreen) {
        if (stu.superview != self) [self addSubview:stu];
        stu.frame = CGRectMake(0, self.bounds.size.height - 150, self.bounds.size.width, 150);
        [self sendSubviewToBack:stu];
        [stu setChevronStyle:stuChevron ? 1 : 0];

        // the shimmer color lives on a sublayer, guard it so a layout change can't crash springboard
        NSArray *outer = stu.layer.sublayers;
        if (outer.count > 0 && outer[0].sublayers.count > 2) {
            outer[0].sublayers[2].backgroundColor = [colorFromHex(stuColor) colorWithAlphaComponent:0.65].CGColor;
        }

        if (hiddenByContent) [stu hide]; else [stu show];
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
    if (notifEnabled && !notifBlur) {
        notifBlur = [[UIVisualEffectView alloc] initWithEffect:[UIBlurEffect effectWithStyle:UIBlurEffectStyleDark]];
        notifBlur.frame = self.view.bounds;
        notifBlur.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        notifBlur.userInteractionEnabled = NO;
        notifBlur.alpha = 0.0;
        [self.view insertSubview:notifBlur atIndex:0];
    }
}

- (void)viewWillAppear:(BOOL)animated {
    %orig;
    BOOL authed = [self respondsToSelector:@selector(isAuthenticated)] ? [self isAuthenticated] : NO;
    setOnLockscreen(!authed);
}
%end
%end

// ================================================================ notifications

static void updateNotifBlur(void) {
    [UIView animateWithDuration:0.2 animations:^{
        notifBlur.alpha = (enabled && notifEnabled && hasNotifications && !musicActive) ? 1.0 : 0.0;
    }];
}

%group Notifications

%hook NCNotificationShortLookView
%property (nonatomic, retain) UIView *nlsSeparator;

- (void)layoutSubviews {
    %orig;
    if (!(enabled && notifEnabled)) return;

    id owner = [self _viewControllerForAncestor];
    id delegate = [owner respondsToSelector:@selector(delegate)] ? [owner delegate] : nil;
    BOOL isBanner = delegate && [delegate isKindOfClass:%c(SBNotificationBannerDestination)];
    if (isBanner) return; // banners keep the ios 10 look

    // ios 9 look: no platter, thin separator under each notification
    UIView *bg = [self respondsToSelector:@selector(backgroundView)] ? [self valueForKey:@"backgroundView"] : nil;
    bg.hidden = YES;

    if (notifSeparators) {
        if (!self.nlsSeparator) {
            self.nlsSeparator = [UIView new];
            self.nlsSeparator.backgroundColor = [UIColor whiteColor];
            [self addSubview:self.nlsSeparator];
        }
        self.nlsSeparator.alpha = notifSeparatorAlpha;
        self.nlsSeparator.frame = CGRectMake(0, self.bounds.size.height - 0.5, self.bounds.size.width, 0.5);
    }
}
%end

// notification count -> blur the wallpaper + hide slide to unlock (like ios 9)
%hook NCNotificationPriorityList
- (unsigned long long)count {
    unsigned long long c = %orig;
    BOOL has = c > 0;
    if (has != hasNotifications) {
        hasNotifications = has;
        dispatch_async(dispatch_get_main_queue(), ^{
            updateNotifBlur();
            updateSTU();
        });
    }
    return c;
}
%end

%end // Notifications

// ================================================================ music

%group Music

%hook SBDashBoardMainPageView
%property (nonatomic, retain) UIVisualEffectView *nlsMusicBlur;
%property (nonatomic, retain) UIImageView *nlsArtwork;

- (void)layoutSubviews {
    %orig;
    mainPageView = self;
    if (!self.nlsMusicBlur) {
        self.nlsMusicBlur = [[UIVisualEffectView alloc] initWithEffect:[UIBlurEffect effectWithStyle:UIBlurEffectStyleDark]];
        self.nlsMusicBlur.userInteractionEnabled = NO;
        self.nlsMusicBlur.alpha = 0.0;

        self.nlsArtwork = [UIImageView new];
        self.nlsArtwork.contentMode = UIViewContentModeScaleAspectFill;
        self.nlsArtwork.clipsToBounds = YES;
        self.nlsArtwork.layer.cornerRadius = 5.0;
        [self.nlsMusicBlur.contentView addSubview:self.nlsArtwork];
    }
    [self nls_updateMusic];
}

%new
- (void)nls_updateMusic {
    UIVisualEffectView *blur = self.nlsMusicBlur;
    if (!blur) return;
    if (blur.superview != self) [self insertSubview:blur atIndex:0];

    CGRect b = self.bounds;
    blur.frame = b;
    // big artwork in the lower part, the stock ios 10 player (title + controls) stays up top
    CGFloat side = MIN(b.size.width - 80, b.size.height * 0.42);
    self.nlsArtwork.frame = CGRectMake((b.size.width - side) / 2, b.size.height - side - 120, side, side);

    BOOL show = enabled && musicEnabled && musicActive && isOnLockscreen;
    [UIView animateWithDuration:0.3 animations:^{ blur.alpha = show ? 1.0 : 0.0; }];
}
%end

%hook SBMediaController
- (void)setNowPlayingInfo:(id)info {
    %orig;
    MRMediaRemoteGetNowPlayingInfo(dispatch_get_main_queue(), ^(CFDictionaryRef information) {
        NSDictionary *dict = (__bridge NSDictionary *)information;
        NSData *art = dict[(__bridge NSString *)kMRMediaRemoteNowPlayingInfoArtworkData];
        musicActive = dict.count > 0;
        UIImageView *artView = artworkView();
        if (art && artView) {
            [UIView transitionWithView:artView duration:0.2
                               options:UIViewAnimationOptionTransitionCrossDissolve
                            animations:^{ artView.image = [UIImage imageWithData:art]; }
                            completion:nil];
        }
        updateMusic();
        updateSTU();
        updateNotifBlur();
    });
}
%end

%end // Music

// ================================================================ ios 9 lock sound

%group LockSound
%hook SBLockScreenManager
- (void)_playLockSound {
    if (!(enabled && lockSound)) { %orig; return; }
    static SystemSoundID sound = 0;
    if (!sound) {
        NSURL *url = [NSURL fileURLWithPath:@"/Library/Application Support/NineLS10/lock.caf"];
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
    updateNotifBlur();
}

static void displayChanged(CFNotificationCenterRef c, void *o, CFStringRef n, const void *obj, CFDictionaryRef info) {
    SBMediaController *mc = [%c(SBMediaController) sharedInstance];
    musicActive = [mc respondsToSelector:@selector(hasTrack)] && [mc hasTrack];
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
    if (has("NCNotificationShortLookView") && has("NCNotificationPriorityList")) %init(Notifications);
    if (has("SBMediaController") && has("SBDashBoardMainPageView")) %init(Music);
    if ([%c(SBLockScreenManager) instancesRespondToSelector:@selector(_playLockSound)]) %init(LockSound);

    CFNotificationCenterRef darwin = CFNotificationCenterGetDarwinNotifyCenter();
    CFNotificationCenterAddObserver(darwin, NULL, prefsChanged, CFSTR("com.jhu7600.ninels10/prefsupdated"), NULL, CFNotificationSuspensionBehaviorCoalesce);
    CFNotificationCenterAddObserver(darwin, NULL, displayChanged, CFSTR("com.apple.iokit.hid.displayStatus"), NULL, CFNotificationSuspensionBehaviorDeliverImmediately);
}
