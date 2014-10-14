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
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCloner;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNode;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNodes;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.JavaLanguage;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ReferencesUpdater {
    
    private final EditorController editorController;
    private final FXOMDocument fxomDocument;
    private final List<Job> executedJobs = new LinkedList<>();
    private final Set<String> declaredFxIds = new HashSet<>();
    private final FXOMCloner cloner;
    
    public ReferencesUpdater(EditorController editorController) {
        assert editorController != null;
        assert editorController.getFxomDocument() != null;
        this.editorController = editorController;
        this.fxomDocument = editorController.getFxomDocument();
        this.cloner = new FXOMCloner(this.fxomDocument);
    }
    
    public void update() {
        if (fxomDocument.getFxomRoot() != null) {
            declaredFxIds.clear();
            update(fxomDocument.getFxomRoot());
        }
    }
    
    public List<Job> getExecutedJobs() {
        return new LinkedList<>(executedJobs);
    }
    
    
    /*
     * Private
     */
    
    private void update(FXOMNode node) {
        if (node instanceof FXOMCollection) {
            updateCollection((FXOMCollection) node);
        } else if (node instanceof FXOMInstance) {
            updateInstance((FXOMInstance) node);
        } else if (node instanceof FXOMIntrinsic) {
            updateIntrinsic((FXOMIntrinsic) node);
        } else if (node instanceof FXOMPropertyC) {
            updatePropertyC((FXOMPropertyC) node);
        } else if (node instanceof FXOMPropertyT) {
            updatePropertyT((FXOMPropertyT) node);
        } else {
            throw new RuntimeException("Bug"); //NOI18N
        }
    }
    
    
    private void updateCollection(FXOMCollection collection) {
        if (collection.getFxId() != null) {
            declaredFxIds.add(collection.getFxId());
        }
        final List<FXOMObject> items = collection.getItems();
        for (int i = 0, count = items.size(); i < count; i++) {
            update(items.get(i));
        }
    }
    
    
    private void updateInstance(FXOMInstance instance) {
        if (instance.getFxId() != null) {
            declaredFxIds.add(instance.getFxId());
        }
        final Map<PropertyName, FXOMProperty> properties = instance.getProperties();
        final List<PropertyName> names = new LinkedList<>(properties.keySet());
        for (PropertyName propertyName : names) {
            update(properties.get(propertyName));
        }
    }
    
    
    private void updateIntrinsic(FXOMIntrinsic intrinsic) {
        switch(intrinsic.getType()) {
            case FX_REFERENCE:
            case FX_COPY:
                updateReference(intrinsic, intrinsic.getSource());
                break;
            default:
                break;
        }
    }
    
    
    private void updatePropertyC(FXOMPropertyC property) {
        final List<FXOMObject> values = property.getValues();
        for (int i = 0, count = values.size(); i < count; i++) {
            update(values.get(i));
        }
    }
    
    
    private void updatePropertyT(FXOMPropertyT property) {
        final PrefixedValue pv = new PrefixedValue(property.getValue());
        if (pv.isExpression()) {
            final String suffix = pv.getSuffix();
            if (JavaLanguage.isIdentifier(suffix)) {
                updateReference(property, suffix);
            }
        }
    }
    
    
    private void updateReference(FXOMNode r, String fxId) {
        assert (r instanceof FXOMPropertyT) || (r instanceof FXOMIntrinsic);
        assert fxId != null;
        
        if (declaredFxIds.contains(fxId) == false) {
            // r is a forward reference
            //
            // 0) r is a toggleGroup reference
            //    => if toggle group exists, we swap it with the reference
            //    => if not, replace the reference by a new toggle group
            // 1) r is a weak reference (like labelFor)
            //    => we remove the reference
            // 2) else r is a strong reference
            //    => we expand the reference
            
            
            final FXOMObject declarer = fxomDocument.searchWithFxId(fxId);

            // 0)
            if (FXOMNodes.isToggleGroupReference(r)) {
                final Job fixJob = new FixToggleGroupReferenceJob(r, editorController);
                fixJob.execute();
                executedJobs.add(fixJob);
                declaredFxIds.add(fxId);
            }
            
            // 1
            else if (FXOMNodes.isWeakReference(r) || (declarer == null)) {
                final Job removeJob = new RemoveNodeJob(r, editorController);
                removeJob.execute();
                executedJobs.add(removeJob);
                
            // 2)
            } else {
                
                final Job expandJob = new ExpandReferenceJob(r, cloner, editorController);
                expandJob.execute();
                executedJobs.add(expandJob);
            }
        }
    }
    
}
