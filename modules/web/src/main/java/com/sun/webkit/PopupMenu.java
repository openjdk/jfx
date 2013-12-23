/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
