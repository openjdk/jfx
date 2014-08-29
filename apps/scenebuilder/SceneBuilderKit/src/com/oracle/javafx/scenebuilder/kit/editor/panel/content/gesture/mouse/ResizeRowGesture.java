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
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.gridpane.GridPaneHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer.GridPaneRowResizer;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.RowConstraintsListPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

/**
 *
 */
public class ResizeRowGesture extends AbstractMouseGesture {

    private static final PropertyName rowConstraintsName
            = new PropertyName("rowConstraints"); //NOI18N
    private static final ValuePropertyMetadata rowConstraintsMeta 
            = new RowConstraintsListPropertyMetadata(
                rowConstraintsName,
                true, /* readWrite */
                Collections.emptyList(), /* defaultValue */
                InspectorPath.UNUSED);
    
    private final GridPaneHandles gridPaneHandles;
    private final FXOMInstance fxomInstance;
    private final int rowIndex;
    private final GridPane gridPane;
    private GridPaneRowResizer resizer;


    public ResizeRowGesture(GridPaneHandles gridPaneHandles, int rowIndex) {
        super(gridPaneHandles.getContentPanelController());
        
        assert rowIndex >= 0;
        
        this.gridPaneHandles = gridPaneHandles;
        this.fxomInstance = gridPaneHandles.getFxomInstance(); // Shortcut
        this.gridPane = (GridPane) fxomInstance.getSceneGraphObject(); // Shortcut
        this.rowIndex = rowIndex;
        
        assert this.rowIndex < Deprecation.getGridPaneRowCount(this.gridPane);
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
        
        resizer = new GridPaneRowResizer(gridPane, rowIndex);
        
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
        final Point2D start = gridPane.sceneToLocal(startSceneX, startSceneY);
        final Point2D current = gridPane.sceneToLocal(currentSceneX, currentSceneY);
        final double dy = current.getY() - start.getY();
        
        resizer.updateHeight(dy);
        gridPane.layout();
        gridPaneHandles.layoutDecoration();
    }

    @Override
    protected void mouseDragEnded() {
        assert resizer != null;
        
        /*
         * Three steps
         * 
         * 1) Collects the modified row constraints list
         * 2) Reverts to initial sizing
         *    => this step is equivalent to userDidCancel()
         * 3) Push a BatchModifyObjectJob to officially resize the rows
         */
        
        // Step #1
        final List<RowConstraints> newConstraints 
                = cloneRowConstraintsList(gridPane);

        // Step #2
        userDidCancel();
        
        // Step #3
        final Map<ValuePropertyMetadata, Object> metaValueMap = new HashMap<>();
        metaValueMap.put(rowConstraintsMeta, newConstraints);
        
        final EditorController editorController 
                = contentPanelController.getEditorController();
        final ModifyObjectJob j = new ModifyObjectJob(
                fxomInstance,
                rowConstraintsMeta,
                newConstraints,
                editorController,
                I18N.getString("label.action.edit.resize.row"));
        editorController.getJobManager().push(j);
        
        gridPaneHandles.layoutDecoration();
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
        gridPane.layout();
    }
    
    
    
    /*
     * Private
     */
    
    private List<RowConstraints> cloneRowConstraintsList(GridPane gridPane) {
        final List<RowConstraints> result = new ArrayList<>();
        
        for (RowConstraints rc : gridPane.getRowConstraints()) {
            result.add(cloneRowConstraints(rc));
        }
        
        return result;
    }
    
    
    private RowConstraints cloneRowConstraints(RowConstraints cc) {
        final RowConstraints result = new RowConstraints();
        
        result.setFillHeight(cc.isFillHeight());
        result.setValignment(cc.getValignment());
        result.setVgrow(cc.getVgrow());
        result.setMaxHeight(cc.getMaxHeight());
        result.setMinHeight(cc.getMinHeight());
        result.setPercentHeight(cc.getPercentHeight());
        result.setPrefHeight(cc.getPrefHeight());
        
        return result;
    }
}
