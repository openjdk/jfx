/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.linux;

import com.sun.glass.ui.monocle.input.InputDeviceRegistry;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

class LinuxInputDeviceRegistry extends InputDeviceRegistry {

    LinuxInputDeviceRegistry() {
        Map<File, LinuxInputDevice> deviceMap = new HashMap<>();
        UdevListener udevListener = new UdevListener() {
            @Override
            public void udevEvent(String action, Map<String, String> event) {
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
                                devices.add(device);
                            }
                        } else if (action.equals("remove")) {
                            LinuxInputDevice device = deviceMap.get(devPath);
                            deviceMap.remove(devPath);
                            if (device != null) {
                                devices.remove(device);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
        LinuxInputProcessor processor = createInputProcessor(device);
        if (processor == null) {
            return null;
        } else {
            device.setInputProcessor(createInputProcessor(device));
            Thread thread = new Thread(device);
            thread.setName("Linux input: " + devNode.toString());
            thread.setDaemon(true);
            thread.start();
            return device;
        }
    }

    private LinuxInputProcessor createInputProcessor(LinuxInputDevice device) {
        if (device.isTouch()) {
            return new LinuxTouchProcessor(device);
        } else if (device.isRelative()) {
            return new LinuxMouseProcessor();
        } else {
            BitSet keyCaps = device.getCapability("key");
            if (keyCaps != null && !keyCaps.isEmpty()) {
                System.err.println("TODO: implement LinuxKeyboardProcessor");
                return new LinuxInputProcessor.Logger();
            } else {
                return null;
            }
        }
    }

}
