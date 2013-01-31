/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.behavior;

import static com.sun.javafx.scene.control.behavior.OptionalBoolean.ANY;
import static com.sun.javafx.scene.control.behavior.OptionalBoolean.FALSE;
import static com.sun.javafx.scene.control.behavior.OptionalBoolean.TRUE;
import javafx.event.EventType;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Just like regular key binding but the specificity is adjusted depending
 * on the orientation of the control.
 */
public abstract class OrientedKeyBinding extends KeyBinding {
    private OptionalBoolean vertical = FALSE;

    public OrientedKeyBinding(KeyCode code, String action) {
        super(code, action);
    }

    public OrientedKeyBinding(KeyCode code, EventType<KeyEvent> type, String action) {
        super(code, type, action);
    }

    public OrientedKeyBinding vertical() {
        vertical = TRUE;
        return this;
    }

    protected abstract boolean getVertical(Control control);

    @Override public int getSpecificity(Control control, KeyEvent event) {
        // If the control's vertical property does not match the binding's
        // vertical property then this binding does not apply
        final boolean verticalControl = getVertical(control);
        if (!vertical.equals(verticalControl)) return 0;
        // Delegate to super to compute the specificity
        final int s = super.getSpecificity(control, event);
        // If the super implementation says it is not a match, then it is not a
        // match and we should return 0
        if (s == 0) return 0;
        // Otherwise it was a match so return s + 1
        return (vertical != ANY) ? s + 1 : s;
    }

    @Override public String toString() {
        return "OrientedKeyBinding [code=" + getCode() + ", shift=" + getShift() +
                ", ctrl=" + getCtrl() + ", alt=" + getAlt() + ", shortcut=" + getShortcut() +
                ", type=" + getType() + ", vertical=" + vertical +
                ", action=" + getAction() + "]";
    }
}

