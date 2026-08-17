import { NextResponse } from "next/server";
import { supabaseAdmin } from "@/lib/supabase/server";
import { generatePairCode } from "@/lib/pair-code";
import { generateDeviceName } from "@/lib/device-name";
import type { DeviceRow, PairResponse, Platform } from "@/lib/types";

const PLATFORMS: Platform[] = ["web", "ios", "android"];

function toResponse(row: DeviceRow): PairResponse {
  return {
    deviceId: row.id,
    name: row.name,
    platform: row.platform,
    pairCode: row.pair_code,
    createdAt: row.created_at,
  };
}

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);
  if (!body || typeof body !== "object") {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const { deviceId, name, platform } = body as {
    deviceId?: string;
    name?: string;
    platform?: string;
  };

  if (!platform || !PLATFORMS.includes(platform as Platform)) {
    return NextResponse.json(
      { error: `platform must be one of: ${PLATFORMS.join(", ")}` },
      { status: 400 },
    );
  }

  const supabase = supabaseAdmin();

  // Re-pairing an existing device: just touch it and optionally rename.
  if (deviceId) {
    const update: { last_seen_at: string; name?: string } = {
      last_seen_at: new Date().toISOString(),
    };
    if (name && name.trim()) update.name = name.trim().slice(0, 60);

    const { data, error } = await supabase
      .from("devices")
      .update(update)
      .eq("id", deviceId)
      .select()
      .maybeSingle();

    if (error) {
      return NextResponse.json({ error: error.message }, { status: 500 });
    }
    if (data) {
      return NextResponse.json(toResponse(data as DeviceRow));
    }
    // deviceId not found (e.g. stale localStorage) — fall through and create fresh.
  }

  const deviceName = name && name.trim() ? name.trim().slice(0, 60) : generateDeviceName();

  // pair_code has a unique constraint; retry a couple of times on collision.
  for (let attempt = 0; attempt < 5; attempt++) {
    const { data, error } = await supabase
      .from("devices")
      .insert({
        name: deviceName,
        platform,
        pair_code: generatePairCode(),
      })
      .select()
      .single();

    if (!error) {
      return NextResponse.json(toResponse(data as DeviceRow), { status: 201 });
    }
    if (error.code !== "23505") {
      return NextResponse.json({ error: error.message }, { status: 500 });
    }
  }

  return NextResponse.json({ error: "Could not allocate a pair code, try again" }, { status: 500 });
}
