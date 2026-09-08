#!/usr/bin/env bash
# Preview the cron-utils-web static site over HTTP: serves
# cron-utils-web/target/pages (the TeaVM WASM-GC build output) with
# `python3 -m http.server`. Builds first with Maven when the pages directory
# is missing or `--build` is passed. Browsers refuse WASM loads over
# `file://`, so always preview over HTTP.
# Usage: bash scripts/preview-web.sh [--build] [port]  (default port 8080)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

BUILD=0
PORT=8080
for ARG in "$@"; do
    case "$ARG" in
        --build) BUILD=1 ;;
        -h|--help)
            echo "Usage: bash scripts/preview-web.sh [--build] [port]"
            exit 0
            ;;
        *)
            if [[ "$ARG" =~ ^[0-9]+$ ]]; then
                PORT="$ARG"
            else
                echo "ERROR: expected a port number or --build, got: '$ARG'" >&2
                exit 1
            fi
            ;;
    esac
done

PAGES="$REPO_ROOT/cron-utils-web/target/pages"
if [ "$BUILD" -eq 1 ] || [ ! -f "$PAGES/index.html" ]; then
    echo "Building cron-utils-web (mvn -pl cron-utils-web -am package)..."
    mvn -q -pl cron-utils-web -am package
fi

command -v python3 >/dev/null || { echo "ERROR: python3 is required to serve the preview." >&2; exit 1; }

echo "Serving $PAGES at http://localhost:$PORT (Ctrl+C to stop)."
python3 -m http.server "$PORT" --directory "$PAGES"
