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
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.model.time.ExecutionTime;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static io.github.projectunified.cronutils.model.CronType.QUARTZ;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by johnpatrick.manalo on 6/19/17.
 */
public class Issue200Test {

    @Test
    public void testMustMatchCronEvenIfNanoSecondsVaries() {
        final CronDefinition cronDefinition =
                CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);

        final CronParser parser = new CronParser(cronDefinition);
        final Cron quartzCron = parser.parse("00 00 10 * * ?");

        quartzCron.validate();

        // NOTE: Off by 3 nano seconds
        final ZonedDateTime zdt = ZonedDateTime.of(1999, 07, 18, 10, 00, 00, 03, ZoneId.systemDefault());

        // Must be true
        assertTrue(ExecutionTime.forCron(quartzCron).isMatch(zdt), "Nano seconds must not affect matching of Cron Expressions");
    }

    // Nano second-perfect (passes, no surprises here)
    @Test
    public void testMatchExact() {
        final CronDefinition cronDefinition =
                CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);

        final CronParser parser = new CronParser(cronDefinition);
        final Cron quartzCron = parser.parse("00 00 10 * * ?");

        quartzCron.validate();

        final ZonedDateTime zdt = ZonedDateTime.of(1999, 07, 18, 10, 00, 00, 00, ZoneId.systemDefault());

        assertTrue(ExecutionTime.forCron(quartzCron).isMatch(zdt), "Nano seconds must not affect matching of Cron Expressions");
    }
}
