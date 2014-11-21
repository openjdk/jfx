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
package com.oracle.javafx.scenebuilder.kit.editor.job;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import java.util.List;

/**
 * This Job updates the FXOM document AND the selection at execution time.
 *
 * The sub jobs are created and executed just after.
 */
public abstract class InlineSelectionJob extends InlineDocumentJob {

    private AbstractSelectionGroup oldSelectionGroup;
    private AbstractSelectionGroup newSelectionGroup;

    public InlineSelectionJob(EditorController editorController) {
        super(editorController);
    }

    protected final AbstractSelectionGroup getOldSelectionGroup() {
        return oldSelectionGroup;
    }

    protected abstract AbstractSelectionGroup getNewSelectionGroup();

    @Override
    public final void execute() {
        final Selection selection = getEditorController().getSelection();
        try {
            selection.beginUpdate();
            oldSelectionGroup = selection.getGroup() == null ? null
                    : selection.getGroup().clone();
            super.execute();
            newSelectionGroup = getNewSelectionGroup();
            selection.select(newSelectionGroup);
            selection.endUpdate();

        } catch (CloneNotSupportedException x) {
            // Emergency code
            throw new RuntimeException(x);
        }
    }

    @Override
    public final void undo() {
        final Selection selection = getEditorController().getSelection();
        selection.beginUpdate();
        super.undo();
        selection.select(oldSelectionGroup);
        selection.endUpdate();
    }

    @Override
    public final void redo() {
        final Selection selection = getEditorController().getSelection();
        selection.beginUpdate();
        super.redo();
        selection.select(newSelectionGroup);
        selection.endUpdate();
    }

    @Override
    protected abstract List<Job> makeAndExecuteSubJobs();
}
