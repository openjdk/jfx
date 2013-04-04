/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Window;

import java.io.File;
import java.util.List;

/**
 * MS Windows platform implementation class for CommonDialogs.
 */
final class WinCommonDialogs {

    private native static void _initIDs();
    static {
        _initIDs();
    }

    private static native FileChooserResult _showFileChooser(long owner, String folder, String filename, String title, int type,
                                                    boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex);

    private static native String _showFolderChooser(long owner, String folder, String title);

    static FileChooserResult showFileChooser_impl(Window owner, String folder, String filename, String title, int type,
                                         boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {
        if (owner != null) {
            ((WinWindow)owner).setDeferredClosing(true);
        }
        try {
            return _showFileChooser(owner != null ? owner.getNativeWindow() : 0L,
                    folder, filename, title, type, multipleMode, extensionFilters, defaultFilterIndex);
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
            String filename = _showFolderChooser(owner != null ? owner.getNativeWindow() : 0L, folder, title);
            return filename != null ? new File(filename) : null;
        } finally {
            if (owner != null) {
                ((WinWindow)owner).setDeferredClosing(false);
            }
        }
    }
}
