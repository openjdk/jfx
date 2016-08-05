/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

/**
 *
 */
class AndroidInputDevice implements Runnable, InputDevice {

    private AndroidInputProcessor inputProcessor;

    @Override
    public void run() {
        if (inputProcessor == null) {
            System.err.println("Error: no input processor");
            return;
        }
       // read from the android device (change this into push)
        // and process the events
    }

    @Override
    public boolean isTouch() {
        return true;
    }

    @Override
    public boolean isMultiTouch() {
        return true;
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    @Override
    public boolean is5Way() {
        return false;
    }

    @Override
    public boolean isFullKeyboard() {
// if we return false, the JavaFX virtual keyboard will be used instead of the android built-in one
        return true;
    }

    void setInputProcessor(AndroidInputProcessor inputProcessor) {
        this.inputProcessor = inputProcessor;
    }
}
