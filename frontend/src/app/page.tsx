'use client';

import { useEffect, useState } from 'react';
import AppShell from '@/components/AppShell';
import { api, MyTask } from '@/lib/api';

export default function KidHome() {
  return (
    <AppShell requireRole="KID">
      <KidTaskList />
    </AppShell>
  );
}

function KidTaskList() {
  const [tasks, setTasks] = useState<MyTask[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState<Set<number>>(new Set());

  useEffect(() => { reload(); }, []);
  function reload() {
    api.myTasks().then(setTasks).catch((e) => setError(e.message));
  }

  async function toggle(t: MyTask) {
    const next = !t.done;
    setTasks((cur) => cur?.map((x) => (x.id === t.id ? { ...x, done: next } : x)) ?? cur);
    setPending((s) => new Set(s).add(t.id));
    try {
      await api.checkTask(t.id, next);
    } catch (e: any) {
      setError(e.message);
      reload();
    } finally {
      setPending((s) => { const c = new Set(s); c.delete(t.id); return c; });
    }
  }

  if (error) return <div className="text-red-600">{error}</div>;
  if (!tasks) return <div className="text-slate-500">Loading your chores…</div>;

  const today = new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' });
  const doneCount = tasks.filter((t) => t.done).length;

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Today's chores</h1>
          <p className="text-sm text-slate-500">{today}</p>
        </div>
        <div className="text-right">
          <div className="text-3xl font-bold">{doneCount}<span className="text-slate-400">/{tasks.length}</span></div>
          <div className="text-xs text-slate-500">done</div>
        </div>
      </div>

      {tasks.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 dark:border-slate-700 p-8 text-center text-slate-500">
          No chores assigned yet. Ask the admin!
        </div>
      )}

      <ul className="space-y-2">
        {tasks.map((t) => (
          <li key={t.id} className="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-800 px-4 py-3 flex items-center gap-3">
            <button
              aria-label={t.done ? 'Mark as not done' : 'Mark as done'}
              onClick={() => toggle(t)}
              disabled={pending.has(t.id)}
              className={'kid-check ' + (t.done ? 'done' : 'notdone')}
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
    </div>
  );
}
