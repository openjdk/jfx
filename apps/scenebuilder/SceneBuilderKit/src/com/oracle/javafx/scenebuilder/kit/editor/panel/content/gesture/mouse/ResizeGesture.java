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
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.relocater.AbstractRelocater;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.relocater.AnchorPaneRelocater;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.relocater.PaneRelocater;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer.AbstractResizer;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer.AbstractResizer.Feature;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.rudder.ResizeRudder;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.guides.ResizingGuideController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.CardinalPoint;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.RegionRectangle;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Transform;

/**
 *
 * 
 */
public class ResizeGesture extends AbstractMouseGesture {
    
    private final FXOMInstance fxomInstance;
    private final CardinalPoint tunable;
    private final ResizeRudder rudder;
    
    private AbstractResizer<?> resizer;
    private AbstractRelocater<?> relocater;
    private ResizingGuideController resizingGuideController;
    private RegionRectangle shadow;
    private boolean snapEnabled;
    private boolean guidesDisabled;

    public ResizeGesture(ContentPanelController contentPanelController, FXOMInstance fxomInstance, CardinalPoint tunable) {
        super(contentPanelController);
        assert contentPanelController.lookupDriver(fxomInstance) != null;
        assert fxomInstance.getSceneGraphObject() instanceof Node;
        this.fxomInstance = fxomInstance;
        this.tunable = tunable;
        this.rudder = new ResizeRudder(contentPanelController, fxomInstance);
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
        resizer = contentPanelController.lookupDriver(fxomInstance).makeResizer(fxomInstance);
        assert resizer != null;
        assert resizer.getSceneGraphObject() == fxomInstance.getSceneGraphObject();
        
        final Node sceneGraphObject = resizer.getSceneGraphObject();
        final Parent sceneGraphParent = sceneGraphObject.getParent();
        assert sceneGraphParent != null;
        if (sceneGraphParent.getClass() == Pane.class) {
            relocater = new PaneRelocater(sceneGraphObject);
        } else if (sceneGraphParent.getClass() == AnchorPane.class) {
            relocater = new AnchorPaneRelocater(sceneGraphObject);
        } else {
            relocater = null;
        }
        
        if (relocater != null && contentPanelController.isGuidesVisible()) {
            setupResizingGuideController();
            assert resizingGuideController != null;
        }
        
        snapEnabled = getMousePressedEvent().isShiftDown();
        
        setupAndOpenHudWindow();
        showShadow();
        
        contentPanelController.getHandleLayer().setVisible(false);
        
        // Now same as mouseDragged
        mouseDragged();
    }
    
    @Override
    protected void mouseDragged() {   
        setRudderVisible(isSnapRequired());      
        updateSceneGraphObjectSize();
        contentPanelController.getHudWindowController().updatePopupLocation();
        updateShadow();
    }

    @Override
    protected void mouseDragEnded() {
        updateSceneGraphObjectSize();
        
        /*
         * Three steps
         * 
         * 1) Collects sizing properties that have changed
         * 2) Reverts to initial sizing
         *    => this step is equivalent to userDidCancel()
         * 3) Push a BatchModifyObjectJob to officially resize the object
         */
        
        // Step #1
        final Map<PropertyName, Object> changeMap = new HashMap<>();
        changeMap.putAll(resizer.getChangeMap());
        if (relocater != null) {
            changeMap.putAll(relocater.getChangeMap());
        }
        
        // Step #2
        userDidCancel();
        
        // Step #3
        final Metadata metadata = Metadata.getMetadata();
        final Map<ValuePropertyMetadata, Object> metaValueMap = new HashMap<>();
        for (Map.Entry<PropertyName,Object> e : changeMap.entrySet()) {
            final ValuePropertyMetadata vpm = metadata.queryValueProperty(fxomInstance, e.getKey());
            assert vpm != null;
            metaValueMap.put(vpm, e.getValue());
        }
        if (changeMap.isEmpty() == false) {
            final EditorController editorController 
                    = contentPanelController.getEditorController();
            for (Map.Entry<ValuePropertyMetadata, Object> e : metaValueMap.entrySet()) {
                final ModifyObjectJob job = new ModifyObjectJob(
                        fxomInstance,
                        e.getKey(),
                        e.getValue(),
                        editorController,
                        "Resize");
                if (job.isExecutable()) {
                    editorController.getJobManager().push(job);
                }
            }
        }

    }

