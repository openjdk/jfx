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

package com.oracle.javafx.scenebuilder.kit.fxom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.scene.control.ToggleGroup;

/**
 *
 */
public class FXOMFxIdIndex {
    
    private final FXOMDocument fxomDocument;
    private final Map<String, FXOMObject> fxIds;
    
    public FXOMFxIdIndex(FXOMDocument fxomDocument) {
        assert fxomDocument != null;
        this.fxomDocument = fxomDocument;
        this.fxIds = fxomDocument.collectFxIds();
    }

    public FXOMDocument getFxomDocument() {
        return fxomDocument;
    }
    
    public FXOMObject lookup(String fxId) {
        assert fxId != null;
        return fxIds.get(fxId);
    }
    
    public Map<String, FXOMObject> getFxIds() {
        return fxIds;
    }
    
    public List<FXOMInstance> collectToggleGroups() {
        final List<FXOMInstance> result = new ArrayList<>();
        
        for (Map.Entry<String, FXOMObject> e : fxIds.entrySet()) {
            final FXOMObject fxomObject = e.getValue();
            if (fxomObject instanceof FXOMInstance) {
                final FXOMInstance fxomInstance = (FXOMInstance) fxomObject;
                if (fxomInstance.getDeclaredClass() == ToggleGroup.class) {
                    result.add(fxomInstance);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Returns true if tree below fxomObject does not contain any fx:reference
     * pointing outside of the tree.
     * 
     * @param fxomObject an fxom object (never null)
     * @return true if fxomObject subtree is self-contained
     */
    public boolean isSelfContained(FXOMObject fxomObject) {
        final List<FXOMIntrinsic> references = fxomObject.collectReferences(null);
        int externalCount = 0;
        for (FXOMIntrinsic reference : references) {
            assert reference.getSource() != null;
            final FXOMObject target = fxIds.get(reference.getSource());
            assert target != null;
            if (target.isDescendantOf(fxomObject) == false) {
                externalCount++;
            }
        }
        
        return externalCount == 0;
    }
    
    
    /**
     * Facility : creates an FXOMFxIdIndex and check if the specified object is
 self-contained. Use this only if you have one object to check.
     * 
     * @param fxomObject an fxom object (cannot be null)
     * @return true if fxom object is self contained
     */
    public static boolean isSelfContainedObject(FXOMObject fxomObject) {
        final FXOMFxIdIndex fxomIndex = new FXOMFxIdIndex(fxomObject.getFxomDocument());
        return fxomIndex.isSelfContained(fxomObject);
    }
}
