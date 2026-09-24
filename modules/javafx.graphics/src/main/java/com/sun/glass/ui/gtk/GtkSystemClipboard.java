/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.SystemClipboard;
import java.util.HashMap;

/**
 * The GTK clipboard peer. Every method below was a {@code native} of
 * {@code GlassSystemClipboard.cpp} at commit {@code 033187ad90}; the GTK calls they made are now in
 * {@code GtkGlassNative}, which names each of them.
 */
final class GtkSystemClipboard extends SystemClipboard {

    public GtkSystemClipboard() {
        super(Clipboard.SYSTEM);
        init();
    }

    @Override
    protected void close() {
        super.close();
        dispose();
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_init}. */
    void init() {
        GtkGlassNative.clipboardInit(this);
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_dispose}. */
    void dispose() {
        GtkGlassNative.clipboardDispose();
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_isOwner}: a flag the owner-change handler keeps. */
    @Override
    protected boolean isOwner() {
        return GtkGlassNative.clipboardIsOwner();
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_pushToSystem}, which ignored
     * {@code supportedActions}.
     */
    @Override
    protected void pushToSystem(HashMap<String, Object> cacheData, int supportedActions) {
        GtkGlassNative.clipboardPush(cacheData);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_pushTargetActionToSystem} did nothing: the target
     * action is a drag-and-drop notion, not a clipboard one.
     */
    @Override
    protected void pushTargetActionToSystem(int actionDone) {
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_popFromSystem}. */
    @Override
    protected Object popFromSystem(String mimeType) {
        return GtkGlassNative.clipboardPop(mimeType);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_supportedSourceActionsFromSystem} answered 0: the
     * source actions are a drag-and-drop notion, not a clipboard one.
     */
    @Override
    protected int supportedSourceActionsFromSystem() {
        return 0;
    }

    /** {@code Java_com_sun_glass_ui_gtk_GtkSystemClipboard_mimesFromSystem}. */
    @Override
    protected String[] mimesFromSystem() {
        return GtkGlassNative.clipboardMimes();
    }
}
