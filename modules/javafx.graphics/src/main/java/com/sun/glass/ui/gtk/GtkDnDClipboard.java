/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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

final class GtkDnDClipboard extends SystemClipboard{

    /**
     * The data map of the drag in progress: the argument of the {@link #pushToSystemImpl} that is running, which
     * {@code dnd_source_push_data} ({@code glass_dnd.cpp}) attached to its drag widget as a JNI global reference at
     * commit {@code 033187ad90} and the drag-and-drop table's {@code source_get_data} slot now reads here. Every read
     * of it happens inside that call - {@code gtk_drag_begin} and the loop that runs until the drag widget is gone -
     * so it is set around the call. Only touched on the thread that iterates the main context.
     */
    private static HashMap<String, Object> dragInProgress;

    public GtkDnDClipboard() {
        super(Clipboard.DND);
    }

    /** The data map of the drag in progress, {@code null} when no {@link #pushToSystemImpl} is running. */
    static HashMap<String, Object> dragInProgress() {
        return dragInProgress;
    }

    @Override
    protected void pushToSystem(HashMap<String, Object> cacheData,
                                int supportedActions) {
        HashMap<String, Object> outer = dragInProgress;
        dragInProgress = cacheData;
        final int performedAction;
        try {
            performedAction = pushToSystemImpl(cacheData,
                                               supportedActions);
        } finally {
            dragInProgress = outer;
        }
        actionPerformed(performedAction);
    }

    /*
     * The natives of GlassDnDClipboard.cpp at commit 033187ad90, now the ggtk_dnd_* functions of glass_gtk_api.h,
     * through GtkGlassNative.
     */

    @Override
    protected boolean isOwner() {
        return GtkGlassNative.dndIsOwner();
    }

    protected int pushToSystemImpl(HashMap<String, Object> cacheData, int supportedActions) {
        return GtkGlassNative.dndPushToSystem(cacheData, supportedActions);
    }

    /**
     * {@code Java_com_sun_glass_ui_gtk_GtkDnDClipboard_pushTargetActionToSystem} ({@code GlassDnDClipboard.cpp},
     * commit {@code 033187ad90}) did nothing ("Never called").
     */
    @Override
    protected void pushTargetActionToSystem(int actionDone) {
    }

    @Override
    protected Object popFromSystem(String mimeType) {
        return GtkGlassNative.dndPopFromSystem(mimeType);
    }

    @Override
    protected int supportedSourceActionsFromSystem() {
        return GtkGlassNative.dndSupportedSourceActions();
    }

    @Override
    protected String[] mimesFromSystem() {
        return GtkGlassNative.dndMimesFromSystem();
    }

}
