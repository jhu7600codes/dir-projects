// subscription links: fetch, decode (base64 or plain list of share links),
// and read the profile-title / subscription-userinfo headers panels send.

#import <Foundation/Foundation.h>

@interface HPSubscription : NSObject

// every "scheme://..." line in a subscription body. accepts plain text,
// standard or url-safe base64 (padding optional, line breaks allowed).
+ (NSArray<NSString *> *)linksFromBody:(NSData *)body;

// "profile-title" header, plain or "base64:..."; nil if absent/empty.
+ (NSString *)titleFromHeader:(NSString *)header;

// "upload=1; download=2; total=3; expire=1700000000" -> numbers by key.
+ (NSDictionary<NSString *, NSNumber *> *)userInfoFromHeader:(NSString *)header;

// GET with a v2rayN user agent (makes panels answer with base64 links).
// completion runs on the main queue. info keys: links, title, userinfo.
+ (void)fetch:(NSString *)url completion:(void (^)(NSDictionary *info, NSString *error))completion;

@end
