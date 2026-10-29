// turns a vless:// share link into an xray config.json.
// shared by the app and the vless2json command line tool.

#import <Foundation/Foundation.h>

#define VP_HTTP_PORT 8080
#define VP_SOCKS_PORT 1080

@interface VLESSConfig : NSObject

// returns nil and sets *error (a short human readable message) on bad input.
+ (NSDictionary *)configFromLink:(NSString *)link error:(NSString **)error;

// pretty printed json, or nil on error.
+ (NSData *)configJSONFromLink:(NSString *)link error:(NSString **)error;

// "name (host:port, reality, xtls-rprx-vision)" - safe to show, no uuid/keys.
+ (NSString *)summaryForLink:(NSString *)link;

@end
