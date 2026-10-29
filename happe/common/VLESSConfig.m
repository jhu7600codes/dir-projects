#import "VLESSConfig.h"

static NSString *VPDecode(NSString *s) {
    NSString *d = [[s stringByReplacingOccurrencesOfString:@"+" withString:@"%20"] stringByRemovingPercentEncoding];
    return d ? d : s;
}

// query values keep '+' as-is (base64url keys never contain it, but paths may)
static NSString *VPDecodeQuery(NSString *s) {
    NSString *d = [s stringByRemovingPercentEncoding];
    return d ? d : s;
}

static NSString *VPTrim(NSString *s) {
    return [s stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
}

@implementation VLESSConfig

// splits the link into uuid/host/port/name and a lowercase-keyed query dict.
+ (NSDictionary *)partsOfLink:(NSString *)rawLink error:(NSString **)error {
    NSString *link = VPTrim(rawLink ?: @"");
    // people paste with stray spaces/newlines in the middle sometimes
    link = [[link componentsSeparatedByCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]] componentsJoinedByString:@""];
    if (![[link lowercaseString] hasPrefix:@"vless://"]) {
        if (error) *error = @"link must start with vless://";
        return nil;
    }
    NSString *rest = [link substringFromIndex:8];

    NSString *name = @"";
    NSRange hash = [rest rangeOfString:@"#"];
    if (hash.location != NSNotFound) {
        name = VPDecode([rest substringFromIndex:hash.location + 1]);
        rest = [rest substringToIndex:hash.location];
    }

    NSString *query = @"";
    NSRange q = [rest rangeOfString:@"?"];
    if (q.location != NSNotFound) {
        query = [rest substringFromIndex:q.location + 1];
        rest = [rest substringToIndex:q.location];
    }
    if ([rest hasSuffix:@"/"]) rest = [rest substringToIndex:rest.length - 1];

    NSRange at = [rest rangeOfString:@"@" options:NSBackwardsSearch];
    if (at.location == NSNotFound || at.location == 0) {
        if (error) *error = @"missing uuid@host:port";
        return nil;
    }
    NSString *uuid = VPDecode([rest substringToIndex:at.location]);
    NSString *hostport = [rest substringFromIndex:at.location + 1];

    NSString *host = nil, *portStr = nil;
    if ([hostport hasPrefix:@"["]) {
        NSRange close = [hostport rangeOfString:@"]"];
        if (close.location == NSNotFound) {
            if (error) *error = @"bad ipv6 address";
            return nil;
        }
        host = [hostport substringWithRange:NSMakeRange(1, close.location - 1)];
        NSString *after = [hostport substringFromIndex:close.location + 1];
        if ([after hasPrefix:@":"]) portStr = [after substringFromIndex:1];
    } else {
        NSRange colon = [hostport rangeOfString:@":" options:NSBackwardsSearch];
        if (colon.location != NSNotFound) {
            host = [hostport substringToIndex:colon.location];
            portStr = [hostport substringFromIndex:colon.location + 1];
        } else {
            host = hostport;
        }
    }
    int port = portStr.length ? [portStr intValue] : 443;
    if (host.length == 0) {
        if (error) *error = @"missing server address";
        return nil;
    }
    if (port <= 0 || port > 65535) {
        if (error) *error = @"bad port";
        return nil;
    }
    if (uuid.length == 0) {
        if (error) *error = @"missing uuid";
        return nil;
    }

    NSMutableDictionary *params = [NSMutableDictionary dictionary];
    for (NSString *pair in [query componentsSeparatedByString:@"&"]) {
        if (pair.length == 0) continue;
        NSRange eq = [pair rangeOfString:@"="];
        NSString *k = eq.location == NSNotFound ? pair : [pair substringToIndex:eq.location];
        NSString *v = eq.location == NSNotFound ? @"" : [pair substringFromIndex:eq.location + 1];
        params[[VPDecodeQuery(k) lowercaseString]] = VPDecodeQuery(v);
    }

    return @{ @"uuid": uuid, @"host": host, @"port": @(port), @"name": name, @"params": params };
}

static NSString *P(NSDictionary *p, NSString *key) {
    NSString *v = p[key];
    return v.length ? v : nil;
}

static NSArray *VPSplitList(NSString *s) {
    NSMutableArray *out = [NSMutableArray array];
    for (NSString *x in [s componentsSeparatedByString:@","]) {
        NSString *t = VPTrim(x);
        if (t.length) [out addObject:t];
    }
    return out;
}

