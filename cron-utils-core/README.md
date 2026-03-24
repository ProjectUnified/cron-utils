# cron-utils-core

> [!NOTE]
> This is a part of the `cron-utils` project, a fork of [https://github.com/jmrozanec/cron-utils](https://github.com/jmrozanec/cron-utils).

The **core** module provides the fundamental logic for:
- Defining cron formats (`CronDefinition`)
- Parsing cron expressions (`CronParser`)
- Building crons programmatically (`CronBuilder`)
- Calculating execution times (`ExecutionTime`)
- Mapping between different cron definitions (`CronMapper`)

## Usage

### Define and Parse a Cron

```java
import io.github.projectunified.cronutils.model.definition.CronDefinition;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.parser.CronParser;
import io.github.projectunified.cronutils.model.CronType;

CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
CronParser parser = new CronParser(cronDefinition);
Cron quartzCron = parser.parse("0 23 * ? * 1-5 *");
```

### Build a Cron

```java
import static io.github.projectunified.cronutils.model.field.expression.FieldExpressionFactory.*;
import io.github.projectunified.cronutils.builder.CronBuilder;

Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
    .withYear(always())
    .withDoM(between(SpecialChar.L, 3))
    .withMonth(always())
    .withDoW(questionMark())
    .withHour(always())
    .withMinute(always())
    .withSecond(on(0))
    .instance();
```

### Calculate Execution Time

```java
import io.github.projectunified.cronutils.model.time.ExecutionTime;
import java.time.ZonedDateTime;

ExecutionTime executionTime = ExecutionTime.forCron(quartzCron);
ZonedDateTime nextExecution = executionTime.nextExecution(ZonedDateTime.now()).orElse(null);
```
