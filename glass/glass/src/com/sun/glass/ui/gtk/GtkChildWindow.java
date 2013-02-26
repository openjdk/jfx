/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.gtk;

import com.sun.glass.ui.Pixels;

final class GtkChildWindow extends GtkWindow {

    public GtkChildWindow(long parent) {
        super(parent);
    }

    @Override
    protected void _enterModal(long ptr) {
    }

    @Override
    protected void _enterModalWithWindow(long dialog, long window) {
    }

    @Override
    protected void _exitModal(long ptr) {
    }

    @Override
    protected boolean _maximize(long ptr, boolean maximize, boolean wasMaximized) {
        return true;
    }

    @Override
    protected boolean _minimize(long ptr, boolean minimize) {
        return true;
    }

    @Override
    protected void _setEnabled(long ptr, boolean enabled) {
    }

    @Override
    protected void _setFocusable(long ptr, boolean isFocusable) {
    }

    @Override
    protected void _setIcon(long ptr, Pixels pixels) {
    }

    @Override
    protected void _setLevel(long ptr, int level) {
    }

    @Override
    protected void _setAlpha(long ptr, float alpha) {
    }

    @Override
    protected boolean _setMaximumSize(long ptr, int width, int height) {
        return true;
    }

    @Override
    protected boolean _setMinimumSize(long ptr, int width, int height) {
        return true;
    }

    @Override
    protected boolean _setResizable(long ptr, boolean resizable) {
        return true;
    }

    @Override
    protected boolean _setMenubar(long ptr, long menubarPtr) {
        return true;
    }

    @Override
    protected boolean _setTitle(long ptr, String title) {
        return true;
    }
}
