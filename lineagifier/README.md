# lineagifier

an android app (kotlin, compose, shizuku) that gets a stock aosp android 13 image as close to lineageos as an unprivileged app with shell access can. it's meant for the twoyi container: no root, no framework patching, no system partition changes.

everything it does goes through shizuku as the `shell` user (uid 2000), the same rights `adb shell` has.

## flow

1. **shizuku check**: running? permission granted? shell or root?
2. **pick apps**: the catalog, with what's really on your image next to every entry (`pm list packages -s -u`)
3. **tweaks**: only the ones whose commands exist on this image
4. **apply**: download, sha256 check, `pm install`, set default, apply tweaks, with progress and errors per step
5. **stock apps**: disable or remove a stock app, only once it's safe. "restore everything" lives here too

## the catalog (`app/src/main/assets/catalog.json`)

checked on 2026-11-27. lineage package ids were read from each repo's `build.gradle.kts` / `AndroidManifest.xml`, aosp ids from the android13-release manifests, and every f-droid sha256 was computed from the apk and matched against f-droid's `index-v2.json`.

| slot | lineage app | package | status | stock it replaces | prebuilt you can install |
|---|---|---|---|---|---|
| launcher | Trebuchet | `com.android.launcher3` | build from source (soong only, same id as aosp launcher3) | `com.android.launcher3` | none |
| camera | Aperture | `org.lineageos.aperture` | build from source (gradle) | `com.android.camera2` | none |
| gallery | Glimpse | `org.lineageos.glimpse` | build from source (gradle) | `com.android.gallery3d` | Fossify Gallery |
| browser | Jelly | `org.lineageos.jelly` | build from source (gradle) | `org.chromium.webview_shell` | none |
| calendar | Etar | `ws.xsoh.etar` | **prebuilt** (f-droid, upstream etar) | `com.android.calendar` | Etar 1.0.58 |
| recorder | Recorder | `org.lineageos.recorder` | build from source (gradle) | nothing (aosp 13 has none) | Fossify Voice Recorder |
| clock | DeskClock | `com.android.deskclock` | same as stock (lineage fork, same id) | `com.android.deskclock` | Fossify Clock |
| calculator | ExactCalculator | `com.android.calculator2` | same as stock | `com.android.calculator2` | Fossify Calculator |
| files | DocumentsUI | `com.android.documentsui` | same as stock, never removed (system file picker) | nothing | Fossify File Manager |
| contacts | Contacts | `com.android.contacts` | same as stock | `com.android.contacts` | Fossify Contacts |
| dialer | Dialer | `com.android.dialer` | same as stock | `com.android.dialer` | Fossify Phone |
| messaging | Messaging | `com.android.messaging` | same as stock | `com.android.messaging` | Fossify Messages |
| wallpapers | Backgrounds | `org.lineageos.backgrounds` | build from source (soong only) | nothing | none |
| backup | Seedvault | `com.stevesoltys.seedvault` | **not possible** (needs `android.permission.BACKUP`, priv-app) | nothing | none |

only etar exists as a published apk. lineage doesn't publish aperture, glimpse, jelly, recorder, trebuchet or backgrounds anywhere (not on f-droid, not on izzyondroid, no github releases). the fossify apps are labelled in the ui as "not lineage, closest prebuilt". if you build aperture / glimpse / jelly / recorder yourself (they have gradle builds), their debug builds install as `org.lineageos.<name>.dev`, and lineagifier counts those as installed replacements.

f-droid moves old versions to `/archive`, so each download has that as a fallback url. if the hash ever doesn't match, the file is deleted and nothing is installed.

## safety rules

- a stock app is only offered for disable/remove after its replacement is installed **and enabled**
- if the slot has a default-app role (home, browser, dialer, sms), the replacement must already hold it
- anything that can be home is only removable when another enabled launcher is the current home. after removing it, lineagifier checks again and puts the stock one back straight away if no launcher is left
- these are never touched: systemui, settings, shell, phone, documentsui, webview, permission controller, package installer, external storage, networkstack, latinime, shizuku, lineagifier
- the removal list only shows stock apps that `pm list packages -s -u` actually found
- "remove" is `pm uninstall -k --user 0` (the apk stays on /system, data kept), "disable" is `pm disable-user --user 0`

## undo

every disable, removal, role change, setting and overlay change is written to `files/undo-log.json` **before** the command runs, with the old value. "restore everything" undoes them newest first:

