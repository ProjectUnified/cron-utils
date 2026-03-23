package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue413Test {
    @Test
    public void testFridayToSaturdayUnix() {
        final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        try {
            parser.parse("* * * */12 *");
        } catch (IllegalArgumentException expected) {
            assertEquals("Failed to parse '* * * */12 *'. Period 12 not in range [1, 12]", expected.getMessage());
        }
    }
}
