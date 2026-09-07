# cron-utils-web

Static cron explainer and generator. All cron logic (parse, validate, describe,
next runs, generate, cross-type mapping) runs client-side: Java compiled with
TeaVM to WASM-GC, no backend. The only handwritten JS is the WASM-GC loader
snippet in `src/main/webapp/index.html`.

## Layout

- `src/main/java/com/cronutils/web/CronWebApp.java` — TeaVM entry point
  (`mainClass`); builds every DOM node via TeaVM JSO APIs.
- `src/main/java/com/cronutils/web/Explainer.java` — pure parse → validate →
  describe → next-5-runs logic (no DOM).
- `src/main/java/com/cronutils/web/Generator.java` — pure per-field builder,
  nickname presets, and cross-type equivalents (no DOM).
- `src/main/webapp/index.html` — static shell (loader + stylesheet link).
- `src/main/webapp/style.css` — single stylesheet, zero framework.

Depends on `cron-utils-core` and `cron-utils-descriptor` only. Converter and
validator are out of v1 (keeps `Calendar`/`jakarta.validation` risk out of
the image).

- Spike A (ResourceBundle) — RED at runtime, fixed by vendoring: lookups
  compile but TeaVM WASM-GC reports "Bundle not found" at runtime (its
  `bundleProviders` map is never populated for the `.properties` files).
  Per the plan's fallback, `scripts/gen-web-bundles.py` vendors all 18
  `CronUtilsI18N*.properties` verbatim into `EmbeddedBundleData.java`
  (ASCII with `\u` escapes, significant trailing spaces preserved), served
  through an in-memory `ResourceBundle` with per-key English fallback
  mirroring the JVM parent chain. `EmbeddedBundlesTest` proves every shipped
  key and every-locale `describe()` output identical to the JVM describer.
  Browser proof: German locale renders `um 12:00` for `0 0 12 * * ?`.
- Spike B (java.time) — GREEN at runtime: TeaVM compiles the reached JDK
  `java.time` bytecode directly (no classlib emulation needed in the reached
  paths). Named zones resolve with correct DST (reference
  `2026-03-07T12:00` in `America/New_York` yields `-04:00` runs after the
  March 8 changeover). The timezone dropdown stays a fixed allowlist
  (`UTC`, `UTC±HH:MM` offsets, plus `America/New_York`, `Europe/London`,
  `Asia/Tokyo`); unknown zones fall back to `UTC`.
- Spike C (CronMapper) — GREEN at build and on JVM: QUARTZ→UNIX of the weekly
  preset matches the JVM `CronMapper` result. Targets without a direct static
  pair render `n/a for <TYPE>` instead of failing the page.

Build-time gap found and fixed: TeaVM 0.15.0 does not emulate
`List.parallelStream()` or `Pattern.splitAsStream()`, both called from core
sources reachable from any parse. Fixed by `patches/07-teavm-stream-compat`
(sequential `stream()` / `Arrays.stream(split.split(...))`); semantics
unchanged — `collect(toList())` on an ordered stream preserves encounter
order exactly as the ordered parallel collection did. Core + descriptor suites
stay green.

## Build and preview

- `mvn -pl cron-utils-web -am clean package`
- `python3 -m http.server 8080 --directory cron-utils-web/target/pages`
  (`file://` WASM loads are refused by browsers; always preview over HTTP.
  `target/pages` is the exploded webapp `web-deploy.yml` uploads to Pages.)
- Open `http://localhost:8080` in a WASM-GC-capable Chromium:
  QUARTZ + `0 0 12 * * ?` shows the English description and next runs from
  `2026-01-01T00:00Z[UTC]`; `*/bad` shows the parser message in place.

`mvn clean package` at the root still builds all five modules. The web module
never publishes: root `pom.xml` routes `central-publishing-maven-plugin`
behind `${maven.deploy.skip}` (default `false`), the web pom forces it `true`
alongside `gpg.skip`/`source.skip`/`javadoc.skip`, and `maven-release.yml`
deploys and releases only the four library modules.
