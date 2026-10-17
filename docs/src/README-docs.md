# Documentation

Everything about this system that is not code lives here, in Markdown, in the same
repository as the code — diffable, reviewable in the same pull request, versioned with
the release it describes, and readable by your AI agent.

| Folder | What goes there |
| --- | --- |
| `src/user-stories/` | Who needs what, and why — in the stakeholder's language |
| `src/specifications/` | What the solution does — precise enough to build and test from |
| `src/manuals/` | Three one-page manuals — database, REST API, frontend — each with the required minimum and where the example meets it |
| `_templates/` | Copy these to start your own story and specification |

## Where everything runs

With `scripts/run-backend` and `scripts/run-frontend` up:

| Service | URL |
| --- | --- |
| The application | <http://localhost:18640> |
| **Swagger UI — the live API contract** | <http://localhost:18440/swagger-ui.html> |
| OpenAPI JSON (what agents and generators read) | <http://localhost:18440/v3/api-docs> |
| The imitated population registry | <http://localhost:18440/mock-registry/persons/38102130265> |
| PostgreSQL | `localhost:18520` — `scripts/psql` for a shell |
| This book, with live reload | <http://localhost:18740> — `scripts/run-docs` |

Swagger UI is generated from the running code by springdoc, so it is always
current — the same generated-never-drawn principle as the ER diagram. Endpoints
answer only with `Authorization: Bearer <user>`; the *Authorize* button in
Swagger UI takes any username in local mode.

The worked example — the **animals register** — has one story and one specification.
Read them before writing your own: your component follows the same shape, in a
different domain.
