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

package com.oracle.javafx.scenebuilder.kit.editor.job.togglegroup;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.AddPropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.RemovePropertyJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNodes;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.List;
import javafx.scene.control.ToggleGroup;

/**
 *
 */
public class AdjustToggleGroupJob extends Job {
    
    private static final PropertyName toggleGroupName 
            = new PropertyName("toggleGroup"); //NOI18N
    
    private final String toggleGroupId;
    private final BatchJob batchJob ;

    public AdjustToggleGroupJob(String toggleGroupId, EditorController editorController) {
        super(editorController);
        
        assert toggleGroupId != null;
        
        this.toggleGroupId = toggleGroupId;
        this.batchJob = new BatchJob(getEditorController());
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
        
        /*
         * 1) search all the toggleGroup properties
         * 2) extract the ones that refer to toggleGroupInstance
         * 3) serialize all the objects of the scene graph
         * 4) check that toggleGroupInstance parent is before any referal
         *      - if no, generate a job to swap toggleGroupInstance 
         */
        
        final FXOMDocument fxomDocument
                = getEditorController().getFxomDocument();
        final FXOMObject fxomRoot 
                = fxomDocument.getFxomRoot();
        final FXOMObject toggleGroupInstance
                = fxomRoot.searchWithFxId(toggleGroupId);
        final List<FXOMPropertyT> references
                = FXOMNodes.collectToggleGroupReferences(fxomRoot, toggleGroupId);
        
        if (references.size() >= 1) {
            final FXOMPropertyT firstReference = references.get(0);
            final FXOMInstance firstReferencer = firstReference.getParentInstance();
            
            if (toggleGroupInstance == null) {
                // We have references but declaration has gone.
                // Let's transform the first reference into a declaration.
                
                final FXOMInstance newToggleGroupInstance
                        = new FXOMInstance(fxomDocument, ToggleGroup.class);
                newToggleGroupInstance.setFxId(toggleGroupId);
                final FXOMPropertyC declarationProperty
                        = new FXOMPropertyC(fxomDocument, toggleGroupName, newToggleGroupInstance);
                
                final RemovePropertyJob removeFirstReferenceJob
                        = new RemovePropertyJob(firstReference, getEditorController());
                final AddPropertyJob addDeclarationJob
                        = new AddPropertyJob(declarationProperty, 
                                firstReference.getParentInstance(),
                                -1, getEditorController());
                batchJob.addSubJob(removeFirstReferenceJob);
                batchJob.addSubJob(addDeclarationJob);
            } else {
                // We have some references and a declaration.
                // We need to make sure that declaration is *before* any references.
                
                final FXOMPropertyC declaration = toggleGroupInstance.getParentProperty();
                final FXOMInstance declarer = declaration.getParentInstance();

                /*
                 * declarer is the object holding toggle group declaration
                 * 
                 *      <RadioButton>
                 *          <toggleGroup><ToggleGroup fx:id="niceToggleGroup" /> </toggleGroup>   //NOI18N
                 *      </RadioButton>
                 * 
                 * firstReferencer is the object holding the first reference to the toggle group
                 * 
                 *      <RadioButton toggleGroup="$niceToggleGroup" />   //NOI18N
                 * 
                 * firstReferencer position must be after declarer position.
                 * If not, we swap 'firstReference' and 'declaration'.
                 */

                final List<FXOMObject> objectSequence = FXOMNodes.serializeObjects(fxomRoot);
                final int firstReferencerIndex = objectSequence.indexOf(firstReferencer);
                final int declarerIndex = objectSequence.indexOf(declarer);
                assert firstReferencerIndex != -1;
                assert declarerIndex != -1;

                if (firstReferencerIndex < declarerIndex) {
                    // Let's create jobs for swapping 
                    //      firstReference 
                    // with
                    //      declaration

                    final RemovePropertyJob removeFirstReferenceJob
                            = new RemovePropertyJob(firstReference, getEditorController());
                    final RemovePropertyJob removeDeclarationJob
                            = new RemovePropertyJob(declaration, getEditorController());
                    final AddPropertyJob addDeclarationJob
                            = new AddPropertyJob(declaration, firstReference.getParentInstance(), 
                                    -1, getEditorController());
                    final AddPropertyJob addReferenceJob
                            = new AddPropertyJob(firstReference, declarer, -1, getEditorController());
                    batchJob.addSubJob(removeFirstReferenceJob);
                    batchJob.addSubJob(removeDeclarationJob);
                    batchJob.addSubJob(addDeclarationJob);
                    batchJob.addSubJob(addReferenceJob);
                }
            }
        }
            
        if (batchJob.isExecutable()) {
            batchJob.execute();
        }
        
    }

    @Override
    public void undo() {
        if (batchJob.isExecutable()) {
            batchJob.undo();
        }
    }

    @Override
    public void redo() {
        if (batchJob.isExecutable()) {
            batchJob.redo();
        }
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName(); // Should not reach the user
    }
    
    
}
