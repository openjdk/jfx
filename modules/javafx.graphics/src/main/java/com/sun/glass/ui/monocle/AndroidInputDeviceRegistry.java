/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.monocle.TouchState.Point;
import java.security.AllPermission;
import javafx.application.Platform;


public class AndroidInputDeviceRegistry extends InputDeviceRegistry {

    private static AndroidInputDeviceRegistry instance = new AndroidInputDeviceRegistry();
    private AndroidInputDevice androidDevice;
    private AndroidInputProcessor processor;
    private final KeyState keyState = new KeyState();

    static AndroidInputDeviceRegistry getInstance() {
        return instance;
    }

    public static void registerDevice() {
        Platform.runLater(() -> instance.createDevice());
    }

    public static void gotTouchEventFromNative(int count, int[] actions, int[] ids, int[] x, int[] y, int primary) {
        TouchState touchState = new TouchState();

        if (primary == -1) {
           System.out.println("don't add points, primary = -1");
        }
        else {
            for (int i = 0; i < count; i++) {
                Point p = new Point();
                p.id = ids[i];
                p.x = x[i];
                p.y = y[i];
                touchState.addPoint(p);
            }
        }
        Platform.runLater(() -> instance.gotTouchEvent(touchState));
    }

    private void gotTouchEvent(TouchState touchState) {
        if (androidDevice == null) {
            System.out.println("[MON] got touch event, but no registered device yet");
            Thread.dumpStack();
            return;
        }
        if (processor == null) {
            System.out.println("[MON] got touch event, but processor not yet initialized");
            Thread.dumpStack();
            return;
        }
        processor.pushEvent(touchState);
    }


    public static void dispatchKeyEventFromNative(int type, int key, char[] chars, int modifiers) {
        instance.processor.dispatchKeyEvent(type, key, chars, modifiers);
    }

    public static void gotKeyEventFromNative(int action, int linuxKey) {
        instance.gotKeyEvent (action, linuxKey);
    }

    private void gotKeyEvent(int action, int lk) {
        int vk = LinuxKeyProcessor.getVirtualKeyCode(lk);
        if (action == 0) {
            keyState.pressKey(vk);
        }
        else if (action ==1) {
            keyState.releaseKey(vk);
        }
        else {
            System.out.println("[JVDBG] ERROR, what action is this? "+action);
        }
        instance.gotKeyEvent(keyState);
    }

    private void gotKeyEvent(KeyState keyState) {
        processor.pushKeyEvent(keyState);
    }

    private AndroidInputDeviceRegistry() {
    }

    private void createDevice() {
        System.out.println("[MON] Create device");
        AndroidInputDevice device = new AndroidInputDevice();
        androidDevice = addDeviceInternal(device, "Android Touch Input");
        System.out.println("[MON] Create device done, add done");
    }

    private AndroidInputDevice addDeviceInternal(AndroidInputDevice device, String name) {
        processor = createInputProcessor(device);

        device.setInputProcessor(processor);
        Thread thread = new Thread(device);
        thread.setName(name);
        thread.setDaemon(true);
        thread.start();
        devices.add(device);

        return device;

    }

    void removeDevice(AndroidInputDevice device) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new AllPermission());
        }
        devices.remove(device);
    }

    private AndroidInputProcessor createInputProcessor(AndroidInputDevice device) {
        return new AndroidInputProcessor(device);
    }

}
