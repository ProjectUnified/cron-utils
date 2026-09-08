package com.cronutils.web;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.definition.FieldDefinition;
import com.cronutils.model.field.expression.FieldExpression;
import com.cronutils.model.field.expression.visitor.ValidationFieldExpressionVisitor;
import com.cronutils.parser.CronParserField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the generator's teachable field guide: quick values and syntax rows
 * stay valid for every field of every type.
 */
class FieldGuideTest {

    @Test
    void quickValuesAlwaysValidate() {
        for (CronType type : CronType.values()) {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
            for (FieldDefinition field : Generator.orderedFields(definition)) {
                List<String> quick = Generator.quickValues(field);
                assertFalse(quick.isEmpty(), "quick values for " + type + " " + field.getFieldName());
                assertTrue(quick.size() <= 4, "at most 4 for " + type + " " + field.getFieldName());
                for (String value : quick) {
                    assertFalse(value == null || value.isEmpty(),
                            "quick value non-empty for " + type + " " + field.getFieldName());
                    assertValid(field, value);
                }
            }
        }
    }

    @Test
    void syntaxGuideExamplesValidate() {
        for (CronType type : CronType.values()) {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
            for (FieldDefinition field : Generator.orderedFields(definition)) {
                List<Generator.SyntaxItem> guide = Generator.syntaxGuide(field);
                assertFalse(guide.isEmpty(), "guide for " + type + " " + field.getFieldName());
                boolean hasEvery = false;
                for (Generator.SyntaxItem item : guide) {
                    assertFalse(item.pattern == null || item.pattern.isEmpty(), "pattern set");
                    assertTrue(item.example != null, "example non-null");
                    assertFalse(item.explains == null || item.explains.trim().isEmpty(), "explains set");
                    if ("*".equals(item.pattern)) {
                        hasEvery = true;
                    }
                    if (item.example.isEmpty()) {
                        assertTrue(field.isOptional(),
                                "empty example only for optional " + type + " " + field.getFieldName());
                    } else {
                        assertValid(field, item.example);
                    }
                }
                assertTrue(hasEvery, "every-value row for " + type + " " + field.getFieldName());
            }
        }
    }

    private static void assertValid(FieldDefinition field, String value) {
        try {
            FieldExpression expression = new CronParserField(field.getFieldName(),
                    field.getConstraints(), field.isOptional()).parse(value).getExpression();
            expression.accept(new ValidationFieldExpressionVisitor(field.getConstraints()));
        } catch (RuntimeException e) {
            fail("value \"" + value + "\" invalid for " + field.getFieldName()
                    + " (" + field.getConstraints().getStartRange()
                    + "-" + field.getConstraints().getEndRange() + ")", e);
        }
    }
}
