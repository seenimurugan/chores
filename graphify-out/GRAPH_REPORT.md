# Graph Report - chores  (2026-05-30)

## Corpus Check
- 72 files · ~17,905 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 567 nodes · 778 edges · 58 communities (42 shown, 16 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 65 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `4045543a`
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
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
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
- [[_COMMUNITY_Community 41|Community 41]]
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
- [[_COMMUNITY_Community 52|Community 52]]
- [[_COMMUNITY_Community 53|Community 53]]
- [[_COMMUNITY_Community 54|Community 54]]
- [[_COMMUNITY_Community 55|Community 55]]

## God Nodes (most connected - your core abstractions)
1. `compilerOptions` - 17 edges
2. `Chores — maintenance` - 14 edges
3. `TaskService` - 12 edges
4. `AdminTaskController` - 9 edges
5. `of()` - 9 edges
6. `PreAuthorize` - 9 edges
7. `Troubleshooting` - 9 edges
8. `api` - 8 edges
9. `LocalDate` - 8 edges
10. `Long` - 8 edges

## Surprising Connections (you probably didn't know these)
- `of()` --references--> `TaskDto`  [EXTRACTED]
  backend/src/main/java/com/nila/chores/task/AdminTaskController.java → backend/src/main/java/com/nila/chores/task/AdminTaskController.java  _Bridges community 7 → community 1_

## Communities (58 total, 16 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (28): AdminChoresMatrix(), DateCell(), DAY_MONTH, isToday(), KidChoresMatrix(), parseLocal(), WEEKDAY, ComparisonLines() (+20 more)

### Community 1 - "Community 1"
Cohesion: 0.10
Nodes (23): AssignedTaskRow, AdminMatrix, KidMatrix, KidStats, List, LocalDate, Long, TaskAssignmentRepository (+15 more)

### Community 2 - "Community 2"
Cohesion: 0.06
Nodes (33): Address (cluster-internal), Backend pod in `CrashLoopBackOff`, Backups, Browser shows "Sign in" but `/api/auth/login` returns 401, Chores — maintenance, Cluster topology, Connect from a GUI (TablePlus / DBeaver / pgAdmin), "Conversion of type 'number' to type 'string'" at build time (+25 more)

### Community 3 - "Community 3"
Cohesion: 0.14
Nodes (16): Integer, List, LocalDate, Long, String, Task, TaskAssignment, TaskAssignmentRepository (+8 more)

### Community 4 - "Community 4"
Cohesion: 0.11
Nodes (17): PostMapping, AuthUser, User, JwtService, Override, String, User, Claims (+9 more)

### Community 5 - "Community 5"
Cohesion: 0.13
Nodes (14): List, Optional, String, User, List, Long, PasswordEncoder, String (+6 more)

### Community 6 - "Community 6"
Cohesion: 0.09
Nodes (22): dependencies, clsx, next, react, react-dom, recharts, devDependencies, autoprefixer (+14 more)

### Community 7 - "Community 7"
Cohesion: 0.16
Nodes (13): DeleteMapping, GetMapping, List, Long, PostMapping, ResponseEntity, TaskService, Void (+5 more)

### Community 8 - "Community 8"
Cohesion: 0.19
Nodes (11): List, LocalDate, Long, Optional, Query, TaskCompletion, CompletionRow, DailyCount (+3 more)

### Community 9 - "Community 9"
Cohesion: 0.10
Nodes (20): compilerOptions, allowJs, baseUrl, esModuleInterop, incremental, isolatedModules, jsx, lib (+12 more)

### Community 10 - "Community 10"
Cohesion: 0.15
Nodes (14): DeleteMapping, GetMapping, List, Long, PostMapping, ResponseEntity, User, Void (+6 more)

### Community 11 - "Community 11"
Cohesion: 0.21
Nodes (11): AdminMatrix, AuthUser, GetMapping, KidMatrix, KidStats, List, LocalDate, Long (+3 more)

### Community 12 - "Community 12"
Cohesion: 0.12
Nodes (16): 1. First login (once), 2. Add a kid, 3. Create a chore, 4. Assign a chore to one or more kids, 5. Deactivate vs. delete a chore, 6. Watch the graphs, Access — every URL that hits this app, Admin guide (+8 more)

### Community 13 - "Community 13"
Cohesion: 0.18
Nodes (10): AuthUser, List, LocalDate, Long, PostMapping, ResponseEntity, TaskService, Void (+2 more)

### Community 14 - "Community 14"
Cohesion: 0.24
Nodes (7): AuthUser, GetMapping, JwtService, PasswordEncoder, UserRepository, MeDto, AuthController

### Community 15 - "Community 15"
Cohesion: 0.31
Nodes (6): Bean, PasswordEncoder, HttpSecurity, JwtAuthFilter, SecurityConfig, SecurityFilterChain

### Community 16 - "Community 16"
Cohesion: 0.20
Nodes (9): Access, Chores — kids' chore tracker, File reference, Initial credentials, LAN + localhost access (optional), See also, Stack & framework, Storage (+1 more)

### Community 17 - "Community 17"
Cohesion: 0.22
Nodes (8): Auth, Backend structure, Chores — Architecture, Data model, Deployment shape, Frontend structure, Why path-routed Ingress (not subdomain split), Why these choices

### Community 18 - "Community 18"
Cohesion: 0.39
Nodes (6): ApplicationRunner, Bean, PasswordEncoder, String, UserRepository, AdminBootstrap

### Community 19 - "Community 19"
Cohesion: 0.25
Nodes (7): Access, chores, Depends on, Docs, Quick start, Tear down, What it does

### Community 20 - "Community 20"
Cohesion: 0.25
Nodes (7): args, command, cwd, env, type, mcpServers, code-review-graph

### Community 21 - "Community 21"
Cohesion: 0.47
Nodes (4): Override, WebConfig, CorsRegistry, WebMvcConfigurer

### Community 22 - "Community 22"
Cohesion: 0.53
Nodes (3): List, Task, TaskRepository

### Community 23 - "Community 23"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 24 - "Community 24"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 25 - "Community 25"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 26 - "Community 26"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 27 - "Community 27"
Cohesion: 0.40
Nodes (4): Debug Issue, Steps, Tips, Token Efficiency Rules

### Community 28 - "Community 28"
Cohesion: 0.40
Nodes (4): Explore Codebase, Steps, Tips, Token Efficiency Rules

### Community 29 - "Community 29"
Cohesion: 0.40
Nodes (4): Refactor Safely, Safety Checks, Steps, Token Efficiency Rules

### Community 30 - "Community 30"
Cohesion: 0.40
Nodes (4): Output Format, Review Changes, Steps, Token Efficiency Rules

### Community 31 - "Community 31"
Cohesion: 0.40
Nodes (4): Debug Issue, Steps, Tips, Token Efficiency Rules

### Community 32 - "Community 32"
Cohesion: 0.40
Nodes (4): Explore Codebase, Steps, Tips, Token Efficiency Rules

### Community 33 - "Community 33"
Cohesion: 0.40
Nodes (4): Refactor Safely, Safety Checks, Steps, Token Efficiency Rules

### Community 34 - "Community 34"
Cohesion: 0.40
Nodes (4): Output Format, Review Changes, Steps, Token Efficiency Rules

### Community 35 - "Community 35"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 36 - "Community 36"
Cohesion: 0.40
Nodes (4): Key Tools, MCP Tools: code-review-graph, When to use graph tools FIRST, Workflow

### Community 39 - "Community 39"
Cohesion: 0.50
Nodes (3): hooks, PostToolUse, SessionStart

### Community 40 - "Community 40"
Cohesion: 0.50
Nodes (3): hooks, AfterTool, SessionStart

### Community 41 - "Community 41"
Cohesion: 0.50
Nodes (3): hooks, PostToolUse, SessionStart

## Knowledge Gaps
- **224 isolated node(s):** `setup-graph.sh script`, `command`, `args`, `cwd`, `type` (+219 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **16 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `of()` connect `Community 10` to `Community 1`?**
  _High betweenness centrality (0.019) - this node is a cross-community bridge._
- **Why does `PreAuthorize` connect `Community 11` to `Community 10`, `Community 13`, `Community 7`?**
  _High betweenness centrality (0.014) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `of()` (e.g. with `.getIcon()` and `.getId()`) actually correct?**
  _`of()` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `setup-graph.sh script`, `command`, `args` to the rest of the system?**
  _224 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05889724310776942 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.09872241579558652 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.058823529411764705 - nodes in this community are weakly interconnected._