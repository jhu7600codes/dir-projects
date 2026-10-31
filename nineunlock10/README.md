# NineUnlock10

ios 9 slide to unlock on ios 10 (10.0 - 10.3.x). backport of [NineUnlock](https://github.com/Minh-Ton/NineUnlock) by Minh Ton (MIT, see `LICENSE.nineunlock`).

- slide to unlock: shimmering text + chevron, swipe right to unlock (the today page is emptied out and becomes the unlock page)
- hides "press home to unlock" and the page dots
- ios 9 lock sound
- settings pane (Settings > NineUnlock10)

## what changed from the original

the original targets the ios 13/14 CoverSheet classes (`CS*`), none of which exist on ios 10, so the hooks were rewritten for `SBDashBoard*`. every hook group is only turned on if its classes exist, so a mismatch on some ios 10 build disables that feature instead of crashing springboard.

dropped since ios 10 doesn't have them: home bar / quick actions hiding and the NineMusic hooks. cephei and libcolorpicker were dropped too, the color is a hex text field.

## build

```
make package FINALPACKAGE=1
make package install THEOS_DEVICE_IP=<device ip>
```
