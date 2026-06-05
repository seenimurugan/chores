'use client';

import { useEffect } from 'react';

/**
 * SwRegister — registers the Chores service worker on first mount.
 * Mounted in layout.tsx so it runs on every page load.
 *
 * Logs:
 *   console.info  event=sw.registered  scope=<scope>
 *   console.info  event=sw.updated     scope=<scope>
 *   console.error event=sw.registration.failed  reason=<message>
 *   console.info  event=sw.unsupported  reason=browser lacks serviceWorker API
 */
export default function SwRegister() {
  useEffect(() => {
    if (!('serviceWorker' in navigator)) {
      console.info('event=sw.unsupported reason="browser lacks serviceWorker API"');
      return;
    }

    navigator.serviceWorker
      .register('/sw.js', { scope: '/' })
      .then(registration => {
        console.info(
          'event=sw.registered scope=%s state=%s',
          registration.scope,
          registration.active?.state ?? 'installing'
        );

        // Surface any future update to the console so devs know a new SW is waiting.
        registration.addEventListener('updatefound', () => {
          const newWorker = registration.installing;
          newWorker?.addEventListener('statechange', () => {
            if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
              console.info(
                'event=sw.updated scope=%s reason="new service worker installed and waiting"',
                registration.scope
              );
            }
          });
        });
      })
      .catch((err: unknown) => {
        console.error(
          'event=sw.registration.failed reason=%s',
          err instanceof Error ? err.message : String(err)
        );
      });
  }, []);

  // Renders nothing — side-effect only.
  return null;
}
