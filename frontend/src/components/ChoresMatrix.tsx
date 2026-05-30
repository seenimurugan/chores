'use client';

import { AdminMatrix, KidMatrix, TaskCol } from '@/lib/api';

const WEEKDAY = new Intl.DateTimeFormat(undefined, { weekday: 'short' });
const DAY_MONTH = new Intl.DateTimeFormat(undefined, { day: '2-digit', month: 'short' });

const DATE_W = 96;
const KID_W = 132;

function parseLocal(day: string) {
  const [y, m, d] = day.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function isToday(day: string) {
  const t = new Date();
  const d = parseLocal(day);
  return t.getFullYear() === d.getFullYear() && t.getMonth() === d.getMonth() && t.getDate() === d.getDate();
}

function DateCell({ day }: { day: string }) {
  const d = parseLocal(day);
  const today = isToday(day);
  return (
    <div className="leading-tight">
      <div className="font-semibold">{today ? 'Today' : WEEKDAY.format(d)}</div>
      <div className="text-[11px] text-slate-500">{DAY_MONTH.format(d)}</div>
    </div>
  );
}

function KidCell({ name, color }: { name: string; color: string }) {
  return (
    <div className="flex items-center gap-2">
      <span
        className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold shrink-0"
        style={{ background: color }}
      >
        {name.charAt(0).toUpperCase()}
      </span>
      <span className="font-medium truncate">{name}</span>
    </div>
  );
}

function TaskHeader({ tasks }: { tasks: TaskCol[] }) {
  return (
    <>
      {tasks.map((t) => (
        <th
          key={t.id}
          className="font-medium px-2 py-2 text-center align-bottom whitespace-nowrap border-b border-slate-200 dark:border-slate-800"
          title={t.title}
        >
          <div className="flex flex-col items-center gap-0.5">
            {t.icon && <span className="text-lg leading-none" aria-hidden>{t.icon}</span>}
            <span className="text-[11px] normal-case text-slate-600 dark:text-slate-300 max-w-[5.5rem] truncate">
              {t.title}
            </span>
          </div>
        </th>
      ))}
    </>
  );
}

function TickCell({ done, assigned }: { done: boolean; assigned: boolean }) {
  if (!assigned) {
    return (
      <td className="px-2 py-2 text-center text-slate-300 dark:text-slate-700 select-none border-b border-slate-100 dark:border-slate-800" aria-label="not assigned">
        —
      </td>
    );
  }
  if (done) {
    return (
      <td className="px-2 py-2 text-center border-b border-slate-100 dark:border-slate-800" aria-label="done">
        <svg viewBox="0 0 20 20" className="w-5 h-5 inline text-emerald-500" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="4 11 8 15 16 6" />
        </svg>
      </td>
    );
  }
  return (
    <td className="px-2 py-2 text-center text-slate-300 dark:text-slate-600 select-none border-b border-slate-100 dark:border-slate-800" aria-label="not done">
      ·
    </td>
  );
}

const stickyBg = 'bg-white dark:bg-slate-900';

export function KidChoresMatrix({ data }: { data: KidMatrix }) {
  if (data.tasks.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-slate-300 dark:border-slate-700 p-6 text-center text-sm text-slate-500">
        No chores assigned yet.
      </div>
    );
  }
  return (
    <div className="overflow-x-auto -mx-4 sm:mx-0">
      <table className="min-w-full text-sm border-separate border-spacing-0">
        <colgroup>
          <col style={{ width: DATE_W }} />
          {data.tasks.map((t) => (
            <col key={t.id} />
          ))}
        </colgroup>
        <thead>
          <tr className="text-xs uppercase tracking-wider text-slate-500">
            <th
              className={`text-left font-medium px-3 py-2 sticky left-0 z-10 align-bottom border-b border-slate-200 dark:border-slate-800 ${stickyBg}`}
              style={{ width: DATE_W, minWidth: DATE_W }}
            >
              Date
            </th>
            <TaskHeader tasks={data.tasks} />
          </tr>
        </thead>
        <tbody>
          {data.rows.map((r) => {
            const done = new Set(r.doneTaskIds);
            return (
              <tr key={r.day} className="hover:bg-slate-50 dark:hover:bg-slate-800/40">
                <td
                  className={`px-3 py-2 whitespace-nowrap sticky left-0 z-10 border-b border-slate-100 dark:border-slate-800 ${stickyBg}`}
                  style={{ width: DATE_W, minWidth: DATE_W }}
                >
                  <DateCell day={r.day} />
                </td>
                {data.tasks.map((t) => (
                  <TickCell key={t.id} done={done.has(t.id)} assigned={true} />
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 px-4 sm:px-0">
        <span className="inline-flex items-center gap-1">
          <svg viewBox="0 0 20 20" className="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="4 11 8 15 16 6" />
          </svg>
          done
        </span>
        <span className="inline-flex items-center gap-1"><span className="text-slate-300 dark:text-slate-600">·</span> not done</span>
      </div>
    </div>
  );
}

export function AdminChoresMatrix({ data }: { data: AdminMatrix }) {
  if (data.kids.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-slate-300 dark:border-slate-700 p-6 text-center text-sm text-slate-500">
        No kids yet.
      </div>
    );
  }
  if (data.tasks.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-slate-300 dark:border-slate-700 p-6 text-center text-sm text-slate-500">
        No chores assigned yet.
      </div>
    );
  }

  const kidById = new Map(data.kids.map((k) => [k.userId, k]));

  return (
    <div className="overflow-x-auto -mx-4 sm:mx-0">
      <table className="min-w-full text-sm border-separate border-spacing-0">
        <colgroup>
          <col style={{ width: DATE_W }} />
          <col style={{ width: KID_W }} />
          {data.tasks.map((t) => (
            <col key={t.id} />
          ))}
        </colgroup>
        <thead>
          <tr className="text-xs uppercase tracking-wider text-slate-500">
            <th
              className={`text-left font-medium px-3 py-2 sticky left-0 z-20 align-bottom border-b border-slate-200 dark:border-slate-800 ${stickyBg}`}
              style={{ width: DATE_W, minWidth: DATE_W }}
            >
              Date
            </th>
            <th
              className={`text-left font-medium px-3 py-2 sticky z-20 align-bottom border-b border-slate-200 dark:border-slate-800 whitespace-nowrap ${stickyBg}`}
              style={{ width: KID_W, minWidth: KID_W, left: DATE_W }}
            >
              Kid
            </th>
            <TaskHeader tasks={data.tasks} />
          </tr>
        </thead>
        <tbody>
          {data.rows.map((r, i) => {
            const kid = kidById.get(r.userId);
            if (!kid) return null;
            const done = new Set(r.doneTaskIds);
            const assigned = new Set(kid.assignedTaskIds);
            const prev = data.rows[i - 1];
            const isFirstOfDay = !prev || prev.day !== r.day;
            return (
              <tr
                key={`${r.day}-${r.userId}`}
                className={
                  'hover:bg-slate-50 dark:hover:bg-slate-800/40 ' +
                  (isFirstOfDay ? 'border-t-2 border-slate-200 dark:border-slate-700' : '')
                }
              >
                <td
                  className={`px-3 py-2 whitespace-nowrap sticky left-0 z-10 border-b border-slate-100 dark:border-slate-800 ${stickyBg}`}
                  style={{ width: DATE_W, minWidth: DATE_W }}
                >
                  {isFirstOfDay ? <DateCell day={r.day} /> : <span className="sr-only">{r.day}</span>}
                </td>
                <td
                  className={`px-3 py-2 whitespace-nowrap sticky z-10 border-b border-slate-100 dark:border-slate-800 ${stickyBg}`}
                  style={{ width: KID_W, minWidth: KID_W, left: DATE_W }}
                >
                  <KidCell name={kid.displayName} color={kid.avatarColor} />
                </td>
                {data.tasks.map((t) => (
                  <TickCell key={t.id} done={done.has(t.id)} assigned={assigned.has(t.id)} />
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 px-4 sm:px-0">
        <span className="inline-flex items-center gap-1">
          <svg viewBox="0 0 20 20" className="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="4 11 8 15 16 6" />
          </svg>
          done
        </span>
        <span className="inline-flex items-center gap-1"><span className="text-slate-300 dark:text-slate-600">·</span> not done</span>
        <span className="inline-flex items-center gap-1"><span className="text-slate-300 dark:text-slate-700">—</span> not assigned</span>
      </div>
    </div>
  );
}
