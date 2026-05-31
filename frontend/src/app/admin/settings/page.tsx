'use client';

import { useEffect, useState } from 'react';
import AppShell from '@/components/AppShell';
import { api, Kid } from '@/lib/api';

export default function SettingsPage() {
  return (
    <AppShell requireRole="ADMIN">
      <EditWindowSettings />
    </AppShell>
  );
}

function EditWindowSettings() {
  const [kids, setKids] = useState<Kid[] | null>(null);
  const [values, setValues] = useState<Record<number, number>>({});
  const [saving, setSaving] = useState<Record<number, boolean>>({});
  const [saved, setSaved] = useState<Record<number, boolean>>({});
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.listKids().then((ks) => {
      setKids(ks);
      const init: Record<number, number> = {};
      for (const k of ks) init[k.id] = k.editWindowDays ?? 14;
      setValues(init);
    }).catch((e) => setError(e.message));
  }, []);

  async function save(kid: Kid) {
    setSaving((s) => ({ ...s, [kid.id]: true }));
    setSaved((s) => ({ ...s, [kid.id]: false }));
    try {
      await api.updateKidEditWindow(kid.id, values[kid.id]);
      setSaved((s) => ({ ...s, [kid.id]: true }));
      setTimeout(() => setSaved((s) => ({ ...s, [kid.id]: false })), 2000);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving((s) => ({ ...s, [kid.id]: false }));
    }
  }

  if (error) return <div className="text-red-600">{error}</div>;
  if (!kids) return <div className="text-slate-500">Loading…</div>;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Settings</h1>
        <p className="text-sm text-slate-500 mt-1">
          Configure how many past days each kid can edit their chores.
          <br />
          <span className="font-medium">0</span> = today only. <span className="font-medium">14</span> = today + 14 previous days.
        </p>
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 divide-y divide-slate-100 dark:divide-slate-800">
        {kids.length === 0 && (
          <div className="p-6 text-slate-500 text-sm">No kids yet. Add kids on the Kids tab first.</div>
        )}
        {kids.map((kid) => (
          <div key={kid.id} className="flex items-center gap-4 px-5 py-4">
            <span
              className="w-9 h-9 rounded-full inline-flex items-center justify-center text-white font-bold shrink-0"
              style={{ background: kid.avatarColor }}
            >
              {kid.displayName.charAt(0).toUpperCase()}
            </span>
            <div className="flex-1 min-w-0">
              <div className="font-medium truncate">{kid.displayName}</div>
              <div className="text-xs text-slate-500 truncate">@{kid.username}</div>
            </div>
            <div className="flex items-center gap-2">
              <label className="text-sm text-slate-600 dark:text-slate-400 whitespace-nowrap">
                Edit window (days):
              </label>
              <input
                type="number"
                min={0}
                max={365}
                value={values[kid.id] ?? 14}
                onChange={(e) => setValues((v) => ({ ...v, [kid.id]: Math.max(0, Math.min(365, Number(e.target.value))) }))}
                className="w-20 rounded-md border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-2 py-1 text-sm text-center focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
              <button
                onClick={() => save(kid)}
                disabled={saving[kid.id]}
                className="px-3 py-1.5 rounded-md text-sm bg-brand-500 text-white hover:bg-brand-600 disabled:opacity-60 transition-colors"
              >
                {saving[kid.id] ? 'Saving…' : saved[kid.id] ? 'Saved ✓' : 'Save'}
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
