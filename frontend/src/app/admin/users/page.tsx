'use client';

import { useEffect, useState } from 'react';
import AppShell from '@/components/AppShell';
import { api, Kid } from '@/lib/api';

const PALETTE = ['#4263eb', '#22c55e', '#f59e0b', '#ec4899', '#06b6d4', '#a855f7', '#ef4444'];

export default function AdminUsersPage() {
  return (
    <AppShell requireRole="ADMIN">
      <Inner />
    </AppShell>
  );
}

function Inner() {
  const [kids, setKids] = useState<Kid[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({ username: '', password: '', displayName: '', avatarColor: PALETTE[0] });

  useEffect(() => { reload(); }, []);
  function reload() {
    api.listKids().then(setKids).catch((e) => setError(e.message));
  }

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.createKid(form);
      setForm({ username: '', password: '', displayName: '', avatarColor: PALETTE[0] });
      reload();
    } catch (err: any) { setError(err.message); }
    finally { setBusy(false); }
  }

  async function reset(k: Kid) {
    const np = window.prompt(`New password for ${k.displayName}:`);
    if (!np) return;
    try { await api.resetKidPassword(k.id, np); alert('Password updated.'); } catch (e: any) { alert(e.message); }
  }

  async function remove(k: Kid) {
    if (!window.confirm(`Delete ${k.displayName}? Their tasks and history will be removed.`)) return;
    try { await api.deleteKid(k.id); reload(); } catch (e: any) { alert(e.message); }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Kids</h1>

      <form onSubmit={create} className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-4 space-y-3">
        <h2 className="font-semibold">Add a new kid</h2>
        <div className="grid sm:grid-cols-2 gap-3">
          <Input label="Display name" value={form.displayName} onChange={(v) => setForm({ ...form, displayName: v })} required />
          <Input label="Username" value={form.username} onChange={(v) => setForm({ ...form, username: v })} required autoComplete="off" />
          <Input label="Password" type="password" value={form.password} onChange={(v) => setForm({ ...form, password: v })} required autoComplete="new-password" />
          <div>
            <span className="text-sm font-medium">Avatar color</span>
            <div className="flex gap-2 mt-1 flex-wrap">
              {PALETTE.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setForm({ ...form, avatarColor: c })}
                  className={'w-8 h-8 rounded-full ' + (form.avatarColor === c ? 'ring-2 ring-offset-2 ring-slate-800' : '')}
                  style={{ background: c }}
                  aria-label={'pick ' + c}
                />
              ))}
            </div>
          </div>
        </div>
        {error && <div className="text-sm text-red-600">{error}</div>}
        <button disabled={busy} className="rounded-lg bg-brand-500 hover:bg-brand-600 text-white font-semibold py-2 px-4">
          {busy ? 'Creating…' : 'Create kid'}
        </button>
      </form>

      <section className="space-y-2">
        <h2 className="font-semibold">Current kids ({kids?.length ?? 0})</h2>
        {kids?.length === 0 && <p className="text-sm text-slate-500">No kids yet.</p>}
        <ul className="space-y-2">
          {kids?.map((k) => (
            <li key={k.id} className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-4 flex items-center gap-3">
              <span className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold" style={{ background: k.avatarColor }}>
                {k.displayName.charAt(0).toUpperCase()}
              </span>
              <div className="flex-1 min-w-0">
                <div className="font-semibold truncate">{k.displayName}</div>
                <div className="text-xs text-slate-500 truncate">@{k.username}</div>
              </div>
              <button onClick={() => reset(k)} className="text-sm text-slate-600 hover:text-slate-900 dark:hover:text-white">Reset password</button>
              <button onClick={() => remove(k)} className="text-sm text-red-600 hover:text-red-700">Delete</button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

function Input({ label, value, onChange, type = 'text', required, autoComplete }: {
  label: string; value: string; onChange: (v: string) => void; type?: string; required?: boolean; autoComplete?: string;
}) {
  return (
    <label className="block">
      <span className="text-sm font-medium">{label}</span>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        type={type}
        required={required}
        autoComplete={autoComplete}
        className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500"
      />
    </label>
  );
}
