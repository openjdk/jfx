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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class Keystroke {
    private BooleanProperty shift;

    public final void setShift(boolean value) {
        shiftProperty().set(value);
    }

    public final boolean isShift() {
        return shift == null ? false : shift.get();
    }

    public final BooleanProperty shiftProperty() {
        if (shift == null) {
            shift = new SimpleBooleanProperty(this, "shift");
        }
        return shift;
    }
    private BooleanProperty control;

    public final void setControl(boolean value) {
        controlProperty().set(value);
    }

    public final boolean isControl() {
        return control == null ? false : control.get();
    }

    public final BooleanProperty controlProperty() {
        if (control == null) {
            control = new SimpleBooleanProperty(this, "control");
        }
        return control;
    }
    private BooleanProperty alt;

    public final void setAlt(boolean value) {
        altProperty().set(value);
    }

    public final boolean isAlt() {
        return alt == null ? false : alt.get();
    }

    public final BooleanProperty altProperty() {
        if (alt == null) {
            alt = new SimpleBooleanProperty(this, "alt");
        }
        return alt;
    }
    private BooleanProperty meta;

    public final void setMeta(boolean value) {
        metaProperty().set(value);
    }

    public final boolean isMeta() {
        return meta == null ? false : meta.get();
    }

    public final BooleanProperty metaProperty() {
        if (meta == null) {
            meta = new SimpleBooleanProperty(this, "meta");
        }
        return meta;
    }
    private ObjectProperty<KeyCode> code;

    public final void setCode(KeyCode value) {
        codeProperty().set(value);
    }

    public final KeyCode getCode() {
        return code == null ? null : code.get();
    }

    public final ObjectProperty<KeyCode> codeProperty() {
        if (code == null) {
            code = new SimpleObjectProperty<KeyCode>(this, "code");
        }
        return code;
    }
    private ObjectProperty<EventType<KeyEvent>> eventType;

    public final void setEventType(EventType<KeyEvent> value) {
        eventTypeProperty().set(value);
    }

    public final EventType<KeyEvent> getEventType() {
        return eventType == null ? KeyEvent.KEY_PRESSED : eventType.get();
    }

    public final ObjectProperty<EventType<KeyEvent>> eventTypeProperty() {
        if (eventType == null) {
            eventType = new SimpleObjectProperty<EventType<KeyEvent>>(this, "eventType");
        }
        return eventType;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is equal to the {@code obj} argument; {@code false} otherwise.
     */
    @Override public boolean equals(Object o) {
        if (o instanceof Keystroke) {
            Keystroke other = (Keystroke) o;
            return this.isShift() == other.isShift() && isControl() == other.isControl() && this.isAlt() == other.isAlt() && isMeta() == other.isMeta() && getCode().equals(other.getCode()) && getEventType().equals(other.getEventType());
        }
        return false;
    }

    /**
     * Returns a hash code for this {@code Keystroke} object.
     * @return a hash code for this {@code Keystroke} object.
     */ 
    @Override public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Boolean.valueOf(isShift()).hashCode();
        hash = 31 * hash + Boolean.valueOf(isControl()).hashCode();
        hash = 31 * hash + Boolean.valueOf(isAlt()).hashCode();
        hash = 31 * hash + Boolean.valueOf(isMeta()).hashCode();
        hash = 31 * hash + getCode().hashCode();
        hash = 31 * hash + getEventType().hashCode();
        return hash;
    }

    /**
     * Returns a string representation of this {@code Keystroke} object.
     * @return a string representation of this {@code Keystroke} object.
     */ 
    @Override public String toString() {
        return "Keystroke [code=" + getCode() + ", shift=" + isShift() + ", control=" + isControl() + ", alt=" + isAlt() + ", meta=" + isMeta() + ", type=" + getEventType() + "]";
    }

}
