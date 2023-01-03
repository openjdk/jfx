/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.security.AllPermission;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

class LinuxInputDeviceRegistry extends InputDeviceRegistry {

    LinuxInputDeviceRegistry(boolean headless) {
        if (headless) {
            // Keep the registry but do not bind it to udev.
            return;
        }
        Map<File, LinuxInputDevice> deviceMap = new HashMap<>();
        UdevListener udevListener = (action, event) -> {
            String subsystem = event.get("SUBSYSTEM");
            String devPath = event.get("DEVPATH");
            String devName = event.get("DEVNAME");
            if (subsystem != null && subsystem.equals("input")
                    && devPath != null && devName != null) {
                try {
                    File sysPath = new File("/sys", devPath);
                    if (action.equals("add")
                            || (action.equals("change")
                            && !deviceMap.containsKey(sysPath))) {
                        File devNode = new File(devName);
                        LinuxInputDevice device = createDevice(
                                devNode, sysPath, event);
                        if (device != null) {
                            deviceMap.put(sysPath, device);
                        }
                    } else if (action.equals("remove")) {
                        LinuxInputDevice device = deviceMap.get(sysPath);
                        deviceMap.remove(sysPath);
                        if (device != null) {
                            devices.remove(device);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Udev.getInstance().addListener(udevListener);
        // Request updates for existing devices
        SysFS.triggerUdevNotification("input");
    }

    private LinuxInputDevice createDevice(File devNode,
                                          File sysPath,
                                          Map<String, String> udevManifest)
            throws IOException {
        LinuxInputDevice device = new LinuxInputDevice(
                devNode, sysPath, udevManifest);
        return addDeviceInternal(device, "Linux input: " + devNode.toString());
    }

    LinuxInputDevice addDevice(LinuxInputDevice device, String name) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new AllPermission());
        }
        return addDeviceInternal(device, name);
    }

    private LinuxInputDevice addDeviceInternal(LinuxInputDevice device, String name) {
        LinuxInputProcessor processor = createInputProcessor(device);
        if (processor == null) {
            return null;
        } else {
            device.setInputProcessor(processor);
            Thread thread = new Thread(device);
            thread.setName(name);
            thread.setDaemon(true);
            thread.start();
            devices.add(device);
            return device;
        }
    }

    void removeDevice(LinuxInputDevice device) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new AllPermission());
        }
        devices.remove(device);
    }

    private LinuxInputProcessor createInputProcessor(LinuxInputDevice device) {
        if (device.isTouch()) {
            BitSet absCaps = device.getCapability("abs");
            boolean isMT = absCaps.get(LinuxInput.ABS_MT_POSITION_X)
                    && absCaps.get(LinuxInput.ABS_MT_POSITION_Y);
            if (isMT) {
                if (absCaps.get(LinuxInput.ABS_MT_TRACKING_ID)) {
                    return new LinuxStatefulMultiTouchProcessor(device);
                } else {
                    return new LinuxStatelessMultiTouchProcessor(device);
                }
            } else {
                return new LinuxSimpleTouchProcessor(device);
            }
        } else if (device.isRelative()) {
            return new LinuxMouseProcessor();
        } else {
            BitSet keyCaps = device.getCapability("key");
            if (keyCaps != null && !keyCaps.isEmpty()) {
                return new LinuxKeyProcessor();
            } else {
                return null;
            }
        }
    }

}
