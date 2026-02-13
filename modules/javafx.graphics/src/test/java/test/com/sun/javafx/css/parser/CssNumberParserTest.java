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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class CssNumberParserTest {

    @Test
    public void parseIntegerAndSignedInteger() {
        assertEquals(0.0, parseDouble("0"), 0.0);
        assertEquals(255.0, parseDouble("255"), 0.0);
        assertEquals(2.0, parseDouble("+2"), 0.0);
        assertEquals(-2.0, parseDouble("-2"), 0.0);
        assertEquals(42.0, parseDouble("00042"), 0.0);
    }

    @Test
    public void parseFractionalForms() {
        assertEquals(0.5, parseDouble(".5"), 0.0);
        assertEquals(-0.5, parseDouble("-.5"), 0.0);
        assertEquals(12.34, parseDouble("12.34"), 0.0);
        assertEquals(0.00123, parseDouble("0.00123"), 1e-18);
        assertEquals(100.0, parseDouble("100.0"), 0.0);
    }

    @Test
    public void parseExponentForms() {
        assertEquals(100.0, parseDouble("1e2"), 0.0);
        assertEquals(100.0, parseDouble("1E+2"), 0.0);
        assertEquals(125.0, parseDouble("1.25e2"), 0.0);
        assertEquals(0.01, parseDouble("1e-2"), 0.0);
        assertEquals(100.0, parseDouble("1e0002"), 0.0);
        assertEquals(1.0, parseDouble("1e-0"), 0.0);
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
    }

    @Test
    public void preserveNegativeZero() {
        double pz = parseDouble("0");
        assertEquals(0.0, pz, 0.0);
        assertEquals(Double.doubleToRawLongBits(0.0), Double.doubleToRawLongBits(pz));

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

    @ParameterizedTest
    @CsvSource({
        "0", "1", "-1", "42", "00042", "255", "1000",
        "9007199254740991",   // 2^53 - 1
        "9007199254740992",   // 2^53: last integer that can be represented exactly as a double
        "9007199254740993",   // 2^53 + 1 (rounds to 2^53)
        "9223372036854775807" // Long.MAX_VALUE
    })
    public void roundingMatchesJavaLangDoubleExactlyForIntegers(String value) {
        double expected = Double.parseDouble(value);
        double actual = parseDouble(value);
        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual));
    }

    /**
     * Deterministic fuzz test that asserts that the rounding of parsed numbers is within 2 ulps
     * of the rounding of {@link Double#parseDouble(String)} for random numbers with up to 20 integer
     * digits, up to 20 fractional digits, and {@code |exp| <= 10}.
     */
    @Test
    public void roundingIsCloseToJavaLangDouble() {
        var rnd = new Random(0xc0ffee);
        for (int i = 0; i < 100_000; i++) {
            String number = randomNumber(rnd, 20, 20, 10);
            assertRounding(number, 2);
        }
    }

    /**
     * Returns the string representation of a random floating-point number.
     */
    private static String randomNumber(Random rnd, int maxIntDigits, int maxFracDigits, int maxAbsExp) {
        StringBuilder sb = new StringBuilder(32);

        int sign = rnd.nextInt(5);
        if (sign == 0) {
            sb.append('-');
        } else if (sign == 1) {
            sb.append('+');
        }

        boolean dotForm = rnd.nextInt(6) == 0;
        if (dotForm) {
            sb.append('.');
            int fracDigits = 1 + rnd.nextInt(Math.max(1, maxFracDigits));
            for (int i = 0; i < fracDigits; i++) {
                sb.append((char)('0' + rnd.nextInt(10)));
            }
        } else {
            int intDigits = 1 + rnd.nextInt(Math.max(1, maxIntDigits));
            for (int i = 0; i < intDigits; i++) {
                sb.append((char)('0' + rnd.nextInt(10)));
            }

            if (maxFracDigits > 0 && rnd.nextBoolean()) {
                sb.append('.');
                int fracDigits = 1 + rnd.nextInt(maxFracDigits);
                for (int i = 0; i < fracDigits; i++) {
                    sb.append((char) ('0' + rnd.nextInt(10)));
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

            int abs = Math.abs(exp);
            sb.append(abs);
        }

        return sb.toString();
    }

    private static double parseDouble(String s) {
        return CssNumberParser.parseDouble(s, 0, s.length());
    }

    private static double parseDouble(String s, int start, int end) {
        return CssNumberParser.parseDouble(s, start, end);
    }

    private static void assertNumberFormatException(String s) {
        assertThrows(NumberFormatException.class, () -> parseDouble(s));
    }

    private static void assertNumberFormatException(String s, int start, int end) {
        assertThrows(NumberFormatException.class, () -> parseDouble(s, start, end));
    }

    private static void assertRounding(String s, long maxUlps) {
        double expected = Double.parseDouble(s);
        double actual = parseDouble(s);
        assertWithinUlps(expected, actual, maxUlps, "representation=\"" + s + "\"");
    }

    private static void assertWithinUlps(double expected, double actual, long maxUlps, String context) {
        long ulps = Math.abs(ordered(expected) - ordered(actual));
        assertTrue(ulps <= maxUlps, () -> context +
            " expected=" + expected +
            " actual=" + actual +
            " ulps=" + ulps + " (max=" + maxUlps + ")");
    }

    // Maps a double value into a long space where adjacent representable doubles differ by exactly 1.
    private static long ordered(double v) {
        long rawBits = Double.doubleToRawLongBits(v);
        return rawBits >= 0 ? rawBits : (0x8000_0000_0000_0000L - rawBits);
    }
}
