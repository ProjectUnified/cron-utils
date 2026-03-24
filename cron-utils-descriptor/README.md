# cron-utils-descriptor

> [!NOTE]
> This is a part of the `cron-utils project`, a fork of [https://github.com/jmrozanec/cron-utils](https://github.com/jmrozanec/cron-utils).

The **descriptor** module provides functionality to generate human-readable descriptions from cron expressions in multiple languages.

## Usage

### Generate Description

```java
import io.github.projectunified.cronutils.descriptor.CronDescriptor;
import io.github.projectunified.cronutils.model.Cron;
import java.util.Locale;

// Get a descriptor for a specific locale
CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);

// Describe a parsed cron
String description = descriptor.describe(quartzCron);
// Example output: "at minute 23 every day between Monday and Friday"
```

## Supported Locales

It supports many languages, including:
- English, German, Chinese, Japanese, Korean, Polish, Romanian, Spanish, Turkish, etc.
- Basic support for Dutch, French, Italian, Portuguese, and Russian.
