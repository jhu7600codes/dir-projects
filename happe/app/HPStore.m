#import "HPStore.h"
#import "HPSubscription.h"
#import "VLESSConfig.h"
#include <sys/stat.h>

#define STATE_DIR  @"/var/mobile/Library/Happe"
#define STATE_PATH @"/var/mobile/Library/Happe/state.plist"

static NSString *HPStripFragment(NSString *link) {
    NSRange r = [link rangeOfString:@"#"];
    return r.location == NSNotFound ? link : [link substringToIndex:r.location];
}

@implementation HPServer

+ (instancetype)serverWithLink:(NSString *)link {
    HPServer *s = [[HPServer alloc] init];
    s.link = link;
    NSString *name = [VLESSConfig nameForLink:link];
    s.detail = [VLESSConfig detailForLink:link];
    s.name = name.length ? name : s.detail;
    return s;
}

@end

@implementation HPSub

- (NSString *)displayTitle {
    if (self.title.length) return self.title;
    NSString *host = [NSURL URLWithString:self.url].host;
    return host.length ? host : @"Subscription";
}

- (void)setLinks:(NSArray<NSString *> *)links {
    NSMutableArray *servers = [NSMutableArray array];
    NSUInteger skipped = 0;
    for (NSString *l in links) {
        if ([l.lowercaseString hasPrefix:@"vless://"] && [VLESSConfig configFromLink:l error:NULL]) [servers addObject:[HPServer serverWithLink:l]];
        else skipped++;
    }
    self.servers = servers;
    self.skipped = skipped;
}

- (NSDictionary *)plist {
    NSMutableDictionary *d = [NSMutableDictionary dictionary];
    d[@"id"] = self.subID;
    d[@"url"] = self.url;
    if (self.title) d[@"title"] = self.title;
    d[@"links"] = [self.servers valueForKey:@"link"] ?: @[];
    d[@"skipped"] = @(self.skipped);
    if (self.userinfo) d[@"userinfo"] = self.userinfo;
    if (self.updated) d[@"updated"] = self.updated;
    if (self.error) d[@"error"] = self.error;
    return d;
}

+ (instancetype)subWithPlist:(NSDictionary *)d {
    HPSub *s = [[HPSub alloc] init];
    s.subID = d[@"id"] ?: [NSUUID UUID].UUIDString;
    s.url = d[@"url"] ?: @"";
    s.title = d[@"title"];
    [s setLinks:d[@"links"] ?: @[]];
    s.skipped = [d[@"skipped"] unsignedIntegerValue];
    s.userinfo = d[@"userinfo"];
    s.updated = d[@"updated"];
    s.error = d[@"error"];
    return s;
}

@end

@implementation HPStore {
    NSMutableArray<HPSub *> *_subs;
    NSMutableArray<HPServer *> *_manual;
}

+ (instancetype)shared {
    static HPStore *s;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        s = [[HPStore alloc] init];
    });
    return s;
}

- (instancetype)init {
    if ((self = [super init])) {
        _subs = [NSMutableArray array];
        _manual = [NSMutableArray array];
        NSDictionary *d = [NSDictionary dictionaryWithContentsOfFile:STATE_PATH];
        for (NSDictionary *s in d[@"subs"]) [_subs addObject:[HPSub subWithPlist:s]];
        for (NSString *l in d[@"manual"]) [_manual addObject:[HPServer serverWithLink:l]];
        _selectedLink = d[@"selected"];
    }
    return self;
}

- (NSMutableArray<HPSub *> *)subs {
    return _subs;
}

- (NSMutableArray<HPServer *> *)manual {
    return _manual;
}

- (void)save {
    NSMutableDictionary *d = [NSMutableDictionary dictionary];
    d[@"subs"] = [_subs valueForKey:@"plist"];
    d[@"manual"] = [_manual valueForKey:@"link"];
    if (self.selectedLink) d[@"selected"] = self.selectedLink;
    [[NSFileManager defaultManager] createDirectoryAtPath:STATE_DIR withIntermediateDirectories:YES
                                               attributes:@{ NSFilePosixPermissions: @0700 } error:nil];
    NSData *data = [NSPropertyListSerialization dataWithPropertyList:d format:NSPropertyListBinaryFormat_v1_0 options:0 error:nil];
    [data writeToFile:STATE_PATH atomically:YES];
    chmod(STATE_PATH.fileSystemRepresentation, 0600);
}

