import type { MetadataRoute } from 'next';

/**
 * Web App Manifest — served at /manifest.webmanifest by Next.js 15.
 * Enables "Add to Home Screen" on iOS and Android.
 */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: 'Chores',
    short_name: 'Chores',
    description: 'Kids chore tracker',
    start_url: '/',
    scope: '/',
    display: 'standalone',
    orientation: 'portrait',
    background_color: '#ffffff',
    theme_color: '#4263eb',
    icons: [
      {
        src: '/icon-192.png',
        sizes: '192x192',
        type: 'image/png',
        purpose: 'any',
      },
      {
        src: '/icon-512.png',
        sizes: '512x512',
        type: 'image/png',
        purpose: 'any',
      },
      {
        src: '/icon-maskable-512.png',
        sizes: '512x512',
        type: 'image/png',
        purpose: 'maskable',
      },
    ],
  };
}
