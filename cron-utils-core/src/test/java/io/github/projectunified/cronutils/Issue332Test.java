package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.model.time.ExecutionTime;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Issue332Test {

    @Test
    public void testIsMatchDailightSavingsChange_loop() {
        CronParser cronparser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        ZonedDateTime date = ZonedDateTime.of(2018, 8, 12, 3, 0, 0, 0, ZoneId.of("America/Santiago"));
        Cron cron = cronparser.parse("0 6 * * *");
        ExecutionTime exectime = ExecutionTime.forCron(cron);
        exectime.isMatch(date);
    }
}
