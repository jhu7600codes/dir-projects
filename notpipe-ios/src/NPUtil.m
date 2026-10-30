//
//  NPUtil.m
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//

#import "NPUtil.h"
#import <objc/runtime.h>

static char kImageURLKey;

@implementation NPUtil

+ (NSCache *)cache {
    static NSCache *cache;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cache = [NSCache new];
        cache.countLimit = 150;
    });
    return cache;
}

+ (void)loadImage:(NSString *)url into:(UIImageView *)view {
    objc_setAssociatedObject(view, &kImageURLKey, url, OBJC_ASSOCIATION_COPY_NONATOMIC);
    view.image = nil;
    if (!url.length) return;
    UIImage *cached = [[self cache] objectForKey:url];
    if (cached) {
        view.image = cached;
        return;
    }
    NSURL *u = [NSURL URLWithString:url];
    if (!u) return;
    [[[NSURLSession sharedSession] dataTaskWithURL:u completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        UIImage *image = data ? [UIImage imageWithData:data] : nil;
        if (!image) return;
        dispatch_async(dispatch_get_main_queue(), ^{
            [[self cache] setObject:image forKey:url];
            NSString *wanted = objc_getAssociatedObject(view, &kImageURLKey);
            if ([wanted isEqualToString:url]) view.image = image;
        });
    }] resume];
}

+ (NSString *)thumbnailForVideo:(NSString *)videoId {
    return [NSString stringWithFormat:@"https://i.ytimg.com/vi/%@/hqdefault.jpg", videoId];
}

+ (NSString *)duration:(NSInteger)seconds {
    if (seconds <= 0) return @"";
    NSInteger h = seconds / 3600, m = (seconds / 60) % 60, s = seconds % 60;
    if (h > 0) return [NSString stringWithFormat:@"%ld:%02ld:%02ld", (long)h, (long)m, (long)s];
    return [NSString stringWithFormat:@"%ld:%02ld", (long)m, (long)s];
}

+ (NSString *)views:(long long)count {
    if (count <= 0) return @"";
    NSString *number;
    if (count >= 1000000000) number = [NSString stringWithFormat:@"%.1fB", count / 1e9];
    else if (count >= 1000000) number = [NSString stringWithFormat:@"%.1fM", count / 1e6];
    else if (count >= 1000) number = [NSString stringWithFormat:@"%.1fK", count / 1e3];
    else number = [NSString stringWithFormat:@"%lld", count];
    number = [number stringByReplacingOccurrencesOfString:@".0" withString:@""];
    return [number stringByAppendingString:@" views"];
}

+ (NSString *)absolute:(NSString *)url base:(NSString *)base {
    if (![url isKindOfClass:[NSString class]] || !url.length) return @"";
    if ([url hasPrefix:@"//"]) return [@"https:" stringByAppendingString:url];
    if ([url hasPrefix:@"/"]) return [base stringByAppendingString:url];
    return url;
}

+ (NSString *)avatarFrom:(NSArray *)thumbnails base:(NSString *)base {
    if (![thumbnails isKindOfClass:[NSArray class]] || !thumbnails.count) return @"";
    NSDictionary *best = thumbnails.firstObject;
    for (NSDictionary *t in thumbnails) {
        if ([t[@"width"] integerValue] >= 88) {
            best = t;
            break;
        }
    }
    NSString *url = [self absolute:best[@"url"] base:base];
    // Instance image proxies often refuse avatars, the ggpht original works
    NSRange proxy = [url rangeOfString:@"/ggpht/"];
    if (proxy.location != NSNotFound) url = [@"https://yt3.ggpht.com/" stringByAppendingString:[url substringFromIndex:NSMaxRange(proxy)]];
    return url;
}

#pragma mark - History

+ (NSArray *)history {
    return [[NSUserDefaults standardUserDefaults] arrayForKey:@"history"] ?: [NSArray array];
}

+ (void)addToHistory:(NSDictionary *)video {
    NSString *videoId = video[@"videoId"];
    if (!videoId.length) return;
    NSMutableDictionary *entry = [NSMutableDictionary new];
    for (NSString *key in @[@"videoId", @"title", @"author", @"authorId", @"lengthSeconds"]) {
        if (video[key] && video[key] != [NSNull null]) entry[key] = video[key];
    }
    NSMutableArray *list = [[self history] mutableCopy];
    for (NSInteger i = list.count - 1; i >= 0; i--) {
        if ([list[i][@"videoId"] isEqualToString:videoId]) [list removeObjectAtIndex:i];
    }
    [list insertObject:entry atIndex:0];
    while (list.count > 200) [list removeLastObject];
    [[NSUserDefaults standardUserDefaults] setObject:list forKey:@"history"];
}

+ (void)clearHistory {
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"history"];
}

#pragma mark - Quality

+ (NSString *)quality {
    return [[NSUserDefaults standardUserDefaults] stringForKey:@"quality"] ?: @"360";
}

+ (void)setQuality:(NSString *)quality {
    [[NSUserDefaults standardUserDefaults] setObject:quality forKey:@"quality"];
}

@end
