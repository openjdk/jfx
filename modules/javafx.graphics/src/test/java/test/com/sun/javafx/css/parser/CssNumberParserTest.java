/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.css.parser;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import java.util.function.LongConsumer;
import com.sun.javafx.css.parser.CssNumberParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CssNumberParserTest {

    @Test
    public void parseIntegerAndSignedInteger() {
        assertSameDouble(0.0, "0");
        assertSameDouble(255.0, "255");
        assertSameDouble(2.0, "+2");
        assertSameDouble(-2.0, "-2");
        assertSameDouble(42.0, "00042");
    }

    @Test
    public void parseFractionalForms() {
        assertSameDouble(0.5, ".5");
        assertSameDouble(-0.5, "-.5");
        assertSameDouble(12.34, "12.34");
        assertSameDouble(0.00123, "0.00123");
        assertSameDouble(100.0, "100.0");
        assertSameDouble(0.2, "0.2");
        assertSameDouble(0.2, "0.20000000000000001110");
        assertSameDouble(0.19999999999999998335, "0.19999999999999998335");
        assertSameDouble(9000000000000000., "9000000000000000.5");
        assertSameDouble(9000000000000002., "9000000000000001.5");
        assertSameDouble(9000000000000002., "9000000000000002.5");
        assertSameDouble(9007199254740993., "9007199254740993");
    }

    @Test
    public void parseExponentForms() {
        assertSameDouble(100.0, "1e2");
        assertSameDouble(100.0, "1E+2");
        assertSameDouble(125.0, "1.25e2");
        assertSameDouble(0.01, "1e-2");
        assertSameDouble(100.0, "1e0002");
        assertSameDouble(1.0, "1e-0");
        assertSameDouble(1e22, "1e22");
        assertSameDouble(1e23, "1e23");
        assertSameDouble(7450580596923828125e-27, "7450580596923828125e-27");
        assertSameDouble(2440254496e57, "2440254496e57");
        assertSameDouble(9.109e-31, "9.109e-31");
    }

    @Test
    public void parseSubstringRanges() {
        assertEquals(-125.0, parseDouble("xx-1.25e2yy", 2, 9), 0.0); // slice is "-1.25e2"
        assertEquals(0.5, parseDouble("abc+.5def", 3, 6), 0.0); // slice is "+.5"
    }

    @Test
    public void rejectTrailingOrLeadingJunkWithinRange() {
        assertNumberFormatException("1a");
        assertNumberFormatException("1e2x");
        assertNumberFormatException(" 1");
        assertNumberFormatException("1 ");
        assertNumberFormatException("xx1e2yy", 2, 6); // slice is "1e2y"
        assertNumberFormatException("++0");
        assertNumberFormatException("1e--2");
    }

    @Test
    public void rejectMissingDigits() {
        assertNumberFormatException("");
        assertNumberFormatException("+");
        assertNumberFormatException("-");
        assertNumberFormatException(".");
        assertNumberFormatException("+.");
        assertNumberFormatException("-.");
        assertNumberFormatException("+e");
        assertNumberFormatException("-e");
        assertNumberFormatException("e1");
        assertNumberFormatException("+e1");
    }

    @Test
    public void rejectDotWithoutFollowingDigit() {
        // Per CSS number token grammar, '.' is only part of the number if followed by a digit.
        assertNumberFormatException("1.");
        assertNumberFormatException("1.e2");
        assertNumberFormatException("0.");
    }

    @Test
    public void rejectInvalidExponentForms() {
        assertNumberFormatException("1e");
        assertNumberFormatException("1E");
        assertNumberFormatException("1e+");
        assertNumberFormatException("1e-");
        assertNumberFormatException("1e+.");
        assertNumberFormatException("1e-.");
        assertNumberFormatException("1.0e");
        assertNumberFormatException("1.0e+");
        assertNumberFormatException("1.0e-");
        assertNumberFormatException("+e170");
        assertNumberFormatException("-e170");
        assertNumberFormatException("1234    e10");
        assertNumberFormatException("-1234    e10");
    }

    @Test
    public void subnormalsAndLimits() {
        assertEquals(Double.MIN_NORMAL, parseDouble("2.2250738585072012E-308")); // below MIN_NORMAL, rounds up
        assertTrue(Double.MIN_NORMAL > parseDouble("2.2250738585072011E-308")); // below MIN_NORMAL, rounds down
        assertEquals(2.225073858507201E-308, parseDouble("2.2250738585072011E-308")); // below MIN_NORMAL, rounds down
        assertSameDouble(4.9e-324, "4e-324"); // below Double.MIN_VALUE, rounds up
        assertSameDouble(4.9e-324, "4.9e-324"); // Double.MIN_VALUE
        assertSameDouble(9.8e-324, "9.8e-324"); // Double.MIN_VALUE*2
        assertSameDouble(1.1125369292536007E-308, "1.1125369292536007E-308"); // MIN_NORMAL/2
        assertSameDouble(2.2250738585072014e-308, "2.2250738585072014e-308"); // MIN_NORMAL
        assertSameDouble(2.2250738585072012e-308, "2.2250738585072012e-308"); // near MIN_NORMAL
        assertSameDouble(2.4703282292062329e-324, "2.4703282292062329e-324"); // above MIN_VALUE/2
        assertSameDouble(0.0, "2.4703282292062327e-324"); // MIN_VALUE/2
        assertSameDouble(0.0, "2.4703282292062325e-324"); // below MIN_VALUE/2
        assertSameDouble(0.0, "1e-324");
        assertSameDouble(0.0, "2e-324");
        assertSameDouble(0.0, "0e0");
        assertSameDouble(0.0, "0.0e-999");
        assertSameDouble(0.0, "-0e0");
        assertSameDouble(0.0, "-1e-324");
        assertSameDouble(1.7976931348623157E308, "1.7976931348623157E308"); // Double.MAX_VALUE
        assertSameDouble(1.7976931348623158e+308, "1.7976931348623158e+308"); // near MAX_VALUE + ulp(MAX_VALUE)/2
        assertEquals(Double.parseDouble("1.7976931348623159e+308"), // near MAX_VALUE + ulp(MAX_VALUE)/2
                     parseDouble("1.7976931348623159e+308"));
    }

    @Test
    public void preserveNegativeZero() {
        double pz1 = parseDouble("0");
        assertEquals(0.0, pz1, 0.0);
        assertEquals(Double.doubleToRawLongBits(0.0), Double.doubleToRawLongBits(pz1));

        double pz2 = parseDouble("0.0");
        assertEquals(0.0, pz2, 0.0);
        assertEquals(Double.doubleToRawLongBits(0.0), Double.doubleToRawLongBits(pz2));

        double nz1 = parseDouble("-0");
        assertEquals(0.0, nz1, 0.0);
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(nz1));

        double nz2 = parseDouble("-0.0");
        assertEquals(0.0, nz2, 0.0);
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(nz2));

        double nz3 = parseDouble("-0e-10000");
        assertEquals(0.0, nz3, 0.0);
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(nz3));
    }

    @Test
    public void matchLongMaxValueWhenNoExponentOrFraction() {
        String s = Long.toString(Long.MAX_VALUE);
        assertEquals((double)Long.MAX_VALUE, parseDouble(s), 0.0);
    }

    @Test
    public void handleExponentSaturationAndOverflowUnderflow() {
        double posInf = parseDouble("1e100000");
        assertTrue(Double.isInfinite(posInf) && posInf > 0.0);

        double negInf = parseDouble("-1e100000");
        assertTrue(Double.isInfinite(negInf) && negInf < 0.0);

        double under = parseDouble("1e-100000");
        assertEquals(0.0, under, 0.0);
        assertEquals(Double.doubleToRawLongBits(0.0), Double.doubleToRawLongBits(under));

        double negUnder = parseDouble("-1e-100000");
        assertEquals(0.0, negUnder, 0.0);
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(negUnder));
    }

    /**
     * Deterministic fuzz test that asserts that the rounding of parsed numbers is bitwise identical to
     * the rounding of {@link Double#parseDouble(String)} for random subnormal numbers.
     */
    @Test
    public void roundingIsCorrectForSubnormals() {
        long seed = new Random().nextLong();
        System.out.println("Testing CssNumberParserTest.roundingIsCorrectForSubnormals with seed " + seed);

        var rnd = new Random(seed);
        for (int i = 0; i < 100_000; i++) {
            long frac = rnd.nextLong() & 0x000f_ffff_ffff_ffffL;
            if (frac == 0) {
                frac = 1; // keep subnormal non-zero
            }

            long bits = frac;
            if (rnd.nextBoolean()) {
                bits |= 1L << 63; // random sign
            }

            double d = Double.longBitsToDouble(bits);
            String s = Double.toString(d);
            assertSameDouble(d, s);
            testAdjacentValues(d);
        }
    }

    /**
     * Deterministic fuzz test that asserts that the rounding of parsed numbers is bitwise identical to
     * the rounding of {@link Double#parseDouble(String)} for random numbers with up to 19 significant
     * digits, and {@code |exp| <= 10}.
     */
    @Test
    public void roundingIsCorrectFor64BitSignificands() {
        long seed = new Random().nextLong();
        System.out.println("Testing CssNumberParserTest.roundingIsCorrectFor64BitSignificands with seed " + seed);

        var rnd = new Random(seed);
        for (int i = 0; i < 100_000; i++) {
            String number = randomNumber(rnd, 19, 10);
            double expected = Double.parseDouble(number);
            double actual = parseDouble(number);
            assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual));
            testAdjacentValues(actual);
        }
    }

    /**
     * Returns the string representation of a random floating-point number.
     * <p>
     * {@code maxSigDigits} limits the total number of digits in the significand (digits before
     * and after the decimal point, excluding sign and exponent).
     */
    private static String randomNumber(Random rnd, int maxSigDigits, int maxAbsExp) {
        StringBuilder sb = new StringBuilder(32);

        int sign = rnd.nextInt(5);
        if (sign == 0) {
            sb.append('-');
        } else if (sign == 1) {
            sb.append('+');
        }

        int maxDigits = Math.max(1, maxSigDigits);
        boolean dotForm = rnd.nextInt(6) == 0;
        if (dotForm) {
            sb.append('.');
            int fracDigits = 1 + rnd.nextInt(maxDigits);
            for (int i = 0; i < fracDigits; i++) {
                sb.append((char)('0' + rnd.nextInt(10)));
            }
        } else {
            int intDigits = 1 + rnd.nextInt(maxDigits);
            for (int i = 0; i < intDigits; i++) {
                sb.append((char)('0' + rnd.nextInt(10)));
            }

            int remaining = maxDigits - intDigits;
            if (remaining > 0 && rnd.nextBoolean()) {
                sb.append('.');
                int fracDigits = 1 + rnd.nextInt(remaining);
                for (int i = 0; i < fracDigits; i++) {
                    sb.append((char)('0' + rnd.nextInt(10)));
                }
            }
        }

        if (maxAbsExp > 0 && rnd.nextInt(3) == 0) {
            sb.append(rnd.nextBoolean() ? 'e' : 'E');
            int exp = rnd.nextInt(2 * maxAbsExp + 1) - maxAbsExp;
            if (rnd.nextBoolean()) {
                sb.append(exp >= 0 ? '+' : '-');
            } else if (exp < 0) {
                sb.append('-');
            }

            sb.append(Math.abs(exp));
        }

        return sb.toString();
    }

    private static void testAdjacentValues(double d) {
        double v = d;
        for (int i = 0; i < 3; i++) {
            v = Math.nextUp(v);
            if (!Double.isFinite(v)) {
                break;
            }

            assertRoundTrip(v);
        }

        v = d;
        for (int i = 0; i < 3; i++) {
            v = Math.nextDown(v);
            if (!Double.isFinite(v)) {
                break;
            }

            assertRoundTrip(v);
        }
    }

    /**
     * Tests rounding at midpoint boundaries around every power of two {@code d} (including subnormals).
     * For each {@code d}, checks the half-ULP lower/upper boundaries that should round to {@code d}.
     * We test powers of two because they sit at binade boundaries where the ULP size changes.
     */
    @Test
    public void roundingAtBinadeBoundaries() {
        for (int i = -1074; i <= 1023; i++) {
            double d = Math.scalb(1.0, i);
            var dBD = new BigDecimal(d);
            var lowerBound = dBD.subtract(new BigDecimal(Math.ulp(Math.nextUp(-d))).multiply(HALF));
            var upperBound = dBD.add(new BigDecimal(Math.ulp(d)).multiply(HALF));
            assertRounding(lowerBound.toString());
            assertRounding(upperBound.toString());
        }

        // Also verify that the overflow midpoint above MAX_VALUE rounds to +Inf.
        assertRounding(new BigDecimal(Double.MAX_VALUE)
            .add(new BigDecimal(Math.ulp(Double.MAX_VALUE)).multiply(HALF)).toString());
    }

    /**
     * Asserts that the precomputed powers-of-five table is correct.
     */
    @Test
    public void powersOfFiveTable() throws Exception {
        int min = -342, max = 308;
        long[] expected = new long[(max - min + 1) * 2];
        var twoPow127 = BigInteger.ONE.shiftLeft(127);
        var twoPow128 = BigInteger.ONE.shiftLeft(128);
        var five = BigInteger.valueOf(5);
        int i = 0;

        // Start with 5^342 and walk down by dividing by 5 each step.
        BigInteger power5 = five.pow(342);

        // q in [-342, -27)
        for (int q = -342; q < -27; q++) {
            int z = ceilLog2(power5);
            int b = 2 * z + 128;
            var c = BigInteger.ONE.shiftLeft(b).divide(power5).add(BigInteger.ONE);
            while (c.bitLength() > 128) c = c.shiftRight(1);
            store128(expected, i++, c);
            power5 = power5.divide(five);
        }

        // q in [-27, 0)
        for (int q = -27; q < 0; q++) {
            int z = ceilLog2(power5);
            int b = z + 127;
            var c = BigInteger.ONE.shiftLeft(b).divide(power5).add(BigInteger.ONE);
            while (c.bitLength() > 128) c = c.shiftRight(1);
            store128(expected, i++, c);
            power5 = power5.divide(five);
        }

        // Now walk up by multiplying with 5 each step.
        power5 = BigInteger.ONE;

        // q in [0, 308]
        for (int q = 0; q <= 308; q++) {
            BigInteger v = power5;
            while (v.compareTo(twoPow127) < 0) v = v.shiftLeft(1);
            while (v.compareTo(twoPow128) >= 0) v = v.shiftRight(1);
            store128(expected, i++, v);
            power5 = power5.multiply(five);
        }

        Field field = CssNumberParser.class.getDeclaredField("T");
        field.setAccessible(true);
        long[] actual = (long[])field.get(null);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testBitPatterns() {
        LongConsumer test = p -> {
            double value = Double.longBitsToDouble(p);
            if (Double.isFinite(value)) {
                assertRoundTrip(value);
            }
        };

        repeating(test);
        walkingOnes(test);
        walkingZeros(test);
        slidingOnes(test);
        slidingZeros(test);
    }

    private void walkingOnes(LongConsumer test) {
        long v = 1L;
        for (int i = 0; i < 64; i++) {
            test.accept(v);
            v <<= 1;
        }
    }

    private void walkingZeros(LongConsumer test) {
        long v = 0xffff_ffff_ffff_fffeL;
        for (int i = 0; i < 64; i++) {
            test.accept(v);
            v <<= 1;
            v |= 0x01L;
        }
    }

    private void repeating(LongConsumer test) {
        for (int i = 0; i < 256; i++) {
            long v = i;
            v = (v << 8) | i;
            v = (v << 8) | i;
            v = (v << 8) | i;
            v = (v << 8) | i;
            v = (v << 8) | i;
            v = (v << 8) | i;
            v = (v << 8) | i;
            test.accept(v);
        }
    }

    private void slidingOnes(LongConsumer test) {
        for (int n = 1; n < 64; n++) {
            long v = 0L;
            for (int i = 0; i < n; i++) {
                v = ((v << 1) | 0x1L);
            }

            for (int i = 0; i <= 64 - n; i++) {
                long d = v << i;
                test.accept(d);
            }
        }
    }

    private void slidingZeros(LongConsumer test) {
        for (int n = 1; n < 64; n++) {
            long v = 0L;
            for (int i = 0; i < n; i++) {
                v = ((v << 1) | 0x1L);
            }

            for (int i = 0; i <= 64 - n; i++) {
                long d = ~(v << i);
                test.accept(d);
            }
        }
    }

    private static void store128(long[] out, int entryIndex, BigInteger x) {
        int base = entryIndex * 2;
        out[base] = x.shiftRight(64).longValue();
        out[base + 1] = x.longValue();
    }

    private static int ceilLog2(BigInteger x) {
        int bl = x.bitLength();
        boolean isPow2 = x.and(x.subtract(BigInteger.ONE)).equals(BigInteger.ZERO);
        return isPow2 ? bl - 1 : bl;
    }

    private static double parseDouble(String s) {
        return CssNumberParser.parseDouble(s, 0, s.length());
    }

    private static double parseDouble(String s, int start, int end) {
        return CssNumberParser.parseDouble(s, start, end);
    }

    private static void assertSameDouble(double e, String s) {
        double expected = Double.parseDouble(s);
        double actual = CssNumberParser.parseDouble(s, 0, s.length());
        assertEquals(e, actual, 0.0);
        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual));
    }

    private static void assertNumberFormatException(String s) {
        assertThrows(NumberFormatException.class, () -> parseDouble(s));
    }

    private static void assertNumberFormatException(String s, int start, int end) {
        assertThrows(NumberFormatException.class, () -> parseDouble(s, start, end));
    }

    private static void assertRoundTrip(double v) {
        String s = Double.toString(v);
        double parsed = CssNumberParser.parseDouble(s, 0, s.length());
        assertEquals(Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(parsed), s);
    }

    private static void assertRounding(String s) {
        double n = parseDouble(s);
        boolean isNegativeN = n < 0 || n == 0 && 1/n < 0;
        double na = Math.abs(n);
        boolean isNegative = false;

        if (s.charAt(0) == '+') {
            s = s.substring(1);
        } else if (s.charAt(0) == '-') {
            s = s.substring(1);
            isNegative = true;
        }

        assertEquals(isNegativeN, isNegative);

        var bd = new BigDecimal(s);
        BigDecimal l, u;

        if (Double.isInfinite(na)) {
            l = new BigDecimal(Double.MAX_VALUE).add(new BigDecimal(Math.ulp(Double.MAX_VALUE)).multiply(HALF));
            u = null;
        } else {
            l = new BigDecimal(na).subtract(new BigDecimal(Math.ulp(Math.nextUp(-na))).multiply(HALF));
            u = new BigDecimal(na).add(new BigDecimal(Math.ulp(n)).multiply(HALF));
        }

        int cmpL = bd.compareTo(l);
        int cmpU = u != null ? bd.compareTo(u) : -1;
        if ((Double.doubleToLongBits(n) & 1) != 0) {
            assertFalse(cmpL <= 0 || cmpU >= 0);
        } else {
            assertFalse(cmpL < 0 || cmpU > 0);
        }
    }

    private static final BigDecimal HALF = BigDecimal.valueOf(0.5);
}
