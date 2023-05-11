/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.sun.glass.ui.CommonDialogs;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;

import com.sun.javafx.tk.FileChooserType;
import com.sun.javafx.tk.Toolkit;

/**
 * Provides support for standard platform file dialogs. These dialogs have look
 * and feel of the platform UI components which is independent of JavaFX.
 * <p>
 * On some platforms where file access may be restricted or not part of the user
 * model (for example, on some mobile or embedded devices), opening a file
 * dialog may always result in a no-op (that is, null file(s) being returned).
 * </p>
 * <p>
 * A {@code FileChooser} can be used to invoke file open dialogs for selecting
 * single file ({@code showOpenDialog}), file open dialogs for selecting
 * multiple files ({@code showOpenMultipleDialog}) and file save dialogs
 * ({@code showSaveDialog}). The configuration of the displayed dialog is
 * controlled by the values of the {@code FileChooser} properties set before the
 * corresponding {@code show*Dialog} method is called. This configuration
 * includes the dialog's title, the initial directory displayed in the dialog
 * and the extension filter(s) for the listed files. For configuration
 * properties which values haven't been set explicitly, the displayed dialog
 * uses their platform default values. A call to a show dialog method is
 * blocked until the user makes a choice or cancels the dialog. The return
 * value specifies the selected file(s) or equals to {@code null} if the dialog
 * has been canceled.
 * </p>
 * <p>
 * Example:
 * <pre>{@code
 * FileChooser fileChooser = new FileChooser();
 * fileChooser.setTitle("Open Resource File");
 * fileChooser.getExtensionFilters().addAll(
 *         new ExtensionFilter("Text Files", "*.txt"),
 *         new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"),
 *         new ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"),
 *         new ExtensionFilter("All Files", "*.*"));
 * File selectedFile = fileChooser.showOpenDialog(mainStage);
 * if (selectedFile != null) {
 *    mainStage.display(selectedFile);
 * }
 * }</pre>
 *
 * @since JavaFX 2.0
 */
public final class FileChooser {

    /**
     * Defines an extension filter, used for filtering which files can be chosen
     * in a FileDialog based on the file name extensions.
     * @since JavaFX 2.0
     */
    public static final class ExtensionFilter {

        private final String description;
        private final List<String> extensions;

        /**
         * Creates an {@code ExtensionFilter} with the specified description
         * and the file name extensions.
         * <p>
         * File name extension should be specified in the {@code *.<extension>}
         * format.
         *
         * @param description the textual description for the filter
         * @param extensions the accepted file name extensions
         * @throws NullPointerException if the description or the extensions
         *      are {@code null}
         * @throws IllegalArgumentException if the description or the extensions
         *      are empty
         */
        public ExtensionFilter(final String description, final String... extensions) {
            this(description, List.of(extensions));
        }

        /**
         * Creates an {@code ExtensionFilter} with the specified description
         * and the file name extensions.
         * <p>
         * File name extension should be specified in the {@code *.<extension>}
         * format.
         *
         * @param description the textual description for the filter
         * @param extensions the accepted file name extensions
         * @throws NullPointerException if the description or the extensions
         *      are {@code null}
         * @throws IllegalArgumentException if the description or the extensions
         *      are empty
         */
        public ExtensionFilter(final String description, final List<String> extensions) {
            var extensionsList = List.copyOf(extensions);
            validateArgs(description, extensionsList);

            this.description = description;
            this.extensions = extensionsList;
        }

        /**
         * Gets the description for this {@code ExtensionFilter}.
         *
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Gets the file name extensions for this {@code ExtensionFilter}.
         * <p>
         * The returned list is unmodifiable and will throw
         * {@code UnsupportedOperationException} on each modification attempt.
         *
         * @return the file name extensions
         */
        public List<String> getExtensions() {
            return extensions;
        }