+ (NSDictionary *)streamSettingsFromParams:(NSDictionary *)p host:(NSString *)host error:(NSString **)error {
    NSString *network = [P(p, @"type") ?: @"tcp" lowercaseString];
    if ([network isEqualToString:@"raw"]) network = @"tcp";
    if ([network isEqualToString:@"splithttp"]) network = @"xhttp";
    if ([network isEqualToString:@"http"]) network = @"h2";
    NSString *security = [P(p, @"security") ?: @"none" lowercaseString];

    NSMutableDictionary *ss = [NSMutableDictionary dictionary];
    ss[@"network"] = network;
    ss[@"security"] = security;

    NSString *path = P(p, @"path");
    NSString *hostHeader = P(p, @"host");

    if ([network isEqualToString:@"tcp"]) {
        if ([[P(p, @"headertype") lowercaseString] isEqualToString:@"http"]) {
            NSMutableDictionary *req = [NSMutableDictionary dictionary];
            req[@"path"] = path ? VPSplitList(path) : @[@"/"];
            if (hostHeader) req[@"headers"] = @{ @"Host": VPSplitList(hostHeader) };
            ss[@"tcpSettings"] = @{ @"header": @{ @"type": @"http", @"request": req } };
        }
    } else if ([network isEqualToString:@"ws"]) {
        NSMutableDictionary *ws = [NSMutableDictionary dictionary];
        ws[@"path"] = path ?: @"/";
        if (hostHeader) {
            ws[@"host"] = hostHeader;
            ws[@"headers"] = @{ @"Host": hostHeader };
        }
        ss[@"wsSettings"] = ws;
    } else if ([network isEqualToString:@"httpupgrade"]) {
        NSMutableDictionary *hu = [NSMutableDictionary dictionary];
        hu[@"path"] = path ?: @"/";
        if (hostHeader) hu[@"host"] = hostHeader;
        ss[@"httpupgradeSettings"] = hu;
    } else if ([network isEqualToString:@"grpc"]) {
        NSMutableDictionary *g = [NSMutableDictionary dictionary];
        g[@"serviceName"] = P(p, @"servicename") ?: @"";
        g[@"multiMode"] = @([[P(p, @"mode") lowercaseString] isEqualToString:@"multi"]);
        if (P(p, @"authority")) g[@"authority"] = P(p, @"authority");
        ss[@"grpcSettings"] = g;
    } else if ([network isEqualToString:@"xhttp"]) {
        NSMutableDictionary *x = [NSMutableDictionary dictionary];
        x[@"path"] = path ?: @"/";
        if (hostHeader) x[@"host"] = hostHeader;
        x[@"mode"] = P(p, @"mode") ?: @"auto";
        NSString *extra = P(p, @"extra");
        if (extra) {
            id obj = [NSJSONSerialization JSONObjectWithData:[extra dataUsingEncoding:NSUTF8StringEncoding] options:0 error:nil];
            if ([obj isKindOfClass:[NSDictionary class]]) x[@"extra"] = obj;
        }
        ss[@"xhttpSettings"] = x;
    } else if ([network isEqualToString:@"h2"]) {
        NSMutableDictionary *h = [NSMutableDictionary dictionary];
        h[@"path"] = path ?: @"/";
        if (hostHeader) h[@"host"] = VPSplitList(hostHeader);
        ss[@"httpSettings"] = h;
    } else {
        if (error) *error = [NSString stringWithFormat:@"unsupported transport type=%@", network];
        return nil;
    }

    NSString *sni = P(p, @"sni") ?: P(p, @"peer");
    NSString *fp = P(p, @"fp");

    if ([security isEqualToString:@"reality"]) {
        NSString *pbk = P(p, @"pbk");
        if (!pbk) {
            if (error) *error = @"reality link has no pbk (public key)";
            return nil;
        }
        NSMutableDictionary *r = [NSMutableDictionary dictionary];
        r[@"serverName"] = sni ?: @"";
        r[@"fingerprint"] = fp ?: @"chrome";
        r[@"publicKey"] = pbk;
        r[@"shortId"] = P(p, @"sid") ?: @"";
        r[@"spiderX"] = P(p, @"spx") ?: @"";
        if (P(p, @"pqv")) r[@"mldsa65Verify"] = P(p, @"pqv");
        ss[@"realitySettings"] = r;
    } else if ([security isEqualToString:@"tls"]) {
        NSMutableDictionary *t = [NSMutableDictionary dictionary];
        t[@"serverName"] = sni ?: (hostHeader ?: host);
        if (fp) t[@"fingerprint"] = fp;
        if (P(p, @"alpn")) t[@"alpn"] = VPSplitList(P(p, @"alpn"));
        NSString *insecure = P(p, @"allowinsecure") ?: P(p, @"insecure");
        if ([insecure isEqualToString:@"1"] || [[insecure lowercaseString] isEqualToString:@"true"]) t[@"allowInsecure"] = @YES;
        ss[@"tlsSettings"] = t;
    } else if (![security isEqualToString:@"none"]) {
        if (error) *error = [NSString stringWithFormat:@"unsupported security=%@", security];
        return nil;
    }
    return ss;
}

