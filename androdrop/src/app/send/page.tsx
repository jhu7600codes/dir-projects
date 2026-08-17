"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useDeviceContext } from "@/lib/device-context";
import { normalizePairCode } from "@/lib/pair-code";
import { formatBytes } from "@/lib/format-bytes";

interface ResolvedTarget {
  deviceId: string;
  name: string;
}

export default function SendPage() {
  const router = useRouter();
  const { device } = useDeviceContext();

  const [code, setCode] = useState("");
  const [target, setTarget] = useState<ResolvedTarget | null>(null);
  const [resolving, setResolving] = useState(false);
  const [resolveError, setResolveError] = useState<string | null>(null);
  const [files, setFiles] = useState<File[]>([]);
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  // If the debounced timer fires and its fetch is still in flight when the
  // user types again, a second fetch can start before the first resolves;
  // this guards against the older response landing later and clobbering
  // the newer one.
  const resolveRequestId = useRef(0);

  // Pre-fill from a scanned QR link like /send?code=ABC123.
  useEffect(() => {
    // One-time read of the URL a QR scan/deep link landed on.
    const params = new URLSearchParams(window.location.search);
    const prefilled = params.get("code");
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (prefilled) setCode(normalizePairCode(prefilled));
  }, []);

  useEffect(() => {
    // Debounced lookup: reset prior results, then resolve the new code
    // against the API after a short pause in typing.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setTarget(null);
    setResolveError(null);
    if (debounceRef.current) clearTimeout(debounceRef.current);

    const normalized = normalizePairCode(code);
    if (normalized.length !== 6) return;

    debounceRef.current = setTimeout(async () => {
      const thisRequestId = ++resolveRequestId.current;
      setResolving(true);
      try {
        const res = await fetch(`/api/pair/${normalized}`);
        const body = await res.json();
        if (thisRequestId !== resolveRequestId.current) return;
        if (!res.ok) throw new Error(body.error || "Device not found");
        if (body.deviceId === device?.deviceId) {
          throw new Error("That's this device — pair from a different one");
        }
        setTarget({ deviceId: body.deviceId, name: body.name });
      } catch (err) {
        if (thisRequestId === resolveRequestId.current) {
          setResolveError(err instanceof Error ? err.message : "Device not found");
        }
      } finally {
        if (thisRequestId === resolveRequestId.current) setResolving(false);
      }
    }, 400);

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [code, device?.deviceId]);

  async function handleSend() {
    if (!device || !target || files.length === 0) return;
    setSending(true);
    setSendError(null);
    try {
      const form = new FormData();
      form.set("senderDeviceId", device.deviceId);
      form.set("targetDeviceId", target.deviceId);
      for (const file of files) form.append("files", file);

      const res = await fetch("/api/transfer", { method: "POST", body: form });
      const body = await res.json();
      if (!res.ok) throw new Error(body.error || "Send failed");

      router.push(`/send/${body.transferId}`);
    } catch (err) {
      setSendError(err instanceof Error ? err.message : "Send failed");
      setSending(false);
    }
  }

  const canSend = Boolean(device && target && files.length > 0 && !sending);

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col px-5 pb-10 pt-8">
      <header className="mb-8 flex items-center gap-3">
        <Link href="/" className="flex h-10 w-10 items-center justify-center rounded-2xl bg-surface-variant" aria-label="Back">
          ←
        </Link>
        <h1 className="text-2xl font-semibold tracking-tight">Send a file</h1>
      </header>

      <section>
        <label className="text-sm font-medium text-on-surface-variant" htmlFor="pair-code">
          Recipient&apos;s pairing code
        </label>
        <input
          id="pair-code"
          value={code}
          onChange={(e) => setCode(normalizePairCode(e.target.value))}
          placeholder="ABC123"
          maxLength={6}
          autoCapitalize="characters"
          className="mt-2 w-full rounded-2xl bg-surface-container px-4 py-4 text-center font-mono text-2xl tracking-[0.3em] text-on-surface outline-none ring-primary focus:ring-2"
        />

        <div className="mt-3 min-h-6 text-center text-sm">
          {resolving && <span className="text-on-surface-variant">Looking up device…</span>}
          {!resolving && target && (
            <span className="font-medium text-primary">Sending to {target.name}</span>
          )}
          {!resolving && resolveError && <span className="text-error">{resolveError}</span>}
        </div>
      </section>

      <section className="mt-6">
        <label className="text-sm font-medium text-on-surface-variant">Files</label>
        <label className="mt-2 flex cursor-pointer flex-col items-center justify-center rounded-[28px] border-2 border-dashed border-outline-variant bg-surface-container px-6 py-10 text-center">
          <span className="text-3xl">📎</span>
          <span className="mt-2 text-sm font-medium text-on-surface">
            {files.length === 0 ? "Tap to choose files" : `${files.length} file${files.length > 1 ? "s" : ""} selected`}
          </span>
          <input
            type="file"
            multiple
            className="hidden"
            onChange={(e) => setFiles(Array.from(e.target.files ?? []))}
          />
        </label>

        {files.length > 0 && (
          <ul className="mt-3 space-y-2">
            {files.map((f, i) => (
              <li key={i} className="flex items-center justify-between rounded-2xl bg-surface-container px-4 py-3 text-sm">
                <span className="truncate pr-2">{f.name}</span>
                <span className="shrink-0 text-on-surface-variant">{formatBytes(f.size)}</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      {sendError && <p className="mt-4 text-center text-sm text-error">{sendError}</p>}

      <div className="flex-1" />

      <button className="btn btn-filled mt-8 w-full" disabled={!canSend} onClick={handleSend}>
        {sending ? "Sending…" : "Send"}
      </button>
    </main>
  );
}
