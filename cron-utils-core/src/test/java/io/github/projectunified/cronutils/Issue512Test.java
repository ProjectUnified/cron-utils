package io.github.projectunified.cronutils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.projectunified.cronutils.builder.CronBuilder;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.model.field.expression.FieldExpressionFactory;
import io.github.projectunified.cronutils.model.time.ExecutionTime;

public class Issue512Test {
    
    @Test
    public void testPreviousMonthIsShorter() {
        LocalDateTime time = LocalDateTime.of(2022, 3, 30, 9, 55, 0);
        
        ExecutionTime execTime = ExecutionTime.forCron(CronBuilder
                .cron(CronDefinitionBuilder
                        .defineCron()
                        .withYear().and()
                        .withMonth().and()
                        .withDayOfMonth().and()
                        .withHours().and()
                        .withMinutes().and()
                        .withSeconds().and()
                        .instance())
                .withYear(FieldExpressionFactory.on(time.getYear()))
                .withMonth(FieldExpressionFactory.on(time.getMonthValue()))
                .withDoM(FieldExpressionFactory.on(time.getDayOfMonth()))
                .withHour(FieldExpressionFactory.on(time.getHour()))
                .withMinute(FieldExpressionFactory.on(time.getMinute()))
                .withSecond(FieldExpressionFactory.on(time.getSecond()))
                .instance());
        
        Optional<ZonedDateTime> result =
                execTime.lastExecution(ZonedDateTime.of(2022, 3, 30, 9, 54, 0, 0, ZoneId.systemDefault()));
        
        assertNotNull(result);
        assertFalse(result.isPresent());
    }
    
}
