//
//  NPNet.h
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//  Networking: the instance lists (same notPipe.json as Android) and racing several
//  Invidious instances at once so a dead one never holds the app up.
//

#import <Foundation/Foundation.h>

typedef void (^NPJSONBlock)(id json, NSString *base, NSError *error);

@interface NPNet : NSObject

/** Downloads and parses JSON; the block runs on the main queue */
+ (void)getJSON:(NSString *)url timeout:(NSTimeInterval)timeout completion:(void (^)(id json, NSError *error))completion;

/** Asks several Invidious instances at once; the first good answer wins */
+ (void)invidious:(NSString *)path completion:(NPJSONBlock)completion;

/** Refreshes the instance lists from the notPipe.json the Android app uses */
+ (void)refreshInstances;

+ (NSArray *)invidiousInstances;
+ (NSArray *)yt2009Instances;

/**
 * Asks a yt2009 instance for an HD file and returns where it redirects (the finished mp4).
 * yt2009 downloads and processes the video first, so this can take a while.
 */
+ (void)resolveHD:(NSString *)videoId quality:(NSString *)quality completion:(void (^)(NSString *url, NSError *error))completion;

@end