+ (NSDictionary *)configFromLink:(NSString *)link error:(NSString **)error {
    NSDictionary *parts = [self partsOfLink:link error:error];
    if (!parts) return nil;
    NSDictionary *p = parts[@"params"];

    NSDictionary *stream = [self streamSettingsFromParams:p host:parts[@"host"] error:error];
    if (!stream) return nil;

    NSMutableDictionary *user = [NSMutableDictionary dictionary];
    user[@"id"] = parts[@"uuid"];
    user[@"encryption"] = P(p, @"encryption") ?: @"none";
    if (P(p, @"flow")) user[@"flow"] = P(p, @"flow");

    NSDictionary *proxy = @{
        @"tag": @"proxy",
        @"protocol": @"vless",
        @"settings": @{ @"vnext": @[ @{ @"address": parts[@"host"], @"port": parts[@"port"], @"users": @[user] } ] },
        @"streamSettings": stream,
    };

    NSDictionary *sniffing = @{ @"enabled": @YES, @"destOverride": @[@"http", @"tls"], @"routeOnly": @YES };

    return @{
        @"log": @{ @"loglevel": @"warning" },
        // https:// (not https+local://) servers are dialed through the routing
        // table, so with "proxy" as the first outbound every dns query xray
        // makes goes out through the vless tunnel.
        @"dns": @{
            @"servers": @[ @"https://1.1.1.1/dns-query", @"https://8.8.8.8/dns-query" ],
            @"queryStrategy": @"UseIPv4",
        },
        @"inbounds": @[
            @{ @"tag": @"http-in", @"listen": @"127.0.0.1", @"port": @(VP_HTTP_PORT), @"protocol": @"http",
               @"settings": @{}, @"sniffing": sniffing },
            @{ @"tag": @"socks-in", @"listen": @"127.0.0.1", @"port": @(VP_SOCKS_PORT), @"protocol": @"socks",
               @"settings": @{ @"auth": @"noauth", @"udp": @YES, @"ip": @"127.0.0.1" }, @"sniffing": sniffing },
        ],
        @"outbounds": @[
            proxy,
            @{ @"tag": @"direct", @"protocol": @"freedom" },
            @{ @"tag": @"block", @"protocol": @"blackhole" },
            @{ @"tag": @"dns-out", @"protocol": @"dns" },
        ],
        @"routing": @{
            @"domainStrategy": @"AsIs",
            @"rules": @[
                // plain dns sent over socks5 udp gets answered by xray's own
                // resolver, which itself goes through the proxy (see "dns")
                @{ @"type": @"field", @"inboundTag": @[@"socks-in"], @"network": @"udp", @"port": @"53", @"outboundTag": @"dns-out" },
                @{ @"type": @"field", @"domain": @[@"full:localhost"], @"outboundTag": @"direct" },
                @{ @"type": @"field",
                   @"ip": @[ @"127.0.0.0/8", @"10.0.0.0/8", @"172.16.0.0/12", @"192.168.0.0/16", @"169.254.0.0/16",
                             @"::1/128", @"fc00::/7", @"fe80::/10" ],
                   @"outboundTag": @"direct" },
            ],
        },
    };
}

+ (NSData *)configJSONFromLink:(NSString *)link error:(NSString **)error {
    NSDictionary *cfg = [self configFromLink:link error:error];
    if (!cfg) return nil;
    NSError *e = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:cfg options:NSJSONWritingPrettyPrinted error:&e];
    if (!data && error) *error = e.localizedDescription ?: @"json encoding failed";
    return data;
}

+ (NSString *)summaryForLink:(NSString *)link {
    NSDictionary *parts = [self partsOfLink:link error:NULL];
    if (!parts) return nil;
    NSDictionary *p = parts[@"params"];
    NSMutableArray *bits = [NSMutableArray array];
    [bits addObject:[NSString stringWithFormat:@"%@:%@", parts[@"host"], parts[@"port"]]];
    [bits addObject:P(p, @"type") ?: @"tcp"];
    [bits addObject:P(p, @"security") ?: @"none"];
    if (P(p, @"flow")) [bits addObject:P(p, @"flow")];
    NSString *name = [parts[@"name"] length] ? parts[@"name"] : @"server";
    return [NSString stringWithFormat:@"%@ (%@)", name, [bits componentsJoinedByString:@", "]];
}

@end
