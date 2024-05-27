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

import java.util.Collection;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import junit.framework.Assert;
import test.com.sun.glass.ui.monocle.TestRunnable;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

public class InputDevicePropertyTest  extends ParameterizedTestBase {

    public InputDevicePropertyTest(TestTouchDevice device) {
        super(device);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    @Before
    public void checkPlatform() throws Exception {
        Assume.assumeTrue(TestApplication.isMonocle() || TestApplication.isLens());
    }

    @Test
    public void testTouch() throws Exception {
        TestRunnable.invokeAndWait(() -> Assert.assertTrue(Platform.isSupported(ConditionalFeature.INPUT_TOUCH)));
    }

    @Test
    public void testMultiTouch() throws Exception {
        TestRunnable.invokeAndWait(() -> Assert.assertEquals(device.getPointCount() > 1,
                            Platform.isSupported(
                                    ConditionalFeature.INPUT_MULTITOUCH)));
    }

    @Test
    public void testPointer() throws Exception {
        TestRunnable.invokeAndWait(() -> Assert.assertFalse(
                Platform.isSupported(ConditionalFeature.INPUT_POINTER)));
    }

}