- removed: `cmd package install-existing --user 0 <pkg>`
- disabled: `pm enable --user 0 <pkg>`
- settings: `settings put` the old value, or `settings delete` if there wasn't one
- overlays: back to their old enabled/disabled state
- roles: the old holder gets the role back

newest first means a stock launcher is re-enabled before home goes back to it. restore never removes anything. apps you installed stay installed.

## tweaks

each one is probed first (`cmd -l`, `cmd overlay list`, `settings get`), then applied, then read back to check it stuck.

| tweak | how | needs |
|---|---|---|
| dark mode | `cmd uimode night yes/no/auto` | `uimode` service |
| animation speed | `settings put global window/transition/animator_*_scale` | shell's WRITE_SECURE_SETTINGS |
| navigation mode | `cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.*` | navbar overlays on the image |
| accent color | android 12+: merges `system_palette`/`accent_color` into `secure theme_customization_overlay_packages` (monet). older: `com.android.theme.color.*` overlays | the setting, or the overlays |
| icon shape | android 12+: `adaptive_icon_shape` in the same json. older: `com.android.theme.icon.*` overlays | shape overlays on the image |

## what's approximated, what's missing

**approximated**
- lineage's apps: only etar is the real thing (upstream, not lineage's fork). gallery, recorder, clock, calculator, files, contacts, dialer and messaging can only get fossify stand-ins
- clock, calculator, contacts, dialer, messaging, files: lineage's versions are forks of the aosp apps you already have, with the same package ids, so they can't be installed over the system ones without lineage's platform key. your stock ones are the closest you'll get
- launcher: aosp launcher3 is what trebuchet is forked from, but trebuchet's extras (icon packs, hidden apps, protected apps, grid options) aren't there
- accent color / icon shape: done through the same theme setting the aosp wallpaper picker uses, so systemui may change it again if you change wallpaper

**missing, can't be done without root or framework changes**
- signature spoofing, so no microg
- the whole lineage sdk: privacy guard, trust interface, lineageparts, livedisplay, profiles, styles beyond what aosp has
- status bar options (clock position, battery style, network traffic, brightness slider), lockscreen options, power menu options, button/gesture customisation, long-press actions
- seedvault (needs to be a privileged system backup transport)
- lineage's updater, recovery, `adb root`, and anything that edits /system or the framework
- default camera / gallery / calendar: android has no role for these, so pick them once from the "open with" chooser

## twoyi notes

- the target is the ios build of twoyi, which runs android 13 (sdk 33). that's the same sdk as the emulator lineagifier was tested on, so roles (read via `dumpsys role`), monet accent and the navbar overlays should all apply. the android-host twoyi (android 8.1) is a different thing; lineagifier still reads the sdk at runtime and turns off what an image doesn't have
- shizuku has to be started inside the container. if the container has no adb or wireless debugging, shizuku can't start, and lineagifier can't do anything. none of this has been tested inside twoyi yet

## what's been tested

on the aosp 13 emulator image (`system-images;android-33;default;x86_64`, sdk 33, userdebug), with shizuku 13.6 started over adb, so lineagifier ran as uid 2000 like it would in a container:

- shizuku permission grant, the shell user service, `pm list packages -s -u` detection
- etar: sha256 check of the apk, then install by streaming it into `pm install -S` through shizuku
- dark mode, animation scale, gestural nav (`enable-exclusive --category`), lineage teal accent (systemui generated its monet overlays)
- icon shape correctly reported as unavailable (that image has no shape overlays)
- removing gallery3d and disabling calendar once fossify gallery / etar were installed; launcher and camera stayed locked
- dialer stayed locked until fossify phone held the dialer role, then "make default" set it
- "restore everything" put all 14 logged changes back, checked one by one afterwards

two real things that came out of that:
- aosp 13 has no `cmd role get-role-holders` (it's 14+), so holders are read from `dumpsys role`
- on a slow device `cmd role add-role-holder` fails with `TimeoutException` and then applies the change a few seconds later, so lineagifier polls the real holders instead of trusting the exit code

**not tested**: inside twoyi itself, a real (non-emulator) device, the in-app download over the network (the test emulator had no internet; the downloader is tested on the jvm against f-droid instead: `LINEAGIFIER_NET_TEST=1 ./gradlew testDebugUnitTest`), the launcher path end to end (there's no prebuilt launcher in the catalog, only the safety rules are unit tested), android 8.1-11 images.

## build

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # catalog, parsing, safety rules, undo, tweak probing
```

the github actions workflow (`.github/workflows/lineagifier.yml`) runs both and uploads the debug apk.