    @Override
    protected void mouseReleased() {
        // Everything is done in mouseDragEnded
    }

    @Override
    protected void keyEvent(KeyEvent ke) {
        if (ke.getCode() == KeyCode.SHIFT) {
            final EventType<KeyEvent> eventType = ke.getEventType();
            if (eventType == KeyEvent.KEY_PRESSED) {
                snapEnabled = true;
            } else if (eventType == KeyEvent.KEY_RELEASED) {
                snapEnabled = false;
            }
            if (isMouseDidDrag()) {
                mouseDragged();
            }
        } else if (ke.getCode() == KeyCode.ALT) {
            final EventType<KeyEvent> eventType = ke.getEventType();
            if (eventType == KeyEvent.KEY_PRESSED) {
                guidesDisabled = true;
            } else if (eventType == KeyEvent.KEY_RELEASED) {
                guidesDisabled = false;
            }
            if (isMouseDidDrag()) {
                mouseDragged();
            }
        }
    }

    @Override
    protected void userDidCancel() {
        resizer.revertToOriginalSize();
        if (relocater != null) {
            relocater.revertToOriginalLocation();
        }
        if (resizingGuideController != null) {
            dismantleResizingGuideController();
            assert resizingGuideController == null;
        }
        setRudderVisible(false);
        hideShadow();
        contentPanelController.getHudWindowController().closeWindow();
        contentPanelController.getHandleLayer().setVisible(true);
        resizer.getSceneGraphObject().getParent().layout();
    }
    
    
    /*
     * Private
     */
    
