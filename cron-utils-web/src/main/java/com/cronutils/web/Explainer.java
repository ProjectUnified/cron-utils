package com.cronutils.web;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure cron explainer logic: parse, validate, describe, next runs.
 * No DOM access; safe to unit-test on the JVM.
 */
public final class Explainer {

    /** Number of upcoming executions reported for a valid cron. */
    public static final int NEXT_RUN_COUNT = 5;

    private Explainer() {
    }

    /** Outcome of {@link #explain(CronType, String, Locale, ZoneId, ZonedDateTime)}. */
    public static final class ExplainResult {
        /** True when the expression parsed and validated. */
        public final boolean ok;
        /** True when the input was empty: show a hint, not an error. */
        public final boolean hint;
        /** User-facing error message; non-null only when {@code !ok && !hint}. */
        public final String error;
        /** Normalized expression ({@code Cron.asString()}); non-null only when {@code ok}. */
        public final String normalized;
        /** Human-readable description; non-null only when {@code ok}. */
        public final String description;
        /** Next executions; empty when none found. */
        public final List<ZonedDateTime> nextRuns;

        private ExplainResult(boolean ok, boolean hint, String error,
                              String normalized, String description, List<ZonedDateTime> nextRuns) {
            this.ok = ok;
            this.hint = hint;
            this.error = error;
            this.normalized = normalized;
            this.description = description;
            this.nextRuns = nextRuns;
        }

        static ExplainResult hint() {
            return new ExplainResult(false, true, null, null, null, Collections.<ZonedDateTime>emptyList());
        }

        static ExplainResult error(String message) {
            return new ExplainResult(false, false, message, null, null, Collections.<ZonedDateTime>emptyList());
        }

        static ExplainResult success(String normalized, String description, List<ZonedDateTime> nextRuns) {
            return new ExplainResult(true, false, null, normalized, description, nextRuns);
        }
    }

    /**
     * Parses and validates {@code expression} for {@code type}, then returns the
     * normalized form, the locale-specific description and the next executions
     * after {@code from} in {@code zone}.
     *
     * @param type       cron dialect; never null
     * @param expression raw user input; may be null or empty (yields a hint)
     * @param locale     description locale; never null
     * @param zone       zone for upcoming executions; never null
     * @param from       reference date-time; null yields an empty run list
     * @return result; never null
     */
    public static ExplainResult explain(CronType type, String expression, Locale locale,
                                        ZoneId zone, ZonedDateTime from) {
        if (expression == null || expression.trim().isEmpty()) {
            return ExplainResult.hint();
        }
        try {
            CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(type);
            Cron cron = new CronParser(definition).parse(expression);
            cron.validate();
            String normalized = cron.asString();
            String description = new CronDescriptor(EmbeddedBundles.forLocale(locale)).describe(cron);
            return ExplainResult.success(normalized, description, nextRuns(cron, zone, from));
        } catch (IllegalArgumentException e) {
            return ExplainResult.error(e.getMessage());
        } catch (RuntimeException e) {
            String message = e.getMessage();
            return ExplainResult.error(message == null ? e.toString() : message);
        }
    }

    /**
     * Short readable form of a run, e.g. {@code "Thu, 1 Jan 2026, 12:00 (+00:00)"}.
     * Always English so it renders identically for every description language.
     *
     * @param dateTime run; never null
     * @return display text; never null
     */
    public static String display(ZonedDateTime dateTime) {
        return DateTimeFormatter.ofPattern("EEE, d MMM yyyy, HH:mm (xxx)", Locale.ENGLISH)
                .format(dateTime);
    }

    private static List<ZonedDateTime> nextRuns(Cron cron, ZoneId zone, ZonedDateTime from) {
        if (from == null) {
            return Collections.<ZonedDateTime>emptyList();
        }
        List<ZonedDateTime> runs = new ArrayList<>(NEXT_RUN_COUNT);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        ZonedDateTime cursor = from.withZoneSameInstant(zone);
        for (int i = 0; i < NEXT_RUN_COUNT; i++) {
            Optional<ZonedDateTime> next = executionTime.nextExecution(cursor);
            if (!next.isPresent()) {
                break;
            }
            cursor = next.get();
            runs.add(cursor);
        }
        return runs;
    }
}
