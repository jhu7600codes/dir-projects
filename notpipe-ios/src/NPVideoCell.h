//
//  NPVideoCell.h
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//  Big YouTube-style video card: 16:9 thumbnail with duration, then title and channel/views.
//

#import <UIKit/UIKit.h>

@interface NPVideoCell : UITableViewCell

/** Fills the card from an Invidious video dictionary */
- (void)showVideo:(NSDictionary *)video;

/** Row height for a table of the given width */
+ (CGFloat)heightForWidth:(CGFloat)width;

@end
