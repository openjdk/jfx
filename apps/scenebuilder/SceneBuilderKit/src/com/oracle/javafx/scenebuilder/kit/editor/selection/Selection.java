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

import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup.Type;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

/**
 * Selection class represents the selected objects for an editor controller.
 * <p>
 * Selected objects are represented an instance of {@link AbstractSelectionGroup}.
 * 
 * 
 */
public class Selection {
    
    private AbstractSelectionGroup group;
    private final SimpleIntegerProperty revision = new SimpleIntegerProperty();
    private boolean lock;
    private long lastListenerInvocationTime;
    private int updateDepth;
    
    /**
     * Returns the property holding the revision number of this selection.
     * Selection class adds +1 to this number each time the selection changes.
     * 
     * @return the property holding the revision number of this selection.
     */
    public ReadOnlyIntegerProperty revisionProperty() {
        return revision;
    }
    
    /**
     * Returns the revision number of this selection.
     * 
     * @return the revision number of this selection.
     */
    public int getRevision() {
        return revision.get();
    }
    
    /**
     * Replaces the selected items by the specified fxom object.
     * This routine adds +1 to the revision number.
     * 
     * @param fxomObject the object to be selected
     */
    public void select(FXOMObject fxomObject) {
        assert fxomObject != null;
        
        select(fxomObject, null);
    }
    
    /**
     * Replaces the selected items by the specified fxom object and hit node.
     * This routine adds +1 to the revision number.
     * 
     * @param fxomObject the object to be selected
     * @param hitPoint null or the point hit by the mouse during selection
     */
    public void select(FXOMObject fxomObject, Point2D hitPoint) {
        
        assert fxomObject != null;
        
        select(new ObjectSelectionGroup(fxomObject, hitPoint));
    }
    
    /**
     * Replaces the selected items by the specified fxom objects.
     * This routine adds +1 to the revision number.
     * 
     * @param fxomObjects the objects to be selected
     */
    public void select(Collection<FXOMObject> fxomObjects) {
        assert fxomObjects != null;
        
        final FXOMObject hitObject;
        if (fxomObjects.isEmpty()) {
            hitObject = null;
        } else {
            hitObject = fxomObjects.iterator().next();
        }
        
        select(fxomObjects, hitObject, null);
    }
    
    /**
     * Replaces the selected items by the specified fxom objects.
     * This routine adds +1 to the revision number.
     * 
     * @param fxomObjects the objects to be selected
     * @param hitObject the object hit by the mouse during selection
     * @param hitPoint null or the point hit by the mouse during selection
     */
    public void select(Collection<FXOMObject> fxomObjects, FXOMObject hitObject, Point2D hitPoint) {
        
        assert fxomObjects != null;
        
        final ObjectSelectionGroup newGroup;
        if (fxomObjects.isEmpty()) {
            newGroup = null;
        } else {
            newGroup = new ObjectSelectionGroup(fxomObjects, hitObject, hitPoint);
        }
        select(newGroup);
    }
    
    /**
     * Adds/removes the specified object from the selected items.
     * This routine adds +1 to the revision number.
     * 
     * @param fxomObject the object to be added/removed
     */
    public void toggleSelection(FXOMObject fxomObject) {
        toggleSelection(fxomObject, null);
    }
    
    /**
     * Adds/removes the specified object from the selected items.
     * This routine adds +1 to the revision number.
     * 
     * @param fxomObject the object to be added/removed
     * @param hitPoint null or the point hit by the mouse during selection
     */
    public void toggleSelection(FXOMObject fxomObject, Point2D hitPoint) {
        
        assert fxomObject != null;
        
        final ObjectSelectionGroup newGroup;
        if (group instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) group;
            final Set<FXOMObject> currentItems = osg.getItems();
            if (currentItems.contains(fxomObject)) {
                if (currentItems.size() == 1) {
                    // fxomObject is selected and is the last item
                    newGroup = null;
                } else {
                    final Set<FXOMObject> newItems = new HashSet<>();
                    newItems.addAll(currentItems);
                    newItems.remove(fxomObject);
                    final FXOMObject newHitItem = newItems.iterator().next();
                    newGroup = new ObjectSelectionGroup(newItems, newHitItem, null);
                }
            } else {
                final Set<FXOMObject> newItems = new HashSet<>();
                newItems.addAll(currentItems);
                newItems.add(fxomObject);
                newGroup = new ObjectSelectionGroup(newItems, fxomObject, hitPoint);
            }
        } else {
            newGroup = new ObjectSelectionGroup(fxomObject, hitPoint);
        }
        
