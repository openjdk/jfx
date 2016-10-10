/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.stage;

import javafx.stage.PopupWindow;

import com.sun.javafx.tk.FocusCause;


public class PopupWindowPeerListener extends WindowPeerListener {

    private final PopupWindow popupWindow;

    public PopupWindowPeerListener(PopupWindow popupWindow) {
        super(popupWindow);
        this.popupWindow = popupWindow;
    }

    public void changedFocused(boolean cf, FocusCause cause) {
        // TODO: at the native level popup windows are unfocusable, so we
        // don't get any focus notifications from the platform. Temporary
        // workaround is to emulate them from the peers (see PopupStage
        // for details), but the real fix would be to forward the focus
        // events from the owner window
        WindowHelper.setFocused(popupWindow, cf);
    }

    public void closing() {
    }

    public void changedLocation(float x, float y) {
    }

    public void changedIconified(boolean iconified) {
        // Not applicable for popups
    }

    public void changedMaximized(boolean maximized) {
        // Not applicable for popups
    }

    public void changedResizable(boolean resizable) {
        // Not applicable for popups
    }

    public void changedFullscreen(boolean fs) {
        //  Not applicable for popups
    }

    @Override public void focusUngrab() {
        // Not applicable for popups
    }

}
