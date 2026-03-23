package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.model.time.ExecutionTime;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Issue319Test {
    @Test
    // Daylightsaving change in EU is - 2018-03-25T02:00
    // - Bug319: endless loop/fails/hangs at 2018-03-25T02:00 and 2018-03-26T02:00
    public void testPreviousClosestMatchDailightSavingsChangeBug319_loop() {
        CronParser cronparser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        for (int month = 1; month < 13; month++) {
            for (int day = 1; day < 29; day++) {
                ZonedDateTime date = ZonedDateTime.of(2018, month, day, 2, 00, 00, 0, ZoneId.of("Europe/Berlin"));
                System.out.print(date);
                Cron cron = cronparser.parse("00 02 * * * ");
                ExecutionTime exectime = ExecutionTime.forCron(cron);
                ZonedDateTime lastrun = exectime.lastExecution(date).get();
                System.out.println("-ok");
            }
        }
    }
}
