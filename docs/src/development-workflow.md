# Development workflow

The course's rules, in the order they bite. These mirror the production Helex
process (BABOK-lite), reduced to what a three-person course project needs.

## 1. Spec first

No implementation before the specification is confirmed by the whole group.
An AI agent given a vague prompt produces confident, wrong code — and you cannot
tell it is wrong until it fails. The specification is where precision happens.

## 2. Tests second

The last section of every specification is **business tests**, in plain language:
given / when / then. They become executable tests **before** the implementation.
When an agent writes the implementation, the failing test is your only independent
evidence that it solved *your* problem. See
`backend/src/test/java/.../AnimalBusinessRulesIT.java` — every test there quotes
the specification.

## 3. Recreatable

The standard for a finished specification: delete the implementation, hand the
spec to someone (or some agent) who was never in the room, and they rebuild the
same thing. If tacit knowledge is needed, the spec is not done.

## 4. The spec stays alive

When the implementation settles differently than specified, **update the
specification in the same pull request**. A spec that no longer matches the code
is worse than no spec — it is confident misinformation.

## 5. Work through pull requests

Branch, commit, open a PR, have a group-mate review, merge. Your individual
contribution is evidenced by history under your own name — that is gate B of the
assessment. Every PR also runs the **Verify** checks (`.github/workflows/verify.yml`):
`./gradlew test`, `npx tsc -b` + `vite build`, and `scripts/check-changesets` —
the same commands `AGENTS.md` asks you to run before opening it. Red is not
mergeable; the reviewer reviews, the machine verifies.

## 6. Keep the work diary

A Markdown file per student: hours, what you asked the agent, what it got wrong,
how you corrected it. The corrections column is the interesting one — it is what
the AI-workflow criterion grades.
