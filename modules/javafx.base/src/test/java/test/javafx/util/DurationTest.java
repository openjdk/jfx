/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.javafx.util;

import javafx.util.Duration;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 */
public class DurationTest {

    /************************************************************************************
     *
     * Tests for the static millis() method
     *
     ***********************************************************************************/

    @Test public void millis_withZeroResultsIn_ZERO() {
        assertSame(Duration.ZERO, Duration.millis(0));
    }

    @Test public void millis_withOneResultsIn_ONE() {
        assertSame(Duration.ONE, Duration.millis(1));
    }

    @Test public void millis_withPositiveInfinityResultsIn_INDEFINITE() {
        assertSame(Duration.INDEFINITE, Duration.millis(Double.POSITIVE_INFINITY));
    }

    @Test public void millis_withNaNResultsIn_UNKNOWN() {
        assertSame(Duration.UNKNOWN, Duration.millis(Double.NaN));
    }

    @Test public void millis_withPositiveResultsInNewDuration() {
        final Duration result = Duration.millis(9);
        assertEquals(result.toMillis(), 9, 0);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    /************************************************************************************
     *
     * Tests for the static seconds() method
     *
     ***********************************************************************************/

    @Test public void seconds_withZeroResultsIn_ZERO() {
        assertSame(Duration.ZERO, Duration.seconds(0));
    }

    @Test public void seconds_withPositiveInfinityResultsIn_INDEFINITE() {
        assertSame(Duration.INDEFINITE, Duration.seconds(Double.POSITIVE_INFINITY));
    }

    @Test public void seconds_withNaNResultsIn_UNKNOWN() {
        assertSame(Duration.UNKNOWN, Duration.seconds(Double.NaN));
    }

    @Test public void seconds_withPositiveResultsInNewDuration() {
        final Duration result = Duration.seconds(9);
        assertEquals(result.toSeconds(), 9, 0);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    /************************************************************************************
     *
     * Tests for the static minutes() method
     *
     ***********************************************************************************/

    @Test public void minutes_withZeroResultsIn_ZERO() {
        assertSame(Duration.ZERO, Duration.minutes(0));
    }

    @Test public void minutes_withPositiveInfinityResultsIn_INDEFINITE() {
        assertSame(Duration.INDEFINITE, Duration.minutes(Double.POSITIVE_INFINITY));
    }

    @Test public void minutes_withNaNResultsIn_UNKNOWN() {
        assertSame(Duration.UNKNOWN, Duration.minutes(Double.NaN));
    }

    @Test public void minutes_withPositiveResultsInNewDuration() {
        final Duration result = Duration.minutes(9);
        assertEquals(result.toMinutes(), 9, 0);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    /************************************************************************************
     *
     * Tests for the static hours() method
     *
     ***********************************************************************************/

    @Test public void hours_withZeroResultsIn_ZERO() {
        assertSame(Duration.ZERO, Duration.hours(0));
    }

    @Test public void hours_withPositiveInfinityResultsIn_INDEFINITE() {
        assertSame(Duration.INDEFINITE, Duration.hours(Double.POSITIVE_INFINITY));
    }

    @Test public void hours_withNaNResultsIn_UNKNOWN() {
        assertSame(Duration.UNKNOWN, Duration.hours(Double.NaN));
    }

    @Test public void hours_withPositiveResultsInNewDuration() {
        final Duration result = Duration.hours(9);
        assertEquals(result.toHours(), 9, 0);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    /************************************************************************************
     *
     * Tests for the one-arg constructor
     *
     ***********************************************************************************/

    @Test public void constructor_withZeroEquals_ZERO() {
        assertEquals(Duration.ZERO, new Duration(0));
    }

    @Test public void constructor_withOneEquals_ONE() {
        assertEquals(Duration.ONE, new Duration(1));
    }

    @Test public void constructor_withPositiveInfinityEquals_INDEFINITE() {
        assertTrue(Duration.INDEFINITE.equals(new Duration(Double.POSITIVE_INFINITY)));
    }

    @Test public void constructor_withNaNDoesNotEqual_UNKNOWN() {
        assertFalse(Duration.UNKNOWN.equals(new Duration(Double.NaN)));
    }

    @Test public void constructor_withPositiveResultsInNewDuration() {
        final Duration result = new Duration(9);
        assertEquals(result.toMillis(), 9, 0);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    /************************************************************************************
     *
     * Tests for the toMillis method
     *
     ***********************************************************************************/

    @Test public void toMillis_when_ZERO_resultsInZero() {
        assertEquals(0, Duration.ZERO.toMillis(), 0);
    }

    @Test public void toMillis_when_ONE_resultsInOne() {
        assertEquals(1, Duration.ONE.toMillis(), 0);
    }

    @Test public void toMillis_when_INDEFINITE_resultsInPositiveInfinity() {
        assertTrue(Double.isInfinite(Duration.INDEFINITE.toMillis()));
    }

    @Test public void toMillis_whenNegativeInfinityResultsInNegativeInfinity() {
        assertTrue(Double.isInfinite(new Duration(Double.NEGATIVE_INFINITY).toMillis()));
    }

    @Test public void toMillis_when_UNKNOWN_ResultsInNaN() {
        assertTrue(Double.isNaN(Duration.UNKNOWN.toMillis()));
    }

    @Test public void toMillis_whenPositiveNumberResultsInTheSameNumber() {
        assertEquals(87, new Duration(87).toMillis(), 0);
    }

    @Test public void toMillis_whenNegativeNumberResultsInTheSameNumber() {
        assertEquals(-7, new Duration(-7).toMillis(), 0);
    }

    /************************************************************************************
     *
     * Tests for the toSeconds method
     *
     ***********************************************************************************/

    @Test public void toSeconds_when_ZERO_resultsInZero() {
        assertEquals(0, Duration.ZERO.toSeconds(), 0);
    }

    @Test public void toSeconds_when_ONE_resultsInPointZeroZeroOne() {
        assertEquals(.001, Duration.ONE.toSeconds(), 0);
    }

    @Test public void toSeconds_when_INDEFINITE_resultsInPositiveInfinity() {
        assertTrue(Double.isInfinite(Duration.INDEFINITE.toSeconds()));
    }

    @Test public void toSeconds_whenNegativeInfinityResultsInNegativeInfinity() {
        assertTrue(Double.isInfinite(new Duration(Double.NEGATIVE_INFINITY).toSeconds()));
    }

    @Test public void toSeconds_when_UNKNOWN_ResultsInNaN() {
        assertTrue(Double.isNaN(Duration.UNKNOWN.toSeconds()));
    }

    @Test public void toSeconds_whenPositiveNumberResultsInTheSameNumber() {
        assertEquals(.087, new Duration(87).toSeconds(), 0);
    }

    @Test public void toSeconds_whenNegativeNumberResultsInTheSameNumber() {
        assertEquals(-.007, new Duration(-7).toSeconds(), 0);
    }

    // toMinutes
        // 0, +infinity, -infinity, Nan, positive value, negative value
    // toHours
        // 0, +infinity, -infinity, Nan, positive value, negative value


    /************************************************************************************
     *
     * Tests for adding two Durations. For the most part I just test millis,
     * because I know the implementation stores just millis, but for completeness
     * and to avoid regressions should we change the storage format, I'll also
     * throw in a few tests which work on seconds, minutes, and a mixture of
     * seconds and minutes.
     *
     ***********************************************************************************/

    @Test public void add_doesNotModifyTheInputArguments() {
        Duration a = new Duration(10);
        Duration b = new Duration(20);
        final Duration result = a.add(b);
        assertEquals(new Duration(30), result);
        assertEquals(10, a.toMillis(), 0);
        assertEquals(20, b.toMillis(), 0);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    @Test public void add_ZERO_and_ZERO_ResultsIn_ZERO() {
        assertSame(Duration.ZERO, Duration.ZERO.add(Duration.ZERO));
    }

    @Test public void add_ZERO_and_ONE_ResultsIn_ONE() {
        assertSame(Duration.ONE, Duration.ZERO.add(Duration.ONE));
        assertSame(Duration.ONE, Duration.ONE.add(Duration.ZERO));
    }

    @Test public void add_ONE_and_ONE_ResultsInTwo() {
        final Duration result = Duration.ONE.add(Duration.ONE);
        assertEquals(new Duration(2), result);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    @Test public void add_ONE_Second_and_ONE_Milli_ResultsInOneThousandOneMillis() {
        final Duration result = Duration.seconds(1).add(Duration.ONE);
        assertEquals(new Duration(1001), result);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    @Test public void add_ZERO_and_INDEFINITE_ResultsInIndefinite() {
        //assertTrue(0.0 + Double.POSITIVE_INFINITY == Double.POSITIVE_INFINITY); // sanity check
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(0.0 + Double.POSITIVE_INFINITY)); // sanity check

        assertTrue(Duration.ZERO.add(Duration.INDEFINITE).isIndefinite());
        assertFalse(Duration.ZERO.add(Duration.INDEFINITE).isUnknown());
    }

    @Test public void add_ONE_and_INDEFINITE_ResultsInIndefinite() {
        //assertTrue(1.0 + Double.POSITIVE_INFINITY == Double.POSITIVE_INFINITY); // sanity check
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(1.0 + Double.POSITIVE_INFINITY)); // sanity check

        assertTrue(Duration.ONE.add(Duration.INDEFINITE).isIndefinite());
        assertFalse(Duration.ONE.add(Duration.INDEFINITE).isUnknown());
    }

    @Test public void add_INDEFINITE_and_INDEFINITE_ResultsInIndefinite() {
        //assertTrue(Double.POSITIVE_INFINITY + Double.POSITIVE_INFINITY == Double.POSITIVE_INFINITY); // sanity check
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(Double.POSITIVE_INFINITY + Double.POSITIVE_INFINITY)); // sanity check

        assertTrue(Duration.INDEFINITE.add(Duration.INDEFINITE).isIndefinite());
        assertFalse(Duration.INDEFINITE.add(Duration.INDEFINITE).isUnknown());
    }

    @Test public void add_UNKNOWN_and_INDEFINITE_ResultsInUnknown() {
        assertTrue(Double.isNaN(Double.NaN + Double.POSITIVE_INFINITY)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(Double.NaN + Double.POSITIVE_INFINITY)); // sanity check

        assertFalse(Duration.UNKNOWN.add(Duration.INDEFINITE).isIndefinite());
        assertTrue(Duration.UNKNOWN.add(Duration.INDEFINITE).isUnknown());
    }

    @Test public void add_ZERO_and_UNKNOWN_ResultsInUnknown() {
        assertTrue(Double.isNaN(0.0 + Double.NaN)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(0.0 + Double.NaN)); // sanity check

        assertFalse(Duration.ZERO.add(Duration.UNKNOWN).isIndefinite());
        assertTrue(Duration.ZERO.add(Duration.UNKNOWN).isUnknown());
    }

    @Test public void add_ONE_and_UNKNOWN_ResultsInUnknown() {
        assertTrue(Double.isNaN(1.0 + Double.NaN)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(1.0 + Double.NaN)); // sanity check

        assertFalse(Duration.ONE.add(Duration.UNKNOWN).isIndefinite());
        assertTrue(Duration.ONE.add(Duration.UNKNOWN).isUnknown());
    }

    @Test public void testAddUsingMixedUnits() {
        Duration a = Duration.seconds(30);
        Duration b = Duration.minutes(10);
        Duration result = a.add(b);
        Duration expected = Duration.millis((1000 * 30) + (10 * 60 * 1000));
        assertEquals(expected, result);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    /************************************************************************************
     *
     * Tests for subtracting two Durations
     *
     ***********************************************************************************/

    @Test public void subtract_doesNotModifyTheInputArguments() {
        Duration a = new Duration(30);
        Duration b = new Duration(20);
        Duration result = a.subtract(b);
        assertEquals(new Duration(10), result);
        assertEquals(30, a.toMillis(), 0);
        assertEquals(20, b.toMillis(), 0);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    @Test public void subtract_ZERO_and_ZERO_ResultsIn_ZERO() {
        assertSame(Duration.ZERO, Duration.ZERO.subtract(Duration.ZERO));
    }

    @Test public void subtract_ZERO_and_ONE_ResultsIn_NegativeOne() {
        assertEquals(new Duration(-1), Duration.ZERO.subtract(Duration.ONE));
    }

    @Test public void subtract_ONE_and_ZERO_ResultsIn_ONE() {
        assertSame(Duration.ONE, Duration.ONE.subtract(Duration.ZERO));
    }

    @Test public void subtract_ONE_and_ONE_ResultsInZERO() {
        assertEquals(Duration.ZERO, Duration.ONE.subtract(Duration.ONE));
    }

    @Test public void subtract_ONE_Second_and_ONE_Milli_ResultsInNineHundredNinetyNineMillis() {
        assertEquals(new Duration(999), Duration.seconds(1).subtract(Duration.ONE));
    }

    @Test public void subtract_ZERO_and_INDEFINITE_ResultsInNegativeInfinity() {
        //assertTrue(0.0 - Double.POSITIVE_INFINITY == Double.NEGATIVE_INFINITY); // sanity check
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), Double.valueOf(0.0 - Double.POSITIVE_INFINITY)); // sanity check

        final Duration result = Duration.ZERO.subtract(Duration.INDEFINITE);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
        assertEquals(new Duration(Double.NEGATIVE_INFINITY), result);
    }

    @Test public void subtract_ONE_and_INDEFINITE_ResultsInNegativeInfinity() {
        //assertTrue(1.0 - Double.POSITIVE_INFINITY == Double.NEGATIVE_INFINITY); // sanity check
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), Double.valueOf(1.0 - Double.POSITIVE_INFINITY)); // sanity check

        final Duration result = Duration.ONE.subtract(Duration.INDEFINITE);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
        assertEquals(new Duration(Double.NEGATIVE_INFINITY), result);
    }

    @Test public void subtract_INDEFINITE_and_INDEFINITE_ResultsInUnknown() {
        assertTrue(Double.isNaN(Double.POSITIVE_INFINITY - Double.POSITIVE_INFINITY)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(Double.POSITIVE_INFINITY - Double.POSITIVE_INFINITY)); // sanity check

        assertFalse(Duration.INDEFINITE.subtract(Duration.INDEFINITE).isIndefinite());
        assertTrue(Duration.INDEFINITE.subtract(Duration.INDEFINITE).isUnknown());
    }

    @Test public void subtract_UNKNOWN_and_INDEFINITE_ResultsInUnknown() {
        assertTrue(Double.isNaN(Double.NaN - Double.POSITIVE_INFINITY)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(Double.NaN - Double.POSITIVE_INFINITY)); // sanity check

        assertFalse(Duration.UNKNOWN.subtract(Duration.INDEFINITE).isIndefinite());
        assertTrue(Duration.UNKNOWN.subtract(Duration.INDEFINITE).isUnknown());
    }

    @Test public void subtract_ZERO_and_UNKNOWN_ResultsInUnknown() {
        assertTrue(Double.isNaN(0 - Double.NaN)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(0.0 - Double.NaN)); // sanity check

        assertFalse(Duration.ZERO.subtract(Duration.UNKNOWN).isIndefinite());
        assertTrue(Duration.ZERO.subtract(Duration.UNKNOWN).isUnknown());
    }

    @Test public void subtract_ONE_and_UNKNOWN_ResultsInUnknown() {
        assertTrue(Double.isNaN(1.0 - Double.NaN)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(1.0 - Double.NaN)); // sanity check

        assertFalse(Duration.ONE.subtract(Duration.UNKNOWN).isIndefinite());
        assertTrue(Duration.ONE.subtract(Duration.UNKNOWN).isUnknown());
    }

    @Test public void testSubtractUsingMixedUnits() {
        Duration a = Duration.minutes(10);
        Duration b = Duration.seconds(30);
        Duration expected = Duration.millis((1000 * 30) + (9 * 60 * 1000));
        assertEquals(expected, a.subtract(b));
    }

    /************************************************************************************
     *
     * Tests for multiplying two Durations
     *
     ***********************************************************************************/

    @Test public void multiply_doesNotModifyTheInputArguments() {
        Duration a = new Duration(3);
        Duration result = a.multiply(2);
        assertEquals(new Duration(6), result);
        assertEquals(3, a.toMillis(), 0);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    @Test public void multiply_ZERO_and_ZERO_ResultsIn_ZERO() {
        assertSame(Duration.ZERO, Duration.ZERO.multiply(0));
    }

    @Test public void multiply_ZERO_and_ONE_ResultsIn_ZERO() {
        assertSame(Duration.ZERO, Duration.ZERO.multiply(1));
        assertSame(Duration.ZERO, Duration.ONE.multiply(0));
    }

    @Test public void multiply_ONE_and_ONE_ResultsIn_ONE() {
        assertEquals(Duration.ONE, Duration.ONE.multiply(1));
    }

    @Test public void multiply_ONE_Second_and_ONE_Milli_ResultsInOneSecond() {
        assertEquals(new Duration(1000), Duration.seconds(1).multiply(1));
    }

    @Test public void multiply_ZERO_and_INDEFINITE_ResultsInUnknown() {
        assertTrue(Double.isNaN(0.0 * Double.POSITIVE_INFINITY)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(0.0 * Double.POSITIVE_INFINITY)); // sanity check

        assertFalse(Duration.ZERO.multiply(Double.POSITIVE_INFINITY).isIndefinite());
        assertTrue(Duration.ZERO.multiply(Double.POSITIVE_INFINITY).isUnknown());
    }

    @Test public void multiply_ONE_and_INDEFINITE_ResultsInIndefinite() {
        //assertTrue(1.0 * Double.POSITIVE_INFINITY == Double.POSITIVE_INFINITY); // sanity check
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(1.0 * Double.POSITIVE_INFINITY)); // sanity check

        assertTrue(Duration.ONE.multiply(Double.POSITIVE_INFINITY).isIndefinite());
        assertFalse(Duration.ONE.multiply(Double.POSITIVE_INFINITY).isUnknown());
    }

    @Test public void multiply_INDEFINITE_and_INDEFINITE_ResultsInIndefinite() {
        //assertTrue(Double.POSITIVE_INFINITY * Double.POSITIVE_INFINITY == Double.POSITIVE_INFINITY); // sanity check
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), Double.valueOf(Double.POSITIVE_INFINITY * Double.POSITIVE_INFINITY)); // sanity check

        assertTrue(Duration.INDEFINITE.multiply(Double.POSITIVE_INFINITY).isIndefinite());
        assertFalse(Duration.INDEFINITE.multiply(Double.POSITIVE_INFINITY).isUnknown());
    }

    @Test public void multiply_UNKNOWN_and_INDEFINITE_ResultsInUnknown() {
        assertTrue(Double.isNaN(Double.NaN * Double.POSITIVE_INFINITY)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(Double.NaN * Double.POSITIVE_INFINITY)); // sanity check

        assertFalse(Duration.UNKNOWN.multiply(Double.POSITIVE_INFINITY).isIndefinite());
        assertTrue(Duration.UNKNOWN.multiply(Double.POSITIVE_INFINITY).isUnknown());
    }

    @Test public void multiply_ZERO_and_UNKNOWN_ResultsInUnknown() {
        assertTrue(Double.isNaN(0 * Double.NaN)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(0.0 * Double.NaN)); // sanity check

        assertFalse(Duration.ZERO.multiply(Double.NaN).isIndefinite());
        assertTrue(Duration.ZERO.multiply(Double.NaN).isUnknown());
    }

    @Test public void multiply_ONE_and_UNKNOWN_ResultsInUnknown() {
        assertTrue(Double.isNaN(1.0 * Double.NaN)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(1.0 * Double.NaN)); // sanity check

        assertFalse(Duration.ONE.multiply(Double.NaN).isIndefinite());
        assertTrue(Duration.ONE.multiply(Double.NaN).isUnknown());
    }

    @SuppressWarnings("deprecation")
    @Test public void testMultiplyUsingMixedUnits() {
        Duration a = Duration.minutes(10);
        Duration b = Duration.seconds(30);
        Duration expected = Duration.millis((1000.0 * 30) * (10 * 60 * 1000.0));
        assertEquals(expected, a.multiply(b));
    }

    /************************************************************************************
     *
     * Tests for dividing two Durations
     *
     ***********************************************************************************/

    @Test public void divide_doesNotModifyTheInputArguments() {
        Duration a = new Duration(10);
        Duration result = a.divide(2);
        assertEquals(new Duration(5), result);
        assertEquals(10, a.toMillis(), 0);
        assertFalse(result.isIndefinite());
        assertFalse(result.isUnknown());
    }

    @Test public void divide_Ten_by_ZERO_ResultsIn_INDEFINITE() {
        assertTrue(new Duration(10).divide(0).isIndefinite());
        assertFalse(new Duration(10).divide(0).isUnknown());
    }

    @Test public void divide_ZERO_by_Ten_ResultsIn_ZERO() {
        assertSame(Duration.ZERO, Duration.ZERO.divide(10));
    }

    @Test public void divide_ONE_by_ONE_ResultsIn_ONE() {
        assertEquals(Duration.ONE, Duration.ONE.divide(1));
    }

    @Test public void divide_ONE_Second_by_ONE_Milli_ResultsInOneSecond() {
        assertEquals(new Duration(1000), Duration.seconds(1).divide(1));
    }

    @Test public void divide_ZERO_by_INDEFINITE_ResultsIn_ZERO() {
        //assertTrue(0.0 / Double.POSITIVE_INFINITY == 0.0); // sanity check
        assertEquals(Double.valueOf(0.0), Double.valueOf(0.0 / Double.POSITIVE_INFINITY)); // sanity check

        assertSame(Duration.ZERO, Duration.ZERO.divide(Double.POSITIVE_INFINITY));
    }

    @Test public void divide_ONE_by_INDEFINITE_ResultsIn_ZERO() {
        //assertTrue(1.0 / Double.POSITIVE_INFINITY == 0.0); // sanity check
        assertEquals(Double.valueOf(0.0), Double.valueOf(1.0 / Double.POSITIVE_INFINITY)); // sanity check

        assertSame(Duration.ZERO, Duration.ONE.divide(Double.POSITIVE_INFINITY));
    }

    @Test public void divide_INDEFINITE_by_INDEFINITE_ResultsInUnknown() {
        assertTrue(Double.isNaN(Double.POSITIVE_INFINITY / Double.POSITIVE_INFINITY)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(Double.POSITIVE_INFINITY / Double.POSITIVE_INFINITY)); // sanity check

        assertFalse(Duration.INDEFINITE.divide(Double.POSITIVE_INFINITY).isIndefinite());
        assertTrue(Duration.INDEFINITE.divide(Double.POSITIVE_INFINITY).isUnknown());
    }

    @Test public void divide_UNKNOWN_by_INDEFINITE_ResultsInUnknown() {
        assertTrue(Double.isNaN(Double.NaN / Double.POSITIVE_INFINITY)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(Double.NaN / Double.POSITIVE_INFINITY)); // sanity check

        assertFalse(Duration.UNKNOWN.divide(Double.POSITIVE_INFINITY).isIndefinite());
        assertTrue(Duration.UNKNOWN.divide(Double.POSITIVE_INFINITY).isUnknown());
    }

    @Test public void divide_ZERO_by_UNKNOWN_ResultsInUnknown() {
        assertTrue(Double.isNaN(0.0 / Double.NaN)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(0.0 / Double.NaN)); // sanity check

        assertFalse(Duration.ZERO.divide(Double.NaN).isIndefinite());
        assertTrue(Duration.ZERO.divide(Double.NaN).isUnknown());
    }

    @Test public void divide_ONE_by_UNKNOWN_ResultsInUnknown() {
        assertTrue(Double.isNaN(1.0 / Double.NaN)); // sanity check
        assertEquals(Double.valueOf(Double.NaN), Double.valueOf(1.0 / Double.NaN)); // sanity check

        assertFalse(Duration.ONE.divide(Double.NaN).isIndefinite());
        assertTrue(Duration.ONE.divide(Double.NaN).isUnknown());
    }

    @SuppressWarnings("deprecation")
    @Test public void testDivideUsingMixedUnits() {
        Duration a = Duration.minutes(10);
        Duration b = Duration.seconds(30);
        Duration expected = Duration.millis((10 * 60 * 1000.0) / (1000.0 * 30));
        assertEquals(expected, a.divide(b));
    }

    /************************************************************************************
     *
     * Tests for negating a Duration
     *
     ***********************************************************************************/

    @Test public void negate_ZERO_ResultsIn_NegativeZeroOr_ZERO() {
        assertEquals(new Duration(-0), Duration.ZERO.negate());
        assertSame(Duration.ZERO, Duration.ZERO.negate());
    }

    @Test public void negate_ONE_ResultsIn_NegativeOne() {
        assertEquals(new Duration(-1), Duration.ONE.negate());
    }

    @Test public void negate_INDEFINITE_ResultsIn_NegativeInfinity() {
        final Duration result = Duration.INDEFINITE.negate();
        assertEquals(new Duration(Double.NEGATIVE_INFINITY), result);
        assertNotSame(Duration.INDEFINITE, result);
    }

    @Test public void negate_UNKNOWN_ResultsIn_UNKNOWN() {
        final Duration result = Duration.UNKNOWN.negate();
        assertSame(Duration.UNKNOWN, result);
        assertFalse(result.isIndefinite());
        assertTrue(result.isUnknown());
    }

    @Test public void negate_NegativeResultsInPositive() {
        assertEquals(new Duration(50), new Duration(-50).negate());
    }

    @Test public void negate_PositiveResultsInNegative() {
        assertEquals(new Duration(-50), new Duration(50).negate());
    }

    /************************************************************************************
     *
     * Tests for lessThan comparison with Duration
     *
     ***********************************************************************************/

    @Test public void negativeInfinityIsLessThan_ZERO() {
        assertTrue(Double.NEGATIVE_INFINITY < 0);
        assertTrue(new Duration(Double.NEGATIVE_INFINITY).lessThan(Duration.ZERO));
    }

    @Test public void negativeInfinityIsNotLessThanNegativeInfinity() {
        assertFalse(Double.NEGATIVE_INFINITY < Double.NEGATIVE_INFINITY);
        Duration a = new Duration(Double.NEGATIVE_INFINITY);
        assertFalse(a.lessThan(a));
    }

    @Test public void negativeNumberIsLessThanZero() {
        assertTrue(-10 < 0.0);
        assertTrue(new Duration(-10).lessThan(Duration.ZERO));
    }

    @Test public void ZERO_isLessThan_ONE() {
        assertTrue(0 < 1);
        assertTrue(Duration.ZERO.lessThan(Duration.ONE));
    }

    @Test public void ONE_isLessThan_INDEFINITE() {
        assertTrue(0 < Double.POSITIVE_INFINITY);
        assertTrue(Duration.ONE.lessThan(Duration.INDEFINITE));
    }

    @Test public void INDEFINITE_isNotLessThan_INDEFINITE() {
        assertFalse(Double.POSITIVE_INFINITY < Double.POSITIVE_INFINITY);
        assertFalse(Duration.INDEFINITE.lessThan(Duration.INDEFINITE));
    }

    @Test public void UNKNOWN_isNotLessThan_NegativeInfinity() {
        assertFalse(Double.NaN < Double.NEGATIVE_INFINITY);
        assertFalse(Duration.UNKNOWN.lessThan(new Duration(Double.NEGATIVE_INFINITY)));
    }

    @Test public void UNKNOWN_isNotLessThan_ZERO() {
        assertFalse(Double.NaN < 0.0);
        assertFalse(Duration.UNKNOWN.lessThan(Duration.ZERO));
    }

    @Test public void UNKNOWN_isNotLessThan_ONE() {
        assertFalse(Double.NaN < 1.0);
        assertFalse(Duration.UNKNOWN.lessThan(Duration.ONE));
    }

    @Test public void UNKNOWN_isNotLessThan_INDEFINITE() {
        assertFalse(Double.NaN < Double.POSITIVE_INFINITY);
        assertFalse(Duration.UNKNOWN.lessThan(Duration.INDEFINITE));
    }

    @Test public void UNKNOWN_isNotLessThan_UNKNOWN() {
        assertFalse(Double.NaN < Double.NaN);
        assertFalse(Duration.UNKNOWN.lessThan(Duration.UNKNOWN));
    }

    /************************************************************************************
     *
     * Tests for lessThanOrEqualTo comparison with Duration
     *
     ***********************************************************************************/

    @Test public void negativeInfinityIsLessThanOrEqualTo_ZERO() {
        assertTrue(Double.NEGATIVE_INFINITY <= 0);
        assertTrue(new Duration(Double.NEGATIVE_INFINITY).lessThanOrEqualTo(Duration.ZERO));
    }

    @Test public void negativeInfinityIsLessThanOrEqualToNegativeInfinity() {
        assertTrue(Double.NEGATIVE_INFINITY <= Double.NEGATIVE_INFINITY);
        Duration a = new Duration(Double.NEGATIVE_INFINITY);
        assertTrue(a.lessThanOrEqualTo(a));
    }

    @Test public void negativeNumberIsLessOrEqualToThanZero() {
        assertTrue(-10 <= 0);
        assertTrue(new Duration(-10).lessThanOrEqualTo(Duration.ZERO));
    }

    @Test public void ZERO_isLessThanOrEqualTo_ONE() {
        assertTrue(0 <= 1);
        assertTrue(Duration.ZERO.lessThanOrEqualTo(Duration.ONE));
    }

    @Test public void ONE_isLessThanOrEqualTo_INDEFINITE() {
        assertTrue(1 <= Double.POSITIVE_INFINITY);
        assertTrue(Duration.ONE.lessThanOrEqualTo(Duration.INDEFINITE));
    }

    @Test public void INDEFINITE_isLessThanOrEqualTo_INDEFINITE() {
        assertTrue(Double.POSITIVE_INFINITY <= Double.POSITIVE_INFINITY);
        assertTrue(Duration.INDEFINITE.lessThanOrEqualTo(Duration.INDEFINITE));
    }

    @Test public void UNKNOWN_isNotLessThanOrEqualTo_NegativeInfinity() {
        assertFalse(Double.NaN <= Double.NEGATIVE_INFINITY);
        assertFalse(Duration.UNKNOWN.lessThanOrEqualTo(new Duration(Double.NEGATIVE_INFINITY)));
    }

    @Test public void UNKNOWN_isNotLessThanOrEqualTo_ZERO() {
        assertFalse(Double.NaN <= 0.0);
        assertFalse(Duration.UNKNOWN.lessThanOrEqualTo(Duration.ZERO));
    }

    @Test public void UNKNOWN_isNotLessThanOrEqualTo_ONE() {
        assertFalse(Double.NaN <= 1.0);
        assertFalse(Duration.UNKNOWN.lessThanOrEqualTo(Duration.ONE));
    }

    @Test public void UNKNOWN_isNotLessThanOrEqualTo_INDEFINITE() {
        assertFalse(Double.NaN <= Double.POSITIVE_INFINITY);
        assertFalse(Duration.UNKNOWN.lessThanOrEqualTo(Duration.INDEFINITE));
    }

    @Test public void UNKNOWN_isNotLessThanOrEqualTo_UNKNOWN() {
        assertFalse(Double.NaN <= Double.NaN);
        assertFalse(Duration.UNKNOWN.lessThanOrEqualTo(Duration.UNKNOWN));
    }

    /************************************************************************************
     *
     * Tests for greaterThan comparison with Duration
     *
     ***********************************************************************************/

    @Test public void ZERO_isGreaterThanNegativeInfinity() {
        assertTrue(Duration.ZERO.greaterThan(new Duration(Double.NEGATIVE_INFINITY)));
    }

    @Test public void negativeInfinityIsNotGreaterThanNegativeInfinity() {
        Duration a = new Duration(Double.NEGATIVE_INFINITY);
        assertFalse(a.greaterThan(a));
    }

    @Test public void ZERO_isGreaterThanNegativeNumber() {
        assertTrue(Duration.ZERO.greaterThan(new Duration(-10)));
    }

    @Test public void ONE_isGreaterThan_ZERO() {
        assertTrue(Duration.ONE.greaterThan(Duration.ZERO));
    }

    @Test public void INDEFINITE_isGreaterThan_ONE() {
        assertTrue(Duration.INDEFINITE.greaterThan(Duration.ONE));
    }

    @Test public void INDEFINITE_isNotGreaterThan_INDEFINITE() {
        assertFalse(Duration.INDEFINITE.greaterThan(Duration.INDEFINITE));
    }

    @Test public void UNKNOWN_isNotGreaterThan_NegativeInfinity() {
        assertFalse(Duration.UNKNOWN.greaterThan(new Duration(Double.NEGATIVE_INFINITY)));
    }

    @Test public void UNKNOWN_isNotGreaterThan_ZERO() {
        assertFalse(Duration.UNKNOWN.greaterThan(Duration.ZERO));
    }

    @Test public void UNKNOWN_isNotGreaterThan_ONE() {
        assertFalse(Duration.UNKNOWN.greaterThan(Duration.ONE));
    }

    @Test public void UNKNOWN_isNotGreaterThan_INDEFINITE() {
        assertFalse(Duration.UNKNOWN.greaterThan(Duration.INDEFINITE));
    }

    @Test public void UNKNOWN_isNotGreaterThan_UNKNOWN() {
        assertFalse(Duration.UNKNOWN.greaterThan(Duration.UNKNOWN));
    }

    /************************************************************************************
     *
     * Tests for greaterThanOrEqualTo comparison with Duration
     *
     ***********************************************************************************/

    @Test public void ZERO_isGreaterThanOrEqualToNegativeInfinity() {
        assertTrue(Duration.ZERO.greaterThanOrEqualTo(new Duration(Double.NEGATIVE_INFINITY)));
    }

    @Test public void negativeInfinityIsGreaterThanOrEqualToNegativeInfinity() {
        Duration a = new Duration(Double.NEGATIVE_INFINITY);
        assertTrue(a.greaterThanOrEqualTo(a));
    }

    @Test public void ZERO_isGreaterThanOrEqualToNegativeNumber() {
        assertTrue(Duration.ZERO.greaterThanOrEqualTo(new Duration(-10)));
    }

    @Test public void ONE_isGreaterThanOrEqualTo_ZERO() {
        assertTrue(Duration.ONE.greaterThanOrEqualTo(Duration.ZERO));
    }

    @Test public void INDEFINITE_isGreaterThanOrEqualTo_ONE() {
        assertTrue(Duration.INDEFINITE.greaterThanOrEqualTo(Duration.ONE));
    }

    @Test public void INDEFINITE_isGreaterThanOrEqualTo_INDEFINITE() {
        assertTrue(Duration.INDEFINITE.greaterThanOrEqualTo(Duration.INDEFINITE));
    }

    @Test public void UNKNOWN_isNotGreaterThanOrEqualTo_NegativeInfinity() {
        assertFalse(Duration.UNKNOWN.greaterThanOrEqualTo(new Duration(Double.NEGATIVE_INFINITY)));
    }

    @Test public void UNKNOWN_isNotGreaterThanOrEqualTo_ZERO() {
        assertFalse(Duration.UNKNOWN.greaterThanOrEqualTo(Duration.ZERO));
    }

    @Test public void UNKNOWN_isNotGreaterThanOrEqualTo_ONE() {
        assertFalse(Duration.UNKNOWN.greaterThanOrEqualTo(Duration.ONE));
    }

    @Test public void UNKNOWN_isNotGreaterThanOrEqualTo_INDEFINITE() {
        assertFalse(Duration.UNKNOWN.greaterThanOrEqualTo(Duration.INDEFINITE));
    }

    @Test public void UNKNOWN_isNotGreaterThanOrEqualTo_UNKNOWN() {
        assertFalse(Duration.UNKNOWN.greaterThanOrEqualTo(Duration.UNKNOWN));
    }

    /************************************************************************************
     *
     * Tests for equality comparison with Duration
     *
     ***********************************************************************************/

    @Test public void NegativeInfinityEqualsNegativeInfinity() {
        assertEquals(new Duration(Double.NEGATIVE_INFINITY), new Duration(Double.NEGATIVE_INFINITY));
    }

    @Test public void ZERO_EqualsZero() {
        assertEquals(Duration.ZERO, new Duration(0));
    }

    @Test public void ONE_EqualsOne() {
        assertEquals(Duration.ONE, new Duration(1));
    }

    @Test public void INDEFINITE_Equals_PositiveInfinity() {
        assertEquals(Duration.INDEFINITE, new Duration(Double.POSITIVE_INFINITY));
    }

    @Test public void UNKNOWN_DoesNotEqualNaN() {
        assertFalse(Duration.UNKNOWN.equals(new Duration(Double.NaN)));
    }

    @Test public void UNKNOWN_Equals_UNKNOWN() {
        assertEquals(Duration.UNKNOWN, Duration.UNKNOWN);
    }
}
