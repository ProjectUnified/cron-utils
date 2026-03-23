/*
 * Copyright 2019 fahmpeermoh
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

package com.cronutils.converter;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

import com.cronutils.utils.StringUtils;

public class CronConverter {

	private static final String CRON_FIELDS_SEPARATOR = " ";
	private String[] cronParts;
	private Calendar fromCalendar;

	private CronToCalendarTransformer toCalendarConverter;
	private CalendarToCronTransformer toCronConverter;

	public CronConverter(CronToCalendarTransformer toCalendarConverter, CalendarToCronTransformer toCronConverter){
		this.toCalendarConverter = toCalendarConverter;
		this.toCronConverter = toCronConverter;
	}

	public CronConverter using(String cronExpression) {
		cronParts = cronExpression.split(CRON_FIELDS_SEPARATOR);
		return this;
	}

	public CronConverter from(ZoneId zoneId) {
		fromCalendar = getCalendar(zoneId);
		toCalendarConverter.apply(cronParts, fromCalendar);
		return this;
	}

	public CronConverter to(ZoneId zoneId) {
		Calendar toCalendar = getCalendar(zoneId);
		toCalendar.setTimeInMillis(fromCalendar.getTimeInMillis());
		toCronConverter.apply(cronParts, toCalendar);
		return this;
	}

	public String convert() {
		return StringUtils.join(cronParts, CRON_FIELDS_SEPARATOR);
	}

	private Calendar getCalendar(ZoneId id) {
		return Calendar.getInstance(TimeZone.getTimeZone(id));
	}
}
