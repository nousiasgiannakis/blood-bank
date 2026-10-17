# AGENTS.md — ITE4120 animals-register template

Canonical instructions for AI coding agents working in this repository — the
single source of truth, the same rules for every tool. Read on startup by
Claude Code (via `CLAUDE.md`), Cursor (via `.cursor/rules/`), GitHub Copilot
(via `.github/copilot-instructions.md`), Gemini Code Assist (via `GEMINI.md`),
OpenAI Codex, Aider, Continue and any other agent that follows the
[agents.md convention](https://agents.md). The tool-specific files are pointers
only: **no rule lives anywhere but here, and this file wins on any conflict.**
Humans: this is also your onboarding — the rules are identical.

| Tool | Reads | Which is |
| --- | --- | --- |
| Claude Code | `CLAUDE.md` | a pointer here |
| Cursor | `.cursor/rules/*.mdc` | pointers here, scoped by file type |
| GitHub Copilot (chat + coding agent) | `.github/copilot-instructions.md` | a pointer here; the coding agent also reads this file directly |
| Gemini Code Assist | `GEMINI.md` | a pointer here |
| Codex, Aider, Continue, Junie, … | `AGENTS.md` | this file, natively |

Adding a rule: write it **here**, in the section it belongs to. Never in a
tool file — a rule only Claude can see is a rule the group-mate on Cursor will
break.

## What this is

A course template: a working Spring Boot + React application consuming the Helex
platform as **published libraries** (`org.helex.emr:*` from GitHub Packages,
`@helex/ui` and friends from GitHub npm). The worked example component is the
**animals register** (`ee.taltech.ite4120.animals`); each student group adds its
own component in a different domain, following the same shape.

## Process rules (non-negotiable)

1. **Spec first.** No implementation before a confirmed specification in
   `docs/src/specifications/`. When asked to build something unspecified, ask for the
   spec — or draft one and stop for confirmation.
2. **Tests second.** Business tests from the spec become executable tests before
   the implementation. Never write a test to match code you just wrote.
3. **Update the spec in the same PR** when the implementation settles differently.
4. **Never commit to main.** Branch, PR, review.
5. **Never edit a released Liquibase changeset.** Append a new one — see
   `docs/src/liquibase-guide.md`.
6. **Formatted-SQL grammar.** `--` lines belong to Liquibase (directives and the
   bare `--` terminator closing every changeset); prose is a single-line
   `--comment` plus a `/* */` block; body comments that start a line are
   `/* */` blocks; datatypes align at column 25; business-table indexes carry
   `where (sys_status = 'A')`. Every file under `animals/db/changelog/` is the
   example.

## Architecture conventions

- One component = one Java package under `ee.taltech.ite4120.<component>` + one
  schema + one changelog folder + pages under `frontend/src/pages/`.
- Layers: controller (HTTP only) → service (business rules) → repository
  (extends `BaseRepository`, uses `SqlBuilder`/`PgBeanProcessor`). Business
  logic in a controller is a defect.
- Errors: `application/problem+json` everywhere, via `ApiErrorHandler`. 4xx =
  caller's fault, 5xx = ours. Throw the commons exceptions
  (`NotFoundException`, `ConflictException`, `ApiClientException`).
- External registries go behind an adapter interface with schema-faithful wire
  records (Estonian field names stay Estonian) and a single mapper — see
  `ownerregistry/`. Never store data a registry owns.
- IDs from `core.seq_id`; `sys_*` columns on business tables, never written by
  application code — the trigger fills them, and `config/JdbcConfig` tells it
  who the signed-in user is; soft delete via `sys_status`; identifiers
  (isikukood, codes) are **strings, never numbers**.
- Cross-cutting configuration (security, JDBC) lives in
  `ee.taltech.ite4120.config`, not inside a component.
- UI: every input is an existing `@helex/ui` or antd component. Lists on
  `ResourceList`, record pages on `ResourceForm` + `useDataController`
  (`AnimalDetail.tsx` is the example).

## Known pitfalls (earned the hard way)

- Missing `GITHUB_TOKEN`/`gpr.key` → Gradle "cannot resolve org.helex.emr" and
  npm 401. Fix credentials first; do not vendor jars.
- Spring dispatches 404s to `/error`; the security chain must permit the ERROR
  dispatcher or every 404 becomes a 401 (already configured — do not remove).
- The frontend MockAuthProvider probes `/api/uma/auth/mock-users` and
  `/api/uma/userinfo` pre-auth; `MockUmaController` answers them. Removing it
  brings back error toasts on every page load.
- Testcontainers runs as DB user `test` — `application-test.yml` overrides the
  Liquibase grant parameters. Your component needs no change there.
- Testcontainers finds Docker Desktop on its own; any other engine needs to be
  pointed at. Colima: `export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
  TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` before
  `./gradlew test`, or every IT dies with "Could not find a valid Docker
  environment" while `docker ps` works fine.
- Two Reacts = "Invalid hook call": keep the `overrides` in
  `frontend/package.json` and the `dedupe` list in `vite.config.ts`.
- Bumping `helexCommonsVersion` can change the checksums of the platform's own
  `core-db` changesets, and the backend then refuses to start against an old
  local database. That is Liquibase working as designed — run `scripts/reset-db`
  (the database is disposable) and it replays clean. Testcontainers tests are
  unaffected: they always start from empty.
- Testcontainers 2.x moved `PostgreSQLContainer` to
  `org.testcontainers.postgresql` (non-generic); the old
  `org.testcontainers.containers` import is a deprecated shim.
- `@helex/ui`'s stylesheet (`dist/index.css`) is not listed in the package's
  `exports`, so `import '@helex/ui/dist/index.css'` is refused; `main.tsx`
  imports it **by file path** instead. Keep that line — without it
  `ResourceForm` loses its layout (sidebar, resize handle) and the calendar
  family renders unstyled.
- `ResourceForm`'s built-in `date` field crashes the picker (`… isValid is not a
  function`): antd's `Form.Item` injects the form-store value over the dayjs the
  field prepares, and the store holds the API's `YYYY-MM-DD` string. Use a
  `custom` field whose input normalises string ↔ dayjs itself — `DateField` in
  `AnimalDetail.tsx`.

## Verification before any PR

```
backend:  ./gradlew test          # includes migrations-from-empty via Testcontainers
frontend: npx tsc -b              # typecheck
db:       scripts/check-changesets  # formatted-SQL grammar + layout (rule 6), fails closed
manual:   scripts/run-backend + run-frontend → the flow you touched, in the browser
```

CI runs the first three on every pull request (`.github/workflows/verify.yml`,
the "Verify" checks) — a red check is not mergeable. The fourth is yours: no
machine clicks through the flow you changed.
