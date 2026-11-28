# lineagifier

an android app (kotlin, compose) that gets the stock aosp android 13 image in the ios build of twoyi as close to lineageos as root allows. twoyi's image comes rooted with kernelsu, so lineagifier runs everything through `su`. it never edits /system or the framework files: system apps go in as kernelsu modules, everything else is settings, overlays and roles, and all of it is logged for undo.

## the config app

the home screen has a card per feature, each with its own switch or page:

- **lineage apps**: install etar and the closest prebuilts, make them the default
- **stock apps**: disable or remove stock apps once a replacement is in, plus "restore everything"
- **microG**: gmscore + signature spoofing
- **tweaks**: dark mode, animations, navigation mode, accent, icon shape
- **quick settings**: lineage 20's quick settings panel

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

every change is written to `files/undo-log.json` **before** its command runs, with the old value. "restore everything" undoes them newest first:

- removed: `cmd package install-existing --user 0 <pkg>`
- disabled: `pm enable --user 0 <pkg>`
- settings: `settings put` the old value, or `settings delete` if there wasn't one
- overlays: back to their old state
- roles: the old holder gets the role back
- kernelsu modules lineagifier installed: `ksud module uninstall <id>` (gone after the next reboot)
- extra apps lineagifier installed (fakegapps): `pm uninstall`
- appops (the overlay permission): back to the old mode

newest first means a stock launcher is re-enabled before home goes back to it. replacement apps you picked yourself stay installed.

## tweaks

each one is probed first (`cmd -l`, `cmd overlay list`, `settings get`), then applied, then read back to check it stuck.

| tweak | how | needs |
|---|---|---|
| dark mode | `cmd uimode night yes/no/auto` | `uimode` service |
| animation speed | `settings put global window/transition/animator_*_scale` | shell's WRITE_SECURE_SETTINGS |
| navigation mode | `cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.*` | navbar overlays on the image |
| accent color | android 12+: merges `system_palette`/`accent_color` into `secure theme_customization_overlay_packages` (monet). older: `com.android.theme.color.*` overlays | the setting, or the overlays |
| icon shape | android 12+: `adaptive_icon_shape` in the same json. older: `com.android.theme.icon.*` overlays | shape overlays on the image |

## microG

1. gmscore 0.3.18 + companion (and optionally gsfproxy) come from microg's own repo, `microg.org/fdroid/repo`. the sha256s are pinned from microg's signed `index-v1.jar`, checked on 2026-11-28: signed by microg's repo key `9bd06727…14165`, signature valid, digests chain from the .SF to the json
2. they go into `/system/priv-app` through a kernelsu module lineagifier builds on the device. the privapp permission list is generated from what each apk asks for and what this exact image marks privileged, so a missing entry can't cause a bootloop. native libs are unpacked next to the apk, since system apps don't get them extracted
3. signature spoofing comes from **neozygisk** + **vector** (JingMatrix's maintained lsposed fork) + **fakegapps**, the usual root setup. these only exist as github releases, so lineagifier gets the latest one from the github api on the device and refuses anything github hasn't published a sha256 for. if you already have a zygisk or lsposed module it's left alone
4. then: reboot, open vector from its notification, enable fakegapps for "system framework", reboot again. lineagifier checks spoofing by comparing gmscore's reported signature with the google certificate gmscore itself carries (sha1 `38918a45…5788`)

it refuses to run if real google services or another microg is installed. framework hooks can stop an image from booting: if twoyi won't boot afterwards, remove the modules from kernelsu's safe mode if twoyi gives you one, otherwise the container may need a reset.

## quick settings

a copy of lineage 20's quick settings panel, built from lineage-20.0's systemui source: 80dp tiles with 28dp corners, 2 columns × 4 rows per page, a 4-tile quick row, the 48dp rounded brightness slider with lineage's auto-brightness button, the footer (build number, page dots, edit) and the settings + power buttons. it uses the same brightness gamma curve as aosp (settingslib `BrightnessUtils`).

it doesn't copy colors or icons by hand. it reads them from your device's systemui at runtime: the accent and `offStateColor` from `Theme.SystemUI.QuickSettings`, the animated tile icons (`qs_*_icon_on/off`), the labels and the "On/Off/Unavailable" text (`tile_states_*`). so it follows your accent, dark/light mode and language like the real one. lineage's own tiles use lineage's vector icons (copied into `res/drawable/los_*.xml` with their apache 2.0 headers).

tiles: internet, bluetooth, flashlight, do not disturb, alarm, airplane mode, auto-rotate, battery saver, location, dark theme, night light, plus lineage's caffeine (same 5/10/30 min/∞ cycling and long press as lineage's CaffeineTile), heads up, sync, ambient display, aod and vpn. tap toggles, long press opens the matching settings page. tiles the image can't do show up dimmed as "unavailable", like on lineage.

