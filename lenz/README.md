# Lenz

a camera app for android, forked from [Open Camera](https://opencamera.org.uk/) (GPL-3.0).

package: `com.lenz.camera`. it installs next to the real Open Camera.

## what's different from Open Camera

- **mode bar like gcam**: swipe across the viewfinder (or the bar) to switch between
  Photo Sphere, Panorama, Night, Portrait, Photo, Video, Slow Motion and Time Lapse.
  modes your phone can't do are hidden
- **night mode**: Open Camera's low-light noise reduction, which stacks a burst of long exposures
- **photo**: multi-frame noise reduction (HDR+-style stacking) by default, JPEG quality 95
- **photo sphere**: follow the dots and it shoots by itself, then stitches a 360 photo
  (equirectangular with GPano metadata, so Google Photos shows it as a sphere). saved to DCIM/Lenz
- **Material You**: shutter, mode bar, settings and dialogs use your wallpaper colors (Android 12+)
- new name and icon, installs next to the real Open Camera (`com.lenz.camera`)

portrait only shows up if the phone has a camera extension for bokeh.
photo sphere places shots using the gyro, not feature matching, so seams can be a bit visible.

## build

```
./gradlew assembleRelease
```

the apk ends up in `app/build/outputs/apk/release/`.
