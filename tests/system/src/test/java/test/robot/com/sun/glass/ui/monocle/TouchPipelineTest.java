/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.monocle.TestLogShim;
import test.robot.com.sun.glass.ui.monocle.TestApplication;
import com.sun.glass.ui.monocle.TouchFilterShim.FlushingFilter;
import com.sun.glass.ui.monocle.TouchFilterShim.LoggingFilter;
import com.sun.glass.ui.monocle.TouchFilterShim.NoMultiplesOfTenOnXFilter;
import com.sun.glass.ui.monocle.TouchFilterShim.OverrideIDFilter;
import com.sun.glass.ui.monocle.TouchFilterShim.TranslateFilter;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Test installation of custom touch filters
 */
public class TouchPipelineTest extends TouchTestBase {

    @Before
    public void createDevice() throws Exception {
        Assume.assumeTrue(TestApplication.isMonocle());
        ui = new UInput();
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.addTouchListeners();
        TestLogShim.reset();
        System.setProperty("monocle.input.ca/fe/ba/be.touchFilters",
                           TranslateFilter.class.getName() + ","
                           + OverrideIDFilter.class.getName() + ","
                           + FlushingFilter.class.getName() + ","
                           + LoggingFilter.class.getName() + ","
                           + NoMultiplesOfTenOnXFilter.class.getName());
        Rectangle2D r = Screen.getPrimary().getBounds();
        ui.processLine("OPEN");
        ui.processLine("EVBIT EV_SYN");
        ui.processLine("EVBIT EV_KEY");
        ui.processLine("KEYBIT BTN_TOUCH");
        ui.processLine("EVBIT EV_ABS");
        ui.processLine("ABSBIT ABS_X");
        ui.processLine("ABSBIT ABS_Y");
        ui.processLine("ABSMIN ABS_X 0");
        ui.processLine("ABSMAX ABS_X " + (int) (r.getWidth() - 1));
        ui.processLine("ABSMIN ABS_Y 0");
        ui.processLine("ABSMAX ABS_Y " + (int) (r.getHeight() - 1));
        ui.processLine("PROPBIT INPUT_PROP_POINTER");
        ui.processLine("PROPBIT INPUT_PROP_DIRECT");
        ui.processLine("PROPERTY ID_INPUT_TOUCHSCREEN 1");
        ui.processLine("BUS 0xCA");
        ui.processLine("VENDOR 0xFE");
        ui.processLine("PRODUCT 0xBA");
        ui.processLine("VERSION 0xBE");
        ui.processLine("CREATE");
    }


    @Test
    public void testFilters() throws Exception {
        ui.processLine("EV_KEY BTN_TOUCH 1");
        ui.processLine("EV_ABS ABS_X 195");
        ui.processLine("EV_ABS ABS_Y 200");
        ui.processLine("EV_SYN");
        ui.processLine("EV_ABS ABS_X 200");
        ui.processLine("EV_ABS ABS_Y 200");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Touch pressed: 203, 195");
        TestLogShim.waitForLog("Touch point id=5 at 203,195");
        // Check for events send by the flushing filter
        TestLogShim.waitForLog("Touch moved: 413, 95");
        TestLogShim.waitForLog("Touch moved: 313, 95");
        TestLogShim.waitForLog("Touch moved: 213, 95");
        // This one should have been filtered out
        Assert.assertEquals(0, TestLogShim.countLog("Touch Pressed: 208, 195"));
        ui.processLine("EV_KEY BTN_TOUCH 0");
        ui.processLine("EV_SYN");
        TestLogShim.waitForLog("Touch released: 213, 95");
    }

}

