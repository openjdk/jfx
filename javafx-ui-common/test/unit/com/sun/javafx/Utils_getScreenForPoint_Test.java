/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.pgstub.StubToolkit.ScreenConfiguration;
import com.sun.javafx.tk.Toolkit;
import java.util.Arrays;
import java.util.Collection;
import javafx.stage.Screen;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class Utils_getScreenForPoint_Test {
    private final double x;
    private final double y;
    private final int expectedScreenIndex;

    @Parameters
    public static Collection data() {
        return Arrays.asList(
                new Object[] {
                    config(100, 100, 0),
                    config(2000, 200, 1),
                    config(1920, 0, 0),
                    config(1920, 200, 1),
                    config(1920, 1100, 0),
                    config(2020, 50, 0),
                    config(2020, 70, 1),
                    config(1970, -50, 0),
                    config(2170, -50, 1),
                    config(2020, 1150, 1),
                    config(2020, 1170, 0),
                    config(1970, 1250, 0),
                    config(2170, 1250, 1)
                });
    }

    public Utils_getScreenForPoint_Test(
            final double x, final double y, final int expectedScreenIndex) {
        this.x = x;
        this.y = y;
        this.expectedScreenIndex = expectedScreenIndex;
    }

    @Before
    public void setUp() {
        ((StubToolkit) Toolkit.getToolkit()).setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200, 0, 0, 1920, 1172, 96),
                new ScreenConfiguration(1920, 160, 1440, 900,
                                        1920, 160, 1440, 900, 96));
    }

    @After
    public void tearDown() {
        ((StubToolkit) Toolkit.getToolkit()).resetScreens();
    }

    @Test
    public void test() {
        final Screen selectedScreen = Utils.getScreenForPoint(x, y);
        Assert.assertEquals(expectedScreenIndex,
                            Screen.getScreens().indexOf(selectedScreen));
    }

    private static Object[] config(final double x, final double y,
                                   final int expectedScreenIndex) {
        return new Object[] { x, y, expectedScreenIndex };
    }
}
