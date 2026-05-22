package io.github.projectunified.cronutils.model.time;

import io.github.projectunified.cronutils.model.CronType;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.parser.CronParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionTimeInstantAndLongTest {
    private ExecutionTime executionTime;
    private ZoneId zoneId;
    private ZonedDateTime startDateTime;
    private Instant startInstant;
    private long startMillis;

    @BeforeEach
    public void setUp() {
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        // Cron: every day at 12:00:00 UTC
        executionTime = ExecutionTime.forCron(parser.parse("0 0 12 * * ?"));
        zoneId = ZoneId.of("UTC");
        
        // Start date: 2026-05-23T10:00:00Z
        startDateTime = ZonedDateTime.of(2026, 5, 23, 10, 0, 0, 0, zoneId);
        startInstant = startDateTime.toInstant();
        startMillis = startInstant.toEpochMilli();
    }

    @Test
    public void testNextExecution() {
        Instant expectedNextInstant = ZonedDateTime.of(2026, 5, 23, 12, 0, 0, 0, zoneId).toInstant();
        long expectedNextMillis = expectedNextInstant.toEpochMilli();

        Optional<Instant> nextInstant = executionTime.nextExecution(startInstant, zoneId);
        assertTrue(nextInstant.isPresent());
        assertEquals(expectedNextInstant, nextInstant.get());

        OptionalLong nextMillis = executionTime.nextExecution(startMillis, zoneId);
        assertTrue(nextMillis.isPresent());
        assertEquals(expectedNextMillis, nextMillis.getAsLong());
    }

    @Test
    public void testTimeToNextExecution() {
        Duration expectedDuration = Duration.ofHours(2);

        Optional<Duration> durationInstant = executionTime.timeToNextExecution(startInstant, zoneId);
        assertTrue(durationInstant.isPresent());
        assertEquals(expectedDuration, durationInstant.get());

        Optional<Duration> durationMillis = executionTime.timeToNextExecution(startMillis, zoneId);
        assertTrue(durationMillis.isPresent());
        assertEquals(expectedDuration, durationMillis.get());
    }

    @Test
    public void testLastExecution() {
        Instant expectedLastInstant = ZonedDateTime.of(2026, 5, 22, 12, 0, 0, 0, zoneId).toInstant();
        long expectedLastMillis = expectedLastInstant.toEpochMilli();

        Optional<Instant> lastInstant = executionTime.lastExecution(startInstant, zoneId);
        assertTrue(lastInstant.isPresent());
        assertEquals(expectedLastInstant, lastInstant.get());

        OptionalLong lastMillis = executionTime.lastExecution(startMillis, zoneId);
        assertTrue(lastMillis.isPresent());
        assertEquals(expectedLastMillis, lastMillis.getAsLong());
    }

    @Test
    public void testTimeFromLastExecution() {
        Duration expectedDuration = Duration.ofHours(22);

        Optional<Duration> durationInstant = executionTime.timeFromLastExecution(startInstant, zoneId);
        assertTrue(durationInstant.isPresent());
        assertEquals(expectedDuration, durationInstant.get());

        Optional<Duration> durationMillis = executionTime.timeFromLastExecution(startMillis, zoneId);
        assertTrue(durationMillis.isPresent());
        assertEquals(expectedDuration, durationMillis.get());
    }

    @Test
    public void testIsMatch() {
        // 12:00:00 should match
        ZonedDateTime matchDateTime = ZonedDateTime.of(2026, 5, 23, 12, 0, 0, 0, zoneId);
        Instant matchInstant = matchDateTime.toInstant();
        long matchMillis = matchInstant.toEpochMilli();

        assertTrue(executionTime.isMatch(matchInstant, zoneId));
        assertTrue(executionTime.isMatch(matchMillis, zoneId));

        // 10:00:00 should not match
        assertFalse(executionTime.isMatch(startInstant, zoneId));
        assertFalse(executionTime.isMatch(startMillis, zoneId));
    }

    @Test
    public void testCountExecutions() {
        Instant endInstant = ZonedDateTime.of(2026, 5, 25, 15, 0, 0, 0, zoneId).toInstant();
        long endMillis = endInstant.toEpochMilli();

        // 2026-05-23T12:00:00, 2026-05-24T12:00:00, 2026-05-25T12:00:00 -> 3 executions
        assertEquals(3, executionTime.countExecutions(startInstant, endInstant, zoneId));
        assertEquals(3, executionTime.countExecutions(startMillis, endMillis, zoneId));
    }

    @Test
    public void testGetExecutionDates() {
        Instant endInstant = ZonedDateTime.of(2026, 5, 25, 15, 0, 0, 0, zoneId).toInstant();
        long endMillis = endInstant.toEpochMilli();

        List<Instant> datesInstant = executionTime.getExecutionDates(startInstant, endInstant, zoneId);
        assertEquals(3, datesInstant.size());
        assertEquals(ZonedDateTime.of(2026, 5, 23, 12, 0, 0, 0, zoneId).toInstant(), datesInstant.get(0));
        assertEquals(ZonedDateTime.of(2026, 5, 24, 12, 0, 0, 0, zoneId).toInstant(), datesInstant.get(1));
        assertEquals(ZonedDateTime.of(2026, 5, 25, 12, 0, 0, 0, zoneId).toInstant(), datesInstant.get(2));

        List<Long> datesMillis = executionTime.getExecutionDates(startMillis, endMillis, zoneId);
        assertEquals(3, datesMillis.size());
        assertEquals(ZonedDateTime.of(2026, 5, 23, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli(), (long) datesMillis.get(0));
        assertEquals(ZonedDateTime.of(2026, 5, 24, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli(), (long) datesMillis.get(1));
        assertEquals(ZonedDateTime.of(2026, 5, 25, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli(), (long) datesMillis.get(2));
    }

    @Test
    public void testDefaultTimezoneMethods() {
        // Verifying default system time zone overloads do not throw exceptions and execute cleanly
        assertNotNull(executionTime.nextExecution(startInstant));
        assertNotNull(executionTime.nextExecution(startMillis));
        assertNotNull(executionTime.timeToNextExecution(startInstant));
        assertNotNull(executionTime.timeToNextExecution(startMillis));
        assertNotNull(executionTime.lastExecution(startInstant));
        assertNotNull(executionTime.lastExecution(startMillis));
        assertNotNull(executionTime.timeFromLastExecution(startInstant));
        assertNotNull(executionTime.timeFromLastExecution(startMillis));
        
        executionTime.isMatch(startInstant);
        executionTime.isMatch(startMillis);

        Instant endInstant = startInstant.plus(Duration.ofDays(2));
        long endMillis = startMillis + Duration.ofDays(2).toMillis();
        assertTrue(executionTime.countExecutions(startInstant, endInstant) >= 0);
        assertTrue(executionTime.countExecutions(startMillis, endMillis) >= 0);
        assertNotNull(executionTime.getExecutionDates(startInstant, endInstant));
        assertNotNull(executionTime.getExecutionDates(startMillis, endMillis));
    }
}
