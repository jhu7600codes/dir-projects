//
//  NPPlayerViewController.h
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//  The watch page: player on top, then title, channel and Related / Comments.
//

#import <UIKit/UIKit.h>

@interface NPPlayerViewController : UIViewController

/** @param video an Invidious video dictionary (at least "videoId") */
- (instancetype)initWithVideo:(NSDictionary *)video;

@end
