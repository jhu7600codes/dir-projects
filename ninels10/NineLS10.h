// private ios 10 interfaces used by NineLS10
#import <UIKit/UIKit.h>
#import <AudioToolbox/AudioServices.h>
#import <objc/runtime.h>

// --- uikit ---
@interface _UIGlintyStringView : UIView
- (instancetype)initWithText:(NSString *)text andFont:(UIFont *)font;
- (void)setText:(NSString *)text;
- (void)setChevronStyle:(long long)style;
- (void)setNeedsTextUpdate:(BOOL)needs;
- (void)updateText;
- (void)show;
- (void)hide;
@end

@interface UIView (Private)
- (UIViewController *)_viewControllerForAncestor;
@end

// --- springboard (ios 10) ---
@interface SBLockScreenManager : NSObject
+ (instancetype)sharedInstance;
- (void)lockScreenViewControllerRequestsUnlock;
- (void)unlockUIFromSource:(int)source withOptions:(id)options;
@end

@interface SBDashBoardViewController : UIViewController
- (BOOL)isAuthenticated;
@end

@interface SBDashBoardMainPageView : UIView
@property (nonatomic, retain) _UIGlintyStringView *stuView;
- (void)nls_updateSTU;
// music
@property (nonatomic, retain) UIVisualEffectView *nlsMusicBlur;
@property (nonatomic, retain) UIImageView *nlsArtwork;
- (void)nls_updateMusic;
@end

@interface SBPagedScrollView : UIScrollView
- (void)scrollToPageAtIndex:(unsigned long long)index animated:(BOOL)animated;
@end

@interface SBDashBoardPageViewController : UIViewController
@end
@interface SBDashBoardTodayPageViewController : SBDashBoardPageViewController
@end
@interface SBDashBoardFixedFooterViewController : UIViewController
@end

@interface SBUICallToActionLabel : UIView
- (void)setText:(NSString *)text forLanguage:(NSString *)lang animated:(BOOL)animated;
@end

@interface SBMediaController : NSObject
+ (instancetype)sharedInstance;
- (BOOL)hasTrack;
@end

// --- notifications (UserNotificationsUIKit, ios 10) ---
@interface NCNotificationShortLookView : UIView
@property (nonatomic, retain) UIView *nlsSeparator;
@end

@interface NCNotificationShortLookViewController : UIViewController
@property (nonatomic, weak) id delegate;
@end

@interface NCNotificationPriorityList : NSObject
- (unsigned long long)count;
@end

// --- mediaremote ---
typedef void (^MRMediaRemoteGetNowPlayingInfoCompletion)(CFDictionaryRef information);
extern void MRMediaRemoteGetNowPlayingInfo(dispatch_queue_t queue, MRMediaRemoteGetNowPlayingInfoCompletion completion);
extern CFStringRef kMRMediaRemoteNowPlayingInfoArtworkData;
