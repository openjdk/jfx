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

package com.oracle.javafx.scenebuilder.kit.editor;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMAssetIndex;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.util.FileWatcher;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import javafx.application.Platform;

/**
 *
 */
class WatchingController implements FileWatcher.Delegate {
    
    private final EditorController editorController;
    private final FileWatcher fileWatcher 
            = new FileWatcher(2000 /*ms*/, this,  EditorController.class.getSimpleName());

    public WatchingController(EditorController editorController) {
        this.editorController = editorController;
    }
    
    public void fxomDocumentDidChange() {
        updateFileWatcher();
    }
    
    public void jobManagerRevisionDidChange() {
        updateFileWatcher();
    }
    
    public void start() {
        fileWatcher.start();
    }
    
    public void stop() {
        fileWatcher.stop();
    }
    
    public boolean isStarted() {
        return fileWatcher.isStarted();
    }
    
    /*
     * FileWatcher.Delegate
     */
    
    @Override
    public void fileWatcherDidWatchTargetCreation(Path target) {
        assert Platform.isFxApplicationThread();
        updateEditorController("file.watching.file.created", target); //NOI18N
    }

    @Override
    public void fileWatcherDidWatchTargetDeletion(Path target) {
        assert Platform.isFxApplicationThread();
        updateEditorController("file.watching.file.deleted", target); //NOI18N
    }

    @Override
    public void fileWatcherDidWatchTargetModification(Path target) {
        assert Platform.isFxApplicationThread();
        updateEditorController("file.watching.file.modified", target); //NOI18N
    }
    
    
    /*
     * Private
     */
    
    private void updateFileWatcher() {
        
        final FXOMDocument fxomDocument = editorController.getFxomDocument();
        final Collection<Path> targets;
        if (fxomDocument == null) {
            targets = Collections.emptyList();
        } else {
            final FXOMAssetIndex assetIndex = new FXOMAssetIndex(fxomDocument);
            targets = assetIndex.getFileAssets().keySet();
        }
        fileWatcher.setTargets(targets);
    }
    
    private void updateEditorController(String messageKey, Path target) {
        final String targetFileName = target.getFileName().toString();
        editorController.getMessageLog().logInfoMessage(messageKey, targetFileName);
        editorController.getErrorReport().forget();
        if (targetFileName.toLowerCase(Locale.ROOT).endsWith(".css")) { //NOI18N
            editorController.getErrorReport().cssFileDidChange(target);
            editorController.getFxomDocument().reapplyCSS(target);
        } else {
            editorController.getFxomDocument().refreshSceneGraph();
        }
    }
}
