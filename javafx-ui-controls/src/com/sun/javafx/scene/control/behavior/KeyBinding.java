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

import com.sun.javafx.Utils;
import static com.sun.javafx.scene.control.behavior.OptionalBoolean.ANY;
import static com.sun.javafx.scene.control.behavior.OptionalBoolean.FALSE;
import static com.sun.javafx.scene.control.behavior.OptionalBoolean.TRUE;
import com.sun.javafx.tk.Toolkit;
import javafx.event.EventType;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * KeyBindings are used to describe which action should occur based on some
 * KeyEvent state and Control state. These bindings are used to populate the
 * keyBindings variable on BehaviorBase. The KeyBinding can be subclassed to
 * add additional matching criteria. A match in a subclass should always have
 * a specificity that is 1 greater than its superclass in the case of a match,
 * or 0 in the case where there is no match.
 *
 * Note that this API is, at present, quite odd in that you use a constructor
 * and then use shift(), ctrl(), alt(), or meta() separately. It gave me an
 * object-literal like approach but isn't ideal. We will want some builder
 * approach here (similar as in other places).
 */
public class KeyBinding {
    private KeyCode code;
    private EventType<KeyEvent> eventType = KeyEvent.KEY_PRESSED;
    private String action;
    private OptionalBoolean shift = FALSE;
    private OptionalBoolean ctrl = FALSE;
    private OptionalBoolean alt = FALSE;
    private OptionalBoolean meta = FALSE;

    public KeyBinding(KeyCode code, String action) {
        this.code = code;
        this.action = action;
    }

    public KeyBinding(KeyCode code, EventType<KeyEvent> type, String action) {
        this.code = code;
        this.eventType = type;
        this.action = action;
    }

    public KeyBinding shift() {
        return shift(TRUE);
    }

    public KeyBinding shift(OptionalBoolean value) {
        shift = value;
        return this;
    }

    public KeyBinding ctrl() {
        return ctrl(TRUE);
    }

    public KeyBinding ctrl(OptionalBoolean value) {
        ctrl = value;
        return this;
    }

    public KeyBinding alt() {
        return alt(TRUE);
    }

    public KeyBinding alt(OptionalBoolean value) {
        alt = value;
        return this;
    }
    
    public KeyBinding meta() {
        return meta(TRUE);
    }

    public KeyBinding meta(OptionalBoolean value) {
        meta = value;
        return this;
    }
    
    public KeyBinding shortcut() {
        if (Toolkit.getToolkit().getClass().getName().endsWith("StubToolkit")) {
            // FIXME: We've hit the terrible StubToolkit (which only appears 
            // during testing). We will dumb down what we do here
            if (Utils.isMac()) {
                return meta();
            } else {
                return ctrl();
            }
        } else {
            switch (Toolkit.getToolkit().getPlatformShortcutKey()) {
                case SHIFT:
                    return shift();

                case CONTROL:
                    return ctrl();

                case ALT:
                    return alt();

                case META:
                    return meta();

                default:
                    return this;
            }
        }
    }

    public final KeyCode getCode() { return code; }
    public final EventType<KeyEvent> getType() { return eventType; }
    public final String getAction() { return action; }
    public final OptionalBoolean getShift() { return shift; }
    public final OptionalBoolean getCtrl() { return ctrl; }
    public final OptionalBoolean getAlt() { return alt; }
    public final OptionalBoolean getMeta() { return meta; }

    public int getSpecificity(Control control, KeyEvent event) {
        int s = 0;
        if (code != null && code != event.getCode()) return 0; else s = 1;
        if (!shift.equals(event.isShiftDown())) return 0; else if (shift != ANY) s++;
        if (!ctrl.equals(event.isControlDown())) return 0; else if (shift != ANY) s++;
        if (!alt.equals(event.isAltDown())) return 0; else if (shift != ANY) s++;
        if (!meta.equals(event.isMetaDown())) return 0; else if (shift != ANY) s++;
        if (eventType != null && eventType != event.getEventType()) return 0; else s++;
        // We can now trivially accept it
        return s;
    }

    @Override public String toString() {
        return "KeyBinding [code=" + code + ", shift=" + shift +
                ", ctrl=" + ctrl + ", alt=" + alt + 
                ", meta=" + meta + ", type=" + eventType + 
                ", action=" + action + "]";
    }
}
