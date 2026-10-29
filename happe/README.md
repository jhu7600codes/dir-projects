# Happe

a vless + reality client for a jailbroken iphone 5s on ios 10.x (arm64). add subscription urls (several are fine) and/or single `vless://` links, pick a server from the list, tap connect. the app turns the selected server into an xray `config.json`, starts xray-core as a root launchdaemon and points the wifi http proxy at it. disconnect undoes all of that.

everything is built by github actions on a macos runner (`.github/workflows/happe.yml`). no mac or apple developer account needed.

## plan

1. **xray for ios 10.** cross-build xray-core with `GOOS=ios GOARCH=arm64`, cgo, and apple clang. it links against the **ios 10.3 sdk** from theos/sdks with `-target arm64-apple-ios10.0`. linking against the old sdk is itself a check: any symbol ios 10 doesn't have fails the link. `scripts/verify-macho.sh` then checks the binary with `lipo -info` (thin arm64), with `otool -l` (`LC_VERSION_MIN_IPHONEOS 10.0`, no chained fixups), and confirms every imported symbol and dylib exists in the 10.3 sdk. `scripts/build-xray.sh` tries xray releases newest first, each one with the newest go its `go.mod` allows. it keeps every combo that passes.
2. **daemon.** `/Library/LaunchDaemons/com.jhu.happe.plist` runs `/usr/libexec/happe/xray run -c /etc/happe/config.json` as root. the plist ships in `/usr/share/happe/` and `happectl start` copies it into `/Library/LaunchDaemons` and loads it; `stop` unloads and removes it. (the first version used `Disabled=true` + `load -w`, which ios 10's launchctl accepted with exit 0 and then never started the job.) a reboot while connected brings xray back, matching the wifi proxy setting that also survives the reboot. if start fails, the error includes `launchctl list com.jhu.happe` and the last log lines.
3. **helper.** `/usr/libexec/happe/happectl` is root:wheel 4755 and only knows `start | stop | status | proxy-on | proxy-off | log`. `start` copies the app's config into a root-only dir (refusing symlinks and files that aren't yours), runs `xray run -test` on it, then loads the daemon.
4. **app.** plain uikit in objective-c, built against the 10.3 sdk (so nothing newer than ios 10 can even compile). one grouped list: status card, connect/disconnect button, one section per subscription (with traffic used, expiry and last update from the `subscription-userinfo` header), then manually added links.
   - subscriptions are fetched with a `v2rayN` user agent so panels (marzban, 3x-ui, remnawave, ...) answer with base64 share links. plain lists work too. non-vless entries (vmess, trojan, ss) are hidden and counted.
   - pull down to update all subscriptions. stale ones (older than 6 h) update when the app opens. swipe a server for update / delete.
   - tapping another server while connected reconnects right away.
   - add servers by pasting, scanning a qr code with the camera, or picking a qr screenshot from photos. `happ://add/<url>` links/qr codes work, and the app registers the `happ`, `happe` and `vless` url schemes so those links open it from safari. encrypted `happ://crypt…` links can't work: only the happ app has the key.
   - the bottom of the list shows the app and xray versions.
   - subscriptions, links and the selected server are stored only on the phone in `/var/mobile/Library/Happe/state.plist` (0600). fakesigned with `ldid -S` plus entitlements (`entitlements/`).
5. **wifi proxy.** `happectl proxy-on` uses the private SCPreferences api (as root, with the systemconfiguration write entitlements) to set http+https proxy 127.0.0.1:8080 on the wifi service, and backs up the old settings. `proxy-off` restores them. afterwards the app reads the live setting back with `CFNetworkCopySystemProxySettings()`. if it isn't 127.0.0.1:8080, the app shows a one-line manual instruction.

### config written from the link

