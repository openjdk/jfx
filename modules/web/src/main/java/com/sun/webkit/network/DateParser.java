/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.network;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An RFC 6265-compliant date parser.
 */
final class DateParser {

    private static final Logger logger =
            Logger.getLogger(DateParser.class.getName());

    private static final Pattern DELIMITER_PATTERN = Pattern.compile(
            "[\\x09\\x20-\\x2F\\x3B-\\x40\\x5B-\\x60\\x7B-\\x7E]+");
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(\\d{1,2}):(\\d{1,2}):(\\d{1,2})(?:[^\\d].*)*");
    private static final Pattern DAY_OF_MONTH_PATTERN = Pattern.compile(
            "(\\d{1,2})(?:[^\\d].*)*");
    private static final Pattern YEAR_PATTERN = Pattern.compile(
            "(\\d{2,4})(?:[^\\d].*)*");
    private static final Map<String,Integer> MONTH_MAP;
    static {
        Map<String,Integer> map = new HashMap<String,Integer>(12);
        map.put("jan", 0);
        map.put("feb", 1);
        map.put("mar", 2);
        map.put("apr", 3);
        map.put("may", 4);
        map.put("jun", 5);
        map.put("jul", 6);
        map.put("aug", 7);
        map.put("sep", 8);
        map.put("oct", 9);
        map.put("nov", 10);
        map.put("dec", 11);
        MONTH_MAP = Collections.unmodifiableMap(map);
    }


    /**
     * The private default constructor. Ensures non-instantiability.
     */
    private DateParser() {
        throw new AssertionError();
    }


    /**
     * Parses a given date string as required by RFC 6265.
     * @param date the string to parse
     * @return the difference, measured in milliseconds, between the parsed
     *         date and midnight, January 1, 1970 UTC
     * @throws ParseException if {@code date} cannot be parsed
     */
    static long parse(String date) throws ParseException {
        logger.log(Level.FINEST, "date: [{0}]", date);

        Time time = null;
        Integer dayOfMonth = null;
        Integer month = null;
        Integer year = null;
        String[] tokens = DELIMITER_PATTERN.split(date, 0);
        for (String token : tokens) {
            if (token.length() == 0) {
                continue;
            }

            Time timeTmp;
            if (time == null && (timeTmp = parseTime(token)) != null) {
                time = timeTmp;
                continue;
            }

            Integer dayOfMonthTmp;
            if (dayOfMonth == null
                    && (dayOfMonthTmp = parseDayOfMonth(token)) != null)
            {
                dayOfMonth = dayOfMonthTmp;
                continue;
            }

            Integer monthTmp;
            if (month == null && (monthTmp = parseMonth(token)) != null) {
                month = monthTmp;
                continue;
            }

            Integer yearTmp;
            if (year == null && (yearTmp = parseYear(token)) != null) {
                year = yearTmp;
                continue;
            }
        }

        if (year != null) {
            if (year >= 70 && year <= 99) {
                year += 1900;
            } else if (year >= 0 && year <= 69) {
                year += 2000;
            }
        }

        if (time == null || dayOfMonth == null || month == null || year == null
                || dayOfMonth < 1 || dayOfMonth > 31
                || year < 1601
                || time.hour > 23
                || time.minute > 59
                || time.second > 59)
        {
            throw new ParseException("Error parsing date", 0);
        }

        Calendar calendar = Calendar.getInstance(
                TimeZone.getTimeZone("UTC"), Locale.US);
        calendar.setLenient(false);
        calendar.clear();
        calendar.set(year, month, dayOfMonth,
                time.hour, time.minute, time.second);

        try {
            long result = calendar.getTimeInMillis();
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "result: [{0}]",
                        new Date(result).toString());
            }
            return result;
        } catch (Exception ex) {
            ParseException pe = new ParseException("Error parsing date", 0);
            pe.initCause(ex);
            throw pe;
        }
    }

    /**
     * Parses a token as a time string.
     */
    private static Time parseTime(String token) {
        Matcher matcher = TIME_PATTERN.matcher(token);
        if (matcher.matches()) {
            return new Time(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        } else {
            return null;
        }
    }

    /**
     * Container for parsed time.
     */
    private static final class Time {
        private final int hour;
        private final int minute;
        private final int second;

        private Time(int hour, int minute, int second) {
            this.hour = hour;
            this.minute = minute;
            this.second = second;
        }
    }

    /**
     * Parses a token as a day of month.
     */
    private static Integer parseDayOfMonth(String token) {
        Matcher matcher = DAY_OF_MONTH_PATTERN.matcher(token);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            return null;
        }
    }

    /**
     * Parses a token as a month.
     */
    private static Integer parseMonth(String token) {
        if (token.length() >= 3) {
            return MONTH_MAP.get(token.substring(0, 3).toLowerCase());
        } else {
            return null;
        }
    }

    /**
     * Parses a token as a year.
     */
    private static Integer parseYear(String token) {
        Matcher matcher = YEAR_PATTERN.matcher(token);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            return null;
        }
    }
}
