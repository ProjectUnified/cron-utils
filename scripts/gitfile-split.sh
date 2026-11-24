#!/usr/bin/env bash
# Materialize hook for gitfile: called once for the pristine baseline and once
# for the working tree, with GITFILE_UPSTREAM / GITFILE_DEST set.
set -euo pipefail

REPO_ROOT="${GITFILE_ROOT:?run through gitfile}"
# shellcheck source=lib-split.sh
source "$REPO_ROOT/scripts/lib-split.sh"

split_upstream "$GITFILE_UPSTREAM" "$GITFILE_DEST"
