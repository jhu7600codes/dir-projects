// forwards krala's gemini requests (openai compatible endpoint) from an eu region, for places
// where google blocks the api. the user's own gemini key travels in x-krala-key; only signed in
// krala users (guests included) can call this (verify_jwt). the reply streams straight through.
import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const UPSTREAM = "https://generativelanguage.googleapis.com/v1beta/openai/";
const PASS = ["content-type", "retry-after", "x-ratelimit-remaining-requests", "x-ratelimit-reset-requests"];

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  const i = url.pathname.indexOf("/gemini-proxy/");
  const path = i >= 0 ? url.pathname.slice(i + "/gemini-proxy/".length) : "";
  if (!/^(chat\/completions|models|embeddings|audio\/[a-z]+)$/.test(path)) {
    return new Response(JSON.stringify({ error: { message: "unknown path" } }), { status: 404 });
  }
  // only signed in krala users (guests count): check the session with supabase auth
  const token = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  const who = token ? await fetch(Deno.env.get("SUPABASE_URL") + "/auth/v1/user", {
    headers: { "Authorization": "Bearer " + token, "apikey": Deno.env.get("SUPABASE_ANON_KEY") ?? "" },
  }) : null;
  if (!who || !who.ok) return new Response(JSON.stringify({ error: { message: "sign in to krala (guest is fine) to use the gemini proxy" } }), { status: 401 });
  const key = req.headers.get("x-krala-key") ?? "";
  if (!key) return new Response(JSON.stringify({ error: { message: "missing gemini key" } }), { status: 401 });
  const body = req.method === "GET" || req.method === "HEAD" ? undefined : await req.arrayBuffer();
  const upstream = await fetch(UPSTREAM + path + url.search, {
    method: req.method,
    headers: {
      "Authorization": "Bearer " + key,
      "Content-Type": req.headers.get("content-type") ?? "application/json",
      "Accept": req.headers.get("accept") ?? "*/*",
    },
    body,
  });
  const headers = new Headers();
  for (const h of PASS) { const v = upstream.headers.get(h); if (v) headers.set(h, v); }
  headers.set("Cache-Control", "no-cache");
  return new Response(upstream.body, { status: upstream.status, headers });
});
