#!/usr/bin/env bash
# Shared split logic plus quilt-engine helpers. Sourced, never executed.
# Expects REPO_ROOT to be set by the caller.
#
# Patch mechanics (apply/order/state/regen) are quilt's: patches/ holds the
# -p1 files, patches/series sets apply order, .pc/ holds quilt state
# (gitignored). This library only knows how to BUILD the module trees.

UPSTREAM_URL="https://github.com/jmrozanec/cron-utils.git"
MODULES="cron-utils-core cron-utils-descriptor cron-utils-converter cron-utils-validator"

# A test file belongs to the descriptor module when it touches quartz, spring
# or the descriptor API (import or fully-qualified use). This one rule covers
# both the 10 root-level Issue*Test files and the packaged quartz/spring
# integration tests (parser/*Quartz*, parser/*Spring*, model/time/*Quartz*).
DESCRIPTOR_BOUND_RE='org\.quartz|springframework|com\.cronutils\.descriptor|com\.cronutils\.utils\.descriptor'

cpdir() {
    # cpdir <srcdir> <destdir>: copy directory contents, skip silently when absent.
    if [ -d "$1" ]; then
        mkdir -p "$2"
        cp -r "$1/." "$2/"
    fi
}

split_upstream() {
    local up="${1:-$REPO_ROOT/.upstream}"
    local um="$up/src/main/java/com/cronutils"
    local ut="$up/src/test/java/com/cronutils"
    local ute="$up/src/test/resources"

    for m in $MODULES; do
        rm -rf "$REPO_ROOT/$m/src"
    done

    local core_j="$REPO_ROOT/cron-utils-core/src/main/java/com/cronutils"
    local desc_j="$REPO_ROOT/cron-utils-descriptor/src/main/java/com/cronutils"
    local conv_j="$REPO_ROOT/cron-utils-converter/src/main/java/com/cronutils"
    local val_j="$REPO_ROOT/cron-utils-validator/src/main/java/com/cronutils"
    mkdir -p "$core_j" "$desc_j" "$conv_j" "$val_j"

    # --- main sources: package -> module ---
    cp "$um/"*.java "$core_j/" 2>/dev/null || true
    for p in builder mapper model parser utils; do
        cpdir "$um/$p" "$core_j/$p"
    done
    rm -rf "$core_j/utils/descriptor"
    cpdir "$um/descriptor" "$desc_j/descriptor"
    cpdir "$um/utils/descriptor" "$desc_j/utils/descriptor"
    cpdir "$um/converter" "$conv_j/converter"
    cpdir "$um/validation" "$val_j/validation"
    for d in "$um/"*/; do
        case "$(basename "$d")" in
            builder|mapper|model|parser|utils|descriptor|converter|validation) ;;
            *) echo "WARNING: unmapped upstream package '$(basename "$d")' skipped by split" >&2 ;;
        esac
    done

    # --- main resources: I18N bundles live with the descriptor module ---
    if [ -d "$up/src/main/resources" ]; then
        ( cd "$up/src/main/resources" && find . -type f ) | while IFS= read -r f; do
            f="${f#./}"
            case "$f" in
                com/cronutils/descriptor/*|com/cronutils/CronUtilsI18N*)
                    mkdir -p "$REPO_ROOT/cron-utils-descriptor/src/main/resources/$(dirname "$f")"
                    cp "$up/src/main/resources/$f" "$REPO_ROOT/cron-utils-descriptor/src/main/resources/$f"
                    ;;
                *)
                    mkdir -p "$REPO_ROOT/cron-utils-core/src/main/resources/$(dirname "$f")"
                    cp "$up/src/main/resources/$f" "$REPO_ROOT/cron-utils-core/src/main/resources/$f"
                    ;;
            esac
        done
    fi

    # --- test sources: same package rule, plus the descriptor-bound content rule ---
    local core_t="$REPO_ROOT/cron-utils-core/src/test/java/com/cronutils"
    local desc_t="$REPO_ROOT/cron-utils-descriptor/src/test/java/com/cronutils"
    local conv_t="$REPO_ROOT/cron-utils-converter/src/test/java/com/cronutils"
    local val_t="$REPO_ROOT/cron-utils-validator/src/test/java/com/cronutils"
    mkdir -p "$core_t" "$desc_t" "$conv_t" "$val_t"
    cp "$ut/"*.java "$core_t/" 2>/dev/null || true
    for p in builder mapper model parser utils validator; do
        cpdir "$ut/$p" "$core_t/$p"
    done
    cpdir "$ut/descriptor" "$desc_t/descriptor"
    cpdir "$ut/utils/descriptor" "$desc_t/utils/descriptor"
    cpdir "$ut/converter" "$conv_t/converter"
    cpdir "$ut/validation" "$val_t/validation"
    # Move descriptor-bound files (root Issue tests + quartz/spring integration
    # tests) from the core tree to the descriptor tree, preserving relpath.
    ( cd "$core_t" && grep -rlE "$DESCRIPTOR_BOUND_RE" . 2>/dev/null ) | while IFS= read -r f; do
        f="${f#./}"
        mkdir -p "$desc_t/$(dirname "$f")"
        mv "$core_t/$f" "$desc_t/$f"
    done

    # --- test resources: mirror the main-resource rule ---
    if [ -d "$ute" ]; then
        ( cd "$ute" && find . -type f ) | while IFS= read -r f; do
            f="${f#./}"
            case "$f" in
                com/cronutils/descriptor/*|com/cronutils/CronUtilsI18N*)
                    mkdir -p "$REPO_ROOT/cron-utils-descriptor/src/test/resources/$(dirname "$f")"
                    cp "$ute/$f" "$REPO_ROOT/cron-utils-descriptor/src/test/resources/$f"
                    ;;
                com/cronutils/converter/*)
                    mkdir -p "$REPO_ROOT/cron-utils-converter/src/test/resources/$(dirname "$f")"
                    cp "$ute/$f" "$REPO_ROOT/cron-utils-converter/src/test/resources/$f"
                    ;;
                com/cronutils/validation/*)
                    mkdir -p "$REPO_ROOT/cron-utils-validator/src/test/resources/$(dirname "$f")"
                    cp "$ute/$f" "$REPO_ROOT/cron-utils-validator/src/test/resources/$f"
                    ;;
                *)
                    mkdir -p "$REPO_ROOT/cron-utils-core/src/test/resources/$(dirname "$f")"
                    cp "$ute/$f" "$REPO_ROOT/cron-utils-core/src/test/resources/$f"
                    ;;
            esac
        done
    fi

    # --- overlays: whole fixed files that cannot be expressed as a patch ---
    if [ -d "$REPO_ROOT/overlays" ]; then
        cp -a "$REPO_ROOT/overlays/." "$REPO_ROOT/"
    fi

    # Fail fast if a non-descriptor main tree references the descriptor API:
    # that would not compile with the module split.
    if grep -rqE "$DESCRIPTOR_BOUND_RE" "$REPO_ROOT/cron-utils-core/src/main" \
        "$REPO_ROOT/cron-utils-converter/src/main" "$REPO_ROOT/cron-utils-validator/src/main" 2>/dev/null; then
        echo "ERROR: non-descriptor main sources reference quartz/spring/descriptor API:" >&2
        grep -rlE "$DESCRIPTOR_BOUND_RE" "$REPO_ROOT/cron-utils-core/src/main" \
            "$REPO_ROOT/cron-utils-converter/src/main" "$REPO_ROOT/cron-utils-validator/src/main" 2>/dev/null >&2
        return 1
    fi
}

# --- quilt engine helpers (deterministic flags; --quiltrc - ignores user config) ---

# quilt_run <args...>: invoke quilt from the repo root.
quilt_run() {
    quilt --quiltrc - "$@" 2>&1
}

# stack_clean: true when no patch is applied (no session in progress).
stack_clean() {
    [ -z "$(quilt --quiltrc - applied 2>/dev/null || true)" ]
}

# stack_full: true when every series patch is applied (set comparison:
# `quilt unapplied` prints a status message on full stacks, so its output
# cannot be tested for emptiness).
stack_full() {
    [ "$({ quilt --quiltrc - applied 2>/dev/null || true; } | sed 's|^patches/||' | LC_ALL=C sort | sha256sum)" == "$(grep -vE '^\s*(#|$)' "$REPO_ROOT/patches/series" | LC_ALL=C sort | sha256sum)" ]
}

# quilt_top: basename of the topmost applied patch, empty when none.
quilt_top() {
    { quilt --quiltrc - top 2>/dev/null || true; } | sed 's|^patches/||'
}

# in_series <basename>: true when listed in patches/series.
in_series() {
    [ -f "$REPO_ROOT/patches/series" ] && grep -qxF "$1" "$REPO_ROOT/patches/series"
}

# series_max_prefix: highest two-digit prefix in patches/series (empty if none).
series_max_prefix() {
    [ -f "$REPO_ROOT/patches/series" ] || return 0
    cut -c1-2 "$REPO_ROOT/patches/series" | LC_ALL=C sort -u | tail -n 1
}

# norm_patch_arg <arg>: accept `02-foo.patch` or `patches/02-foo.patch`.
norm_patch_arg() {
    local a="$1"
    a="${a#patches/}"
    printf '%s' "$a"
}

# upstream_pinned_ok: .upstream exists at the SHA in upstream.ref.
upstream_pinned_ok() {
    [ -d "$REPO_ROOT/.upstream" ] || return 1
    local want have
    want="$(tr -d '[:space:]' < "$REPO_ROOT/upstream.ref")"
    have="$(git -C "$REPO_ROOT/.upstream" rev-parse HEAD 2>/dev/null)" || return 1
    [ "$want" == "$have" ]
}
