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

import java.util.Random;
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
    }

    @Test
    public void parseExponentForms() {
        assertSameDouble(100.0, "1e2");
        assertSameDouble(100.0, "1E+2");
        assertSameDouble(125.0, "1.25e2");
        assertSameDouble(0.01, "1e-2");
        assertSameDouble(100.0, "1e0002");
        assertSameDouble(1.0, "1e-0");
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
     * the rounding of {@link Double#parseDouble(String)} for random numbers with up to 19 significant
     * digits, and {@code |exp| <= 10}.
     */
    @Test
    public void roundingIsCorrectFor64BitSignificands() {
        int seed = new Random().nextInt();
        System.out.println("Testing CssNumberParserTest.roundingIsCorrectFor64BitSignificands with seed " + seed);

        var rnd = new Random(seed);
        for (int i = 0; i < 100_000; i++) {
            String number = randomNumber(rnd, 19, 10);
            double expected = Double.parseDouble(number);
            double actual = parseDouble(number);
            assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual));
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
}
