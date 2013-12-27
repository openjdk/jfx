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

import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import javafx.scene.input.MouseEvent;

/**
 *
 * 
 */
public class SelectAndMoveInGridGesture extends AbstractMouseDragGesture {

    private final FXOMInstance gridPaneInstance;
    private final GridSelectionGroup.Type feature;
    private final int featureIndex;
    
    
    public SelectAndMoveInGridGesture(ContentPanelController contentPanelController,
            FXOMInstance gridPaneInstance, GridSelectionGroup.Type feature, 
            int featureIndex) {
        super(contentPanelController);
        
        this.gridPaneInstance = gridPaneInstance;
        this.feature = feature;
        this.featureIndex = featureIndex;
    }

    public FXOMInstance getGridPaneInstance() {
        return gridPaneInstance;
    }

    public GridSelectionGroup.Type getFeature() {
        return feature;
    }

    public int getFeatureIndex() {
        return featureIndex;
    }

    
    
    /*
     * AbstractMouseDragGesture
     */

    @Override
    protected void mousePressed(MouseEvent e) {
        
        /*
         *             |        Object      |                      GridSelectionGroup                      |
         *             |      Selection     |                                                              |
         *             |        Group       +-----------------------------------------+--------------------+
         *             |                    |               feature type              |    feature type    |
         *             |                    |                  matches                |   does not match   |
         *             |                    +--------------------+--------------------+                    |
         *             |                    |       feature      |       feature      |                    |
         *             |                    |  not yet selected  |  already selected  |                    |
         * ------------+--------------------+--------------------+--------------------+--------------------+ 
         *             |                    |                    |                    |                    |
         *             |   select feature   |   select feature   |                    |   select feature   |
         *   shift up  | start drag gesture | start drag gesture | start drag gesture | start drag gesture |
         *             |                    |                    |                    |                    |
         *             |         (A)        |       (B.1.1)      |       (B.1.2)      |       (B.2)        |
         * ------------+--------------------+--------------------+--------------------+--------------------+
         *             |                    |                    |                    |                    |
         *             |   select feature   |     add feature    |   remove feature   |   select feature   |
         *  shift down | start drag gesture |    to selection    |   from selection   | start drag gesture |
         *             |                    | start drag gesture |ignore drag gesture |                    |
         *             |         (C)        |       (D.1.1)      |       (D.1.2)      |       (D.2)        |
         * ------------+--------------------+--------------------+--------------------+--------------------+
         */
        
        final Selection selection 
                = contentPanelController.getEditorController().getSelection();
        final boolean extendKeyDown
                = EditorPlatform.isContinuousSelectKeyDown(e) 
                || EditorPlatform.isNonContinousSelectKeyDown(e);
        
        if (selection.getGroup() instanceof GridSelectionGroup) {
            if (extendKeyDown) { // Case D.1.* and D.2
                selection.toggleSelection(gridPaneInstance, feature, featureIndex);
            } else { // Cases B.1.*, B.2 or B.2
                selection.select(gridPaneInstance, feature, featureIndex);
            }
        } else { // Cases A and B
            assert selection.getGroup() instanceof ObjectSelectionGroup;
            selection.select(gridPaneInstance, feature, featureIndex);
        }
    }

    @Override
    protected void mouseDragDetected(MouseEvent e) {
        final Selection selection 
                = contentPanelController.getEditorController().getSelection();
        
        /*
         *             |        Object      |                      GridSelectionGroup                      |
         *             |      Selection     |                                                              |
         *             |        Group       +-----------------------------------------+--------------------+
         *             |                    |               feature type              |    feature type    |
         *             |                    |                  matches                |   does not match   |
         *             |                    +--------------------+--------------------+                    |
         *             |                    |       feature      |       feature      |                    |
         *             |                    |  not yet selected  |  already selected  |                    |
         * ------------+--------------------+--------------------+--------------------+--------------------+ 
         *             |                    |                    |                    |                    |
         *             |   select feature   |   select feature   |                    |   select feature   |
         *   shift up  | start drag gesture | start drag gesture | start drag gesture | start drag gesture |
         *             |                    |                    |                    |                    |
         *             |         (A)        |       (B.1.1)      |       (B.1.2)      |       (B.2)        |
         * ------------+--------------------+--------------------+--------------------+--------------------+
         *             |                    |                    |                    |                    |
         *             |   select feature   |     add feature    |   remove feature   |   select feature   |
         *  shift down | start drag gesture |    to selection    |   from selection   | start drag gesture |
         *             |                    | start drag gesture |ignore drag gesture |                    |
         *             |         (C)        |       (D.1.1)      |       (D.1.2)      |       (D.2)        |
         * ------------+--------------------+--------------------+--------------------+--------------------+
         */
        

        if (selection.isSelected(gridPaneInstance, feature, featureIndex)) {
            // Case A, B.*.*, C, D.1.1 and D.2
            
//            final EditorController editorController
//                    = contentPanelController.getEditorController();
//            final Window ownerWindow
//                    = contentPanelController.getPanelRoot().getScene().getWindow();
//            final GridDragSource dragSource = new GridDragSource();
//
//            final Node glassLayer = contentPanelController.getGlassLayer();
//            final Dragboard db = glassLayer.startDragAndDrop(TransferMode.COPY_OR_MOVE);
//            db.setContent(dragSource.makeClipboardContent());
//            db.setDragView(dragSource.makeDragView());
//
//            assert editorController.getDragController().getDragSource() == null;
//            editorController.getDragController().begin(dragSource);
            System.out.println("SelectAndMoveInGridGesture.mouseDragDetected: will start column/row drag...");
        } 
        // else Case D.1.2 : drag gesture is ignored
        
    }

    @Override
    protected void mouseReleased(MouseEvent e) {
        // Nothing to do
    }

    @Override
    protected void mouseExited(MouseEvent e) {
        // Should be not called because mouse should exit glass layer
        // during this gesture
//        assert false;
    }
    
}
