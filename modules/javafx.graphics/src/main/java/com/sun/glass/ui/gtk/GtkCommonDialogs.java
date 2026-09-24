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

import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Window;

import java.io.File;

/**
 * The GTK file and folder choosers. Both were {@code native} methods of {@code GlassCommonDialogs.cpp} at commit
 * {@code 033187ad90}; the GTK calls they made are now in {@code GtkGlassNative}, which names each of them.
 * <p>
 * The C took the owner as its {@code WindowContext *} and asked that object for its {@code GtkWindow}; here the
 * owner is identified by the X11 window id its peer already exposes, and GDK is asked for the widget of that
 * window.
 */
final class GtkCommonDialogs {

    static FileChooserResult showFileChooser(Window owner,
                                    String folder,
                                    String filename,
                                    String title,
                                    int type,
                                    boolean multipleMode,
                                    ExtensionFilter[] extensionFilters, int defaultFilterIndex) {

        if (owner != null) owner.setEnabled(false);
        FileChooserResult result = GtkGlassNative.showFileChooser(nativeWindow(owner),
                folder, filename, title, type, multipleMode, extensionFilters, defaultFilterIndex);
        if (owner != null) owner.setEnabled(true);
        return result;
    }

    static File showFolderChooser(Window owner, String folder, String title) {
        if (owner != null) {
            owner.setEnabled(false);
        }
        try {
            String filename = GtkGlassNative.showFolderChooser(nativeWindow(owner), folder, title);
            return filename != null ? new File(filename) : null;

        } finally {
            if (owner != null) {
                owner.setEnabled(true);
            }
        }

    }

    /** The X11 window id of {@code owner}, or 0 for no owner and for an owner without a window yet. */
    private static long nativeWindow(Window owner) {
        return owner == null ? 0L : owner.getNativeWindow();
    }
}
