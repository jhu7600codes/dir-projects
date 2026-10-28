# ssog

Patches for jhu's Subway Surfers 1.0 build (`com.kiloo.subwaysurf.og`, the one fixed for Android 10+). The build starts from that apk and doesn't redo the Android 10 fix.

## What it changes

- **Daily challenge works offline.** The game used to fetch the daily word from `hoodrunner.kiloo.com`, which is gone. The word now comes from a fixed list in `patch/SsogDaily.cs`. It's picked by the UTC date, so it stays the same all day and changes at midnight UTC, and you see every word once before any repeats.
- **Cheat menu.** Long-press the game icon, then tap **Cheats**. It has:
  - infinite gold (a toggle that refills after every purchase) and max gold now
  - unlock all characters, max all upgrades
  - +10 hoverboards, +5 of each headstart, +5 mystery boxes
  - finish the current missions, finish today's daily challenge
  - set multiplier and set high score (to bring your old ones back after reinstalling)

  Changes kick in when you go back to the game.

## How it works

- `patch/*.cs` is compiled against the game's own stripped Mono 2.0 dlls. Unity strips unused methods from its mscorlib (there's no `File.ReadAllLines`, for example), so the compiler catches anything that isn't available.
- `patcher/` uses Mono.Cecil to copy those classes into `Assembly-CSharp.dll`, so the apk doesn't get a new dll. It also rewires `DailyWord`:
  - `ForceSync` picks the day's word from the list
  - `Start` applies cheats first
  - a new `OnApplicationPause` applies cheats and resyncs the daily word when the game comes back
- `android/` holds the cheat menu, a plain `Activity` in its own process, plus the static shortcut. The menu only writes `ssog_actions.txt` and `ssog_settings.txt` into the app's files folders, and the game reads them through `Application.persistentDataPath`.

## Building

```
TOOLS=<dir> ./build.sh <subway-surfers-og-1.0.apk> <out.apk>
```

`TOOLS` needs `apktool.jar` (2.10), `sdk/` with build-tools 35.0.0 and platforms/android-34, and `dotnet/` (.NET 8 SDK). You also need Java 17+ and python3.

The apk is signed with `ssog.keystore` (password `ssogssog`), so later builds install over this one. The first install has to replace the old build, which was signed with a different key, and that means uninstalling it first.
