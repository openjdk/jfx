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

import java.security.AccessController;
import java.security.PrivilegedAction;

/** Native platform compatible with X11
 *
 */
class X11Platform extends NativePlatform {

    private final boolean x11Input;

    @SuppressWarnings("removal")
    X11Platform() {
        LinuxSystem.getLinuxSystem().loadLibrary();
        x11Input = AccessController.doPrivileged((PrivilegedAction<Boolean>)
                () -> Boolean.getBoolean("x11.input"));
    }

    /** Create the appropriate input device registry - if the system property
     * x11.input is true, then we use X11InputDeviceRegistry and get native
     * events via the X11 event queue.  If x11.input is not true, we listen
     * directly to the udev raw events.
     */
    @Override
    protected InputDeviceRegistry createInputDeviceRegistry() {
        if (x11Input) {
            return new X11InputDeviceRegistry();
        } else {
            return new LinuxInputDeviceRegistry(false);
        }
    }

    /** Create the appropriate X11 cursor.  If we are using x11 input, we let
     * X11 handle the cursor movement.  If we are using udev input, we need
     * to handle the cursor movement ourselves via X11WarpingCursor.
     */
    @Override
    protected NativeCursor createCursor() {
        if (useCursor) {
            final NativeCursor c = x11Input ? new X11Cursor() : new X11WarpingCursor();
            return logSelectedCursor(c);
        } else {
            return logSelectedCursor(new NullCursor());
        }
    }

    /** Create the native screen for this platform
     */
    @Override
    protected NativeScreen createScreen() {
        return new X11Screen(x11Input);
    }

    /** Create the accelerated screen for this platform
     *
     * @param attributes a sequence of pairs (GLAttibute, value)
     * @throws GLException
     */
    @Override
    public synchronized AcceleratedScreen getAcceleratedScreen(
            int[] attributes) throws GLException {
        if (accScreen == null) {
            accScreen = new X11AcceleratedScreen(attributes);
        }
        return accScreen;
    }
}
