/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.input;

import com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import com.sun.javafx.PlatformUtil;
import org.junit.*;

/**
 * Scroll tests that are checking scroll threshold
 */
public class ScrollThresholdTest extends ScrollTestBase {

    private int delta;

    public ScrollThresholdTest(TestTouchDevice device) {
        super(device);
    }

    @BeforeClass
    public static void beforeInit() {
        int threshold =
                Integer.getInteger("com.sun.javafx.gestures.scroll.threshold", 10);
        Assume.assumeTrue(threshold > 1);
        System.setProperty("monocle.input.touchRadius",
                Integer.toString(threshold - 2));
    }

    @Before
    public void init() {
        super.init();
        Assume.assumeTrue(device.getTapRadius() < getScrollThreshold());
        delta = getScrollThreshold() - 1;
        Assume.assumeTrue(!PlatformUtil.isMac());
        Assume.assumeTrue(!PlatformUtil.isWindows());
    }

    /**
     * Tap one finger, drag it less then threshold, scroll shouldn't happen
     */
    @Test
    public void testMoveUpCheckThreshold() throws Exception {
        pressFirstFinger();
        moveOneFinger(0, -delta , 1, true);
        releaseFirstFinger();
        tapToStopInertia();
    }

    /**
     * Tap one finger, drag it less then threshold - scroll shouldn't happen,
     * drag it again (pass the threshold) - verify scrolling
     */
    @Test
    public void testMoveDownCheckThreshold() throws Exception {
        pressFirstFinger();
        moveOneFinger(0, delta , 3, true);
        releaseFirstFinger();
        tapToStopInertia();
    }
}
