/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.util.FileWatcher;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;

/**
 *
 */
public class DocumentWatchingController implements FileWatcher.Delegate {
    
    private final DocumentWindowController documentWindowController;
    private final EditorController editorController;
    private final ResourceController resourceController;
    private final SceneStyleSheetMenuController sceneStyleSheetMenuController;
    private final FileWatcher fileWatcher 
            = new FileWatcher(2000 /* ms */, this, DocumentWindowController.class.getSimpleName());
    
    
    public DocumentWatchingController(DocumentWindowController documentWindowController) {
        this.documentWindowController = documentWindowController;
        this.editorController = documentWindowController.getEditorController();
        this.resourceController = documentWindowController.getResourceController();
        this.sceneStyleSheetMenuController = documentWindowController.getSceneStyleSheetMenuController();
        
        this.editorController.sceneStyleSheetProperty().addListener(
                (ChangeListener<ObservableList<File>>) (ov, t,
                        t1) -> update());
    }
    
    public void start() {
        fileWatcher.start();
    }

    
    public void stop() {
        fileWatcher.stop();
    }
    
    
    public void update() {
        /*
         * The file watcher associated to this document window controller watches:
         *  1) the file holding the FXML document (if any)
         *  2) the resource file set in the Preview menu (if any)
         *  3) the style sheets files set in the Preview menu (if any)
         */
        
        final List<Path> targets = new ArrayList<>();
        
        // 1)
        final FXOMDocument fxomDocument = editorController.getFxomDocument();
        if ((fxomDocument != null) && (fxomDocument.getLocation() != null)) {
            try {
                final File fxmlFile = new File(fxomDocument.getLocation().toURI());
                targets.add(fxmlFile.toPath());
            } catch(URISyntaxException x) {
                throw new IllegalStateException("Bug", x); //NOI18N
            }
        }
        
        // 2)
        if (resourceController.getResourceFile() != null) {
            targets.add(resourceController.getResourceFile().toPath());
        }
        
        // 3)
        if (editorController.getSceneStyleSheets() != null) {
            for (File sceneStyleSheet : editorController.getSceneStyleSheets()) {
                targets.add(sceneStyleSheet.toPath());
            }
        }
        
        fileWatcher.setTargets(targets);
    }
    
    public void removeDocumentTarget() {
        final FXOMDocument fxomDocument = editorController.getFxomDocument();
        assert fxomDocument != null;
        assert fxomDocument.getLocation() != null;
        
        try {
            final File fxmlFile = new File(fxomDocument.getLocation().toURI());
            assert fileWatcher.getTargets().contains(fxmlFile.toPath());
            fileWatcher.removeTarget(fxmlFile.toPath());
        } catch(URISyntaxException x) {
            throw new IllegalStateException("Bug", x); //NOI18N
        }
    }
    
    /*
     * FileWatcher.Delegate
     */
    
    @Override
    public void fileWatcherDidWatchTargetCreation(Path target) {
        // Ignored
    }

    @Override
    public void fileWatcherDidWatchTargetDeletion(Path target) {
        if (isPathMatchingResourceLocation(target)) {
            // Resource file has disappeared
            resourceController.performRemoveResource(); 
            // Call above has invoked
            //      - FXOMDocument.refreshSceneGraph()
            //      - DocumentWatchingController.update()
            editorController.getMessageLog().logInfoMessage("log.info.file.deleted", 
                    I18N.getBundle(), target.getFileName());
        } else if (isPathMatchingSceneStyleSheet(target)) {
            sceneStyleSheetMenuController.performRemoveSceneStyleSheet(target.toFile());
            // Call above has invoked
            //      - FXOMDocument.reapplyCSS()
            //      - DocumentWatchingController.update()
            editorController.getMessageLog().logInfoMessage("log.info.file.deleted", 
                    I18N.getBundle(), target.getFileName());
        }
        /* 
         * Else it's the document file which has disappeared : 
         * We ignore this event : file will be recreated when user runs
         * the save command.
         */
    }

    @Override
    public void fileWatcherDidWatchTargetModification(Path target) {
        if (isPathMatchingResourceLocation(target)) {
            // Resource file has been modified -> refresh the scene graph
            resourceController.performReloadResource(); 
            // Call above has invoked FXOMDocument.refreshSceneGraph()
            editorController.getMessageLog().logInfoMessage("log.info.reload", 
                    I18N.getBundle(), target.getFileName());
            
        } else if (isPathMatchingDocumentLocation(target)) {
            if (documentWindowController.isDocumentDirty() == false) {
                // Try to reload the fxml text on disk
                try {
                    documentWindowController.reload();
                    editorController.getMessageLog().logInfoMessage("log.info.reload", 
                            I18N.getBundle(), target.getFileName());
                } catch(IOException x) {
                    // Here we silently ignore the failure :
                    // loadFromFile() has failed but left the document unchanged.
                }
            }
        } else if (isPathMatchingSceneStyleSheet(target)) {
            final FXOMDocument fxomDocument = editorController.getFxomDocument();
            if (fxomDocument != null) {
                fxomDocument.reapplyCSS(target);
                editorController.getMessageLog().logInfoMessage("log.info.reload", 
                        I18N.getBundle(), target.getFileName());
            }
        }
    }
    
    
    /*
     * Private
     */
    
    private boolean isPathMatchingDocumentLocation(Path p) {
        final boolean result;
        
        final FXOMDocument fxomDocument = editorController.getFxomDocument();
        if ((fxomDocument != null) && (fxomDocument.getLocation() != null)) {
            try {
                final File fxmlFile = new File(fxomDocument.getLocation().toURI());
                result = p.equals(fxmlFile.toPath());
            } catch(URISyntaxException x) {
                throw new IllegalStateException("Bug", x); //NOI18N
            }
        } else {
            result = false;
        }
        
        return result;
    }
    
    private boolean isPathMatchingResourceLocation(Path p) {
        final boolean result;
        
        if (resourceController.getResourceFile() != null) {
            result = p.equals(resourceController.getResourceFile().toPath());
        } else {
            result = false;
        }
        
        return result;
    }
    
    
    private boolean isPathMatchingSceneStyleSheet(Path p) {
        final boolean result;
        
        if (editorController.getSceneStyleSheets() != null) {
            result = editorController.getSceneStyleSheets().contains(p.toFile());
        } else {
            result = false;
        }
        
        return result;
    }
    
}
