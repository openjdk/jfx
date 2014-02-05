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

package com.oracle.javafx.scenebuilder.kit.editor.job;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.job.v2.CompositeJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Point2D;

/**
 *
 */
public class RelocateSelectionJob extends CompositeJob {
    
    private static final long MERGE_PERIOD = 1000; //  milliseconds
    
    private final Map<FXOMObject, Point2D> locationMap = new HashMap<>();
    private long time = System.currentTimeMillis();

    public RelocateSelectionJob(Map<FXOMObject, Point2D> locationMap,
            EditorController editorController) {
        super(editorController);
        this.locationMap.putAll(locationMap);
    }
    
    public boolean canBeMergedWith(Job other) {
        
        /*
         * This job is collapsible with other if:
         *      0) other is a RelocateSelectionJob instance
         *      1) other is younger than this of 1000 ms no more
         *      2) other and this have the same location map keys
         */
        
        final boolean result;
        if (other instanceof RelocateSelectionJob) {
            final RelocateSelectionJob otherRelocate = (RelocateSelectionJob)other;
            final long timeDifference = otherRelocate.time - this.time;
            if ((0 <= timeDifference) && (timeDifference < MERGE_PERIOD)) {
                final Set<FXOMObject> thisKeys = this.locationMap.keySet();
                final Set<FXOMObject> otherKeys = otherRelocate.locationMap.keySet();
                result = thisKeys.equals(otherKeys);
            } else {
                result = false;
            }
        } else {
            result = false;
        }
        
        return result;
    }
    
    
    public void mergeWith(Job younger) {
        assert canBeMergedWith(younger); // (1)
        assert younger instanceof RelocateSelectionJob; // Because (1)
        
        final RelocateSelectionJob youngerSelection = (RelocateSelectionJob) younger;
        for (Job subJob : getSubJobs()) {
            assert subJob instanceof RelocateNodeJob;
            final RelocateNodeJob thisRelocateJob 
                    = (RelocateNodeJob) subJob;
            final RelocateNodeJob youngerRelocateJob 
                    = youngerSelection.lookupSubJob(thisRelocateJob.getFxomInstance());
            thisRelocateJob.mergeWith(youngerRelocateJob);
        }
        
        this.time = youngerSelection.time;
    }
    
    
    public RelocateNodeJob lookupSubJob(FXOMObject fxomObject) {
        RelocateNodeJob result = null;
        
        for (Job subJob : getSubJobs()) {
            assert subJob instanceof RelocateNodeJob;
            final RelocateNodeJob relocateJob = (RelocateNodeJob) subJob;
            if (relocateJob.getFxomInstance() == fxomObject) {
                result = relocateJob;
                break;
            }
        }
        
        return result;
    }
    
    public static boolean isSelectionMovable(EditorController editorController) {
        /*
         * Selection can be moved if:
         * 1) it's an object selection (group instanceof ObjectSelectionGroup)
         * 2) selected objects have a single parent
         * 3) single parent supports free child positioning
         * 
         * => all selected items are Node.
         */
        
        final boolean result;
        
        final Selection selection = editorController.getSelection();
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
            if (osg.hasSingleParent()) {
                final FXOMObject parent = osg.getAncestor();
                final DesignHierarchyMask m = new DesignHierarchyMask(parent);
                result = m.isFreeChildPositioning();
            } else {
                result = false;
            }
        } else {
            result = false;
        }
        
        return result;
    }
    
    /*
     * CompositeJob
     */
    
    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result = new ArrayList<>();
        
        for (Map.Entry<FXOMObject, Point2D> entry : locationMap.entrySet()) {
            assert entry.getKey() instanceof FXOMInstance;
            final FXOMInstance fxomInstance = (FXOMInstance) entry.getKey();
            final Point2D layoutXY = entry.getValue();
            final Job relocateJob = new RelocateNodeJob(fxomInstance,
                layoutXY.getX(), layoutXY.getY(), getEditorController());
            result.add(relocateJob);
        }
        
        return result;
    }

    @Override
    protected String makeDescription() {
        final String result;
        
        final Set<FXOMObject> movedObjects = locationMap.keySet();
        if (locationMap.size() == 1) {
            final FXOMObject movedObject = movedObjects.iterator().next();
            final Object sceneGraphObject = movedObject.getSceneGraphObject();
            if (sceneGraphObject == null) {
                result = I18N.getString("drop.job.move.single.unresolved");
            } else {
                result = I18N.getString("drop.job.move.single.resolved",
                        sceneGraphObject.getClass().getSimpleName());
            }
        } else {
            final Set<Class<?>> classes = new HashSet<>();
            int unresolvedCount = 0;
            for (FXOMObject o : movedObjects) {
                if (o.getSceneGraphObject() != null) {
                    classes.add(o.getSceneGraphObject().getClass());
                } else {
                    unresolvedCount++;
                }
            }
            final boolean homogeneous = (classes.size() == 1) && (unresolvedCount == 0);
            
            if (homogeneous) {
                final Class<?> singleClass = classes.iterator().next();
                result = I18N.getString("drop.job.move.multiple.homogeneous",
                        movedObjects.size(),
                        singleClass.getSimpleName());
            } else {
                result = I18N.getString("drop.job.move.multiple.heterogeneous",
                        movedObjects.size());
            }
        }
        
        return result;
    }
    
}
