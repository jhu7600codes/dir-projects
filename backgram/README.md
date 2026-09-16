# Backgram

An exteraGram / AyuGram plugin that backports newer Telegram protocol features
into builds that stopped receiving updates.

The first thing it goes after is **rich text messages** — the formatted,
instant-view-style messages newer official clients render. Those are not a UI
feature that can simply be drawn; they are a protocol feature
(`TL_iv.RichMessage`, constructor `0xbaf39d8b`) that the server only sends to a
client declaring a high enough MTProto layer.

## Why this is possible at all

The declared layer is not baked into native tgnet. It travels from Java:

```
ConnectionsManager.init(version, layer, apiId, …)  ->  native_init(…)
```

so a hook can change it before it reaches the handshake.

The obvious worry is that a newer layer makes the server speak a dialect the old
build cannot parse. Measured rather than assumed: comparing a 12.5.x build's
type table against layer 229, only six already-known types changed their wire
id:

| constructor  | type                     |
|--------------|--------------------------|
| `0x033ed001` | `connectedBot`           |
| `0x773f4e66` | `messageMediaPoll`       |
| `0x71e4ea58` | `reactionsNotifySettings`|
| `0x16a4b93c` | `storyItem`              |
| `0x2c63a72b` | `stories.editStory`      |
| `0x8f9e6898` | `stories.sendStory`      |

That is ~1.8% of the shared types. Everything else new to layer 229 is a
genuinely new type, which fails to deserialize rather than corrupting something
that already worked.

## The ordering problem (found in 0.1.0)

0.1.0 hooked `ConnectionsManager.init` and reported "nothing unknown seen yet",
which turned out to mean the hook never fired at all.

`init()` is not called from a late startup path a plugin can get ahead of. It is
called from the **`ConnectionsManager` constructor** (`ConnectionsManager.java:224`),
which runs the first time anything touches `getInstance(account)` — well before
the plugin engine loads. So the hook attaches to a method that will not be called
again this process.

0.2.0 handles that two ways:

- the diagnostics now say whether the hook fired, so "nothing unknown" can no
  longer be confused with "nothing was watching";
- a second switch, **Re-init connections**, calls `init()` again from the plugin,
  rebuilding every argument the constructor built, with the layer swapped.

That second part is the risky one — re-entering `native_init` on a live
connection is not something the app ever does — so it is off by default and
separate from the layer switch.

## Stage 1 — what is in here now

Stage 1 deliberately renders nothing. It:

1. hooks `ConnectionsManager.init` and rewrites the layer argument to 229,
2. hooks the `TLdeserialize` factories on `Message`, `MessageMedia`,
   `MessageEntity`, `RichText`, `PageBlock` and `Update`, and counts every
   constructor the build has no class for,
3. calls out the six changed-wire-format types separately, because those are the
   ones that can actually mis-read a stream,
4. watches specifically for `0xbaf39d8b` arriving.

Everything is behind a switch that is **off** by default.

### Testing it

1. Install `backgram.plugin` (share the file into the app and tap it, or drop it
   in the plugins folder).
2. Settings → Plugins → Backgram → turn on **Declare layer 229** *and*
   **Re-init connections**.
3. Restart the app fully.
4. Use it normally for a while — open channels that post formatted content.
5. Come back to the plugin settings and tap **Show what the server is sending**.

Expected wobble while it is on: polls and stories may mis-render, because those
are two of the six. If something is badly broken, turn the switch off and
restart; nothing is written to the account.

### What the readout decides

- `0xbaf39d8b` appears → the server will talk rich messages to this build, and
  Stage 2 is worth writing.
- it never appears, but lots of other unknown constructors do → the layer bump
  works and rich messages are gated on something else; the target changes.
- nothing unknown at all → read the counters above it. `init hook fired: 0` means
  the layer was never touched; `probes attached: 0/6` means hooking itself is
  broken and nothing else in the readout means anything.

## Stage 2 — not written yet, on purpose

The decoder is only sensible once the probe says what actually arrives. The good
news for it: a 12.5.1 build **already ships** `RichText` (16 subclasses),
`PageBlock`, and the whole `ArticleViewer` renderer. A rich message is built out
of exactly those pieces. So Stage 2 is not "write a renderer" — it is parse the
new wrapper, fill in the ~15 `RichText` and ~6 `PageBlock` types added since,
and hand the result to a renderer the app already has.

## Status

Stage 1 is written and unrun. It cannot be tested from here — it needs a phone.
