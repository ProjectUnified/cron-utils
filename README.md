# cron-utils
> [!NOTE]
> This is a fork of [https://github.com/jmrozanec/cron-utils](https://github.com/jmrozanec/cron-utils).

`cron-utils` is a Java library to define, parse, validate, and migrate crons. The project follows the [Semantic Versioning Convention](https://semver.org/), provides OSGi metadata, and uses the Apache 2.0 license.

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
    <version>9.2.2-SNAPSHOT</version>
</dependency>
```

## Features

- **Define Arbitrary Crons**: Define your own cron format with custom fields and constraints.
- **Predefined Definitions**: Unix, Cron4j, Quartz, and Spring definitions provided out-of-the-box.
- **Execution Time**: Calculate last/next execution time, duration from/to execution.
- **Cron Builder**: Decouple cron creation from specific providers.
- **Validation**: Validate cron strings against definitions or via Bean Validation.
- **Migration**: Map crons between different definitions (e.g., Quartz to Cron4j).
- **Human Readable**: Describe crons in multiple languages (English, German, Chinese, etc.).
