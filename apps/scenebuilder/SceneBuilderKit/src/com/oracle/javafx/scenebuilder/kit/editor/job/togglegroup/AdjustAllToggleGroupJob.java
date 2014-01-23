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
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.CompositeJob;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIndex;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class AdjustAllToggleGroupJob extends CompositeJob {

    public AdjustAllToggleGroupJob(EditorController editorController) {
        super(editorController);
    }

    
    /*
     * CompositeJob
     */
    
    @Override
    protected List<Job> makeSubJobs() {
        
        /*
         * Search for all the toggle group ids:
         *   1) declarations <ToggleGroup fx:id="niceToggleGroup" />            //NOI18N
         *   2) references   <RadioButton toggleGroup="$niceToggleGroup" />     //NOI18N
         */
        final Set<String> toggleGroupIds = new HashSet<>();
        
        /*
         * #1
         */
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final FXOMIndex fxomIndex = new FXOMIndex(fxomDocument);
        for (FXOMInstance toggleGroupInstance : fxomIndex.collectToggleGroups()) {
            final String toggleGroupId = toggleGroupInstance.getFxId();
            if (toggleGroupId != null) {
                toggleGroupIds.add(toggleGroupId);
            }
        }
        
        /*
         * #2
         */
        final PropertyName toggleGroupName = new PropertyName("toggleGroup"); //NOI18N
        for (FXOMProperty p : fxomDocument.getFxomRoot().collectProperties(toggleGroupName)) {
            if (p instanceof FXOMPropertyT) {
                final FXOMPropertyT pt = (FXOMPropertyT) p;
                final PrefixedValue pv = new PrefixedValue(pt.getValue());
                if (pv.isExpression()) {
                    /*
                     * p is an FXOMPropertyT like this:
                     * 
                     * <.... toggleGroup="$id" .... />              //NOI18N
                     */
                    toggleGroupIds.add(pv.getSuffix());
                }
            }
        }
        
        /*
         * Creates AdjustToggleGroup job for each toggle group id
         */
        
        final List<Job> result = new ArrayList<>();
        for (String toggleGroupId : toggleGroupIds) {
            final Job adjustJob = new AdjustToggleGroupJob(toggleGroupId, getEditorController());
            assert adjustJob.isExecutable();
            result.add(adjustJob);
        }
        
        return result;
    }

    @Override
    protected String makeDescription() {
        return getClass().getSimpleName(); // Should not reach user
    }
    
}