        private static void validateArgs(final String description, final List<String> extensions) {
            Objects.requireNonNull(description, "Description must not be null");

            if (description.isEmpty()) {
                throw new IllegalArgumentException("Description must not be empty");
            }

            if (extensions.isEmpty()) {
                throw new IllegalArgumentException("At least one extension must be defined");
            }

            for (String extension : extensions) {
                if (extension.isEmpty()) {
                    throw new IllegalArgumentException("Extension must not be empty");
                }
            }
        }
    }

    /**
     * The title of the displayed file dialog.
     */
    private StringProperty title;

    /**
     * Creates a {@code FileChooser}.
     */
    public FileChooser() {
    }

    public final void setTitle(final String value) {
        titleProperty().set(value);
    }

    public final String getTitle() {
        return (title != null) ? title.get() : null;
    }

    public final StringProperty titleProperty() {
        if (title == null) {
            title = new SimpleStringProperty(this, "title");
        }

        return title;
    }

    /**
     * The initial directory for the displayed file dialog.
     */
    private ObjectProperty<File> initialDirectory;

    public final void setInitialDirectory(final File value) {
        initialDirectoryProperty().set(value);
    }

    public final File getInitialDirectory() {
        return (initialDirectory != null) ? initialDirectory.get() : null;
    }

    public final ObjectProperty<File> initialDirectoryProperty() {
        if (initialDirectory == null) {
            initialDirectory =
                    new SimpleObjectProperty<>(this, "initialDirectory");
        }

        return initialDirectory;
    }

    /**
     * The initial file name for the displayed dialog.
     * <p>
     * This property is used mostly in the displayed file save dialogs as the
     * initial file name for the file being saved. If set for a file open
     * dialog it will have any impact on the displayed dialog only if the
     * corresponding platform provides support for such property in its
     * file open dialogs.
     * </p>
     *
     * @since JavaFX 2.2.40
     */
    private ObjectProperty<String> initialFileName;

    public final void setInitialFileName(final String value) {
        initialFileNameProperty().set(value);
    }

    public final String getInitialFileName() {
        return (initialFileName != null) ? initialFileName.get() : null;
    }

    public final ObjectProperty<String> initialFileNameProperty() {
        if (initialFileName == null) {
            initialFileName =
                    new SimpleObjectProperty<>(this, "initialFileName");
        }

        return initialFileName;
    }

    /**
     * Specifies the extension filters used in the displayed file dialog.
     */
    private ObservableList<ExtensionFilter> extensionFilters =
            FXCollections.<ExtensionFilter>observableArrayList();

