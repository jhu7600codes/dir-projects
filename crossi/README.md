# CROSSI

crossword game for android, like the nyt crossword but not just daily and more
fun. kotlin + jetpack compose, material 3 / material you, dark by default,
offline, english + russian.

full design is in [plan.md](plan.md).

## status

- [x] m0 skeleton: theme, navigation, en/ru strings, placeholder screens
- [ ] m1 generator
- [ ] m2 dictionaries
- [ ] m3 game screen + keyboards
- [ ] m4 finish flow + power ups
- [ ] m5 persistence
- [ ] m6 modes (levels, free roam, daily)
- [ ] m7 bosses
- [ ] m8 polish

## build

needs jdk 17+ and the android sdk (platform 35). put `sdk.dir=...` in
`local.properties` or set `ANDROID_HOME`.

```
./gradlew :app:assembleDebug        # apk in app/build/outputs/apk/debug
./gradlew :app:testDebugUnitTest    # unit tests (generator + game, plain jvm)
```
