import { NextResponse } from "next/server";
import { supabaseAdmin } from "@/lib/supabase/server";
import type { DeviceRow, IncomingTransferSummary, TransferRow } from "@/lib/types";

/**
 * List pending transfers targeting a device. The web PWA gets live updates
 * for free via Supabase Realtime (anon-readable `transfers` table), but a
 * plain background poller — like the Android foreground service — has no
 * such channel, so it polls this instead of embedding the Supabase anon key.
 */
export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const deviceId = searchParams.get("deviceId");

  if (!deviceId) {
    return NextResponse.json({ error: "deviceId is required" }, { status: 400 });
  }

  const supabase = supabaseAdmin();
  const { data: transfers, error } = await supabase
    .from("transfers")
    .select("*")
    .eq("target_device_id", deviceId)
    .eq("status", "pending")
    .order("created_at", { ascending: false });

  if (error) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }

  const rows = (transfers ?? []) as unknown as TransferRow[];
  const senderIds = [...new Set(rows.map((r) => r.sender_device_id))];

  const { data: senders } = senderIds.length
    ? await supabase.from("devices").select("id, name").in("id", senderIds)
    : { data: [] as Pick<DeviceRow, "id" | "name">[] };

  const senderNameById = new Map((senders ?? []).map((s) => [s.id, s.name]));

  const response: IncomingTransferSummary[] = rows.map((row) => ({
    id: row.id,
    senderName: senderNameById.get(row.sender_device_id) ?? "Unknown device",
    files: row.file_paths.map((f) => ({ name: f.name, size: f.size, type: f.type })),
    createdAt: row.created_at,
  }));

  return NextResponse.json(response);
}
