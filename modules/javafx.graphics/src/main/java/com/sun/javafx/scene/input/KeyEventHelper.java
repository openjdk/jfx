/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.input;

import com.sun.javafx.util.Utils;
import javafx.scene.input.KeyEvent;

/**
 * Used to access internal methods of KeyEvent.
 */
public class KeyEventHelper {

    private static KeyEventAccessor keyEventAccessor;

    static {
        Utils.forceInit(KeyEvent.class);
    }

    private KeyEventHelper() {
    }

    public static void setHardwareCode(KeyEvent keyEvent, int hardwareCode) {
        keyEventAccessor.setHardwareCode(keyEvent, hardwareCode);
    }

    public static int getHardwareCode(KeyEvent keyEvent) {
        return keyEventAccessor.getHardwareCode(keyEvent);
    }

    public static void setKeyEventAccessor(final KeyEventAccessor newAccessor) {
        if (keyEventAccessor != null) {
            throw new IllegalStateException();
        }

        keyEventAccessor = newAccessor;
    }

    public interface KeyEventAccessor {
        void setHardwareCode(KeyEvent keyEvent, int hardwareCode);
        int getHardwareCode(KeyEvent keyEvent);
    }
}
