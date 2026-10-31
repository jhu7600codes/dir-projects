// SlideToUnlock - ios 9 style slide to unlock for ios 10
// the lock screen content follows your finger 1:1 to the right,
// let go past the threshold (or flick) and it unlocks / shows the passcode.

#import <UIKit/UIKit.h>

@interface SBDashBoardViewController : UIViewController
- (NSArray *)stu_scrollViewsIn:(UIView *)root;
- (void)stu_handlePan:(UIPanGestureRecognizer *)pan;
@end

@interface SBLockScreenManager : NSObject
+ (instancetype)sharedInstance;
- (void)lockScreenViewControllerRequestsUnlock;
- (void)unlockUIFromSource:(int)source withOptions:(id)options;
@end

@interface SBUICallToActionLabel : UIView
- (void)setText:(NSString *)text forLanguage:(NSString *)lang animated:(BOOL)animated;
@end

static const CGFloat kUnlockFraction = 0.35;   // drag this much of the width...
static const CGFloat kUnlockVelocity = 700.0;  // ...or flick at least this fast

@interface STUPanDelegate : NSObject <UIGestureRecognizerDelegate>
@end

@implementation STUPanDelegate
// only start on a mostly-horizontal swipe to the right, so swipe left (camera)
// and vertical swipes (notification center / control center) keep working
- (BOOL)gestureRecognizerShouldBegin:(UIPanGestureRecognizer *)pan {
    CGPoint v = [pan velocityInView:pan.view];
    return v.x > 0 && fabs(v.x) > fabs(v.y) * 1.5;
}
- (BOOL)gestureRecognizer:(UIGestureRecognizer *)g shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)o {
    return NO;
}
@end

static STUPanDelegate *panDelegate;

static void stu_unlock(void) {
    SBLockScreenManager *mgr = [%c(SBLockScreenManager) sharedInstance];
    if ([mgr respondsToSelector:@selector(lockScreenViewControllerRequestsUnlock)]) {
        [mgr lockScreenViewControllerRequestsUnlock];
    } else if ([mgr respondsToSelector:@selector(unlockUIFromSource:withOptions:)]) {
        [mgr unlockUIFromSource:0 withOptions:nil];
    }
}

static void stu_reset(UIView *v, BOOL animated) {
    void (^b)(void) = ^{ v.transform = CGAffineTransformIdentity; v.alpha = 1.0; };
    if (!animated) { b(); return; }
    [UIView animateWithDuration:0.45 delay:0 usingSpringWithDamping:0.8
          initialSpringVelocity:0 options:UIViewAnimationOptionBeginFromCurrentState
                     animations:b completion:nil];
}

%hook SBDashBoardViewController

- (void)viewDidLoad {
    %orig;
    if (!panDelegate) panDelegate = [STUPanDelegate new];

    UIPanGestureRecognizer *pan = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(stu_handlePan:)];
    pan.delegate = panDelegate;
    pan.maximumNumberOfTouches = 1;
    [self.view addGestureRecognizer:pan];

    // make the page scroll view (today view / camera) wait for us to fail first
    for (UIScrollView *sv in [self stu_scrollViewsIn:self.view]) {
        [sv.panGestureRecognizer requireGestureRecognizerToFail:pan];
    }
}

- (void)viewWillAppear:(BOOL)animated {
    %orig;
    stu_reset(self.view, NO); // back to normal every time the lock screen shows up
}

%new
- (NSArray *)stu_scrollViewsIn:(UIView *)root {
    NSMutableArray *out = [NSMutableArray array];
    for (UIView *sub in root.subviews) {
        if ([sub isKindOfClass:[UIScrollView class]]) [out addObject:sub];
        [out addObjectsFromArray:[self stu_scrollViewsIn:sub]];
    }
    return out;
}

%new
- (void)stu_handlePan:(UIPanGestureRecognizer *)pan {
    UIView *v = self.view;
    CGFloat w = v.bounds.size.width;
    CGFloat x = MAX(0, [pan translationInView:v].x); // 1:1, never past the left edge

    switch (pan.state) {
        case UIGestureRecognizerStateChanged:
            v.transform = CGAffineTransformMakeTranslation(x, 0);
            v.alpha = 1.0 - (x / w) * 0.6;
            break;

        case UIGestureRecognizerStateEnded: {
            CGFloat vx = [pan velocityInView:v].x;
            if (x > w * kUnlockFraction || vx > kUnlockVelocity) {
                NSTimeInterval d = MIN(0.3, MAX(0.12, (w - x) / MAX(vx, 1000)));
                [UIView animateWithDuration:d delay:0 options:UIViewAnimationOptionCurveEaseOut animations:^{
                    v.transform = CGAffineTransformMakeTranslation(w, 0);
                    v.alpha = 0.0;
                } completion:^(BOOL done) {
                    stu_unlock();
                    // if a passcode page popped up instead, bring the content back
                    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.4 * NSEC_PER_SEC)),
                                   dispatch_get_main_queue(), ^{ stu_reset(v, NO); });
                }];
            } else {
                stu_reset(v, YES);
            }
            break;
        }

        case UIGestureRecognizerStateCancelled:
        case UIGestureRecognizerStateFailed:
            stu_reset(v, YES);
            break;

        default: break;
    }
}

%end

// "press home to unlock" -> "slide to unlock"
%hook SBUICallToActionLabel
- (void)setText:(NSString *)text forLanguage:(NSString *)lang animated:(BOOL)animated {
    %orig(text.length ? @"slide to unlock" : text, lang, animated);
}
%end
