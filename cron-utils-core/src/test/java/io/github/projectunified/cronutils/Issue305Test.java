package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinition;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.model.time.ExecutionTime;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue305Test {

    @Test
    public void testIssue305(){
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
        CronParser parser = new CronParser(cronDefinition);
        Cron cron = parser.parse("0 0 0 15 8 ? 2015-2099/2");

        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        Set<ZonedDateTime> dates = new LinkedHashSet<>();
        while (!nextExecution.get().isAfter(ZonedDateTime.of(2020, 12, 31, 0, 0, 0, 0, ZoneId.of("UTC")))) {
            dates.add(nextExecution.get());
            nextExecution = executionTime.nextExecution(nextExecution.get());
        }
        Set<Integer> years = dates.stream().map(d->d.getYear()).collect(Collectors.toSet());
        Set<Integer> expectedYears = new HashSet<>();
        expectedYears.add(2015);
        expectedYears.add(2017);
        expectedYears.add(2019);
        assertEquals(expectedYears, years);
    }
}
