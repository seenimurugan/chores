'use client';

import { useEffect, useState } from 'react';
import AppShell from '@/components/AppShell';
import { api, Kid } from '@/lib/api';

const PALETTE = ['#4263eb', '#22c55e', '#f59e0b', '#ec4899', '#06b6d4', '#a855f7', '#ef4444'];

// Curated list of common IANA timezones for the dropdown
const TIMEZONES = [
  'Europe/London',
  'Europe/Dublin',
  'Europe/Paris',
  'Europe/Berlin',
  'Europe/Amsterdam',
  'Europe/Rome',
  'Europe/Madrid',
  'Europe/Athens',
  'Europe/Istanbul',
  'Asia/Dubai',
  'Asia/Kolkata',
  'Asia/Colombo',
  'Asia/Dhaka',
  'Asia/Karachi',
  'Asia/Riyadh',
  'Asia/Singapore',
  'Asia/Tokyo',
  'Asia/Shanghai',
  'Australia/Sydney',
  'Australia/Melbourne',
  'Pacific/Auckland',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'America/Toronto',
  'America/Vancouver',
  'America/Sao_Paulo',
  'Africa/Johannesburg',
  'Africa/Lagos',
  'UTC',
];

const DEFAULT_TIMEZONE = 'Europe/London';

export default function AdminUsersPage() {
  return (
    <AppShell requireRole="ADMIN">
      <Inner />
    </AppShell>
  );
}

type CreateForm = {
  username: string;
  password: string;
  displayName: string;
  avatarColor: string;
  email: string;
  telegramChatId: string;
  timezone: string;
};

type ContactsForm = { email: string; telegramChatId: string; timezone: string };

const EMPTY_CREATE: CreateForm = {
  username: '',
  password: '',
  displayName: '',
  avatarColor: PALETTE[0],
  email: '',
  telegramChatId: '',
  timezone: DEFAULT_TIMEZONE,
};

