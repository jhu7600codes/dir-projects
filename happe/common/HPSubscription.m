#import "HPSubscription.h"

static NSString *HPTrim(NSString *s) {
    return [s stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
}

static NSData *HPBase64Decode(NSString *s) {
    NSMutableString *b = [[[s componentsSeparatedByCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]]
        componentsJoinedByString:@""] mutableCopy];
    [b replaceOccurrencesOfString:@"-" withString:@"+" options:0 range:NSMakeRange(0, b.length)];
    [b replaceOccurrencesOfString:@"_" withString:@"/" options:0 range:NSMakeRange(0, b.length)];
    while (b.length % 4) [b appendString:@"="];
    return [[NSData alloc] initWithBase64EncodedString:b options:0];
}

@implementation HPSubscription

+ (NSArray<NSString *> *)linksInText:(NSString *)text {
    NSMutableArray *out = [NSMutableArray array];
    for (NSString *raw in [text componentsSeparatedByCharactersInSet:[NSCharacterSet newlineCharacterSet]]) {
        NSString *line = HPTrim(raw);
        NSRange r = [line rangeOfString:@"://"];
        if (r.location == NSNotFound || r.location == 0 || r.location > 16) continue;
        [out addObject:line];
    }
    return out;
}

+ (NSArray<NSString *> *)linksFromBody:(NSData *)body {
    NSString *text = [[NSString alloc] initWithData:body encoding:NSUTF8StringEncoding];
    if (!text) return @[];
    NSArray *plain = [self linksInText:text];
    if (plain.count) return plain;
    NSData *decoded = HPBase64Decode(text);
    if (!decoded) return @[];
    NSString *inner = [[NSString alloc] initWithData:decoded encoding:NSUTF8StringEncoding];
    return inner ? [self linksInText:inner] : @[];
}

+ (NSString *)titleFromHeader:(NSString *)header {
    NSString *h = HPTrim(header ?: @"");
    if ([h.lowercaseString hasPrefix:@"base64:"]) {
        NSData *d = HPBase64Decode([h substringFromIndex:7]);
        h = d ? HPTrim([[NSString alloc] initWithData:d encoding:NSUTF8StringEncoding] ?: @"") : @"";
    }
    return h.length ? h : nil;
}

+ (NSDictionary<NSString *, NSNumber *> *)userInfoFromHeader:(NSString *)header {
    NSMutableDictionary *out = [NSMutableDictionary dictionary];
    for (NSString *part in [header ?: @"" componentsSeparatedByString:@";"]) {
        NSArray *kv = [part componentsSeparatedByString:@"="];
        if (kv.count != 2) continue;
        NSString *k = HPTrim(kv[0]).lowercaseString;
        NSString *v = HPTrim(kv[1]);
        if (k.length && v.length) out[k] = @(strtoll(v.UTF8String, NULL, 10));
    }
    return out;
}

// some panels only answer apps they know; try these in order
// some panels only answer apps they know (allvpn: happ and karing only);
// try these in order. happ goes first since it gets plain share links.
+ (NSArray<NSString *> *)userAgents {
    return @[ @"Happ/3.5.0", @"v2rayN/6.0", @"v2rayNG/1.8.0", @"karing/1.1.2.0", @"Streisand/1.0" ];
}

// a stable per-install device id, sent like happ's x-hwid (panels with a
// device limit count devices by it)
+ (NSString *)hwid {
    NSUserDefaults *d = [NSUserDefaults standardUserDefaults];
    NSString *h = [d stringForKey:@"hwid"];
    if (!h.length) {
        h = [[NSUUID UUID].UUIDString.lowercaseString stringByReplacingOccurrencesOfString:@"-" withString:@""];
        [d setObject:h forKey:@"hwid"];
        [d synchronize];
    }
    return h;
}

+ (void)fetch:(NSString *)url completion:(void (^)(NSDictionary *, NSString *))completion {
    NSURL *u = [NSURL URLWithString:HPTrim(url)];
    if (!u || !u.host) {
        completion(nil, @"Not a valid URL.");
        return;
    }
    [self fetch:u agent:0 firstError:nil completion:completion];
}

+ (void)fetch:(NSURL *)u agent:(NSUInteger)agent firstError:(NSString *)firstError
   completion:(void (^)(NSDictionary *, NSString *))completion {
    NSMutableURLRequest *req = [NSMutableURLRequest requestWithURL:u cachePolicy:NSURLRequestReloadIgnoringLocalCacheData
                                                   timeoutInterval:20];
    NSString *ua = self.userAgents[agent];
    [req setValue:ua forHTTPHeaderField:@"User-Agent"];
    if ([ua hasPrefix:@"Happ/"]) {
        [req setValue:self.hwid forHTTPHeaderField:@"X-HWID"];
        [req setValue:@"iOS" forHTTPHeaderField:@"X-Device-OS"];
        [req setValue:[[NSProcessInfo processInfo] operatingSystemVersionString] forHTTPHeaderField:@"X-Ver-OS"];
        [req setValue:@"iPhone" forHTTPHeaderField:@"X-Device-Model"];
    }
    [[[NSURLSession sharedSession] dataTaskWithRequest:req completionHandler:^(NSData *data, NSURLResponse *resp, NSError *err) {
        NSDictionary *info = nil;
        NSString *error = nil;
        BOOL retry = NO;
        NSHTTPURLResponse *http = [resp isKindOfClass:[NSHTTPURLResponse class]] ? (NSHTTPURLResponse *)resp : nil;
        if (err) {
            // a timeout can be a panel ignoring an app it doesn't know, so
            // try the next user agent; other network errors won't change
            retry = err.code == NSURLErrorTimedOut;
            error = [NSString stringWithFormat:@"%@ (%@ %ld)", err.localizedDescription,
                     [err.domain isEqualToString:NSURLErrorDomain] ? @"url" : err.domain, (long)err.code];
        } else if (http && (http.statusCode < 200 || http.statusCode >= 300)) {
            error = [NSString stringWithFormat:@"Server answered %ld.", (long)http.statusCode];
            retry = YES;
        } else {
            NSArray *links = [self linksFromBody:data ?: [NSData data]];
            if (!links.count) {
                error = @"No servers found in this subscription.";
                retry = YES;
            } else {
                NSDictionary *headers = http.allHeaderFields;
                NSString *title = nil, *userinfo = nil;
                for (NSString *k in headers) {
                    if ([k caseInsensitiveCompare:@"profile-title"] == NSOrderedSame) title = [self titleFromHeader:headers[k]];
                    if ([k caseInsensitiveCompare:@"subscription-userinfo"] == NSOrderedSame) userinfo = headers[k];
                }
                NSMutableDictionary *m = [NSMutableDictionary dictionary];
                m[@"links"] = links;
                if (title) m[@"title"] = title;
                if (userinfo) m[@"userinfo"] = [self userInfoFromHeader:userinfo];
                info = m;
            }
        }
        if (!info && retry && agent + 1 < self.userAgents.count) {
            [self fetch:u agent:agent + 1 firstError:firstError ?: error completion:completion];
            return;
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            completion(info, info ? nil : (firstError ?: error));
        });
    }] resume];
}

@end
