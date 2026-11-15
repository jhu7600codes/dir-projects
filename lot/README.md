# lot

a fake money lottery simulator for android. kotlin + jetpack compose + material 3, dark theme, single activity, no backend. everything is saved locally with datastore, all sounds are synthesized at runtime and all art is drawn with compose canvas.

## modes

- **player** - shop with 3 scratch ticket tiers (paper / shiny / golden, finger-scratch erase mask) and tv tickets (pick 5 of 30, watch a live draw show with a goofy host and a bouncing ball globe). wallet, stats, ticket history, and mom bails you out when you're broke.
- **host** - run the stand. customers with moods and patience walk up with orders, you sell, they scratch, you pay out (or pretend it lost). yell buttons are funny and dangerous for your reputation. end of day summary, profit goes to your wallet.
- **stealer** - hide in a bush, drag the binoculars to read tiny ticket numbers, grab winners with a timing minigame. more guards and faster customers every round, 3 lives.

26 badges across all modes, unlock popup, tutorials on first launch of each mode, sound + haptics toggles in settings.

## install

grab `apk/lot-debug.apk`, open it on the phone and allow "install unknown apps" for your browser / file manager. or with adb:

```
adb install -r apk/lot-debug.apk
```

## build

```
./gradlew assembleDebug
```

output: `app/build/outputs/apk/debug/app-debug.apk`. needs jdk 17+ and the android sdk (platform 34).