function Inner() {
  const [kids, setKids] = useState<Kid[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<CreateForm>(EMPTY_CREATE);

  // Which kid's contacts panel is expanded for editing
  const [editingContactsId, setEditingContactsId] = useState<number | null>(null);
  const [contactsForm, setContactsForm] = useState<ContactsForm>({ email: '', telegramChatId: '', timezone: DEFAULT_TIMEZONE });
  const [contactsBusy, setContactsBusy] = useState(false);
  const [contactsError, setContactsError] = useState<string | null>(null);

  useEffect(() => { reload(); }, []);

  function reload() {
    api.listKids().then(setKids).catch((e) => setError(e.message));
  }

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const chatId = form.telegramChatId.trim() ? Number(form.telegramChatId.trim()) : null;
      await api.createKid({
        username: form.username,
        password: form.password,
        displayName: form.displayName,
        avatarColor: form.avatarColor,
        email: form.email.trim() || undefined,
        telegramChatId: chatId ?? undefined,
        timezone: form.timezone || DEFAULT_TIMEZONE,
      });
      setForm(EMPTY_CREATE);
      reload();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed');
    } finally {
      setBusy(false);
    }
  }

  async function reset(k: Kid) {
    const np = window.prompt(`New password for ${k.displayName}:`);
    if (!np) return;
    try { await api.resetKidPassword(k.id, np); alert('Password updated.'); } catch (e: unknown) { alert(e instanceof Error ? e.message : 'Failed'); }
  }

  async function remove(k: Kid) {
    if (!window.confirm(`Delete ${k.displayName}? Their tasks and history will be removed.`)) return;
    try { await api.deleteKid(k.id); reload(); } catch (e: unknown) { alert(e instanceof Error ? e.message : 'Failed'); }
  }

  function openContactsEdit(k: Kid) {
    setEditingContactsId(k.id);
    setContactsForm({
      email: k.email ?? '',
      telegramChatId: k.telegramChatId != null ? String(k.telegramChatId) : '',
      timezone: k.timezone ?? DEFAULT_TIMEZONE,
    });
    setContactsError(null);
  }

  async function saveContacts(kidId: number) {
    setContactsBusy(true);
    setContactsError(null);
    try {
      const chatId = contactsForm.telegramChatId.trim() ? Number(contactsForm.telegramChatId.trim()) : null;
      await api.updateKidContacts(kidId, {
        email: contactsForm.email.trim() || null,
        telegramChatId: chatId,
        timezone: contactsForm.timezone || DEFAULT_TIMEZONE,
      });
      setEditingContactsId(null);
      reload();
    } catch (e: unknown) {
      setContactsError(e instanceof Error ? e.message : 'Failed');
    } finally {
      setContactsBusy(false);
    }
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
          <Input
            label="Email (for reminders — optional)"
            type="email"
            value={form.email}
            onChange={(v) => setForm({ ...form, email: v })}
          />
          <Input
            label="Telegram chat ID (optional)"
            type="text"
            value={form.telegramChatId}
            onChange={(v) => setForm({ ...form, telegramChatId: v })}
            placeholder="e.g. 123456789"
          />
          <TimezoneSelect
            label="Timezone"
            value={form.timezone}
            onChange={(v) => setForm({ ...form, timezone: v })}
          />
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
            <li key={k.id} className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-4 space-y-3">
              <div className="flex items-center gap-3">
                <span className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold" style={{ background: k.avatarColor }}>
                  {k.displayName.charAt(0).toUpperCase()}
                </span>
                <div className="flex-1 min-w-0">
                  <div className="font-semibold truncate">{k.displayName}</div>
                  <div className="text-xs text-slate-500 truncate">@{k.username}</div>
                  <div className="text-xs text-slate-400 mt-0.5 space-x-2">
                    {k.email && <span>Email: {k.email}</span>}
                    {k.telegramChatId && <span>TG: {k.telegramChatId}</span>}
                    <span>TZ: {k.timezone ?? DEFAULT_TIMEZONE}</span>
                  </div>
                </div>
                <button onClick={() => reset(k)} className="text-sm text-slate-600 hover:text-slate-900 dark:hover:text-white">Reset password</button>
                <button
                  onClick={() => editingContactsId === k.id ? setEditingContactsId(null) : openContactsEdit(k)}
                  className="text-sm text-brand-700 hover:text-brand-600 dark:text-brand-100 font-medium"
                >
                  {editingContactsId === k.id ? 'Cancel' : 'Edit contacts'}
                </button>
                <button onClick={() => remove(k)} className="text-sm text-red-600 hover:text-red-700">Delete</button>
              </div>

              {editingContactsId === k.id && (
                <div className="border-t border-slate-100 dark:border-slate-800 pt-3 space-y-3">
                  <h3 className="text-sm font-semibold">Contact info for reminders</h3>
                  <div className="grid sm:grid-cols-2 gap-3">
                    <Input
                      label="Email"
                      type="email"
                      value={contactsForm.email}
                      onChange={(v) => setContactsForm({ ...contactsForm, email: v })}
                      placeholder="kid@example.com"
                    />
                    <Input
                      label="Telegram chat ID"
                      type="text"
                      value={contactsForm.telegramChatId}
                      onChange={(v) => setContactsForm({ ...contactsForm, telegramChatId: v })}
                      placeholder="e.g. 123456789"
                    />
                    <TimezoneSelect
                      label="Timezone"
                      value={contactsForm.timezone}
                      onChange={(v) => setContactsForm({ ...contactsForm, timezone: v })}
                    />
                  </div>
                  {contactsError && <div className="text-sm text-red-600">{contactsError}</div>}
                  <button
                    disabled={contactsBusy}
                    onClick={() => saveContacts(k.id)}
                    className="rounded-lg bg-brand-500 hover:bg-brand-600 disabled:opacity-60 text-white font-semibold py-1.5 px-3 text-sm"
                  >
                    {contactsBusy ? 'Saving…' : 'Save contacts'}
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

function Input({ label, value, onChange, type = 'text', required, autoComplete, placeholder }: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  required?: boolean;
  autoComplete?: string;
  placeholder?: string;
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
        placeholder={placeholder}
        className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500"
      />
    </label>
  );
}

function TimezoneSelect({ label, value, onChange }: {
  label: string;
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <label className="block">
      <span className="text-sm font-medium">{label}</span>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500"
      >
        {TIMEZONES.map((tz) => (
          <option key={tz} value={tz}>{tz}</option>
        ))}
      </select>
    </label>
  );
}
