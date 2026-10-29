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
rm -rf "$T"
