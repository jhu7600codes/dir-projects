// tcp "ping": how long a plain tcp connect to host:port takes. goes direct
// (raw sockets ignore the http proxy), so it shows if the server is
// reachable from this network at all.

#import <Foundation/Foundation.h>

// ms >= 0 on success, -1 on timeout/refused/unresolvable
typedef void (^HPPingResult)(NSString *link, NSInteger ms);

@interface HPPing : NSObject
// pings every link (a few at a time); result runs on the main queue per link
+ (void)pingLinks:(NSArray<NSString *> *)links each:(HPPingResult)each done:(void (^)(void))done;
@end
