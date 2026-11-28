// deletes the calling user's auth account. every krala table references auth.users with
// on delete cascade, so all of their cloud data goes with it.
import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return new Response("method not allowed", { status: 405 });
  const token = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  if (!token) return new Response(JSON.stringify({ error: "not signed in" }), { status: 401 });
  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data, error } = await admin.auth.getUser(token);
  if (error || !data.user) return new Response(JSON.stringify({ error: "not signed in" }), { status: 401 });
  const { error: delError } = await admin.auth.admin.deleteUser(data.user.id);
  if (delError) return new Response(JSON.stringify({ error: delError.message }), { status: 500 });
  return new Response(JSON.stringify({ deleted: true }), { headers: { "Content-Type": "application/json" } });
});
