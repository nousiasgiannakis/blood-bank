# BLOOD-US-001 · Find a donation centre and check readiness before going

> **BLOOD-US-001** · state **Proposed** · traces to [BLOOD.01](../specifications/BLOOD.01-donation-readiness.md)

**As** a person thinking about giving blood
**I want** to find a donation centre and check, before I leave home, whether I
am prepared and likely to be accepted
**so that** I do not make an unnecessary trip and be turned away for something
I could have known in advance.

## Context

People are sent home from donation centres for avoidable reasons: they have
not eaten, have slept too little, recently had a fever or a new piercing. The
information exists, but scattered. This component puts two things in one
place: a **directory of donation centres** (address, location, opening hours)
and a **readiness questionnaire** that turns the basic preparation and
eligibility guidance into a short yes/no checklist with advice.

Blood service staff maintain the directory and the questions. The centre's
operating organisation is **not** stored as free text: the directory keeps
only its business registry code and asks the business register — over X-Road
in production, over the imitated registry in this course — for the current
legal name and address. One source of truth per fact.

**Privacy by design.** The questionnaire is guidance, not a medical decision
and not a record. Answers are evaluated in the person's own browser and are
never sent to the server, stored or logged. The component holds no personal
data about donors at all. Collect nothing that is not necessary.

Out of scope: donor accounts and donation history, appointment booking, blood
stock, the centre's own medical screening (staff at the centre always make the
final decision). The questions and rules in this draft are placeholders until
the group's research into the official eligibility guidance, the applicable
jurisdiction and the legal requirements is done.

## Acceptance criteria

- The directory lists donation centres with name, region, address, location
  and opening hours; it is searchable by name or address and filterable by
  region.
- Staff can add, edit and retire a centre; a centre code identifies exactly
  one **active** centre, and codes of retired centres may be reused.
- Obvious nonsense is refused with a clear message: unknown region, a location
  that is not a real coordinate, a malformed business registry code.
- For a centre with an operator code, the page shows the operator's current
  legal name and address, fetched live from the business register — never
  stored locally.
- The questionnaire shows the active questions in order, each answered yes or
  no, and ends with one of three outcomes — ready, prepare first, not today —
  plus the advice for every answer that raised a concern.
- Every question that can raise a concern carries advice telling the person
  what to do about it.
- Answering the questionnaire sends nothing to the server and leaves nothing
  behind: no answers in the database, in requests or in browser storage.
