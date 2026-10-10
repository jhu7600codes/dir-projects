# Simulasim

A pocket full of simulators for Android, in Kotlin and Jetpack Compose.
Grab the APK from `dist/simulasim-debug.apk`.

## Sims

- **Rugpull sim**: two modes.
  - *market*: 5 live memecoins with random pumps, FUD, and rugs. Buy, ape, sell,
    and watch your bags go to zero. If you go broke you can ask mom for $1000.
  - *launch a coin*: seed a liquidity pool, keep 50% of supply, then shill, pay
    influencers, run bot armies and fake audits. Rug before suspicion hits 100 or
    you get exposed. The pool is a real constant-product AMM (x*y=k), so buys and
    sells actually move the price. A big rug might get you fined.
- **Teacher sim**: a full 4-week term. 12 students, each with a personality
  (nerd, clown, sleepy, rebel, phone addict, chatterbox, normal), mood and
  knowledge. 5 periods a day, 8 actions per period: teach, fun activity, pop quiz,
  joke, yell, video, or tap a kid to call on / handle / praise / move / send out.
  Random events (principal visits, fire alarms, angry parent emails, wasps...).
  Breaks between periods (coffee, gossip, car nap, or a homework grading
  minigame), evening choices, and a Friday exam with a principal review. Get
  fired, burn out, go viral for snapping, or become Teacher of the Year.
- **Shakelight sim**: shake twice (chop chop) to toggle the flashlight. Steady,
  strobe (1-12 Hz) and SOS modes, adjustable sensitivity, a live g-force meter,
  and a screen-light fallback for phones without a flash.
- **Hacker sim**: tap to "type" movie hacker code. ACCESS GRANTED (sometimes).
- **Bubble wrap sim**: pop bubbles with haptics, lifetime counter.

## Building

```
./gradlew :app:assembleDebug
```

Same toolchain as `spy/`: AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06,
compileSdk/targetSdk 34, minSdk 26. Needs an Android SDK with platform 34 and
build-tools 34 (point `ANDROID_HOME` or `local.properties` at it).

Only permission is `VIBRATE`. The flashlight uses `CameraManager.setTorchMode`,
which doesn't need the camera permission.
