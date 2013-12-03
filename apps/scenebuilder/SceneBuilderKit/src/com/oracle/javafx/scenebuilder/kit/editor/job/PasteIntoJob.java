/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.job;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ClipboardDecoder;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.input.Clipboard;

/**
 *
 */
public class PasteIntoJob extends Job {

    private final List<InsertAsSubComponentJob> subJobs = new ArrayList<>();
    private String description; // Final but constructed lazily
    private AbstractSelectionGroup selectionSnapshot;

    public PasteIntoJob(EditorController editorController) {
        super(editorController);
        buildSubJobs();
    }

    @Override
    public boolean isExecutable() {
        return subJobs.isEmpty() == false;
    }

    @Override
    public void execute() {
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();

        if (selection.getGroup() != null) {
            try {
                selectionSnapshot = selection.getGroup().clone();
            } catch (CloneNotSupportedException x) {
                // Emergency code
                throw new RuntimeException(x);
            }
        } else {
            selectionSnapshot = null;
        }
        selection.clear();
        selection.beginUpdate();
        fxomDocument.beginUpdate();
        for (InsertAsSubComponentJob subJob : subJobs) {
            subJob.execute();
            selection.toggleSelection(subJob.getNewObject());
        }
        fxomDocument.endUpdate();
        selection.endUpdate();
    }

    @Override
    public void undo() {
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();

        selection.beginUpdate();
        fxomDocument.beginUpdate();
        for (int i = subJobs.size() - 1; i >= 0; i--) {
            subJobs.get(i).undo();
        }
        fxomDocument.endUpdate();
        if (selectionSnapshot != null) {
            selection.select(selectionSnapshot);
        } else {
            selection.clear();
        }
        selection.endUpdate();
    }

    @Override
    public void redo() {
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final Selection selection = getEditorController().getSelection();

        selection.clear();
        selection.beginUpdate();
        fxomDocument.beginUpdate();
        for (InsertAsSubComponentJob subJob : subJobs) {
            subJob.redo();
            selection.toggleSelection(subJob.getNewObject());
        }
        fxomDocument.endUpdate();
        selection.endUpdate();
    }

    @Override
    public String getDescription() {
        if (description == null) {
            buildDescription();
        }

        return description;
    }

    private void buildSubJobs() {
       final FXOMDocument fxomDocument = getEditorController().getFxomDocument();

        if (fxomDocument == null) {
            return;
        }

        // Retrieve the FXOMObjects from the clipboard
        final ClipboardDecoder clipboardDecoder
                = new ClipboardDecoder(Clipboard.getSystemClipboard());
        final List<FXOMObject> newObjects 
                = clipboardDecoder.decode(fxomDocument);

        // Retrieve the target FXOMObject
        final Selection selection = getEditorController().getSelection();
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
            // Single selection
            if (osg.getItems().size() == 1) {
                final FXOMObject targetObject = osg.getItems().iterator().next();
                // Build InsertAsSubComponent jobs
                final DesignHierarchyMask targetMask = new DesignHierarchyMask(targetObject);
                if (targetMask.isAcceptingSubComponent(newObjects)) {
                    for (FXOMObject newObject : newObjects) {
                        final InsertAsSubComponentJob subJob = new InsertAsSubComponentJob(
                                newObject,
                                targetObject,
                                targetMask.getSubComponentCount(),
                                getEditorController());
                        subJobs.add(subJob);
                    }
                }
            }
        }
    }

    private void buildDescription() {
        assert subJobs.isEmpty() != false;
        if (subJobs.size() == 1) {
            description = makeSingleSelectionDescription();
        } else {
            description = makeMultipleSelectionDescription();
        }
    }

    private String makeSingleSelectionDescription() {
        final StringBuilder result = new StringBuilder();

        assert subJobs.size() == 1;
        final InsertAsSubComponentJob insertJob = subJobs.get(0);
        final FXOMObject fxomObject = insertJob.getNewObject();
        result.append("Paste Into ");
        if (fxomObject instanceof FXOMInstance) {
            final Object sceneGraphObject = fxomObject.getSceneGraphObject();
            if (sceneGraphObject != null) {
                result.append(sceneGraphObject.getClass().getSimpleName());
            } else {
                result.append("Unresolved Object");
            }
        } else if (fxomObject instanceof FXOMCollection) {
            result.append("Collection");
        } else {
            assert false;
            result.append(fxomObject.getClass().getSimpleName());
        }

        return result.toString();
    }

    private String makeMultipleSelectionDescription() {
        final StringBuilder result = new StringBuilder();

        result.append("Paste Into ");
        result.append(subJobs.size());
        result.append(" Objects");

        return result.toString();
    }
}
