"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import type { PairResponse } from "./types";

const STORAGE_KEY = "androdrop:deviceId";

export interface DeviceIdentity {
  deviceId: string;
  name: string;
  pairCode: string;
}

export function useDevice() {
  const [device, setDevice] = useState<DeviceIdentity | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // Guards against React Strict Mode's double-invoked mount effect (and any
  // other accidental re-entry) racing two concurrent `pair()` calls before
  // either has written a deviceId to localStorage — which would otherwise
  // register two separate devices for one browser tab. Cleared once the
  // request settles, so a later `rename()` still fires its own call.
  const inFlight = useRef<Promise<DeviceIdentity> | null>(null);

  const pair = useCallback(async (name?: string) => {
    if (inFlight.current) return inFlight.current;

    const request = (async (): Promise<DeviceIdentity> => {
      setLoading(true);
      setError(null);
      try {
        const existingId = localStorage.getItem(STORAGE_KEY) || undefined;
        const res = await fetch("/api/pair", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ deviceId: existingId, name, platform: "web" }),
        });
        if (!res.ok) {
          const body = await res.json().catch(() => ({}));
          throw new Error(body.error || "Pairing failed");
        }
        const data: PairResponse = await res.json();
        localStorage.setItem(STORAGE_KEY, data.deviceId);
        const identity = { deviceId: data.deviceId, name: data.name, pairCode: data.pairCode };
        setDevice(identity);
        return identity;
      } catch (err) {
        const message = err instanceof Error ? err.message : "Pairing failed";
        setError(message);
        throw err;
      } finally {
        setLoading(false);
        inFlight.current = null;
      }
    })();

    inFlight.current = request;
    return request;
  }, []);

  useEffect(() => {
    // Fetch-on-mount: pair (or re-pair, via the id already in localStorage)
    // as soon as this hook is live.
    pair().catch(() => {
      /* surfaced via `error` state */
    });
    // Only run once on mount — re-pairing on demand goes through `rename`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { device, loading, error, rename: (name: string) => pair(name) };
}
