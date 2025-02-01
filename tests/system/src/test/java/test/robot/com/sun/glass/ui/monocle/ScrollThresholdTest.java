/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.com.sun.glass.ui.monocle;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;

/**
 * Scroll tests that are checking scroll threshold
 */
public class ScrollThresholdTest extends ScrollTestBase {

    private int delta;

    @BeforeAll
    public static void beforeInit() {
        int threshold =
                Integer.getInteger("com.sun.javafx.gestures.scroll.threshold", 10);
        Assumptions.assumeTrue(threshold > 1);
        System.setProperty("monocle.input.touchRadius",
                Integer.toString(threshold - 2));
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    @Override
    public void init(TestTouchDevice device) throws Exception {
        super.init(device);
        Assumptions.assumeTrue(device.getTapRadius() < getScrollThreshold());
        delta = getScrollThreshold() - 1;
    }

    /**
     * Tap one finger, drag it less then threshold, scroll shouldn't happen
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testMoveUpCheckThreshold(TestTouchDevice device) throws Exception {
        init(device);
        pressFirstFinger();
        moveOneFinger(0, -delta , 1, true);
        releaseFirstFinger();
        tapToStopInertia();
    }

    /**
     * Tap one finger, drag it less then threshold - scroll shouldn't happen,
     * drag it again (pass the threshold) - verify scrolling
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testMoveDownCheckThreshold(TestTouchDevice device) throws Exception {
        init(device);
        pressFirstFinger();
        moveOneFinger(0, delta , 3, true);
        releaseFirstFinger();
        tapToStopInertia();
    }
}
