# ANIMALS.01 · Animal register

> **ANIMALS.01** · state **Implemented** · traces from [ANIMALS-US-001](../user-stories/ANIMALS-US-001.md)
> · source `backend/src/main/java/ee/taltech/ite4120/animals/`, `backend/src/main/resources/animals/db/changelog/`, `frontend/src/pages/`

The worked example specification. Your component's spec follows this shape — every
section here maps onto one course session and one layer of the implementation.

## Description

A register of animals: create, search, view, update, retire (soft delete). Owner
identity is held as a personal code only; name and address are resolved live from
the population registry through the owner-registry adapter (mock in this course,
X-Road-shaped in production). Excluded: authentication management (mock auth
only), multi-tenancy, audit beyond the platform sys columns.

## Data model  *(→ session 2, changesets 01–04)*

| Table | Field | Type | Rules |
| --- | --- | --- | --- |
| `animals.species` | `code` PK | text | reference list: DOG, CAT, HORSE, PARROT |
| | `name` | text | display name |
| `animals.animal` | `id` PK | bigint | from `core.seq_id` |
| | `registry_code` | text | required; **unique among active rows** |
| | `name` | text | required, ≤255 |
| | `species_code` | text | required; FK → species |
| | `birth_date` | date | optional; **not in the future** |
| | `owner_isikukood` | text | optional; exactly 11 digits — a string, never a number |
| | `chip_number` | text | optional, ≤30 — added by changeset 04 |
| | `sys_*` | — | platform columns, trigger-managed |
| `animals.vaccination` | `id` PK | bigint | from `core.seq_id` |
| | `animal_id` | bigint | required; FK → animal |
| | `vaccine`, `vaccinated_on`, `valid_until` | text/date | vaccination event |

## API  *(→ session 3)*

| Method + path | Does | Failure answers |
| --- | --- | --- |
| `GET /api/animals` | paged search: `textContains`, `speciesCode`, `limit`, `offset`, `sort` | — |
| `GET /api/animals/{id}` | one active animal | 404 |
| `POST /api/animals` | register; answers **201 + Location** | 400 validation · 409 duplicate code |
| `PUT /api/animals/{id}` | update; registry code immutable | 400 · 404 |
| `DELETE /api/animals/{id}` | retire (soft) — **204** | 404 |
| `GET /api/animals/{id}/owner` | owner via the registry adapter | 404 · 502 registry down |
| `GET /api/animals/species` | the reference list | — |

Every error is `application/problem+json`. All endpoints require
`Authorization: Bearer <user>` (mock auth); the API contract is live at
`/swagger-ui.html`.

**Failure policy** for the owner lookup: **fail open** — an unreachable registry
answers 502 on the owner endpoint only; the animal itself stays fully usable.
Rationale: the owner's address is auxiliary here, never a precondition.

## UI  *(→ session 4)*

| Screen | Element | Component | Rules |
| --- | --- | --- | --- |
| Animal list | table | `ResourceList` | columns: registry code (locked, **a link to the animal's page**), name, species tag, born, chip (hidden by default); search field; row detail panel |
| | register button | `AppButtonPrimary` | navigates to the form |
| | detail panel | `ResourceList.detailView` | shows owner name/address fetched via `/owner`; silently omits it when the registry is down |
| Animal page `/animals/{id}` | view / edit | `ResourceForm` + `useDataController` | sections Identity (registry code read-only, name, species) and Details (birth date, owner code, chip); Edit → Save is `PUT`, Cancel restores; Retire is the soft `DELETE` with confirmation; Back to the list |
| | owner | `ResourceForm` field, view mode | the personal code plus name and address via `/owner`; omitted when the registry fails — the page never depends on it |
| | metadata | `ResourceForm.sidebar` — `AppCard` of `FieldItem`s | created at / by, modified at / by, version — the platform `sys*` columns, read-only; "by" is the signed-in user |
| Register form | registry code | `Input` | required, ≤50 |
| | name | `Input` | required, ≤255 |
| | species | `Select` | required, options from `/species` |
| | birth date | `DatePicker` | optional |
| | owner code | `Input` | pattern `\d{11}` |
| | chip number | `Input` | ≤30 |
| | submit | `AppButtonPrimary` | 400/409 problem text shown verbatim in a notification |

## Business tests  *(→ executable in `AnimalBusinessRulesIT`)*

1. **An animal cannot be born in the future.**
   Given a request for an animal born tomorrow, when it is registered, then the
   registration is refused with "birthDate must not be in the future".
2. **A registry code is unique among active animals.**
   Given an active animal with code X, when a second animal claims X, then the
   registration conflicts (409). And when the first is retired, code X may be
   used again — retirement is a soft delete.
