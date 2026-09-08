package com.cronutils.web;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards schedule carry-over between cron types: mapped pairs round-trip to
 * a valid target expression, unmapped pairs and bad input yield null.
 */
class CarryOverTest {

    private static void assertValid(CronType type, String expression) {
        try {
            Cron cron = new CronParser(
                    CronDefinitionBuilder.instanceDefinitionFor(type)).parse(expression);
            cron.validate();
        } catch (RuntimeException e) {
            fail("not valid in " + type + ": \"" + expression + "\"", e);
        }
    }

    @Test
    void sameTypeCarriesIdentity() {
        for (CronType type : CronType.values()) {
            String daily = Generator.preset(type, "daily");
            String carried = Generator.carryOver(type, daily, type);
            assertNotNull(carried, "identity carry in " + type);
            assertValid(type, carried);
        }
    }

    @Test
    void mappedPairsCarryToValidExpressions() {
        for (CronType source : CronType.values()) {
            for (CronType target : CronType.values()) {
                if (Generator.mapperFor(source, target) == null) {
                    continue;
                }
                String daily = Generator.preset(source, "daily");
                String carried = Generator.carryOver(source, daily, target);
                assertNotNull(carried, "carry " + source + " to " + target);
                assertValid(target, carried);
            }
        }
    }

    @Test
    void noonCarriesQuartzToUnix() {
        String carried = Generator.carryOver(CronType.QUARTZ, "0 0 12 * * ?", CronType.UNIX);
        assertNotNull(carried, "quartz noon maps to unix");
        assertValid(CronType.UNIX, carried);
        assertTrue(carried.contains("12"), "noon kept in: " + carried);
    }

    @Test
    void unmappedPairYieldsNull() {
        boolean found = false;
        for (CronType source : CronType.values()) {
            for (CronType target : CronType.values()) {
                if (source != target && Generator.mapperFor(source, target) == null) {
                    String daily = Generator.preset(source, "daily");
                    assertNull(Generator.carryOver(source, daily, target),
                            "no carry " + source + " to " + target);
                    found = true;
                }
            }
        }
        assertTrue(found, "expected at least one unmapped pair");
    }

    @Test
    void carriedScheduleKeepsMeaning() {
        String carried = Generator.carryOver(CronType.QUARTZ, "0 15 10 ? * MON-FRI", CronType.UNIX);
        assertNotNull(carried, "weekday schedule maps");
        assertValid(CronType.UNIX, carried);
        assertTrue(carried.contains("15") && carried.contains("10"),
                "time kept in: " + carried);
    }

    @Test
    void badInputYieldsNull() {
        assertNull(Generator.carryOver(CronType.QUARTZ, "*/bad", CronType.UNIX), "invalid");
        assertNull(Generator.carryOver(CronType.QUARTZ, "", CronType.UNIX), "empty");
        assertNull(Generator.carryOver(CronType.QUARTZ, null, CronType.UNIX), "null");
        assertNull(Generator.carryOver(CronType.QUARTZ, "0 0 12 * * ?", null), "null target");
    }

}
