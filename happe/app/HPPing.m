#import "HPPing.h"
#include <netdb.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>

static const int kTimeoutMs = 3000;

static double HPNowMs(void) {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0;
}

// host:port out of a vless:// link, without the full parser
static BOOL HPHostPort(NSString *link, NSString **host, NSString **port) {
    NSString *rest = link;
    NSRange r = [rest rangeOfString:@"://"];
    if (r.location == NSNotFound) return NO;
    rest = [rest substringFromIndex:NSMaxRange(r)];
    for (NSString *cut in @[ @"#", @"?", @"/" ]) {
        NSRange c = [rest rangeOfString:cut];
        if (c.location != NSNotFound) rest = [rest substringToIndex:c.location];
    }
    NSRange at = [rest rangeOfString:@"@" options:NSBackwardsSearch];
    if (at.location != NSNotFound) rest = [rest substringFromIndex:NSMaxRange(at)];
    if ([rest hasPrefix:@"["]) {
        NSRange close = [rest rangeOfString:@"]"];
        if (close.location == NSNotFound) return NO;
        *host = [rest substringWithRange:NSMakeRange(1, close.location - 1)];
        NSString *after = [rest substringFromIndex:close.location + 1];
        *port = [after hasPrefix:@":"] ? [after substringFromIndex:1] : @"443";
    } else {
        NSRange colon = [rest rangeOfString:@":" options:NSBackwardsSearch];
        *host = colon.location == NSNotFound ? rest : [rest substringToIndex:colon.location];
        *port = colon.location == NSNotFound ? @"443" : [rest substringFromIndex:NSMaxRange(colon)];
    }
    return (*host).length > 0;
}

static NSInteger HPConnectMs(NSString *host, NSString *port) {
    struct addrinfo hints = { 0 }, *res = NULL;
    hints.ai_socktype = SOCK_STREAM;
    if (getaddrinfo(host.UTF8String, port.UTF8String, &hints, &res) != 0 || !res) return -1;
    NSInteger best = -1;
    for (struct addrinfo *ai = res; ai && best < 0; ai = ai->ai_next) {
        int s = socket(ai->ai_family, ai->ai_socktype, ai->ai_protocol);
        if (s < 0) continue;
        int on = 1;
        setsockopt(s, SOL_SOCKET, SO_NOSIGPIPE, &on, sizeof on);
        fcntl(s, F_SETFL, fcntl(s, F_GETFL, 0) | O_NONBLOCK);
        double start = HPNowMs();
        int rc = connect(s, ai->ai_addr, ai->ai_addrlen);
        if (rc != 0 && errno == EINPROGRESS) {
            fd_set w;
            FD_ZERO(&w);
            FD_SET(s, &w);
            struct timeval tv = { kTimeoutMs / 1000, (kTimeoutMs % 1000) * 1000 };
            if (select(s + 1, NULL, &w, NULL, &tv) == 1) {
                int err = 0;
                socklen_t len = sizeof err;
                getsockopt(s, SOL_SOCKET, SO_ERROR, &err, &len);
                rc = err == 0 ? 0 : -1;
            } else {
                rc = -1;
            }
        }
        if (rc == 0) best = (NSInteger)(HPNowMs() - start + 0.5);
        close(s);
    }
    freeaddrinfo(res);
    return best;
}

@implementation HPPing

+ (void)pingLinks:(NSArray<NSString *> *)links each:(HPPingResult)each done:(void (^)(void))done {
    // a few at a time: the 5s has two cores and a slow radio
    dispatch_semaphore_t slots = dispatch_semaphore_create(6);
    dispatch_group_t group = dispatch_group_create();
    dispatch_queue_t q = dispatch_get_global_queue(QOS_CLASS_UTILITY, 0);
    for (NSString *link in links) {
        dispatch_group_async(group, q, ^{
            dispatch_semaphore_wait(slots, DISPATCH_TIME_FOREVER);
            NSString *host = nil, *port = nil;
            NSInteger ms = HPHostPort(link, &host, &port) ? HPConnectMs(host, port) : -1;
            dispatch_semaphore_signal(slots);
            dispatch_async(dispatch_get_main_queue(), ^{
                each(link, ms);
            });
        });
    }
    dispatch_group_notify(group, dispatch_get_main_queue(), ^{
        if (done) done();
    });
}

@end
