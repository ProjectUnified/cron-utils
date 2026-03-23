package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.builder.CronBuilder;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import org.junit.jupiter.api.Test;

import static io.github.projectunified.cronutils.model.CronType.UNIX;
import static io.github.projectunified.cronutils.model.field.expression.FieldExpression.always;
import static io.github.projectunified.cronutils.model.field.expression.FieldExpressionFactory.on;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Issue459Test {
    @Test
    public void testNegativeValuesNotAllowed() {
        assertThrows(RuntimeException.class, () -> CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(UNIX))
                .withDoM(always())
                .withMonth(always())
                .withDoW(always())
                .withHour(on(-1))
                .withMinute(on(5))
                .instance());
    }
}
