package io.github.projectunified.cronutils;

import io.github.projectunified.cronutils.builder.CronBuilder;
import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.definition.CronConstraintsFactory;
import io.github.projectunified.cronutils.model.definition.CronDefinition;
import io.github.projectunified.cronutils.model.definition.CronDefinitionBuilder;
import io.github.projectunified.cronutils.model.time.ExecutionTime;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Optional;

import static io.github.projectunified.cronutils.model.field.expression.FieldExpression.always;
import static io.github.projectunified.cronutils.model.field.expression.FieldExpression.questionMark;
import static io.github.projectunified.cronutils.model.field.expression.FieldExpressionFactory.every;
import static io.github.projectunified.cronutils.model.field.expression.FieldExpressionFactory.on;
import static org.junit.jupiter.api.Assertions.*;


public class Issue446Test {
    private static final CronDefinition definition = CronDefinitionBuilder.defineCron()
            .withMinutes().and()
            .withHours().and()
            .withDayOfWeek().supportsQuestionMark().and()
            .withDayOfMonth().supportsL().supportsQuestionMark().and()
            .withDayOfYear().supportsQuestionMark().and()
            .withMonth().and()
            .matchDayOfWeekAndDayOfMonth()
            .withCronValidation(CronConstraintsFactory.ensureEitherDayOfWeekOrDayOfMonth())
            .withCronValidation(CronConstraintsFactory.ensureEitherDayOfYearOrMonth())
            .instance();

