//
//  NPNet.m
//  notPipe for iOS
//
//  Created by Claude on 30.10.2026.
//

#import "NPNet.h"

static NSString * const kInstancesURL = @"http://144.31.189.129/notPipe.json";
static NSString * const kInvidiousKey = @"invidious_instances";
static NSString * const kYt2009Key = @"yt2009_instances";
static NSString * const kScoresKey = @"instance_scores";
static const NSUInteger kRaceWidth = 4;

#pragma mark - Redirect catcher

/** Stops at the first redirect and remembers where it pointed */
@interface NPRedirectCatcher : NSObject <NSURLSessionTaskDelegate>
@property (nonatomic, copy) NSString *location;
@end

@implementation NPRedirectCatcher
- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task
willPerformHTTPRedirection:(NSHTTPURLResponse *)response newRequest:(NSURLRequest *)request
 completionHandler:(void (^)(NSURLRequest *))completionHandler {
    self.location = request.URL.absoluteString;
    completionHandler(nil);
}
@end

#pragma mark - Race state

/** Keeps track of one race: which instances are left and whether someone already won */
@interface NPRace : NSObject
@property (nonatomic, strong) NSMutableArray *pending;
@property (nonatomic, copy) NSString *path;
@property (nonatomic, copy) NPJSONBlock completion;
@property (nonatomic) NSInteger running;
@property (nonatomic) BOOL done;
@property (nonatomic, strong) NSError *lastError;
@end

@implementation NPRace
@end

@implementation NPNet

+ (NSURLSession *)session {
    static NSURLSession *session;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        NSURLSessionConfiguration *config = [NSURLSessionConfiguration defaultSessionConfiguration];
        config.HTTPAdditionalHeaders = @{@"User-Agent": @"notPipe-iOS/0.1"};
        config.HTTPMaximumConnectionsPerHost = 6;
        session = [NSURLSession sessionWithConfiguration:config];
    });
    return session;
}

+ (void)getJSON:(NSString *)url timeout:(NSTimeInterval)timeout completion:(void (^)(id, NSError *))completion {
    NSURL *u = [NSURL URLWithString:url];
    if (!u) {
        completion(nil, [NSError errorWithDomain:@"notPipe" code:1 userInfo:@{NSLocalizedDescriptionKey: @"Bad URL"}]);
        return;
    }
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:u];
    request.timeoutInterval = timeout;
    [[[self session] dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        id json = nil;
        NSInteger status = [response isKindOfClass:[NSHTTPURLResponse class]] ? ((NSHTTPURLResponse *)response).statusCode : 0;
        if (!error && status >= 400) {
            error = [NSError errorWithDomain:@"notPipe" code:status userInfo:@{NSLocalizedDescriptionKey:
                    [NSString stringWithFormat:@"HTTP %ld", (long)status]}];
        }
        if (!error && data) {
            json = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            completion(json, error);
        });
    }] resume];
}

#pragma mark - Instances

+ (NSArray *)invidiousInstances {
    NSArray *saved = [[NSUserDefaults standardUserDefaults] arrayForKey:kInvidiousKey];
    if (saved.count) return saved;
    return @[@"http://invid.ddnsgeek.com", @"http://34.80.232.20", @"http://61.221.6.111",
             @"http://www.invidious.frantisekfiala.eu"];
}

+ (NSArray *)yt2009Instances {
    NSArray *saved = [[NSUserDefaults standardUserDefaults] arrayForKey:kYt2009Key];
    if (saved.count) return saved;
    return @[@"http://176.100.39.186:8888", @"http://87.106.23.41", @"http://femboy.email:730"];
}

+ (void)refreshInstances {
    [self getJSON:kInstancesURL timeout:15 completion:^(id json, NSError *error) {
        if (![json isKindOfClass:[NSDictionary class]]) return;
        NSArray *invidious = json[@"invidious"], *yt2009 = json[@"yt2009"];
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        if ([invidious isKindOfClass:[NSArray class]] && invidious.count) [defaults setObject:invidious forKey:kInvidiousKey];
        if ([yt2009 isKindOfClass:[NSArray class]] && yt2009.count) [defaults setObject:yt2009 forKey:kYt2009Key];
        [defaults synchronize];
    }];
}

