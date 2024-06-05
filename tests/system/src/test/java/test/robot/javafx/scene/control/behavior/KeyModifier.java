/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package test.robot.javafx.scene.control.behavior;

import javafx.scene.input.KeyCode;
import com.sun.javafx.PlatformUtil;

/**
 * Key Modifiers for use in behavior tests.
 */
public enum KeyModifier {
    ALT,
    CTRL,
    COMMAND,
    META,
    OPTION,
    SHIFT,
    SHORTCUT;

    public static KeyCode findAlt(KeyModifier[] modifiers) {
        for (KeyModifier m : modifiers) {
            switch (m) {
            case ALT:
                return KeyCode.ALT;
            case OPTION:
                if(PlatformUtil.isMac()) {
                    return KeyCode.ALT;
                }
                break;
            }
        }
        return null;
    }

    public static KeyCode findCtrl(KeyModifier[] modifiers) {
        for (KeyModifier m : modifiers) {
            switch (m) {
            case CTRL:
                return KeyCode.CONTROL;
            case SHORTCUT:
                if (!PlatformUtil.isMac()) {
                    return KeyCode.CONTROL;
                }
                break;
            }
        }
        return null;
    }

    public static KeyCode findMeta(KeyModifier[] modifiers) {
        for (KeyModifier m : modifiers) {
            switch (m) {
            case META:
                return KeyCode.META;
            case COMMAND:
            case SHORTCUT:
                if (PlatformUtil.isMac()) {
                    return KeyCode.COMMAND;
                }
                break;
            }
        }
        return null;
    }

    public static KeyCode findShift(KeyModifier[] modifiers) {
        for (KeyModifier m : modifiers) {
            switch (m) {
            case SHIFT:
                return KeyCode.SHIFT;
            }
        }
        return null;
    }
}
