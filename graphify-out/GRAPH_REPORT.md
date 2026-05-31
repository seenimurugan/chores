# Graph Report - chores  (2026-05-31)

## Corpus Check
- 74 files · ~18,619 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 545 nodes · 775 edges · 53 communities (43 shown, 10 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 66 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f45a1df7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 37|Community 37]]
- [[_COMMUNITY_Community 38|Community 38]]
- [[_COMMUNITY_Community 39|Community 39]]
- [[_COMMUNITY_Community 40|Community 40]]
- [[_COMMUNITY_Community 42|Community 42]]
- [[_COMMUNITY_Community 43|Community 43]]
- [[_COMMUNITY_Community 44|Community 44]]
- [[_COMMUNITY_Community 45|Community 45]]
- [[_COMMUNITY_Community 46|Community 46]]
- [[_COMMUNITY_Community 47|Community 47]]
- [[_COMMUNITY_Community 48|Community 48]]
- [[_COMMUNITY_Community 49|Community 49]]
- [[_COMMUNITY_Community 50|Community 50]]
- [[_COMMUNITY_Community 51|Community 51]]

## God Nodes (most connected - your core abstractions)
1. `Chores Maintenance` - 19 edges
2. `TaskService` - 17 edges
3. `compilerOptions` - 17 edges
4. `AdminTaskController` - 11 edges
5. `StatsController` - 10 edges
6. `UserService` - 9 edges
7. `api` - 9 edges
8. `of()` - 9 edges
9. `PreAuthorize` - 9 edges
10. `Troubleshooting` - 9 edges

## Surprising Connections (you probably didn't know these)
- `Chores Project README` --references--> `Chores Maintenance`  [EXTRACTED]
  README.md → docs/MAINTENANCE.md
- `Chores Maintenance` --references--> `Frontend K8s Deployment`  [EXTRACTED]
  docs/MAINTENANCE.md → k8s/20-frontend.yaml
- `Backend K8s Deployment` --references--> `Backend application.yml`  [INFERRED]
  k8s/10-backend.yaml → backend/src/main/resources/application.yml
- `Chores Project README` --references--> `Chores Docs README`  [EXTRACTED]
  README.md → docs/README.md
- `Chores Maintenance` --references--> `Backend K8s Deployment`  [EXTRACTED]
  docs/MAINTENANCE.md → k8s/10-backend.yaml

## Communities (53 total, 10 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (28): AdminChoresMatrix(), DateCell(), DAY_MONTH, isToday(), KidChoresMatrix(), parseLocal(), WEEKDAY, ComparisonLines() (+20 more)

### Community 1 - "Community 1"
Cohesion: 0.17
Nodes (13): AssignedTaskRow, AdminMatrix, KidMatrix, Task, Integer, List, Long, Query (+5 more)

### Community 2 - "Community 2"
Cohesion: 0.08
Nodes (24): AuthUser, GetMapping, JwtService, PasswordEncoder, PostMapping, UserRepository, AuthUser, User (+16 more)

### Community 3 - "Community 3"
Cohesion: 0.16
Nodes (16): List, Long, String, Transactional, UserRepository, Boolean, Integer, LocalDate (+8 more)

### Community 4 - "Community 4"
Cohesion: 0.14
Nodes (14): List, Optional, String, User, List, Long, String, Transactional (+6 more)

### Community 5 - "Community 5"
Cohesion: 0.16
Nodes (17): AuthUser, GetMapping, KidMatrix, List, LocalDate, Long, KidStats, List (+9 more)

### Community 6 - "Community 6"
Cohesion: 0.09
Nodes (21): dependencies, clsx, next, react, react-dom, recharts, devDependencies, autoprefixer (+13 more)

### Community 7 - "Community 7"
Cohesion: 0.10
Nodes (20): compilerOptions, allowJs, baseUrl, esModuleInterop, incremental, isolatedModules, jsx, lib (+12 more)

### Community 8 - "Community 8"
Cohesion: 0.16
Nodes (15): Long, User, CreateKidRequest, DeleteMapping, GetMapping, PatchMapping, PostMapping, ResetPasswordRequest (+7 more)

### Community 9 - "Community 9"
Cohesion: 0.20
Nodes (12): DeleteMapping, GetMapping, Long, PostMapping, ResponseEntity, TaskService, Void, PutMapping (+4 more)

### Community 10 - "Community 10"
Cohesion: 0.26
Nodes (10): List, LocalDate, Long, Optional, Query, TaskCompletion, CompletionRow, DailyCount (+2 more)

### Community 11 - "Community 11"
Cohesion: 0.11
Nodes (17): 1. First login (once), 2. Add a kid, 3. Create a chore, 4. Assign a chore to one or more kids, 4. Settings — per-kid edit window, 5. Deactivate vs. delete a chore, 6. Watch the graphs, Access — every URL that hits this app (+9 more)