/** Instances that answered before go first, the rest are shuffled to spread the load */
+ (NSMutableArray *)orderedInstances:(NSArray *)list {
    NSMutableArray *shuffled = [list mutableCopy];
    for (NSUInteger i = shuffled.count; i > 1; i--) {
        [shuffled exchangeObjectAtIndex:i - 1 withObjectAtIndex:arc4random_uniform((uint32_t)i)];
    }
    NSDictionary *scores = [[NSUserDefaults standardUserDefaults] dictionaryForKey:kScoresKey] ?: [NSDictionary dictionary];
    [shuffled sortWithOptions:NSSortStable usingComparator:^NSComparisonResult(NSString *a, NSString *b) {
        NSInteger sa = [scores[a] integerValue], sb = [scores[b] integerValue];
        return sa > sb ? NSOrderedAscending : (sa < sb ? NSOrderedDescending : NSOrderedSame);
    }];
    return shuffled;
}

+ (void)addScore:(NSInteger)delta toInstance:(NSString *)base {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSMutableDictionary *scores = [[defaults dictionaryForKey:kScoresKey] mutableCopy] ?: [NSMutableDictionary new];
    NSInteger value = MAX(-5, MIN(10, [scores[base] integerValue] + delta));
    scores[base] = @(value);
    [defaults setObject:scores forKey:kScoresKey];
}

#pragma mark - Racing

+ (void)invidious:(NSString *)path completion:(NPJSONBlock)completion {
    NPRace *race = [NPRace new];
    race.pending = [self orderedInstances:[self invidiousInstances]];
    race.path = path;
    race.completion = completion;
    for (NSUInteger i = 0; i < kRaceWidth; i++) [self launchNext:race];
    if (race.running == 0) {
        completion(nil, nil, [NSError errorWithDomain:@"notPipe" code:2 userInfo:@{NSLocalizedDescriptionKey: @"No instances"}]);
    }
}

+ (void)launchNext:(NPRace *)race {
    if (race.done || race.pending.count == 0) return;
    NSString *base = race.pending.firstObject;
    [race.pending removeObjectAtIndex:0];
    race.running++;
    [self getJSON:[base stringByAppendingString:race.path] timeout:10 completion:^(id json, NSError *error) {
        race.running--;
        if (race.done) return;
        BOOL empty = !json || ([json isKindOfClass:[NSArray class]] && [json count] == 0)
                || ([json isKindOfClass:[NSDictionary class]] && json[@"error"]);
        if (!error && !empty) {
            race.done = YES;
            [self addScore:1 toInstance:base];
            race.completion(json, base, nil);
            return;
        }
        [self addScore:-1 toInstance:base];
        race.lastError = error ?: [NSError errorWithDomain:@"notPipe" code:3 userInfo:@{NSLocalizedDescriptionKey: @"Empty answer"}];
        [self launchNext:race];
        if (race.running == 0 && race.pending.count == 0) {
            race.done = YES;
            race.completion(nil, nil, race.lastError);
        }
    }];
}

#pragma mark - HD

+ (void)resolveHD:(NSString *)videoId quality:(NSString *)quality completion:(void (^)(NSString *, NSError *))completion {
    NSMutableArray *instances = [self orderedInstances:[self yt2009Instances]];
    [self resolveHD:videoId quality:quality instances:instances completion:completion];
}

+ (void)resolveHD:(NSString *)videoId quality:(NSString *)quality instances:(NSMutableArray *)instances
       completion:(void (^)(NSString *, NSError *))completion {
    if (instances.count == 0) {
        completion(nil, [NSError errorWithDomain:@"notPipe" code:4 userInfo:@{NSLocalizedDescriptionKey: @"HD not available"}]);
        return;
    }
    NSString *base = instances.firstObject;
    [instances removeObjectAtIndex:0];
    NSString *path = [quality isEqualToString:@"1080"]
            ? [NSString stringWithFormat:@"/exp_hd?video_id=%@&fhd=1", videoId]
            : [quality isEqualToString:@"480"] ? [NSString stringWithFormat:@"/get_480?video_id=%@", videoId]
            : [NSString stringWithFormat:@"/exp_hd?video_id=%@", videoId];
    NPRedirectCatcher *catcher = [NPRedirectCatcher new];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]
                                                          delegate:catcher delegateQueue:nil];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:[base stringByAppendingString:path]]];
    request.timeoutInterval = 240;
    [[session dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        [session finishTasksAndInvalidate];
        NSString *location = catcher.location;
        dispatch_async(dispatch_get_main_queue(), ^{
            if (location.length && [location rangeOfString:@"/assets/"].location != NSNotFound) {
                [self addScore:1 toInstance:base];
                completion(location, nil);
            } else {
                [self addScore:-1 toInstance:base];
                [self resolveHD:videoId quality:quality instances:instances completion:completion];
            }
        });
    }] resume];
}

@end
