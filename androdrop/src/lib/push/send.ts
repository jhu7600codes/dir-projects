import "server-only";
import webpush from "web-push";

let configured = false;

function ensureConfigured() {
  if (configured) return;

  const publicKey = process.env.NEXT_PUBLIC_VAPID_PUBLIC_KEY;
  const privateKey = process.env.VAPID_PRIVATE_KEY;
  const subject = process.env.VAPID_SUBJECT;

  if (!publicKey || !privateKey || !subject) {
    throw new Error("Missing VAPID env vars — push notifications are not configured.");
  }

  webpush.setVapidDetails(subject, publicKey, privateKey);
  configured = true;
}

/**
 * Best-effort push send: swallows errors (expired/invalid subscriptions,
 * missing VAPID config) so a failed push never breaks the transfer flow —
 * the in-app Realtime subscription and polling fallback still work.
 */
export async function sendPushNotification(
  subscription: webpush.PushSubscription,
  payload: Record<string, unknown>,
): Promise<boolean> {
  try {
    ensureConfigured();
    await webpush.sendNotification(subscription, JSON.stringify(payload));
    return true;
  } catch (err) {
    console.warn("push notification failed", err);
    return false;
  }
}