    private void updateSceneGraphObjectSize() {
        assert resizer != null;
        
        // Put the scene graph object back in its size/location at mouse pressed time
        resizer.revertToOriginalSize();
        if (relocater != null) {
            relocater.revertToOriginalLocation();
        }
        final Node sceneGraphObject = resizer.getSceneGraphObject();
        sceneGraphObject.getParent().layout();
        
        // Compute mouse displacement in local coordinates of scene graph object
        final double startSceneX = getMousePressedEvent().getSceneX();
        final double startSceneY = getMousePressedEvent().getSceneY();
        final double currentSceneX = getLastMouseEvent().getSceneX();
        final double currentSceneY = getLastMouseEvent().getSceneY();
        final Point2D start = sceneGraphObject.sceneToLocal(startSceneX, startSceneY, true /* rootScene */);
        final Point2D current = sceneGraphObject.sceneToLocal(currentSceneX, currentSceneY, true /* rootScene */);
        final double rawDeltaX, rawDeltaY;
        if ((start != null) && (current != null)) {
            rawDeltaX = current.getX() - start.getX();
            rawDeltaY = current.getY() - start.getY();
        } else {
            // sceneGraphObject is bizarrely configured (eg it has scaleX=0)
            // We use the scene coordinates
            rawDeltaX = currentSceneX - startSceneX;
            rawDeltaY = currentSceneY - startSceneY;
        }
        
        // Clamps deltaX/deltaY relatively to tunable.
        // Example: tunable == E => clampDeltaX = rawDeltaX, clampDeltaY = 0.0
        final Point2D clampDelta = tunable.clampVector(rawDeltaX, rawDeltaY);
        final double clampDeltaX = clampDelta.getX();
        final double clampDeltaY = clampDelta.getY();
        
        // Compute candidateBounds
        final Bounds layoutBounds = sceneGraphObject.getLayoutBounds();
        final Bounds resizedBounds = tunable.getResizedBounds(layoutBounds, clampDeltaX, clampDeltaY);
        final Bounds candidateBounds;
        if (isSnapRequired()) {
            final double ratio = layoutBounds.getHeight() / layoutBounds.getWidth();
            candidateBounds = tunable.snapBounds(resizedBounds, ratio);
        } else {
            candidateBounds = resizedBounds;
        }
        
        // Computes new layout bounds from the candidate bounds
        final double candidateWidth = candidateBounds.getWidth();
        final double candidateHeight = candidateBounds.getHeight();
        final Bounds newLayoutBounds = resizer.computeBounds(candidateWidth, candidateHeight);
        
        final Bounds guidedLayoutBounds;
        if (resizingGuideController == null) {
            guidedLayoutBounds = newLayoutBounds;
        } else if (guidesDisabled) {
            resizingGuideController.clear();
            guidedLayoutBounds = newLayoutBounds;
        } else {
            resizingGuideController.match(newLayoutBounds);
            final double suggestedWidth  = resizingGuideController.getSuggestedWidth();
            final double suggestedHeight = resizingGuideController.getSuggestedHeight();
            guidedLayoutBounds = resizer.computeBounds(suggestedWidth, suggestedHeight);
        }

        // Now computes the new location (in parent's local coordinate space)
        final CardinalPoint fix = tunable.getOpposite();
        final Point2D currentFixPos = fix.getPosition(layoutBounds);
        final Point2D newFixPos = fix.getPosition(guidedLayoutBounds);
        final Point2D currentParent = sceneGraphObject.localToParent(currentFixPos);
        final Point2D newParent = sceneGraphObject.localToParent(newFixPos);
        final double layoutDX = currentParent.getX() - newParent.getX();
        final double layoutDY = currentParent.getY() - newParent.getY();
        final double newLayoutX = sceneGraphObject.getLayoutX() + layoutDX;
        final double newLayoutY = sceneGraphObject.getLayoutY() + layoutDY;
        
        // Apply the new size and new location
        resizer.changeWidth(guidedLayoutBounds.getWidth());
        resizer.changeHeight(guidedLayoutBounds.getHeight());
        if (relocater != null) {
            sceneGraphObject.getParent().layout();
            relocater.moveToLayoutX(newLayoutX, guidedLayoutBounds);
            relocater.moveToLayoutY(newLayoutY, guidedLayoutBounds);
        }
        sceneGraphObject.getParent().layout();
                
        updateHudWindow();
    }

    
    private boolean isSnapRequired() {
        return snapEnabled || (resizer.getFeature() == Feature.SCALING);
    }
    
    private void setRudderVisible(boolean visible) {
        final boolean alreadyVisible = rudder.getRootNode().getParent() != null;
        
        if (alreadyVisible != visible) {
            final Group rudderLayer = contentPanelController.getRudderLayer();
            if (visible) {
                assert rudder.getRootNode().getParent() == null;
                rudderLayer.getChildren().add(rudder.getRootNode());
            } else {
                assert rudder.getRootNode().getParent() == rudderLayer;
                rudderLayer.getChildren().remove(rudder.getRootNode());
            }
        }
    }
    
    
    private void setupAndOpenHudWindow() {
        final HudWindowController hudWindowController
                = contentPanelController.getHudWindowController();
        
        
        final int sizeRowCount = resizer.getPropertyNames().size();
        final int locationRowCount;
        if (relocater != null) {
            locationRowCount = relocater.getPropertyNames().size();
        } else {
            locationRowCount = 0;
        }
        hudWindowController.setRowCount(sizeRowCount + locationRowCount);
        
        
        final List<PropertyName> sizePropertyNames = resizer.getPropertyNames();
        for (int i = 0; i < sizeRowCount; i++) {
            final PropertyName pn = sizePropertyNames.get(i);
            hudWindowController.setNameAtRowIndex(makeNameString(pn), i);
        }
        
        if (relocater != null) {
            final List<PropertyName> locationPropertyNames = relocater.getPropertyNames();
            for (int i = 0; i < locationRowCount; i++) {
                final PropertyName pn = locationPropertyNames.get(i);
                hudWindowController.setNameAtRowIndex(makeNameString(pn), sizeRowCount+i);
            }
        }
        
        updateHudWindow();
        
        hudWindowController.setRelativePosition(tunable);
        hudWindowController.openWindow(resizer.getSceneGraphObject());
    }
    
