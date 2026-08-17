import { NextResponse } from "next/server";
import { supabaseAdmin } from "@/lib/supabase/server";
import { normalizePairCode } from "@/lib/pair-code";
import type { DeviceRow } from "@/lib/types";

export async function GET(_request: Request, context: RouteContext<"/api/pair/[code]">) {
  const { code } = await context.params;
  const normalized = normalizePairCode(code);

  if (!normalized) {
    return NextResponse.json({ error: "Invalid pair code" }, { status: 400 });
  }

  const supabase = supabaseAdmin();
  const { data, error } = await supabase
    .from("devices")
    .select("id, name, platform")
    .eq("pair_code", normalized)
    .maybeSingle();

  if (error) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
  if (!data) {
    return NextResponse.json({ error: "No device found for that code" }, { status: 404 });
  }

  const device = data as Pick<DeviceRow, "id" | "name" | "platform">;
  return NextResponse.json({
    deviceId: device.id,
    name: device.name,
    platform: device.platform,
  });
}
