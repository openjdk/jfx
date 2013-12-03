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
package com.oracle.javafx.scenebuilder.kit.editor.drag.target;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.Objects;

/**
 *
 */
public class CollectionDropTarget extends AbstractDropTarget {
    
    private final FXOMCollection targetCollection;
    private final int targetIndex;

    public CollectionDropTarget(FXOMCollection targetCollection, int targetIndex) {
        this.targetCollection = targetCollection;
        this.targetIndex = targetIndex;
    }

    public FXOMCollection getTargetCollection() {
        return targetCollection;
    }

    public int getTargetIndex() {
        return targetIndex;
    }
    
    /*
     * AbstractDropTarget
     */
    @Override
    public FXOMObject getTargetObject() {
        return targetCollection;
    }

    @Override
    public boolean acceptDragSource(AbstractDragSource dragSource) {
        assert dragSource != null;
        
        // TODO(elp) : can we really put any kind of FXOMObject in a collection ?
        return dragSource.getDraggedObjects().isEmpty() == false;
    }

    @Override
    public Job makeDropJob(AbstractDragSource dragSource, EditorController editorController) {
        throw new UnsupportedOperationException("To be implemented"); //NOI18N
    }
    
    /*
     * Object
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.targetCollection);
        hash = 37 * hash + this.targetIndex;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CollectionDropTarget other = (CollectionDropTarget) obj;
        if (!Objects.equals(this.targetCollection, other.targetCollection)) {
            return false;
        }
        if (this.targetIndex != other.targetIndex) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CollectionDropTarget{" + "targetCollection=" + targetCollection + ", targetIndex=" + targetIndex + '}'; //NOI18N
    }
    
    
}
