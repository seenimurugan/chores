/**
 * sw.js — Chores PWA service worker
 *
 * Caching strategy:
 *   Navigation requests  → Network-first; fallback to /offline only when
 *                          the network is completely unreachable (TypeError).
 *                          Authenticated pages always stay fresh.
 *   Static assets        → Cache-first (precached at install time).
 *   /api/*               → Never cached; falls straight through to the network.
 *   Cross-origin         → Never cached.
 *
 * Bump CACHE_VERSION on every deploy to invalidate stale caches.
 */

const CACHE_VERSION = 'v1';
const SHELL_CACHE   = `chores-shell-${CACHE_VERSION}`;

/** Assets precached during `install`. */
const PRECACHE_URLS = [
  '/offline',          // offline fallback page (navigation)
  '/icon-192.png',
  '/icon-512.png',
  '/apple-touch-icon.png',
  '/favicon.ico',
];

// ---------------------------------------------------------------------------
// Install — precache shell assets
// ---------------------------------------------------------------------------
self.addEventListener('install', event => {
  console.info('[SW] install — cache version:', CACHE_VERSION);
  event.waitUntil(
    caches.open(SHELL_CACHE).then(cache => {
      console.info('[SW] precaching:', PRECACHE_URLS);
      return cache.addAll(PRECACHE_URLS);
    }).then(() => {
      console.info('[SW] install complete — skipping waiting');
      return self.skipWaiting();
    })
  );
});

// ---------------------------------------------------------------------------
// Activate — purge old cache versions
// ---------------------------------------------------------------------------
self.addEventListener('activate', event => {
  console.info('[SW] activate — purging caches older than', CACHE_VERSION);
  event.waitUntil(
    caches.keys().then(keys => {
      const deletions = keys
        .filter(k => k.startsWith('chores-') && k !== SHELL_CACHE)
        .map(k => {
          console.info('[SW] deleting old cache:', k);
          return caches.delete(k);
        });
      return Promise.all(deletions);
    }).then(() => {
      console.info('[SW] activate complete — claiming clients');
      return self.clients.claim();
    })
  );
});

// ---------------------------------------------------------------------------
// Fetch — routing logic
// ---------------------------------------------------------------------------
self.addEventListener('fetch', event => {
  const { request } = event;
  const url = new URL(request.url);

  // 1. Skip non-GET requests entirely (POST/PUT/DELETE go straight to network).
  if (request.method !== 'GET') return;

  // 2. Skip cross-origin requests (Tailscale backend, CDNs, etc.).
  if (url.origin !== self.location.origin) return;

  // 3. Never cache API routes — auth data must always be fresh.
  if (url.pathname.startsWith('/api/')) return;

  // 4. Navigation requests → network-first, offline fallback.
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request).catch(err => {
        console.warn('[SW] navigation fetch failed, serving /offline:', err.message);
        return caches.match('/offline').then(cached => {
          if (cached) return cached;
          // Last resort: return a minimal inline page so the user sees something.
          return new Response(
            '<html><body><h1>Offline</h1><p>Please check your connection.</p></body></html>',
            { headers: { 'Content-Type': 'text/html' } }
          );
        });
      })
    );
    return;
  }

  // 5. Static assets (icons, etc.) → cache-first.
  event.respondWith(
    caches.match(request).then(cached => {
      if (cached) return cached;
      return fetch(request).then(response => {
        // Only cache successful same-origin responses.
        if (response.ok) {
          const clone = response.clone();
          caches.open(SHELL_CACHE).then(cache => cache.put(request, clone));
        }
        return response;
      });
    })
  );
});
