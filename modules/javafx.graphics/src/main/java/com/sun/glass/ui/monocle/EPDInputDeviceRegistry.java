/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains an observable set of input devices. This class is responsible for
 * detecting the attached input devices and generating their input events. Run
 * the following commands as <i>root</i> to list the properties of the keypad
 * (event0) and touch screen (event1) input devices on the system:
 * <pre>{@code
 * # udevadm info -q all -n /dev/input/event0
 * # udevadm info -q all -n /dev/input/event1
 * }</pre>
 *
 * @implNote {@code EPDPlatform} creates an instance of this class instead of
 * {@code LinuxInputDeviceRegistry} because this class replaces two of its
 * methods.
 * <p>
 * It replaces the {@link #createDevice} method to work around bug JDK-8201568
 * by opening the device before creating its {@code LinuxInputDevice}. It also
 * replaces the {@link #addDeviceInternal} method to work around older versions
 * of <i>udev</i>, such as version 142, which do not provide the
 * ID_INPUT_TOUCHSCREEN=1 property for the touch screen device.
 * {@link LinuxInputDevice#isTouch} requires that property and value; otherwise
 * the method returns {@code false}, and the touch screen is mistakenly assigned
 * a keyboard input processor. Newer versions of <i>udev</i>, such as version
 * 204, provide the correct property and value.</p>
 * <p>
 * Therefore, once JDK-8201568 is fixed and the old version of <i>udev</i> is no
 * longer in use, this entire class can be removed and replaced by
 * {@code LinuxInputDeviceRegistry}.</p>
 */
class EPDInputDeviceRegistry extends InputDeviceRegistry {

    /**
     * The file name of the keypad input device.
     */
    private static final String KEYPAD_FILENAME = "event0";

    /**
     * The file name of the touch screen input device.
     */
    private static final String TOUCH_FILENAME = "event1";

    /**
     * Creates a new observable set of input devices.
     *
     * @implNote This is a verbatim copy of the {@link LinuxInputDeviceRegistry}
     * constructor.
     *
     * @param headless {@code true} if this environment cannot support a
     * display, keyboard, and mouse; otherwise {@code false}
     */
    EPDInputDeviceRegistry(boolean headless) {
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

    /**
     * Creates a Linux input device with the given properties.
     *
     * @implNote Works around bug
     * <a href="https://bugs.openjdk.java.net/browse/JDK-8201568">JDK-8201568</a>,
     * "zForce touchscreen input device fails when closed and immediately
     * reopened," by opening the device before creating its
     * {@code LinuxInputDevice}.
     *
     * @param devNode the file representing the device name, such as
     * <i>/dev/input/event1</i>
     * @param sysPath the system path to the device, such as
     * <i>/sys/devices/virtual/input/input1/event1</i>
     * @param udevManifest the set of properties for the device
     * @return the new Linux input device, or {@code null} if no processor is
     * found for the device
     * @throws IOException if an error occurs opening the device
     */
    private LinuxInputDevice createDevice(File devNode, File sysPath,
            Map<String, String> udevManifest) throws IOException {
        LinuxSystem system = LinuxSystem.getLinuxSystem();
        system.open(devNode.getPath(), LinuxSystem.O_RDONLY);

        var device = new LinuxInputDevice(devNode, sysPath, udevManifest);
        return addDeviceInternal(device, "Linux input: " + devNode.toString());
    }

    /**
     * Creates an input processor for the device which runs on a new daemon
     * background thread. Run the following commands as <i>root</i> to display
     * the events generated by the keypad (0) and touch screen (1) input devices
     * when you press buttons or touch the screen:
     * <pre>{@code
     * # input-events 0
     * # input-events 1
     * }</pre>
     *
     * @implNote The "mxckpd" keypad device driver does not generate EV_SYN
     * events, yet the {@link LinuxInputDevice#run} method schedules an event
     * for processing only after receiving the EV_SYN event terminator (see the
     * {@link LinuxEventBuffer#put} method). The events from this device,
     * therefore, are never delivered to the JavaFX application. The "gpio-keys"
     * keypad device driver on more recent systems, though, correctly generates
     * the EV_SYN event terminator.
     *
     * @param device the Linux input device
     * @param name the device name, such as <i>/dev/input/event0</i>
     * @return the Linux input device, or {@code null} if no input processor is
     * found for the device
     */
    private LinuxInputDevice addDeviceInternal(LinuxInputDevice device, String name) {
        LinuxInputProcessor processor = null;
        if (name.endsWith(KEYPAD_FILENAME)) {
            processor = new LinuxKeyProcessor();
        } else if (name.endsWith(TOUCH_FILENAME)) {
            processor = new LinuxSimpleTouchProcessor(device);
        }
        if (processor == null) {
            return null;
        } else {
            device.setInputProcessor(processor);
            var thread = new Thread(device);
            thread.setName(name);
            thread.setDaemon(true);
            thread.start();
            devices.add(device);
            return device;
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}[devices={1}]", getClass().getName(), devices);
    }
}
