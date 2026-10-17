# ANIMALS-US-001 · Register and look up animals

> **ANIMALS-US-001** · state **Implemented** · traces to [ANIMALS.01](../specifications/ANIMALS.01-animal-register.md)

**As** a municipal veterinary officer
**I want** a register of animals with their owners and vaccinations
**so that** I can answer, for any animal, who owns it and whether its
vaccinations are current — without phoning three offices.

## Context

Estonia keeps registers for people, vehicles and buildings; animals follow the
same pattern. The officer registers an animal once, with a unique registry code.
The owner is **not** stored as free text: the register keeps only the personal
code (isikukood) and asks the population registry — over X-Road in production,
over the imitated registry in this course — whenever a name and address is
needed. One source of truth per fact.

## Acceptance criteria

- An animal can be registered with a registry code, name, species and optional
  birth date, owner and chip number.
- A registry code identifies exactly one **active** animal; codes of retired
  animals may be reused.
- Obvious nonsense is refused with a clear message: unknown species, birth date
  in the future, malformed personal code.
- The list is searchable by name or registry code and filterable by species.
- For an animal with an owner, the officer sees the owner's current name and
  address, fetched live from the registry — never stored locally.
