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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.mode;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.DragController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.GridPaneDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.RootDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.RelocateSelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.messagelog.MessageLog;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.AbstractDecoration;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.AbstractDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.gridpane.GridPaneHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.gridpane.GridPaneTring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.AbstractHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.AbstractPring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.AbstractTring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.AbstractGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.DragGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.SelectAndMoveGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.mouse.SelectWithMarqueeGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.ZoomGesture;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.gesture.key.MoveWithKeyGesture;
import com.oracle.javafx.scenebuilder.kit.editor.util.InlineEditController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.editor.util.ContextMenuController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.DragEvent;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

/**
 *
 * 
 */

/**
 *
 * 
 */
public class EditModeController extends AbstractModeController
implements AbstractGesture.Observer {
    
    private final List<AbstractHandles<?>> handles = new ArrayList<>();
    private final Set<FXOMObject> excludes = new HashSet<>();
    private final SelectWithMarqueeGesture selectWithMarqueeGesture;
    private final SelectAndMoveGesture selectAndMoveGesture;
    private final ZoomGesture zoomGesture;
    
    private AbstractPring<?> pring;
    private AbstractTring<?> tring;
    private AbstractGesture activeGesture;
    private AbstractGesture glassGesture;
    private FXOMInstance inlineEditedObject;
    
    
    public EditModeController(ContentPanelController contentPanelController) {
        super(contentPanelController);
        selectWithMarqueeGesture = new SelectWithMarqueeGesture(contentPanelController);
        selectAndMoveGesture = new SelectAndMoveGesture(contentPanelController);
        zoomGesture = new ZoomGesture(contentPanelController);
    }
    
    
    /**
     * Returns null or the handles associated to the specified fxom object.
     * 
     * @param fxomObject an fxom object (never null)
     * @return null or the handles associated to the specified fxom object.
     */

    public AbstractHandles<?> lookupHandles(FXOMObject fxomObject) {
        assert fxomObject != null;
        
        AbstractHandles<?> result = null;
        for (AbstractHandles<?> h : handles) {
            if (h.getFxomObject() == fxomObject) {
                result = h;
                break;
            }
        }
        
        return result;
    }
    
    /*
     * AbstractGesture.Observer
     */
    
    @Override
    public void gestureDidTerminate(AbstractGesture gesture) {
        assert activeGesture == gesture;
        activeGesture = null;
        startListeningToInputEvents();
        contentPanelController.endInteraction();
    }
    
    /*
     * AbstractModeController
     */
    
    @Override
    public void willResignActive(AbstractModeController nextModeController) {
        stopListeningToInputEvents();
    }

    @Override
    public void didBecomeActive(AbstractModeController previousModeController) {
        assert contentPanelController.getGlassLayer() != null;
        assert contentPanelController.getHandleLayer() != null;
        assert contentPanelController.getPringLayer() != null;
        
        editorSelectionDidChange();
        startListeningToInputEvents();
    }
    
    @Override
    public void editorSelectionDidChange() {
        updateParentRing();
        updateHandles();
        makeSelectionVisible();
    }

    @Override
    public void fxomDocumentDidChange(FXOMDocument oldDocument) {
        // Same logic as when the scene graph is changed
        fxomDocumentDidRefreshSceneGraph();
    }

    @Override
    public void fxomDocumentDidRefreshSceneGraph() {
        updateParentRing();
        updateHandles();
        
        // Object below the mouse may have changed : current glass gesture
        // must searched again.
        this.glassGesture = null;
    }

    @Override
    public void dropTargetDidChange() {
        updateTring();
    }

    /*
     * Private
     */
    
    private void makeSelectionVisible() {
        
        // Scrolls the content panel so that selected objects are visible.
        contentPanelController.scrollToSelection();
        
        // Walks trough the ancestor nodes of the first selected object and
        // makes sure that TabPane and Accordion are setup for displaying 
        // this selected object.
        if (handles.isEmpty() == false) {
            contentPanelController.reveal(handles.get(0).getFxomObject());
        }
    }
    
    /*
     * Private (pring)
     */
    private void updateParentRing() {
        final AbstractPring<?> newPring;
        
        if (contentPanelController.isContentDisplayable()) {
            final Selection selection 
                    = contentPanelController.getEditorController().getSelection();
            if ((pring == null) || (pring.getFxomObject() != selection.getAncestor())) {
                if (selection.getAncestor() != null) {
                    newPring = makePring(selection.getAncestor());
                } else {
                    newPring = null;
                }
            } else {
                switch(pring.getState()) {
                    default:
                    case CLEAN:
                        newPring = pring;
                        break;
                    case NEEDS_RECONCILE:
                        newPring = pring;
                        pring.reconcile();;
                        break;
                    case NEEDS_REPLACE:
                        newPring = makePring(pring.getFxomObject());
                        break;
                }
            }
        } else {
            // Document content cannot be displayed in content panel
            newPring = null;
        }
        
        if (newPring != pring) {
            final Group pringLayer = contentPanelController.getPringLayer();
            if (pring != null) {
                pringLayer.getChildren().remove(pring.getRootNode());
            }
            pring = newPring;
            if (pring != null) {
                pringLayer.getChildren().add(pring.getRootNode());
            }
        } else {
            assert (pring == null) || pring.getState() == AbstractPring.State.CLEAN;
        }
    }
    
    private AbstractPring<?> makePring(FXOMObject fxomObject) {
        final AbstractDriver driver = contentPanelController.lookupDriver(fxomObject);
        final AbstractPring<?> result;
        
        if (driver != null) {
            result = driver.makePring(fxomObject);
            if (result != null) {
                result.changeStroke(contentPanelController.getPringColor());
            }
        } else {
            result = null;
        }
        
        return result;
    }
    

    /*
     * Private (tring)
     */
    private void updateTring() {
        final DragController dragController
                = contentPanelController.getEditorController().getDragController();
        final AbstractTring<?> newTring;
        
        if (dragController.isDropAccepted()
                && contentPanelController.isContentDisplayable()) {
            final AbstractDropTarget dropTarget = dragController.getDropTarget();
            if ((tring instanceof GridPaneTring) && (dropTarget instanceof GridPaneDropTarget)) {
                // Let's reuse the GridPaneTring (because it's costly)
                newTring = tring;
                updateTring((GridPaneTring) tring, (GridPaneDropTarget) dropTarget);
            } else {
                newTring = makeTring(dragController.getDropTarget());
            }
        } else {
            newTring = null;
        }
        
        if (newTring != tring) {
            final Group rudderLayer = contentPanelController.getRudderLayer();
            if (tring != null) {
                rudderLayer.getChildren().remove(tring.getRootNode());
            }
            tring = newTring;
            if (tring != null) {
                rudderLayer.getChildren().add(tring.getRootNode());
            }
        } else {
            assert (tring == null) || tring.getState() == AbstractPring.State.CLEAN;
        }
    }
    
    private void updateTring(GridPaneTring tring, GridPaneDropTarget dropTarget) {
        assert tring != null;
        assert dropTarget != null;
        
        tring.setupWithDropTarget(dropTarget);
    }
    
    private AbstractTring<?> makeTring(AbstractDropTarget dropTarget) {
        final AbstractTring<?> result;
        
        if (dropTarget.getTargetObject() == null) {
            assert dropTarget instanceof RootDropTarget;
            result = null;
        } else {
            final AbstractDriver driver 
                    = contentPanelController.lookupDriver(dropTarget.getTargetObject());
            if (driver != null) {
                result = driver.makeTring(dropTarget);
                if (result != null) {
                    result.changeStroke(contentPanelController.getPringColor());
                }
            } else {
                result = null;
            }
        }
        
        return result;
    }
    
    /*
     * Private (handles)
     */

    private void updateHandles() {
        final Selection selection = contentPanelController.getEditorController().getSelection();
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            updateHandles((ObjectSelectionGroup) selection.getGroup());
        } else if (selection.getGroup() instanceof GridSelectionGroup) {
            updateHandles((GridSelectionGroup) selection.getGroup());
        } else {
            assert selection.getGroup() == null 
                    : "Implement updateHandles() for " + selection.getGroup();
            // Selection is empty : removes all handles
            removeAllHandles();
        }
    }
    
    private void updateHandles(ObjectSelectionGroup osg) {
        final List<AbstractHandles<?>> obsoleteHandles = new ArrayList<>();
        final List<FXOMObject> incomingObjects = new ArrayList<>();
        
        // Collects fxom objects from selection
        if (contentPanelController.isContentDisplayable()) {
            incomingObjects.addAll(osg.getItems());
        }
        
        // Collects obsolete handles
        for (AbstractHandles<?> h : handles) {
            if (incomingObjects.contains(h.getFxomObject())) {
                // FXOM object associated to these handles is still selected
                switch(h.getState()) {
                    case CLEAN:
                        incomingObjects.remove(h.getFxomObject());
                        break;
                    case NEEDS_RECONCILE:
                        // scene graph associated to h has changed but h is still compatible
                        h.reconcile();
                        incomingObjects.remove(h.getFxomObject());
                        break;
                    case NEEDS_REPLACE:
                        // h is no longer compatible with the new scene graph object 
                        obsoleteHandles.add(h);
                        break;
                }
                // If h is grid pane handles reset the selected columns/rows
                if (h instanceof GridPaneHandles) {
                    final GridPaneHandles gph = (GridPaneHandles) h;
                    gph.updateColumnRowSelection(null);
                }
            } else {
                // FXOM object associated to these handles is no longer selected
                // => handles become obsolete
                obsoleteHandles.add(h);
            }
        }
        
        // Let's create new handles for the incoming objects
        excludes.clear();
        final Group handleLayer = contentPanelController.getHandleLayer();
        for (FXOMObject incomingObject : incomingObjects) {
            final AbstractDriver driver = contentPanelController.lookupDriver(incomingObject);
            if (driver == null) {
                // incomingObject cannot be managed by content panel (eg MenuItem)
                excludes.add(incomingObject);
            } else {
                final AbstractHandles<?> newHandles = driver.makeHandles(incomingObject);
                handleLayer.getChildren().add(newHandles.getRootNode());
                handles.add(newHandles);
            }
        }
        
        // Let's disconnect the obsolete handles
        for (AbstractHandles<?> h : obsoleteHandles) {
            handleLayer.getChildren().remove(h.getRootNode());
            handles.remove(h);
        }
    }
    
    
    private void updateHandles(GridSelectionGroup gsg) {
        final List<AbstractHandles<?>> obsoleteHandles = new ArrayList<>();
        
        // Collects obsolete handles
        if (contentPanelController.isContentDisplayable()) {
            for (AbstractHandles<?> h : handles) {
                if (h.getFxomObject() == gsg.getParentObject()) {
                    assert h instanceof GridPaneHandles;

                    if (h.getState() == AbstractDecoration.State.NEEDS_RECONCILE) {
                        // scene graph associated to h has changed but h is still compatible
                        h.reconcile();
                    } else {
                        assert h.getState() == AbstractDecoration.State.CLEAN;
                    }

                    final GridPaneHandles gph = (GridPaneHandles) h;
                    gph.updateColumnRowSelection(gsg);
                } else {
                    // FXOM object associated to these handles is no longer selected
                    // => handles become obsolete
                    obsoleteHandles.add(h);
                }
            }
        } else {
            // Document content is not displayed (because its root is not a node)
            // => all handles are obsoletes
            obsoleteHandles.addAll(handles);
        }
        
        // Let's create new handles for the incoming objects
        excludes.clear();
        final Group handleLayer = contentPanelController.getHandleLayer();
        if (handles.size() == obsoleteHandles.size()) {
            // No handles for grid pane row/column selection : creates one.
            assert gsg.getParentObject().getSceneGraphObject() instanceof GridPane;
            final AbstractDriver driver = contentPanelController.lookupDriver(gsg.getParentObject());
            assert driver != null;
            final AbstractHandles<?> newHandles = driver.makeHandles(gsg.getParentObject());
            handleLayer.getChildren().add(newHandles.getRootNode());
            handles.add(newHandles);
            assert newHandles instanceof GridPaneHandles;
            final GridPaneHandles gridPaneHandles = (GridPaneHandles) newHandles;
            gridPaneHandles.updateColumnRowSelection(gsg);
        }
        
        // Let's disconnect the obsolete handles
        for (AbstractHandles<?> h : obsoleteHandles) {
            handleLayer.getChildren().remove(h.getRootNode());
            handles.remove(h);
        }
    }
    
    private void removeAllHandles() {
        final Group handleLayer = contentPanelController.getHandleLayer();
        for (AbstractHandles<?> h : new ArrayList<>(handles)) {
            handleLayer.getChildren().remove(h.getRootNode());
            handles.remove(h);
        }
    }
    
    /*
     * Private (event listeners)
     */

    private final EventHandler<MouseEvent> mouseEnteredGlassLayerListener
            = new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    mouseEnteredGlassLayer(e);
                }
            };
    
    private final EventHandler<MouseEvent> mouseExitedGlassLayerListener
            = new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    mouseExitedGlassLayer(e);
                }
            };
    
    private final EventHandler<MouseEvent> mouseMovedOnGlassLayerListener
            = new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    mouseMovedOnGlassLayer(e);
                }
            };
    
    private final EventHandler<MouseEvent> mousePressedOnGlassLayerListener
            = new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    mousePressedOnGlassLayer(e);
                }
            };
    
    private final EventHandler<KeyEvent> keyPressedOnGlassLayerListener
            = new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent e) {
                    keyPressedOnGlassLayer(e);
                }
            };
    
    private final EventHandler<ZoomEvent> zoomStartedOnGlassLayer
            = new EventHandler<ZoomEvent>() {
                @Override
                public void handle(ZoomEvent e) {
                    zoomStartedOnGlassLayer(e);
                }
            };
    
    private final EventHandler<DragEvent> dragEnteredGlassLayerListener
            = new EventHandler<DragEvent>() {
                @Override
                public void handle(DragEvent e) {
                    dragEnteredGlassLayer(e);
                }
            };

    private final EventHandler<MouseEvent> mousePressedOnHandleLayerListener
            = new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    mousePressedOnHandleLayer(e);
                }
            };

    private final EventHandler<MouseEvent> mousePressedOnPringLayerListener
            = new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    mousePressedOnPringLayer(e);
                }
            };

    private void startListeningToInputEvents() {
        final Node glassLayer = contentPanelController.getGlassLayer();
        assert glassLayer.getOnMouseEntered() == null;
        assert glassLayer.getOnMouseExited() == null;
        assert glassLayer.getOnMouseMoved() == null;
        assert glassLayer.getOnMousePressed() == null;
        assert glassLayer.getOnKeyPressed() == null;
        assert glassLayer.getOnZoomStarted() == null;
        assert glassLayer.getOnDragEntered() == null;
        
        glassLayer.setOnMouseEntered(mouseEnteredGlassLayerListener);
        glassLayer.setOnMouseExited(mouseExitedGlassLayerListener);
        glassLayer.setOnMouseMoved(mouseMovedOnGlassLayerListener);
        glassLayer.setOnMousePressed(mousePressedOnGlassLayerListener);
        glassLayer.setOnKeyPressed(keyPressedOnGlassLayerListener);
        glassLayer.setOnZoomStarted(zoomStartedOnGlassLayer);
        glassLayer.setOnDragEntered(dragEnteredGlassLayerListener);
        
        final Node handleLayer = contentPanelController.getHandleLayer();
        assert handleLayer.getOnMousePressed() == null;
        handleLayer.setOnMousePressed(mousePressedOnHandleLayerListener);
        
        final Node pringLayer = contentPanelController.getPringLayer();
        assert pringLayer.getOnMousePressed() == null;
        pringLayer.setOnMousePressed(mousePressedOnPringLayerListener);
    }
    
    private void stopListeningToInputEvents() {
        
        final Node glassLayer = contentPanelController.getGlassLayer();
        glassLayer.setOnMouseEntered(null);
        glassLayer.setOnMouseExited(null);
        glassLayer.setOnMouseMoved(null);
        glassLayer.setOnMousePressed(null);
        glassLayer.setOnKeyPressed(null);
        glassLayer.setOnZoomStarted(null);
        glassLayer.setOnDragEntered(null);
        
        final Node handleLayer = contentPanelController.getHandleLayer();
        handleLayer.setOnMousePressed(null);
        
        final Node pringLayer = contentPanelController.getPringLayer();
        pringLayer.setOnMousePressed(null);
    }
    
    
    /*
     * Private (event handlers)
     */

    private void mouseEnteredGlassLayer(MouseEvent e) {
        mouseMovedOnGlassLayer(e);
    }
    
    private void mouseExitedGlassLayer(MouseEvent e) {
        assert activeGesture == null : "activateGesture=" + activeGesture;
        glassGesture = null;
    }
    
    private void mouseMovedOnGlassLayer(MouseEvent e) {
        assert activeGesture == null : "activateGesture=" + activeGesture;
        
        /*
         *   1) hitObject == null
         *                  => mouse is over the workspace/background
         *                  => mouse press+drag should "select with marquee"
         *
         *   2) hitObject != null
         *
         *      2.1) hitObject == root object
         *                  => mouse is over the root object (possibly selected)
         *                  => mouse press+drag should "select with marquee"
         * 
         *      2.2) hitObject != root object
         * 
         *          2.2.1) hitObject is the selectionAncestor
         *                  => mouse is over the "parent ring object"
         *                  => mouse press+drag should "select with marquee"
         *
         *          2.2.2) hitObject is not the selectionAncestor
         *                  => mouse is over an object
         *                  => this object is inside or outside of the parent ring
         *                  => mouse press+drag should "select and move"
         */
        
        final FXOMObject hitObject 
                = contentPanelController.pick(e.getSceneX(), e.getSceneY());
        final FXOMObject selectionAncestor
                = contentPanelController.getEditorController().getSelection().getAncestor();
        final FXOMDocument fxomDocument
                = contentPanelController.getEditorController().getFxomDocument();
        final FXOMObject fxomRoot
                = (fxomDocument == null) ? null : fxomDocument.getFxomRoot();
        if (hitObject == null) {
            // Case #1
            selectWithMarqueeGesture.setup(null, selectionAncestor);
            glassGesture = selectWithMarqueeGesture;
        } else if (hitObject == fxomRoot) {
            // Case #2.1
            selectWithMarqueeGesture.setup(fxomRoot, fxomRoot);
            glassGesture = selectWithMarqueeGesture;
        } else if (hitObject == selectionAncestor) {
            // Case #2.2.1
            selectWithMarqueeGesture.setup(selectionAncestor, selectionAncestor);
            glassGesture = selectWithMarqueeGesture;
        } else { 
            // Case #2.2.2
            selectAndMoveGesture.setHitObject(hitObject);
            selectAndMoveGesture.setHitSceneX(e.getSceneX());
            selectAndMoveGesture.setHitSceneY(e.getSceneY());
            glassGesture = selectAndMoveGesture;
        }
    }
    
    private void mousePressedOnGlassLayer(MouseEvent e) {
        
        /*
         * At that point, is expected that a "mouse entered" or "mouse moved" 
         * event was received before and that this.glassGesture is setup.
         * 
         * However this is no always the case. It may be null in two cases:
         * 1) on Linux, mouse entered/moved events are not always delivered
         *    before mouse pressed event (see DTL-5956).
         * 2) while the mouse is immobile, fxomDocumentDidRefreshSceneGraph()
         *    method may have been invoked and reset this.glassGesture.
         * 
         * That is why we test this.glassGesture and manually invoke
         * mouseMovedOnGlassLayer() here.
         */
        if (glassGesture == null) {
            mouseMovedOnGlassLayer(e);
        }
        
        assert glassGesture != null;
        switch(e.getClickCount()) {
            case 1:
                if (e.getButton() == MouseButton.SECONDARY) {
                    // Update the selection (see spec detailed in DTL-5640)
                    final FXOMObject hitObject;
                    if (glassGesture == selectAndMoveGesture) {
                        hitObject = selectAndMoveGesture.getHitObject();
                    } else {
                        assert glassGesture == selectWithMarqueeGesture;
                        hitObject = selectWithMarqueeGesture.getHitObject();
                    }
                    final Selection selection
                            = contentPanelController.getEditorController().getSelection();
                    if (hitObject != null && selection.isSelected(hitObject) == false) {
                        selection.select(hitObject);
                    }
                    final ContextMenuController contextMenuController
                            = contentPanelController.getEditorController().getContextMenuController();
                    // The context menu items depend on the selection so
                    // we need to rebuild it each time it is invoked.
                    contextMenuController.updateContextMenuItems();
                } else {
                    activateGesture(glassGesture, e);
                }
                break;
            case 2:
                mouseDoubleClickedOnGlassLayer(e);
                break;
            default:
                // We ignore triple clicks and upper...
                break;
        }
        e.consume();
    }
    
    private void mouseDoubleClickedOnGlassLayer(MouseEvent e) {
        assert activeGesture == null;
        assert (glassGesture == selectAndMoveGesture) 
                || (glassGesture == selectWithMarqueeGesture);
        
        if (glassGesture == selectAndMoveGesture) {
            assert selectAndMoveGesture.getHitObject() instanceof FXOMInstance;
            final FXOMInstance hitObject
                    = (FXOMInstance) selectAndMoveGesture.getHitObject();
            final DesignHierarchyMask m
                    = new DesignHierarchyMask(hitObject);
            // Do not allow inline editing of the I18N value
            if (m.isResourceKey() == false) {
                handleInlineEditing((FXOMInstance) selectAndMoveGesture.getHitObject());
            } else {
                final MessageLog ml = contentPanelController.getEditorController().getMessageLog();
                ml.logWarningMessage("log.warning.inline.edit.internationalized.strings");
            }
        }
    }
    
    private void handleInlineEditing(FXOMInstance hitObject) {

        assert hitObject != null;
        assert inlineEditedObject == null;
        inlineEditedObject = hitObject;
        
        final AbstractDriver driver
                = contentPanelController.lookupDriver(inlineEditedObject);

        final Node inlineEditingBounds
                = driver.getInlineEditorBounds(inlineEditedObject);
        if (inlineEditingBounds != null) {
            final InlineEditController inlineEditController = 
                    contentPanelController.getEditorController().getInlineEditController();
            final DesignHierarchyMask m
                    = new DesignHierarchyMask(inlineEditedObject);
            final String text = m.getDescription();
            final InlineEditController.Type type;
            if (inlineEditingBounds instanceof TextArea
                    || DesignHierarchyMask.containsLineFeed(text)) {
                type = InlineEditController.Type.TEXT_AREA;
            } else {
                type = InlineEditController.Type.TEXT_FIELD;
            }
            final TextInputControl inlineEditor
                    = inlineEditController.createTextInputControl(
                            type, inlineEditingBounds, text);
            final Callback<String, Boolean> requestCommit
                    = new Callback<String, Boolean>() {
                        @Override
                        public Boolean call(String value) {
                            return inlineEditingDidRequestCommit(value);
                        }
                    };
            inlineEditController.startEditingSession(inlineEditor,
                    inlineEditingBounds, requestCommit);
        } else {
            System.out.println("Beep");
        }
    }
    
    
    private boolean inlineEditingDidRequestCommit(String newValue) {
        assert inlineEditedObject != null;
        
        final DesignHierarchyMask m 
                = new DesignHierarchyMask(inlineEditedObject);
        final PropertyName propertyName
                = m.getPropertyNameForDescription();
        assert propertyName != null;
        final ValuePropertyMetadata vpm
                = Metadata.getMetadata().queryValueProperty(inlineEditedObject, propertyName);
        final EditorController editorController
                = contentPanelController.getEditorController();
        final ModifyObjectJob job
                = new ModifyObjectJob(inlineEditedObject, vpm, newValue, editorController);
        
        if (job.isExecutable()) {
            editorController.getJobManager().push(job);
        }
        
        inlineEditedObject = null;
        
        return true;
    }
    
    private void keyPressedOnGlassLayer(KeyEvent e) {
        assert activeGesture == null : "activateGesture=" + activeGesture;
        switch(e.getCode()) {
            case UP:
            case DOWN:
            case LEFT:
            case RIGHT:
                if (RelocateSelectionJob.isSelectionMovable(contentPanelController.getEditorController())) {
                    activateGesture(new MoveWithKeyGesture(contentPanelController), e);
                } else {
                    System.out.println("Selection is not movable");
                }
                e.consume();
                break;
            default:
                // We let other key events flow up in the scene graph
                break;
        }
    }
    
    private void zoomStartedOnGlassLayer(ZoomEvent e) {
        activateGesture(zoomGesture, e);
        e.consume();
    }
    
    private void dragEnteredGlassLayer(DragEvent e) {
        activateGesture(new DragGesture(contentPanelController), e);
    }

    
    private void mousePressedOnHandleLayer(MouseEvent e) {
        assert e.getTarget() instanceof Node;
        
        final Node target = (Node) e.getTarget();
        Node hitNode = target;
        AbstractHandles<?> hitHandles = AbstractHandles.lookupHandles(hitNode);
        while ((hitHandles == null) && (hitNode.getParent() != null)) {
            hitNode = hitNode.getParent();
            hitHandles = AbstractHandles.lookupHandles(hitNode);
        }
        
        if (hitHandles != null) {
            activateGesture(hitHandles.findGesture(hitNode), e);
        } else {
            // Emergency code
            assert false : "event target has no HANDLES property :" + target;
        }
        e.consume();
    }
    
    private void mousePressedOnPringLayer(MouseEvent e) {
        assert e.getTarget() instanceof Node;
        
        final Node target = (Node) e.getTarget();
        Node hitNode = target;
        AbstractPring<?> hitPring = AbstractPring.lookupPring(target);
        while ((hitPring == null) && (hitNode.getParent() != null)) {
            hitNode = hitNode.getParent();
            hitPring = AbstractPring.lookupPring(hitNode);
        }
        
        if (hitPring != null) {
            activateGesture(hitPring.findGesture(hitNode), e);
        } else {
            // Emergency code
            assert false : "event target has no PRING property :" + target;
        }
        e.consume();
    }
    
    private void activateGesture(AbstractGesture gesture, InputEvent e) {
        assert activeGesture == null : "activeGesture=" + activeGesture;
        assert gesture != null;
        
        /*
         * Before activating the gesture, we check:
         *   - that there is a document attached to the editor controller
         *   - if a text session is on-going and can be completed cleanly. 
         * If not, we do not activate the gesture.
         */
        final EditorController editorController
                = contentPanelController.getEditorController();
        if ((editorController.getFxomDocument() != null) && editorController.canGetFxmlText()) {
            
            contentPanelController.beginInteraction();
            
            stopListeningToInputEvents();
            activeGesture = gesture;
            gesture.start(e, this);

            // Note that some gestures may terminates immediately.
            // So activeGesture may have switch back to null.
            assert (activeGesture == gesture) || (activeGesture == null);

            // Make sure that glass layer has keyboard focus
            contentPanelController.getGlassLayer().requestFocus();
        }
    }
    
}
