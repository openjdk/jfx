/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

public abstract class ContextMenu {

    protected abstract void show(ShowContext showContext, int x, int y);
    
    protected abstract void appendItem(ContextMenuItem item);

    protected abstract void insertItem(ContextMenuItem item, int index);

    protected abstract int getItemCount();

    public final class ShowContext {
        private final WebPage page;
        private final long pdata;

        private ShowContext(WebPage page, long pdata) {
            this.page = page;
            this.pdata = pdata;
        }

        public WebPage getPage() {
            return page;
        }

        public void notifyItemSelected(int itemAction) {
            twkHandleItemSelected(pdata, itemAction);
        }
    }

    private static ContextMenu fwkCreateContextMenu() {
        return Utilities.getUtilities().createContextMenu();
    }

    private void fwkShow(WebPage webPage, long pData, int x, int y) {
        show(new ShowContext(webPage, pData), x, y);
    }

    private void fwkAppendItem(ContextMenuItem item) {
        appendItem(item);
    }

    private void fwkInsertItem(ContextMenuItem item, int index) {
        insertItem(item, index);
    }

    private int fwkGetItemCount() {
        return getItemCount();
    }

    private native void twkHandleItemSelected(long menuPData, int itemAction);
}
