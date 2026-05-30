'use client';

import { useEffect, useState } from 'react';
import AppShell from '@/components/AppShell';
import { api, Kid, Task } from '@/lib/api';

// Two groups so the picker can show a labelled break in the UI.
const ICONS_CHORES = ['🧹', '🛏️', '🦷', '🍽️', '🐕', '🚿', '🗑️', '👕', '🥗', '📚'];
// School / homework specific to the kids' weekly stack:
//   🏃 exercise · 🧪 science experiment · 🎬 science video watching
//   🎯 Tassomai · 🔢 Sparx Math · 📖 Sparx Reader · 💬 LanguageNut
//   🦉 Professor Assessor · 📘 Seneca
const ICONS_SCHOOL = ['🏃', '🧪', '🎬', '🎯', '🔢', '📖', '💬', '🦉', '📘'];
const SCHOOL_HINTS: Record<string, string> = {
  '🏃': 'Exercise',
  '🧪': 'Science experiment',
  '🎬': 'Science video',
  '🎯': 'Tassomai',
  '🔢': 'Sparx Math',
  '📖': 'Sparx Reader',
  '💬': 'LanguageNut',
  '🦉': 'Professor Assessor',
  '📘': 'Seneca',
};

type FormValues = {
  title: string;
  description: string;
  points: number;
  icon: string;
  recurrence: Task['recurrence'];
};

const EMPTY_FORM: FormValues = {
  title: '',
  description: '',
  points: 1,
  icon: ICONS_CHORES[0],
  recurrence: 'DAILY',
};

export default function AdminTasksPage() {
  return (
    <AppShell requireRole="ADMIN">
      <Inner />
    </AppShell>
  );
}

