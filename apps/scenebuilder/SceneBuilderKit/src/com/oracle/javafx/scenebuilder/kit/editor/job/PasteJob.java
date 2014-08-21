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
import javafx.scene.Node;
import javafx.scene.input.Clipboard;

/**
 *
 */
public class PasteJob extends BatchSelectionJob {
    
    private final List<Job> insertJobs = new ArrayList<>();
    private FXOMObject targetObject;
    private List<FXOMObject> newObjects;

    public PasteJob(EditorController editorController) {
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
            newObjects = clipboardDecoder.decode(fxomDocument);
            assert newObjects != null; // But possible empty

            if (newObjects.isEmpty() == false) {
                
                // Retrieve the target FXOMObject :
                // If the document is empty (root object is null), then the target 
                // object is null.
                // If the selection is root or is empty, the target object is
                // the root object.
                // Otherwise, the target object is the selection common ancestor.
                if (fxomDocument.getFxomRoot() == null) {
                    targetObject = null;
                } else {
                    final Selection selection = getEditorController().getSelection();
                    final FXOMObject rootObject = fxomDocument.getFxomRoot();
                    if (selection.isEmpty() || selection.isSelected(rootObject)) {
                        targetObject = rootObject;
                    } else {
                        targetObject = selection.getAncestor();
                    }
                }
                assert (targetObject != null) || (fxomDocument.getFxomRoot() == null);

                if (targetObject == null) {
                    // Document is empty : only one object can be inserted
                    if (newObjects.size() == 1) {
                        final FXOMObject newObject0 = newObjects.get(0);
                        final SetDocumentRootJob subJob = new SetDocumentRootJob(
                                newObject0,
                                getEditorController());
                        result.add(subJob);
                        result.add(new AdjustAllToggleGroupJob(getEditorController()));
                        insertJobs.add(subJob);
                    }
                } else {
                    // Checks if pasted objects are
                    // Build InsertAsSubComponent jobs
                    final DesignHierarchyMask targetMask = new DesignHierarchyMask(targetObject);
                    if (targetMask.isAcceptingSubComponent(newObjects)) {
                        
                        final double relocateDelta;
                        if (targetMask.isFreeChildPositioning()) {
                            final int pasteJobCount = countPasteJobs();
                            relocateDelta = 10.0 * (pasteJobCount + 1);
                        } else {
                            relocateDelta = 0.0;
                        }
                        for (FXOMObject newObject : newObjects) {
                            final InsertAsSubComponentJob subJob = new InsertAsSubComponentJob(
                                    newObject,
                                    targetObject,
                                    targetMask.getSubComponentCount(),
                                    getEditorController());
                            result.add(0, subJob);
                            insertJobs.add(subJob);
                            if ((relocateDelta != 0.0) && newObject.isNode()) {
                                final Node sceneGraphNode = (Node) newObject.getSceneGraphObject();
                                final RelocateNodeJob relocateJob = new RelocateNodeJob(
                                        (FXOMInstance) newObject,
                                        sceneGraphNode.getLayoutX() + relocateDelta,
                                        sceneGraphNode.getLayoutY() + relocateDelta,
                                        getEditorController()
                                );
                                result.add(relocateJob);
                            }
                        }
                        result.add(new AdjustAllToggleGroupJob(getEditorController()));
                    }
                }
            }
        }
        
        return result;
    }

    
    @Override
    protected String makeDescription() {
        final String result;
        
        if (insertJobs.size() == 1) {
            result = makeSingleSelectionDescription();
        } else {
            result = makeMultipleSelectionDescription();
        }
        
        return result;
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        assert newObjects != null; // But possibly empty
        if (newObjects.isEmpty()) {
            return null;
        } else {
            return new ObjectSelectionGroup(newObjects, newObjects.iterator().next(), null);
        }
    }

    /*
     * Private
     */
    private String makeSingleSelectionDescription() {
        final String result;

        assert insertJobs.size() == 1;
        final Job subJob0 = insertJobs.get(0);
        final FXOMObject newObject;
        if (subJob0 instanceof InsertAsSubComponentJob) {
            final InsertAsSubComponentJob insertJob = (InsertAsSubComponentJob) subJob0;
            newObject = insertJob.getNewObject();
        } else {
            assert subJob0 instanceof SetDocumentRootJob;
            final SetDocumentRootJob setRootJob = (SetDocumentRootJob) subJob0;
            newObject = setRootJob.getNewRoot();
        }
        if (newObject instanceof FXOMInstance) {
            final Object sceneGraphObject = newObject.getSceneGraphObject();
            if (sceneGraphObject != null) {
                result = I18N.getString("label.action.edit.paste.1", sceneGraphObject.getClass().getSimpleName());
            } else {
                result = I18N.getString("label.action.edit.paste.unresolved");
            }
        } else if (newObject instanceof FXOMCollection) {
            result = I18N.getString("label.action.edit.paste.collection");
        } else {
            assert false;
            result = I18N.getString("label.action.edit.paste.1", newObject.getClass().getSimpleName());
        }

        return result;
    }

    private String makeMultipleSelectionDescription() {
        final int objectCount = insertJobs.size();
        return I18N.getString("label.action.edit.paste.n", objectCount);
    }
    
    private int countPasteJobs() {
        int result = 0;
        
        final List<Job> undoStack = getEditorController().getJobManager().getUndoStack();
        for (Job job : undoStack) {
            if (job instanceof PasteJob) {
                final PasteJob pasteJob = (PasteJob) job;
                if (this.targetObject == pasteJob.targetObject) {
                    result++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        
        return result;
    }
}
