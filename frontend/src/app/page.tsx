'use client';

import { useEffect, useState } from 'react';
import AppShell from '@/components/AppShell';
import { api, Me, MyTask } from '@/lib/api';

export default function KidHome() {
  return (
    <AppShell requireRole="KID">
      <KidTaskList />
    </AppShell>
  );
}

function toDateStr(d: Date): string {
  // YYYY-MM-DD in local time
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function addDays(d: Date, n: number): Date {
  const copy = new Date(d);
  copy.setDate(copy.getDate() + n);
  return copy;
}

function KidTaskList() {
  const [me, setMe] = useState<Me | null>(null);
  const [selectedDate, setSelectedDate] = useState<Date>(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  });
  const [tasks, setTasks] = useState<MyTask[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState<Set<number>>(new Set());

  useEffect(() => {
    api.me().then(setMe).catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!me) return;
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [me, selectedDate]);

  function reload() {
    setTasks(null);
    const dateStr = toDateStr(selectedDate);
    api.myTasks(dateStr).then(setTasks).catch((e) => setError(e.message));
  }

  async function toggle(t: MyTask) {
    const next = !t.done;
    const dateStr = toDateStr(selectedDate);
    setTasks((cur) => cur?.map((x) => (x.id === t.id ? { ...x, done: next } : x)) ?? cur);
    setPending((s) => new Set(s).add(t.id));
    try {
      await api.checkTask(t.id, next, dateStr);
    } catch (e: any) {
      setError(e.message);
      reload();
    } finally {
      setPending((s) => { const c = new Set(s); c.delete(t.id); return c; });
    }
  }

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const editWindowDays = me?.editWindowDays ?? 0;
  const earliest = addDays(today, -editWindowDays);

  const canGoPrev = selectedDate > earliest;
  const canGoNext = selectedDate < today;
  const isToday = toDateStr(selectedDate) === toDateStr(today);
  const withinWindow = selectedDate >= earliest && selectedDate <= today;

  const dateLabel = isToday
    ? `Today · ${selectedDate.toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' })}`
    : selectedDate.toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' });

  if (error) return <div className="text-red-600">{error}</div>;
  if (!me) return <div className="text-slate-500">Loading…</div>;

  const doneCount = tasks?.filter((t) => t.done).length ?? 0;
  const totalCount = tasks?.length ?? 0;

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-semibold">
            {isToday ? "Today's chores" : "Chores"}
          </h1>
          {/* Date navigator */}
          <div className="flex items-center gap-2 mt-1">
            <button
              aria-label="Previous day"
              onClick={() => setSelectedDate((d) => addDays(d, -1))}
              disabled={!canGoPrev}
              className="rounded-full w-7 h-7 flex items-center justify-center border border-slate-300 dark:border-slate-700 disabled:opacity-30 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              ‹
            </button>
            <span className="text-sm text-slate-500 min-w-[180px] text-center">{dateLabel}</span>
            <button
              aria-label="Next day"
              onClick={() => setSelectedDate((d) => addDays(d, 1))}
              disabled={!canGoNext}
              className="rounded-full w-7 h-7 flex items-center justify-center border border-slate-300 dark:border-slate-700 disabled:opacity-30 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              ›
            </button>
          </div>
        </div>
        <div className="text-right">
          <div className="text-3xl font-bold">{doneCount}<span className="text-slate-400">/{totalCount}</span></div>
          <div className="text-xs text-slate-500">done</div>
        </div>
      </div>

      {!tasks && <div className="text-slate-500">Loading your chores…</div>}

      {tasks && tasks.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 dark:border-slate-700 p-8 text-center text-slate-500">
          No chores assigned yet. Ask the admin!
        </div>
      )}

      {tasks && (
        <ul className="space-y-2">
          {tasks.map((t) => (
            <li key={t.id} className="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-800 px-4 py-3 flex items-center gap-3">
              <button
                aria-label={t.done ? 'Mark as not done' : 'Mark as done'}
                onClick={() => toggle(t)}
                disabled={pending.has(t.id) || !withinWindow}
                className={'kid-check ' + (t.done ? 'done' : 'notdone') + (!withinWindow ? ' opacity-40 cursor-not-allowed' : '')}
              >
                <span className="text-xl font-bold leading-none">{t.done ? '✓' : '✗'}</span>
              </button>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  {t.icon && <span className="text-lg">{t.icon}</span>}
                  <span className={'font-semibold truncate ' + (t.done ? 'line-through text-slate-400' : '')}>{t.title}</span>
                </div>
                {t.description && <p className="text-xs text-slate-500 mt-0.5 line-clamp-2">{t.description}</p>}
              </div>
              <span className="text-xs px-2 py-1 rounded-full bg-brand-50 text-brand-700 font-medium shrink-0">
                {t.points} pt
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
