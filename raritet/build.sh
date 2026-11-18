#!/usr/bin/env bash
# builds raritet.dpk (needs a droid checkout, DROID=path) and raritet.apk (needs the android sdk, ANDROID_HOME=path)
# no gradle: aapt2 + javac + d8 + apksigner straight from build-tools
set -euo pipefail
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-/opt/android-sdk}; BT=$SDK/build-tools/34.0.0; JAR=$SDK/platforms/android-34/android.jar
if [ -n "${DROID:-}" ]; then (cd "$DROID" && node tools/pack-dpk/pack.mjs "$OLDPWD/web" "$OLDPWD/raritet.dpk" --integrity); fi
B=build; rm -rf $B; mkdir -p $B/assets $B/classes $B/dex
cp web/*.html web/*.js web/*.css $B/assets/
$BT/aapt2 compile --dir android/res -o $B/res.zip
$BT/aapt2 link -I $JAR --manifest android/AndroidManifest.xml -A $B/assets --java $B/gen -o $B/unsigned.apk $B/res.zip
javac -source 8 -target 8 -classpath $JAR -d $B/classes android/src/com/jhu/raritet/*.java $(find $B/gen -name '*.java') 2>&1 | grep -v "warning: \[options\]" || true
$BT/d8 --min-api 21 --lib $JAR --output $B/dex $(find $B/classes -name '*.class')
cp $B/unsigned.apk $B/app.apk && (cd $B/dex && zip -q ../app.apk classes.dex)
$BT/zipalign -f 4 $B/app.apk $B/aligned.apk
# debug key, fine for sideloading. keep the same key around or android won't let you update over it
[ -f android/debug.keystore ] || keytool -genkeypair -keystore android/debug.keystore -storepass android -keypass android -alias raritet -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=raritet" >/dev/null 2>&1
$BT/apksigner sign --ks android/debug.keystore --ks-pass pass:android --key-pass pass:android --out raritet.apk $B/aligned.apk
rm -rf $B raritet.apk.idsig
echo built raritet.apk
