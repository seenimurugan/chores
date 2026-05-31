// Same-origin API client. Backend mounted at /api via Ingress path routing.
// In dev, set NEXT_PUBLIC_API_BASE to e.g. http://localhost:8080
const BASE = process.env.NEXT_PUBLIC_API_BASE ?? '';

export type Role = 'ADMIN' | 'KID';

export type Me = {
  id: number;
  username: string;
  displayName: string;
  role: Role;
  avatarColor: string;
};

export type Kid = Me & { editWindowDays: number };

export type Task = {
  id: number;
  title: string;
  description: string | null;
  points: number;
  icon: string | null;
  recurrence: 'DAILY' | 'WEEKLY' | 'ONCE';
  active: boolean;
};

export type MyTask = Task & { done: boolean };

export type KidStats = {
  userId: number;
  displayName: string;
  avatarColor: string;
  activeTasks: number;
  totalDone: number;
  completionRate: number;
  series: { day: string; done: number }[];
};

export type TaskCol = {
  id: number;
  title: string;
  icon: string | null;
  points: number;
};

export type KidMatrix = {
  tasks: TaskCol[];
  rows: { day: string; doneTaskIds: number[] }[];
};

export type AdminMatrix = {
  tasks: TaskCol[];
  kids: { userId: number; displayName: string; avatarColor: string; assignedTaskIds: number[] }[];
  rows: { day: string; userId: number; doneTaskIds: number[] }[];
};

function token(): string | null {
  if (typeof window === 'undefined') return null;
  return window.localStorage.getItem('chores.token');
}

export function setToken(t: string | null) {
  if (typeof window === 'undefined') return;
  if (t) window.localStorage.setItem('chores.token', t);
  else window.localStorage.removeItem('chores.token');
}

async function req<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const t = token();
  if (t) headers.set('Authorization', `Bearer ${t}`);
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const res = await fetch(`${BASE}${path}`, { ...init, headers });
  if (res.status === 401) {
    setToken(null);
    if (typeof window !== 'undefined' && !window.location.pathname.endsWith('/login')) {
      window.location.href = '/login';
    }
    throw new Error('Unauthorized');
  }
  if (!res.ok) {
    let msg = res.statusText;
    try { const j = await res.json(); if (j?.message) msg = j.message; } catch {}
    throw new Error(msg || `HTTP ${res.status}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  login: (username: string, password: string) =>
    req<{ token: string; expiresInSeconds: number; user: Me }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  me: () => req<Me>('/api/auth/me'),

  // Kid
  myTasks: () => req<MyTask[]>('/api/me/tasks'),
  checkTask: (id: number, done: boolean) =>
    req<void>(`/api/me/tasks/${id}/check`, { method: 'POST', body: JSON.stringify({ done }) }),
  myStats: (days = 14) => req<KidStats>(`/api/me/stats?days=${days}`),
  myMatrix: (days = 14) => req<KidMatrix>(`/api/me/stats/matrix?days=${days}`),

  // Admin — users
  listKids: () => req<Kid[]>('/api/admin/users'),
  createKid: (b: { username: string; password: string; displayName: string; avatarColor?: string }) =>
    req<Kid>('/api/admin/users', { method: 'POST', body: JSON.stringify(b) }),
  resetKidPassword: (id: number, password: string) =>
    req<void>(`/api/admin/users/${id}/reset-password`, { method: 'POST', body: JSON.stringify({ password }) }),
  deleteKid: (id: number) => req<void>(`/api/admin/users/${id}`, { method: 'DELETE' }),
  updateKidEditWindow: (id: number, editWindowDays: number) =>
    req<Kid>(`/api/admin/users/${id}/edit-window`, { method: 'PATCH', body: JSON.stringify({ editWindowDays }) }),

  // Admin — tasks
  listTasks: () => req<Task[]>('/api/admin/tasks'),
  createTask: (b: Partial<Task>) =>
    req<Task>('/api/admin/tasks', { method: 'POST', body: JSON.stringify(b) }),
  updateTask: (id: number, b: Partial<Task>) =>
    req<Task>(`/api/admin/tasks/${id}`, { method: 'PUT', body: JSON.stringify(b) }),
  deleteTask: (id: number) => req<void>(`/api/admin/tasks/${id}`, { method: 'DELETE' }),
  taskAssignees: (id: number) => req<number[]>(`/api/admin/tasks/${id}/assignees`),
  assign: (taskId: number, userId: number) =>
    req<void>(`/api/admin/tasks/${taskId}/assign/${userId}`, { method: 'POST' }),
  unassign: (taskId: number, userId: number) =>
    req<void>(`/api/admin/tasks/${taskId}/assign/${userId}`, { method: 'DELETE' }),

  // Admin — stats
  allKidStats: (days = 14) => req<KidStats[]>(`/api/admin/stats?days=${days}`),
  adminMatrix: (days = 14) => req<AdminMatrix>(`/api/admin/stats/matrix?days=${days}`),
};
