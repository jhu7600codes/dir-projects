import { NextResponse } from "next/server";
import { supabaseAdmin } from "@/lib/supabase/server";

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);
  const deviceId = body?.deviceId;
  const subscription = body?.subscription;

  if (typeof deviceId !== "string" || !deviceId) {
    return NextResponse.json({ error: "deviceId is required" }, { status: 400 });
  }
  if (!subscription || typeof subscription !== "object") {
    return NextResponse.json({ error: "subscription is required" }, { status: 400 });
  }

  const supabase = supabaseAdmin();
  const { error } = await supabase
    .from("devices")
    .update({ push_subscription: subscription })
    .eq("id", deviceId);

  if (error) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
  return NextResponse.json({ ok: true });
}

export async function DELETE(request: Request) {
  const body = await request.json().catch(() => null);
  const deviceId = body?.deviceId;

  if (typeof deviceId !== "string" || !deviceId) {
    return NextResponse.json({ error: "deviceId is required" }, { status: 400 });
  }

  const supabase = supabaseAdmin();
  const { error } = await supabase
    .from("devices")
    .update({ push_subscription: null })
    .eq("id", deviceId);

  if (error) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
  return NextResponse.json({ ok: true });
}
