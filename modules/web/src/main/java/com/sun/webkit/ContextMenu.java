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