    /**
     * Gets the extension filters used in the displayed file dialog. Only
     * one extension filter from the list is active at any time in the displayed
     * dialog and only files which correspond to this extension filter are
     * shown. The first extension filter from the list is activated when the
     * dialog is invoked. Then the user can switch the active extension filter
     * to any other extension filter from the list and in this way control the
     * set of displayed files.
     *
     * @return An observable list of the extension filters used in this dialog
     */
    public ObservableList<ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }

    /**
     * This property is used to pre-select the extension filter for the next
     * displayed dialog and to read the user-selected extension filter from the
     * dismissed dialog.
     * <p>
     * When the file dialog is shown, the selectedExtensionFilter will be checked.
     * If the value of selectedExtensionFilter is null or is not contained in
     * the list of extension filters, then the first extension filter in the list
     * of extension filters will be selected instead. Otherwise, the specified
     * selectedExtensionFilter will be activated.
     * <p>
     * After the dialog is dismissed the value of this property is updated to
     * match the user-selected extension filter from the dialog.
     *
     * @since JavaFX 8.0
     */

    private ObjectProperty<ExtensionFilter> selectedExtensionFilter;

    public final ObjectProperty<ExtensionFilter> selectedExtensionFilterProperty() {
        if (selectedExtensionFilter == null) {
            selectedExtensionFilter =
                    new SimpleObjectProperty<>(this,
                    "selectedExtensionFilter");
        }
        return selectedExtensionFilter;
    }

    public final void setSelectedExtensionFilter(ExtensionFilter filter) {
        selectedExtensionFilterProperty().setValue(filter);
    }

    public final ExtensionFilter getSelectedExtensionFilter() {
        return (selectedExtensionFilter != null)
                ? selectedExtensionFilter.get()
                : null;
    }

    /**
     * Shows a new file open dialog. The method doesn't return until the
     * displayed open dialog is dismissed. The return value specifies
     * the file chosen by the user or {@code null} if no selection has been
     * made. If the owner window for the file dialog is set, input to all
     * windows in the dialog's owner chain is blocked while the file dialog
     * is being shown.
     *
     * @param ownerWindow the owner window of the displayed file dialog
     * @return the selected file or {@code null} if no file has been selected
     */
    public File showOpenDialog(final Window ownerWindow) {
        final List<File> selectedFiles =
                showDialog(ownerWindow, FileChooserType.OPEN);

        return ((selectedFiles != null) && (selectedFiles.size() > 0))
                ? selectedFiles.get(0) : null;
    }

    /**
     * Shows a new file open dialog in which multiple files can be selected.
     * The method doesn't return until the displayed open dialog is dismissed.
     * The return value specifies the files chosen by the user or {@code null}
     * if no selection has been made. If the owner window for the file dialog is
     * set, input to all windows in the dialog's owner chain is blocked while
     * the file dialog is being shown.
     * <p>
     * The returned list is unmodifiable and will throw
     * {@code UnsupportedOperationException} on each modification attempt.
     *
     * @param ownerWindow the owner window of the displayed file dialog
     * @return the selected files or {@code null} if no file has been selected
     */
    public List<File> showOpenMultipleDialog(final Window ownerWindow) {
        final List<File> selectedFiles =
                showDialog(ownerWindow, FileChooserType.OPEN_MULTIPLE);

        return ((selectedFiles != null) && (selectedFiles.size() > 0))
                ? Collections.unmodifiableList(selectedFiles)
                : null;
    }

    /**
     * Shows a new file save dialog. The method doesn't return until the
     * displayed file save dialog is dismissed. The return value specifies the
     * file chosen by the user or {@code null} if no selection has been made.
     * If the owner window for the file dialog is set, input to all windows in
     * the dialog's owner chain is blocked while the file dialog is being shown.
     *
     * @param ownerWindow the owner window of the displayed file dialog
     * @return the selected file or {@code null} if no file has been selected
     */
    public File showSaveDialog(final Window ownerWindow) {
        final List<File> selectedFiles =
                showDialog(ownerWindow, FileChooserType.SAVE);

        return ((selectedFiles != null) && (selectedFiles.size() > 0))
                ? selectedFiles.get(0) : null;
    }

    private ExtensionFilter findSelectedFilter(CommonDialogs.ExtensionFilter filter) {
        if (filter != null) {
            String description = filter.getDescription();
            List<String> extensions = filter.getExtensions();

            for (ExtensionFilter ef : extensionFilters) {
                if (description.equals(ef.getDescription())
                        && extensions.equals(ef.getExtensions())) {
                    return ef;
                }
            }
        }

        return null;
    }

    private List<File> showDialog(final Window ownerWindow,
                                  final FileChooserType fileChooserType) {
        FileChooserResult result = Toolkit.getToolkit().showFileChooser(
                (ownerWindow != null) ? ownerWindow.getPeer() : null,
                getTitle(),
                getInitialDirectory(),
                getInitialFileName(),
                fileChooserType,
                extensionFilters,
                getSelectedExtensionFilter());

        if (result == null) {
            return null;
        }

        List<File> files = result.getFiles();
        if (files != null && files.size() > 0) {
            selectedExtensionFilterProperty().set(
                    findSelectedFilter(result.getExtensionFilter()));
        }
        return files;
    }
}
