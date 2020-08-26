/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.tk.Toolkit;
import java.io.File;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// PENDING_DOC_REVIEW
/**
 * Provides support for standard directory chooser dialogs. These dialogs have
 * look and feel of the platform UI components which is independent of JavaFX.
 *
 * On some platforms where file access may be restricted or not part of the user
 * model (for example, on some mobile or embedded devices), opening a directory
 * dialog may always result in a no-op (that is, null file being returned).
 *
 * @since JavaFX 2.1
 */
public final class DirectoryChooser {
    /**
     * The title of the displayed dialog.
     */
    private StringProperty title;

    /**
     * Creates a {@code DirectoryChooser}.
     */
    public DirectoryChooser() {
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
     * The initial directory for the displayed dialog.
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
                    new SimpleObjectProperty<File>(this, "initialDirectory");
        }

        return initialDirectory;
    }

    /**
     * Shows a new directory selection dialog. The method doesn't return until
     * the displayed dialog is dismissed. The return value specifies the
     * directory chosen by the user or {@code null} if no selection has been
     * made. If the owner window for the directory selection dialog is set,
     * input to all windows in the dialog's owner chain is blocked while the
     * dialog is being shown.
     *
     * @param ownerWindow the owner window of the displayed dialog
     * @return the selected directory or {@code null} if no directory has been
     *      selected
     */
    public File showDialog(final Window ownerWindow) {
        return Toolkit.getToolkit().showDirectoryChooser(
                (ownerWindow != null) ? ownerWindow.getPeer() : null,
                getTitle(),
                getInitialDirectory());
    }
}
