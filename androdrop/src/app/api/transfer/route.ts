import { NextResponse } from "next/server";
import { supabaseAdmin, STORAGE_BUCKET } from "@/lib/supabase/server";
import { normalizePairCode } from "@/lib/pair-code";
import { sanitizeFilename } from "@/lib/sanitize-filename";
import { sendPushNotification } from "@/lib/push/send";
import type { DeviceRow, TransferFileMeta } from "@/lib/types";
import type { Json } from "@/lib/supabase/database.types";

const MAX_FILES = 10;

export async function POST(request: Request) {
  const form = await request.formData().catch(() => null);
  if (!form) {
    return NextResponse.json({ error: "Expected multipart/form-data" }, { status: 400 });
  }

  const senderDeviceId = form.get("senderDeviceId");
  const targetCode = form.get("targetCode");
  const targetDeviceIdField = form.get("targetDeviceId");
  const files = form.getAll("files").filter((f): f is File => f instanceof File);

  if (typeof senderDeviceId !== "string" || !senderDeviceId) {
    return NextResponse.json({ error: "senderDeviceId is required" }, { status: 400 });
  }
  if (files.length === 0) {
    return NextResponse.json({ error: "At least one file is required" }, { status: 400 });
  }
  if (files.length > MAX_FILES) {
    return NextResponse.json({ error: `At most ${MAX_FILES} files per transfer` }, { status: 400 });
  }

  const supabase = supabaseAdmin();

  // Resolve the target device, either directly by id or via a pair code.
  let targetDeviceId: string | null = null;
  if (typeof targetDeviceIdField === "string" && targetDeviceIdField) {
    targetDeviceId = targetDeviceIdField;
  } else if (typeof targetCode === "string" && targetCode) {
    const { data } = await supabase
      .from("devices")
      .select("id")
      .eq("pair_code", normalizePairCode(targetCode))
      .maybeSingle();
    targetDeviceId = (data as Pick<DeviceRow, "id"> | null)?.id ?? null;
  }

  if (!targetDeviceId) {
    return NextResponse.json({ error: "Target device not found" }, { status: 404 });
  }
  if (targetDeviceId === senderDeviceId) {
    return NextResponse.json({ error: "Cannot send a transfer to yourself" }, { status: 400 });
  }

  const [{ data: sender }, { data: target }] = await Promise.all([
    supabase.from("devices").select("*").eq("id", senderDeviceId).maybeSingle(),
    supabase.from("devices").select("*").eq("id", targetDeviceId).maybeSingle(),
  ]);

  if (!sender) {
    return NextResponse.json({ error: "Sender device not found — pair first" }, { status: 404 });
  }
  if (!target) {
    return NextResponse.json({ error: "Target device not found" }, { status: 404 });
  }

  const { data: transfer, error: insertError } = await supabase
    .from("transfers")
    .insert({
      sender_device_id: senderDeviceId,
      target_device_id: targetDeviceId,
      file_paths: [],
      status: "pending",
    })
    .select()
    .single();

  if (insertError || !transfer) {
    return NextResponse.json({ error: insertError?.message ?? "Could not create transfer" }, { status: 500 });
  }

  const transferId = transfer.id as string;
  const fileMeta: TransferFileMeta[] = [];

  for (const [index, file] of files.entries()) {
    const path = `${transferId}/${index}-${sanitizeFilename(file.name)}`;
    const buffer = Buffer.from(await file.arrayBuffer());

    const { error: uploadError } = await supabase.storage
      .from(STORAGE_BUCKET)
      .upload(path, buffer, {
        contentType: file.type || "application/octet-stream",
        upsert: false,
      });

    if (uploadError) {
      // Best-effort cleanup of whatever we already uploaded, then bail.
      if (fileMeta.length > 0) {
        await supabase.storage.from(STORAGE_BUCKET).remove(fileMeta.map((f) => f.path));
      }
      await supabase.from("transfers").delete().eq("id", transferId);
      return NextResponse.json({ error: `Upload failed: ${uploadError.message}` }, { status: 500 });
    }

    fileMeta.push({
      path,
      name: file.name || "file",
      size: file.size,
      type: file.type || "application/octet-stream",
    });
  }

  const { error: updateError } = await supabase
    .from("transfers")
    .update({ file_paths: fileMeta as unknown as Json })
    .eq("id", transferId);

  if (updateError) {
    return NextResponse.json({ error: updateError.message }, { status: 500 });
  }

  const pushSubscription = (target as DeviceRow).push_subscription;
  if (pushSubscription && typeof pushSubscription === "object") {
    await sendPushNotification(pushSubscription as never, {
      title: "androdrop",
      body: `${(sender as DeviceRow).name} wants to send you ${files.length === 1 ? "a file" : `${files.length} files`}`,
      transferId,
    });
  }

  return NextResponse.json(
    {
      transferId,
      status: "pending",
      expiresAt: transfer.expires_at,
    },
    { status: 201 },
  );
}
