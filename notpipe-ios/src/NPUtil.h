//
//  NPUtil.h
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//  Small shared helpers: image loading, number/time formatting, colors and the local watch history.
//

#import <UIKit/UIKit.h>

#define NP_RED [UIColor colorWithRed:0.86 green:0.0 blue:0.0 alpha:1.0]
#define NP_BG [UIColor colorWithWhite:0.07 alpha:1.0]
#define NP_GREY [UIColor colorWithWhite:0.62 alpha:1.0]

@interface NPUtil : NSObject

/** Loads an image into the view, cached in memory; a recycled cell's old request is ignored */
+ (void)loadImage:(NSString *)url into:(UIImageView *)view;

/** Best thumbnail for a video ID (straight from i.ytimg.com) */
+ (NSString *)thumbnailForVideo:(NSString *)videoId;

/** "4:05", "1:02:03" */
+ (NSString *)duration:(NSInteger)seconds;

/** "1.2M views" */
+ (NSString *)views:(long long)count;

/** Turns an Invidious relative URL into an absolute one */
+ (NSString *)absolute:(NSString *)url base:(NSString *)base;

/** Channel avatar URL from an Invidious "authorThumbnails" array */
+ (NSString *)avatarFrom:(NSArray *)thumbnails base:(NSString *)base;

/** Local watch history (newest first), stored as small dictionaries like the Invidious ones */
+ (NSArray *)history;
+ (void)addToHistory:(NSDictionary *)video;
+ (void)clearHistory;

/** Preferred playback quality: "360", "720" or "1080" */
+ (NSString *)quality;
+ (void)setQuality:(NSString *)quality;

@end
