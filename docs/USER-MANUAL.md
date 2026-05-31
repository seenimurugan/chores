# Chores — usage guide

How to actually use the app, day-to-day. Two roles: **admin** (parent) and **kid**.

URL: **https://chores.stoat-perch.ts.net** — works on phone Safari, no app to install.

---

## Access — every URL that hits this app

| Who needs it | URL |
|---|---|
| **Kids on their phones / Mum on her phone** | https://chores.stoat-perch.ts.net |
| **You on this Mac (browser)** | https://chores.stoat-perch.ts.net (also works — Mac is on the tailnet) |
| **Debugging from this Mac (no Tailscale)** | `kubectl -n homelab port-forward svc/chores-frontend 3000:3000` → http://localhost:3000 |
| **Hitting the API directly (curl/Postman)** | https://chores.stoat-perch.ts.net/api/... — JWT in `Authorization: Bearer <token>` |
| **Cluster-internal (other pods or Mac shell)** | Frontend: `http://chores-frontend.homelab.svc.cluster.local` · Backend: `http://chores-backend.homelab.svc.cluster.local:8080` |
| **DB direct (psql / GUI)** | `shared-postgres.homelab.svc.cluster.local:5432`, db `kidstasks`, user `kidstasks` — see [Maintenance — Postgres access](MAINTENANCE.md#postgres-access) |

The two paths through the Tailscale ingress on `chores.stoat-perch.ts.net`:
- `/api/*` and `/actuator/*` → backend pod (Spring Boot, port 8080)
- everything else → frontend pod (Next.js, port 3000)

So you can hit the same hostname from a browser and from `curl` and they "just work" — no separate `api.` subdomain, no CORS pre-flights.

---

# Admin guide

## 1. First login (once)

1. Open https://chores.stoat-perch.ts.net.
2. Sign in: username `admin`, password `admin`.
3. **Change the password immediately** — see [Maintenance → Rotate admin password](MAINTENANCE.md#rotate-admin-password). This is not done from the UI (no "change password" screen for admin); it's a one-command DB update.

## 2. Add a kid

1. Top nav → **Kids**.
2. Fill **Add a new kid**:
   - **Display name** — what the kid sees and the dashboard shows ("Asha", "Rohan")
   - **Username** — what they type to log in ("asha")
   - **Password** — give them something simple but unique. You can reset it later.
   - **Avatar colour** — pick from the palette. This is also the line colour on the admin comparison chart.
3. Click **Create kid**. They appear in the list below.
4. Tell the kid their username + password.

Each kid only sees their **own** chores and stats — never another kid's.

### Reset a kid's password
**Kids** page → **Reset password** next to their name → type new password → done.

### Delete a kid
**Kids** page → **Delete**. Removes the account, their assignments, and their completion history. Permanent.

## 3. Create a chore

1. Top nav → **Tasks**.
2. Fill **Create a new task**:
   - **Title** — e.g. `Brush teeth`
   - **Description** (optional) — e.g. `morning + night`
   - **Points** — gamification number shown next to the chore (0–N)
   - **Recurrence** — `DAILY` / `WEEKLY` / `ONCE`. Currently informational; completion is tracked per-day either way.
   - **Icon** — pick an emoji
3. Click **Create task**.

## 4. Assign a chore to one or more kids

On each task card, the **Assign to:** row shows a pill per kid. Click a pill to toggle assignment:
- blue + filled = assigned (kid sees it on Today)
- outline = not assigned

A chore appears on a kid's **Today** list only while assigned. Unassign to hide without deleting (history is kept).

## 5. Deactivate vs. delete a chore

- **Deactivate** — keeps the chore + its completion history, but hides it from kids' lists. Use this when a chore is paused (e.g. weekend break).
- **Delete** — removes the chore *and* its completion history. Permanent.

## 6. Watch the graphs

**Dashboard** (top nav) shows for the last 7/14/30 days:

- **Per-kid card** — completion rate %, total chores done, active chores assigned. Big number is `done ÷ expected` over the window.
- **Daily comparison line chart** — one line per kid in their avatar colour. Each point = number of chores that kid ticked off on that day.

Switch the time window with the dropdown (top right).

## 4. Settings — per-kid edit window

Top nav → **Settings**.

Each kid can be given a configurable *edit window* — how many past days they are allowed to tick or un-tick chores. The default is **14 days** (today + 14 previous days).

| Value | Effect |
|---|---|
| `0` | Kid can only edit **today**. |
| `14` (default) | Kid can edit today plus the 14 previous days. |
| `365` | Maximum — roughly one year back. |

**To change a kid's window:**
1. Top nav → **Settings**.
2. Find the kid by name.
3. Type the new number of days in the input box.
4. Click **Save**. The response shows "Saved ✓" briefly.

The backend enforces this limit — an out-of-window check attempt returns HTTP 400. The change takes effect immediately with no restart needed.

---

# Kid guide (give this section to your kids)

## Sign in
Open the URL Mum/Dad gave you. Type your username and password. You'll stay signed in for 30 days.

## Today's chores
The home screen shows your chores for the selected date, each as a card with a big square button on the left:

- The button shows **✗** (white) when you haven't done the chore yet.
- Tap it to mark the chore done — the button turns green with a **✓**.
- Tap again to undo (in case you tapped the wrong one).

The counter at the top right shows `done / total` for the selected date.

## Viewing and editing past days

Below the "Today's chores" heading you'll see a date navigator:

```
‹   Saturday, 31 May   ›
```

- Tap **‹** to go back one day; tap **›** to go forward.
- The **›** button is greyed out on today (you can't navigate to the future).
- The **‹** button is greyed out once you reach the earliest day you're allowed to edit (set by Mum or Dad in Settings — default is 14 days back).
- You can tick or un-tick chores on any day within your edit window — the changes save immediately.
- Outside the edit window the checkboxes are shown but are disabled (greyed out) so you can still see what you did that day.

## My stats
Top nav → **My stats**. The bar chart shows how many chores you ticked off on each of the last 7, 14, or 30 days (use the dropdown to switch).

- **Active chores** — how many are assigned to you right now.
- **Total done** — chores you ticked in the chosen window.
- **Completion %** — chores you did ÷ chores you could have done.

---

## Tips

- **iPhone shortcut**: in Safari, tap Share → **Add to Home Screen** → "Chores". Becomes a one-tap app icon. Works fullscreen.
- **Forgot password (kid)**: ask the admin to reset it on the **Kids** page.
- **Forgot password (admin)**: see [Maintenance → Rotate admin password](MAINTENANCE.md#rotate-admin-password) — this needs a DB query, not the UI.
- **Two kids sharing one phone**: tap the avatar circle (top-left) → **Log out** → sign in as the other kid.
