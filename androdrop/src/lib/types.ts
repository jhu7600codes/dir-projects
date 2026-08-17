export type Platform = "web" | "ios" | "android";

export type TransferStatus = "pending" | "accepted" | "declined" | "expired";

export interface TransferFileMeta {
  path: string;
  name: string;
  size: number;
  type: string;
}

export interface TransferFileView {
  name: string;
  size: number;
  type: string;
  url?: string;
}

export interface DeviceRow {
  id: string;
  name: string;
  platform: Platform;
  pair_code: string;
  push_subscription: unknown;
  created_at: string;
  last_seen_at: string;
}

export interface TransferRow {
  id: string;
  sender_device_id: string;
  target_device_id: string;
  file_paths: TransferFileMeta[];
  status: TransferStatus;
  created_at: string;
  responded_at: string | null;
  expires_at: string;
}

export interface PairResponse {
  deviceId: string;
  name: string;
  platform: Platform;
  pairCode: string;
  createdAt: string;
}

export interface TransferDetailResponse {
  id: string;
  status: TransferStatus;
  senderName: string;
  senderDeviceId: string;
  targetName: string;
  targetDeviceId: string;
  files: TransferFileView[];
  createdAt: string;
  respondedAt: string | null;
  expiresAt: string;
}
