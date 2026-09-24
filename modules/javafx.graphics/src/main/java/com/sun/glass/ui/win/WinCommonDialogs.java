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
package com.sun.glass.ui.win;

import com.sun.glass.ui.CommonDialogs;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Window;

import java.io.File;

/**
 * MS Windows platform implementation class for CommonDialogs, on the {@code gwin_dialog_*} ABI of
 * {@code glass_win_api.h}. The two dialogs stay in C - {@code IFileDialog} is a COM
 * object, and below Vista {@code comdlg32} / {@code SHBrowseForFolder} - but the six JNI upcalls the
 * C made to read the filters and build the result were pure marshalling and are done here: an
 * {@code ExtensionFilter[]} is flattened to descriptions and {@code ';'}-joined extensions before
 * the call, and the paths that come back go through {@link CommonDialogs#createFileChooserResult}.
 * {@code _initIDs} cached the ids of exactly those six methods and is gone.
 * <p>
 * Three distinctions the C makes are carried through untouched: a {@code null} filter array skips
 * the filter setup, an <em>empty</em> one still runs it (which is what stops the shell appending an
 * extension to a typed SAVE name - {@code javafx.stage.FileChooser} without filters hands down an
 * empty array); a {@code NULL} file list means the result could not be read and yields
 * {@code null}, an empty one means cancel and yields an empty result; and the selected filter index
 * is passed through unclamped, {@code -1} when the query failed. Both calls block in a modal loop on
 * this thread.
 */
final class WinCommonDialogs {

    private WinCommonDialogs() {
    }

    static FileChooserResult showFileChooser_impl(Window owner, String folder, String filename, String title, int type,
                                         boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
        if (owner != null) {
            ((WinWindow)owner).setDeferredClosing(true);
        }
        try {
            WinGlassNative.FileDialogResult result = WinGlassNative.dialogFile(
                    owner != null ? owner.getNativeWindow() : 0L, folder, filename, title, type, multipleMode,
                    filterDescriptions(extensionFilters), filterExtensions(extensionFilters), defaultFilterIndex);
            return fileChooserResult(result, extensionFilters);
        } finally {
            if (owner != null) {
                ((WinWindow)owner).setDeferredClosing(false);
            }
        }
    }

    static File showFolderChooser_impl(Window owner, String folder, String title) {
        if (owner != null) {
            ((WinWindow)owner).setDeferredClosing(true);
        }
        try {
            String filename = WinGlassNative.dialogFolder(owner != null ? owner.getNativeWindow() : 0L, folder, title);
            return filename != null ? new File(filename) : null;
        } finally {
            if (owner != null) {
                ((WinWindow)owner).setDeferredClosing(false);
            }
        }
    }

    /**
     * The result the JNI built: {@code null} when the paths could not be read (the JNI fed a null
     * {@code String[]} to {@code createFileChooserResult}, whose {@code NullPointerException} was
     * cleared and turned into a null return), else {@code createFileChooserResult} over the paths
     * and the unclamped index - an empty list for cancel, and a filter only when the index is in
     * range.
     */
    static FileChooserResult fileChooserResult(WinGlassNative.FileDialogResult result,
                                               ExtensionFilter[] extensionFilters) {
        if (result.files() == null) {
            return null;
        }
        return CommonDialogs.createFileChooserResult(result.files(), extensionFilters, result.filterIndex());
    }

    /**
     * {@code getDescription()} of each filter, in order; {@code null} for a null array (the C skips
     * the filter setup for a NULL pointer and runs it for an empty one). A null element or a null
     * description becomes {@code ""}: the JNI's {@code JString} dereferenced the null and crashed
     * there, so this is a hardening, not a behaviour change.
     */
    static String[] filterDescriptions(ExtensionFilter[] extensionFilters) {
        if (extensionFilters == null) {
            return null;
        }
        String[] descriptions = new String[extensionFilters.length];
        for (int i = 0; i < extensionFilters.length; i++) {
            ExtensionFilter filter = extensionFilters[i];
            String description = filter == null ? null : filter.getDescription();
            descriptions[i] = description == null ? "" : description;
        }
        return descriptions;
    }

    /**
     * The extensions of each filter joined with {@code ';'} - no leading, no trailing semicolon -
     * which is the join the C did over {@code extensionsToArray()}; {@code getExtensions()} is the
     * same list. {@code null} for a null array, {@code ""} for a null element.
     */
    static String[] filterExtensions(ExtensionFilter[] extensionFilters) {
        if (extensionFilters == null) {
            return null;
        }
        String[] extensions = new String[extensionFilters.length];
        for (int i = 0; i < extensionFilters.length; i++) {
            ExtensionFilter filter = extensionFilters[i];
            extensions[i] = filter == null ? "" : String.join(";", filter.getExtensions());
        }
        return extensions;
    }
}
