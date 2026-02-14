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

package com.sun.javafx.css.parser;

import java.math.BigInteger;

public final class CssNumberParser {

    private CssNumberParser() {}

    // Up to 19 decimal digits fit in an unsigned 64-bit long
    private static final int MAX_SIG_DIGITS = 19;

    // Saturation: beyond this, doubles overflow/underflow anyway
    private static final int EXP_LIMIT = 10000;

    /**
     * Parses a number according to the W3C "Consume a number" algorithm. A number can have integer
     * and fractional parts, and it supports E notation (exponential).
     * <p>
     * This method operates on the substring {@code [start, end)} of {@code s}, which is expected to
     * contain the string representation of a number without leading or trailing whitespace.
     * <p>
     * This implementation is allocation-free and avoids per-digit floating-point math by accumulating a
     * (truncated) base-10 significand and a base-10 exponent. For significands with less than 20 digits,
     * it uses Lemire's algorithm to guarantee correct rounding to the nearest representable double.
     * For significands with 20 digits or more, the result is not guaranteed to be correctly rounded.
     *
     * @throws NumberFormatException if the substring does not consist entirely of a valid number representation
     * @see <a href="https://www.w3.org/TR/css-syntax-3/#consume-number">Consume a number</a>
     */
    public static double parseDouble(String s, int start, int end) {
        int p = start;
        boolean negative = false;

        // Optional sign
        if (p < end) {
            char c = s.charAt(p);
            if (c == '+' || c == '-') {
                negative = (c == '-');
                p++;
            }
        }

        long significand = 0;
        int sigDigits = 0;
        int exp10 = 0; // base-10 exponent applied to significand
        int digitsConsumed = 0; // digits in the main number (excluding exponent)
        boolean seenNonZero = false;
        boolean truncated = false;

        // While the next input code point is a digit, consume and append.
        while (p < end) {
            char c = s.charAt(p);
            if (c >= '0' && c <= '9') {
                digitsConsumed++;
                int d = c - '0';
                if (d != 0 || seenNonZero) {
                    seenNonZero = true;
                    if (sigDigits < MAX_SIG_DIGITS) {
                        // May go negative as signed, but bit pattern is correct uint64
                        significand = significand * 10L + d;
                        sigDigits++;
                    } else {
                        if (d != 0) {
                            truncated = true;
                        }

                        exp10++; // truncate extra integer digits by increasing the exponent
                    }
                }

                // Leading zeros in the integer part don't affect significand or exponent.
                p++;
            } else {
                break;
            }
        }

        // If '.' followed by a digit, consume fractional digits.
        if (p + 1 < end && s.charAt(p) == '.') {
            char next = s.charAt(p + 1);
            if (next >= '0' && next <= '9') {
                p++; // consume '.'
                while (p < end) {
                    char c = s.charAt(p);
                    if (c >= '0' && c <= '9') {
                        digitsConsumed++;
                        int d = c - '0';
                        if (d != 0 || seenNonZero) {
                            seenNonZero = true;
                            if (sigDigits < MAX_SIG_DIGITS) {
                                significand = significand * 10L + d;
                                sigDigits++;
                                exp10--; // appended a fractional digit
                            } else if (d != 0) {
                                truncated = true;
                            }
                        } else {
                            // Leading zeros immediately after the decimal point shift the exponent.
                            exp10--;
                        }

                        p++;
                    } else {
                        break;
                    }
                }
            }
        }

        // Exponent part (E/e, optional sign, then a digit)
        if (p < end) {
            char e = s.charAt(p);
            if (e == 'E' || e == 'e') {
                boolean expNegative = false;
                int q = p + 1;
                if (q < end) {
                    char sign = s.charAt(q);
                    if (sign == '+' || sign == '-') {
                        expNegative = (sign == '-');
                        q++;
                    }
                }

                if (q < end) {
                    char firstExpDigit = s.charAt(q);
                    if (firstExpDigit >= '0' && firstExpDigit <= '9') {
                        int exp = 0;
                        while (q < end) {
                            char c = s.charAt(q);
                            if (c >= '0' && c <= '9') {
                                if (exp < EXP_LIMIT) {
                                    exp = exp * 10 + (c - '0');
                                    if (exp > EXP_LIMIT) {
                                        exp = EXP_LIMIT;
                                    }
                                }

                                q++;
                            } else {
                                break;
                            }
                        }

                        p = q;
                        exp10 += expNegative ? -exp : exp;
                    }
                }
            }
        }

        // At least one digit must have been consumed from either the integer or fractional part.
        if (digitsConsumed == 0 || p != end) {
            throw new NumberFormatException("Invalid number: " + s.substring(start, end));
        }

        // If the numeric value is zero, preserve the sign.
        if (significand == 0) {
            return negative ? -0.0 : 0.0;
        }

        double value;

        // If the significand is not truncated, we can use Lemire's algorithm.
        // If it is truncated, we use a simple fallback that doesn't guarantee correct rounding.
        if (!truncated && exp10 >= -342 && exp10 <= 308) {
            value = convertToNearestDouble(significand, exp10);
        } else if (exp10 == 0) {
            value = unsignedLongToDouble(significand);
        } else {
            double v = unsignedLongToDouble(significand);
            value = v * Math.pow(10.0, exp10);
        }

        return negative ? -value : value;
    }

