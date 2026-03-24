# cron-utils-converter

> [!NOTE]
> This is a part of the `cron-utils` project, a fork of [https://github.com/jmrozanec/cron-utils](https://github.com/jmrozanec/cron-utils).

The **converter** module provides tools for transforming cron expressions into other formats or calendar-based representations.

## Usage

### Cron Conversion

```java
import io.github.projectunified.cronutils.converter.CronConverter;
import java.time.ZoneId;

// Example of using CronConverter (logic depends on your specific transformers)
CronConverter converter = new CronConverter(toCalendarTransformer, toCronTransformer);
String result = converter.using("0 23 * * *").from(ZoneId.of("UTC")).to(ZoneId.of("Europe/Berlin")).convert();
```
