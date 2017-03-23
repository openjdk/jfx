/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;

import javafx.beans.NamedArg;


/**
 * This class describes features of a Web popup window as specified by
 * JavaScript {@code window.open} function. Instances are passed into popup
 * handlers registered on a {@code WebEngine} using
 * {@link WebEngine#setCreatePopupHandler} method.
 *
 * @see WebEngine
 * @see WebEngine#setCreatePopupHandler
 * @since JavaFX 2.0
 */
public final class PopupFeatures {

    private final boolean menu, status, toolbar, resizable;

    /**
     * Creates a new instance.
     *
     * @param menu whether menu bar should be present
     * @param status whether status bar should be present
     * @param toolbar whether tool bar should be present
     * @param resizable whether popup window should be resizable
     */
    public PopupFeatures(
            @NamedArg("menu") boolean menu, @NamedArg("status") boolean status, @NamedArg("toolbar") boolean toolbar, @NamedArg("resizable") boolean resizable) {
        this.menu = menu;
        this.status = status;
        this.toolbar = toolbar;
        this.resizable = resizable;
    }

    /**
     * Returns whether menu bar should be present.
     * @return true if the menu bar should be present
     */
    public final boolean hasMenu() {
        return menu;
    }

    /**
     * Returns whether status bar should be present.
     * @return true if the status bar should be present
     */
    public final boolean hasStatus() {
        return status;
    }

    /**
     * Returns whether tool bar should be present.
     * @return true if the tool bar should be present
     */
    public final boolean hasToolbar() {
        return toolbar;
    }

    /**
     * Returns whether popup window should be resizable.
     * @return true if the popup window should be resizable
     */
    public final boolean isResizable() {
        return resizable;
    }
}
