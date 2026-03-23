package io.github.projectunified.cronutils.utils;

import io.github.projectunified.cronutils.model.Cron;
import io.github.projectunified.cronutils.model.time.ExecutionTime;

import java.time.ZonedDateTime;
import java.util.Comparator;

public class CronFrequencyComparator implements Comparator<Cron> {
    private final ZonedDateTime startDate;
    private final ZonedDateTime endDate;

    public CronFrequencyComparator(ZonedDateTime startDate, ZonedDateTime endDate){
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public int compare(Cron o1, Cron o2) {
        int executions1 = ExecutionTime.forCron(o1).countExecutions(startDate, endDate);
        int executions2 = ExecutionTime.forCron(o2).countExecutions(startDate, endDate);
        return executions1-executions2;
    }
}
