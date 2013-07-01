/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.network;

import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * A test for the {@link DateParser} class.
 */
public class DateParserTest {

    private static final DateFormat DF;
    static {
        DF = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        DF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Tests a selection of date strings.
     */
    @Test
    public void testSelectedStrings() {
        test("Wed, 28-Sep-2011 09:00:00 GMT", "28-09-2011 09:00:00");
        test("Wed, 28 Sep 2011 17:00:00 GMT", "28-09-2011 17:00:00");
        test("Wed  Sep 28 2011 23:59:59 GMT", "28-09-2011 23:59:59");
        test("1-Jan-1970 00:00:00", "01-01-1970 00:00:00");
    }

    /**
     * Tests the day-of-month field handling.
     */
    @Test
    public void testDayOfMonthField() {
        // Some legal values
        test("28-Sep-2011 00:00:00", "28-09-2011 00:00:00");
        test("08-Sep-2011 00:00:00", "08-09-2011 00:00:00");
        test("8-Sep-2011 00:00:00", "08-09-2011 00:00:00");

        // Limits
        test("01-Sep-2011 00:00:00", "01-09-2011 00:00:00");

        try {
            DateParser.parse("00-Sep-2011 00:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        test("30-Sep-2011 00:00:00", "30-09-2011 00:00:00");

        try {
            DateParser.parse("31-Sep-2011 00:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        try {
            DateParser.parse("32-Sep-2011 00:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        // Leap year handling
        test("28-Feb-2011 00:00:00", "28-02-2011 00:00:00");

        try {
            DateParser.parse("29-Feb-2011 00:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        test("29-Feb-2012 00:00:00", "29-02-2012 00:00:00");

        try {
            DateParser.parse("30-Feb-2012 00:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        // Field is missing
        try {
            DateParser.parse("O1-Sep-2011 00:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        try {
            DateParser.parse("Sep-2011 00:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}
    }

    /**
     * Tests the month field handling.
     */
    @Test
    public void testMonthField() {
        // Some legal values
        test("01-Jan-2011 00:00:00", "01-01-2011 00:00:00");
        test("01-Feb-2011 00:00:00", "01-02-2011 00:00:00");
        test("01-Mar-2011 00:00:00", "01-03-2011 00:00:00");
        test("01-Apr-2011 00:00:00", "01-04-2011 00:00:00");
        test("01-May-2011 00:00:00", "01-05-2011 00:00:00");
        test("01-Jun-2011 00:00:00", "01-06-2011 00:00:00");
        test("01-Jul-2011 00:00:00", "01-07-2011 00:00:00");
        test("01-Aug-2011 00:00:00", "01-08-2011 00:00:00");
        test("01-Sep-2011 00:00:00", "01-09-2011 00:00:00");
        test("01-Oct-2011 00:00:00", "01-10-2011 00:00:00");
        test("01-Nov-2011 00:00:00", "01-11-2011 00:00:00");
        test("01-Dec-2011 00:00:00", "01-12-2011 00:00:00");
        test("01-jan-2011 00:00:00", "01-01-2011 00:00:00");

        // Field is missing
        try {
            DateParser.parse("28-Seq-2011 09:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        try {
            DateParser.parse("28-2011 09:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}
    }

    /**
     * Tests the year field handling.
     */
    @Test
    public void testYearField() {
        // Some legal values
        test("28-Sep-2011 09:00:00", "28-09-2011 09:00:00");
        test("01-Jan-1970 00:00:00", "01-01-1970 00:00:00");
        test("31-Dec-1969 23:59:59", "31-12-1969 23:59:59");

        // Limits
        test("1-Jan-1601 00:00:00", "01-01-1601 00:00:00");

        try {
            DateParser.parse("31-Dec-1600 23:59:59");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        test("31-Dec-9999 23:59:59", "31-12-9999 23:59:59");

        // Two digit year handling
        test("01-Jan-00 00:00:00", "01-01-2000 00:00:00");
        test("01-Jan-01 00:00:00", "01-01-2001 00:00:00");
        test("01-Jan-69 00:00:00", "01-01-2069 00:00:00");
        test("01-Jan-70 00:00:00", "01-01-1970 00:00:00");
        test("01-Jan-99 00:00:00", "01-01-1999 00:00:00");

        // Field is missing
        try {
            DateParser.parse("01-Sep-2O11 00:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        try {
            DateParser.parse("01-Sep- 00:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}
    }

    /**
     * Tests the hour field handling.
     */
    @Test
    public void testHourField() {
        // Some legal values
        test("01-Jan-2011 09:00:00", "01-01-2011 09:00:00");
        test("01-Jan-2011 9:00:00", "01-01-2011 09:00:00");
        test("01-Jan-2011 17:00:00", "01-01-2011 17:00:00");
        test("01-Jan-2011 0:00:00", "01-01-2011 00:00:00");

        // Limits
        test("01-Jan-2011 00:00:00", "01-01-2011 00:00:00");
        test("01-Jan-2011 23:00:00", "01-01-2011 23:00:00");

        try {
            DateParser.parse("01-Jan-2011 24:00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        // Field is missing
        try {
            DateParser.parse("01-Jan-2011 :00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        try {
            DateParser.parse("01-Jan-2011 :00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}
    }

    /**
     * Tests the minute field handling.
     */
    @Test
    public void testMinuteField() {
        // Some legal values
        test("01-Jan-2011 00:01:00", "01-01-2011 00:01:00");
        test("01-Jan-2011 00:30:00", "01-01-2011 00:30:00");
        test("01-Jan-2011 00:1:00", "01-01-2011 00:01:00");
        test("01-Jan-2011 00:0:00", "01-01-2011 00:00:00");

        // Limits
        test("01-Jan-2011 00:00:00", "01-01-2011 00:00:00");
        test("01-Jan-2011 00:59:00", "01-01-2011 00:59:00");

        try {
            DateParser.parse("01-Jan-2011 00:60:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        // Field is missing
        try {
            DateParser.parse("01-Jan-2011 00::00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        try {
            DateParser.parse("01-Jan-2011 00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}
    }

    /**
     * Tests the second field handling.
     */
    @Test
    public void testSecondField() {
        // Some legal values
        test("01-Jan-2011 00:00:01", "01-01-2011 00:00:01");
        test("01-Jan-2011 00:00:30", "01-01-2011 00:00:30");
        test("01-Jan-2011 00:00:1", "01-01-2011 00:00:01");
        test("01-Jan-2011 00:00:0", "01-01-2011 00:00:00");

        // Limits
        test("01-Jan-2011 00:00:00", "01-01-2011 00:00:00");
        test("01-Jan-2011 00:00:59", "01-01-2011 00:00:59");

        try {
            DateParser.parse("01-Jan-2011 00:00:60");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        // Field is missing
        try {
            DateParser.parse("01-Jan-2011 00:00:");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}

        try {
            DateParser.parse("01-Jan-2011 00:00");
            fail("ParseException expected but not thrown");
        } catch (ParseException expected) {}
    }

    /**
     * Tests delimiter handling.
     */
    @Test
    public void testDelimiters() {
        test("  28  Oct  2011  12:34:56  ", "28-10-2011 12:34:56");
        test("   28   Oct   2011   12:34:56   ", "28-10-2011 12:34:56");
        test("\t28\tOct\t2011\t12:34:56  ", "28-10-2011 12:34:56");
        test("--28-Oct-2011-12:34:56--", "28-10-2011 12:34:56");
    }


    /**
     * Tests parsing of a given date string.
     */
    private static void test(String dateString, String expectedResult) {
        try {
            long actualResult = DateParser.parse(dateString);
            assertEquals("Unexpected result, date string: [" + dateString
                    + "],", expectedResult, DF.format(new Date(actualResult)));
        } catch (ParseException ex) {
            throw new AssertionError(ex);
        }
    }
}
