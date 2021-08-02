/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.com.sun.glass.ui.monocle.input.devices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TestTouchDevices {

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
                                    Class.forName(selectedDeviceClass).getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return devices;
        }
        devices.addAll(Arrays.asList(new TestTouchDevice[] {
                new SingleTouchDevice1(),
                new SingleTouchDevice2(),
                new EGalaxSingleTouchDevice1(),
                new EGalaxSingleTouchDevice2(),
                new EGalaxMultiTouchDevice1(),
                new EGalaxMultiTouchDevice2(),
                new EGalaxMultiTouchDevice3(),
                new EGalaxMultiTouchDevice4(),
                new EGalaxMultiTouchDevice5(),
                new EGalaxMultiTouchDevice6(),
                new TouchRevolutionFusionDevice(),
                new NTrigDevice(),
                new SamsungLMS700KF07004Device(),
                new TabletDevice(),
                new DellP2714TDevice(),
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

}
