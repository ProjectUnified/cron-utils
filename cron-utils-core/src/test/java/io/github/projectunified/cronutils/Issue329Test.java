package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.model.time.ExecutionTime;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

public class Issue329Test {
    @Test
    public void infiniteLoop() {
        Cron cron = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX))
                .parse("0 0 30 2 *");//m H DoM M DoW
        ExecutionTime.forCron(cron).nextExecution(ZonedDateTime.now());
    }
}
