'use client';

import { useEffect, useState } from 'react';
import AppShell from '@/components/AppShell';
import { DailyBars } from '@/components/StatsChart';
import { KidChoresMatrix } from '@/components/ChoresMatrix';
import { api, KidMatrix, KidStats } from '@/lib/api';

export default function MyStatsPage() {
  return (
    <AppShell requireRole="KID">
      <Inner />
    </AppShell>
  );
}

function Inner() {
  const [days, setDays] = useState(14);
  const [stats, setStats] = useState<KidStats | null>(null);
  const [matrix, setMatrix] = useState<KidMatrix | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setStats(null);
    setMatrix(null);
    Promise.all([api.myStats(days), api.myMatrix(days)])
      .then(([s, m]) => { setStats(s); setMatrix(m); })
      .catch((e) => setError(e.message));
  }, [days]);

  if (error) return <div className="text-red-600">{error}</div>;
  if (!stats || !matrix) return <div className="text-slate-500">Loading…</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-semibold">My stats</h1>
        <select
          value={days}
          onChange={(e) => setDays(parseInt(e.target.value, 10))}
          className="rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-1.5 text-sm"
        >
          <option value={7}>Last 7 days</option>
          <option value={14}>Last 14 days</option>
          <option value={30}>Last 30 days</option>
        </select>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
        <KpiCard label="Active chores" value={stats.activeTasks} />
        <KpiCard label="Total done" value={stats.totalDone} />
        <KpiCard label="Completion" value={`${stats.completionRate}%`} highlight />
      </div>

      <section className="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-800 p-4">
        <h2 className="font-semibold mb-2">Chores done per day</h2>
        <DailyBars data={stats.series} color={stats.avatarColor} />
      </section>

      <section className="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-800 p-4">
        <h2 className="font-semibold mb-3">Daily chore log</h2>
        <KidChoresMatrix data={matrix} />
      </section>
    </div>
  );
}

function KpiCard({ label, value, highlight }: { label: string; value: React.ReactNode; highlight?: boolean }) {
  return (
    <div className={'rounded-2xl border p-4 ' + (highlight
      ? 'bg-brand-500 text-white border-brand-500'
      : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800')}>
      <div className={'text-xs ' + (highlight ? 'text-brand-100' : 'text-slate-500')}>{label}</div>
      <div className="text-2xl font-bold mt-1">{value}</div>
    </div>
  );
}
