"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { supabaseBrowser } from "@/lib/supabase/browser";
import { formatBytes } from "@/lib/format-bytes";
import type { TransferDetailResponse } from "@/lib/types";

type ViewState =
  | { kind: "idle" }
  | { kind: "incoming"; transfer: TransferDetailResponse }
  | { kind: "responding"; transfer: TransferDetailResponse }
  | { kind: "accepted"; transfer: TransferDetailResponse }
  | { kind: "error"; message: string };

/**
 * Global full-screen accept/decline surface. Mounted once at the app root
 * so an incoming transfer interrupts whatever screen the recipient is on —
 * one primary action visible at a time, per the design brief.
 */
export function IncomingTransferOverlay({ deviceId }: { deviceId: string }) {
  const [state, setState] = useState<ViewState>({ kind: "idle" });
  const seenIds = useRef(new Set<string>());

  const loadTransfer = useCallback(
    async (transferId: string) => {
      if (seenIds.current.has(transferId)) return;
      const res = await fetch(`/api/transfer/${transferId}?deviceId=${deviceId}`);
      if (!res.ok) return;
      const detail: TransferDetailResponse = await res.json();
      if (detail.status !== "pending") return;
      seenIds.current.add(transferId);
      setState({ kind: "incoming", transfer: detail });
    },
    [deviceId],
  );

  useEffect(() => {
    const supabase = supabaseBrowser();

    // Catch anything already pending when this page/tab loads.
    supabase
      .from("transfers")
      .select("id")
      .eq("target_device_id", deviceId)
      .eq("status", "pending")
      .order("created_at", { ascending: false })
      .limit(1)
      .then(({ data }) => {
        const row = data?.[0] as { id: string } | undefined;
        if (row) loadTransfer(row.id);
      });

    const channel = supabase
      .channel(`incoming-${deviceId}`)
      .on(
        "postgres_changes",
        {
          event: "INSERT",
          schema: "public",
          table: "transfers",
          filter: `target_device_id=eq.${deviceId}`,
        },
        (payload) => {
          const row = payload.new as { id: string; status: string };
          if (row.status === "pending") loadTransfer(row.id);
        },
      )
      .subscribe();

    // Fallback in case Realtime hiccups: light periodic re-check.
    const interval = setInterval(() => {
      supabase
        .from("transfers")
        .select("id")
        .eq("target_device_id", deviceId)
        .eq("status", "pending")
        .order("created_at", { ascending: false })
        .limit(1)
        .then(({ data }) => {
          const row = data?.[0] as { id: string } | undefined;
          if (row) loadTransfer(row.id);
        });
    }, 8000);

    return () => {
      clearInterval(interval);
      supabase.removeChannel(channel);
    };
  }, [deviceId, loadTransfer]);

  if (state.kind === "idle") return null;

  async function respond(action: "accept" | "decline") {
    if (state.kind !== "incoming") return;
    const transfer = state.transfer;
    setState({ kind: "responding", transfer });
    try {
      const res = await fetch(`/api/transfer/${transfer.id}/respond`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ deviceId, action }),
      });
      const body = await res.json();
      if (!res.ok) throw new Error(body.error || "Request failed");

      if (action === "decline") {
        setState({ kind: "idle" });
        return;
      }
      setState({
        kind: "accepted",
        transfer: { ...transfer, status: "accepted", files: body.files },
      });
    } catch (err) {
      setState({ kind: "error", message: err instanceof Error ? err.message : "Something went wrong" });
    }
  }

  const transfer =
    state.kind === "incoming" || state.kind === "responding" || state.kind === "accepted"
      ? state.transfer
      : null;

  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-end bg-black/50 p-4 backdrop-blur-sm sm:items-center sm:justify-center">
      <div className="w-full max-w-sm rounded-[28px] bg-surface p-6 text-on-surface shadow-2xl">
        {state.kind === "error" && (
          <>
            <p className="text-lg font-semibold text-on-surface">Couldn&apos;t respond</p>
            <p className="mt-2 text-sm text-on-surface-variant">{state.message}</p>
            <button className="btn btn-filled mt-6 w-full" onClick={() => setState({ kind: "idle" })}>
              Dismiss
            </button>
          </>
        )}

        {transfer && state.kind !== "accepted" && (
          <>
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-primary-container text-2xl">
              📥
            </div>
            <p className="text-center text-sm font-medium text-on-surface-variant">Incoming transfer</p>
            <p className="mt-1 text-center text-xl font-semibold text-on-surface">{transfer.senderName}</p>
            <p className="mt-1 text-center text-sm text-on-surface-variant">
              wants to send you {transfer.files.length === 1 ? "a file" : `${transfer.files.length} files`}
            </p>

            <ul className="mt-4 max-h-40 space-y-2 overflow-y-auto">
              {transfer.files.map((f, i) => (
                <li
                  key={i}
                  className="flex items-center justify-between rounded-2xl bg-surface-variant px-4 py-3 text-sm"
                >
                  <span className="truncate pr-2">{f.name}</span>
                  <span className="shrink-0 text-on-surface-variant">{formatBytes(f.size)}</span>
                </li>
              ))}
            </ul>

            <div className="mt-6 flex flex-col gap-3">
              <button
                className="btn btn-filled w-full"
                disabled={state.kind === "responding"}
                onClick={() => respond("accept")}
              >
                Accept
              </button>
              <button
                className="btn btn-outlined w-full"
                disabled={state.kind === "responding"}
                onClick={() => respond("decline")}
              >
                Decline
              </button>
            </div>
          </>
        )}

        {transfer && state.kind === "accepted" && (
          <>
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-primary-container text-2xl">
              ✅
            </div>
            <p className="text-center text-xl font-semibold text-on-surface">Accepted</p>
            <p className="mt-1 text-center text-sm text-on-surface-variant">
              From {transfer.senderName} — links expire in 1 hour
            </p>

            <div className="mt-4 flex flex-col gap-2">
              {transfer.files.map((f, i) => (
                <a
                  key={i}
                  href={f.url}
                  download={f.name}
                  className="flex items-center justify-between rounded-2xl bg-surface-variant px-4 py-3 text-sm font-medium text-on-surface underline-offset-2 hover:underline"
                >
                  <span className="truncate pr-2">{f.name}</span>
                  <span className="shrink-0 text-on-surface-variant">{formatBytes(f.size)}</span>
                </a>
              ))}
            </div>

            <button className="btn btn-tonal mt-6 w-full" onClick={() => setState({ kind: "idle" })}>
              Done
            </button>
          </>
        )}
      </div>
    </div>
  );
}
