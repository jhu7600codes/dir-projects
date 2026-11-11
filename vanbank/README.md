# VANBank

A fake bank simulator for Android. Everything -- accounts, cards, transfers,
loans, savings goals, bills, and the DIR AI Assistant's payment requests --
runs entirely on-device against a local Room database. No real payment or
banking APIs are involved anywhere in this app.

## Stack

Kotlin, Jetpack Compose (Material 3, overridden with a custom dark/black
palette), Room, WorkManager, DataStore, Navigation Compose, MVVM. No Hilt/Dagger
-- a small hand-rolled `AppContainer` + `ViewModelProvider.Factory` wires
everything up (see `di/`).

## Module layout

This project is split into two Gradle modules with very different build
requirements, the same pattern used elsewhere in this repo (see `orbitalsurf/`):

- **`:core`** -- a plain `org.jetbrains.kotlin.jvm` module, no Android
  dependency at all. Every piece of VANBank's actual math and business logic
  that doesn't need an Android runtime lives here: PBKDF2 password hashing,
  DIR card-number generation (prefix `8` + a real Luhn checksum) and VANBank
  account-number generation, loan amortization scheduling, a deterministic
  loan-rate quote engine, statement totals, and budget category aggregation.
  It's unit tested (40 tests) and builds with nothing but a JDK:

  ```
  ./gradlew :core:test
  ```

- **`:app`** -- the Android application module: Compose UI, Room database,
  WorkManager bill scheduling, notifications. It depends on `:core` and
  requires a real Android SDK (`ANDROID_HOME` / `local.properties`) to
  configure at all, since AGP resolves the SDK during Gradle's configuration
  phase before any task runs. This module was written and organized
  carefully but **could not be compiled in the sandbox this was built in**
  (no Android SDK available there) -- build it in Android Studio or any
  environment with the SDK installed:

  ```
  ./gradlew :app:assembleDebug
  ```

### `:app` package layout

```
data/
  local/          Room entities, DAOs, VanBankDatabase, TypeConverters
  repository/     One repository per feature area; all money movement goes
                   through Room transactions (withTransaction) so a balance
                   never changes without a matching transaction record
  prefs/          DataStore-backed SessionManager (just the signed-in user id)
di/               AppContainer + VanBankViewModelFactory (no DI framework)
notifications/    NotificationHelper -- AI request + bill-pay notifications
work/             BillPayWorker (WorkManager, 15-minute sweep)
ui/
  theme/          Dark palette, tabular/monospace numeral styles, DIR wordmark
  components/     Shared composables: card visuals, panels, buttons, chips
  navigation/     NavHost + routes
  auth/ home/ transfer/ ai/ transactions/ statements/
  loans/ vaults/ budgeting/ billpay/ admin/
                   One screen + ViewModel per feature
```

## Features

- **Auth** -- username/password, PBKDF2-hashed (never stored in plaintext)
- **Accounts** -- checking + savings per user, unique 10-digit account
  numbers, starter balance on signup
- **Cards** -- debit + credit, issued on VANBank's own **DIR** network
  (card numbers start with `8`, the way Visa starts with `4` and Mastercard
  with `5`); freeze/unfreeze per card; credit cards carry a limit + running
  balance
- **Transfers** -- between your own accounts, to another VANBank user (by
  `@username` or account number), or straight to a DIR card number
- **Transaction history** -- categorized, filterable, tabular-numeral amounts
- **Statements** -- date-range summaries (totals in/out, net, category
  breakdown) generated in-app
- **Loans** -- request a loan, get a deterministic risk-based rate quote,
  and a full amortization schedule; pay installments one at a time
- **Savings vaults** -- named goals with a target amount and running progress
- **Budgeting** -- category spend breakdown as a pie chart (YCharts)
- **Bill pay** -- recurring bills that auto-deduct on schedule via a
  WorkManager periodic worker
- **Admin/dev panel** -- spawn test users, reset any account's balance,
  manually trigger a DIR AI Assistant payment request

### DIR AI Assistant

The signature feature: an AI assistant entity that sends legitimate-looking
payment requests for completed work (e.g. "Research summary compilation --
₽240"), presented as an invoice/approval card rather than a random popup.
Approving deducts the balance and logs a completed transaction; declining
logs the request as declined with no deduction. Requests can also surface as
a real Android notification (`POST_NOTIFICATIONS`, Android 13+) so they're
visible even when the app is backgrounded.

## Design

Black (`#0A0A0A`) as the primary background, near-black panels
(`#141C2B`–`#1B2536`), white/light text, and an electric-blue DIR accent
(`#3D5AFE`) for primary actions and the wordmark. Debit cards render a dark
navy-to-charcoal gradient; credit cards a warm orange/rust gradient. Card
numbers and every currency amount use monospace/tabular numerals. Currency
is ₽ throughout, formatted from whole minor units (kopecks) so balances
never drift from floating-point rounding.
