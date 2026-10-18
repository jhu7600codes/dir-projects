# Lenz

a camera app for android, forked from [Open Camera](https://opencamera.org.uk/) (GPL-3.0).

package: `com.lenz.camera`. it installs next to the real Open Camera.

## what's different from Open Camera

- new name and icon
- Material You: the shutter button and accents use your wallpaper colors (Android 12+)
- NR (multi-frame noise reduction, HDR+-style stacking) is the default photo mode when the camera supports it
- default JPEG quality is 95 instead of 90

## already in Open Camera

HDR, DRO, panorama, slow motion (high-speed video), time lapse, expo/focus bracketing, fast burst, RAW/DNG,
manual ISO/exposure/focus/white balance, and the camera extensions your phone supports (night, bokeh, etc).

## build

```
./gradlew assembleRelease
```

the apk ends up in `app/build/outputs/apk/release/`.
