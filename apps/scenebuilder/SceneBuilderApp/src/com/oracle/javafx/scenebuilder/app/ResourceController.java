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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import javafx.stage.FileChooser;

/**
 *
 */
class ResourceController {

    private final DocumentWindowController documentWindowController;
    private File resourceFile;

    public ResourceController(DocumentWindowController dwc) {
        this.documentWindowController = dwc;
    }

    public File getResourceFile() {
        return resourceFile;
    }
    
    public void performSetResource() {
        // Open a file chooser for *.properties & *.bss
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18N.getString("resource.filechooser.filter.msg"),
                "*.properties")); //NOI18N
        File newResourceFile = fileChooser.showOpenDialog(documentWindowController.getStage());
        if (newResourceFile != null) {
            if (readPropertyResourceBundle(newResourceFile) == null) {
                // Property file syntax is probably incorrect
                
            } else {
                resourceFile = newResourceFile;
                resourceFileDidChange();
            }            
        }

    }

    public void performRemoveResource() {
        assert resourceFile != null;
        resourceFile = null;
        resourceFileDidChange();
    }

    public void performRevealResource() {
        assert resourceFile != null;
        try {
            EditorPlatform.revealInFileBrowser(resourceFile);
        } catch (IOException ioe) {
            final ErrorDialog errorDialog = new ErrorDialog(null);
            errorDialog.setTitle(I18N.getString("error.file.reveal.title"));
            errorDialog.setMessage(I18N.getString("error.file.reveal.message"));
            errorDialog.setDetails(I18N.getString("error.filesystem.details"));
            errorDialog.setDebugInfoWithThrowable(ioe);
            errorDialog.showAndWait();
        }
    }

    public void performReloadResource() {
        assert resourceFile != null;
        resourceFileDidChange();
    }
    
    /*
     * Private
     */
    
    private void resourceFileDidChange() {
        ResourceBundle resources;
        
        if (resourceFile != null) {
            resources = readPropertyResourceBundle(resourceFile);
            assert resources != null;
        } else {
            resources = null;
        }
        
        documentWindowController.getEditorController().setResources(resources);
        documentWindowController.getWatchingController().update();
    }
    
    
    private static PropertyResourceBundle readPropertyResourceBundle(File f) {
        PropertyResourceBundle result;
        try {
            result = new PropertyResourceBundle(new InputStreamReader(new FileInputStream(f), Charset.forName("UTF-8"))); //NOI18N
        } catch (IOException ex) {
            result = null;
        }
        return result;
    }
}
