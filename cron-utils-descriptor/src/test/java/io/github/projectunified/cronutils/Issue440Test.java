package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.descriptor.CronDescriptor;
import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue440Test {

	@Test
	public void testCase1() {
		CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
		CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);
		String description = descriptor.describe(parser.parse("* 2,1/31 * * * ?"));

		assertEquals("every second at minute 2 and every 31 minutes from minute 1", description);
	}

	@Test
	public void testCase2() {
		CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
		CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);
		String description = descriptor.describe(parser.parse("2,1/31 * * * *"));

		assertEquals("at minute 2 and every 31 minutes from minute 1", description);
	}
}
