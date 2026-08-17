"use client";
import { useEffect, useState } from "react";
import QRCode from "qrcode";

export function PairQrCode({ value, size = 220 }: { value: string; size?: number }) {
  const [dataUrl, setDataUrl] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    QRCode.toDataURL(value, {
      width: size,
      margin: 1,
      color: { dark: "#1b1b1f", light: "#00000000" },
    })
      .then((url) => {
        if (!cancelled) setDataUrl(url);
      })
      .catch(() => {
        if (!cancelled) setDataUrl(null);
      });
    return () => {
      cancelled = true;
    };
  }, [value, size]);

  return (
    <div
      className="flex items-center justify-center rounded-3xl bg-surface-container p-4"
      style={{ width: size + 32, height: size + 32 }}
    >
      {dataUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={dataUrl} alt="Pairing QR code" width={size} height={size} />
      ) : (
        <div className="animate-pulse rounded-2xl bg-surface-variant" style={{ width: size, height: size }} />
      )}
    </div>
  );
}
