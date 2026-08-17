# androdrop

Cross-device airdrop-style file transfer. Pair two devices with a 6-character
code (or a QR code), push files from one to the other, accept or decline on
the receiving end, download over a short-lived signed URL.

**Status:** steps 1–4 are done — Next.js app, Supabase schema/storage/RLS,
VAPID + web push route + service worker, and the PWA pages (pairing screen +
live incoming-transfer accept/decline overlay). Verified working end-to-end
locally. Android app and Vercel deploy are next, pending your go-ahead.

## Stack

- **Next.js 16** (App Router, TypeScript, Tailwind v4) — PWA + API backend
- **Supabase** — Postgres (`devices`, `transfers`), private Storage bucket,
  Realtime, RLS
- **Web Push (VAPID)** — for the installed-PWA / iOS Safari 16.4+ leg
- **Android app** (`/android`, not yet scaffolded) — Kotlin, Compose,
  Material You

## Architecture

```
devices           id, name, platform, pair_code, push_subscription, timestamps
transfers         id, sender_device_id, target_device_id, file_paths (jsonb),
                   status (pending|accepted|declined|expired), timestamps
```

- `devices` has RLS enabled with **no** anon policies — all reads/writes go
  through the API routes with the service-role key. Pair codes and push
  subscriptions never reach the client directly.
- `transfers` has RLS enabled with an anon **select** policy, so the browser's
  Supabase Realtime subscription (and the polling fallback) can see live
  status changes. The row data alone (storage keys, device ids) grants no
  file access — only a service-role-minted signed URL can download.
- Storage bucket `transfers` is private; every read is a signed URL created
  server-side, scoped to one transfer, expiring in 1 hour.
- A `pg_cron` job (`purge-expired-transfers-hourly`) hits an Edge Function
  every hour that deletes storage objects and marks rows `expired` once
  `expires_at` (24h after creation) has passed.

### API routes

- `POST /api/pair` — register/re-pair a device, returns a pairing code
- `GET /api/pair/:code` — resolve a pairing code to public device info
- `POST /api/transfer` — multipart upload; creates a pending transfer
- `GET /api/transfer/:id` — poll status (fallback if push isn't available)
- `POST /api/transfer/:id/respond` — accept (→ signed URLs) or decline (→
  deletes the files)
- `POST /api/push/subscribe`, `DELETE /api/push/subscribe` — web push
  subscription management

## Local development

1. `npm install`
2. Copy `.env.example` to `.env.local` and fill in the Supabase + VAPID
   values (a `.env.local` with the project's own values is already set up
   if you're continuing this session).
3. `npm run dev`, open http://localhost:3000

To see the full pairing + transfer flow, open the app in **two separate
browser profiles** (or one normal + one incognito window) — `localStorage`
is per-profile, so each one registers as its own device. Pair both, then use
"Send a file" on one with the other's pairing code or QR.

### Regenerating icons

`public/icons/*.png` are generated (no external deps) by:

```bash
node scripts/generate-icons.mjs
```

## Environment variables

See `.env.example`. `SUPABASE_SERVICE_ROLE_KEY`, `VAPID_PRIVATE_KEY` and
`VAPID_SUBJECT` are server-only secrets — never expose them with a
`NEXT_PUBLIC_` prefix.
