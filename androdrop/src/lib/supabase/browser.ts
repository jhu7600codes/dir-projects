"use client";
import { createClient } from "@supabase/supabase-js";
import type { Database } from "./database.types";

// Anon/publishable-key client for the browser. RLS restricts this to
// read-only access on `transfers` (used for Realtime + status polling);
// `devices` has no anon policies at all, so writes and lookups always go
// through the API routes instead.
let client: ReturnType<typeof createClient<Database>> | null = null;

export function supabaseBrowser() {
  if (client) return client;

  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;

  if (!url || !anonKey) {
    throw new Error("Missing NEXT_PUBLIC_SUPABASE_URL or NEXT_PUBLIC_SUPABASE_ANON_KEY.");
  }

  client = createClient<Database>(url, anonKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  return client;
}
