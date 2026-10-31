# SlideToUnlock

ios 9 style slide to unlock for ios 10 (10.0 - 10.3.x), jailbroken.

- swipe right on the lock screen, the content follows your finger 1:1
- let go past ~35% of the screen or flick it and it unlocks (or shows the passcode)
- let go early and it springs back
- "press home to unlock" becomes "slide to unlock"
- swipe left (camera) and vertical swipes still work, swipe right replaces the today view

## install

grab the deb from [`dist/`](dist) and install it with filza or `dpkg -i`, then respring.

## build

needs [theos](https://theos.dev) with an ios 10 sdk:

```
make package
make package install THEOS_DEVICE_IP=<device ip>
```
