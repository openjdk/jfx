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
package com.oracle.javafx.scenebuilder.kit.editor.drag;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;

/**
 *
 */
class LiveUpdater {
    
    private final AbstractDragSource dragSource;
    private final EditorController editorController;
    private AbstractDropTarget dropTarget;
    private Job dropTargetMoveJob;
    
    public LiveUpdater(AbstractDragSource dragSource, EditorController editorController) {
        assert dragSource != null;
        assert editorController != null;
        
        this.dragSource = dragSource;
        this.editorController = editorController;
    }
    
    public void setDropTarget(AbstractDropTarget newDropTarget) {
        assert (newDropTarget == null) || (this.dropTarget != newDropTarget);
        
        /*
         *   \ newDropTarget |                     |
         * this.dropTarget   |        null         |        non null
         * ------------------+---------------------+------------------------
         *                   |                     |          (A)
         *       null        |        nop          | move to new drop target
         *                   |                     |           
         * ------------------+---------------------+------------------------
         *                   |        (B)          |          (C)
         *     not null      |    undo last move   |     undo last move
         *                   |                     | move to new drop target
         * ------------------+---------------------+------------------------
         * 
         */
        
        if (this.dropTarget != null) {
            assert this.dropTargetMoveJob != null;
            this.dropTargetMoveJob.undo();
        }
        this.dropTarget = newDropTarget;
        this.dropTargetMoveJob = null;
        if (this.dropTarget != null) {
            this.dropTargetMoveJob = this.dropTarget.makeDropJob(dragSource, editorController);
            this.dropTargetMoveJob.execute();
        }
    }
    
    public AbstractDropTarget getDropTarget() {
        return dropTarget;
    }
}
