#!/usr/bin/env bash
# Regenerate the per-file patches from the working tree: diff .vanilla (the
# pristine split) against the four module src trees and write one patch per
# changed file. Files that no longer differ lose their patch; new and deleted
# files get a /dev/null patch.
#
# Requires .vanilla (created by sync-upstream.sh) and the marker its successful
# apply step leaves behind. The marker matters: running this on an unpatched
# tree would turn every pristine-vs-work diff into a "delete everything" patch.
# No arguments.
set -euo pipefail
export LC_ALL=C

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"
# shellcheck source=lib-split.sh
source "$REPO_ROOT/scripts/lib-split.sh"

if [ ! -f "$REPO_ROOT/.vanilla/.patches-applied" ]; then
    echo "ERROR: .vanilla/.patches-applied is missing; run bash scripts/sync-upstream.sh first." >&2
    exit 1
fi

regen_patches "$REPO_ROOT/.vanilla"
echo "Regenerated $(patch_count) per-file patch(es) under patches/."
echo "Verify from scratch: bash scripts/sync-upstream.sh && mvn clean package"
