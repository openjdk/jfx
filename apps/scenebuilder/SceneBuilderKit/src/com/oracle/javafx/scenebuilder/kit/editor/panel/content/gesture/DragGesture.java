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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture;

import com.oracle.javafx.scenebuilder.kit.editor.drag.DragController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.ExternalDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.RootDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.AbstractDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.guides.MovingGuideController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.BoundsUtils;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.HashSet;
import java.util.Set;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

/**
 *
 * 
 */
public class DragGesture extends AbstractGesture {
    
    private final double MARGIN = 14.0;
    
    private final DragController dragController;
    private final Set<FXOMObject> pickExcludes = new HashSet<>();
    private DragEvent dragEnteredEvent;
    private DragEvent lastDragEvent;
    private Observer observer;
    private boolean shouldEndOnExitOrDropped;
    private FXOMObject hitParent;
    private DesignHierarchyMask hitParentMask;
    private MovingGuideController movingGuideController;
    private boolean guidesDisabled;
    private Node shadow;

    public DragGesture(ContentPanelController contentPanelController) {
        super(contentPanelController);
        this.dragController = contentPanelController.getEditorController().getDragController();
    }
    
    /*
     * AbstractDragGesture
     */
    
    @Override
    public void start(InputEvent e, Observer observer) {
        assert e != null;
        assert e instanceof DragEvent;
        assert e.getEventType() == DragEvent.DRAG_ENTERED;
        
        final Node glassLayer = contentPanelController.getGlassLayer();
        assert glassLayer.getOnDragOver()== null;
        assert glassLayer.getOnDragExited()== null;
        assert glassLayer.getOnDragDropped()== null;
        assert glassLayer.getOnDragDone()== null;
        assert glassLayer.getOnKeyPressed()== null;
        
        glassLayer.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent e) {
                lastDragEvent = e;
                dragOverGlassLayer();
            }
        });
        glassLayer.setOnDragExited(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent e) {
                lastDragEvent = e;
                dragExitedGlassLayer();
            }
        });
        glassLayer.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent e) {
                lastDragEvent = e;
                dragDroppedOnGlassLayer();
                e.consume();
                // On Linux, "drag over" is randomly called before "drag done".
                // It's unclear whether it's an FX bug or feature.
                // To make things unambiguous, we clear the "drag over" callback.
                // See DTL-5956.
                glassLayer.setOnDragOver(null);
            }
        });
        glassLayer.setOnDragDone(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent e) {
                lastDragEvent = e;
                dragDoneOnGlassLayer();
                e.getDragboard().clear();
                e.consume();
            }
        });
        glassLayer.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                handleKeyPressed(e);
            }
        });
        
        this.dragEnteredEvent = (DragEvent) e;
        this.lastDragEvent = this.dragEnteredEvent;
        this.observer = observer;
        this.shouldEndOnExitOrDropped = false;
        assert this.hitParent == null;
        assert this.hitParentMask == null;
        assert this.shadow == null;
        
        setupMovingGuideController();
        
        dragEnteredGlassLayer();
    }
    
    
    /*
     * Private
     */
    
    private void dragEnteredGlassLayer() {
        if (dragController.getDragSource() == null) { // Drag started externally
            final FXOMDocument fxomDocument
                    = contentPanelController.getEditorController().getFxomDocument();
            final Window ownerWindow
                    = contentPanelController.getPanelRoot().getScene().getWindow();
            final ExternalDragSource dragSource = new ExternalDragSource(
                    lastDragEvent.getDragboard(), fxomDocument, ownerWindow);
            dragController.begin(dragSource);
            shouldEndOnExitOrDropped = true;
        }
        
        // Objects being dragged should be excluded from the pick.
        // We create the exclude list here once.
        pickExcludes.clear();
        pickExcludes.addAll(dragController.getDragSource().getDraggedObjects());
        
        // We show the shadow
        showShadow();
        
        // Now same logic as dragOver
        dragOverGlassLayer();
    }
    
    private void dragOverGlassLayer() {
                
        final double hitX = lastDragEvent.getSceneX();
        final double hitY = lastDragEvent.getSceneY();
        final FXOMObject hitObject =
                contentPanelController.pick(hitX, hitY, pickExcludes);
        final FXOMDocument fxomDocument
                = contentPanelController.getEditorController().getFxomDocument();
        assert fxomDocument != null;
        
        final FXOMInstance candidateParent;
        if (hitObject == null) {
            // Mouse is over workspace background:
            // 1) If document is empty, we keep candidateParent == null
            //    but drop will be enabled later
            // 2) if document is not empty, we check if the root object
            //    accepts parent positionning.
            if (fxomDocument.getFxomRoot() == null) {
                // Case #1: document is empty
                candidateParent = null;
            } else {
                // Case #2: we check for "free child positioning"
                assert fxomDocument.getFxomRoot() != null;
                final DesignHierarchyMask m 
                        = new DesignHierarchyMask(fxomDocument.getFxomRoot());
                if (m.isFreeChildPositioning()) {
                    assert fxomDocument.getFxomRoot() instanceof FXOMInstance;
                    candidateParent = (FXOMInstance) fxomDocument.getFxomRoot();
                } else {
                    candidateParent = null;
                }
            }
        } else {
            final DesignHierarchyMask m = new DesignHierarchyMask(hitObject);
            final boolean isBorderPane = hitObject.getSceneGraphObject() instanceof BorderPane;
            if (m.isAcceptingSubComponent() || isBorderPane) {
                assert hitObject instanceof FXOMInstance;
                candidateParent = (FXOMInstance) hitObject;
            } else {
                assert hitObject.getParentObject() instanceof FXOMInstance;
                candidateParent = (FXOMInstance) hitObject.getParentObject();
            }
        }
        
        if (candidateParent != hitParent) {
            hitParent = candidateParent;
            if (hitParent == null) {
                hitParentMask = null;
                movingGuideController.clearSampleBounds();
            } else {
                hitParentMask = new DesignHierarchyMask(hitParent);
                if (hitParentMask.isFreeChildPositioning()) {
                    populateMovingGuideController();
                } else {
                    movingGuideController.clearSampleBounds();
                }
            }
        }
        
        final double guidedX, guidedY;
        if ((hitParentMask != null) 
                && hitParentMask.isFreeChildPositioning()
                && (guidesDisabled == false)) {
            updateShadow(hitX, hitY);
            final Bounds shadowBounds = shadow.getLayoutBounds();
            final Bounds shadowBoundsInScene = shadow.localToScene(shadowBounds);
            movingGuideController.match(shadowBoundsInScene);
            
            guidedX = hitX + movingGuideController.getSuggestedDX();
            guidedY = hitY + movingGuideController.getSuggestedDY();
        } else {
            guidedX = hitX;
            guidedY = hitY;
        }

        updateShadow(guidedX, guidedY);
        
        final AbstractDropTarget dropTarget;
        if (hitParent == null) {
            if (fxomDocument.getFxomRoot() == null) {
                dropTarget = new RootDropTarget();
            } else {
                dropTarget = null;
            }
        } else {
            final AbstractDriver driver = contentPanelController.lookupDriver(hitParent);
            dropTarget = driver.makeDropTarget(hitParent, guidedX, guidedY);
        }
        
        dragController.setDropTarget(dropTarget);
        lastDragEvent.acceptTransferModes(dragController.getAcceptedTransferModes());
        
    }
    
    private void dragExitedGlassLayer() {
        dragController.setDropTarget(null);
        if (shouldEndOnExitOrDropped) {
            dragController.end();
        }
        if (willReceiveDragDone() == false) {
            performTermination();
        }
    }
    
    private void dragDroppedOnGlassLayer() {
        dragController.commit();
        if (shouldEndOnExitOrDropped) {
            dragController.end();
        }
        if (willReceiveDragDone() == false) {
            performTermination();
        }
    }
    
    private void dragDoneOnGlassLayer() {
        assert shouldEndOnExitOrDropped == false;
        dragController.end();
        performTermination();
    }
    
    private void handleKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) {
            dragController.setDropTarget(null);
            performTermination();
        } else if (e.getCode() == KeyCode.ALT) {
            final EventType<KeyEvent> eventType = e.getEventType();
            if (eventType == KeyEvent.KEY_PRESSED) {
                guidesDisabled = true;
            } else if (eventType == KeyEvent.KEY_RELEASED) {
                guidesDisabled = false;
            }
            dragOverGlassLayer();
        }
    }
    
    
    private boolean willReceiveDragDone() {
        /*
         * DragGesture.dragDoneOnGlassLayer() will be invoked only if 
         * glass layer is the gesture source ie this gesture has been
         * initiated by SelectAndMoveGesture.mouseDragDetected.
         * 
         * If drag done will be called, then this gesture must perform itss 
         * termination sequence only when drag done is called.
         * If drag done will not be called, then this gesture must perform its
         * termination sequence when drag exits or drops.
         * 
         */
        return dragEnteredEvent.getGestureSource() 
                == contentPanelController.getGlassLayer();
    }
    
    
    private void performTermination() {
        final Node glassLayer = contentPanelController.getGlassLayer();
        glassLayer.setOnDragOver(null);
        glassLayer.setOnDragExited(null);
        glassLayer.setOnDragDropped(null);
        glassLayer.setOnDragDone(null);
        glassLayer.setOnKeyPressed(null);
        
        hideShadow();
        dismantleMovingGuideController();
        
        observer.gestureDidTerminate(this);
        observer = null;
        
        dragEnteredEvent = null;
        lastDragEvent = null;
        shouldEndOnExitOrDropped = false;
        hitParent = null;
        hitParentMask = null;
        assert shadow == null; // Because we called hideShadow()
    }
    
    /*
     * Shadow
     */
    
    private void showShadow() {
        assert shadow == null;
        
        shadow = dragController.getDragSource().makeShadow();
        shadow.setMouseTransparent(true);
        contentPanelController.getRudderLayer().getChildren().add(shadow);
        
        updateShadow(0.0, 0.0);
    }
    
    private void updateShadow(double hitX, double hitY) {
        assert shadow != null;
        
        final Group rudderLayer = contentPanelController.getRudderLayer();
        final Point2D p = rudderLayer.sceneToLocal(hitX, hitY);
        shadow.setLayoutX(p.getX());
        shadow.setLayoutY(p.getY());
    }
    
    private void hideShadow() {
        assert shadow != null;
        contentPanelController.getRudderLayer().getChildren().remove(shadow);
        shadow = null;
    }
    
    /*
     * MovingGuideController
     */
    
    private void setupMovingGuideController() {
        final Bounds scope = contentPanelController.getWorkspacePane().getLayoutBounds();
        final Bounds scopeInScene = contentPanelController.getWorkspacePane().localToScene(scope);
        this.movingGuideController = new MovingGuideController(
                contentPanelController.getGuidesColor(), scopeInScene);
        final Group rudderLayer = contentPanelController.getRudderLayer();
        final Group guideGroup = movingGuideController.getGuideGroup();
        assert guideGroup.isMouseTransparent();
        rudderLayer.getChildren().add(guideGroup);
    }
    
    
    private void populateMovingGuideController() {
        assert hitParentMask != null;
        assert hitParentMask.isFreeChildPositioning(); // (1)
        
        movingGuideController.clearSampleBounds();
        
        // Adds N, S, E, W and center lines for each child of the hitParent
        for (int i = 0, c = hitParentMask.getSubComponentCount(); i < c; i++) {
            final FXOMObject child = hitParentMask.getSubComponentAtIndex(i);
            final boolean isNode = child.getSceneGraphObject() instanceof Node;
            if ((pickExcludes.contains(child) == false) && isNode) {
                final Node childNode = (Node) child.getSceneGraphObject();
                movingGuideController.addSampleBounds(childNode);
            }
        }
        
        // Adds N, S, E, W and center lines of the hitParent itself
        assert hitParent.getSceneGraphObject() instanceof Node; // Because (1)
        final Node hitParentNode = (Node) hitParent.getSceneGraphObject();
        movingGuideController.addSampleBounds(hitParentNode);
        
        // If bounds of hitParent are larger enough then adds the margin boundaries
        final Bounds hitParentBounds = hitParentNode.getLayoutBounds();
        final Bounds insetBounds = BoundsUtils.inset(hitParentBounds, MARGIN, MARGIN);
        if (insetBounds.isEmpty() == false) {
            final Bounds insetBoundsInScene = hitParentNode.localToScene(insetBounds);
            movingGuideController.addSampleBounds(insetBoundsInScene, false /* addMiddle */);
        }
    }    
    
    
    private void dismantleMovingGuideController() {
        assert movingGuideController != null;
        final Group guideGroup = movingGuideController.getGuideGroup();
        final Group rudderLayer = contentPanelController.getRudderLayer();
        assert rudderLayer.getChildren().contains(guideGroup);
        rudderLayer.getChildren().remove(guideGroup);
        movingGuideController = null;
    }
}
