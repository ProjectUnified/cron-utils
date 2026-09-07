# cron-utils
> [!NOTE]
> This is a fork of [https://github.com/jmrozanec/cron-utils](https://github.com/jmrozanec/cron-utils).

`cron-utils` is a Java library to define, parse, validate, and migrate crons. The project follows the [Semantic Versioning Convention](https://semver.org/) and uses the Apache 2.0 license.

## Modules

The project is structured into several modules to separate core logic from niche features:

- **[cron-utils-core](./cron-utils-core)**: Core model, parser, builder, mapping, and execution time calculation.
- **[cron-utils-descriptor](./cron-utils-descriptor)**: Human-readable cron descriptions and internationalization.
- **[cron-utils-converter](./cron-utils-converter)**: Utilities for converting between different cron-to-calendar formats.
- **[cron-utils-validator](./cron-utils-validator)**: Jakarta Bean Validation (`@Cron`) support.

## Download

Available on Maven Central:

```xml
<dependency>
    <groupId>io.github.projectunified</groupId>
    <artifactId>cron-utils-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Patch-based build

No `src/` tree is checked in: module sources are generated from the pinned
upstream commit recorded in `upstream.ref`, split into the four modules, and
patched with [Quilt](https://savannah.nongnu.org/projects/quilt) (`patches/`
+ `patches/series`; quilt state in `.pc/` is gitignored). Version mapping:
`fork 2.x == upstream <SHA in upstream.ref> + patches/`.

Prereqs: `git`, `quilt` 0.69+, JDK 21, Maven.

```sh
bash scripts/sync-upstream.sh && mvn clean package
```

| Task | Command |
|---|---|
| Edit a patch | `bash scripts/edit-patch.sh <NN-name>` (target stays applied on top) |
| Regenerate + re-apply (`--continue` resumes after conflicts) | `bash scripts/finish-patch.sh <NN-name> [--continue]` |
| Start a patch (next free `NN`, appended last; prefixes never reordered) | `bash scripts/new-patch.sh <NN-name>`, then edit, then finish |
| Drop an empty patch | `quilt delete -r <NN-name>` |
| Upgrade upstream | re-resolve `git ls-remote https://github.com/jmrozanec/cron-utils.git HEAD` into `upstream.ref`, then `bash scripts/sync-upstream.sh`, resolving failures via the edit/finish cycle |

Rules: `quilt add` every file you will touch BEFORE editing or creating it
(refresh only records tracked files; unrecorded edits are silently missed).
Pushes use `--fuzz=0` (exact context); refresh uses `-p ab --no-timestamps
--no-index` (stable `a/`/`b/` headers). Mid-session trees may not compile
when deferred patches remove dependencies; verify after finishing.

## Features

- **Define Arbitrary Crons**: Define your own cron format with custom fields and constraints.
- **Predefined Definitions**: Unix, Cron4j, Quartz, and Spring definitions provided out-of-the-box.
- **Execution Time**: Calculate last/next execution time, duration from/to execution.
- **Cron Builder**: Decouple cron creation from specific providers.
- **Validation**: Validate cron strings against definitions or via Bean Validation.
- **Migration**: Map crons between different definitions (e.g., Quartz to Cron4j).
- **Human Readable**: Describe crons in multiple languages (English, German, Chinese, etc.).
