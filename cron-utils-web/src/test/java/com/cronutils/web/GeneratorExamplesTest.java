package com.cronutils.web;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.parser.CronParser;
import com.cronutils.model.definition.CronDefinitionBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards example genericity: every {@link CronWebApp} shelf example must
 * build a valid expression in every {@link CronType} (loading uses the
 * generator's current type, never a fixed one).
 */
class GeneratorExamplesTest {

    @Test
    void everyExampleBuildsAndValidatesInEveryType() {
        assertFalse(CronWebApp.EXAMPLES.length == 0, "example shelf is empty");
        for (CronWebApp.Example example : CronWebApp.EXAMPLES) {
            for (CronType type : CronType.values()) {
                String expression;
                try {
                    expression = Generator.buildExample(type, example.fields);
                } catch (IllegalArgumentException e) {
                    fail("example \"" + example.label + "\" does not build in " + type, e);
                    return;
                }
                try {
                    Cron cron = new CronParser(
                            CronDefinitionBuilder.instanceDefinitionFor(type)).parse(expression);
                    cron.validate();
                } catch (IllegalArgumentException e) {
                    fail("example \"" + example.label + "\" builds to invalid \""
                            + expression + "\" in " + type, e);
                }
            }
        }
    }

    @Test
    void questionMarkDegradesToStarWhereUnsupported() {
        CronWebApp.Example weekdays = null;
        for (CronWebApp.Example example : CronWebApp.EXAMPLES) {
            if ("Weekdays at 10:15".equals(example.label)) {
                weekdays = example;
            }
        }
        assertFalse(weekdays == null, "weekday example missing");
        assertEquals("*",
                Generator.exampleInputs(CronType.UNIX, weekdays.fields).get("DAY_OF_MONTH"),
                "unix has no question mark");
    }
}