- inbounds: `http` on 127.0.0.1:8080, `socks` (udp on) on 127.0.0.1:1080
- outbound `proxy`: vless with uuid, `encryption`, `flow`, transport `type` (tcp/raw, xhttp, grpc, ws, httpupgrade, h2) and security (`reality`: sni, fp, pbk, sid, spx, pqv; or `tls`)
- dns: `https://1.1.1.1/dns-query`, `https://8.8.8.8/dns-query`. these are dialed through the routing table, so they go out through the proxy. dns from socks5 udp port 53 goes to xray's resolver (`dns-out`). the vless server's own hostname is resolved by the system, so there's no chicken-and-egg loop.
- private/lan ranges go direct. no geoip/geosite files needed.

## what won't work (or is shaky) on ios 10

- **stock go since 1.18 can't make an ios 10 binary at all.** `crypto/x509` on ios hard-imports `SecTrustEvaluateWithError` (ios 12+) and, in newer go, `SecTrustCopyCertificateChain` (ios 15+). dyld on ios 10 refuses to start such a binary. reality needs xray ≥ 1.8.0, which needs go ≥ 1.20, so there is no unpatched combo. fix: `scripts/patch-go-ios10.sh` patches the downloaded go so `GOOS=ios` reads a pem ca bundle (`/usr/share/happe/cacert.pem`, mozilla's list from curl.se) instead of calling Security.framework. the ci "control" row in the build report shows the unpatched link failing.
- **go 1.27's runtime reads the arm generic timer (`CNTVCT_EL0`) in `procyield`.** ios 10 doesn't allow that from user code, so the first build died with `SIGILL: illegal instruction` in `runtime.procyieldAsm` on the phone. the go patch puts back the old `YIELD` loop, and `verify-macho.sh` now disassembles every binary and fails if anything reads `cntvct`/`cntpct`/`cntfrq`.
- **checked when building, not at runtime.** the build proves the binary loads on ios 10 (right min version and only symbols ios 10 has). it can't prove the go runtime behaves on an ios 10 kernel. that's what stage 1 on the phone is for. older candidates (older go) are uploaded too, in case the newest one misbehaves.
- **xcode 15+ dropped ios < 12 deployment targets** in its build system and removed libarclite. we bypass xcode projects (plain `clang` + the 10.3 sdk) and don't pass `-fobjc-arc` at link time.
- **setting the wifi proxy from code is unverified on device.** ios keeps proxy settings per wifi network, and settings.app may not reflect what configd uses. the app checks the real effective setting and falls back to the one-line manual hint.
- **setuid helpers** work on the usual ios 10 jailbreaks (h3lix, doubleh3lix, meridian, g0blin). if yours ignores the setuid bit, `happectl` says so. use ssh as root then.
- **memory.** the 5s has 1 GB of ram. the daemon gets `GOMEMLIMIT=96MiB` and `GOGC=50`, and `happectl` raises its jetsam priority and limit (best effort).
- **the .ipa on its own can't run a root daemon.** an ipa can't install setuid files or launchdaemons. the ipa is only the app. use the .deb, which has everything.
- **ios 10 cydia's dpkg** doesn't know zstd, so the .deb is built with gzip.
- `xhttp` needs xray ≥ 24.9. older fallback candidates only cover tcp/grpc/ws reality.

## artifacts (actions tab, latest run of "happe (ios 10)")

| artifact | what |
|---|---|
| `stage1-xray` | signed `xray`, `vless2json`, `cacert.pem` and `xray-build-report.md` (which go/xray combo was picked and why) |
| `happe-deb` | `com.jhu.happe_1.0.N_iphoneos-arm.deb`, everything |
| `happe-ipa` | `Happe.ipa`, the app only |
| `xray-candidates-unsigned` | every xray build that passed, newest first by name (sign with `ldid -S` before use) |

github zips artifacts, so unzip them on ubuntu first.

## install + test, stage by stage

you need openssh on the phone (cydia: openssh). the default root password is `alpine`, so change it with `passwd`. in the commands below, `PHONE` is the phone's wifi ip.

### stage 1: does xray run on the phone?

```sh
# on ubuntu, in the unzipped stage1-xray folder
scp xray vless2json cacert.pem root@PHONE:/tmp/
ssh root@PHONE
chmod +x /tmp/xray /tmp/vless2json
/tmp/xray version            # must print "Xray 2x.x.x ... (go1.xx ios/arm64)"
/tmp/xray uuid               # exercises crypto/rand
```