function Inner() {
  const [tasks, setTasks] = useState<Task[] | null>(null);
  const [kids, setKids] = useState<Kid[]>([]);
  const [assignees, setAssignees] = useState<Record<number, number[]>>({});
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);

  useEffect(() => { reload(); }, []);

  async function reload() {
    try {
      const [ts, ks] = await Promise.all([api.listTasks(), api.listKids()]);
      setTasks(ts);
      setKids(ks);
      const map: Record<number, number[]> = {};
      await Promise.all(ts.map(async (t) => { map[t.id] = await api.taskAssignees(t.id); }));
      setAssignees(map);
    } catch (e: any) { setError(e.message); }
  }

  async function create(values: FormValues) {
    await api.createTask(values);
    reload();
  }

  async function save(id: number, values: FormValues) {
    await api.updateTask(id, values);
    setEditingId(null);
    reload();
  }

  async function toggleActive(t: Task) {
    try { await api.updateTask(t.id, { active: !t.active }); reload(); } catch (e: any) { alert(e.message); }
  }

  async function remove(t: Task) {
    if (!window.confirm(`Delete "${t.title}"? Completion history will be removed.`)) return;
    try { await api.deleteTask(t.id); reload(); } catch (e: any) { alert(e.message); }
  }

  async function toggleAssign(taskId: number, kidId: number, on: boolean) {
    try {
      if (on) await api.assign(taskId, kidId); else await api.unassign(taskId, kidId);
      const ids = await api.taskAssignees(taskId);
      setAssignees((s) => ({ ...s, [taskId]: ids }));
    } catch (e: any) { alert(e.message); }
  }

  if (error) return <div className="text-red-600">{error}</div>;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Tasks</h1>

      <section className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-4 space-y-3">
        <h2 className="font-semibold">Create a new task</h2>
        <TaskForm
          initial={EMPTY_FORM}
          submitLabel="Create task"
          onSubmit={create}
          resetAfterSubmit
        />
      </section>

      <section className="space-y-2">
        <h2 className="font-semibold">All tasks</h2>
        {tasks?.length === 0 && <p className="text-sm text-slate-500">No tasks yet.</p>}
        <ul className="space-y-2">
          {tasks?.map((t) => {
            const isEditing = editingId === t.id;
            return (
              <li key={t.id} className={'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-4 space-y-3 ' + (t.active ? '' : 'opacity-60')}>
                {isEditing ? (
                  <TaskForm
                    initial={{
                      title: t.title,
                      description: t.description ?? '',
                      points: t.points,
                      icon: t.icon ?? ICONS_CHORES[0],
                      recurrence: t.recurrence,
                    }}
                    submitLabel="Save changes"
                    onSubmit={(v) => save(t.id, v)}
                    onCancel={() => setEditingId(null)}
                  />
                ) : (
                  <div className="flex items-start gap-3">
                    <span className="text-2xl">{t.icon ?? '✓'}</span>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold">{t.title}</span>
                        <span className="text-xs px-2 py-0.5 rounded-full bg-brand-50 text-brand-700">{t.points} pt</span>
                        <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300">{t.recurrence}</span>
                        {!t.active && <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700">Inactive</span>}
                      </div>
                      {t.description && <p className="text-sm text-slate-500 mt-0.5">{t.description}</p>}
                    </div>
                    <div className="flex gap-3 flex-wrap justify-end">
                      <button onClick={() => setEditingId(t.id)} className="text-sm text-brand-700 hover:text-brand-600 dark:text-brand-100 font-medium">Edit</button>
                      <button onClick={() => toggleActive(t)} className="text-sm text-slate-600 hover:text-slate-900 dark:hover:text-white">
                        {t.active ? 'Deactivate' : 'Activate'}
                      </button>
                      <button onClick={() => remove(t)} className="text-sm text-red-600 hover:text-red-700">Delete</button>
                    </div>
                  </div>
                )}

                {!isEditing && (
                  <div>
                    <div className="text-xs text-slate-500 mb-1">Assign to:</div>
                    <div className="flex flex-wrap gap-2">
                      {kids.length === 0 && <span className="text-xs text-slate-500">Add a kid first.</span>}
                      {kids.map((k) => {
                        const on = (assignees[t.id] ?? []).includes(k.id);
                        return (
                          <button
                            key={k.id}
                            onClick={() => toggleAssign(t.id, k.id, !on)}
                            className={'inline-flex items-center gap-2 px-3 py-1 rounded-full text-sm border ' +
                              (on ? 'bg-brand-500 text-white border-brand-500' : 'bg-white dark:bg-slate-950 border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-200')}
                          >
                            <span className="w-2.5 h-2.5 rounded-full" style={{ background: k.avatarColor }} />
                            {k.displayName}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      </section>
    </div>
  );
}

function TaskForm({
  initial, submitLabel, onSubmit, onCancel, resetAfterSubmit,
}: {
  initial: FormValues;
  submitLabel: string;
  onSubmit: (v: FormValues) => Promise<void> | void;
  onCancel?: () => void;
  resetAfterSubmit?: boolean;
}) {
  const [form, setForm] = useState<FormValues>(initial);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr(null);
    try {
      await onSubmit(form);
      if (resetAfterSubmit) setForm(initial);
    } catch (e: any) {
      setErr(e?.message ?? 'Failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <div className="grid sm:grid-cols-2 gap-3">
        <label className="block sm:col-span-2">
          <span className="text-sm font-medium">Title</span>
          <input
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            required
            className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </label>
        <label className="block sm:col-span-2">
          <span className="text-sm font-medium">Description (optional)</span>
          <textarea
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            rows={2}
            className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium">Points</span>
          <input
            type="number"
            min={0}
            value={form.points}
            onChange={(e) => setForm({ ...form, points: parseInt(e.target.value || '0', 10) })}
            className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2"
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium">Recurrence</span>
          <select
            value={form.recurrence}
            onChange={(e) => setForm({ ...form, recurrence: e.target.value as Task['recurrence'] })}
            className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2"
          >
            <option value="DAILY">Daily</option>
            <option value="WEEKLY">Weekly</option>
            <option value="ONCE">Once</option>
          </select>
        </label>
        <div className="sm:col-span-2 space-y-2">
          <span className="text-sm font-medium">Icon</span>
          <IconGroup label="Household" icons={ICONS_CHORES} selected={form.icon} onPick={(i) => setForm({ ...form, icon: i })} />
          <IconGroup label="Homework / school" icons={ICONS_SCHOOL} selected={form.icon} onPick={(i) => setForm({ ...form, icon: i })} hints={SCHOOL_HINTS} />
        </div>
      </div>
      {err && <div className="text-sm text-red-600">{err}</div>}
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={busy}
          className="rounded-lg bg-brand-500 hover:bg-brand-600 disabled:opacity-60 text-white font-semibold py-2 px-4"
        >
          {busy ? 'Saving…' : submitLabel}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="rounded-lg border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-200 py-2 px-4 hover:bg-slate-50 dark:hover:bg-slate-800"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}

function IconGroup({
  label, icons, selected, onPick, hints,
}: {
  label: string;
  icons: string[];
  selected: string | null | undefined;
  onPick: (i: string) => void;
  hints?: Record<string, string>;
}) {
  return (
    <div>
      <div className="text-xs text-slate-500 mb-1">{label}</div>
      <div className="flex flex-wrap gap-2">
        {icons.map((i) => (
          <button
            key={i}
            type="button"
            title={hints?.[i] ?? i}
            onClick={() => onPick(i)}
            className={'w-10 h-10 rounded-lg border text-lg ' + (selected === i
              ? 'border-brand-500 bg-brand-50'
              : 'border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950')}
          >{i}</button>
        ))}
      </div>
    </div>
  );
}
