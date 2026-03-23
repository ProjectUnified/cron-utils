package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.model.time.ExecutionTime;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static java.time.Duration.ofMillis;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue382Test {

    @Test
    public void testLastExecutionWithMillis() {
        CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        String cronString = "0 0 * * WED";
        Cron cron = cronParser.parse(cronString);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);

        ZonedDateTime date = ZonedDateTime.of(2019, 6, 12, 0, 0, 0, 0, UTC);
        ZonedDateTime lastExecution = executionTime.lastExecution(date.plus(ofMillis(300))).get();
        assertEquals(date, lastExecution);
    }
}
