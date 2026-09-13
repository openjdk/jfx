/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.jfx.incubator.scene.control.input;

import javafx.event.EventType;
import javafx.scene.input.KeyEvent;
import jfx.incubator.scene.control.input.KeyBinding;

/**
 * Contains logic for mapping KeyBinding to a specific KeyEvent.
 */
public class KeyEventMapper {
    private static final int PRESSED = 0x01;
    private static final int RELEASED = 0x02;
    private static final int TYPED = 0x04;

    private int types;

    public EventType<KeyEvent> addType(KeyBinding k) {
        if (k.isKeyPressed()) {
            types |= PRESSED;
            return KeyEvent.KEY_PRESSED;
        } else if (k.isKeyReleased()) {
            types |= RELEASED;
            return KeyEvent.KEY_RELEASED;
        } else {
            types |= TYPED;
            return KeyEvent.KEY_TYPED;
        }
    }

    public boolean hasKeyPressed() {
        return (types & PRESSED) != 0;
    }

    public boolean hasKeyReleased() {
        return (types & RELEASED) != 0;
    }

    public boolean hasKeyTyped() {
        return (types & TYPED) != 0;
    }
}
