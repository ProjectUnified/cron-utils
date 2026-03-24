# cron-utils-validator

> [!NOTE]
> This is a part of the `cron-utils` project, a fork of [https://github.com/jmrozanec/cron-utils](https://github.com/jmrozanec/cron-utils).

The **validator** module provides Jakarta Bean Validation support, allowing you to validate cron expressions using annotations in your DTOs.

## Usage

### Bean Validation

Add the `@Cron` annotation to your fields:

```java
import io.github.projectunified.cronutils.validation.Cron;
import io.github.projectunified.cronutils.model.CronType;

public class MyDto {
    @Cron(type = CronType.QUARTZ)
    private String cronExpression;
}
```

## Dependencies

This module requires a Jakarta Bean Validation provider (such as Hibernate Validator) to be present on the classpath.
