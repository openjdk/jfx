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

    // The six fwk methods below are the WKJPopupCallbacks slots. They are package private rather
    // than private because PopupMenuNative, not the library, now dispatches them: an FFM upcall
    // stub is an ordinary Java call and cannot reach a private member, where JNI could.
    static PopupMenu fwkCreatePopupMenu(long pData) {
        PopupMenu popupMenu = Utilities.getUtilities().createPopupMenu();
        popupMenu.pdata = pData;
        return popupMenu;
    }

    void fwkShow(WebPage page, int x, int y, int width) {
        assert(page != null);
        show(page, x, y, width);
    }

    void fwkHide() {
        hide();
    }

    void fwkSetSelectedItem(int index) {
        setSelectedItem(index);
    }

    void fwkAppendItem(String itemText, boolean isLabel, boolean isSeparator,
                       boolean isEnabled, int bgColor, int fgColor, WCFont font)
    {
        appendItem(itemText, isLabel, isSeparator, isEnabled, bgColor, fgColor, font);
    }

    void fwkDestroy() {
        pdata = 0;
    }

    private void twkSelectionCommited(long pdata, int index) {
        PopupMenuNative.selectionCommitted(pdata, index);
    }

    private void twkPopupClosed(long pdata) {
        PopupMenuNative.closed(pdata);
    }
}
