package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinition;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.model.time.ExecutionTime;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class Issue430Test {
    @Test
    public void test() {
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
        CronParser parser = new CronParser(cronDefinition);
        Cron cron = parser.parse("0 0 12 30 6 ? 2020/10");
        ExecutionTime execution = ExecutionTime.forCron(cron);
        ZonedDateTime dateTime = ZonedDateTime.of(2020, 6, 30, 12, 0, 0, 0, ZoneOffset.UTC);
        // The cron starts from 2020, so no last execution date should be returned.
        assertNull(execution.lastExecution(dateTime).orElse(null));
        assertEquals(dateTime.plusYears(10), execution.nextExecution(dateTime).orElse(null));
    }
}
