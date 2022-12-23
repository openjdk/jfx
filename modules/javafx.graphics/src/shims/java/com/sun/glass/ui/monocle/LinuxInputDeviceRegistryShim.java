/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.monocle;

import java.nio.channels.ReadableByteChannel;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class LinuxInputDeviceRegistryShim {

    private static LinuxInputDeviceRegistry registry
            = (LinuxInputDeviceRegistry) NativePlatformFactory.getNativePlatform().getInputDeviceRegistry();

    public static class LinuxInputDeviceShim {

        LinuxInputDevice device;

        private LinuxInputDeviceShim(LinuxInputDevice d) {
            device = d;
        }

        public boolean isQuiet() {
            return device.isQuiet();
        }

    }

    private static final int INDEX_VALUE = 0;
    private static final int INDEX_MIN = 1;
    private static final int INDEX_MAX = 2;
    private static final int INDEX_FUZZ = 3;
    private static final int INDEX_FLAT = 4;
    private static final int INDEX_RESOLUTION = 5;
    private static final int INDEX_COUNT = 6;

    static Map<Integer, LinuxAbsoluteInputCapabilities> createAbsCapsMap(Map<Integer, int[]> absCaps) {
        Map<Integer, LinuxAbsoluteInputCapabilities> map
                = new HashMap<>();
        for (Integer axis : absCaps.keySet()) {
            int[] a = absCaps.get(axis);
            if (a != null) {
                LinuxAbsoluteInputCapabilities absCap = new LinuxAbsoluteInputCapabilities(
                        a[INDEX_VALUE],
                        a[INDEX_MAX],
                        a[INDEX_MIN],
                        a[INDEX_FUZZ],
                        a[INDEX_FLAT],
                        a[INDEX_RESOLUTION]);
                map.put(axis, absCap);
            }
        }
        return map;
    }

    public static LinuxInputDeviceShim addDevice(
            Map<String, BitSet> capabilities,
            Map<Integer, int[]> absCaps,
            ReadableByteChannel in,
            Map<String, String> udevManifest,
            Map<String, String> uevent,
            String name) {
        LinuxInputDevice device = new LinuxInputDevice(capabilities,
                createAbsCapsMap(absCaps),
                in,
                udevManifest,
                uevent);

        registry.addDevice(
                device,
                name);

        return new LinuxInputDeviceShim(device);
    }

    public static void removeDevice(LinuxInputDeviceShim device) {
        registry.removeDevice(device.device);
    }

}
