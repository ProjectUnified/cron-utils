/*
 * Copyright 2015 jmrozanec
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.projectunified.cronutils.model.time.generator;

import io.github.projectunified.cronutils.mapper.WeekDay;
import io.github.projectunified.cronutils.model.field.CronField;
import io.github.projectunified.cronutils.model.field.CronFieldName;
import io.github.projectunified.cronutils.model.field.constraint.FieldConstraints;
import io.github.projectunified.cronutils.model.field.constraint.FieldConstraintsBuilder;
import io.github.projectunified.cronutils.model.field.expression.FieldExpression;
import io.github.projectunified.cronutils.model.field.expression.On;
import io.github.projectunified.cronutils.model.field.value.IntegerFieldValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class OnDayOfWeekValueGeneratorTest {
    private OnDayOfWeekValueGenerator fieldValueGenerator;
    private FieldConstraints constraints;
    private WeekDay mondayDoWValue;
    private static final int YEAR = 2015;
    private static final int MONTH = 2;

    @BeforeEach
    public void setUp() {
        constraints = FieldConstraintsBuilder.instance().createConstraintsInstance();
        mondayDoWValue = new WeekDay(1, false);
        fieldValueGenerator = new OnDayOfWeekValueGenerator(new CronField(CronFieldName.DAY_OF_WEEK, new On(new IntegerFieldValue(3)), constraints), YEAR,
                MONTH, mondayDoWValue);
    }

    @Test
    public void testMatchesFieldExpressionClass() {
        assertTrue(fieldValueGenerator.matchesFieldExpressionClass(mock(On.class)));
        assertFalse(fieldValueGenerator.matchesFieldExpressionClass(mock(FieldExpression.class)));
    }

    @Test
    public void testConstructorNotMatchesOnDayOfWeekValueGenerator() {
        assertThrows(IllegalArgumentException.class, () -> new OnDayOfWeekValueGenerator(new CronField(CronFieldName.YEAR, mock(FieldExpression.class), constraints), YEAR, MONTH, mondayDoWValue));
    }
}
