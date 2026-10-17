# Manual · REST API

The API layer in one page: how a request travels through the backend, what
every list, error and identifier looks like, and how you add your own. The
endpoint table itself is in the
[specification](../specifications/ANIMALS.01-animal-register.md#api---session-3);
the living contract is **Swagger UI at <http://localhost:18440/swagger-ui.html>**
(OpenAPI JSON at `/v3/api-docs`).

## Where things are

Package `ee.taltech.ite4120.animals`, one directory per layer:

| Directory | Contains | Rule |
| --- | --- | --- |
| `api/` | `AnimalController`, `AnimalRequest` (request body + Bean Validation), `ApiErrorHandler`, `MockUmaController` | **HTTP only** — a business rule in a controller is a defect |
| `service/` | `AnimalService` | the business rules, one method per use case, `@Transactional` on writes |
| `repository/` | `AnimalRepository extends BaseRepository` | SQL, row mapping, pagination — nothing else |
| `model/` | `Animal`, `Species` | plain Java objects mapping 1:1 onto tables (including `sys*` fields, read-only) |
| `dto/` | `AnimalQueryParams extends QueryParams` | the list endpoint's filters |
| `ownerregistry/` | the registry adapter: port, HTTP and X-Road transports, wire records, imitated registry | external data goes through an interface |

A request runs controller → service → repository and back; exceptions thrown
anywhere are turned into a problem document by `ApiErrorHandler`.

## Required minimum

What the API of your component must have — the example does exactly this:

| Requirement | In the example |
| --- | --- |
| The layers above, in your own package `ee.taltech.ite4120.<yours>` | `animals/` |
| Create · read one · list · update · retire (soft delete) for the main entity | `POST` 201 + `Location` · `GET /{id}` · `GET` paged · `PUT` · `DELETE` 204 |
| The platform list contract: `limit`, `offset`, `sort`, and `{ data, meta }` in the answer | `AnimalQueryParams`, `QueryResult<Animal>` |
| **Every error is `application/problem+json`**; 4xx is the caller's fault, 5xx is ours | `ApiErrorHandler` |
| Validation in three places: request shape, business rules, database | `AnimalRequest` → `AnimalService.validate` → constraints in `02-animal.sql` |
| One external registry behind an adapter interface, with a **stated failure policy** (open or closed, and why) | `OwnerRegistryAdapter`, fail open on `/owner` only |
| Documented in Swagger with `@Operation` / `@ApiResponse` | `AnimalController` |
| The specification's business tests executable against the service | `AnimalBusinessRulesIT` |

## Calling it

Local mode uses mock authentication: the Bearer token **is** the username,
nothing is verified (`auth.mock.enabled=true` in the `local` profile —
`MockBearerAuthFilter`). Without a token every `/api/**` call answers 401.
The name in the token is also who the database records: `sys_created_by` and
`sys_modified_by` carry it (`JdbcConfig` hands it to `core.set_user()` on every
connection a request uses).

```bash
H='Authorization: Bearer superadmin'
curl -s -H "$H" 'http://localhost:18440/api/animals?limit=2&sort=-name'
curl -s -H "$H" http://localhost:18440/api/animals/1000/owner
curl -s -H "$H" -H 'Content-Type: application/json' -d '{"registryCode":"EE-2026-0042","name":"Pontu","speciesCode":"DOG"}' \
     -i http://localhost:18440/api/animals          # 201, Location: /api/animals/<id>
curl -s -H "$H" -X DELETE -i http://localhost:18440/api/animals/1006   # 204, soft delete
```

## The list contract

Every list endpoint takes the platform's `QueryParams` plus its own filters:

| Parameter | Meaning |
| --- | --- |
| `limit`, `offset` | page size and start — `?limit=20&offset=40` |
| `sort` | `sort=name` ascending, `sort=-name` descending; only keys the repository maps are allowed (`name`, `registryCode`, `birthDate` for animals) — anything else is a 400 |
| `textContains`, `speciesCode` | the endpoint's own filters, declared in `AnimalQueryParams` |

and answers a `QueryResult`:

```json
{
  "data": [ { "id": 1000, "registryCode": "EE-2026-0001", "name": "Muri", "speciesCode": "DOG", "birthDate": "2021-04-12", "ownerIsikukood": "38102130265", "sysStatus": "A", "sysVersion": 1, "…": "…" } ],
  "meta": { "total": 3, "offset": 0, "itemsPerPage": 20, "pages": 1 }
}
```

The repository implements it in one line — `query(params, this::count, this::list)`
— with a count query and a page query driven by the same `SqlBuilder` filter.

## Errors

One shape for everything, RFC 9457 `application/problem+json`:

```json
{ "status": 400, "title": "Invalid request", "detail": "birthDate must not be in the future", "instance": "/api/animals" }
```

| Status | Meaning | Throw |
| --- | --- | --- |
| 400 | the request is wrong in itself: shape (Bean Validation) or a rule about the message (unknown species, future birth date, unknown sort key) | `ApiClientException` |
| 404 | no active row with that id | `NotFoundException` |
| 409 | the current state forbids it (duplicate registry code among active animals) | `ConflictException` |
| 502 | an upstream registry could not answer | `OwnerRegistryException` (your own, mapped in the handler) |

`ApiErrorHandler` is a `@RestControllerAdvice`: one method per exception type,
each returning a `ProblemDetail`. Never a stack trace, never framework HTML.
The commons exceptions come from `org.helex.commons.exception`.

## Identity and lifecycle

- Ids are `Long`, minted by the database; the client never sends one on create.
- Business identifiers (`registryCode`, `ownerIsikukood`) are **strings** in
  the model, the request and the database, and the registry code is
  **immutable** after creation — `PUT` ignores it.
- Retire is a soft delete: `DELETE` answers 204 and sets `sys_status = 'C'`;
  every read filters on `'A'`, and the retired row's code becomes reusable.
- `sys*` fields (status, version, created at / by, modified at / by) appear in
  responses but are never accepted on input; the database trigger owns them.

## The registry adapter

`OwnerRegistryAdapter` is the port: one method, `lookup(isikukood)`. Which
transport answers is configuration, `animals.owner-registry.mode`:

| Mode | Class | When |
| --- | --- | --- |
| `http` (default) | `HttpOwnerRegistryAdapter` | plain HTTPS+JSON against `animals.owner-registry.url`; locally that is this application's own imitated registry, `GET /mock-registry/persons/{isikukood}` (`MockOwnerRegistryController`, local profile only) |
| `xroad` | `XRoadOwnerRegistryAdapter` | the forge-xroad transport against a security server — a compiling skeleton that fails fast and clearly without one |

Two-layer rule: the **wire records** (`RrPersonResponse`, `RrIsikResponse`)
keep the provider's Estonian field names verbatim so they stay diffable against
the provider's schema; the **mapper** — one line in the adapter — turns them
into our `OwnerInfo`. A person the registry does not know is a miss (`null`),
not a failure. Never store what the registry owns.

**Failure policy**, stated in the spec: fail open — a dead registry answers
502 on `/owner` only; the animal stays fully usable. Your component states its
own policy and its reason.

## Adding an endpoint, or a component

1. Model + query params in `model/`, `dto/`.
2. Repository: `extends BaseRepository`, `PgBeanProcessor` for mapping,
   `SqlBuilder` for filters, `query(...)` for lists, `sys_status = 'A'` in
   every read.
3. Service: rules from your specification's business tests, throwing the
   commons exceptions; `@Transactional` on writes.
4. Controller: `@RestController @RequestMapping("/api/<yours>")`, `@Valid`
   request bodies, `@Operation` per method; 201 + `Location` on create, 204 on
   retire.
5. Errors: reuse the commons exceptions; add a handler only for exceptions of
   your own (a registry failure).
6. Check it in Swagger, then with `curl`, then in the browser.

References: `AnimalController.java` · `AnimalService.java` ·
`AnimalRepository.java` · `ApiErrorHandler.java` ·
[the API section of the spec](../specifications/ANIMALS.01-animal-register.md#api---session-3)
· `AGENTS.md` § Architecture conventions.
