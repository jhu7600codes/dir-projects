//
//  NPListViewController.h
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//  A list of big video cards: the Home feed, search results or the watch history.
//

#import <UIKit/UIKit.h>

typedef NS_ENUM(NSInteger, NPListMode) {
    NPListModeHome,
    NPListModeSearch,
    NPListModeHistory
};

@interface NPListViewController : UITableViewController

- (instancetype)initWithMode:(NPListMode)mode;

@end
