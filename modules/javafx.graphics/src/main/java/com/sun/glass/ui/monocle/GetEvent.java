/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A test entry point for Monacle to show what events are read from input
 * devices.
 */
class GetEvent {

    private static Set<File> devices = new HashSet<>();

    private static UdevListener udevListener = (action, event) -> {
        String subsystem = event.get("SUBSYSTEM");
        String devPath = event.get("DEVPATH");
        if (subsystem != null && subsystem.equals("input")
                && devPath != null) {

            // show the udev event properties
            System.out.format("%1$ts.%1$tL Received UEVENT:\n",
                    new Date());
            List<String> keys = new ArrayList<>(event.keySet());
            Collections.sort(keys);
            for (String key: keys) {
                System.out.format("  %s='%s'\n", key, event.get(key));
            }

            // process the event
            try {
                File sysPath = new File("/sys", devPath);
                String devNode = event.get("DEVNAME");
                if (devNode == null) {
                    return;
                }
                if (action.equals("add")
                        || (action.equals("change")
                            && !devices.contains(sysPath))) {
                    LinuxInputDevice device = new LinuxInputDevice(
                            new File(devNode), sysPath, event);
                    device.setInputProcessor(new LinuxInputProcessor.Logger());
                    Thread thread = new Thread(device);
                    thread.setName(devNode.toString());
                    thread.setDaemon(true);
                    thread.start();
                    System.out.println("Added device " + devNode);
                    System.out.println("  touch=" + device.isTouch());
                    System.out.println("  multiTouch=" + device.isMultiTouch());
                    System.out.println("  relative=" + device.isRelative());
                    System.out.println("  5-way=" + device.is5Way());
                    System.out.println("  fullKeyboard=" + device.isFullKeyboard());
                    System.out.println("  PRODUCT=" + device.getProduct());
                    for (short axis = 0; axis < LinuxInput.ABS_MAX; axis++) {
                        LinuxAbsoluteInputCapabilities caps =
                                device.getAbsoluteInputCapabilities(axis);
                        if (caps != null) {
                            String axisName = LinuxInput.codeToString("EV_ABS", axis);
                            System.out.format("  ABSVAL %s %d\n",
                                              axisName, caps.getValue());
                            System.out.format("  ABSMIN %s %d\n",
                                              axisName, caps.getMinimum());
                            System.out.format("  ABSMAX %s %d\n",
                                              axisName, caps.getMaximum());
                            System.out.format("  ABSFUZZ %s %d\n",
                                              axisName, caps.getFuzz());
                            System.out.format("  ABSFLAT %s %d\n",
                                              axisName, caps.getFlat());
                            System.out.format("  ABSRES %s %d\n",
                                              axisName, caps.getResolution());
                        }
                    }
                    devices.add(sysPath);
                } else if (action.equals("remove")) {
                    devices.remove(sysPath);
                }
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
            }
        }
    };

    public static void main(String[] argv) throws Exception {
        NativePlatform platform = NativePlatformFactory.getNativePlatform();
        Udev.getInstance().addListener(udevListener);
        // Request updates for existing devices
        SysFS.triggerUdevNotification("input");
        new Thread(platform.getRunnableProcessor()).start();
    }

}