    @Test
    public void testWrongIntervalsForEvery6Months() {
        LocalDateTime dayOfApril = LocalDateTime.of(2020, 4, 25, 0, 0);
        Clock clock = Clock.fixed(dayOfApril.toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
        ZonedDateTime dayOfAprilInLocalTimezone = ZonedDateTime.now(clock);
        Cron cron = getEveryMonthFromNow(dayOfAprilInLocalTimezone, 6).instance();

        ZonedDateTime nextRun = nextRun(cron, dayOfAprilInLocalTimezone); // first run
        assertEquals(2020, nextRun.getYear());
        assertEquals(10, nextRun.getMonthValue());

        nextRun = nextRun(cron, nextRun); // second
        System.out.println(nextRun);
        assertEquals(2021, nextRun.getYear());
        assertEquals(4, nextRun.getMonthValue());
    }

    public static CronBuilder getEveryMonthFromNow(ZonedDateTime now, int every) {
        return CronBuilder.cron(definition)
                .withMinute(on(now.getMinute()))
                .withHour(on(now.getHour()))
                .withDoW(questionMark())
                .withDoM(on(now.getDayOfMonth()))
                .withDoY(questionMark())
                .withMonth(every(on(now.getMonthValue()), every));
    }


    @Test
    public void testDaylightSavingOverlapMinuteNextRun() {


        LocalDateTime daylightSaving2020 = LocalDateTime.of(2020, 10, 25, 1, 10);
        Clock clock = Clock.fixed(daylightSaving2020.toInstant(ZoneOffset.ofHours(2)),ZoneId.of("Europe/Rome"));
        ZonedDateTime daylightSaving2020InLocalTimezone = ZonedDateTime.now(clock);
        Cron cron = getEvery30Minute(daylightSaving2020InLocalTimezone).instance();

        ZonedDateTime nextRun = nextRun(cron, daylightSaving2020InLocalTimezone); // first run
        assertEquals(40, nextRun.getMinute());
        assertEquals(1, nextRun.getHour());
        nextRun = nextRun(cron, nextRun); // second
        assertEquals(10, nextRun.getMinute());
        assertEquals(2, nextRun.getHour());
        assertEquals(ZoneOffset.ofHours(2), nextRun.getOffset());
        nextRun = nextRun(cron, nextRun); // second
        assertEquals(40, nextRun.getMinute());
        assertEquals(2, nextRun.getHour());
        assertEquals(ZoneOffset.ofHours(2), nextRun.getOffset());
        nextRun = nextRun(cron, nextRun); // second
        assertEquals(10, nextRun.getMinute());
        assertEquals(2, nextRun.getHour());
        assertEquals(ZoneOffset.ofHours(1), nextRun.getOffset());
        nextRun = nextRun(cron, nextRun); // second
        assertEquals(40, nextRun.getMinute());
        assertEquals(2, nextRun.getHour());
        assertEquals(ZoneOffset.ofHours(1), nextRun.getOffset());
        nextRun = nextRun(cron, nextRun); // second
        assertEquals(10, nextRun.getMinute());
        assertEquals(3, nextRun.getHour());
        nextRun = nextRun(cron, nextRun); // second
        assertEquals(40, nextRun.getMinute());
        assertEquals(3, nextRun.getHour());

    }

    @Test
    public void testDaylightSavingOverlapHourNextRun() {
        LocalDateTime startDay = LocalDateTime.of(2020, 10, 24, 1, 0); // Day before Daylight saving
        Clock clock = Clock.fixed(startDay.toInstant(ZoneOffset.ofHours(2)), ZoneId.of("Europe/Rome"));
        ZonedDateTime daylightSaving2020InLocalTimezone = ZonedDateTime.now(clock);
        Cron cron = getEveryHour(daylightSaving2020InLocalTimezone).instance();

        ZonedDateTime nextRun = nextRun(cron, daylightSaving2020InLocalTimezone);
        assertEquals(0, nextRun.getMinute());
        assertEquals(2, nextRun.getHour());
        assertEquals(ZoneOffset.ofHours(2), nextRun.getOffset());
        nextRun = nextRun(cron, nextRun); // second
        assertEquals(0, nextRun.getMinute());
        assertEquals(3, nextRun.getHour());
        assertEquals(ZoneOffset.ofHours(2), nextRun.getOffset());

        startDay = LocalDateTime.of(2020, 10, 25, 1, 0); // Daylight saving
        clock = Clock.fixed(startDay.toInstant(ZoneOffset.ofHours(2)), ZoneId.of("Europe/Rome"));
        daylightSaving2020InLocalTimezone = ZonedDateTime.now(clock);
        cron = getEveryHour(daylightSaving2020InLocalTimezone).instance();

        nextRun = nextRun(cron, daylightSaving2020InLocalTimezone); // first run
        assertEquals(0, nextRun.getMinute());
        assertEquals(2, nextRun.getHour());
        assertEquals(ZoneOffset.ofHours(2), nextRun.getOffset());
        nextRun = nextRun(cron, nextRun); // second
        assertEquals(0, nextRun.getMinute());
        assertEquals(2, nextRun.getHour());
        assertEquals(ZoneOffset.ofHours(1), nextRun.getOffset());
        nextRun = nextRun(cron, nextRun); // second
        assertEquals(0, nextRun.getMinute());
        assertEquals(3, nextRun.getHour());
        assertEquals(ZoneOffset.ofHours(1), nextRun.getOffset());

    }


    public static CronBuilder getEveryHour(ZonedDateTime now) {
        return CronBuilder.cron(definition)
                .withMinute(on(now.getMinute()))
                .withHour(every(on(now.getHour()),1))
                .withDoW(questionMark())
                .withDoM(on(now.getDayOfMonth()))
                .withDoY(questionMark())
                .withMonth(on(now.getMonthValue()));
    }

    public static CronBuilder getEvery30Minute(ZonedDateTime now) {
        return CronBuilder.cron(definition)
                .withMinute(every(on(now.getMinute()),30))
                .withHour(always())
                .withDoW(questionMark())
                .withDoM(on(now.getDayOfMonth()))
                .withDoY(questionMark())
                .withMonth(on(now.getMonthValue()));
    }


    private static ZonedDateTime nextRun(Cron cron, ZonedDateTime when) {
        final Optional<ZonedDateTime> next = ExecutionTime.forCron(cron).nextExecution(when);
        if (!next.isPresent()) {
            fail();
        }
        return next.get();
    }
}
