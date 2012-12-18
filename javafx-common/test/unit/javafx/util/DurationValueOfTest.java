/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.util;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 */
@RunWith(Parameterized.class)
public class DurationValueOfTest {
    @SuppressWarnings("rawtypes")
    @Parameterized.Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][]{
                {"5ms", Duration.millis(5)},
                {"0ms", Duration.ZERO},
                {"25.5ms", Duration.millis(25.5)},
                {"-10ms", Duration.millis(-10)},
                {"5s", Duration.seconds(5)},
                {"0s", Duration.ZERO},
                {"25.5s", Duration.seconds(25.5)},
                {"-10s", Duration.seconds(-10)},
                {"5m", Duration.minutes(5)},
                {"0m", Duration.ZERO},
                {"25.5m", Duration.minutes(25.5)},
                {"-10m", Duration.minutes(-10)},
                {"5h", Duration.hours(5)},
                {"0h", Duration.ZERO},
                {"25.5h", Duration.hours(25.5)},
                {"-10h", Duration.hours(-10)}
        });
    }

    private String asString;
    private Duration expected;

    public DurationValueOfTest(String asString, Duration expected) {
        this.asString = asString;
        this.expected = expected;
    }

    @Test public void testValueOf() {
        Duration actual = Duration.valueOf(asString);
        assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void leadingSpaceResultsInException() {
        Duration.valueOf(" " + asString);
    }

    @Test(expected = IllegalArgumentException.class)
    public void trailingSpaceResultsInException() {
        Duration.valueOf(asString + " ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCaseResultsInException() {
        String mangled = asString.substring(0, asString.length()-1) + Character.toUpperCase(asString.charAt(asString.length()-1));
        Duration.valueOf(mangled);
    }
}
