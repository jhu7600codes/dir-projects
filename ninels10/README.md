# NineLS10

the ios 9 lock screen on ios 10 (10.0 - 10.3.x). backport of [NineLS](https://github.com/Minh-Ton/NineLS) by Minh Ton (MIT, see `LICENSE.ninels`).

- slide to unlock: shimmering text + chevron, swipe right to unlock (the today page is emptied out and becomes the unlock page)
- hides "press home to unlock" and the page dots
- ios 9 notifications: no platters, thin separators, the wallpaper blurs when you have notifications
- music: blurred background with big artwork while something is playing
- ios 9 lock sound
- settings pane (Settings > NineLS10)

## what changed from the original

the original targets the ios 13/14 CoverSheet classes (`CS*`, `MRU*`, notification grouping), none of which exist on ios 10, so the hooks were rewritten for `SBDashBoard*`. every hook group is only turned on if its classes exist, so a mismatch on some ios 10 build disables that feature instead of crashing springboard.

dropped since ios 10 doesn't have them: notification grouping fixes, dnd banner hiding, home bar / face id / quick actions hiding, light/dark mode, moving the media controls around (the stock ios 10 player stays). cephei and libcolorpicker were dropped too, the color is a hex text field.

## install

grab the deb from [`dist/`](dist) and install it with filza or `dpkg -i`, then respring. it's built for armv7 + arm64, ios 10.0 - 10.3.x.

## build

```
make package FINALPACKAGE=1
make package install THEOS_DEVICE_IP=<device ip>
```
