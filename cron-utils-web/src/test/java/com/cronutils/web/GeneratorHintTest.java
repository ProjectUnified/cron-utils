package com.cronutils.web;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.definition.FieldDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the generator's per-field hint and meaning texts: ranges, specials,
 * name aliases and the live reading of the current value.
 */
class GeneratorHintTest {

    private static FieldDefinition field(CronType type, CronFieldName name) {
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
        for (FieldDefinition field : Generator.orderedFields(definition)) {
            if (field.getFieldName() == name) {
                return field;
            }
        }
        throw new IllegalArgumentException("No field " + name + " in " + type);
    }

    @Test
    void secondHintShowsRangeAndBaseOperators() {
        String hint = Generator.hint(field(CronType.QUARTZ, CronFieldName.SECOND));
        assertTrue(hint.contains("0-59"), "range in: " + hint);
        assertTrue(hint.contains("* , - /"), "base operators in: " + hint);
    }

    @Test
    void dayOfWeekHintShowsQuestionMarkAndNames() {
        String hint = Generator.hint(field(CronType.QUARTZ, CronFieldName.DAY_OF_WEEK));
        assertTrue(hint.contains("?"), "question mark in: " + hint);
        assertTrue(hint.contains("MON"), "day names in: " + hint);
    }

    @Test
    void monthHintShowsNameAliases() {
        String hint = Generator.hint(field(CronType.QUARTZ, CronFieldName.MONTH));
        assertTrue(hint.contains("JAN"), "month names in: " + hint);
    }

    @Test
    void yearHintMarksOptional() {
        String hint = Generator.hint(field(CronType.QUARTZ, CronFieldName.YEAR));
        assertTrue(hint.contains("1970-2099"), "range in: " + hint);
        assertTrue(hint.contains("optional"), "optional marker in: " + hint);
    }

    @Test
    void meaningReadsCommonShapes() {
        FieldDefinition minute = field(CronType.QUARTZ, CronFieldName.MINUTE);
        assertEquals("every value", Generator.meaning(minute, "*"));
        assertEquals("at 12", Generator.meaning(minute, "12"));
        assertEquals("range 1-5", Generator.meaning(minute, "1-5"));
        assertEquals("list 1,2", Generator.meaning(minute, "1,2"));
        assertEquals("step */15", Generator.meaning(minute, "*/15"));
    }

    @Test
    void meaningReadsQuestionMarkAndEmpties() {
        FieldDefinition dow = field(CronType.QUARTZ, CronFieldName.DAY_OF_WEEK);
        assertEquals("no specific value", Generator.meaning(dow, "?"));
        assertEquals("omitted", Generator.meaning(field(CronType.QUARTZ, CronFieldName.YEAR), ""));
        assertEquals("required", Generator.meaning(dow, "  "));
    }

    @Test
    void meaningFlagsInvalidInput() {
        FieldDefinition minute = field(CronType.QUARTZ, CronFieldName.MINUTE);
        assertTrue(Generator.meaning(minute, "99").startsWith("invalid"),
                "out-of-range minute flagged");
    }
}
