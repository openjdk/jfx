/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.util;

import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.pgstub.StubToolkit.ScreenConfiguration;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.Utils;
import java.util.stream.Stream;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class Utils_getScreenForRectangle_Test {

    public static Stream<Arguments> data() {
        return Stream.of(
            // rectangle, expectedScreenIndex
            Arguments.of(new Rectangle2D(100, 100, 100, 100), 0),
            Arguments.of(new Rectangle2D(2020, 200, 100, 100), 1),
            Arguments.of(new Rectangle2D(1920 - 75, 200, 100, 100), 0),
            Arguments.of(new Rectangle2D(1920 - 25, 200, 100, 100), 1),
            Arguments.of(new Rectangle2D(0, 0, 3360, 1200), 0),
            Arguments.of(new Rectangle2D(2020, 50, 100, 100), 1),
            Arguments.of(new Rectangle2D(2020, 70, 100, 100), 1),
            Arguments.of(new Rectangle2D(1970, -50, 100, 100), 0),
            Arguments.of(new Rectangle2D(2170, -50, 100, 100), 1),
            Arguments.of(new Rectangle2D(2020, 1150, 100, 100), 1),
            Arguments.of(new Rectangle2D(2020, 1170, 100, 100), 0),
            Arguments.of(new Rectangle2D(1970, 1250, 100, 100), 0),
            Arguments.of(new Rectangle2D(2170, 1250, 100, 100), 1)
        );
    }

    @BeforeEach
    public void setUp() {
        ((StubToolkit) Toolkit.getToolkit()).setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200, 0, 0, 1920, 1172, 96),
                new ScreenConfiguration(1920, 160, 1440, 900,
                                        1920, 160, 1440, 900, 96));
    }

    @AfterEach
    public void tearDown() {
        ((StubToolkit) Toolkit.getToolkit()).resetScreens();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void test(Rectangle2D rectangle, int expectedScreenIndex) {
        final Screen selectedScreen = Utils.getScreenForRectangle(rectangle);
        assertEquals(expectedScreenIndex,
                     Screen.getScreens().indexOf(selectedScreen));
    }
}
