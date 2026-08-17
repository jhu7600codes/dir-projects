"use client";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { supabaseBrowser } from "@/lib/supabase/browser";
import type { TransferDetailResponse, TransferStatus } from "@/lib/types";

const STATUS_COPY: Record<TransferStatus, { emoji: string; title: string; subtitle: string }> = {
  pending: { emoji: "📤", title: "Waiting for response…", subtitle: "They haven't accepted or declined yet." },
  accepted: { emoji: "✅", title: "Accepted", subtitle: "They can now download the file(s)." },
  declined: { emoji: "🚫", title: "Declined", subtitle: "The recipient declined this transfer." },
  expired: { emoji: "⌛", title: "Expired", subtitle: "This transfer expired after 24 hours." },
};

export default function SendStatusPage() {
  const params = useParams<{ id: string }>();
  const transferId = params.id;

  const [detail, setDetail] = useState<TransferDetailResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    // The realtime-triggered fetch and the polling-fallback fetch can both
    // be in flight at once and resolve out of order; a request counter
    // makes sure a slower, older response never clobbers a fresher one.
    let requestId = 0;

    async function fetchDetail() {
      const thisRequestId = ++requestId;
      const res = await fetch(`/api/transfer/${transferId}`);
      if (cancelled || thisRequestId !== requestId) return;
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        setError(body.error || "Transfer not found");
        return;
      }
      setDetail(await res.json());
    }

    fetchDetail();

    const supabase = supabaseBrowser();
    const channel = supabase
      .channel(`transfer-${transferId}`)
      .on(
        "postgres_changes",
        { event: "UPDATE", schema: "public", table: "transfers", filter: `id=eq.${transferId}` },
        () => fetchDetail(),
      )
      .subscribe();

    // Polling fallback in case Realtime is unavailable.
    const interval = setInterval(fetchDetail, 4000);

    return () => {
      cancelled = true;
      clearInterval(interval);
      supabase.removeChannel(channel);
    };
  }, [transferId]);

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center px-5 pb-10 pt-8 text-center">
      {error && (
        <div className="rounded-3xl bg-error-container p-5 text-on-error-container">
          <p className="font-medium">{error}</p>
        </div>
      )}

      {!error && !detail && <p className="text-on-surface-variant">Loading…</p>}

      {detail && (
        <>
          <div
            className={`flex h-20 w-20 items-center justify-center rounded-full text-4xl ${
              detail.status === "pending" ? "animate-pulse" : ""
            } bg-primary-container`}
          >
            {STATUS_COPY[detail.status].emoji}
          </div>
          <h1 className="mt-6 text-2xl font-semibold">{STATUS_COPY[detail.status].title}</h1>
          <p className="mt-2 text-on-surface-variant">{STATUS_COPY[detail.status].subtitle}</p>
          <p className="mt-1 text-sm text-on-surface-variant">To {detail.targetName}</p>

          <ul className="mt-6 w-full space-y-2">
            {detail.files.map((f, i) => (
              <li key={i} className="truncate rounded-2xl bg-surface-container px-4 py-3 text-sm">
                {f.name}
              </li>
            ))}
          </ul>
        </>
      )}

      <Link href="/" className="btn btn-tonal mt-10 w-full">
        Done
      </Link>
    </main>
  );
}
