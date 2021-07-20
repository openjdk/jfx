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
package com.sun.glass.ui.mac;

import com.sun.glass.ui.CommonDialogs.Type;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.Window;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.io.File;

/**
 * MacOSX platform implementation class for CommonDialogs.
 */
final class MacCommonDialogs {

    private native static void _initIDs();
    static {
        _initIDs();
    }

    private static native FileChooserResult _showFileOpenChooser(long owner, String folder, String title,
                                                    boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex);
    private static native FileChooserResult _showFileSaveChooser(long owner, String folder, String filename, String title,
                                                                 ExtensionFilter[] extensionFilters, int defaultFilterIndex);

    private static native File _showFolderChooser(long owner, String folder, String title);

    static FileChooserResult showFileChooser_impl(Window owner, String folder, String filename, String title, int type,
                                         boolean multipleMode, ExtensionFilter[] extensionFilters, int defaultFilterIndex) {

        final long ownerPtr = owner != null ? owner.getNativeWindow() : 0L;

        if (type == Type.OPEN) {
            return _showFileOpenChooser(ownerPtr, folder, title, multipleMode, extensionFilters, defaultFilterIndex);
        } else if (type == Type.SAVE) {
            return _showFileSaveChooser(ownerPtr, folder, filename, title, extensionFilters, defaultFilterIndex);
        } else {
            return null;
        }
    }

    static File showFolderChooser_impl(Window owner, String folder, String title) {
        final long ownerPtr = owner != null ? owner.getNativeWindow() : 0L;
        return _showFolderChooser(ownerPtr, folder, title);
    }

    @SuppressWarnings("removal")
    static boolean isFileNSURLEnabled() {
        // The check is dynamic since an app may want to toggle it dynamically.
        // The performance is not critical for FileChoosers.
        return AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("glass.macosx.enableFileNSURL"));
    }
}
