#!/usr/bin/env python3
"""Formatted-SQL changeset grammar and layout check.

Vendored from the Helex platform (helex-tx scripts/check-liquibase-changesets.sh,
whose rules come from emr/docs/architecture/data/02-migration.md). The checking
logic is carried over verbatim; only file discovery differs — this repo is small
enough to scan whole, and there is no baseline: every changelog complies.

Why this exists: in formatted SQL `--` is not a comment marker but Liquibase's
escape character. The errors this catches — a missing terminator, prose parsed
as a directive, a body comment riding the wrong checksum — are invisible until
a baffling runtime failure. Run it before every PR that touches db/changelog.

Usage: python3 scripts/check_changesets.py  (from the repository root)
"""
import os, re, sys

NAME_WIDTH = 20          # 4-space indent + 20 = datatype at column 25 (see header)
COLUMN = re.compile(r'^ {4}([a-z_][a-z0-9_]*) +(?=\S)')

DIRECTIVE = re.compile(
    r'^--(changeset|rollback|preconditions?|precondition-sql-check|validCheckSum|comment|'
    r'ignoreLines|ignore|include|includeAll|property|labels?|liquibase|endDelimiter|runWith|dbms)\b',
    re.I)

# A PROSE line Liquibase will read as a DIRECTIVE.
#
# Liquibase matches a directive after `--` plus optional SPACES, but the pattern above requires the
# keyword immediately after `--`. So a wrapped prose line whose continuation happens to begin with
# `property`, `changeset`, `comment`, `include`, `rollback` … is a directive to Liquibase and prose to
# this gate — which is exactly the shape that got through it. A real file failed the whole application
# context with "Unexpected formatting ... at line 5" because a sentence wrapped onto
# `-- property the stories ask for ...`.
#
# docs/architecture/03-database-conventions.md has warned about this in prose since before the gate existed. This
# is the gate catching up: the hazard is invisible on inspection, the error names a line number and not
# the reason, and the fix — reflow the sentence — is unguessable from the message.
LOOSE_DIRECTIVE = re.compile(
    r'^--[ \t]+(changeset|rollback|preconditions?|precondition-sql-check|validCheckSum|comment|'
    r'ignoreLines|ignore|include|includeAll|property|labels?|liquibase|endDelimiter|runWith|dbms)\b',
    re.I)

DOLLAR = re.compile(r'\$([A-Za-z_][A-Za-z0-9_]*)?\$')

def baseline():
    """path -> set of rules to skip, or None meaning skip the file entirely.

    Three line forms, and the second exists because of a real incident: a file listed WHOLE is
    unreviewed, so a batch of `--` lines beginning with the word validCheckSum went in unnoticed
    and Liquibase parsed each as a directive. Deferring one COSMETIC rule should not cost the
    dangerous ones.

        path/to/file.sql                 skip every rule (legacy; prefer the next form)
        path/to/file.sql:rule,rule       skip only those rules
        path/to/tree/:rule,rule          skip those rules for everything under the prefix
    """
    return {}
    with fh:
        for line in fh:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            path, _, rules = line.partition(':')
            out[path] = {r.strip() for r in rules.split(',') if r.strip()} if rules else None
    return out

def skipped(rules_for, path, rule):
    """Is `rule` deferred for `path`? Exact match first, then any prefix entry."""
    for key, rules in rules_for.items():
        hit = (key == path) if not key.endswith('/') else path.startswith(key)
        if hit and (rules is None or rule in rules):
            return True
    return False

