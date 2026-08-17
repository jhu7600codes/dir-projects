import { NextResponse } from "next/server";
import { supabaseAdmin, STORAGE_BUCKET } from "@/lib/supabase/server";
import type { TransferRow } from "@/lib/types";

const SIGNED_URL_TTL_SECONDS = 60 * 60; // 1 hour

export async function POST(request: Request, context: RouteContext<"/api/transfer/[id]/respond">) {
  const { id } = await context.params;
  const body = await request.json().catch(() => null);

  const deviceId = body?.deviceId;
  const action = body?.action;

  if (typeof deviceId !== "string" || !deviceId) {
    return NextResponse.json({ error: "deviceId is required" }, { status: 400 });
  }
  if (action !== "accept" && action !== "decline") {
    return NextResponse.json({ error: "action must be 'accept' or 'decline'" }, { status: 400 });
  }

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

  if (row.target_device_id !== deviceId) {
    return NextResponse.json({ error: "Only the target device can respond to this transfer" }, { status: 403 });
  }
  if (row.status !== "pending") {
    return NextResponse.json({ error: `Transfer already ${row.status}`, status: row.status }, { status: 409 });
  }

  const newStatus = action === "accept" ? "accepted" : "declined";

  if (newStatus === "declined") {
    const paths = row.file_paths.map((f) => f.path);
    if (paths.length > 0) {
      await supabase.storage.from(STORAGE_BUCKET).remove(paths);
    }
    const { error: updateError } = await supabase
      .from("transfers")
      .update({ status: "declined", responded_at: new Date().toISOString(), file_paths: [] })
      .eq("id", id);
    if (updateError) {
      return NextResponse.json({ error: updateError.message }, { status: 500 });
    }
    return NextResponse.json({ id, status: "declined" });
  }

  const { error: updateError } = await supabase
    .from("transfers")
    .update({ status: "accepted", responded_at: new Date().toISOString() })
    .eq("id", id);

  if (updateError) {
    return NextResponse.json({ error: updateError.message }, { status: 500 });
  }

  const files = await Promise.all(
    row.file_paths.map(async (f) => {
      const { data: signed } = await supabase.storage
        .from(STORAGE_BUCKET)
        .createSignedUrl(f.path, SIGNED_URL_TTL_SECONDS);
      return { name: f.name, size: f.size, type: f.type, url: signed?.signedUrl };
    }),
  );

  return NextResponse.json({ id, status: "accepted", files });
}
