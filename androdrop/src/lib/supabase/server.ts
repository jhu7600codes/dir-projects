import "server-only";
import { createClient } from "@supabase/supabase-js";
import type { Database } from "./database.types";
import { ENV_DEFAULTS } from "@/lib/env-defaults";

// Service-role client for use in API routes only. This key bypasses RLS,
// so it must never reach the browser — `server-only` enforces that at
// build time.
let client: ReturnType<typeof createClient<Database>> | null = null;

export function supabaseAdmin() {
  if (client) return client;

  const url = process.env.NEXT_PUBLIC_SUPABASE_URL || ENV_DEFAULTS.NEXT_PUBLIC_SUPABASE_URL;
  const serviceRoleKey =
    process.env.SUPABASE_SERVICE_ROLE_KEY || ENV_DEFAULTS.SUPABASE_SERVICE_ROLE_KEY;

  client = createClient<Database>(url, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  return client;
}

export const STORAGE_BUCKET =
  process.env.SUPABASE_STORAGE_BUCKET || ENV_DEFAULTS.SUPABASE_STORAGE_BUCKET;
