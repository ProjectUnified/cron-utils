package io.github.projectunified.cronutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import io.github.projectunified.cronutils.descriptor.CronDescriptor;
import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinition;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.parser.CronParser;

public class Issue477Test {

	@Test
	public void testDescribe() {
		CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);
		CronDefinition cd = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
		CronParser parser = new CronParser(cd);

		assertEquals("every 2 minutes every 2 hours", descriptor.describe(parser.parse("*/2 */2 * * *")));
		assertEquals("every minute every 2 hours", descriptor.describe(parser.parse("* */2 * * *")));
		assertEquals("every minute every 2 hours", descriptor.describe(parser.parse("*/1 */2 * * *")));
	}

}