### Community 12 - "Community 12"
Cohesion: 0.05
Nodes (39): Backend K8s Deployment, Frontend K8s Deployment, Tailscale Ingress, Backend application.yml, Address (cluster-internal), Backend pod in `CrashLoopBackOff`, Backups, Browser shows "Sign in" but `/api/auth/login` returns 401 (+31 more)

### Community 13 - "Community 13"
Cohesion: 0.22
Nodes (8): Auth, Backend structure, Chores — Architecture, Data model, Deployment shape, Frontend structure, Why path-routed Ingress (not subdomain split), Why these choices

### Community 14 - "Community 14"
Cohesion: 0.20
Nodes (9): Access, Chores — kids' chore tracker, File reference, Initial credentials, LAN + localhost access (optional), See also, Stack & framework, Storage (+1 more)

### Community 15 - "Community 15"
Cohesion: 0.20
Nodes (12): AuthUser, GetMapping, List, LocalDate, Long, PostMapping, ResponseEntity, TaskService (+4 more)

### Community 16 - "Community 16"
Cohesion: 0.25
Nodes (7): args, command, cwd, env, type, mcpServers, code-review-graph

### Community 17 - "Community 17"
Cohesion: 0.25
Nodes (7): Access, chores, Depends on, Docs, Quick start, Tear down, What it does

### Community 18 - "Community 18"
Cohesion: 0.43
Nodes (5): Bean, HttpSecurity, JwtAuthFilter, SecurityConfig, SecurityFilterChain

### Community 19 - "Community 19"
Cohesion: 0.48
Nodes (6): ApplicationRunner, Bean, PasswordEncoder, String, UserRepository, AdminBootstrap

### Community 20 - "Community 20"
Cohesion: 0.29
Nodes (5): /Users/nila/.local/pipx/venvs/code-review-graph/bin/python, Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 22 - "Community 22"
Cohesion: 0.47
Nodes (4): Override, WebConfig, CorsRegistry, WebMvcConfigurer

### Community 24 - "Community 24"
Cohesion: 0.53
Nodes (3): List, Task, TaskRepository

### Community 25 - "Community 25"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 26 - "Community 26"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 27 - "Community 27"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 28 - "Community 28"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 29 - "Community 29"
Cohesion: 0.40
Nodes (4): Debug Issue, Steps, Tips, Token Efficiency Rules

### Community 30 - "Community 30"
Cohesion: 0.40
Nodes (4): Explore Codebase, Steps, Tips, Token Efficiency Rules

### Community 31 - "Community 31"
Cohesion: 0.40
Nodes (4): Refactor Safely, Safety Checks, Steps, Token Efficiency Rules

### Community 32 - "Community 32"
Cohesion: 0.40
Nodes (4): Output Format, Review Changes, Steps, Token Efficiency Rules

### Community 33 - "Community 33"
Cohesion: 0.40
Nodes (4): Debug Issue, Steps, Tips, Token Efficiency Rules

### Community 34 - "Community 34"
Cohesion: 0.40
Nodes (4): Explore Codebase, Steps, Tips, Token Efficiency Rules

### Community 35 - "Community 35"
Cohesion: 0.40
Nodes (4): Refactor Safely, Safety Checks, Steps, Token Efficiency Rules

### Community 36 - "Community 36"
Cohesion: 0.40
Nodes (4): Output Format, Review Changes, Steps, Token Efficiency Rules

### Community 37 - "Community 37"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 39 - "Community 39"
Cohesion: 0.50
Nodes (3): hooks, PostToolUse, SessionStart

### Community 40 - "Community 40"
Cohesion: 0.50
Nodes (3): hooks, AfterTool, SessionStart

### Community 42 - "Community 42"
Cohesion: 0.50
Nodes (3): hooks, PostToolUse, SessionStart

## Knowledge Gaps
- **223 isolated node(s):** `Why path-routed Ingress (not subdomain split)`, `Auth`, `Data model`, `Frontend structure`, `Backend structure` (+218 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **10 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `of()` connect `Community 8` to `Community 1`?**
  _High betweenness centrality (0.019) - this node is a cross-community bridge._
- **Why does `PreAuthorize` connect `Community 5` to `Community 8`, `Community 9`, `Community 15`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Why does `TaskService` connect `Community 3` to `Community 1`, `Community 10`?**
  _High betweenness centrality (0.015) - this node is a cross-community bridge._
- **What connects `Why path-routed Ingress (not subdomain split)`, `Auth`, `Data model` to the rest of the system?**
  _223 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05649717514124294 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.08143939393939394 - nodes in this community are weakly interconnected._
- **Should `Community 4` be split into smaller, more focused modules?**
  _Cohesion score 0.14333333333333334 - nodes in this community are weakly interconnected._