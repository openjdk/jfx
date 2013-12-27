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
package com.oracle.javafx.scenebuilder.kit.metadata.util;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public class DesignHierarchyPath {
    
    private final List<FXOMObject> pathItems = new ArrayList<>();
    
    public DesignHierarchyPath() {
    }
    
    public DesignHierarchyPath(FXOMObject fxomObject) {
        assert fxomObject != null;
        FXOMObject o = fxomObject;
        do {
            pathItems.add(0, o);
            o = o.getParentObject();
        } while (o != null);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DesignHierarchyPath) {
            final DesignHierarchyPath path = (DesignHierarchyPath) obj;
            return this.pathItems.equals(path.pathItems);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.pathItems);
        return hash;
    }
    
    public int getSize() {
        return pathItems.size();
    }
    
    public boolean isEmpty() {
        return pathItems.isEmpty();
    }
    
    public FXOMObject getRoot() {
        final FXOMObject result;
        
        if (pathItems.isEmpty()) {
            result = null;
        } else {
            result = pathItems.get(0);
        }
        
        return result;
    }
    
    public FXOMObject getLeaf() {
        final FXOMObject result;
        
        if (pathItems.isEmpty()) {
            result = null;
        } else {
            result = pathItems.get(pathItems.size()-1);
        }
        
        return result;
    }
    
    public DesignHierarchyPath getCommonPathWith(DesignHierarchyPath another) {
        final DesignHierarchyPath result = new DesignHierarchyPath();
        
        assert another != null;
        
        int i = 0, count = Math.min(this.getSize(), another.getSize());
        while ((i < count) && this.pathItems.get(i) == another.pathItems.get(i)) {
            result.pathItems.add(this.pathItems.get(i));
            i++;
        }
        
        return result;
    }
}
