#!/usr/bin/env bash
# mirror-packages.sh — LECTURER-ONLY maintenance script (bash, deliberately no
# .ps1 twin: students never run this).
#
# Copies the Helex Maven artifacts this template depends on — INCLUDING their
# org.helex.* transitive dependencies, walked through the Gradle module
# metadata — from the private helex-solutions registries into THIS
# repository's own Maven registry
# (maven.pkg.github.com/igorboss/ite4120). Why: GitHub's Maven registry has no
# per-package access — reading a package requires read access to its source
# REPOSITORY. Mirroring into the course repo means the one grant students
# already have (access to this repo, for cloning) also serves the packages,
# with no helex-solutions seats or source exposure. The npm side needs no
# mirror: the four @helex-solutions npm packages are public per-package.
#
# Usage:
#   scripts/mirror-packages.sh                 # mirror the LATEST versions
#   scripts/mirror-packages.sh 0.1.39 0.25.0   # mirror exact versions
#
# Then bump backend/gradle.properties to the mirrored versions.
#
# Auth: GITHUB_GOD_TOKEN — a token that can read helex-solutions packages and
# write packages on igorboss/ite4120 (scopes: read:packages, write:packages,
# repo). Falls back to GITHUB_TOKEN. Republishing an existing version is
# detected and skipped, never overwritten.
set -euo pipefail

[ -n "${GITHUB_GOD_TOKEN:-}" ] || [ ! -f ~/.zprofile ] || source ~/.zprofile
TOKEN="${GITHUB_GOD_TOKEN:-${GITHUB_TOKEN:-}}"
[ -n "$TOKEN" ] || { echo "set GITHUB_GOD_TOKEN (read helex packages + write:packages on the course repo)" >&2; exit 1; }
ACTOR="${GITHUB_ACTOR:-igorboss}"

SRC_ORG=helex-solutions
DST_REPO=igorboss/ite4120

COMMONS_VERSION="${1:-$(gh api "/orgs/$SRC_ORG/packages/maven/org.helex.emr.commons-db/versions" --jq '.[0].name')}"
FORGE_VERSION="${2:-$(gh api "/orgs/$SRC_ORG/packages/maven/org.helex.forge.forge-xroad/versions" --jq '.[0].name')}"
echo "mirroring org.helex.emr:* $COMMONS_VERSION and org.helex.forge:forge-xroad $FORGE_VERSION -> $DST_REPO"

mirror() { # source-repo group artifact version
  local repo=$1 group=$2 artifact=$3 version=$4
  local gpath="${group//.//}"
  local src="https://maven.pkg.github.com/$SRC_ORG/$repo/$gpath/$artifact/$version"
  local dst="https://maven.pkg.github.com/$DST_REPO/$gpath/$artifact/$version"

  echo "$group:$artifact:$version"
  if curl -sfL -u "$ACTOR:$TOKEN" -o /dev/null "$dst/$artifact-$version.pom"; then
    echo "  already mirrored — skipped (existing versions are never overwritten)"
    return
  fi

  local copied=0 tmp code
  for base in "$artifact-$version.pom" "$artifact-$version.jar" \
              "$artifact-$version.module" "$artifact-$version-sources.jar"; do
    for f in "$base" "$base.sha1" "$base.md5" "$base.sha256" "$base.sha512"; do
      tmp=$(mktemp)
      # -L matters: the registry answers with a 302 to a signed download URL;
      # without following it you mirror the redirect page, not the artifact.
      if curl -sfL -u "$ACTOR:$TOKEN" -o "$tmp" "$src/$f"; then
        code=$(curl -s -u "$ACTOR:$TOKEN" -X PUT --data-binary @"$tmp" -o /dev/null -w '%{http_code}' "$dst/$f")
        case "$code" in
          2*) echo "  $f"; copied=$((copied+1)) ;;
          *)  echo "  $f — upload FAILED (HTTP $code)" >&2; rm -f "$tmp"; exit 1 ;;
        esac
      fi
      rm -f "$tmp"
    done
  done
  [ "$copied" -gt 0 ] || { echo "  found nothing at the source — wrong version?" >&2; exit 1; }
  # Round-trip check: the pom read back from the mirror must be byte-identical
  # to the source. This catches exactly the bug class that shipped once — a
  # redirect page mirrored instead of the artifact.
  local sum_src sum_dst
  sum_src=$(curl -sfL -u "$ACTOR:$TOKEN" "$src/$artifact-$version.pom" | shasum -a 256 | cut -d" " -f1)
  sum_dst=$(curl -sfL -u "$ACTOR:$TOKEN" "$dst/$artifact-$version.pom" | shasum -a 256 | cut -d" " -f1)
  [ "$sum_src" = "$sum_dst" ] || { echo "  MIRROR CORRUPT: pom read back differs from source" >&2; exit 1; }
  echo "  -> $copied files, round-trip verified"
}

repo_for_group() { case "$1" in org.helex.forge*) echo forge ;; *) echo emr-repo ;; esac; }

# The org.helex.* dependencies an artifact declares, read from its Gradle
# module metadata at the source (the POM understates them — Gradle consumers
# resolve via the .module file, which is where commons-model's forge-core
# dependency lives). Prints "group artifact version" lines; empty when the
# artifact publishes no module file or has no helex dependencies.
helex_deps() { # source-repo group artifact version
  local repo=$1 group=$2 artifact=$3 version=$4
  local gpath="${group//.//}"
  curl -sfL -u "$ACTOR:$TOKEN" \
    "https://maven.pkg.github.com/$SRC_ORG/$repo/$gpath/$artifact/$version/$artifact-$version.module" \
    2>/dev/null | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit(0)
seen = set()
for v in d.get("variants", []):
    for x in v.get("dependencies", []):
        g = x.get("group", "")
        if g.startswith("org.helex"):
            seen.add((g, x["module"], x["version"]["requires"]))
for g, m, ver in sorted(seen):
    print(g, m, ver)
' || true
}

# Breadth-first over the declared roots plus every org.helex.* transitive.
# The queue is a space-separated list of repo|group|artifact|version items
# (coordinates never contain spaces); VISITED de-duplicates.
QUEUE=""
for a in commons-db commons-db-core commons-model commons-util; do
  QUEUE="$QUEUE emr-repo|org.helex.emr|$a|$COMMONS_VERSION"
done
QUEUE="$QUEUE forge|org.helex.forge|forge-xroad|$FORGE_VERSION"
VISITED=""

while [ -n "${QUEUE# }" ]; do
  QUEUE="${QUEUE# }"
  item="${QUEUE%% *}"
  [ "$item" = "$QUEUE" ] && QUEUE="" || QUEUE="${QUEUE#* }"
  IFS='|' read -r repo group artifact version <<<"$item"
  key="$group:$artifact:$version"
  case " $VISITED " in *" $key "*) continue ;; esac
  VISITED="$VISITED $key"

  mirror "$repo" "$group" "$artifact" "$version"
  while read -r dg dm dv; do
    [ -n "$dg" ] || continue
    echo "  transitive: $dg:$dm:$dv"
    QUEUE="$QUEUE $(repo_for_group "$dg")|$dg|$dm|$dv"
  done < <(helex_deps "$repo" "$group" "$artifact" "$version")
done

echo "done. Mirrored:$VISITED"
echo "Now set helexCommonsVersion=$COMMONS_VERSION and forgeVersion=$FORGE_VERSION in backend/gradle.properties and run the backend tests."
