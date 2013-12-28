/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.app;

import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;

/**
 *
 */
public class SceneStyleSheetMenuController {
    private final DocumentWindowController documentWindowController;
    
    public SceneStyleSheetMenuController(DocumentWindowController dwc) {
        this.documentWindowController = dwc;
    }

    void performAddSceneStyleSheet() {
        boolean knownFilesModified = false;
        ObservableList<File> knownFiles = documentWindowController.getEditorController().getSceneStyleSheets();
        
        if (knownFiles == null) {
            knownFiles = FXCollections.observableArrayList();
        }
        
        // Open a file chooser for *.css & *.bss
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18N.getString("scenestylesheet.filechooser.filter.msg"),
                "*.css", "*.bss")); //NOI18N
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(documentWindowController.getStage());
        
        for (File f : selectedFiles) {
            if (! knownFiles.contains(f)) {
                knownFiles.add(f);
                knownFilesModified = true;
            }
        }

        // Update sceneStyleSheet property so that listeners will react accordingly
        if (knownFilesModified) {
            documentWindowController.getEditorController().setSceneStyleSheets(knownFiles);
        }
    }
    
    public void performRemoveSceneStyleSheet(File toRemove) {
        ObservableList<File> knownFiles = documentWindowController.getEditorController().getSceneStyleSheets();
        assert knownFiles != null;
        
        if (knownFiles.contains(toRemove)) {
            knownFiles.remove(toRemove);
            documentWindowController.getEditorController().setSceneStyleSheets(knownFiles);
        }
    }
    
    public void performOpenSceneStyleSheet(File toOpen) {
        try {
            EditorPlatform.open(toOpen.getPath());
        } catch (IOException ioe) {
            final ErrorDialog errorDialog = new ErrorDialog(null);
            errorDialog.setTitle(I18N.getString("error.file.open.title"));
            errorDialog.setMessage(I18N.getString("error.file.open.message"));
            errorDialog.setDetails(I18N.getString("error.filesystem.details"));
            errorDialog.setDebugInfoWithThrowable(ioe);
            errorDialog.showAndWait();
        }
    }
    
}
