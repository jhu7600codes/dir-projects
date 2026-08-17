import { NextResponse } from "next/server";
import { supabaseAdmin, STORAGE_BUCKET } from "@/lib/supabase/server";
import type { DeviceRow, TransferDetailResponse, TransferRow } from "@/lib/types";

const SIGNED_URL_TTL_SECONDS = 60 * 60; // 1 hour

export async function GET(request: Request, context: RouteContext<"/api/transfer/[id]">) {
  const { id } = await context.params;
  const { searchParams } = new URL(request.url);
  const deviceId = searchParams.get("deviceId");

  const supabase = supabaseAdmin();
  const { data: transfer, error } = await supabase
    .from("transfers")
    .select("*")
    .eq("id", id)
    .maybeSingle();

  if (error) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
  if (!transfer) {
    return NextResponse.json({ error: "Transfer not found" }, { status: 404 });
  }

  const row = transfer as unknown as TransferRow;

  const [{ data: sender }, { data: target }] = await Promise.all([
    supabase.from("devices").select("name").eq("id", row.sender_device_id).maybeSingle(),
    supabase.from("devices").select("name").eq("id", row.target_device_id).maybeSingle(),
  ]);

  const isTarget = deviceId === row.target_device_id;
  const includeUrls = isTarget && row.status === "accepted";

  const files = await Promise.all(
    row.file_paths.map(async (f) => {
      if (!includeUrls) {
        return { name: f.name, size: f.size, type: f.type };
      }
      const { data: signed } = await supabase.storage
        .from(STORAGE_BUCKET)
        .createSignedUrl(f.path, SIGNED_URL_TTL_SECONDS);
      return { name: f.name, size: f.size, type: f.type, url: signed?.signedUrl };
    }),
  );

  const response: TransferDetailResponse = {
    id: row.id,
    status: row.status,
    senderName: (sender as Pick<DeviceRow, "name"> | null)?.name ?? "Unknown device",
    senderDeviceId: row.sender_device_id,
    targetName: (target as Pick<DeviceRow, "name"> | null)?.name ?? "Unknown device",
    targetDeviceId: row.target_device_id,
    files,
    createdAt: row.created_at,
    respondedAt: row.responded_at,
    expiresAt: row.expires_at,
  };

  return NextResponse.json(response);
}
