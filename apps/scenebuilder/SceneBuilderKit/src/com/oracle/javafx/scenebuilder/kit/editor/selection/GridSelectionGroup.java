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
package com.oracle.javafx.scenebuilder.kit.editor.selection;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.IntegerPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

/**
 *
 * 
 */
public class GridSelectionGroup extends AbstractSelectionGroup {
    
    public enum Type { ROW, COLUMN };
    
    private final FXOMObject parentObject;
    private final Type type;
    private final Set<Integer> indexes = new HashSet<>();
    
    public GridSelectionGroup(FXOMObject parentObject, Type type, int index) {
        assert parentObject != null;
        assert parentObject.getSceneGraphObject() instanceof GridPane;
        assert index >= 0;
        
        this.parentObject = parentObject;
        this.type = type;
        this.indexes.add(index);
    }

    public GridSelectionGroup(FXOMObject parentObject, Type type, Set<Integer> indexes) {
        assert parentObject != null;
        assert parentObject.getSceneGraphObject() instanceof GridPane;
        assert indexes != null;
        assert indexes.isEmpty() == false;
        
        this.parentObject = parentObject;
        this.type = type;
        this.indexes.addAll(indexes);
    }

    public FXOMObject getParentObject() {
        return parentObject;
    }

    public Type getType() {
        return type;
    }
    
    public Set<Integer> getIndexes() {
        return Collections.unmodifiableSet(indexes);
    }    
    
    public List<FXOMInstance> collectConstraintInstances() {
        final List<FXOMInstance> result;
        
        switch(type) {
            case ROW:
                result = collectRowConstraintsInstances();
                break;
            case COLUMN:
                result = collectColumnConstraintsInstances();
                break;
            default:
                throw new RuntimeException("Bug");
        }
        
        return result;
    }
    
    public List<FXOMObject> collectSelectedObjects() {
        final List<FXOMObject> result;
        
        switch(type) {
            case ROW:
                result = collectSelectedObjectsInRow();
                break;
            case COLUMN:
                result = collectSelectedObjectsInColumn();
                break;
            default:
                throw new RuntimeException("Bug");
        }
        
        return result;
    }
    
    /*
     * AbstractSelectionGroup
     */
    
    @Override
    public FXOMObject getAncestor() {
        return parentObject;
    }

    @Override
    public boolean isValid(FXOMDocument fxomDocument) {
        assert fxomDocument != null;
        
        final boolean result;
        final FXOMObject fxomRoot = fxomDocument.getFxomRoot();
        if (fxomRoot == null) {
            result = false;
        } else {
            result = (parentObject == fxomRoot) || parentObject.isDescendantOf(fxomRoot);
        }
        
        return result;
    }
    
    
    /*
     * Cloneable
     */
    @Override
    public GridSelectionGroup clone() throws CloneNotSupportedException {
        return (GridSelectionGroup)super.clone();
    }
    
    
    /*
     * Object
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.parentObject);
        hash = 47 * hash + Objects.hashCode(this.type);
        hash = 47 * hash + Objects.hashCode(this.indexes);
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
        final GridSelectionGroup other = (GridSelectionGroup) obj;
        if (!Objects.equals(this.parentObject, other.parentObject)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        if (!Objects.equals(this.indexes, other.indexes)) {
            return false;
        }
        return true;
    }
    
    
    /*
     * Private
     */
    
    static private final PropertyName rowConstraintsName
            = new PropertyName("rowConstraints");
    
    private List<FXOMInstance> collectRowConstraintsInstances() {
        final List<FXOMInstance> result = new ArrayList<>();
        
        final FXOMInstance gridPaneInstance 
                = (FXOMInstance) parentObject;
        final FXOMProperty fxomProperty 
                = gridPaneInstance.getProperties().get(rowConstraintsName);
        if (fxomProperty != null) {
            assert fxomProperty instanceof FXOMPropertyC;
            final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
            int index = 0;
            for (FXOMObject v : fxomPropertyC.getValues()) {
                assert v.getSceneGraphObject() instanceof RowConstraints;
                assert v instanceof FXOMInstance;
                if (indexes.contains(index++)) {
                    result.add((FXOMInstance)v);
                }
            }
        }
        
        return result;
    }
    
    static private final PropertyName columnConstraintsName
            = new PropertyName("columnConstraints");
    
    private List<FXOMInstance> collectColumnConstraintsInstances() {
        final List<FXOMInstance> result = new ArrayList<>();
        
        final FXOMInstance gridPaneInstance 
                = (FXOMInstance) parentObject;
        final FXOMProperty fxomProperty 
                = gridPaneInstance.getProperties().get(columnConstraintsName);
        if (fxomProperty != null) {
            assert fxomProperty instanceof FXOMPropertyC;
            final FXOMPropertyC fxomPropertyC = (FXOMPropertyC) fxomProperty;
            int index = 0;
            for (FXOMObject v : fxomPropertyC.getValues()) {
                assert v.getSceneGraphObject() instanceof ColumnConstraints;
                assert v instanceof FXOMInstance;
                if (indexes.contains(index++)) {
                    result.add((FXOMInstance)v);
                }
            }
        }
        
        return result;
    }
    
    
    private static final IntegerPropertyMetadata columnIndexMeta =
            new IntegerPropertyMetadata(
                new PropertyName("columnIndex", GridPane.class), //NOI18N
                true, /* readWrite */
                0, /* defaultValue */
                InspectorPath.UNUSED);

    private List<FXOMObject> collectSelectedObjectsInColumn() {
        final List<FXOMObject> result = new ArrayList<>();
        
        final DesignHierarchyMask m = new DesignHierarchyMask(parentObject);
        assert m.isAcceptingSubComponent();
        
        for (int i = 0, count = m.getSubComponentCount(); i <  count; i++) {
            final FXOMObject childObject = m.getSubComponentAtIndex(i);
            if (childObject instanceof FXOMInstance) {
                final FXOMInstance childInstance = (FXOMInstance) childObject;
                if (indexes.contains(columnIndexMeta.getValue(childInstance))) {
                    // child belongs to a selected column
                    result.add(childInstance);
                }
            }
        }
        
        return result;
    }
    
    private static final IntegerPropertyMetadata rowIndexMeta =
            new IntegerPropertyMetadata(
                new PropertyName("rowIndex", GridPane.class), //NOI18N
                true, /* readWrite */
                0, /* defaultValue */
                InspectorPath.UNUSED);

    private List<FXOMObject> collectSelectedObjectsInRow() {
        final List<FXOMObject> result = new ArrayList<>();
        
        final DesignHierarchyMask m = new DesignHierarchyMask(parentObject);
        assert m.isAcceptingSubComponent();
        
        for (int i = 0, count = m.getSubComponentCount(); i <  count; i++) {
            final FXOMObject childObject = m.getSubComponentAtIndex(i);
            if (childObject instanceof FXOMInstance) {
                final FXOMInstance childInstance = (FXOMInstance) childObject;
                if (indexes.contains(rowIndexMeta.getValue(childInstance))) {
                    // child belongs to a selected column
                    result.add(childInstance);
                }
            }
        }
        
        return result;
    }
    
}
