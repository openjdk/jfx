/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import java.lang.annotation.Native;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

public class CommonDialogs {

    /**
     * Available file chooser types.
     *
     * @see #showFileChooser
     */
    public static final class Type {
        @Native public static final int OPEN = 0;
        @Native public static final int SAVE = 1;
    }

    /**
     * Provides a mechanism to filter different kinds of file name extensions.
     *
     * The following example creates an {@code ExtensionFilter} that will show
     * {@code txt} files:
     * <pre>
     *     List<String> extensions = new ArrayList<String>();
     *     extensions.add("*.txt");
     *     ExtensionFilter filter = new ExtensionFilter("Text files", extensions);
     *     List<ExtensionFilter> filters = new ArrayList<ExtensionFilter>();
     *     filters.add(filters);
     *     FileChooser.show(..., filters);
     * </pre>
     *
     * @see #showFileChooser
     */
    public final static class ExtensionFilter {
        private final String description;
        private final List<String> extensions;

        /**
         * Creates an {@code ExtensionFilter} with the specified description
         * and the file name extensions.
         *
         * @param description    the textual description for the filter
         * @param extensions     the accepted file name extensions
         * @throws   IllegalArgumentException
         *           if the description of the filter is {@code null} or empty;
         *           if the extensions is {@code null}, empty or contains an empty string
         */
        public ExtensionFilter(String description, List<String> extensions) {
            Application.checkEventThread();
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Description parameter must be non-null and not empty");
            }

            if (extensions == null || extensions.isEmpty()) {
                throw new IllegalArgumentException("Extensions parameter must be non-null and not empty");
            }

            for (String extension : extensions) {
                if (extension == null || extension.length() == 0) {
                    throw new IllegalArgumentException("Each extension must be non-null and not empty");
                }
            }

            this.description = description;
            this.extensions = extensions;
        }

        public String getDescription() {
            Application.checkEventThread();
            return description;
        }

        public List<String> getExtensions() {
            Application.checkEventThread();
            return extensions;
        }

        // Called from native
        private String[] extensionsToArray() {
            Application.checkEventThread();
            return extensions.toArray(new String[extensions.size()]);
        }
    }

    /**
     * An object representing the result of showing a file chooser dialog.
     */
    public final static class FileChooserResult {
        private final List<File> files;
        private final ExtensionFilter filter;

        /**
         * Creates a new result object.
         * @param files should not be null, may be an empty list
         */
        public FileChooserResult(List<File> files, ExtensionFilter filter) {
            if (files == null) {
                throw new NullPointerException("files should not be null");
            }
            this.files = files;
            this.filter = filter;
        }

        /**
         * Creates an empty results object.
         * Used when a user cancels a file chooser dialog.
         */
        public FileChooserResult() {
            this(new ArrayList<File>(), null);
        }

        /**
         * Returns a list of selected files.
         *
         * If a user cancels the dialog then this list is empty.
         *
         * Note that in case of a SAVE dialog the file name extension
         * corresponding to the selected ExtensionFilter may not be appended
         * automatically. Client code is responsible for checking the file
         * name and appending the extension if needed. Use {@link
         * #getExtensionFilter} to obtain the selected File Type filter.
         */
        public List<File> getFiles() {
            return files;
        }

        /**
         * Returns the ExtensionFilter selected by a user when a
         * file chooser dialog is closed.
         *
         * A reference to one of the extension filter objects passed to the
         * showFileChooser() method.  May be null.
         */
        public ExtensionFilter getExtensionFilter() {
            return filter;
        }
    }

    private CommonDialogs() {
    }

    /**
     * Creates a native file chooser that lets the user select files.
     *
     * @param owner             the owner window for this file chooser (may be null)
     * @param folder            the initial folder, may be {@code null}
     * @param filename          the initial file name for a SAVE dialog (may be null)
     * @param title             the title of the file chooser
     * @param type              the type of the file chooser, one of the constants from {@link Type}
     * @param multipleMode      enables or disable multiple file selections
     * @param extensionFilters  the filters of the file chooser
     * @param defaultFilterIndex the zero-based index of the filter selected by default
     * @throws IllegalArgumentException
     *         if the initial folder is an invalid folder;
     *         if the type doesn't equal one of the constants from {@link Type}
     * @return the files that the user selects and the selected extension
     *         filter. If the user cancels the file chooser, the method returns empty
     *         results object.
     */
    public static FileChooserResult showFileChooser(Window owner, File folder, String filename, String title, int type,
                                        boolean multipleMode, List<ExtensionFilter> extensionFilters, int defaultFilterIndex)
    {
        Application.checkEventThread();
        String _folder = convertFolder(folder);
        if (filename == null) {
            filename = "";
        }

        if (type != Type.OPEN && type != Type.SAVE) {
            throw new IllegalArgumentException("Type parameter must be equal to one of the constants from Type");
        }

        ExtensionFilter[] _extensionFilters = null;
        if (extensionFilters != null) {
            _extensionFilters = extensionFilters.toArray(new ExtensionFilter[extensionFilters.size()]);
        }

        if (extensionFilters == null
                || extensionFilters.isEmpty()
                || defaultFilterIndex < 0
                || defaultFilterIndex >= extensionFilters.size()) {
            defaultFilterIndex = 0;
        }

        return Application.GetApplication().
            staticCommonDialogs_showFileChooser(owner, _folder, filename, convertTitle(title), type, multipleMode, _extensionFilters, defaultFilterIndex);
    }

    /**
     * Creates a native folder chooser that lets the user selects folder.
     *
     * @param owner  the owner window for this folder chooser (may be null)
     * @param folder the initial folder, may be {@code null}
     * @param title  the title of the folder chooser
     * @return the folder that the user selects. If the user cancels the folder chooser,
     *         the method returns null.
     */
    public static File showFolderChooser(Window owner, File folder, String title) {
        Application.checkEventThread();
        return Application.GetApplication().staticCommonDialogs_showFolderChooser(owner, convertFolder(folder), convertTitle(title));
    }

    private static String convertFolder(File folder) {
        if (folder != null) {
            if (folder.isDirectory()) {
                try {
                    return folder.getCanonicalPath();
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unable to get a canonical path for folder", e);
                }
            } else {
                throw new IllegalArgumentException("Folder parameter must be a valid folder");
            }
        }

        return "";
    }

    private static String convertTitle(String title) {
        return (title != null) ? title : "";
    }

    /* a helper method for some platform implementations */
    protected static FileChooserResult createFileChooserResult(String[] files,
            ExtensionFilter[] extensionFilters, int index)
    {
        List<File> list = new ArrayList<>();
        for (String s : files) {
            if (s != null) {
                list.add(new File(s));
            }
        }
        return new FileChooserResult(list,
                extensionFilters == null || index < 0 || index >= extensionFilters.length ?
                null : extensionFilters[index]);
    }
}
