#!/usr/bin/env bash
# builds the patched apk from jhu's android 10+ fixed 1.0 apk
#   TOOLS=<dir> ./build.sh <in.apk> <out.apk>
# TOOLS needs apktool.jar, sdk/ (build-tools 35.0.0 + platforms/android-34) and dotnet/
set -euo pipefail

IN=$(realpath "$1")
OUT=$(realpath -m "$2")
HERE=$(cd "$(dirname "$0")" && pwd)
TOOLS=$(realpath "${TOOLS:?set TOOLS to the tools dir}")
BT=$TOOLS/sdk/build-tools/35.0.0
ANDROID_JAR=$TOOLS/sdk/platforms/android-34/android.jar
export DOTNET_ROOT=$TOOLS/dotnet PATH=$TOOLS/dotnet:$PATH DOTNET_CLI_TELEMETRY_OPTOUT=1 DOTNET_NOLOGO=1

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
DEC=$WORK/dec
MANAGED=$DEC/assets/bin/Data/Managed

echo "decoding"
java -jar "$TOOLS/apktool.jar" d -f -q "$IN" -o "$DEC"

echo "compiling the patch against the game's dlls"
CSC=$(ls "$TOOLS"/dotnet/sdk/*/Roslyn/bincore/csc.dll | head -1)
dotnet "$CSC" -nologo -noconfig -nostdlib -nowarn:0436 -target:library -langversion:7.3 -optimize -deterministic \
  -out:"$WORK/SsogPatch.dll" \
  -r:"$MANAGED/mscorlib.dll" -r:"$MANAGED/System.dll" -r:"$MANAGED/System.Core.dll" \
  -r:"$MANAGED/UnityEngine.dll" -r:"$MANAGED/Assembly-CSharp.dll" \
  "$HERE"/patch/*.cs

echo "merging it into Assembly-CSharp.dll"
dotnet build "$HERE/patcher" -c Release -o "$WORK/patcher" -v q -nologo >/dev/null
dotnet "$WORK/patcher/Patcher.dll" "$MANAGED" "$WORK/SsogPatch.dll" "$WORK/Assembly-CSharp.dll"
cp "$WORK/Assembly-CSharp.dll" "$MANAGED/Assembly-CSharp.dll"

echo "compiling the cheat menu"
# into the main classes.dex as smali: dalvik (below 5.0) can't load a classes2.dex by itself
mkdir -p "$WORK/classes" "$WORK/dex"
javac -nowarn --release 8 -cp "$ANDROID_JAR" -d "$WORK/classes" $(find "$HERE/android/src" -name '*.java')
"$BT/d8" --release --min-api 8 --lib "$ANDROID_JAR" --output "$WORK/dex" $(find "$WORK/classes" -name '*.class')
(cd "$WORK/dex" && zip -q ../menu.apk classes.dex)
java -jar "$TOOLS/apktool.jar" d -f -q -r "$WORK/menu.apk" -o "$WORK/menu"
cp -r "$WORK/menu/smali/." "$DEC/smali/"

echo "adding the shortcut, the old-android icon and the menu activity"
cp -r "$HERE/android/res/." "$DEC/res/"
python3 - "$DEC" <<'EOF2'
import sys
manifest = sys.argv[1] + "/AndroidManifest.xml"
m = open(manifest).read()
launcher = '<category android:name="android.intent.category.LAUNCHER"/>\n            </intent-filter>'
assert launcher in m, "launcher activity not found"
# long-press menu, 7.1+
m = m.replace(launcher, launcher + '\n            <meta-data android:name="android.app.shortcuts" android:resource="@xml/ssog_shortcuts"/>', 1)
m = m.replace("</application>",
    '    <activity android:name="ssog.CheatActivity" android:exported="true" android:process=":cheats"'
    ' android:taskAffinity="com.kiloo.subwaysurf.og.cheats" android:excludeFromRecents="true"'
    ' android:label="@string/ssog_cheats" android:theme="@style/SsogTheme"/>\n'
    # its own icon below 7.1 (@bool/ssog_cheat_icon is false from v25 on)
    '        <activity-alias android:name="ssog.CheatIcon" android:targetActivity="ssog.CheatActivity"'
    ' android:enabled="@bool/ssog_cheat_icon" android:exported="true"'
    ' android:label="@string/ssog_cheats_icon" android:icon="@drawable/app_icon">\n'
    '            <intent-filter>\n'
    '                <action android:name="android.intent.action.MAIN"/>\n'
    '                <category android:name="android.intent.category.LAUNCHER"/>\n'
    '            </intent-filter>\n'
    '        </activity-alias>\n    </application>', 1)
open(manifest, "w").write(m)
EOF2

echo "building"
java -jar "$TOOLS/apktool.jar" b -q "$DEC" -o "$WORK/unsigned.apk"

echo "signing"
"$BT/zipalign" -p -f 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"
mkdir -p "$(dirname "$OUT")"
"$BT/apksigner" sign --ks "$HERE/ssog.keystore" --ks-pass pass:ssogssog --ks-key-alias ssog --out "$OUT" "$WORK/aligned.apk"
rm -f "$OUT.idsig"
echo "done: $OUT"