`killed: 9` means a codesign problem: re-sign on the phone with `ldid -S /tmp/xray` (cydia: ldid). `dyld: Symbol not found` means a symbol check got past ci, so tell me the symbol. a go `fatal error` means try the next binary from `xray-candidates-unsigned`.

then a real connection, without the daemon. the link only goes into the shell's memory, not into history:

```sh
read -r L          # paste your vless:// link, enter
printf '%s' "$L" | /tmp/vless2json > /tmp/config.json; unset L
SSL_CERT_FILE=/tmp/cacert.pem /tmp/xray run -c /tmp/config.json &
curl -sx http://127.0.0.1:8080 https://ifconfig.me; echo     # your server's ip
curl -s --socks5-hostname 127.0.0.1:1080 https://ifconfig.me; echo
kill %1; rm /tmp/config.json
```

(no curl? cydia: cURL. or skip it and test from safari after stage 3.)

### stage 2: the daemon

```sh
scp com.jhu.happe_*.deb root@PHONE:/tmp/
ssh root@PHONE
dpkg -i /tmp/com.jhu.happe_*.deb
ls -l /usr/libexec/happe/      # happectl must be -rwsr-xr-x root wheel
```

as `mobile`, so the setuid path gets tested:

```sh
su mobile
read -r L; printf '%s' "$L" | vless2json > /var/mobile/Library/Happe/config.json; unset L
chmod 600 /var/mobile/Library/Happe/config.json
/usr/libexec/happe/happectl start      # -> "running <pid>"
/usr/libexec/happe/happectl status
curl -sx http://127.0.0.1:8080 https://ifconfig.me; echo
/usr/libexec/happe/happectl log        # xray's log, warnings only
/usr/libexec/happe/happectl proxy-on   # then check settings > wi-fi > (i) > http proxy
/usr/libexec/happe/happectl proxy-off
/usr/libexec/happe/happectl stop       # -> "stopped"
```

if `start` says not setuid root, run `chown root:wheel /usr/libexec/happe/happectl; chmod 4755 /usr/libexec/happe/happectl` as root. if it still fails, your jailbreak blocks setuid, so tell me which jailbreak it is.

### stage 3: the app

the .deb from stage 2 already installed it. no icon? run `su mobile -c uicache` (or respring). or install the .deb from filza/zebra instead of ssh.

1. open "Happe", tap "Add Subscription or Link…", paste your subscription url (or a vless:// link), tap add
2. pick a server (checkmark), tap connect
3. the status card should say "Connected" with the server name. the note under the button says the wi-fi proxy is set, or tells you to set it by hand: settings > wi-fi > (i) > http proxy > manual, server 127.0.0.1, port 8080
4. safari: open ifconfig.me, which should show the server's ip
5. tap disconnect: proxy cleared, xray stopped

the `.ipa` (appsync unified + filza, or reprovision) installs only the app. it still needs the .deb for the daemon and helper.

### uninstall

`dpkg -r com.jhu.happe` clears the proxy and stops the daemon. `dpkg -P` also deletes `/etc/happe` (the copied config) and the log.

## secrets

no link, uuid or key is in this repo. the link only exists in the app's text field and in the generated config files on the phone: `/var/mobile/Library/Happe/config.json` (0600, mobile) and `/etc/happe/config.json` (0600, root). the ci smoke test generates throwaway uuids and keys at runtime.

## layout

```
app/          uikit app (main.m, Info.plist)
common/       vless:// -> xray config (shared with vless2json)
helper/       happectl setuid helper
layout/       deb skeleton: DEBIAN scripts + launchdaemon plist
entitlements/ ldid entitlements per binary
scripts/      patch-go-ios10, build-xray, verify-macho, build-native, package, test-config
```

local build on linux works too, for hacking. use `IOS_CC` with a clang + ld64.lld wrapper (`-target arm64-apple-ios10.0 -mlinker-version=711`). run `scripts/fix-sdk-tbd.sh` on the sdk first (ci does the same; apple ld and lld both read the old tbds as simulator libs otherwise).