        select(newGroup);
    }
    
    /**
     * Update the hit object and hit point of the current selection.
     * 
     * @param hitObject the object hit by the mouse during selection
     * @param hitPoint null or the point hit by the mouse during selection
     */
    public void updateHitObject(FXOMObject hitObject, Point2D hitPoint) {
        if (isSelected(hitObject)) {
            assert group instanceof ObjectSelectionGroup;
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) group;
            select(osg.getItems(), hitObject, hitPoint);
        } else {
            select(hitObject, hitPoint);
        }
    }
    
    /**
     * Returns true if the specified fxom object is part of this selection.
     * Conditions must be met:
     * 1) this selection should an ObjectSelectionGroup
     * 2) the fxom object should belong to this group.
     * 
     * @param fxomObject an fxom object
     * 
     * @return  true if this foxm object is selected.
     */
    public boolean isSelected(FXOMObject fxomObject) {
        final boolean result;
        
        assert fxomObject != null;
        
        if (group instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) group;
            result = osg.getItems().contains(fxomObject);
        } else {
            result = false;
        }
        
        return result;
    }
    
    
    public FXOMObject getHitItem() {
        final FXOMObject result;
        
        if (group instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) group;
            result = osg.getHitItem();
        } else if (group instanceof GridSelectionGroup) {
            final GridSelectionGroup gsg = (GridSelectionGroup) group;
            result = gsg.getParentObject();
        } else {
            result = null;
        }
        
        return result;
    }
    
    public Point2D getHitPoint() {
        final Point2D result;
        
        if (group instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) group;
            result = osg.getHitPoint();
        } else {
            result = null;
        }
        
        return result;
    }
    
    
    public Node findHitNode() {
        final Node result;
        
        if (group instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) group;
            result = osg.findHitNode();
        } else {
            result = null;
        }
        
        return result;
    }
    
    /**
     * Replaces the selected items by the specified column/row.
     * This routine adds +1 to the revision number.
     * 
     * @param gridPaneObject fxom object of the gridpane holding the column/row
     * @param feature column/row
     * @param featureIndex index of the column/row to be selected
     */
    public void select(FXOMInstance gridPaneObject, Type feature, int featureIndex) {
        
        assert gridPaneObject != null;
        assert gridPaneObject.getSceneGraphObject() instanceof GridPane;
        
        select(new GridSelectionGroup(gridPaneObject, feature, featureIndex));
    }
    
    /**
     * Adds/removes the specified column/row to/from the selected items.
     * This routine adds +1 to the revision number.
     * 
     * @param gridPaneObject fxom object of the gridpane holding the column/row
     * @param feature column/row
     * @param featureIndex index of the column/row to be selected
     */
    public void toggleSelection(FXOMInstance gridPaneObject, Type feature, int featureIndex) {
        
        assert gridPaneObject != null;
        assert gridPaneObject.getSceneGraphObject() instanceof GridPane;
        
        final AbstractSelectionGroup newGroup;
        if (group instanceof GridSelectionGroup) {
            final GridSelectionGroup gsg = (GridSelectionGroup) group;
            if (gsg.getType() == feature) {
                final Set<Integer> indexes = gsg.getIndexes();
                if (indexes.contains(featureIndex)) {
                    if (indexes.size() == 1) {
                        // featureIndex is the last selected index
                        // GridSelectionGroup -> ObjectSelectionGroup
                        newGroup = new ObjectSelectionGroup(gridPaneObject, null);
                    } else {
                        final Set<Integer> newIndexes = new HashSet<>();
                        newIndexes.addAll(indexes);
                        newIndexes.remove(featureIndex);
                        newGroup = new GridSelectionGroup(gridPaneObject, feature, newIndexes);
                    }
                } else {
                    final Set<Integer> newIndexes = new HashSet<>();
                    newIndexes.addAll(indexes);
                    newIndexes.add(featureIndex);
                    newGroup = new GridSelectionGroup(gridPaneObject, feature, newIndexes);
                }
            } else {
                newGroup = new GridSelectionGroup(gridPaneObject, feature, featureIndex);
            }
        } else {
            newGroup = new GridSelectionGroup(gridPaneObject, feature, featureIndex);
        }
        
        select(newGroup);
    }
    
    
    /**
     * Returns true if the specified column/row is part of the selection.
     * Conditions must be met:
     * 1) this selection should an GridSelectionGroup
     * 2) GridSelectionGroup.type matches feature
     * 3) GridSelectionGroup.indexes contains featureIndex
     * 
     * @param gridPaneObject fxom object of the gridpane holding the column/row
     * @param feature column/row
     * @param featureIndex index of the column/row to be checked
     * @return  true if this foxm object is selected.
     */
    public boolean isSelected(FXOMInstance gridPaneObject, Type feature, int featureIndex) {
        final boolean result;
        
        assert gridPaneObject != null;
        assert gridPaneObject.getSceneGraphObject() instanceof GridPane;
        
        if (group instanceof GridSelectionGroup) {
            final GridSelectionGroup gsg = (GridSelectionGroup) group;
            result = (gsg.getType() == feature)
                    && (gsg.getIndexes().contains(featureIndex));
        } else {
            result = false;
        }
        
        return result;
    }
    
    /**
     * Replaces the selected items by the one from the specified selection group.
     * 
     * @param newGroup null or the selection group defining items to be selected
     */
    public void select(AbstractSelectionGroup newGroup) {
        
        if (lock) {
            // Method is called from a revision property listener
            throw new IllegalStateException("Changing selection from a selection listener is forbidden");
        }
        
        if (Objects.equals(this.group, newGroup) == false) {
            beginUpdate();
            this.group = newGroup;
            endUpdate();
        }
    }
    
    /**
     * Returns null or the first selected ancestor of the specified fxom object.
     * 
     * @param fxomObject an fxom object
     * @return null or the first selected ancestor of the specified fxom object.
     */
    public FXOMObject lookupSelectedAncestor(FXOMObject fxomObject) {
        assert fxomObject != null;
        
        FXOMObject result = null;
        FXOMObject parent = fxomObject.getParentObject();
        
        while ((parent != null) && (result == null)) {
            if (isSelected(parent)) {
                result = parent;
            }
            parent = parent.getParentObject();
        }
        
        return result;
    }
    
    
    /**
     * Empties this selection.
     * This routine adds +1 to the revision number.
     * 
     */
    public void clear() {
        if (group != null) {
            beginUpdate();
            group = null;
            endUpdate();
        }
    }
    
    /**
     * Returns true if this selection is empty ie its selection group is null.
     * s
     * @return  true if this selection is empty.
     */
    public boolean isEmpty() {
        return getGroup() == null;
    }
    
    /**
     * Returns the group associated to this selection.
     * If this selection is empty, null is returned.
     * 
     * @return  the group containing the selected items or null if selection is empty.
     */
    public AbstractSelectionGroup getGroup() {
        return group;
    }
    
    /**
     * Returns number of nanoseconds taken to execute selection listeners.
     * 
     * @return number of nanoseconds taken to execute selection listeners.
     */
    public long getLastListenerInvocationTime() {
        return lastListenerInvocationTime;
    }
    
    
    /**
     * Begins an update sequence. Subsequent calls to select() and clear()
     * do not trigger any revision incrementation.
     */
    public void beginUpdate() {
        updateDepth++;
    }
    
    /**
     * Ends an update sequence. Revision is incremented.
     */
    public void endUpdate() {
        assert updateDepth >= 1;
        updateDepth--;
        if (updateDepth == 0) {
            incrementRevision();
        }
    }
    
    /**
     * Returns the common ancestor of the selected items or null if selection
     * is empty or root object is selected.
     * 
     * @return 
     */
    public FXOMObject getAncestor() {
        final FXOMObject result;
        
        if (group == null) {
            // Selection is emtpy
            result = null;
        } else {
            result = group.getAncestor();
        }
        
        return result;
    }

    
    /*
     * Private
     */
    
    private void incrementRevision() {
        lock = true;
        final long startTime = System.nanoTime();
        try {
            revision.set(revision.get()+1);
        } finally {
            lock = false;
        }
        lastListenerInvocationTime = System.nanoTime() - startTime;
    }
}
