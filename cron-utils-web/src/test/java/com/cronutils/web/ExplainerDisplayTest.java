package com.cronutils.web;

import com.cronutils.model.CronType;
import com.cronutils.web.Explainer.ExplainResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the human-readable run format and the typed next runs behind it.
 */
class ExplainerDisplayTest {

    @Test
    void displayFormatsUtcRun() {
        assertEquals("Thu, 1 Jan 2026, 12:00:00 (+00:00)",
                Explainer.display(ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"))));
    }

    @Test
    void displayKeepsZoneOffset() {
        assertEquals("Thu, 1 Jan 2026, 12:00:00 (-05:00)",
                Explainer.display(ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0,
                        ZoneId.of("America/New_York"))));
    }

    @Test
    void explainReturnsTypedRuns() {
        ExplainResult result = Explainer.explain(CronType.QUARTZ, "0 0 12 * * ?",
                Locale.UK, ZoneId.of("UTC"),
                ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        assertTrue(result.ok, "valid noon cron explains");
        assertEquals(Explainer.NEXT_RUN_COUNT, result.nextRuns.size());
        assertEquals(ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")),
                result.nextRuns.get(0));
    }

    @Test
    void secondPrecisionScheduleKeepsSeconds() {
        ExplainResult result = Explainer.explain(CronType.QUARTZ, "30 0 12 * * ?",
                Locale.UK, ZoneId.of("UTC"),
                ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));
        assertTrue(result.ok, "second-precision cron explains");
        assertEquals(ZonedDateTime.of(2026, 1, 1, 12, 0, 30, 0, ZoneId.of("UTC")),
                result.nextRuns.get(0));
        assertEquals("Thu, 1 Jan 2026, 12:00:30 (+00:00)",
                Explainer.display(result.nextRuns.get(0)));
    }

    @Test
    void subMinuteStepsResolveWithinTheMinute() {
        ExplainResult result = Explainer.explain(CronType.QUARTZ, "*/15 * * * * ?",
                Locale.UK, ZoneId.of("UTC"),
                ZonedDateTime.of(2026, 1, 1, 0, 0, 5, 0, ZoneId.of("UTC")));
        assertTrue(result.ok, "step cron explains");
        assertEquals(Explainer.NEXT_RUN_COUNT, result.nextRuns.size());
        assertEquals(15, result.nextRuns.get(0).getSecond(), "first run on the :15 mark");
        for (int i = 1; i < result.nextRuns.size(); i++) {
            long gap = Duration.between(
                    result.nextRuns.get(i - 1), result.nextRuns.get(i)).getSeconds();
            assertEquals(15, gap, "runs stay 15 seconds apart");
        }
    }
}
