/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control;

import com.sun.javafx.stage.PopupWindowHelper;
import com.sun.javafx.stage.WindowHelper;
import com.sun.javafx.util.Utils;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.stage.PopupWindow;

/*
 * Used to access internal methods of ContextMenu.
 */
public class ContextMenuHelper extends PopupWindowHelper {

    private static final ContextMenuHelper theInstance;
    private static ContextMenuAccessor contextMenuAccessor;

    static {
        theInstance = new ContextMenuHelper();
        Utils.forceInit(ContextMenu.class);
    }

    private static ContextMenuHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(ContextMenu contextMenu) {
        setHelper(contextMenu, getInstance());
    }

    public static Side getSide(ContextMenu contextMenu) {
        return contextMenuAccessor.getSide(contextMenu);
    }

    public static double getDeltaX(ContextMenu contextMenu) {
        return contextMenuAccessor.getDeltaX(contextMenu);
    }

    public static double getDeltaY(ContextMenu contextMenu) {
        return contextMenuAccessor.getDeltaX(contextMenu);
    }

    public static void setContextMenuAccessor(final ContextMenuAccessor newAccessor) {
        if (contextMenuAccessor != null) {
            throw new IllegalStateException();
        }
        contextMenuAccessor = newAccessor;
    }

    public interface ContextMenuAccessor {
       Side getSide(ContextMenu contextMenu);
       double getDeltaX(ContextMenu contextMenu);
       double getDeltaY(ContextMenu contextMenu);
    }

}
