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
upstream commit recorded in `upstream.ref` and split into the four modules.
Changes are stored as **one patch per file**, mirroring the repo tree under
`patches/` (the same layout Paper uses for Minecraft sources):

```
cron-utils-core/src/main/java/com/cronutils/builder/CronBuilder.java
patches/cron-utils-core/src/main/java/com/cronutils/builder/CronBuilder.java.patch
```

Patches are ordinary `git diff` output applied with `git apply -p1`. There is
no series and no apply order: each patch owns exactly one file, so two changes
to the same file are the same patch and can never conflict. Version mapping:
`fork 2.x == upstream <SHA in upstream.ref> + patches/`.

The patch engine is [GitFile](https://github.com/ProjectUnified/GitFile); the
project-specific package split lives in `scripts/lib-split.sh` and is wired in
through `gitfile.json`.

Prereqs: `git`, JDK 21, Maven, and `gitfile`:

```sh
pipx install git+https://github.com/ProjectUnified/GitFile.git
```

```sh
gitfile sync && mvn clean package
```

`gitfile sync` builds two trees: `.vanilla/` (pristine split, used as the diff
baseline) and the working module trees with every patch applied.

| Task | Command |
|---|---|
| Edit code | `gitfile sync`, edit any module source, then `gitfile update` |
| Add a file | create it under a module `src/`, then `gitfile update` (writes a `/dev/null` patch) |
| Delete a file | `rm` it, then `gitfile update` (writes a deletion patch) |
| Drop a change | restore the file to its pristine content, then `gitfile update` (the patch is removed) |
| Verify from scratch | `gitfile sync && mvn clean package` |
| Export a combined patch for upstream | `gitfile export` (`cron-utils-fork.patch`, paths rewritten to upstream `src/`) |
| Upgrade upstream | re-resolve `git ls-remote https://github.com/jmrozanec/cron-utils.git HEAD` into `upstream.ref`, then `gitfile sync`. Conflicting patches are three-way merged; resolve markers/`.rej` files and run `gitfile update` |

Rules: edit the working tree, never the `.vanilla` baseline. `gitfile update`
requires a successful `gitfile sync` (the `.vanilla/.gitfile-applied` marker),
so it cannot turn an unpatched tree into deletion patches. `gitfile sync` is
the reset hatch and discards unrecorded working-tree edits.

## Features

- **Define Arbitrary Crons**: Define your own cron format with custom fields and constraints.
- **Predefined Definitions**: Unix, Cron4j, Quartz, and Spring definitions provided out-of-the-box.
- **Execution Time**: Calculate last/next execution time, duration from/to execution.
- **Cron Builder**: Decouple cron creation from specific providers.
- **Validation**: Validate cron strings against definitions or via Bean Validation.
- **Migration**: Map crons between different definitions (e.g., Quartz to Cron4j).
- **Human Readable**: Describe crons in multiple languages (English, German, Chinese, etc.).
