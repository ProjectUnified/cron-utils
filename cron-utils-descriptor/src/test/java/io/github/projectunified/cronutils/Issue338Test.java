package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.descriptor.CronDescriptor;
import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue338Test {

	@Test
	public void testEverySecondInFrench() {
		CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
		String cronString = "* * * * * ? *";
		Cron cron = cronParser.parse(cronString);
		String description = CronDescriptor.instance(Locale.FRANCE).describe(cron);
		assertEquals("chaque seconde", description);
	}
}
