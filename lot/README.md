# lot

a fake money lottery simulator for android. kotlin + jetpack compose + material 3, dark theme, single activity, no backend. everything is saved locally with datastore, all sounds are synthesized at runtime and all art is drawn with compose canvas.

## modes

- **player** - shop with 3 scratch ticket tiers (paper / shiny / golden, finger-scratch erase mask) and tv tickets (pick 5 of 30, watch a live draw show with a goofy host and a bouncing ball globe). wallet, stats, ticket history, and mom bails you out when you're broke.
- **host** - run the stand. customers with moods and patience walk up with orders, you sell, they scratch, you pay out (or pretend it lost). yell buttons are funny and dangerous for your reputation. end of day summary, profit goes to your wallet.
- **stealer** - hide in a bush, drag the binoculars to read tiny ticket numbers, grab winners with a timing minigame. more guards and faster customers every round, 3 lives.
- **bingo** - daub your card as numbers get called and yell bingo before three very fast grannies do.
- **dumpster** - rip open trash bags behind the stand, check thrown away tickets against tonight's numbers, fight off raccoons.
- **derby** - bet on one of five pigeons and watch them race (and get distracted).
- **slots** - lucky lot slots, you stop each reel yourself. 3 skulls takes your bet twice.
- **inspector** - quality control at the ticket factory. approve real tickets, reject fakes before the timer runs out.
- **ticket rain** - a lottery truck crashed. catch falling tickets in a basket, dodge bricks and skulls.
- **hi-lo** - guess if the next ball is higher or lower, grow the pot, cash out before you lose it.

### campaign

"the rise of a lottery legend": 5 chapters, 20 levels with story and goals that send you through every mode, ending in a boss fight against the lottery commissioner.

### extras

- item shop with 11 permanent upgrades (lucky sock, ticket sleeve, running shoes, pro binoculars, auto dauber...)
- 3 daily quests with coin rewards
- a news ticker on the home screen
- a rank title that grows with your badges

### tickets

8 scratch tickets across 3 kinds: match-3 (pocket change, paper, shiny, golden, diamond), numbers (number cruncher, big numbers) and the cursed ticket (3 skulls takes coins from you). plus tv tickets and a free daily wheel.

### claiming prizes

winning tickets don't pay out right away. you sign the back, walk to the stand in a 2d platformer (puddles, cones, pigeons and holes all damage the ticket), maybe fight off a ticket thief in a tug of war, then argue with a cashier who points out every single defect. or take the lazy cash-out for 70%.

### coin "value"

settings has a currency picker that shows what your coins are "worth" in usd, eur, rub, shawarmas, potatoes and more. it's fake, nothing can be cashed out.

67 badges across all modes, unlock popup, tutorials on first launch of each mode, sound + haptics toggles in settings.

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
