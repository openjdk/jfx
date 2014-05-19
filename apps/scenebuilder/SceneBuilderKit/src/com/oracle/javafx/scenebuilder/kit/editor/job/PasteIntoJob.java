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
package com.oracle.javafx.scenebuilder.kit.editor.job;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.job.togglegroup.AdjustAllToggleGroupJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.ClearSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.CompositeJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.UpdateSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ClipboardDecoder;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.input.Clipboard;

/**
 *
 */
public class PasteIntoJob extends CompositeJob {

    public PasteIntoJob(EditorController editorController) {
        super(editorController);
    }

    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result = new ArrayList<>();

        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        if (fxomDocument != null) {

            // Retrieve the FXOMObjects from the clipboard
            final ClipboardDecoder clipboardDecoder
                    = new ClipboardDecoder(Clipboard.getSystemClipboard());
            final List<FXOMObject> newObjects
                    = clipboardDecoder.decode(fxomDocument);

            // Retrieve the target FXOMObject
            final Selection selection = getEditorController().getSelection();
            if (selection.getGroup() instanceof ObjectSelectionGroup) {
                final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
                // Single target selection
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
                            result.add(0, subJob);
                        }
                    } // Build InsertAsAccessory jobs for single source selection
                    else if (newObjects.size() == 1) {
                        final FXOMObject newObject = newObjects.get(0);
                        final Accessory[] accessories = {Accessory.CONTENT,
                            Accessory.CONTEXT_MENU, Accessory.GRAPHIC,
                            Accessory.TOOLTIP};
                        for (Accessory a : accessories) {
                            if (targetMask.isAcceptingAccessory(a, newObject)
                                    && targetMask.getAccessory(a) == null) {
                                final InsertAsAccessoryJob subJob = new InsertAsAccessoryJob(
                                        newObject, targetObject, a,
                                        getEditorController());
                                result.add(subJob);
                                break;
                            }
                        }
                    }

                    // Build Selection jobs if needed
                    if (result.size() > 0) {
                        result.add(0, new ClearSelectionJob(
                                getEditorController()));
                        result.add(new AdjustAllToggleGroupJob(
                                getEditorController()));
                        result.add(new UpdateSelectionJob(newObjects,
                                getEditorController()));
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected String makeDescription() {
        final String result;

        if (getSubJobs().size() == 4) { // ClearSelectionJob + InsertJob + AdjustAllToggleGroupJob + UpdateSelectionJob
            result = makeSingleSelectionDescription();
        } else {
            result = makeMultipleSelectionDescription();
        }

        return result;
    }

    private String makeSingleSelectionDescription() {
        final String result;

        assert getSubJobs().size() == 4; // ClearSelectionJob + InsertJob + AdjustAllToggleGroupJob + UpdateSelectionJob
        final Job subJob0 = getSubJobs().get(1);
        final FXOMObject newObject;
        assert subJob0 instanceof InsertAsSubComponentJob
                || subJob0 instanceof InsertAsAccessoryJob;
        if (subJob0 instanceof InsertAsSubComponentJob) {
            final InsertAsSubComponentJob insertJob = (InsertAsSubComponentJob) subJob0;
            newObject = insertJob.getNewObject();
        } else {
            final InsertAsAccessoryJob insertJob = (InsertAsAccessoryJob) subJob0;
            newObject = insertJob.getNewObject();
        }
        if (newObject instanceof FXOMInstance) {
            final Object sceneGraphObject = newObject.getSceneGraphObject();
            if (sceneGraphObject != null) {
                result = I18N.getString("label.action.edit.paste.into.1", sceneGraphObject.getClass().getSimpleName());
            } else {
                result = I18N.getString("label.action.edit.paste.into.unresolved");
            }
        } else if (newObject instanceof FXOMCollection) {
            result = I18N.getString("label.action.edit.paste.into.collection");
        } else {
            assert false;
            result = I18N.getString("label.action.edit.paste.into.1", newObject.getClass().getSimpleName());
        }

        return result;
    }

    private String makeMultipleSelectionDescription() {
        final int objectCount = getSubJobs().size() - 3;
        return I18N.getString("label.action.edit.paste.into.n", objectCount);
    }
}
