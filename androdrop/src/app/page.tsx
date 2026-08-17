"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useDeviceContext } from "@/lib/device-context";
import { PairQrCode } from "@/components/PairQrCode";
import { enablePushForDevice, isPushSupported } from "@/lib/push/subscribe-client";

export default function Home() {
  const { device, loading, error, rename } = useDeviceContext();
  const [editingName, setEditingName] = useState(false);
  const [nameDraft, setNameDraft] = useState("");
  const [pushState, setPushState] = useState<"idle" | "granted" | "denied" | "unsupported">("idle");

  useEffect(() => {
    // One-time browser feature check — done in an effect (not during
    // render) so server and first client render match before hydration.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (!isPushSupported()) setPushState("unsupported");
  }, []);

  function startEditingName() {
    if (!device) return;
    setNameDraft(device.name);
    setEditingName(true);
  }

  const pairUrl =
    device && typeof window !== "undefined"
      ? `${window.location.origin}/send?code=${device.pairCode}`
      : "";

  async function submitRename() {
    const trimmed = nameDraft.trim();
    setEditingName(false);
    if (trimmed && device && trimmed !== device.name) {
      await rename(trimmed);
    }
  }

  async function handleEnableNotifications() {
    if (!device) return;
    const result = await enablePushForDevice(device.deviceId);
    setPushState(result);
  }

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col px-5 pb-10 pt-8">
      <header className="mb-8 flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-primary text-lg">
          <span className="text-on-primary">💧</span>
        </div>
        <h1 className="text-2xl font-semibold tracking-tight">androdrop</h1>
      </header>

      {loading && !device && (
        <div className="flex flex-1 items-center justify-center text-on-surface-variant">
          Pairing this device…
        </div>
      )}

      {error && !device && (
        <div className="rounded-3xl bg-error-container p-5 text-on-error-container">
          <p className="font-medium">Couldn&apos;t pair this device</p>
          <p className="mt-1 text-sm opacity-90">{error}</p>
        </div>
      )}

      {device && (
        <div className="flex flex-1 flex-col items-center">
          <section className="w-full rounded-[28px] bg-surface-container p-6 text-center">
            <p className="text-sm font-medium text-on-surface-variant">This device</p>

            {editingName ? (
              <input
                autoFocus
                value={nameDraft}
                onChange={(e) => setNameDraft(e.target.value)}
                onBlur={submitRename}
                onKeyDown={(e) => e.key === "Enter" && submitRename()}
                maxLength={60}
                className="mt-1 w-full rounded-2xl bg-surface px-4 py-2 text-center text-xl font-semibold text-on-surface outline-none ring-2 ring-primary"
              />
            ) : (
              <button
                className="mt-1 text-xl font-semibold text-on-surface"
                onClick={startEditingName}
                aria-label="Rename this device"
              >
                {device.name} <span className="text-sm text-on-surface-variant">✎</span>
              </button>
            )}

            <div className="my-5 flex justify-center">
              <PairQrCode value={pairUrl} />
            </div>

            <p className="text-xs uppercase tracking-wide text-on-surface-variant">Pairing code</p>
            <p className="mt-1 font-mono text-3xl font-semibold tracking-[0.3em] text-primary">
              {device.pairCode}
            </p>
            <p className="mt-3 text-sm text-on-surface-variant">
              Scan the code or enter it on the sending device.
            </p>
          </section>

          {pushState !== "unsupported" && pushState !== "granted" && (
            <button className="btn btn-tonal mt-4 w-full" onClick={handleEnableNotifications}>
              🔔 Enable notifications
            </button>
          )}
          {pushState === "granted" && (
            <p className="mt-4 text-sm text-on-surface-variant">Notifications enabled ✓</p>
          )}

          <div className="flex-1" />

          <Link href="/send" className="btn btn-filled mt-8 w-full">
            Send a file
          </Link>
        </div>
      )}
    </main>
  );
}
