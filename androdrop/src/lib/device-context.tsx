"use client";
import { createContext, useContext } from "react";
import { useDevice, type DeviceIdentity } from "./use-device";

interface DeviceContextValue {
  device: DeviceIdentity | null;
  loading: boolean;
  error: string | null;
  rename: (name: string) => Promise<DeviceIdentity>;
}

const DeviceContext = createContext<DeviceContextValue | null>(null);

export function DeviceProvider({ children }: { children: React.ReactNode }) {
  const value = useDevice();
  return <DeviceContext.Provider value={value}>{children}</DeviceContext.Provider>;
}

export function useDeviceContext() {
  const ctx = useContext(DeviceContext);
  if (!ctx) throw new Error("useDeviceContext must be used within DeviceProvider");
  return ctx;
}
