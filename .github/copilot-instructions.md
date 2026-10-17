# Copilot instructions — ITE4120 template

Read [`AGENTS.md`](../AGENTS.md) at the repository root before doing anything
else. It is the complete contract for every AI coding agent working here — the
same rules for Copilot, Claude, Cursor and Gemini — and it wins on any conflict
with this file. This file only points there. (The Copilot coding agent also
picks up `AGENTS.md` directly.)

Headlines only — the wording in `AGENTS.md` is the rule:

- Spec first: no implementation before a confirmed spec in `docs/src/specifications/`.
- Tests second: business tests from the spec become executable tests before the code.
- Never commit to `main`; never edit a released Liquibase changeset — append one.
- Controller → service → repository; errors as `application/problem+json`; identifiers are strings, never numbers.
- UI inputs are existing `@helex/ui` or antd components.
- Before a PR: `./gradlew test`, `npx tsc -b`, `scripts/check-changesets`, and the touched flow in the browser.
