/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.skin;

import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public class KeystrokeUtils {

    public static String toString(final KeyCombination kc) {
        if (kc == null) {
            return "";
        }
        
        StringBuilder stringBuilder = new StringBuilder();
        if (com.sun.javafx.PlatformUtil.isMac()) {
            // Macs have a different convention for keyboard accelerators -
            // no pluses to separate modifiers, and special symbols for
            // each modifier (in a particular order), etc
            if (kc.getControl() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("\u2303");
            }
            if (kc.getAlt() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("\u2325");
            }
            if (kc.getShift() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("\u21e7");
            }
            if (kc.getMeta() == KeyCombination.ModifierValue.DOWN || kc.getShortcut() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("\u2318");
            }
            // TODO refer to RT-14486 for remaining glyphs
        }
        else {
            if (kc.getControl() == KeyCombination.ModifierValue.DOWN || kc.getShortcut() == KeyCombination.ModifierValue.DOWN ) {
                stringBuilder.append("Ctrl+");
            }
            if (kc.getAlt() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("Alt+");
            }
            if (kc.getShift() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("Shift+");
            }
            if (kc.getMeta() == KeyCombination.ModifierValue.DOWN) {
                stringBuilder.append("Meta+");
            }
        }
        
        if (kc instanceof KeyCodeCombination) {
            stringBuilder.append(KeyCodeUtils.getAccelerator(((KeyCodeCombination)kc).getCode()));
        } else if (kc instanceof KeyCharacterCombination) {
            stringBuilder.append(((KeyCharacterCombination)kc).getCharacter());
        }

        return stringBuilder.toString();
    }
}
