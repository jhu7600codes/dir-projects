# androdrop share-sheet hook (LSPosed module) — EXPERIMENTAL

**Read this before enabling the module.** It builds a real, valid APK
(`:xposed:assembleDebug` is verified in CI-equivalent conditions — see the
root README), but the hook itself has **never been tested against a real
device**. There was no rooted device or Xposed-capable emulator available
while building this, so what follows is a best-effort, carefully-reasoned
implementation, not a verified one. Treat it as a starting point to debug on
your own LineageOS + microG install, not a finished feature.

## What it does

Android's native share sheet (`ResolverActivity`/`ChooserActivity`) lists
apps by calling `PackageManager.queryIntentActivities()`. androdrop already
shows up in that list normally, via the plain `ACTION_SEND` intent filter in
`:app`'s manifest — no root needed for that part.

This module goes one step further: it hooks that same `queryIntentActivities`
call at the System Framework level (LSPosed scope `android`, which patches
classes in the Zygote so the patch is present in every process forked
afterwards) and, whenever the query is for `ACTION_SEND`/`ACTION_SEND_MULTIPLE`,
inserts androdrop at **position 0** of the results — so it's the first
option, not somewhere in an alphabetical list of forty apps.

## Why this hook point, not Nearby Share

The original ask was to integrate with the phone's system "Quick Share" —
but that's a Google Play Services feature (`com.google.android.gms`), and
this device runs LineageOS + microG, which doesn't include GMS. There's no
Nearby Share surface to inject into on this setup. Pure AOSP (what
LineageOS's share sheet is built on) has no separate "nearby devices" row at
all — so the closest, most honest equivalent is pinning androdrop to the top
of the one share surface that *does* exist: the standard chooser.

This also happens to be a *more* stable target than Google's internals would
have been: `PackageManager.queryIntentActivities()` is a public, documented
AOSP API that's been stable for over a decade, unlike the private fields of
`ResolverActivity`/`ChooserListAdapter`, which get substantially reworked
almost every Android release.

## Setup

1. `./gradlew :xposed:assembleDebug`, install `xposed/build/outputs/apk/debug/xposed-debug.apk`.
2. Open LSPosed Manager → Modules → enable "androdrop share sheet hook".
3. Scope: the module ships scoped to `android` (System Framework) already
   (`res/values/strings.xml`'s `xposed_scope` array). Some LSPosed versions
   still want you to confirm this scope in the Manager UI.
4. **Reboot** — System Framework-scoped modules only take effect after a
   full reboot (LSPosed patches the Zygote, which only re-forks on boot).
5. Share something from any app. androdrop should be the first icon.

## If it doesn't work

Open LSPosed Manager's log viewer and filter for `androdrop-xposed`. Two
outcomes:

- **`failed to hook queryIntentActivities on this build`** — the method
  signature (or even the containing class name,
  `android.app.ApplicationPackageManager`) has moved on your LineageOS
  version. This is the expected failure mode, not a bug — AOSP internals
  drift across releases and OEM/ROM patches. Options:
  - Pull `framework.jar` off the device (`adb pull /system/framework/framework.jar`)
    and inspect it with `jadx` for the actual method that builds the
    resolver's candidate list on your exact build, then adjust
    `ShareSheetHook.kt`'s `findAndHookMethod` call accordingly.
  - Check whether your LineageOS version still routes through
    `ApplicationPackageManager` at all vs. a different implementation class.
- **No log line at all** — the module isn't loaded/scoped correctly, or the
  reboot didn't take. Re-check step 3–4.

## Known limitations

- **Untested.** Everything above is inference from public AOSP source
  structure, not verification against a booted device.
- **Version-fragile by nature.** Any hook into framework internals can break
  on the next LineageOS update. The `try/catch` around the hook installation
  means a broken hook logs and no-ops rather than crashing `system_server`,
  but it will silently stop pinning androdrop until you re-diagnose it.
- Only handles `ACTION_SEND`/`ACTION_SEND_MULTIPLE`. Doesn't touch direct-share
  (contact-shortcut) rows, which are a separate, even more implementation-specific
  mechanism.
- Uses the classic Xposed API (`de.robv.android.xposed:api:82` from
  `https://api.xposed.info/`) rather than the newer `io.github.libxposed`
  API, for broader compatibility with older LSPosed/Xposed-adjacent
  frameworks — LSPosed supports both.
