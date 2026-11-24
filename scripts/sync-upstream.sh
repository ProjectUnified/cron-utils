#!/usr/bin/env bash
# Rebuild everything from the pinned upstream commit: fresh-clone .upstream,
# split its single-module sources into the 4 module trees twice — once into
# .vanilla (the pristine baseline used for regeneration) and once into the
# working tree — then apply every per-file patch with `git apply -p1`.
# No arguments. This is also the reset hatch: it wipes .vanilla and any
# unpatched working-tree edits, then rebuilds deterministically.
set -euo pipefail
export LC_ALL=C

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"
# shellcheck source=lib-split.sh
source "$REPO_ROOT/scripts/lib-split.sh"

SHA="$(tr -d '[:space:]' < upstream.ref)"
if ! [[ "$SHA" =~ ^[0-9a-f]{40}$ ]]; then
    echo "ERROR: upstream.ref must contain a single 40-char commit SHA, got: '$SHA'" >&2
    exit 1
fi

rm -rf .upstream .vanilla
git clone --no-checkout "$UPSTREAM_URL" .upstream
if ! git -C .upstream checkout "$SHA"; then
    echo "ERROR: SHA $SHA unreachable; re-resolve HEAD and overwrite upstream.ref:" >&2
    echo "  git ls-remote $UPSTREAM_URL HEAD" >&2
    exit 1
fi

split_upstream "$REPO_ROOT/.upstream" "$REPO_ROOT/.vanilla"
split_upstream "$REPO_ROOT/.upstream" "$REPO_ROOT"
apply_patches "$REPO_ROOT"
touch .vanilla/.patches-applied

echo "Synced to $SHA; $(patch_count) per-file patch(es) applied."
echo "Next step: mvn clean package"
echo "Edit sources, then run: bash scripts/update-patches.sh"
