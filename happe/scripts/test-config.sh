#!/bin/bash
# ci smoke test (macos runner): builds the link parser for the host, feeds it
# synthetic links (uuid/keys generated right here, nothing real) and checks
# that a host build of the same xray accepts every generated config.
#   XRAY_SRC=... GO=... test-config.sh
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
T="$(mktemp -d)"

clang -fobjc-arc -O1 -Wall -Werror -I"$ROOT/common" "$ROOT/common/VLESSConfig.m" "$ROOT/common/vless2json.m" \
  -framework Foundation -o "$T/vless2json"
( cd "$XRAY_SRC" && env -u GOOS -u GOARCH CGO_ENABLED=0 GOTOOLCHAIN=local "$GO" build -o "$T/xray" ./main )

UUID=$("$T/xray" uuid)
# "Password: x" on new xray, "Public key: x" on old
PBK=$("$T/xray" x25519 | awk -F': ' 'NR==2{print $2}')
SID=$(openssl rand -hex 8)

links=(
  "vless://$UUID@example.com:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=www.microsoft.com&fp=chrome&pbk=$PBK&sid=$SID&spx=%2F&type=tcp&headerType=none#test%20node"
  "vless://$UUID@203.0.113.7:8443?security=reality&sni=dl.google.com&fp=firefox&pbk=$PBK&sid=$SID&type=xhttp&path=%2Fxh&mode=auto#xhttp"
  "vless://$UUID@example.com:443?security=reality&sni=www.apple.com&fp=safari&pbk=$PBK&type=grpc&serviceName=gsvc&mode=multi#grpc"
  "vless://$UUID@[2001:db8::1]:443?security=tls&sni=example.com&type=ws&path=%2Fws%3Fed%3D2048&host=example.com&alpn=h2%2Chttp%2F1.1#ws%20tls"
  "  vless://$UUID@example.com:443?type=tcp&security=reality&pbk=$PBK&sni=a.com&flow=xtls-rprx-vision
  "
)
i=0
for l in "${links[@]}"; do
  i=$((i + 1))
  printf '%s' "$l" | "$T/vless2json" > "$T/c$i.json"
  "$T/xray" run -test -c "$T/c$i.json" > "$T/t$i.log" 2>&1 || { cat "$T/t$i.log"; echo "config $i rejected"; exit 1; }
  echo "link $i: config accepted"
done

# the reality config must carry exactly what the link said
python3 - "$T/c1.json" "$PBK" "$SID" <<'EOF'
import json, sys
c = json.load(open(sys.argv[1]))
o = c["outbounds"][0]
u = o["settings"]["vnext"][0]["users"][0]
r = o["streamSettings"]["realitySettings"]
assert u["flow"] == "xtls-rprx-vision", u
assert r["publicKey"] == sys.argv[2] and r["shortId"] == sys.argv[3], r
assert r["serverName"] == "www.microsoft.com" and r["fingerprint"] == "chrome" and r["spiderX"] == "/", r
ins = {i["protocol"]: (i["listen"], i["port"]) for i in c["inbounds"]}
assert ins == {"http": ("127.0.0.1", 8080), "socks": ("127.0.0.1", 1080)}, ins
assert c["dns"]["servers"][0].startswith("https://"), c["dns"]
print("reality fields ok")
EOF

for bad in "http://x" "vless://@h:1" "vless://u@h:99999" "vless://u@h:443?security=reality"; do
  if printf '%s' "$bad" | "$T/vless2json" > /dev/null 2>&1; then echo "accepted bad link: $bad"; exit 1; fi
done
echo "bad links rejected"

# subscription decoding: base64 (std + url-safe, no padding, wrapped), plain, headers
cat > "$T/subtest.m" <<'OBJC'
#import <Foundation/Foundation.h>
#import "HPSubscription.h"
int main(int argc, char **argv) {
    @autoreleasepool {
        NSString *list = [NSString stringWithContentsOfFile:@(argv[1]) encoding:NSUTF8StringEncoding error:nil];
        NSData *raw = [list dataUsingEncoding:NSUTF8StringEncoding];
        NSString *b64 = [raw base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength];
        NSString *urlsafe = [[[[raw base64EncodedStringWithOptions:0] stringByReplacingOccurrencesOfString:@"+" withString:@"-"]
            stringByReplacingOccurrencesOfString:@"/" withString:@"_"] stringByReplacingOccurrencesOfString:@"=" withString:@""];
        for (NSString *body in @[ list, b64, urlsafe ]) {
            NSArray *links = [HPSubscription linksFromBody:[body dataUsingEncoding:NSUTF8StringEncoding]];
            if (links.count != 6) { printf("decode: got %lu links, want 6\n", (unsigned long)links.count); return 1; }
        }
        NSString *t = [HPSubscription titleFromHeader:@"base64:8J+HqfCfh6ogTXkgVlBO"];
        if (![t isEqualToString:@"\U0001F1E9\U0001F1EA My VPN"]) { printf("title: %s\n", t.UTF8String); return 1; }
        NSDictionary *u = [HPSubscription userInfoFromHeader:@"upload=100; download=2048; total=10737418240; expire=1798761600"];
        if ([u[@"total"] longLongValue] != 10737418240LL || [u[@"expire"] longLongValue] != 1798761600LL) { printf("userinfo\n"); return 1; }
        printf("subscription decoding ok\n");
    }
    return 0;
}
OBJC
clang -fobjc-arc -Wall -Werror -I"$ROOT/common" "$ROOT/common/HPSubscription.m" "$T/subtest.m" -framework Foundation -o "$T/subtest"
{ printf '%s\n' "${links[@]:0:4}"; echo "vmess://eyJhZGQiOiJleGFtcGxlLmNvbSJ9"; echo "trojan://pw@example.com:443#t"; } > "$T/list.txt"
"$T/subtest" "$T/list.txt"
rm -rf "$T"
