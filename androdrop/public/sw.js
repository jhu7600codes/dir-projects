// androdrop service worker — enables PWA installability and web push for
// incoming-transfer notifications (the iOS Safari 16.4+ path in particular
// requires the app be installed to the home screen and have an active
// service worker before push notifications can be delivered).

const CACHE_NAME = "androdrop-v1";
const APP_SHELL = ["/", "/manifest.webmanifest"];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)).catch(() => {}),
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

// Network-first for navigations, cache-first for everything else — this app
// is realtime/live-data driven, so we don't want stale API responses cached.
self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  const url = new URL(event.request.url);
  if (url.pathname.startsWith("/api/")) return;

  event.respondWith(
    fetch(event.request)
      .then((response) => {
        const copy = response.clone();
        caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
        return response;
      })
      .catch(() => caches.match(event.request)),
  );
});

self.addEventListener("push", (event) => {
  if (!event.data) return;
  let payload;
  try {
    payload = event.data.json();
  } catch {
    payload = { title: "androdrop", body: event.data.text() };
  }

  const transferId = payload.transferId;
  event.waitUntil(
    self.registration.showNotification(payload.title || "androdrop", {
      body: payload.body || "You have an incoming transfer",
      icon: "/icons/icon-192.png",
      badge: "/icons/icon-192.png",
      tag: transferId ? `transfer-${transferId}` : undefined,
      data: { transferId },
      requireInteraction: true,
    }),
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const targetUrl = "/";

  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
      for (const client of clients) {
        if ("focus" in client) {
          client.focus();
          return;
        }
      }
      if (self.clients.openWindow) return self.clients.openWindow(targetUrl);
    }),
  );
});
