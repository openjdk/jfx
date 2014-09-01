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

package com.oracle.javafx.scenebuilder.kit.editor.job.atomic;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class UpdateSelectionJob extends Job {

    private AbstractSelectionGroup oldSelectionGroup;
    private final AbstractSelectionGroup newSelectionGroup;

    public UpdateSelectionJob(AbstractSelectionGroup group, EditorController editorController) {
        super(editorController);
        newSelectionGroup = group;
    }

    public UpdateSelectionJob(FXOMObject newSelectedObject, EditorController editorController) {
        super(editorController);

        assert newSelectedObject != null;
        final List<FXOMObject> newSelectedObjects = new ArrayList<>();
        newSelectedObjects.add(newSelectedObject);
        newSelectionGroup = new ObjectSelectionGroup(newSelectedObjects, newSelectedObject, null);
    }

    public UpdateSelectionJob(Collection<FXOMObject> newSelectedObjects, EditorController editorController) {
        super(editorController);

        assert newSelectedObjects != null; // But possibly empty
        if (newSelectedObjects.isEmpty()) {
            newSelectionGroup = null;
        } else {
            newSelectionGroup = new ObjectSelectionGroup(newSelectedObjects, newSelectedObjects.iterator().next(), null);
        }
    }

    /*
     * Job
     */

    @Override
    public boolean isExecutable() {
        return true;
    }

    @Override
    public void execute() {
        final Selection selection = getEditorController().getSelection();
        
        // Saves the current selection
        try {
            if (selection.getGroup() == null) {
                this.oldSelectionGroup = null;
            } else {
                this.oldSelectionGroup = selection.getGroup().clone();
            }
        } catch(CloneNotSupportedException x) {
            throw new RuntimeException("Bug", x);
        }
        
        // Now same as redo()
        redo();
    }

    @Override
    public void undo() {
        final Selection selection = getEditorController().getSelection();
        selection.select(oldSelectionGroup);
        assert selection.isValid(getEditorController().getFxomDocument());
    }

    @Override
    public void redo() {
        final Selection selection = getEditorController().getSelection();
        selection.select(newSelectionGroup);
        assert selection.isValid(getEditorController().getFxomDocument());
    }

    @Override
    public String getDescription() {
        // Not expected to reach the user
        return getClass().getSimpleName();
    }
    
}
