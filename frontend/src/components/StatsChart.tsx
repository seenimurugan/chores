'use client';

import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Tooltip, CartesianGrid, LineChart, Line } from 'recharts';

export function DailyBars({
  data,
  color = '#4263eb',
  height = 220,
}: {
  data: { day: string; done: number }[];
  color?: string;
  height?: number;
}) {
  const formatted = data.map((d) => ({ ...d, label: d.day.slice(5) }));
  return (
    <div style={{ width: '100%', height }}>
      <ResponsiveContainer>
        <BarChart data={formatted}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.3)" />
          <XAxis dataKey="label" tick={{ fontSize: 11 }} />
          <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
          <Tooltip
            cursor={{ fill: 'rgba(148,163,184,0.1)' }}
            contentStyle={{ borderRadius: 8, fontSize: 12, background: 'white', borderColor: '#cbd5e1' }}
          />
          <Bar dataKey="done" fill={color} radius={[6, 6, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function ComparisonLines({
  series,
  height = 280,
}: {
  series: { name: string; color: string; data: { day: string; done: number }[] }[];
  height?: number;
}) {
  // Merge by day across series.
  type Row = { day: string } & Record<string, string | number>;
  const dayIndex = new Map<string, Row>();
  for (const s of series) {
    for (const p of s.data) {
      const row = dayIndex.get(p.day) ?? { day: p.day };
      row[s.name] = p.done;
      dayIndex.set(p.day, row);
    }
  }
  const data = Array.from(dayIndex.values())
    .sort((a, b) => a.day.localeCompare(b.day))
    .map((row) => ({ ...row, label: row.day.slice(5) }));

  return (
    <div style={{ width: '100%', height }}>
      <ResponsiveContainer>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(148,163,184,0.3)" />
          <XAxis dataKey="label" tick={{ fontSize: 11 }} />
          <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
          <Tooltip contentStyle={{ borderRadius: 8, fontSize: 12, background: 'white', borderColor: '#cbd5e1' }} />
          {series.map((s) => (
            <Line key={s.name} type="monotone" dataKey={s.name} stroke={s.color} strokeWidth={2} dot={{ r: 3 }} />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
