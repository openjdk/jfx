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

package com.sun.glass.ui.monocle.input.devices;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TestTouchDevices {

    protected static AtomicReference<Rectangle2D> screen = new AtomicReference<>();

    public static List<TestTouchDevice> getTouchDevices() {
        List<TestTouchDevice> devices = new ArrayList<>();
        String selectedDeviceClass = System.getProperty("device");
        if (selectedDeviceClass != null) {
            if (!selectedDeviceClass.contains(".")) {
                selectedDeviceClass = "com.sun.glass.ui.monocle.input.devices."
                        + selectedDeviceClass;
            }
            try {
                devices.add((TestTouchDevice)
                                    Class.forName(selectedDeviceClass).newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return devices;
        }
        devices.addAll(Arrays.asList(new TestTouchDevice[] {
                new SingleTouchDevice1(),
                new SingleTouchDevice2(),
                new EGalaxSingleTouchDevice(),
                new EGalaxMultiTouchDevice1(),
                new EGalaxMultiTouchDevice2(),
                new TouchRevolutionFusionDevice(),
                new NTrigDevice(),
                new SamsungLMS700KF07004Device(),
                new TabletDevice()
        }));
        return devices;
    }

    public static List<TestTouchDevice> getTouchDevices(int minPoints) {
        return getTouchDevices().stream()
                .filter(d -> d.points.length >= minPoints)
                .collect(Collectors.toList());
    }

    public static Collection<Object[]> getTouchDeviceParameters(int minPoints) {
        Collection c = getTouchDevices().stream()
                .filter(d -> d.points.length >= minPoints)
                .map(d -> new Object[] { d })
                .collect(Collectors.toList());
        return c;
    }

    private static void fetchScreenBounds() {
        if (Platform.isFxApplicationThread()) {
            screen.set(Screen.getPrimary().getBounds());
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                screen.set(Screen.getPrimary().getBounds());
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static Rectangle2D getScreenBounds() {
        Rectangle2D r = screen.get();
        if (r == null) {
            fetchScreenBounds();
            r = screen.get();
        }
        return r;
    }

}
