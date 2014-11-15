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
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.ExternalDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AccessoryDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.ContainerXYDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.ImageViewDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.RootDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.AbstractDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.BorderPaneDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.guides.MovingGuideController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.BoundsUtils;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
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
    
    private static final Logger LOG = Logger.getLogger(DragGesture.class.getName());

    private final double MARGIN = 14.0;
    
    private final DragController dragController;
    private final Set<FXOMObject> pickExcludes = new HashSet<>();
    private DragEvent dragEnteredEvent;
    private DragEvent lastDragEvent;
    private Observer observer;
    private boolean willReceiveDragDone;
    private boolean shouldInvokeEnd;
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
        assert glassLayer.getOnDragEntered()== null;
        assert glassLayer.getOnDragOver()== null;
        assert glassLayer.getOnDragExited()== null;
        assert glassLayer.getOnDragDropped()== null;
        assert glassLayer.getOnDragDone()== null;
        assert glassLayer.getOnKeyPressed()== null;
        
        glassLayer.setOnDragEntered(e1 -> {
            lastDragEvent = e1;
            dragEnteredGlassLayer();
        });
        glassLayer.setOnDragOver(e1 -> {
            lastDragEvent = e1;
            dragOverGlassLayer();
        });
        glassLayer.setOnDragExited(e1 -> {
            lastDragEvent = e1;
            dragExitedGlassLayer();
        });
        glassLayer.setOnDragDropped(e1 -> {
            lastDragEvent = e1;
            dragDroppedOnGlassLayer();
            e1.consume();
            // On Linux, "drag over" is randomly called before "drag done".
            // It's unclear whether it's an FX bug or feature.
            // To make things unambiguous, we clear the "drag over" callback.
            // See DTL-5956.
            glassLayer.setOnDragOver(null);
        });
        glassLayer.setOnDragDone(e1 -> {
            lastDragEvent = e1;
            dragDoneOnGlassLayer();
            e1.getDragboard().clear();
            e1.consume();
        });
        glassLayer.setOnKeyPressed(e1 -> handleKeyPressed(e1));
        
        this.dragEnteredEvent = (DragEvent) e;
        this.lastDragEvent = this.dragEnteredEvent;
        this.observer = observer;
        this.willReceiveDragDone = this.dragEnteredEvent.getGestureSource() == glassLayer;
        this.shouldInvokeEnd = willReceiveDragDone;
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
            assert dragSource.isAcceptable();
            dragController.begin(dragSource);
            shouldInvokeEnd = true;
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
        /*
         * On Linux, Node.onDragOver() is sometimes called *after*
         * Node.onDragDropped() : see RT-34537.
         * We detect those illegal invocations here and ignore them.
         */
        
        if (lastDragEvent.isDropCompleted()) {
            LOG.log(Level.WARNING, "Ignored dragOver() after dragDropped()"); //NOI18N
        } else {
            dragOverGlassLayerBis();
        }
    }
    
    private void dragOverGlassLayerBis() {
        
        // Let's set what is below the mouse
        final double hitX = lastDragEvent.getSceneX();
        final double hitY = lastDragEvent.getSceneY();
        FXOMObject hitObject = contentPanelController.pick(hitX, hitY, pickExcludes);
        if (hitObject == null) {
            final FXOMDocument fxomDocument
                = contentPanelController.getEditorController().getFxomDocument();
            hitObject = fxomDocument.getFxomRoot();
        }
        
        if (hitObject == null) {
            // FXOM document is empty
            dragOverEmptyDocument();
        } else {
            dragOverHitObject(hitObject);
        }
    }
    
    private void dragOverEmptyDocument() {
        dragController.setDropTarget(new RootDropTarget());
        lastDragEvent.acceptTransferModes(dragController.getAcceptedTransferModes());
        updateShadow(lastDragEvent.getSceneX(), lastDragEvent.getSceneY());
    }
    
    private void dragOverHitObject(FXOMObject hitObject) {
        assert hitObject != null;
                
        final FXOMDocument fxomDocument
                = contentPanelController.getEditorController().getFxomDocument();
        final AbstractDragSource dragSource
                = dragController.getDragSource();
        final DesignHierarchyMask m 
                = new DesignHierarchyMask(hitObject);
        final double hitX 
                = lastDragEvent.getSceneX();
        final double hitY 
                = lastDragEvent.getSceneY();
        
        assert fxomDocument != null;
        assert dragSource != null;

        AbstractDropTarget dropTarget = null;
        FXOMObject newHitParent = null;
        DesignHierarchyMask newHitParentMask = null;
        
        // dragSource is a single ImageView ?
        final boolean hitImageView = hitObject.getSceneGraphObject() instanceof ImageView;
        final boolean externalDragSource = dragSource instanceof ExternalDragSource;

        if (dragSource.isSingleImageViewOnly() && hitImageView && externalDragSource) {
            dropTarget = new ImageViewDropTarget(hitObject);
            newHitParent = hitObject;
            newHitParentMask = m;
        }
        
        // dragSource is a single Tooltip ?
        if (dropTarget == null) {
            if (dragSource.isSingleTooltipOnly()) {
                assert hitObject instanceof FXOMInstance;
                dropTarget = new AccessoryDropTarget((FXOMInstance)hitObject, Accessory.TOOLTIP);
                newHitParent = hitObject;
                newHitParentMask = m;
            }
        }
        
        // dragSource is a single ContextMenu ?
        if (dropTarget == null) {
            if (dragSource.isSingleContextMenuOnly()) {
                assert hitObject instanceof FXOMInstance;
                dropTarget = new AccessoryDropTarget((FXOMInstance)hitObject, Accessory.CONTEXT_MENU);
                newHitParent = hitObject;
                newHitParentMask = m;
            }
        }
        
        // hitObject is BorderPane ?
        if (dropTarget == null) {
            if (hitObject.getSceneGraphObject() instanceof BorderPane) {
                final AbstractDriver driver = contentPanelController.lookupDriver(hitObject);
                assert driver instanceof BorderPaneDriver;
                dropTarget = driver.makeDropTarget(hitObject, hitX, hitY);
                newHitParent = hitObject;
                newHitParentMask = m;
            }
        }
        
        // hitObject has sub-components (ie it is a container)
        if (dropTarget == null) {
            if (m.isAcceptingSubComponent()) {
                final AbstractDriver driver = contentPanelController.lookupDriver(hitObject);
                dropTarget = driver.makeDropTarget(hitObject, hitX, hitY);
                newHitParent = hitObject;
                newHitParentMask = m;
            }
        }
        
        // hitObject accepts Accessory.CONTENT
        if (dropTarget == null) {
            if (m.isAcceptingAccessory(Accessory.CONTENT)) {
                assert hitObject instanceof FXOMInstance;
                dropTarget = new AccessoryDropTarget((FXOMInstance)hitObject, Accessory.CONTENT);
                newHitParent = hitObject;
                newHitParentMask = m;
            }
        }
        
        // hitObject parent is a container ?
        if (dropTarget == null) {
            final FXOMObject hitObjectParent = hitObject.getParentObject();
            if (hitObjectParent != null) {
                final DesignHierarchyMask mp = new DesignHierarchyMask(hitObjectParent);
                if (mp.isAcceptingSubComponent()) {
                    final AbstractDriver driver = contentPanelController.lookupDriver(hitObjectParent);
                    dropTarget = driver.makeDropTarget(hitObjectParent, hitX, hitY);
                    newHitParent = hitObjectParent;
                    newHitParentMask = mp;
                }
            }
        }
                
        // Update movingGuideController
        if (newHitParent != hitParent) {
            hitParent = newHitParent;
            hitParentMask = newHitParentMask;
            if (hitParent == null) {
                assert hitParentMask == null;
                movingGuideController.clearSampleBounds();
            } else {
                assert hitParentMask != null;
                if (hitParentMask.isFreeChildPositioning() && dragSource.isNodeOnly()) {
                    populateMovingGuideController();
                } else {
                    movingGuideController.clearSampleBounds();
                }
            }
        }
        
        final double guidedX, guidedY;
        if (movingGuideController.hasSampleBounds() && (guidesDisabled == false)) {
            updateShadow(hitX, hitY);
            final Bounds shadowBounds = shadow.getLayoutBounds();
            final Bounds shadowBoundsInScene = shadow.localToScene(shadowBounds, true /* rootScene */);
            movingGuideController.match(shadowBoundsInScene);
            
            guidedX = hitX + movingGuideController.getSuggestedDX();
            guidedY = hitY + movingGuideController.getSuggestedDY();
        } else {
            guidedX = hitX;
            guidedY = hitY;
        }

        updateShadow(guidedX, guidedY);
        
        if (!MathUtils.equals(guidedX , hitX) || !MathUtils.equals(guidedY, hitY)) {
            assert dropTarget != null;
            assert dropTarget instanceof ContainerXYDropTarget;
            final AbstractDriver driver = contentPanelController.lookupDriver(dropTarget.getTargetObject());
            dropTarget = driver.makeDropTarget(hitParent, guidedX, guidedY);
            assert dropTarget instanceof ContainerXYDropTarget;
        }
        
        dragController.setDropTarget(dropTarget);
        lastDragEvent.acceptTransferModes(dragController.getAcceptedTransferModes());
        
    }
    
    private void dragExitedGlassLayer() {
        
        dragController.setDropTarget(null);
        hideShadow();
        movingGuideController.clearSampleBounds();

        if (willReceiveDragDone == false) {
            dragDoneOnGlassLayer();
        }
    }
    
    private void dragDroppedOnGlassLayer() {    
        lastDragEvent.setDropCompleted(true);
        dragController.commit();
        contentPanelController.getGlassLayer().requestFocus();
    }
    
    private void dragDoneOnGlassLayer() {
        if (shouldInvokeEnd) {
            dragController.end();
        }
        performTermination();
    }
    
    private void handleKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) {
            dragExitedGlassLayer();
            if (willReceiveDragDone) {
                // dragDone will not arrive but 
                // we need to execute the corresponding logic
                dragDoneOnGlassLayer();
            }
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
    
    
    private void performTermination() {
        final Node glassLayer = contentPanelController.getGlassLayer();
        glassLayer.setOnDragEntered(null);
        glassLayer.setOnDragOver(null);
        glassLayer.setOnDragExited(null);
        glassLayer.setOnDragDropped(null);
        glassLayer.setOnDragDone(null);
        glassLayer.setOnKeyPressed(null);
        
        dismantleMovingGuideController();
        
        observer.gestureDidTerminate(this);
        observer = null;
        
        dragEnteredEvent = null;
        lastDragEvent = null;
        shouldInvokeEnd = false;
        hitParent = null;
        hitParentMask = null;
        assert shadow == null; // Because dragExitedGlassLayer() called hideShadow()
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
        final Point2D p = rudderLayer.sceneToLocal(hitX, hitY, true /* rootScene */);
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
        final Bounds scopeInScene = contentPanelController.getWorkspacePane().localToScene(scope, true /* rootScene */);
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
            final Bounds insetBoundsInScene = hitParentNode.localToScene(insetBounds, true /* rootScene */);
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
