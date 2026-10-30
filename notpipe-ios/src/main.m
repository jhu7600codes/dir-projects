//
//  main.m
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//  App setup: dark tab bar with Home, Search and Library.
//

#import <AVFoundation/AVFoundation.h>
#import <UIKit/UIKit.h>
#import "NPListViewController.h"
#import "NPNet.h"
#import "NPUtil.h"

@interface NPAppDelegate : UIResponder <UIApplicationDelegate>
@property (nonatomic, strong) UIWindow *window;
@end

@implementation NPAppDelegate

- (UINavigationController *)navWith:(UIViewController *)root {
    UINavigationController *nav = [[UINavigationController alloc] initWithRootViewController:root];
    nav.navigationBar.barStyle = UIBarStyleBlack;
    nav.navigationBar.tintColor = [UIColor whiteColor];
    return nav;
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayback error:nil];
    [NPNet refreshInstances];

    UITabBarController *tabs = [UITabBarController new];
    tabs.tabBar.barStyle = UIBarStyleBlack;
    tabs.tabBar.tintColor = NP_RED;
    tabs.viewControllers = @[
        [self navWith:[[NPListViewController alloc] initWithMode:NPListModeHome]],
        [self navWith:[[NPListViewController alloc] initWithMode:NPListModeSearch]],
        [self navWith:[[NPListViewController alloc] initWithMode:NPListModeHistory]]
    ];

    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    self.window.backgroundColor = NP_BG;
    self.window.rootViewController = tabs;
    [self.window makeKeyAndVisible];
    return YES;
}

@end

int main(int argc, char *argv[]) {
    @autoreleasepool {
        return UIApplicationMain(argc, argv, nil, NSStringFromClass([NPAppDelegate class]));
    }
}
