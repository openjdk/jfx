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

package com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer.TreeTableColumnResizer;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Point2D;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyEvent;

/**
 *
 */
public class ResizeTreeTableColumnGesture extends AbstractMouseGesture {

    private final FXOMInstance columnInstance;
    private TreeTableColumnResizer resizer;


    public ResizeTreeTableColumnGesture(ContentPanelController contentPanelController,
            FXOMInstance fxomInstance) {
        super(contentPanelController);
        
        assert fxomInstance != null;
        assert fxomInstance.getSceneGraphObject() instanceof TreeTableColumn;
        
        this.columnInstance = fxomInstance;
    }

    /*
     * AbstractMouseGesture
     */
    
    @Override
    protected void mousePressed() {
        // Everthing is done in mouseDragStarted
    }

    @Override
    protected void mouseDragStarted() {
        assert resizer == null;
        assert columnInstance.getSceneGraphObject() instanceof TreeTableColumn;
        
        resizer = new TreeTableColumnResizer((TreeTableColumn<?,?>)columnInstance.getSceneGraphObject());
        
        // Now same as mouseDragged
        mouseDragged();
    }

    @Override
    protected void mouseDragged() {
        assert resizer != null;
        
        final double startSceneX = getMousePressedEvent().getSceneX();
        final double startSceneY = getMousePressedEvent().getSceneY();
        final double currentSceneX = getLastMouseEvent().getSceneX();
        final double currentSceneY = getLastMouseEvent().getSceneY();
        final TreeTableView<?> treeTableView = resizer.getTreeTableColumn().getTreeTableView();
        final Point2D start = treeTableView.sceneToLocal(startSceneX, startSceneY);
        final Point2D current = treeTableView.sceneToLocal(currentSceneX, currentSceneY);
        final double dx = current.getX() - start.getX();
        
        resizer.updateWidth(dx);
        treeTableView.layout();
    }

    @Override
    protected void mouseDragEnded() {
        assert resizer != null;
        
        /*
         * Three steps
         * 
         * 1) Collects sizing properties that have changed
         * 2) Reverts to initial sizing
         *    => this step is equivalent to userDidCancel()
         * 3) Push a BatchModifyObjectJob to officially resize the columns
         */
        
        // Step #1
        final Map<PropertyName, Object> changeMap = resizer.getChangeMap();
        final Map<PropertyName, Object> changeMapNext = resizer.getChangeMapNext();
        

        // Step #2
        userDidCancel();
        
        // Step #3
        final EditorController editorController 
                = contentPanelController.getEditorController();
        final BatchJob batchJob
                = new BatchJob(editorController, true,
                I18N.getString("label.action.edit.resize.column"));
        if (changeMap.isEmpty() == false) {
            batchJob.addSubJobs(makeResizeJob(columnInstance, changeMap));
        }
        if (changeMapNext.isEmpty() == false) {
            batchJob.addSubJobs(makeResizeJob(columnInstance.getNextSlibing(), changeMapNext));
        }
        if (batchJob.isExecutable()) {
            editorController.getJobManager().push(batchJob);
        }
        
        resizer = null; // For sake of symetry...
    }

    @Override
    protected void mouseReleased() {
        // Everything is done in mouseDragEnded
    }

    @Override
    protected void keyEvent(KeyEvent e) {
        // Nothing special here
    }

    @Override
    protected void userDidCancel() {
        resizer.revertToOriginalSize();
        resizer.getTreeTableColumn().getTreeTableView().layout();
    }
    
    
    /*
     * Private
     */
    
    private List<Job> makeResizeJob(FXOMObject columnObject, Map<PropertyName, Object> changeMap) {
        assert columnObject.getSceneGraphObject() instanceof TreeTableColumn;
        assert columnObject instanceof FXOMInstance;
        
        final List<Job> result = new ArrayList<>();
        
        final Metadata metadata = Metadata.getMetadata();
        final Map<ValuePropertyMetadata, Object> metaValueMap = new HashMap<>();
        for (Map.Entry<PropertyName,Object> e : changeMap.entrySet()) {
            final ValuePropertyMetadata vpm = metadata.queryValueProperty(columnInstance, e.getKey());
            assert vpm != null;
            metaValueMap.put(vpm, e.getValue());
        }

        for (Map.Entry<ValuePropertyMetadata, Object> e : metaValueMap.entrySet()) {
            final ModifyObjectJob job = new ModifyObjectJob(
                    (FXOMInstance) columnObject,
                    e.getKey(),
                    e.getValue(),
                    contentPanelController.getEditorController());
            result.add(job);
        }        
        return result;
    }
}
