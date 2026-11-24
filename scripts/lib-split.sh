#!/usr/bin/env bash
# Upstream source split for cron-utils. Sourced by scripts/gitfile-split.sh,
# never executed directly.
#
# Upstream is a single Maven module; this splits its packages across the four
# fork modules. The split is deterministic, so gitfile can materialize it twice
# (pristine baseline and working tree) and diff one against the other.

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

# split_upstream <upstream-dir> <dest-root>
# Rebuilds <dest-root>/<module>/src from an upstream checkout. Deterministic:
# the same upstream tree always yields the same split, which is what makes
# .vanilla a valid baseline for regeneration.
split_upstream() {
    local up="$1"
    local dest="$2"
    local um="$up/src/main/java/com/cronutils"
    local ut="$up/src/test/java/com/cronutils"
    local ute="$up/src/test/resources"

    for m in $MODULES; do
        rm -rf "$dest/$m/src"
    done

    local core_j="$dest/cron-utils-core/src/main/java/com/cronutils"
    local desc_j="$dest/cron-utils-descriptor/src/main/java/com/cronutils"
    local conv_j="$dest/cron-utils-converter/src/main/java/com/cronutils"
    local val_j="$dest/cron-utils-validator/src/main/java/com/cronutils"
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
                    mkdir -p "$dest/cron-utils-descriptor/src/main/resources/$(dirname "$f")"
                    cp "$up/src/main/resources/$f" "$dest/cron-utils-descriptor/src/main/resources/$f"
                    ;;
                *)
                    mkdir -p "$dest/cron-utils-core/src/main/resources/$(dirname "$f")"
                    cp "$up/src/main/resources/$f" "$dest/cron-utils-core/src/main/resources/$f"
                    ;;
            esac
        done
    fi

    # --- test sources: same package rule, plus the descriptor-bound content rule ---
    local core_t="$dest/cron-utils-core/src/test/java/com/cronutils"
    local desc_t="$dest/cron-utils-descriptor/src/test/java/com/cronutils"
    local conv_t="$dest/cron-utils-converter/src/test/java/com/cronutils"
    local val_t="$dest/cron-utils-validator/src/test/java/com/cronutils"
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
                    mkdir -p "$dest/cron-utils-descriptor/src/test/resources/$(dirname "$f")"
                    cp "$ute/$f" "$dest/cron-utils-descriptor/src/test/resources/$f"
                    ;;
                com/cronutils/converter/*)
                    mkdir -p "$dest/cron-utils-converter/src/test/resources/$(dirname "$f")"
                    cp "$ute/$f" "$dest/cron-utils-converter/src/test/resources/$f"
                    ;;
                com/cronutils/validation/*)
                    mkdir -p "$dest/cron-utils-validator/src/test/resources/$(dirname "$f")"
                    cp "$ute/$f" "$dest/cron-utils-validator/src/test/resources/$f"
                    ;;
                *)
                    mkdir -p "$dest/cron-utils-core/src/test/resources/$(dirname "$f")"
                    cp "$ute/$f" "$dest/cron-utils-core/src/test/resources/$f"
                    ;;
            esac
        done
    fi

    # Fail fast if a non-descriptor main tree references the descriptor API:
    # that would not compile with the module split.
    if grep -rqE "$DESCRIPTOR_BOUND_RE" "$dest/cron-utils-core/src/main" \
        "$dest/cron-utils-converter/src/main" "$dest/cron-utils-validator/src/main" 2>/dev/null; then
        echo "ERROR: non-descriptor main sources reference quartz/spring/descriptor API:" >&2
        grep -rlE "$DESCRIPTOR_BOUND_RE" "$dest/cron-utils-core/src/main" \
            "$dest/cron-utils-converter/src/main" "$dest/cron-utils-validator/src/main" 2>/dev/null >&2
        return 1
    fi
}

