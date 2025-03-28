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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import test.com.sun.glass.ui.monocle.TestRunnable;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import test.robot.com.sun.glass.ui.monocle.input.devices.TestTouchDevices;

public final class InputDevicePropertyTest extends ParameterizedTestBase {

    private static Collection<TestTouchDevice> parameters() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void checkPlatform(TestTouchDevice device) throws Exception {
        createDevice(device, null);
        Assumptions.assumeTrue(TestApplication.isMonocle() || TestApplication.isLens());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testTouch(TestTouchDevice device) throws Exception {
        checkPlatform(device);
        TestRunnable.invokeAndWait(() -> Assertions.assertTrue(Platform.isSupported(ConditionalFeature.INPUT_TOUCH)));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testMultiTouch(TestTouchDevice device) throws Exception {
        checkPlatform(device);
        TestRunnable.invokeAndWait(() -> Assertions.assertEquals(device.getPointCount() > 1,
                            Platform.isSupported(
                                    ConditionalFeature.INPUT_MULTITOUCH)));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testPointer(TestTouchDevice device) throws Exception {
        checkPlatform(device);
        TestRunnable.invokeAndWait(() -> Assertions.assertFalse(
                Platform.isSupported(ConditionalFeature.INPUT_POINTER)));
    }
}
