'use client';

/**
 * /offline — static fallback page served by the service worker when the
 * network is unavailable and no cached navigation response exists.
 * This page is precached by sw.js during the install phase.
 */
export default function OfflinePage() {
  return (
    <div
      style={{
        fontFamily: 'system-ui, -apple-system, sans-serif',
        background: '#f8f9fa',
        color: '#212529',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100dvh',
        padding: '1.5rem',
        margin: 0,
      }}
    >
      <div
        style={{
          background: '#fff',
          borderRadius: '1rem',
          padding: '2.5rem 2rem',
          textAlign: 'center',
          maxWidth: '360px',
          boxShadow: '0 4px 24px rgba(0,0,0,.08)',
        }}
      >
        <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📵</div>
        <h1
          style={{
            fontSize: '1.4rem',
            fontWeight: 700,
            color: '#4263eb',
            marginBottom: '.6rem',
          }}
        >
          You are offline
        </h1>
        <p style={{ fontSize: '.95rem', lineHeight: 1.5, color: '#6c757d' }}>
          Chores needs a network connection. Please check your connection and try again.
        </p>
        <button
          onClick={() => window.location.reload()}
          style={{
            marginTop: '1.5rem',
            padding: '.6rem 1.4rem',
            background: '#4263eb',
            color: '#fff',
            border: 'none',
            borderRadius: '.5rem',
            fontSize: '1rem',
            cursor: 'pointer',
          }}
        >
          Try again
        </button>
      </div>
    </div>
  );
}
