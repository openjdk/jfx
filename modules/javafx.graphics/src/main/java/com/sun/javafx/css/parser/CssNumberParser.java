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

public final class CssNumberParser {

    private CssNumberParser() {}

    // Up to 19 decimal digits fit in a signed 64-bit long
    private static final int MAX_SIG_DIGITS = 19;

    // Saturation: beyond this, doubles overflow/underflow anyway
    private static final int EXP_LIMIT = 10000;

    /**
     * Parses a number according to the W3C "Consume a number" algorithm.
     * <p>
     * This method operates on the substring {@code [start, end)} of {@code s}, which is expected to
     * contain the string representation of a number without leading or trailing whitespace.
     * <p>
     * This implementation is allocation-free and avoids per-digit floating-point math by accumulating
     * a (truncated) base-10 significand and a base-10 exponent. It does not aim for "perfectly rounded"
     * conversion for extremely long representations, but generally returns values within 2 ULPs of the
     * value returned by {@link Double#parseDouble(String)}. Representations without a fractional part
     * are bit-for-bit identical.
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

        // While the next input code point is a digit, consume and append.
        while (p < end) {
            char c = s.charAt(p);
            if (c >= '0' && c <= '9') {
                digitsConsumed++;
                int d = c - '0';
                if (d != 0 || seenNonZero) {
                    seenNonZero = true;
                    if (sigDigits < MAX_SIG_DIGITS) {
                        // Append digit if it doesn't overflow.
                        if (significand <= (Long.MAX_VALUE - d) / 10L) {
                            significand = significand * 10L + d;
                            sigDigits++;
                        } else {
                            sigDigits = MAX_SIG_DIGITS; // can't append without overflow, treat as truncated
                            exp10++; // extra integer digit increases magnitude
                        }
                    } else {
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
                                if (significand <= (Long.MAX_VALUE - d) / 10L) {
                                    significand = significand * 10L + d;
                                    sigDigits++;
                                    exp10--; // appended a fractional digit
                                } else {
                                    // Overflow would occur, from here on treat as truncated fraction digits.
                                    sigDigits = MAX_SIG_DIGITS;
                                }
                            } else {
                                // Truncate extra fractional digits (least significant): do nothing.
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

        double value = (double)significand;

        if (exp10 != 0) {
            value *= Math.pow(10.0, exp10);
        }

        return negative ? -value : value;
    }
}
