# krala

an ai chat app for android. kotlin, jetpack compose, material 3. dark red on black by default, wallpaper colors if you want them. works fully offline-first with any openai compatible provider, and remembers you across chats without the model deciding anything.

## features

**chat**
- streaming replies (sse) with a typing indicator, stop at any time, partial text is kept
- markdown: headings, lists, task lists, quotes, tables, links, inline code, code blocks with copy buttons
- multiple chats, drawer with full text search (titles and message content), rename, pin, delete, export (share, .md, .json)
- regenerate, edit a message (everything after it is replaced), retry failed replies
- per chat model picker and per chat system prompt, "use for new chats" default
- image attachments for vision models (downscaled jpegs, sent as data urls)
- share sheet: send text or an image to krala from any app, plus "ask krala" in the text selection menu
- incognito chats: no memory read or write, never synced, never in recall
- quick ask home screen widget with a floating answer sheet

**providers**
- add any openai compatible source: name, base url, api key, models (fetched from `/models`), default model, custom headers, context size, vision flag
- presets: google ai studio (gemini), groq, cerebras, mistral, openrouter, cloudflare workers ai (asks for your account id), nvidia, and the built in no key default `krala free` (`https://api.apisfreellm.com/v1`, key `free`, model `free-ai`)
- test connection with latency, http code and error details
- drag to reorder priority. requests go down the list on errors and rate limits; rate limited providers are skipped until their `retry-after` / `x-ratelimit-reset-*` time passes
- only `choices[0].message.content` is read from responses (`choices[0].delta.content` while streaming). reasoning, tool calls, usage and every other field are ignored
- api keys are typed in the app only and stored encrypted with an android keystore key

**memory** (all client side, the model never decides what is kept)
- every chat is stored in full in room (and synced if you turn sync on)
- when a chat ends (you switch away, start a new one, or the app goes to the background) a workmanager job asks the provider you picked in settings to write exhaustive notes for it. one entry per chat, keyed by chat id, named after the chat
- every new chat gets this block prepended to its first message, hidden in the ui:

  ```
  --krala tags--
  --memory--
  [chat name] notes of that chat
  --system prompt--
  the current system prompt
  --user instructions--
  custom instructions from settings
  --krala tags end--
  ```

  then your real first message. later messages use the normal history. empty sections are always included and tag matching is case insensitive (an echoed block is stripped from replies)
- when the entries pass 60% of the model's context, the oldest ones are merged into one condensed archive entry by the ai. merged entries are kept (marked archived), never deleted
- recall: before every message the raw stored chats are searched (sqlite fts5, optionally embeddings) and strong matches are added as a `--recall--` section to that request only
- passwords, api keys, tokens, card numbers (luhn checked), iban and cvv codes are scrubbed from notes before they are stored
- memory screen: view, edit, delete entries, see archived ones, forget everything

**/recall**
- type `/recall something` in the composer to search all past chats and memory entries. results are cards with the chat name, date and matching excerpt
- tap a card to open that chat, or "give to krala" to attach the excerpt to your next message as a `--recall--` section
- `/recall` alone shows the latest memory entries; typing `/` suggests the command

**accounts and sync** (supabase, email + password only, no oauth)
- anonymous guest by default, so everything works without signing up. guests upgrade in place and keep their chats
- optional cloud sync of chats, messages, memory, settings and the provider list through supabase postgres, with row level security on every table
- offline first: room is the source of truth, conflicts resolve by `updated_at` (newest wins, enforced by a trigger on the server too), deletes are tombstones, supabase realtime pushes changes between devices live
- api keys never leave the phone unless you turn on "sync api keys", then they are encrypted on the device with your passphrase (pbkdf2-sha256, 210k rounds, aes-256-gcm) before upload
- account screen: create account, sign in, change password, sign out, delete account and all cloud data
- biometric lock (fingerprint, face or screen lock)

**extras**: usage stats per provider (requests, success rate, rate limits, latency, token estimates, 30 day chart), backup and restore to a json file, english and russian ui (per app language), light / dark / amoled themes, predictive back, edge to edge, haptics.

## build

needs jdk 17+ and the android sdk (compile sdk 35, min sdk 26).

```
cp local.properties.example local.properties   # set sdk.dir
./gradlew assembleDebug                         # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest                     # unit tests
```

### cloud accounts (optional)

without supabase settings krala runs fully local and the account screen says so.

1. create a supabase project
2. run `supabase/migrations/20261128000000_krala_init.sql` (sql editor, or `supabase db push`)
3. authentication → sign in / providers: enable **email** and **anonymous sign-ins**
4. authentication → url configuration: add `krala://auth-callback` to the redirect urls so confirmation links open the app
5. put the project url and anon (publishable) key in `local.properties`:

   ```
   SUPABASE_URL=https://xxxx.supabase.co
   SUPABASE_ANON_KEY=...
   ```

   they end up in `BuildConfig`. the anon key is meant to ship in apps; row level security (`auth.uid() = user_id`) keeps every account to its own rows.

## layout

```
app/src/main/java/app/krala
  core/        pure logic, unit tested: krala tags block, memory budget, fallback planner,
               context window, rate limit parsing, sse + response parsing, secret scrubber,
               recall query building, markdown parser
  data/db      room entities, daos, fts5 tables + triggers (requery sqlite, so fts5 exists on every android version)
  data/provider presets, ktor client (streaming), provider repository, fallback router
  data/chat    chat engine (send, stream, stop, edit, regenerate), attachments, export
  data/memory  notes writer + worker, recall search, optional embeddings index
  data/sync    supabase client, auth, offline-first sync + realtime, sync worker
  data/backup  json backup / restore
  security     keystore vault, passphrase crypto
  ui/          compose screens: chat, drawer, composer + /recall, settings, personality,
               providers, provider editor, memory, account, usage, lock screen
supabase/migrations  tables, rls policies, last-write-wins trigger, realtime publication, delete_my_account()
```

## tests

`app/src/test` covers provider fallback ordering and error handling, rate limit header parsing, memory budgeting (the 60% trigger, oldest-first merging, archive handling), the context window, the krala tags block (layout, case insensitive parsing, stripping, recall injection), secret scrubbing, response parsing (only `choices[0]`), sse framing, recall queries and the markdown parser.