    private String makeNameString(PropertyName pn) {
        return pn.getName() + ":";
    }
    
    
    private void updateHudWindow() {
        final HudWindowController hudWindowController
                = contentPanelController.getHudWindowController();
        final List<PropertyName> sizePropertyNames = resizer.getPropertyNames();
        final int sizeRowCount = sizePropertyNames.size();

        for (int i = 0; i < sizeRowCount; i++) {
            final PropertyName pn = sizePropertyNames.get(i);
            final String value = String.valueOf(resizer.getValue(pn));
            hudWindowController.setValueAtRowIndex(value, i);
        }
        
        if (relocater != null) {
            final List<PropertyName> locationPropertyNames = relocater.getPropertyNames();
            final int locationRowCount = locationPropertyNames.size();
            for (int i = 0; i < locationRowCount; i++) {
                final PropertyName pn = locationPropertyNames.get(i);
                final String value = String.valueOf(relocater.getValue(pn));
                hudWindowController.setValueAtRowIndex(value, sizeRowCount+i);
            }
        }
    }
    
    
    private void showShadow() {
        assert shadow == null;
        
        shadow = new RegionRectangle();
        shadow.getRegion().getStyleClass().add("resize-shadow");
        shadow.setMouseTransparent(true);
        contentPanelController.getRudderLayer().getChildren().add(shadow);
        
        updateShadow();
    }
    
    private void updateShadow() {
        assert shadow != null;
        
        final Node sceneGraphObject
                = resizer.getSceneGraphObject();
        final Transform sceneGraphObjectTransform
                = contentPanelController.computeSceneGraphToRudderLayerTransform(sceneGraphObject);
        shadow.getTransforms().clear();
        shadow.getTransforms().add(sceneGraphObjectTransform);
        shadow.setLayoutBounds(sceneGraphObject.getLayoutBounds());
    }
    
    private void hideShadow() {
        assert shadow != null;
        contentPanelController.getRudderLayer().getChildren().remove(shadow);
        shadow = null;
    }
    
    
    private void setupResizingGuideController() {
        final boolean matchWidth, matchHeight;
        
        switch(tunable) {
            case N:
            case S:
                matchWidth = false;
                matchHeight = true;
                break;
            case E:
            case W:
                matchWidth = true;
                matchHeight = false;
                break;
            default:
            case SE:
            case SW:
            case NE:
            case NW:
                matchWidth = true;
                matchHeight = true;
                break;
        }
        resizingGuideController = new ResizingGuideController(
                matchWidth, matchHeight, contentPanelController.getGuidesColor());
        
        addToResizingGuideController(fxomInstance.getFxomDocument().getFxomRoot());
        
        final Group rudderLayer = contentPanelController.getRudderLayer();
        final Group guideGroup = resizingGuideController.getGuideGroup();
        assert guideGroup.isMouseTransparent();
        rudderLayer.getChildren().add(guideGroup);
    }
    
    
    private void addToResizingGuideController(FXOMObject fxomObject) {
        assert fxomObject != null;
        
        if (fxomObject != fxomInstance) {
            if (fxomObject.getSceneGraphObject() instanceof Node) {
                final Node sceneGraphNode = (Node) fxomObject.getSceneGraphObject();
                resizingGuideController.addSampleBounds(sceneGraphNode);
            }

            final DesignHierarchyMask m = new DesignHierarchyMask(fxomObject);
            if (m.isAcceptingSubComponent()) {
                for (int i = 0, count = m.getSubComponentCount(); i < count; i++) {
                    addToResizingGuideController(m.getSubComponentAtIndex(i));
                }
            }
        }
    }
    
    
    private void dismantleResizingGuideController() {
        assert resizingGuideController != null;
        final Group guideGroup = resizingGuideController.getGuideGroup();
        final Group rudderLayer = contentPanelController.getRudderLayer();
        assert rudderLayer.getChildren().contains(guideGroup);
        rudderLayer.getChildren().remove(guideGroup);
        resizingGuideController = null;
    }
}