- (NSArray<HPServer *> *)allServers {
    NSMutableArray *all = [NSMutableArray array];
    for (HPSub *s in _subs) [all addObjectsFromArray:s.servers];
    [all addObjectsFromArray:_manual];
    return all;
}

- (HPServer *)selectedServer {
    if (!self.selectedLink) return nil;
    NSArray *all = [self allServers];
    for (HPServer *s in all)
        if ([s.link isEqualToString:self.selectedLink]) return s;
    // a refreshed subscription may rename a server; match on everything but the name
    NSString *bare = HPStripFragment(self.selectedLink);
    for (HPServer *s in all)
        if ([HPStripFragment(s.link) isEqualToString:bare]) return s;
    return nil;
}

- (NSUInteger)addFromText:(NSString *)text newSubs:(NSArray<HPSub *> **)newSubs message:(NSString **)message {
    NSMutableArray *added = [NSMutableArray array];
    NSUInteger links = 0, unsupported = 0, dupes = 0, encrypted = 0;
    for (NSString *raw in [text componentsSeparatedByCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]]) {
        if (!raw.length) continue;
        NSString *token = raw;
        // happ deep links / qr codes: happ://add/<subscription url>
        if ([token.lowercaseString hasPrefix:@"happ://add/"]) {
            token = [token substringFromIndex:11];
            if ([token rangeOfString:@"://"].location == NSNotFound) token = [token stringByRemovingPercentEncoding] ?: token;
        } else if ([token.lowercaseString hasPrefix:@"happ://crypt"]) {
            // encrypted with a key only the happ app has
            encrypted++;
            continue;
        }
        NSString *lower = token.lowercaseString;
        if ([lower hasPrefix:@"https://"] || [lower hasPrefix:@"http://"]) {
            if ([[_subs valueForKey:@"url"] containsObject:token]) { dupes++; continue; }
            HPSub *s = [[HPSub alloc] init];
            s.subID = [NSUUID UUID].UUIDString;
            s.url = token;
            s.servers = @[];
            [_subs addObject:s];
            [added addObject:s];
        } else if ([lower hasPrefix:@"vless://"]) {
            if (![VLESSConfig configFromLink:token error:NULL]) { unsupported++; continue; }
            if ([[_manual valueForKey:@"link"] containsObject:token]) { dupes++; continue; }
            [_manual addObject:[HPServer serverWithLink:token]];
            links++;
        } else if ([lower rangeOfString:@"://"].location != NSNotFound) {
            unsupported++;
        }
    }
    if (!self.selectedLink && _manual.count) self.selectedLink = _manual.firstObject.link;
    [self save];
    if (newSubs) *newSubs = added;
    NSMutableArray *notes = [NSMutableArray array];
    if (encrypted) [notes addObject:@"encrypted Happ links (happ://crypt…) can only be opened by the Happ app; ask your provider for the plain https:// subscription link"];
    if (unsupported) [notes addObject:[NSString stringWithFormat:@"%lu skipped (only vless:// and subscription URLs work)", (unsigned long)unsupported]];
    if (dupes) [notes addObject:[NSString stringWithFormat:@"%lu already added", (unsigned long)dupes]];
    if (message) *message = notes.count ? [notes componentsJoinedByString:@", "] : nil;
    return added.count + links;
}

- (void)removeSub:(HPSub *)sub {
    [_subs removeObject:sub];
    if (!self.selectedServer) self.selectedLink = nil;
    [self save];
}

- (void)removeManual:(HPServer *)server {
    [_manual removeObject:server];
    if (!self.selectedServer) self.selectedLink = nil;
    [self save];
}

- (void)refresh:(HPSub *)sub done:(void (^)(void))done {
    sub.refreshing = YES;
    [HPSubscription fetch:sub.url completion:^(NSDictionary *info, NSString *error) {
        sub.refreshing = NO;
        if (info) {
            [sub setLinks:info[@"links"]];
            if (info[@"title"]) sub.title = info[@"title"];
            sub.userinfo = info[@"userinfo"];
            sub.updated = [NSDate date];
            sub.error = sub.servers.count ? nil : @"No vless servers in this subscription.";
        } else {
            sub.error = error;
        }
        HPServer *sel = self.selectedServer;
        if (sel) self.selectedLink = sel.link; // follow renames
        else if (!self.selectedLink) self.selectedLink = ((HPServer *)[self allServers].firstObject).link;
        [self save];
        if (done) done();
    }];
}

@end
