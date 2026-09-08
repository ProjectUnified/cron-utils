package com.cronutils.web;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.definition.FieldDefinition;
import com.cronutils.model.field.value.SpecialChar;
import com.cronutils.web.Explainer.ExplainResult;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the builder's opening state: every type's defaults must generate a
 * schedule that actually fires, so the landing page never reports
 * "No upcoming executions found."
 */
class DefaultValueTest {

    @Test
    void questionMarkOnlyForDayOfWeek() {
        for (CronType type : CronType.values()) {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
            for (FieldDefinition field : Generator.orderedFields(definition)) {
                boolean supported = field.getConstraints().getSpecialChars().contains(SpecialChar.QUESTION_MARK);
                if (field.getFieldName() == CronFieldName.DAY_OF_WEEK && supported) {
                    assertEquals("?", Generator.defaultValue(field), "dow default in " + type);
                } else {
                    assertEquals("*", Generator.defaultValue(field),
                            "default for " + type + " " + field.getFieldName());
                }
            }
        }
    }

    @Test
    void defaultsFireInEveryType() {
        ZonedDateTime from = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        for (CronType type : CronType.values()) {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
            Map<String, String> inputs = new LinkedHashMap<>();
            for (FieldDefinition field : Generator.orderedFields(definition)) {
                inputs.put(field.getFieldName().name(), Generator.defaultValue(field));
            }
            String expression = Generator.generate(type, inputs);
            ExplainResult result = Explainer.explain(type, expression, Locale.UK, ZoneId.of("UTC"), from);
            assertTrue(result.ok, "defaults valid in " + type + ": " + expression);
            assertEquals(Explainer.NEXT_RUN_COUNT, result.nextRuns.size(),
                    "defaults fire in " + type + ": " + expression);
        }
    }
}
