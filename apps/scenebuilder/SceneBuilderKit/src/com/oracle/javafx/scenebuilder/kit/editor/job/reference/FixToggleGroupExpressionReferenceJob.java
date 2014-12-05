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
import com.oracle.javafx.scenebuilder.kit.editor.job.InlineDocumentJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.AddPropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RemoveObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RemovePropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ReplacePropertyValueJobT;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNodes;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class FixToggleGroupExpressionReferenceJob extends InlineDocumentJob {
    
    private final FXOMPropertyT reference;

    public FixToggleGroupExpressionReferenceJob(
            FXOMPropertyT reference, 
            EditorController editorController) {
        super(editorController);
        
        assert reference != null;
        assert reference.getFxomDocument() == editorController.getFxomDocument();
        
        this.reference = reference;
    }
    
    /*
     * InlineDocumentJob
     */
    @Override
    protected List<Job> makeAndExecuteSubJobs() {
        final List<Job> result = new LinkedList<>();
        
        // 1) Locates the referee
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final String fxId = FXOMNodes.extractReferenceSource(reference);
        final FXOMObject referee = fxomDocument.searchWithFxId(fxId);
        
        /*
         *    <RadioButton toggleGroup="$oxebo" />          // reference    //NOI18N
         *    ...
         *    <RadioButton>
         *       <toggleGroup>
         *           <ToggleGroup fx:id="oxebo" />          // referee      //NOI18N
         *       </toggleGroup>
         *    </RadioButton>
         */
            
        // 2) Finds or create the matching toggle group
        if (referee != null) {
            assert referee.getParentProperty() != null;
            assert referee.getParentProperty().getParentInstance() != null;
            
            // 2a.1) Toggle group is available : disconnect it and re-use it
            final FXOMInstance parentInstance 
                    = referee.getParentProperty().getParentInstance();
            final RemoveObjectJob removeJob 
                    = new RemoveObjectJob(referee, getEditorController());
            removeJob.execute();
            result.add(removeJob);
            
            // 2a.2) Replace the reference by the toggleGroup
            final Job replaceJob = new ReplacePropertyValueJobT(reference, 
                    referee, getEditorController());
            replaceJob.execute();
            result.add(replaceJob);

            // 2a.3) Put reference at referee previous place
            final Job addJob = new AddPropertyJob(reference, parentInstance, 
                    -1, getEditorController());
            addJob.execute();
            result.add(addJob);
            
        } else {
            
            // 2b.1) Removes the reference
            final FXOMInstance targetInstance = reference.getParentInstance();
            final Job removeJob = new RemovePropertyJob(reference, getEditorController());
            removeJob.execute();
            result.add(removeJob);
            
            // 2b.2) Creates and adds toggle group
            final FXOMPropertyC newToggleGroup = FXOMNodes.makeToggleGroup(fxomDocument, fxId);
            final Job addJob = new AddPropertyJob(newToggleGroup, 
                    targetInstance, -1, getEditorController());
            addJob.execute();
            result.add(addJob);
        }
        
        return result;
    }

    @Override
    protected String makeDescription() {
        return getClass().getSimpleName(); // Not expected to reach the user
    }

    @Override
    public boolean isExecutable() {
        final PrefixedValue pv = new PrefixedValue(reference.getValue());
        return pv.isExpression();
    }
}
