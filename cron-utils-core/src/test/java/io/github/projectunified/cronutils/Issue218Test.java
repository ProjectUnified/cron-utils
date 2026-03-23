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

package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.definition.CronDefinition;
import io.github.projectunified.cronutils.model.time.ExecutionTime;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder.defineCron;

public class Issue218Test {

    /**
     * Issue #218 - isMatch() method should return true/false rather then throwing exception.
     */

    private static final String CRON_EXPRESSION = "0-59 7-16 MON-FRI";

    @Test
    public void testCronDefinitionExecutionTimeGenerator() {
        final CronDefinition cronDefinition = defineCron().withMinutes().and()
                .withHours().and()
                .withDayOfWeek()
                .optional()
                .and()
                .instance();
        final CronParser parser = new CronParser(cronDefinition);
        final Cron cron = parser.parse(CRON_EXPRESSION);
        final ExecutionTime executionTime = ExecutionTime.forCron(cron);

        executionTime.isMatch(ZonedDateTime.now());
    }
}
