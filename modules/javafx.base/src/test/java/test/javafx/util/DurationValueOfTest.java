/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.stream.Stream;
import javafx.util.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DurationValueOfTest {

    static Stream<Arguments> provideTestCases() {
        return Stream.of(
                Arguments.of("5ms", Duration.millis(5)),
                Arguments.of("0ms", Duration.ZERO),
                Arguments.of("25.5ms", Duration.millis(25.5)),
                Arguments.of("-10ms", Duration.millis(-10)),
                Arguments.of("5s", Duration.seconds(5)),
                Arguments.of("0s", Duration.ZERO),
                Arguments.of("25.5s", Duration.seconds(25.5)),
                Arguments.of("-10s", Duration.seconds(-10)),
                Arguments.of("5m", Duration.minutes(5)),
                Arguments.of("0m", Duration.ZERO),
                Arguments.of("25.5m", Duration.minutes(25.5)),
                Arguments.of("-10m", Duration.minutes(-10)),
                Arguments.of("5h", Duration.hours(5)),
                Arguments.of("0h", Duration.ZERO),
                Arguments.of("25.5h", Duration.hours(25.5)),
                Arguments.of("-10h", Duration.hours(-10))
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testValueOf(String asString, Duration expected) {
        Duration actual = Duration.valueOf(asString);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void leadingSpaceResultsInException(String asString, Duration expected) {
        assertThrows(IllegalArgumentException.class, () -> Duration.valueOf(" " + asString));
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void trailingSpaceResultsInException(String asString, Duration expected) {
        assertThrows(IllegalArgumentException.class, () -> Duration.valueOf(asString + " "));
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void wrongCaseResultsInException(String asString, Duration expected) {
        String mangled = asString.substring(0, asString.length() - 1)
                + Character.toUpperCase(asString.charAt(asString.length() - 1));
        assertThrows(IllegalArgumentException.class, () -> Duration.valueOf(mangled));
    }
}
