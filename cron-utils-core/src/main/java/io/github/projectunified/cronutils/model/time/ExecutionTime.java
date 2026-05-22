/*
 * Copyright 2014 jmrozanec
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

package io.github.projectunified.cronutils.model.time;

import io.github.projectunified.cronutils.model.CompositeCron;
import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.SingleCron;
import io.github.projectunified.cronutils.model.field.CronField;
import io.github.projectunified.cronutils.model.field.CronFieldName;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * Calculates execution time given a cron pattern.
 */
public interface ExecutionTime {

    /**
     * Creates execution time for given Cron.
     *
     * @param cron - Cron instance
     * @return ExecutionTime instance
     */
    public static ExecutionTime forCron(final Cron cron) {
        if (cron instanceof SingleCron) {
            final Map<CronFieldName, CronField> fields = cron.retrieveFieldsAsMap();
            final ExecutionTimeBuilder executionTimeBuilder = new ExecutionTimeBuilder(cron);
            for (final CronFieldName name : CronFieldName.values()) {
                if (fields.get(name) != null) {
                    switch (name) {
                        case SECOND:
                            executionTimeBuilder.forSecondsMatching(fields.get(name));
                            break;
                        case MINUTE:
                            executionTimeBuilder.forMinutesMatching(fields.get(name));
                            break;
                        case HOUR:
                            executionTimeBuilder.forHoursMatching(fields.get(name));
                            break;
                        case DAY_OF_WEEK:
                            executionTimeBuilder.forDaysOfWeekMatching(fields.get(name));
                            break;
                        case DAY_OF_MONTH:
                            executionTimeBuilder.forDaysOfMonthMatching(fields.get(name));
                            break;
                        case MONTH:
                            executionTimeBuilder.forMonthsMatching(fields.get(name));
                            break;
                        case YEAR:
                            executionTimeBuilder.forYearsMatching(fields.get(name));
                            break;
                        case DAY_OF_YEAR:
                            executionTimeBuilder.forDaysOfYearMatching(fields.get(name));
                            break;
                        default:
                            break;
                    }
                }
            }
            return executionTimeBuilder.build();
        }
        if (cron instanceof CompositeCron) {
            return new CompositeExecutionTime(((CompositeCron) cron).getCrons().parallelStream().map(ExecutionTime::forCron).collect(Collectors.toList()));
        }

        return new ExecutionTime() {
            @Override
            public Optional<ZonedDateTime> nextExecution(ZonedDateTime date) {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> timeToNextExecution(ZonedDateTime date) {
                return Optional.empty();
            }

            @Override
            public Optional<ZonedDateTime> lastExecution(ZonedDateTime date) {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> timeFromLastExecution(ZonedDateTime date) {
                return Optional.empty();
            }

            @Override
            public boolean isMatch(ZonedDateTime date) {
                return false;
            }
        };
    }

    /**
     * Provide nearest date for next execution.
     *
     * @param date - ZonedDateTime instance. If null, a NullPointerException will be raised.
     * @return Optional ZonedDateTime instance, never null. Contains next execution time or empty.
     */
    Optional<ZonedDateTime> nextExecution(final ZonedDateTime date);

    /**
     * Provide nearest date for next execution.
     *
     * @param instant - Instant instance. If null, a NullPointerException will be raised.
     * @return Optional Instant instance, never null. Contains next execution time or empty.
     */
    default Optional<Instant> nextExecution(final Instant instant) {
        return nextExecution(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())).map(ZonedDateTime::toInstant);
    }

    /**
     * Provide nearest date for next execution.
     *
     * @param instant - Instant instance. If null, a NullPointerException will be raised.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return Optional Instant instance, never null. Contains next execution time or empty.
     */
    default Optional<Instant> nextExecution(final Instant instant, final ZoneId zoneId) {
        return nextExecution(ZonedDateTime.ofInstant(instant, zoneId)).map(ZonedDateTime::toInstant);
    }

    /**
     * Provide nearest date for next execution.
     *
     * @param millis - milliseconds in long.
     * @return OptionalLong instance, never null. Contains next execution time or empty.
     */
    default OptionalLong nextExecution(final long millis) {
        Optional<Instant> next = nextExecution(Instant.ofEpochMilli(millis));
        return next.isPresent() ? OptionalLong.of(next.get().toEpochMilli()) : OptionalLong.empty();
    }

    /**
     * Provide nearest date for next execution.
     *
     * @param millis - milliseconds in long.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return OptionalLong instance, never null. Contains next execution time or empty.
     */
    default OptionalLong nextExecution(final long millis, final ZoneId zoneId) {
        Optional<Instant> next = nextExecution(Instant.ofEpochMilli(millis), zoneId);
        return next.isPresent() ? OptionalLong.of(next.get().toEpochMilli()) : OptionalLong.empty();
    }

    /**
     * Provide nearest time for next execution.
     *
     * Due to the question #468 we clarify: crons execute on local instance time.
     * See: https://serverfault.com/questions/791713/what-time-zone-is-a-cron-job-using
     * We ask for a ZonedDateTime for two reasons:
     * (i) to provide flexibility on which timezone the cron is being executed
     * (ii) to be able to reproduce issues regardless of our own local time (e.g.: daylight savings, etc.)
     *
     * @param date - ZonedDateTime instance. If null, a NullPointerException will be raised.
     * @return Duration instance, never null. Time to next execution.
     */
    Optional<Duration> timeToNextExecution(final ZonedDateTime date);

    /**
     * Provide nearest time for next execution.
     *
     * @param instant - Instant instance. If null, a NullPointerException will be raised.
     * @return Duration instance, never null. Time to next execution.
     */
    default Optional<Duration> timeToNextExecution(final Instant instant) {
        return timeToNextExecution(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    /**
     * Provide nearest time for next execution.
     *
     * @param instant - Instant instance. If null, a NullPointerException will be raised.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return Duration instance, never null. Time to next execution.
     */
    default Optional<Duration> timeToNextExecution(final Instant instant, final ZoneId zoneId) {
        return timeToNextExecution(ZonedDateTime.ofInstant(instant, zoneId));
    }

    /**
     * Provide nearest time for next execution.
     *
     * @param millis - milliseconds in long.
     * @return Duration instance, never null. Time to next execution.
     */
    default Optional<Duration> timeToNextExecution(final long millis) {
        return timeToNextExecution(Instant.ofEpochMilli(millis));
    }

    /**
     * Provide nearest time for next execution.
     *
     * @param millis - milliseconds in long.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return Duration instance, never null. Time to next execution.
     */
    default Optional<Duration> timeToNextExecution(final long millis, final ZoneId zoneId) {
        return timeToNextExecution(Instant.ofEpochMilli(millis), zoneId);
    }

    /**
     * Provide nearest date for last execution.
     *
     * Due to the question #468 we clarify: crons execute on local instance time.
     * See: https://serverfault.com/questions/791713/what-time-zone-is-a-cron-job-using
     * We ask for a ZonedDateTime for two reasons:
     * (i) to provide flexibility on which timezone the cron is being executed
     * (ii) to be able to reproduce issues regardless of our own local time (e.g.: daylight savings, etc.)
     *
     * @param date - ZonedDateTime instance. If null, a NullPointerException will be raised.
     * @return Optional ZonedDateTime instance, never null. Last execution time or empty.
     */
    Optional<ZonedDateTime> lastExecution(final ZonedDateTime date);

    /**
     * Provide nearest date for last execution.
     *
     * @param instant - Instant instance. If null, a NullPointerException will be raised.
     * @return Optional Instant instance, never null. Last execution time or empty.
     */
    default Optional<Instant> lastExecution(final Instant instant) {
        return lastExecution(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())).map(ZonedDateTime::toInstant);
    }

    /**
     * Provide nearest date for last execution.
     *
     * @param instant - Instant instance. If null, a NullPointerException will be raised.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return Optional Instant instance, never null. Last execution time or empty.
     */
    default Optional<Instant> lastExecution(final Instant instant, final ZoneId zoneId) {
        return lastExecution(ZonedDateTime.ofInstant(instant, zoneId)).map(ZonedDateTime::toInstant);
    }

    /**
     * Provide nearest date for last execution.
     *
     * @param millis - milliseconds in long.
     * @return OptionalLong instance, never null. Last execution time or empty.
     */
    default OptionalLong lastExecution(final long millis) {
        Optional<Instant> last = lastExecution(Instant.ofEpochMilli(millis));
        return last.isPresent() ? OptionalLong.of(last.get().toEpochMilli()) : OptionalLong.empty();
    }

    /**
     * Provide nearest date for last execution.
     *
     * @param millis - milliseconds in long.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return OptionalLong instance, never null. Last execution time or empty.
     */
    default OptionalLong lastExecution(final long millis, final ZoneId zoneId) {
        Optional<Instant> last = lastExecution(Instant.ofEpochMilli(millis), zoneId);
        return last.isPresent() ? OptionalLong.of(last.get().toEpochMilli()) : OptionalLong.empty();
    }

    /**
     * Provide nearest time from last execution.
     *
     * @param date - ZonedDateTime instance. If null, a NullPointerException will be raised.
     * @return Duration instance, never null. Time from last execution.
     */
    Optional<Duration> timeFromLastExecution(final ZonedDateTime date);

    /**
     * Provide nearest time from last execution.
     *
     * @param instant - Instant instance. If null, a NullPointerException will be raised.
     * @return Duration instance, never null. Time from last execution.
     */
    default Optional<Duration> timeFromLastExecution(final Instant instant) {
        return timeFromLastExecution(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    /**
     * Provide nearest time from last execution.
     *
     * @param instant - Instant instance. If null, a NullPointerException will be raised.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return Duration instance, never null. Time from last execution.
     */
    default Optional<Duration> timeFromLastExecution(final Instant instant, final ZoneId zoneId) {
        return timeFromLastExecution(ZonedDateTime.ofInstant(instant, zoneId));
    }

    /**
     * Provide nearest time from last execution.
     *
     * @param millis - milliseconds in long.
     * @return Duration instance, never null. Time from last execution.
     */
    default Optional<Duration> timeFromLastExecution(final long millis) {
        return timeFromLastExecution(Instant.ofEpochMilli(millis));
    }

    /**
     * Provide nearest time from last execution.
     *
     * @param millis - milliseconds in long.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return Duration instance, never null. Time from last execution.
     */
    default Optional<Duration> timeFromLastExecution(final long millis, final ZoneId zoneId) {
        return timeFromLastExecution(Instant.ofEpochMilli(millis), zoneId);
    }

    /**
     * Provide feedback if a given date matches the cron expression.
     *
     * @param date - ZonedDateTime instance. If null, a NullPointerException will be raised.
     * @return true if date matches cron expression requirements, false otherwise.
     */
    boolean isMatch(ZonedDateTime date);

    /**
     * Provide feedback if a given date matches the cron expression.
     *
     * @param instant - Instant instance. If null, a NullPointerException will be raised.
     * @return true if date matches cron expression requirements, false otherwise.
     */
    default boolean isMatch(final Instant instant) {
        return isMatch(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    /**
     * Provide feedback if a given date matches the cron expression.
     *
     * @param instant - Instant instance. If null, a NullPointerException will be raised.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return true if date matches cron expression requirements, false otherwise.
     */
    default boolean isMatch(final Instant instant, final ZoneId zoneId) {
        return isMatch(ZonedDateTime.ofInstant(instant, zoneId));
    }

    /**
     * Provide feedback if a given date matches the cron expression.
     *
     * @param millis - milliseconds in long.
     * @return true if date matches cron expression requirements, false otherwise.
     */
    default boolean isMatch(final long millis) {
        return isMatch(Instant.ofEpochMilli(millis));
    }

    /**
     * Provide feedback if a given date matches the cron expression.
     *
     * @param millis - milliseconds in long.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return true if date matches cron expression requirements, false otherwise.
     */
    default boolean isMatch(final long millis, final ZoneId zoneId) {
        return isMatch(Instant.ofEpochMilli(millis), zoneId);
    }

    /**
     * Provide count of times cron expression would execute between given start and end dates
     *
     * @param startDate - Start date. If null, a NullPointerException will be raised.
     * @param endDate - End date. If null, a NullPointerException will be raised.
     * @return count of executions
     */
    default int countExecutions(ZonedDateTime startDate, ZonedDateTime endDate) {
        return getExecutionDates(startDate, endDate).size();
    }

    /**
     * Provide count of times cron expression would execute between given start and end dates
     *
     * @param startInstant - Start date. If null, a NullPointerException will be raised.
     * @param endInstant - End date. If null, a NullPointerException will be raised.
     * @return count of executions
     */
    default int countExecutions(Instant startInstant, Instant endInstant) {
        return countExecutions(ZonedDateTime.ofInstant(startInstant, ZoneId.systemDefault()), ZonedDateTime.ofInstant(endInstant, ZoneId.systemDefault()));
    }

    /**
     * Provide count of times cron expression would execute between given start and end dates
     *
     * @param startInstant - Start date. If null, a NullPointerException will be raised.
     * @param endInstant - End date. If null, a NullPointerException will be raised.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return count of executions
     */
    default int countExecutions(Instant startInstant, Instant endInstant, ZoneId zoneId) {
        return countExecutions(ZonedDateTime.ofInstant(startInstant, zoneId), ZonedDateTime.ofInstant(endInstant, zoneId));
    }

    /**
     * Provide count of times cron expression would execute between given start and end dates
     *
     * @param startMillis - Start date in milliseconds.
     * @param endMillis - End date in milliseconds.
     * @return count of executions
     */
    default int countExecutions(long startMillis, long endMillis) {
        return countExecutions(Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis));
    }

    /**
     * Provide count of times cron expression would execute between given start and end dates
     *
     * @param startMillis - Start date in milliseconds.
     * @param endMillis - End date in milliseconds.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return count of executions
     */
    default int countExecutions(long startMillis, long endMillis, ZoneId zoneId) {
        return countExecutions(Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis), zoneId);
    }

    /**
     * Provide date times when cron expression would execute between given start and end dates.
     * End date should be after start date. Otherwise, IllegalArgumentException is raised
     *
     * @param startDate - Start date. If null, a NullPointerException will be raised.
     * @param endDate - End date. If null, a NullPointerException will be raised.
     * @return list of date times
     */
    default List<ZonedDateTime> getExecutionDates(ZonedDateTime startDate, ZonedDateTime endDate) {
        if (endDate.equals(startDate) || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate should take place later in time than startDate");
        }
        List<ZonedDateTime> executions = new ArrayList<>();
        ZonedDateTime nextExecutionDate = nextExecution(startDate).orElse(null);

        if (nextExecutionDate == null) return Collections.emptyList();
        while(nextExecutionDate != null && (nextExecutionDate.isBefore(endDate) || nextExecutionDate.equals(endDate))){
            executions.add(nextExecutionDate);
            nextExecutionDate = nextExecution(nextExecutionDate).orElse(null);
        }
        return executions;
    }

    /**
     * Provide date times when cron expression would execute between given start and end dates.
     *
     * @param startInstant - Start date. If null, a NullPointerException will be raised.
     * @param endInstant - End date. If null, a NullPointerException will be raised.
     * @return list of Instants
     */
    default List<Instant> getExecutionDates(Instant startInstant, Instant endInstant) {
        return getExecutionDates(ZonedDateTime.ofInstant(startInstant, ZoneId.systemDefault()), ZonedDateTime.ofInstant(endInstant, ZoneId.systemDefault()))
                .stream().map(ZonedDateTime::toInstant).collect(Collectors.toList());
    }

    /**
     * Provide date times when cron expression would execute between given start and end dates.
     *
     * @param startInstant - Start date. If null, a NullPointerException will be raised.
     * @param endInstant - End date. If null, a NullPointerException will be raised.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return list of Instants
     */
    default List<Instant> getExecutionDates(Instant startInstant, Instant endInstant, ZoneId zoneId) {
        return getExecutionDates(ZonedDateTime.ofInstant(startInstant, zoneId), ZonedDateTime.ofInstant(endInstant, zoneId))
                .stream().map(ZonedDateTime::toInstant).collect(Collectors.toList());
    }

    /**
     * Provide date times when cron expression would execute between given start and end dates.
     *
     * @param startMillis - Start date in milliseconds.
     * @param endMillis - End date in milliseconds.
     * @return list of millisecond timestamps in Long
     */
    default List<Long> getExecutionDates(long startMillis, long endMillis) {
        return getExecutionDates(Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis))
                .stream().map(Instant::toEpochMilli).collect(Collectors.toList());
    }

    /**
     * Provide date times when cron expression would execute between given start and end dates.
     *
     * @param startMillis - Start date in milliseconds.
     * @param endMillis - End date in milliseconds.
     * @param zoneId - ZoneId instance. If null, a NullPointerException will be raised.
     * @return list of millisecond timestamps in Long
     */
    default List<Long> getExecutionDates(long startMillis, long endMillis, ZoneId zoneId) {
        return getExecutionDates(Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis), zoneId)
                .stream().map(Instant::toEpochMilli).collect(Collectors.toList());
    }
}
