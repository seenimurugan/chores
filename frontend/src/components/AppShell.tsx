'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { api, Me, setToken } from '@/lib/api';

export default function AppShell({
  children,
  requireRole,
}: {
  children: React.ReactNode;
  requireRole?: 'ADMIN' | 'KID';
}) {
  const router = useRouter();
  const path = usePathname();
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.me()
      .then((u) => {
        setMe(u);
        if (requireRole && u.role !== requireRole) {
          router.replace(u.role === 'ADMIN' ? '/admin' : '/');
        }
      })
      .catch(() => router.replace('/login'))
      .finally(() => setLoading(false));
  }, [router, requireRole]);

  if (loading) return <div className="p-6 text-slate-500">Loading…</div>;
  if (!me) return null;

  const isAdmin = me.role === 'ADMIN';
  const adminNav = [
    { href: '/admin', label: 'Dashboard' },
    { href: '/admin/users', label: 'Kids' },
    { href: '/admin/tasks', label: 'Tasks' },
  ];
  const kidNav = [
    { href: '/', label: 'Today' },
    { href: '/stats', label: 'My stats' },
  ];
  const nav = isAdmin ? adminNav : kidNav;

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 sticky top-0 z-10">
        <div className="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span
              className="w-9 h-9 rounded-full inline-flex items-center justify-center text-white font-bold"
              style={{ background: me.avatarColor }}
            >
              {me.displayName.charAt(0).toUpperCase()}
            </span>
            <div className="flex flex-col leading-tight">
              <span className="font-semibold text-sm sm:text-base">{me.displayName}</span>
              <span className="text-xs text-slate-500">{isAdmin ? 'Admin' : 'Kid'}</span>
            </div>
          </div>
          <button
            onClick={() => { setToken(null); router.replace('/login'); }}
            className="text-sm text-slate-600 hover:text-slate-900 dark:hover:text-white">
            Log out
          </button>
        </div>
        <nav className="max-w-5xl mx-auto px-2 pb-2 flex gap-1 overflow-x-auto">
          {nav.map((n) => {
            const active = path === n.href;
            return (
              <Link
                key={n.href}
                href={n.href}
                className={
                  'px-3 py-1.5 rounded-md text-sm whitespace-nowrap ' +
                  (active
                    ? 'bg-brand-500 text-white'
                    : 'text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800')
                }
              >
                {n.label}
              </Link>
            );
          })}
        </nav>
      </header>
      <main className="flex-1 max-w-5xl w-full mx-auto p-4 sm:p-6">{children}</main>
    </div>
  );
}