android's own pull-down belongs to systemui and can't be replaced without framework changes. so the panel opens from a thin handle on the right edge, just under the status bar (tap it or swipe down). swipe down inside the panel to expand it, swipe up or tap outside to close.

not included: reading mode, livedisplay, profiles and powershare (they need lineage's hardware/sdk services). controls, wallet, cast, screen record, hotspot and usb tethering don't apply to a twoyi container.

## what's approximated, what's missing

**approximated**
- lineage's apps: only etar is the real thing (upstream, not lineage's fork). gallery, recorder, clock, calculator, files, contacts, dialer and messaging can only get fossify stand-ins
- clock, calculator, contacts, dialer, messaging, files: lineage's versions are forks of the aosp apps you already have, with the same package ids, so they can't be installed over the system ones without lineage's platform key
- launcher: aosp launcher3 is what trebuchet is forked from, but trebuchet's extras (icon packs, hidden apps, protected apps, grid options) aren't there
- quick settings: a lookalike panel opened from a handle, not systemui's own shade
- microg: spoofing comes from xposed hooks (fakegapps), not lineage-for-microg's built-in framework patch
- accent color / icon shape: done through the same theme setting the aosp wallpaper picker uses, so systemui may change it again if you change wallpaper

**missing**
- the whole lineage sdk: privacy guard, trust interface, lineageparts, livedisplay, profiles, styles beyond what aosp has
- lineage's status bar options (clock position, battery style, network traffic), lockscreen options, power menu options, button/gesture customisation, long-press actions: all lineage systemui/framework code
- seedvault: possible in principle as a priv-app module, but it also needs the backup transport set up in the framework config, which isn't done yet
- lineage's updater and recovery
- default camera / gallery / calendar: android has no role for these, so pick them once from the "open with" chooser

## twoyi notes

- the target is the ios build of twoyi: android 13 (sdk 33, `twoyi13_native` userdebug), kernel 5.10, kernelsu (lkm mode), selinux disabled
- give lineagifier root once in kernelsu's superuser tab. the home screen shows "running as root" when it works
- twoyi handles the iphone notch itself, so lineagifier leaves the status bar size alone
- twoyi's developer options crash, so there's no wireless debugging and no shizuku. that's why lineagifier uses su

## what's been tested

on the aosp 13 emulator image (`system-images;android-33;default;x86_64`, sdk 33, userdebug):

**0.1 (through shizuku as uid 2000)**: shizuku grant, package detection, etar sha256 + install, dark mode / animations / gestural nav / teal accent, removing gallery3d and disabling calendar once replaced (launcher and camera stayed locked), the dialer role gate, and "restore everything" putting all 14 changes back. two real findings: aosp 13 has no `cmd role get-role-holders` (holders come from `dumpsys role`), and `cmd role add-role-holder` times out on a slow device but applies a few seconds later (lineagifier polls the real holders).

**0.2 (root)**: the emulator's own su only serves adb, so a small test relay stood in for kernelsu's su (apps run `su -c`, a root loop runs the command). tested: root detection, the config home, the quick settings panel (quick row, expanded page, systemui icons/labels/states, toggles for airplane/dark/etc. changing the real system, battery saver dimmed while charging like aosp, lineage's tiles with caffeine counting down), and a crash fix: a timed-out `su` call used to kill the app.

**not tested**: anything inside twoyi itself, real kernelsu, the microg install (the emulator has no kernelsu and this sandbox can't reach github's api, so neozygisk/vector/fakegapps downloads and the module install are untested), night light (not available on the emulator), the in-app download over the network (the downloader is tested on the jvm against f-droid instead: `LINEAGIFIER_NET_TEST=1 ./gradlew testDebugUnitTest`), the launcher swap end to end.

## build

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # catalog, parsing, safety rules, undo, tweak probing
```

the github actions workflow (`.github/workflows/lineagifier.yml`) runs both and uploads the debug apk.
