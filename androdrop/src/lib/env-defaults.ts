/**
 * Hardcoded fallbacks so the app runs on a fresh Vercel deploy with zero
 * dashboard configuration — used only when the matching env var isn't set.
 * These are this project's own androdrop Supabase project + VAPID keypair
 * (not third-party secrets). Set the real env vars in Vercel whenever
 * that's convenient; nothing here needs to change to do that, since
 * `process.env.X` always wins over these when present.
 */
export const ENV_DEFAULTS = {
  NEXT_PUBLIC_SUPABASE_URL: "https://jwzahwqqrxyyaouawgva.supabase.co",
  NEXT_PUBLIC_SUPABASE_ANON_KEY: "sb_publishable_cJ2vgwiKI7C3ltGizttRUg_KjJmInt8",
  SUPABASE_SERVICE_ROLE_KEY:
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imp3emFod3Fxcnh5eWFvdWF3Z3ZhIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4Njk0MzExMywiZXhwIjoyMTAyNTE5MTEzfQ.k8ZIqh3cWcqvMTl0P8iUuBw_3fGi1tsrVhZF3FcGIZU",
  SUPABASE_STORAGE_BUCKET: "transfers",
  NEXT_PUBLIC_VAPID_PUBLIC_KEY:
    "BGcL2aLuyhQj4aLYrQ-fR02nt7olYFguJ8ssr18EVHpWFkzST-mfiT7M807C6RyCK1wx5dUxmefdvqzuzpOxD0Q",
  VAPID_PRIVATE_KEY: "iEV0N1g321kq_rx88BiJm-OER8-c7STCuM8h3or8ms0",
  VAPID_SUBJECT: "mailto:vancedjrplus@gmail.com",
} as const;
