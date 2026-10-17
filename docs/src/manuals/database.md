# Manual · Database

The data layer in one page: what is in the database, where it comes from, and
how you change it. The long form, changeset by changeset, is the
[Liquibase guide](../liquibase-guide.md); the data model itself is in the
[specification](../specifications/ANIMALS.01-animal-register.md#data-model---session-2-changesets-0104).

## What runs where

| Thing | Where |
| --- | --- |
| PostgreSQL 18 | Docker, `localhost:18520`, database and user `animals` — `docker/compose.yaml` |
| The schema | created by **Liquibase on backend start** — nothing is created by hand or by init scripts |
| Master changelog | `backend/src/main/resources/animals/db/changelog/changelog.xml` |
| Component changelog | `…/animals/db/changelog/animals/` — one `.sql` file per table, in numbered order |
| Liquibase's own bookkeeping | schema `public`: `databasechangelog`, `databasechangeloglock` |

The master changelog includes two things, in this order: the platform's `core`
schema, which ships **inside the published `commons-db-core` jar**, and the
component's changelog. Your component becomes a third include.

## Three schemas

- **`core`** — the platform's, not yours. It provides `core.seq_id` (one id
  sequence for the whole database), the `core.sys_columns()` trigger that fills
  the `sys_*` columns, and `core.create_table_metadata()` that installs it.
- **`animals`** — the worked example: `species` (reference list), `animal`
  (the business table), `vaccination` (child of animal). Three tables, two
  foreign keys — the assessment's data-layer minimum.
- **`<yours>`** — one schema per component; boundaries in the database mirror
  boundaries in the code.

## Required minimum

What the assessment's data-layer gate (gate A) checks, and where the example
meets it:

| Requirement | In the example |
| --- | --- |
| **At least three tables, at least one foreign key**, in a schema of your own | `species`, `animal`, `vaccination` — two FKs (`animal → species`, `vaccination → animal`) |
| Every business table: `id` from `core.seq_id`, the `sys_*` columns, `core.create_table_metadata()` called | `02-animal.sql`, `03-vaccination.sql` |
| Soft delete via `sys_status`; uniqueness enforced among active rows only | `animal_registry_code_ukey … where (sys_status = 'A')` |
| Identifiers stored as `text`, never as numbers | `registry_code`, `owner_isikukood` |
| Only Liquibase changesets create or change anything; released ones are never edited | `changelog.xml` + numbered files; `04-…` appends |
| Demo rows under `context:demo`, so the app is usable out of the box | `90-demo-data.sql` |
| `scripts/check-changesets` clean, `./gradlew test` green from an empty database | — |

A reference table (like `species`) counts as one of the three; data owned by an
external registry (an owner's name and address) is looked up, never stored —
see the [REST API manual](rest-api.md#the-registry-adapter).

## Anatomy of a business table

Read `02-animal.sql` with this list beside it:

| Line | Why |
| --- | --- |
| `id bigint default nextval('core.seq_id') not null` | ids come from the shared sequence, never `serial` |
| business columns | identifiers (`registry_code`, `owner_isikukood`) are `text` — **strings, never numbers** |
| `sys_status, sys_version, sys_created_at, sys_created_by, sys_modified_at, sys_modified_by` | declared here, **filled by the trigger** — application code never writes them |
| `select core.create_table_metadata('animals.animal');` | installs that trigger; one call per business table |
| `create unique index … where (sys_status = 'A')` | uniqueness among **active** rows only: a retired row's code may be reused |
| `check (owner_isikukood ~ '^[0-9]{11}$')` | the database is the last line of validation |

**Soft delete**: rows are never deleted; retiring sets `sys_status` from `A`
to `C`. Every query in the repository filters `sys_status = 'A'`.

**Reference tables** (`01-species.sql`) are different on purpose: natural key,
no `sys_*` columns, and a separate seed changeset with `on conflict do nothing`
so it converges on any database.

**Who am I?** The trigger stamps `sys_created_by` / `sys_modified_by` from
`core.session_user()`, which reads the session setting `core.client_identifier`.
Two things set it: the pool, once per connection, to the application's own name
(`connection-init-sql` in `application.yml` — what Liquibase and demo data are
attributed to), and `JdbcConfig`, on every connection a request borrows, to the
signed-in user via `core.set_user(?)` — the platform's `JdbcFactory` pattern.
So a row records who created it and who last changed it, and an update that
changes nothing leaves the row untouched (the trigger compares `new = old`).

## Changesets: the two rules that bite

1. **A released changeset is immutable.** Liquibase stores a checksum of every
   changeset it ran and refuses to start on an edited one. To change a table
   you already merged, append a new file — `04-animal-add-chip-number.sql` is
   the example.
2. **Formatted-SQL grammar.** In `--liquibase formatted sql` files a `--` line
   is a Liquibase directive, not a comment: `--changeset`, `--comment`, and
   the bare `--` that terminates every changeset. Prose goes in `/* */`
   blocks. Datatypes align at column 25. The gate is
   `scripts/check-changesets` — run it before every PR that touches a
   changelog; it fails closed.

Demo rows live in `90-demo-data.sql` under `context:demo` and are applied
only because `application.yml` sets `spring.liquibase.contexts: demo`.

## Working with the database

```bash
./scripts/run-backend.sh     # starts PostgreSQL and applies the changelog
./scripts/psql.sh            # psql inside the container
./scripts/reset-db.sh        # destroy and recreate — the database is disposable
```

Useful in `psql`:

```sql
\dt animals.*                                            -- the component's tables
\d animals.animal                                        -- columns, constraints, indexes, trigger
select id, filename, orderexecuted from public.databasechangelog order by orderexecuted;
select registry_code, name, sys_status, sys_created_by from animals.animal;
```

When the backend refuses to start after a library bump complaining about
checksums of `core-db` changesets, that is rule 1 doing its job: reset the
database, it replays clean.

## Tests

`./gradlew test` starts a throwaway PostgreSQL via Testcontainers and applies
the **whole** changelog from empty before the business tests run — so every
test run also proves the migrations. `application-test.yml` only redirects the
core changelog's grant parameters at the container's user; your component
needs nothing there.

## Adding your component

1. `backend/src/main/resources/<yours>/db/changelog/<yours>/` with
   `<yours>-schema.sql`, then `01-…sql`, `02-…sql` per table, then optionally
   `90-demo-data.sql` with `context:demo`.
2. A `changelog.xml` beside them listing the files in order, and **one**
   `<include>` for it in the master changelog.
3. `scripts/check-changesets`, then `./gradlew test` — green means the
   migrations run from empty.

References: [Liquibase guide](../liquibase-guide.md) · the
[data model](../specifications/ANIMALS.01-animal-register.md#data-model---session-2-changesets-0104) ·
`AGENTS.md` rules 5–6 · the files under
`backend/src/main/resources/animals/db/changelog/animals/`.