def check(path):
    out = []
    lines = open(path, encoding='utf-8').read().split('\n')
    heads = [i for i, l in enumerate(lines) if l.startswith('--changeset')]
    if not heads:
        return out

    # A /* */ block that closes MID-LINE hands the rest of that line to the
    # database as SQL. Liquibase does not nest block comments, so the first `*/`
    # ends the block whatever the author meant. This is not hypothetical: a
    # comment in emr's core-db 11-acl.sql explained the block-comment rule by
    # quoting the delimiters, shipped as commons-db-core 0.1.12, and every
    # consuming service failed to start with `syntax error at or near "blocks"`
    # because core.acl was never created. `git diff -w` shows it as innocuous —
    # the bug IS comment syntax — so only applying the changelog catches it.
    inblk = False
    opened_on = -1
    for i, l in enumerate(lines):
        if not inblk:
            o = l.find('/*')
            if o >= 0 and '*/' not in l[o + 2:]:
                inblk, opened_on = True, i
            continue
        c = l.find('*/')
        if c < 0:
            continue
        if l[c + 2:].strip():
            out.append((i + 1, 'changeset-comment-selfclose',
                        f"`*/` mid-line inside a /* */ block opened on line {opened_on + 1} —"
                        " the comment ends here and the rest of the line is handed to the"
                        " database as SQL. Write the delimiters in words instead."))
        inblk = False

    # Where a leading `--` is NOT Liquibase's escape character: inside a /* */
    # block (a dashed banner rule starts with `--` but is already commented) and
    # inside a dollar-quoted body, which is PL/pgSQL source — rewriting its
    # comments would change pg_proc.prosrc, a schema change rather than
    # formatting, so the grammar stops at the function boundary. emr has 46 such
    # lines; this tree has none today, and the mask is what stops that becoming a
    # wave of false positives the first time someone adds a trigger.
    masked = [False] * len(lines)
    dollar = None
    inblk = False
    for i, l in enumerate(lines):
        masked[i] = (dollar is not None) or inblk
        if dollar is not None:
            if dollar in l:
                dollar = None
            continue
        code = re.sub(r"'[^']*'", '', l)
        if inblk:
            if '*/' in code:
                inblk = False
            continue
        m = DOLLAR.search(code)
        if m and code.count(m.group(0)) % 2 == 1:
            dollar = m.group(0)
            continue
        if code.count('/*') > code.count('*/'):
            inblk = True

    # Whole-file scan, preamble included. The per-changeset loop below starts at the first
    # `--changeset`, so anything above it was never examined — and the preamble is exactly where a
    # file's explanatory prose lives, which is where the wrapped-keyword hazard bites.
    for i, l in enumerate(lines):
        if masked[i]:
            continue
        if LOOSE_DIRECTIVE.match(l):
            keyword = LOOSE_DIRECTIVE.match(l).group(1)
            out.append((i + 1, 'prose-reads-as-directive',
                        "prose line begins with `%s` after `-- ` — Liquibase matches a directive"
                        " after `--` plus SPACES, so it parses this as --%s and refuses the whole"
                        " changelog. Reflow the sentence so the line starts with another word."
                        % (keyword, keyword)))

    bounds = heads + [len(lines)]
    for h, start in enumerate(heads):
        end = bounds[h + 1]
        term = None
        rollbacks = []
        for i in range(start + 1, end):
            l = lines[i]
            if masked[i]:
                continue
            s = l.rstrip()
            if s == '--':
                term = i
                continue
            if l.startswith('--rollback'):
                rollbacks.append(i + 1)
            ls = l.lstrip()
            if ls.startswith('--') and not DIRECTIVE.match(ls):
                out.append((i + 1, 'changeset-body-comment',
                            "`--` prose inside a changeset body — use a /* */ block"
                            " (only directives and the bare `--` terminator may start with --)"))
            if term is not None and s.strip():
                out.append((i + 1, 'changeset-orphan-content',
                            "content after the changeset's `--` terminator — it silently belongs"
                            " to the PRECEDING changeset's checksum; move it below the next"
                            " --changeset header"))
                term = None  # report once per stretch, keep scanning
        # Column layout: datatype starts at column 25 — four-space indent, name padded to
        # twenty. Scanning one column shows a wrong type or a missing `not null` at a glance.
        # `constraint` lines are not column definitions and keep their single space.
        in_table = False
        for i in range(start + 1, end):
            low = lines[i].strip().lower()
            if low.startswith('create table'):
                in_table = True
                continue
            if in_table and low.startswith(');'):
                in_table = False
                continue
            if not in_table or masked[i]:
                continue
            m = COLUMN.match(lines[i])
            if m and m.group(1) != 'constraint' and len(m.group(1)) < NAME_WIDTH:
                if len(m.group(0)) != 4 + NAME_WIDTH:
                    out.append((i + 1, 'column-alignment',
                                f"datatype starts at column {len(m.group(0)) + 1}, not"
                                f" {5 + NAME_WIDTH} — indent four spaces and pad the name to"
                                f" {NAME_WIDTH} characters"))
        if len(rollbacks) > 1:
            out.append((rollbacks[1], 'changeset-rollback-one-line',
                        "more than one --rollback line in a changeset — put every rollback"
                        " statement on ONE --rollback line, `;`-separated"))
        # last non-blank line of the region must be the terminator
        j = end - 1
        while j > start and not lines[j].strip():
            j -= 1
        if lines[j].rstrip() != '--':
            out.append((start + 1, 'changeset-terminator',
                        "changeset does not close with a bare `--` line — add the terminator"
                        " after its last statement so the body ends where it says it does"))
    return out

def discover(root='backend/src/main/resources'):
    """Every formatted-SQL changelog under the backend resources tree."""
    hits = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in ('build', 'node_modules')]
        for f in sorted(filenames):
            if not f.endswith('.sql'):
                continue
            p = os.path.join(dirpath, f)
            with open(p, encoding='utf-8') as fh:
                if 'liquibase formatted sql' in fh.read(200):
                    hits.append(p)
    return hits

files = discover()
if not files:
    print('check_changesets: no formatted changelogs found — run from the repository root', file=sys.stderr)
    sys.exit(2)
rules_for = baseline()
violations = 0
for path in files:
    for line, rule, msg in check(path):
        if skipped(rules_for, path, rule):
            continue
        print(f"{path}:{line}: [{rule}] {msg}", file=sys.stderr)
        violations += 1
if violations:
    print(f"liquibase changesets: {violations} violation(s)", file=sys.stderr)
    sys.exit(1)
print(f"liquibase changesets: clean ({len(files)} changelogs scanned)")