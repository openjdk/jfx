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
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.HudWindowController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.SplitPaneDesignInfoX;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.CardinalPoint;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyEvent;

/**
 *
 * 
 */
public class AdjustDividerGesture extends AbstractMouseGesture {

    private final FXOMInstance splitPaneInstance;
    private final int dividerIndex;
    private final SplitPaneDesignInfoX di = new SplitPaneDesignInfoX();
    private double[] originalDividerPositions;
    
    private static final PropertyName dividerPositionsName 
            = new PropertyName("dividerPositions"); //NOI18N

    public AdjustDividerGesture(ContentPanelController contentPanelController,
            FXOMInstance splitPaneInstance, int dividerIndex) {
        super(contentPanelController);
        
        assert splitPaneInstance.getSceneGraphObject() instanceof SplitPane;
        this.splitPaneInstance = splitPaneInstance;
        this.dividerIndex = dividerIndex;
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
        originalDividerPositions = getSplitPane().getDividerPositions();
        setupAndOpenHudWindow();
        contentPanelController.getHandleLayer().setVisible(false);
        // Now same as mouseDragged
        mouseDragged();
    }

    @Override
    protected void mouseDragged() {
        final SplitPane splitPane = (SplitPane)splitPaneInstance.getSceneGraphObject();
        final double sceneX = getLastMouseEvent().getSceneX();
        final double sceneY = getLastMouseEvent().getSceneY();
        final double[] newDividerPositions 
                = di.simulateDividerMove(splitPane, dividerIndex, sceneX, sceneY);
        splitPane.setDividerPositions(newDividerPositions);
        splitPane.layout();
        updateHudWindow();
        contentPanelController.getHudWindowController().updatePopupLocation();
    }

    @Override
    protected void mouseDragEnded() {
        /*
         * Three steps
         * 
         * 1) Copy the updated divider positions
         * 2) Reverts to initial divider positions
         *    => this step is equivalent to userDidCancel()
         * 3) Push a BatchModifyObjectJob to officially update dividers
         */
        
        // Step #1
        final List<Double> newDividerPositions = new ArrayList<>();
        for (double p : getSplitPane().getDividerPositions()) {
            newDividerPositions.add(Double.valueOf(p));
        }
        
        // Step #2
        userDidCancel();
        
        // Step #3
        final Metadata metadata = Metadata.getMetadata();
        final EditorController editorController 
                = contentPanelController.getEditorController();
        final ValuePropertyMetadata dividerPositionsMeta 
                = metadata.queryValueProperty(splitPaneInstance, dividerPositionsName);
        final ModifyObjectJob j = new ModifyObjectJob(
                splitPaneInstance, 
                dividerPositionsMeta,
                newDividerPositions,
                editorController);
        if (j.isExecutable()) {
            editorController.getJobManager().push(j);
        } // else divider has been release to its original position
    }

    @Override
    protected void mouseReleased() {
        // Everything is done in mouseDragEnded
    }

    @Override
    protected void keyEvent(KeyEvent e) {
    }

    @Override
    protected void userDidCancel() {
        getSplitPane().setDividerPositions(originalDividerPositions);
        contentPanelController.getHudWindowController().closeWindow();
        contentPanelController.getHandleLayer().setVisible(true);
        getSplitPane().layout();
    }
    
    
    /*
     * Private
     */
    
    private SplitPane getSplitPane() {
        assert splitPaneInstance.getSceneGraphObject() instanceof SplitPane;
        return (SplitPane) splitPaneInstance.getSceneGraphObject();
    }
    
    private void setupAndOpenHudWindow() {
        final HudWindowController hudWindowController
                = contentPanelController.getHudWindowController();
        
        hudWindowController.setRowCount(1);
        hudWindowController.setNameAtRowIndex("dividerPosition", 0); //NOI18N
        updateHudWindow();
        
        final CardinalPoint cp;
        switch(getSplitPane().getOrientation()) {
            default:
            case HORIZONTAL:
                cp = CardinalPoint.S;
                break;
            case VERTICAL:
                cp = CardinalPoint.E;
                break;
        }
        hudWindowController.setRelativePosition(cp);
        hudWindowController.openWindow(getSplitPane());
    }
    
    private void updateHudWindow() {
        final HudWindowController hudWindowController
                = contentPanelController.getHudWindowController();
        
        double dividerPosition = getSplitPane().getDividerPositions()[dividerIndex];
        String str = String.format("%.2f %%", dividerPosition * 100); //NOI18N
        hudWindowController.setValueAtRowIndex(str, 0);
    }
}