    /**
     * Converts a decimal floating-point number {@code w*10^q} to the nearest representable double.
     * <p>
     * Based on Algorithm 1, "Number Parsing at a Gigabyte per Second" (Lemire, 2021), p. 8, without
     * the fallback condition; see "Fast Number Parsing Without Fallback" (Mushtak, Lemire, 2022).
     * <p>
     * Precondition: {@code w != 0} and {@code q in [-342, 308]}
     *
     * @see <a href="https://arxiv.org/pdf/2101.11408">Number Parsing at a Gigabyte per Second</a>
     * @see <a href="https://arxiv.org/pdf/2212.06644">Fast Number Parsing Without Fallback</a>
     */
    private static double convertToNearestDouble(long w, int q) {
        // Normalize w to [2^63, 2^64), see Algorithm 1 lines 3-4
        int l = Long.numberOfLeadingZeros(w);
        long wNorm = w << l;

        // Load 128-bit T[q] as two 64-bit words
        int idx = (q + 342) << 1;
        long tHi = T[idx];
        long tLo = T[idx + 1];

        // z = (T[q] * wNorm) >> 64 (a 128-bit value split into zHi/zLo), see line 5
        long p1Hi = Math.unsignedMultiplyHigh(tLo, wNorm);
        long p2Hi = Math.unsignedMultiplyHigh(tHi, wNorm);
        long p2Lo = tHi * wNorm;
        long zLo = p2Lo + p1Hi;
        long carry = Long.compareUnsigned(zLo, p2Lo) < 0 ? 1L : 0L;
        long zHi = p2Hi + carry;

        // u = msb(z)
        long u = zHi >>> 63;

        // m = most significant 54 bits of z, ignoring an eventual leading zero bit
        long m;
        if (u != 0) {
            // bits 127..74 correspond to zHi bits 63..10
            m = zHi >>> 10;
        } else {
            // bits 126..73 correspond to zHi bits 62..9
            m = (zHi >>> 9) & ((1L << 54) - 1L);
        }

        // p = floor(log2(10) * q) + 63 - l + u, with log2(10) approximated as 217706/2^16 (line 9)
        int p = (int)(((217706L * (long)q) >> 16) + 63 - l + (int)u);

        // Too small => underflow to zero (line 10)
        if (p <= -1022 - 64) {
            return 0;
        }

        // Subnormals (lines 11-15)
        if (p <= -1022) {
            int s = -1022 - p + 1;
            if (s >= 64) {
                m = 0;
            } else {
                m >>>= s;
            }

            if ((m & 1L) != 0) {
                m++;
            }

            m >>>= 1;
            return Math.scalb((double)m, p - 52);
        }

        // Round ties to even (lines 16-18)
        if (Long.compareUnsigned(zLo, 1L) <= 0 && (m & 1L) != 0 && ((m >>> 1) & 1L) == 0 && q >= -4 && q <= 23) {
            long quo = Long.divideUnsigned(zHi, m);
            long rem = Long.remainderUnsigned(zHi, m);
            if (rem == 0 && quo != 0 && (quo & (quo - 1)) == 0) {
                m--;
            }
        }

        // Round the binary significand (line 19)
        if ((m & 1L) != 0) {
            m++;
        }

        m >>>= 1;

        // Handle significand overflow (line 20)
        if (m == (1L << 53)) {
            m >>>= 1;
            p += 1;
        }

        // Overflow => infinity (line 21)
        if (p > 1023) {
            return Double.POSITIVE_INFINITY;
        }

        return Math.scalb((double)m, p - 52); // line 22
    }

    /**
     * 128-bit reciprocal and normalized powers of five, stored as two 64-bit words.
     * See "Table Generation Script" in "Number Parsing at a Gigabyte per Second" (Lemire, 2021), p. 32
     *
     * @see <a href="https://arxiv.org/pdf/2101.11408">Number Parsing at a Gigabyte per Second</a>
     */
    public static final long[] T;

    static {
        int min = -342, max = 308;
        long[] res = new long[(max - min + 1) * 2];
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
            store128(res, i++, c);
            power5 = power5.divide(five);
        }

        // q in [-27, 0)
        for (int q = -27; q < 0; q++) {
            int z = ceilLog2(power5);
            int b = z + 127;
            var c = BigInteger.ONE.shiftLeft(b).divide(power5).add(BigInteger.ONE);
            while (c.bitLength() > 128) c = c.shiftRight(1);
            store128(res, i++, c);
            power5 = power5.divide(five);
        }

        // Now walk up by multiplying with 5 each step.
        power5 = BigInteger.ONE;

        // q in [0, 308]
        for (int q = 0; q <= 308; q++) {
            BigInteger v = power5;
            while (v.compareTo(twoPow127) < 0) v = v.shiftLeft(1);
            while (v.compareTo(twoPow128) >= 0) v = v.shiftRight(1);
            store128(res, i++, v);
            power5 = power5.multiply(five);
        }

        T = res;
    }

    private static void store128(long[] out, int entryIndex, BigInteger x) {
        int base = entryIndex * 2;
        out[base] = x.shiftRight(64).longValue();
        out[base + 1] = x.longValue();
    }

    // If x is an exact power of two, ceil(log2(x)) = log2(x) = bl-1, else it's bl
    private static int ceilLog2(BigInteger x) {
        int bl = x.bitLength(); // = floor(log2(x)) + 1 for x>0
        boolean isPow2 = x.and(x.subtract(BigInteger.ONE)).equals(BigInteger.ZERO);
        return isPow2 ? (bl - 1) : bl;
    }

    // Interprets x as unsigned 64-bit and converts to double
    private static double unsignedLongToDouble(long x) {
        return x >= 0 ? (double)x : ((double)(x >>> 1) * 2.0 + (double)(x & 1L));
    }
}
