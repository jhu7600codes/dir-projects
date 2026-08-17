"use client";
import { DeviceProvider, useDeviceContext } from "@/lib/device-context";
import { IncomingTransferOverlay } from "./IncomingTransferOverlay";

function Inner({ children }: { children: React.ReactNode }) {
  const { device } = useDeviceContext();
  return (
    <>
      {children}
      {device && <IncomingTransferOverlay deviceId={device.deviceId} />}
    </>
  );
}

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <DeviceProvider>
      <Inner>{children}</Inner>
    </DeviceProvider>
  );
}
