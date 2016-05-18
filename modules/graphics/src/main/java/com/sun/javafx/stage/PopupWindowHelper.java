/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.util.Utils;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

/**
 * Used to access internal window methods.
 */
public class PopupWindowHelper extends WindowHelper {
    private static final PopupWindowHelper theInstance;
    private static PopupWindowAccessor popupWindowAccessor;

    static {
        theInstance = new PopupWindowHelper();
        Utils.forceInit(PopupWindow.class);
    }

    private static WindowHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(PopupWindow popupWindow) {
        setHelper(popupWindow, getInstance());
    }

    @Override
    protected void visibleChangingImpl(Window window, boolean visible) {
        super.visibleChangingImpl(window, visible);
        popupWindowAccessor.doVisibleChanging(window, visible);
    }

    @Override
    protected void visibleChangedImpl(Window window, boolean visible) {
        super.visibleChangedImpl(window, visible);
        popupWindowAccessor.doVisibleChanged(window, visible);
    }

    public static ObservableList<Node> getContent(PopupWindow popupWindow) {
        return popupWindowAccessor.getContent(popupWindow);
    }

    public static void setPopupWindowAccessor(PopupWindowAccessor newAccessor) {
        if (popupWindowAccessor != null) {
            throw new IllegalStateException();
        }

        popupWindowAccessor = newAccessor;
    }

    public interface PopupWindowAccessor {
        ObservableList<Node> getContent(PopupWindow popupWindow);
        void doVisibleChanging(Window window, boolean visible);
        void doVisibleChanged(Window window, boolean visible);
    }
}
