'use client';

import { useEffect, useState } from 'react';
import AppShell from '@/components/AppShell';
import { api, Kid, ChoreOverviewRow } from '@/lib/api';

const PERIODS = [
  { value: 'this-week',  label: 'This week' },
  { value: 'last-week',  label: 'Last week' },
  { value: 'this-month', label: 'This month' },
  { value: 'last-month', label: 'Last month' },
  { value: 'this-year',  label: 'This year' },
  { value: 'last-year',  label: 'Last year' },
] as const;

type Period = typeof PERIODS[number]['value'];

export default function AdminRemindersPage() {
  return (
    <AppShell requireRole="ADMIN">
      <Inner />
    </AppShell>
  );
}

function Inner() {
  const [kids, setKids] = useState<Kid[] | null>(null);
  const [selectedKidId, setSelectedKidId] = useState<number | null>(null);
  const [period, setPeriod] = useState<Period>('this-week');
  const [rows, setRows] = useState<ChoreOverviewRow[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load kids list on mount
  useEffect(() => {
    api.listKids()
      .then((ks) => {
        setKids(ks);
        if (ks.length > 0) setSelectedKidId(ks[0].id);
      })
      .catch((e) => setError(e.message));
  }, []);

  // Load overview whenever selected kid or period changes
  useEffect(() => {
    if (selectedKidId == null) return;
    setLoading(true);
    setError(null);
    api.reminderOverview(selectedKidId, period)
      .then(setRows)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [selectedKidId, period]);

  if (!kids) return <div className="text-slate-500">Loading…</div>;

  return (
    <div className="space-y-6">
      {/* ── Header row ── */}
      <div className="flex items-end justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-semibold">Reminders</h1>
          <p className="text-sm text-slate-500">Per-kid at-risk chore tracker and reminder log</p>
        </div>

        {/* Period selector — same style as the dashboard days selector */}
        <select
          value={period}
          onChange={(e) => setPeriod(e.target.value as Period)}
          className="rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-1.5 text-sm"
        >
          {PERIODS.map((p) => (
            <option key={p.value} value={p.value}>{p.label}</option>
          ))}
        </select>
      </div>

      {kids.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 dark:border-slate-700 p-8 text-center text-slate-500">
          No kids yet — create one in <a className="underline" href="/admin/users">Kids</a>.
        </div>
      ) : (
        <>
          {/* ── Kid selector ── */}
          <div className="flex items-center gap-3">
            <label htmlFor="kid-select" className="text-sm font-medium">Kid:</label>
            <select
              id="kid-select"
              value={selectedKidId ?? ''}
              onChange={(e) => setSelectedKidId(Number(e.target.value))}
              className="rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-1.5 text-sm"
            >
              {kids.map((k) => (
                <option key={k.id} value={k.id}>{k.displayName}</option>
              ))}
            </select>
          </div>

          {error && <div className="text-sm text-red-600">{error}</div>}

          {loading ? (
            <div className="text-slate-500">Loading overview…</div>
          ) : rows == null ? null : rows.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-300 dark:border-slate-700 p-8 text-center text-slate-500">
              This kid has no chores with a weekly target set.
            </div>
          ) : (
            /* ── Overview table — wrapped in a card matching the dashboard ── */
            <section className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-slate-50 dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold text-slate-700 dark:text-slate-300">Chore</th>
                    <th className="px-4 py-3 text-center font-semibold text-slate-700 dark:text-slate-300">Weekly goal</th>
                    <th className="px-4 py-3 text-center font-semibold text-slate-700 dark:text-slate-300">
                      Done <span className="font-normal text-slate-400">({PERIODS.find(p => p.value === period)?.label})</span>
                    </th>
                    <th className="px-4 py-3 text-center font-semibold text-slate-700 dark:text-slate-300">
                      Reminders sent <span className="font-normal text-slate-400">({PERIODS.find(p => p.value === period)?.label})</span>
                    </th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-700 dark:text-slate-300">
                      Last reminder <span className="font-normal text-slate-400">({PERIODS.find(p => p.value === period)?.label})</span>
                    </th>
                    <th className="px-4 py-3 text-center font-semibold text-slate-700 dark:text-slate-300">
                      Status <span className="font-normal text-slate-400">(this week)</span>
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {rows.map((r) => (
                    <tr
                      key={r.choreId}
                      className={r.status === 'AT_RISK' ? 'bg-amber-50 dark:bg-amber-950/30' : ''}
                    >
                      {/* Icon + title */}
                      <td className="px-4 py-3 font-medium">
                        <div className="flex items-center gap-2">
                          {r.icon && (
                            <span className="text-lg leading-none" aria-hidden="true">{r.icon}</span>
                          )}
                          <span>{r.choreTitle}</span>
                        </div>
                      </td>

                      {/* Weekly goal */}
                      <td className="px-4 py-3 text-center">{r.weeklyTarget}</td>

                      {/* Done in period */}
                      <td className="px-4 py-3 text-center">
                        <span className={r.completionsInPeriod >= r.weeklyTarget ? 'text-green-600 font-semibold' : ''}>
                          {r.completionsInPeriod}
                        </span>
                      </td>

                      {/* Reminders sent in period */}
                      <td className="px-4 py-3 text-center">{r.remindersSentInPeriod}</td>

                      {/* Last reminder in period */}
                      <td className="px-4 py-3 text-slate-500 text-xs">
                        {r.lastReminderInPeriod
                          ? new Date(r.lastReminderInPeriod).toLocaleString()
                          : <span className="italic">Never</span>}
                      </td>

                      {/* Live current-week status badge */}
                      <td className="px-4 py-3 text-center">
                        {r.status === 'AT_RISK' ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 dark:bg-amber-900/40 text-amber-800 dark:text-amber-300 px-2 py-0.5 text-xs font-semibold">
                            ⚠ AT_RISK
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 rounded-full bg-green-100 dark:bg-green-900/40 text-green-800 dark:text-green-300 px-2 py-0.5 text-xs font-semibold">
                            ✓ ON_TRACK
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}
        </>
      )}
    </div>
  );
}
