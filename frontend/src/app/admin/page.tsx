'use client';

import { useEffect, useState, useMemo } from 'react';
import AppShell from '@/components/AppShell';
import { ComparisonLines } from '@/components/StatsChart';
import { AdminChoresMatrix } from '@/components/ChoresMatrix';
import { api, AdminMatrix, KidStats } from '@/lib/api';

export default function AdminDashboard() {
  return (
    <AppShell requireRole="ADMIN">
      <Inner />
    </AppShell>
  );
}

function Inner() {
  const [days, setDays] = useState(14);
  const [stats, setStats] = useState<KidStats[] | null>(null);
  const [matrix, setMatrix] = useState<AdminMatrix | null>(null);
  const [kidFilter, setKidFilter] = useState<number | 'all'>('all');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setStats(null);
    setMatrix(null);
    Promise.all([api.allKidStats(days), api.adminMatrix(days)])
      .then(([s, m]) => { setStats(s); setMatrix(m); })
      .catch((e) => setError(e.message));
  }, [days]);

  const filteredMatrix = useMemo<AdminMatrix | null>(() => {
    if (!matrix) return null;
    if (kidFilter === 'all') return matrix;
    const kid = matrix.kids.find((k) => k.userId === kidFilter);
    if (!kid) return { ...matrix, kids: [], rows: [] };
    const assigned = new Set(kid.assignedTaskIds);
    return {
      tasks: matrix.tasks.filter((t) => assigned.has(t.id)),
      kids: [kid],
      rows: matrix.rows.filter((r) => r.userId === kidFilter),
    };
  }, [matrix, kidFilter]);

  if (error) return <div className="text-red-600">{error}</div>;
  if (!stats || !matrix || !filteredMatrix) return <div className="text-slate-500">Loading…</div>;

  const series = stats.map((s) => ({ name: s.displayName, color: s.avatarColor, data: s.series }));

  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-semibold">Dashboard</h1>
          <p className="text-sm text-slate-500">How everyone's doing</p>
        </div>
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

      {stats.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 dark:border-slate-700 p-8 text-center text-slate-500">
          No kids yet — create one in <a className="underline" href="/admin/users">Kids</a>.
        </div>
      ) : (
        <>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {stats.map((s) => (
              <div key={s.userId} className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-4 flex items-center gap-3">
                <span
                  className="w-12 h-12 rounded-full flex items-center justify-center text-white text-xl font-bold shrink-0"
                  style={{ background: s.avatarColor }}
                >
                  {s.displayName.charAt(0).toUpperCase()}
                </span>
                <div className="flex-1 min-w-0">
                  <div className="font-semibold truncate">{s.displayName}</div>
                  <div className="text-xs text-slate-500">{s.activeTasks} chores · {s.totalDone} done</div>
                </div>
                <div className="text-right">
                  <div className="text-2xl font-bold">{s.completionRate}%</div>
                  <div className="text-[10px] text-slate-500 uppercase tracking-wider">completion</div>
                </div>
              </div>
            ))}
          </div>

          <section className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-4">
            <h2 className="font-semibold mb-2">Daily comparison</h2>
            <ComparisonLines series={series} />
          </section>

          <section className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-4">
            <div className="flex items-center justify-between flex-wrap gap-2 mb-3">
              <h2 className="font-semibold">Daily chore log</h2>
              <select
                value={kidFilter === 'all' ? 'all' : String(kidFilter)}
                onChange={(e) => setKidFilter(e.target.value === 'all' ? 'all' : parseInt(e.target.value, 10))}
                className="rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-2 py-1 text-xs"
              >
                <option value="all">All kids</option>
                {stats.map((s) => (
                  <option key={s.userId} value={s.userId}>{s.displayName}</option>
                ))}
              </select>
            </div>
            <AdminChoresMatrix data={filteredMatrix} />
          </section>
        </>
      )}
    </div>
  );
}
