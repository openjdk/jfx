/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import com.sun.webkit.graphics.WCFont;

public abstract class PopupMenu {
    private long pdata;

    protected abstract void show(WebPage page, int x, int y, int width);

    protected abstract void hide();

    protected abstract void setSelectedItem(int index);

    protected abstract void appendItem(String itemText, boolean isLabel, boolean isSeparator,
                                    boolean isEnabled, int bgColor, int fgColor, WCFont font);

    protected void notifySelectionCommited(int index) {
        twkSelectionCommited(pdata, index);
    }

    protected void notifyPopupClosed() {
        twkPopupClosed(pdata);
    }

    private static PopupMenu fwkCreatePopupMenu(long pData) {
        PopupMenu popupMenu = Utilities.getUtilities().createPopupMenu();
        popupMenu.pdata = pData;
        return popupMenu;
    }

    private void fwkShow(WebPage page, int x, int y, int width) {
        assert(page != null);
        show(page, x, y, width);
    }

    private void fwkHide() {
        hide();
    }

    private void fwkSetSelectedItem(int index) {
        setSelectedItem(index);
    }

    private void fwkAppendItem(String itemText, boolean isLabel, boolean isSeparator,
                               boolean isEnabled, int bgColor, int fgColor, WCFont font)
    {
        appendItem(itemText, isLabel, isSeparator, isEnabled, bgColor, fgColor, font);
    }

    private void fwkDestroy() {
        pdata = 0;
    }

    private native void twkSelectionCommited(long pdata, int index);
    private native void twkPopupClosed(long pdata);
}
