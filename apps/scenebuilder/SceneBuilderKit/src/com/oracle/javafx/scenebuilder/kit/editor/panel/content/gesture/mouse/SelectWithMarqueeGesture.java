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

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.AbstractDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.AbstractPring;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.KeyEvent;
import javafx.scene.shape.Rectangle;

/**
 *
 * 
 */
public class SelectWithMarqueeGesture extends AbstractMouseGesture {
    
    private FXOMObject hitObject;
    private FXOMObject scopeObject;
    private AbstractPring<?> scopeHilit;
    private final Set<FXOMObject> candidates = new HashSet<>();
    private final Rectangle marqueeRect = new Rectangle();
    
    public SelectWithMarqueeGesture(ContentPanelController contentPanelController) {
        super(contentPanelController);
    }

    public void setup(FXOMObject hitObject, FXOMObject scopeObject) {
        assert (hitObject == null) || (hitObject.isDescendantOf(scopeObject) == false);
        this.hitObject = hitObject;
        this.scopeObject = scopeObject;
        marqueeRect.getStyleClass().add("marquee");
    }

    public FXOMObject getHitObject() {
        return hitObject;
    }
    
    /*
     * AbstractMouseGesture
     */

    @Override
    protected void mousePressed() {
    }

    @Override
    protected void mouseDragStarted() {
        contentPanelController.getEditorController().getSelection().clear();
        collectCandidates();
        showScopeHilit();
        showMarqueeRect();
    }

    @Override
    protected void mouseDragged() {
        updateMarqueeRect();
        updateSelection();
    }

    @Override
    protected void mouseDragEnded() {
        candidates.clear();
        hideScopeHilit();
        hideMarqueeRect();
    }

    @Override
    protected void mouseReleased() {
        // Mouse has not been dragged
        // If an object is below the mouse, then we select it.
        // Else we unselect all.
        if (isMouseDidDrag() == false) {
            final Selection selection 
                    = contentPanelController.getEditorController().getSelection();
            if (hitObject != null) {
                selection.select(hitObject);
            } else {
                selection.clear();
            }
        }
    }

    @Override
    protected void keyEvent(KeyEvent e) {
    }

    @Override
    protected void userDidCancel() {
    }
    
    
    /*
     * Private
     */
    
    private void showScopeHilit() {
        if (scopeObject != null) {
            final AbstractDriver driver
                    = contentPanelController.lookupDriver(scopeObject);
            final Group rudderLayer 
                    = contentPanelController.getRudderLayer();
            assert driver != null;
            scopeHilit = driver.makePring(scopeObject);
            scopeHilit.changeStroke(contentPanelController.getPringColor());
            rudderLayer.getChildren().add(scopeHilit.getRootNode());
        }
    }
    
    
    private void hideScopeHilit() {
        if (scopeHilit != null) {
            final Group rudderLayer = contentPanelController.getRudderLayer();
            assert rudderLayer.getChildren().contains(scopeHilit.getRootNode());
            rudderLayer.getChildren().remove(scopeHilit.getRootNode());
            scopeHilit = null;
        }
    }
    
    private void showMarqueeRect() {
        final Group rudderLayer = contentPanelController.getRudderLayer();
        rudderLayer.getChildren().add(marqueeRect);
        updateMarqueeRect();
    }
    
    private void updateMarqueeRect() {
        final double xPressed = getMousePressedEvent().getSceneX();
        final double yPressed = getMousePressedEvent().getSceneY();
        final double xCurrent = getLastMouseEvent().getSceneX();
        final double yCurrent = getLastMouseEvent().getSceneY();
        
        final double xMin = Math.min(xPressed, xCurrent);
        final double yMin = Math.min(yPressed, yCurrent);
        final double xMax = Math.max(xPressed, xCurrent);
        final double yMax = Math.max(yPressed, yCurrent);
        
        final Group rudderLayer = contentPanelController.getRudderLayer();
        final Point2D p0 = rudderLayer.sceneToLocal(xMin, yMin, true /* rootScene */);
        final Point2D p1 = rudderLayer.sceneToLocal(xMax, yMax, true /* rootScene */);
        
        marqueeRect.setX(p0.getX());
        marqueeRect.setY(p0.getY());
        marqueeRect.setWidth(p1.getX() - p0.getX());
        marqueeRect.setHeight(p1.getY() - p0.getY());
    }
    
    private void hideMarqueeRect() {
        final Group rudderLayer = contentPanelController.getRudderLayer();
        rudderLayer.getChildren().remove(marqueeRect);
    }
    
    
    private void updateSelection() {
        final double xPressed = getMousePressedEvent().getSceneX();
        final double yPressed = getMousePressedEvent().getSceneY();
        final double xCurrent = getLastMouseEvent().getSceneX();
        final double yCurrent = getLastMouseEvent().getSceneY();
        
        final double xMin = Math.min(xPressed, xCurrent);
        final double yMin = Math.min(yPressed, yCurrent);
        final double xMax = Math.max(xPressed, xCurrent);
        final double yMax = Math.max(yPressed, yCurrent);
        final BoundingBox marqueeBounds 
                = new BoundingBox(xMin, yMin, xMax - xMin, yMax - yMin);
        
        final Set<FXOMObject> winners = new HashSet<>();
        for (FXOMObject candidate : candidates) {
            final AbstractDriver driver
                    = contentPanelController.lookupDriver(candidate);
            if ((driver != null) && driver.intersectsBounds(candidate, marqueeBounds)) {
                winners.add(candidate);
            }
        }
        
        final Selection selection
                = contentPanelController.getEditorController().getSelection();
        selection.select(winners);
    }
    
    
    private void collectCandidates() {
        if (scopeObject == null) {
            // Only one candidate : the root object
            final FXOMDocument fxomDocument
                    = contentPanelController.getEditorController().getFxomDocument();
            if ((fxomDocument != null) && (fxomDocument.getFxomRoot() != null)) {
                candidates.add(fxomDocument.getFxomRoot());
            }
        } else {
            final DesignHierarchyMask m
                    = new DesignHierarchyMask(scopeObject);
            if (m.isAcceptingSubComponent()) {
                final int count = m.getSubComponentCount();
                for (int i = 0; i < count; i++) {
                    candidates.add(m.getSubComponentAtIndex(i));
                }
            } else {
                final List<Accessory> accessories = Arrays.asList(
                        Accessory.CONTENT,
                        Accessory.CENTER,
                        Accessory.BOTTOM, Accessory.TOP,
                        Accessory.LEFT, Accessory.RIGHT,
                        Accessory.XAXIS, Accessory.YAXIS);
                for (Accessory accessory : accessories) {
                    if (m.isAcceptingAccessory(accessory)) {
                        final FXOMObject fxomObject = m.getAccessory(accessory);
                        if (fxomObject != null) {
                            candidates.add(fxomObject);
                        }
                    }
                }
            }
        }
    }
}
