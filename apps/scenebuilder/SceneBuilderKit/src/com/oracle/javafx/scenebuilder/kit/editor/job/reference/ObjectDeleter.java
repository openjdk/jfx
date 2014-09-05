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

package com.oracle.javafx.scenebuilder.kit.editor.job.reference;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RemoveNodeJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RemoveObjectJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNode;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNodes;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import java.util.LinkedList;
import java.util.List;

/**
 */

public class ObjectDeleter {
    
    private final EditorController editorController;
    private final FXOMDocument fxomDocument;
    private final List<Job> executedJobs = new LinkedList<>();
    
    public ObjectDeleter(EditorController editorController) {
        assert editorController != null;
        assert editorController.getFxomDocument() != null;
        this.editorController = editorController;
        this.fxomDocument = editorController.getFxomDocument();
    }
    
    public void delete(FXOMObject target) {
        final FXOMNode node = prepareDeleteObject(target, target);
        
        if (node == target) {
            final RemoveObjectJob removeJob = new RemoveObjectJob(target, editorController);
            removeJob.execute();
            executedJobs.add(removeJob);
        }
    }
    
    public void prepareDelete(FXOMObject target) {
        assert target != null;
        assert target.getFxomDocument() == fxomDocument;
        assert fxomDocument.getFxomRoot() != null; // At least target
        
        prepareDeleteObject(target, target);
    }
    
    public List<Job> getExecutedJobs() {
        return new LinkedList<>(executedJobs);
    }
    
    
    /*
     * Private
     */
    
    private FXOMNode prepareDeleteObject(FXOMObject node, FXOMObject target) {
        final FXOMNode result;
        
        final String nodeFxId = node.getFxId();
        if (nodeFxId == null) {
            // node has no fx:id : it can be deleted safely
            result = node;
        } else {
            final FXOMObject fxomRoot = fxomDocument.getFxomRoot();
            final List<FXOMNode> references = fxomRoot.collectReferences(nodeFxId, target);
            if (references.isEmpty()) {
                // node has an fx:id but this one is not referenced
                // outside of the delete target : it can be deleted safely
                result = node;
            } else {
                // node has an fx:id referenced outside of the delete target
                // => we find the first strong reference R to it
                // => we remove all the weak references between node and R
                // => we combine node with R
                FXOMNode firstReference = null;
                for (FXOMNode r : references) {
                    if (FXOMNodes.isWeakReference(r)) {
                        // This weak reference will become a forward reference
                        // after the deletion => we remove it.
                        final Job clearJob = new RemoveNodeJob(r, editorController);
                        clearJob.execute();
                        executedJobs.add(clearJob);
                    } else {
                        firstReference = r;
                        break;
                    }
                }
                
                if (firstReference == null) {
                    // node has only weak references ; those references have
                    // been removed => node can be delete safely
                    result = node;
                } else {
                    // we combine firstReference with node ie node is 
                    // disconnected from its parent and put in place of
                    // firstReference
                    final Job combineJob = new CombineReferenceJob(firstReference, editorController);
                    combineJob.execute();
                    executedJobs.add(combineJob);
                    result = null;
                }
            }
        }
        
        if (result == node) {
            if (node instanceof FXOMInstance) {
                final FXOMInstance fxomInstance = (FXOMInstance) node;
                for (FXOMProperty p : new LinkedList<>(fxomInstance.getProperties().values())) {
                    if (p instanceof FXOMPropertyC) {
                        final FXOMPropertyC cp = (FXOMPropertyC) p;
                        for (FXOMObject value : new LinkedList<>(cp.getValues())) {
                            prepareDeleteObject(value, target);
                        }
                    }
                }
            } else if (result instanceof FXOMCollection) {
                final FXOMCollection fxomCollection = (FXOMCollection) result;
                for (FXOMObject i : new LinkedList<>(fxomCollection.getItems())) {
                    prepareDeleteObject(i, target);
                }
            } // else no prework needed
        }
        
        return result;
    }
}
