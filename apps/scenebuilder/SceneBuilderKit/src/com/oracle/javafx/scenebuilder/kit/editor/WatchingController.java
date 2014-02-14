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

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.FileWatcher;
import com.oracle.javafx.scenebuilder.kit.util.URLUtils;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
    
    private static final PropertyName valueName = new PropertyName("value"); //NOI18N
    
    private void updateFileWatcher() {
        
        final List<Path> targetPaths = new ArrayList<>();
        
        final FXOMDocument fxomDocument = editorController.getFxomDocument();
        if ((fxomDocument != null) && (fxomDocument.getFxomRoot() != null)) {
            final FXOMObject fxomRoot = fxomDocument.getFxomRoot();
            
            for (FXOMPropertyT p : fxomRoot.collectPropertiesT()) {
                final Path path = extractPath(p);
                if (path != null) {
                    targetPaths.add(path);
                }
            }
            
            for (FXOMObject fxomObject : fxomRoot.collectObjectWithSceneGraphObjectClass(URL.class)) {
                if (fxomObject instanceof FXOMInstance) {
                    final FXOMInstance urlInstance = (FXOMInstance) fxomObject;
                    final FXOMProperty valueProperty = urlInstance.getProperties().get(valueName);
                    if (valueProperty instanceof FXOMPropertyT) {
                        FXOMPropertyT valuePropertyT = (FXOMPropertyT) valueProperty;
                        final Path path = extractPath(valuePropertyT);
                        if (path != null) {
                            targetPaths.add(path);
                        }
                    } else {
                        assert false : "valueProperty.getName()=" + valueProperty.getName();
                    }
                }
            }
        }
        
        fileWatcher.setTargets(targetPaths);
    }
    
    
    private Path extractPath(FXOMPropertyT p) {
        Path result;
        
        final PrefixedValue pv = new PrefixedValue(p.getValue());
        if (pv.isPlainString()) {
            try {
                final File file = URLUtils.getFile(pv.getSuffix());
                if (file == null) { // Not a file URL
                    result = null;
                } else {
                    result = file.toPath();
                }
            } catch(URISyntaxException x) {
                result = null;
            }
        } else if (pv.isDocumentRelativePath()) {
            final URL documentLocation = p.getFxomDocument().getLocation();
            if (documentLocation == null) {
                result = null;
            } else {
                final URL url = pv.resolveDocumentRelativePath(documentLocation);
                if (url == null) {
                    result = null;
                } else {
                    try {
                        result = Paths.get(url.toURI());
                    } catch(FileSystemNotFoundException|URISyntaxException x) {
                        result = null;
                    }
                }
            }
        } else if (pv.isClassLoaderRelativePath()) {
            final ClassLoader classLoader = p.getFxomDocument().getClassLoader();
            if (classLoader == null) {
                result = null;
            } else {
                final URL url = pv.resolveClassLoaderRelativePath(classLoader);
                if (url == null) {
                    result = null;
                } else {
                    try {
                        final File file = URLUtils.getFile(url);
                        if (file == null) { // Not a file URL
                            result = null;
                        } else {
                            result = file.toPath();
                        }
                    } catch(URISyntaxException x) {
                        result = null;
                    }
                }
                
            }
        } else {
            result = null;
        }
        
        return result;
    }
    
    private void updateEditorController(String messageKey, Path target) {
        final String targetFileName = target.getFileName().toString();
        editorController.getMessageLog().logInfoMessage(messageKey, targetFileName);
        if (targetFileName.toLowerCase(Locale.ROOT).endsWith(".css")) { //NOI18N
            editorController.getFxomDocument().reapplyCSS(target);
        } else {
            editorController.getFxomDocument().refreshSceneGraph();
        }
    }
}
