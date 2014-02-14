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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform.Theme;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.mode.AbstractModeController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.mode.EditModeController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.mode.PickModeController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.AbstractDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.FlowPaneDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.BorderPaneDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.GenericDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.GridPaneDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.HBoxDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.LineDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.SplitPaneDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TabDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TabPaneDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TableColumnDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TableViewDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TextFlowDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.ToolBarDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TreeTableColumnDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.TreeTableViewDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.VBoxDriver;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.AbstractHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.outline.NodeOutline;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.BoundsUnion;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.Picker;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.ScrollPaneBooster;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.editor.util.ContextMenuController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

/**
 * This class creates and controls the <b>Content Panel</b> of Scene Builder Kit.
 * 
 */
public class ContentPanelController extends AbstractFxmlPanelController 
                                    implements FXOMDocument.SceneGraphHolder {
    
    @FXML private ScrollPane scrollPane;
    @FXML private Pane workspacePane;
    @FXML private Rectangle extensionRect;
    @FXML private Label backgroundPane;
    @FXML private Group scalingGroup;
    @FXML private Group contentGroup;
    @FXML private Pane glassLayer;
    @FXML private Group outlineLayer;
    @FXML private Group pringLayer;
    @FXML private Group handleLayer;
    @FXML private Group rudderLayer;
    
    private boolean guidesVisible = true;
    private Paint pringColor = Color.rgb(238, 168, 47);
    private Paint guidesColor = Color.RED;
    
    private final WorkspaceController workspaceController
            = new WorkspaceController();
    private final HudWindowController hudWindowController
            = new HudWindowController();
    
    private final EditModeController editModeController;
    private final PickModeController pickModeController;
    private AbstractModeController currentModeController;
    
    private boolean tracingEvents; // For debugging purpose
    
    private final Picker picker = new Picker();
    private final List<NodeOutline> outlines = new ArrayList<>();
    
    /*
     * Public
     */
    
    /**
     * Creates a content panel controller for the specified editor controller.
     * 
     * @param editorController the editor controller (never null).
     */
    public ContentPanelController(EditorController editorController) {
        super(ContentPanelController.class.getResource("ContentPanel.fxml"), I18N.getBundle(), editorController); //NOI18N
        this.editModeController = new EditModeController(this);
        this.pickModeController = new PickModeController(this);
        
        editorController.getDragController().dragSourceProperty().addListener(new 
                ChangeListener<AbstractDragSource>() {
                    @Override
                    public void changed(ObservableValue<? extends AbstractDragSource> ov, AbstractDragSource t, AbstractDragSource t1) {
                        dragSourceDidChange();
                    }
                }
        );
        
        editorController.getDragController().dropTargetProperty().addListener(new 
                ChangeListener<AbstractDropTarget>() {
                    @Override
                    public void changed(ObservableValue<? extends AbstractDropTarget> ov, AbstractDropTarget t, AbstractDropTarget t1) {
                        dropTargetDidChange();
                    }
                }
        );
        
        editorController.themeProperty().addListener(new 
                ChangeListener<Theme>() {
                    @Override
                    public void changed(ObservableValue<? extends Theme> ov, Theme t, Theme t1) {
                        themeDidChange();
                    }
                }
        );
        
        editorController.sceneStyleSheetProperty().addListener(new 
                ListChangeListener<File>() {
                    @Override
                    public void onChanged(ListChangeListener.Change<? extends File> change) {
                        sceneStyleSheetsDidChange();
                    }
                }
        );
        editorController.pickModeEnabledProperty().addListener(new 
                ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                        pickModeDidChange();
                    }
                }
        );
    }

    /**
     * Returns true if this content panel displays outlines.
     * 
     * @return true if this content panel displays outlines.
     */
    public boolean isOutlinesVisible() {
        return (contentGroup != null) && (contentGroup.isVisible() == false);
    }

    /**
     * Enables or disables outline display in this content panel.
     * @param outlinesVisible true if outlines should be visible.
     */
    public void setOutlinesVisible(boolean outlinesVisible) {
        if (outlinesVisible != isOutlinesVisible()) {
            if (outlinesVisible) {
                beginShowingOutlines();
            } else {
                endShowingOutlines();
            }
        }
    }

    /**
     * Returns true if this content panel displays alignment guides.
     * 
     * @return true if this content panel displays alignment guides.
     */
    public boolean isGuidesVisible() {
        return guidesVisible;
    }

    /**
     * Enables or disables alignment guide display in this content panel.
     * 
     * @param guidesVisible  true if alignment guides should be visible.
     */
    public void setGuidesVisible(boolean guidesVisible) {
        this.guidesVisible = guidesVisible;
    }
    
    /**
     * Returns the color used by this content panel to draw parent rings.
     * 
     * @return the color used by this content panel to draw parent rings.
     */
    public Paint getPringColor() {
        return pringColor;
    }

    /**
     * Sets the color used by this content panel to draw parent rings.
     * 
     * @param pringColor the color used by this content panel to draw parent rings.
     */
    public void setPringColor(Paint pringColor) {
        this.pringColor = pringColor;
    }

    /**
     * Returns the color used by this content panel to draw alignment guides.
     * 
     * @return the color used by this content panel to draw alignment guides.
     */
    public Paint getGuidesColor() {
        return guidesColor;
    }

    /**
     * Sets the color used by this content panel to draw alignment guides.
     * 
     * @param guidesColor the color used by this content panel to draw alignment guides.
     */
    public void setGuidesColor(Paint guidesColor) {
        this.guidesColor = guidesColor;
    }
    
    /**
     * Return the scaling factor used by this content panel.
     * 
     * @return the scaling factor used by this content panel.
     */
    public double getScaling() {
        return workspaceController.getScaling();
    }
    
    /**
     * Sets the scaling factor to be used by this content panel.
     * 
     * @param scaling the scaling factor to be used by this content panel.
     */
    public void setScaling(double scaling) {
        this.workspaceController.setScaling(scaling);
    }
    
    
    /**
     * Returns true if this content panel automatically resize 3D content.
     * 
     * @return  true if this content panel automatically resize 3D content.
     */
    public boolean isAutoResize3DContent() {
        return workspaceController.isAutoResize3DContent();
    }

    /**
     * Enables or disables autoresizing of 3D content.
     * 
     * @param autoResize3DContent  true if this content panel should autoresize 3D content.
     */
    public void setAutoResize3DContent(boolean autoResize3DContent) {
        workspaceController.setAutoResize3DContent(autoResize3DContent);
    }
    
    
    /**
     * Scrolls this content panel so that the selected objects are visible.
     */
    public void scrollToSelection() {
        // Walk through the selected objects and computes the enclosing bounds.
        final BoundsUnion union = new BoundsUnion();
        final Selection selection = getEditorController().getSelection();
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
            for (FXOMObject i : osg.getItems()) {
                final DesignHierarchyMask mask = new DesignHierarchyMask(i);
                final FXOMObject nodeFxomObject = mask.getClosestFxNode();
                if (nodeFxomObject != null) {
                    final Node node = (Node) nodeFxomObject.getSceneGraphObject();
                    union.add(node.localToScene(node.getLayoutBounds()));
                }
            }
        }

        if (union.getResult() != null) {
            final Node content = scrollPane.getContent();
            final Bounds enclosing = content.sceneToLocal(union.getResult());
            final ScrollPaneBooster spb = new ScrollPaneBooster(scrollPane);
            spb.scrollTo(enclosing);
        }
    }

    
    public void reveal(FXOMObject targetFxomObject) {
        FXOMObject fxomObject = targetFxomObject;
        
        while (fxomObject != null) {
            final Object sceneGraphObject = fxomObject.getSceneGraphObject();
            
            if (sceneGraphObject instanceof Tab) {
                final Tab tab = (Tab) sceneGraphObject;
                final TabPane tabPane = tab.getTabPane();
                assert tabPane != null;
                tabPane.getSelectionModel().select(tab);
            } else if (sceneGraphObject instanceof TitledPane) {
                final TitledPane titledPane = (TitledPane) sceneGraphObject;
                if (titledPane.getParent() instanceof Accordion) {
                    final Accordion accordion = (Accordion) titledPane.getParent();
                    accordion.setExpandedPane(titledPane);
                }
            }
            
            DesignHierarchyMask mask = new DesignHierarchyMask(fxomObject);
            fxomObject = mask.getParentFXOMObject();
        }
    }
    
    /**
     * Returns the topmost FXOMObject at (sceneX, sceneY) in this content panel.
     * 
     * @param sceneX x coordinate of a scene point
     * @param sceneY y coordinate of a scene point
     * @return null or the topmost FXOMObject located at (sceneX, sceneY)
     */
    public FXOMObject pick(double sceneX, double sceneY) {
        return pick(sceneX, sceneY, Collections.emptySet());
    }
    
    
    /**
     * Returns the topmost FXOMObject at (sceneX, sceneY) but ignoring
     * objects from the exclude set.
     * 
     * @param sceneX x coordinate of a scene point
     * @param sceneY y coordinate of a scene point
     * @param excludes null or a set of FXOMObject to be excluded from the pick.
     * @return null or the topmost FXOMObject located at (sceneX, sceneY)
     */
    public FXOMObject pick(double sceneX, double sceneY, Set<FXOMObject> excludes) {
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        final FXOMObject result;
        if ((fxomDocument == null) 
                || (fxomDocument.getFxomRoot() == null)
                || excludes.contains(fxomDocument.getFxomRoot())) {
            result = null;
        } else {
            result = pick(fxomDocument.getFxomRoot(), sceneX, sceneY, excludes);
        }
        
        return result;
    }
    
    
    /**
     * Returns the topmost FXOMObject at (sceneX, sceneY) but ignoring
     * objects from the exclude set and starting the search from startObject.
     * 
     * @param startObject starting point of the search
     * @param sceneX x coordinate of a scene point
     * @param sceneY y coordinate of a scene point
     * @param excludes null or a set of FXOMObject to be excluded from the pick.
     * @return null or the topmost FXOMObject located at (sceneX, sceneY)
     */
    public FXOMObject pick(
            FXOMObject startObject, 
            double sceneX, 
            double sceneY,
            Set<FXOMObject> excludes) {
        
        final FXOMObject result;
        
        assert startObject != null;
        assert startObject.getFxomDocument() == getEditorController().getFxomDocument();
        assert excludes != null;
        assert excludes.contains(startObject) == false;
        
        if (startObject.getSceneGraphObject() instanceof Node) {
            picker.getExcludes().clear();
            for (FXOMObject exclude : excludes) {
                if (exclude.getSceneGraphObject() instanceof Node) {
                    picker.getExcludes().add((Node) exclude.getSceneGraphObject());
                }
            }

            final Node startNode = (Node) startObject.getSceneGraphObject();
            final List<Node> hitNodes = picker.pick(startNode, sceneX, sceneY);
            if (hitNodes == null) {
                result = null;
            } else {
                assert hitNodes.isEmpty() == false;
                
                FXOMObject hitObject = null;
                Node hitNode = null;
                final Iterator<Node> it = hitNodes.iterator();
                while ((hitObject == null) && it.hasNext()) {
                    hitNode = it.next();
                    hitObject = searchWithNode(hitNode, sceneX, sceneY);
                    if (excludes.contains(hitObject)) {
                        hitObject = null;
                        hitNode = null;
                    }
                }
                result = hitObject;
            }
            
        } else {
            result = null;
        }
        
        return result;
    }
    
    /**
     * Returns the FXOMObject which matches (sceneGraphNode, sceneX, sceneY).
     * 
     * @param sceneGraphNode scene graph node
     * @param sceneX x coordinate of a scene point
     * @param sceneY y coordinate of a scene point
     * @return an FXOMObject that matches (sceneGraphNode, sceneX, sceneY)
     */
    public FXOMObject searchWithNode(Node sceneGraphNode, double sceneX, double sceneY) {
       final FXOMObject result;
        
        final FXOMDocument fxomDocument 
                = getEditorController().getFxomDocument();
        final FXOMObject match
                = fxomDocument.searchWithSceneGraphObject(sceneGraphNode);
         /*
         * Refine the search.
         * With the logic above, a click in a 'tab header' returns the
         * fxom object associated to the 'tab pane'. We would like to get
         * the fxom object associated to the 'tab'. When the pick result is
         * a 'TabPane' we need to refine this result. This refinement logic
         * is available in AbstractDriver.
         */
        if (match != null) {
            final AbstractDriver driver = lookupDriver(match);
            result = driver.refinePick(sceneGraphNode, sceneX, sceneY, match);
        } else {
            result = null;
        }
        
        return result;
    }

    public boolean isTracingEvents() {
        return tracingEvents;
    }

    public void setTracingEvents(boolean tracingEvents) {
        if (this.tracingEvents != tracingEvents) {
            this.tracingEvents = tracingEvents;
            setupEventTracingFilter();
        }
    }
    
    public void layoutContent(boolean applyCSS) {
        workspaceController.layoutContent(applyCSS);
    }
    
    public void beginInteraction() {
        workspaceController.beginInteraction();
    }
    
    public void endInteraction() {
        workspaceController.endInteraction();
    }
    
    
    /*
     * Public which are *private*...
     */
    
    /**
     * @treatAsPrivate Returns the background object of this content panel.
     * @return the background object of this content panel.
     */
    public Pane getWorkspacePane() {
        return workspacePane;
    }
    
    
    /**
     * @treatAsPrivate Returns the glass layer container.
     * @return the glass layer container.
     */
    public Pane getGlassLayer() {
        return glassLayer;
    }
    
    
    /**
     * @treatAsPrivate Returns the outline layer container.
     * @return the outline layer container.
     */
    public Group getOutlineLayer() {
        return outlineLayer;
    }
    
    
    /**
     * @treatAsPrivate Returns the parent ring layer container.
     * @return the parent ring layer container.
     */
    public Group getPringLayer() {
        return pringLayer;
    }
    
    
    /**
     * @treatAsPrivate Returns the handle layer container.
     * @return the handle layer container.
     */
    public Group getHandleLayer() {
        return handleLayer;
    }

    /**
     * @treatAsPrivate Returns the rudder layer container.
     * @return the rudder layer container.
     */
    public Group getRudderLayer() {
        return rudderLayer;
    }

    
    /**
     * Computes the transform that projects from local coordinates of a 
     * scene graph object to the rudder layer local coordinates.
     * @param sceneGraphObject a scene graph object
     * @return transform from sceneGraphObject local coordinates to rudder local coordinates
     */
    public Transform computeSceneGraphToRudderLayerTransform(Node sceneGraphObject) {
        assert sceneGraphObject != null;
        assert sceneGraphObject.getScene() == rudderLayer.getScene();
        
        final Transform t1 = sceneGraphObject.getLocalToSceneTransform();
        final Transform t2 = rudderLayer.getLocalToSceneTransform();
        final Transform result;
        
        try {
            final Transform i2 = t2.createInverse();
            result = i2.createConcatenation(t1);
        } catch(NonInvertibleTransformException x) {
            throw new RuntimeException(x);
        }
        
        return result;
    }
    
    
    /**
     * @treatAsPrivate Returns the hud window controller.
     * @return the hud window controller.
     */
    public HudWindowController getHudWindowController() {
        return hudWindowController;
    }
    
    /**
     * @treatAsPrivate Returns true if pick mode is enabled.
     * @return true if pick mode is enabled.
     */
    public boolean isPickModeEnabled() {
        return currentModeController == pickModeController;
    }
    
    /**
     * @treatAsPrivate Returns the handles associated an fxom object.
     * Returns null if the fxom object is currently not selected or
     * if content panel is not in 'edit mode'.
     * @param fxomObject an fxom object
     * @return null or the associated handles
     */
    public AbstractHandles<?> lookupHandles(FXOMObject fxomObject) {
        final AbstractHandles<?> result;
        
        if (currentModeController != editModeController) {
            result = null;
        } else {
            result = editModeController.lookupHandles(fxomObject);
        }
        
        return result;
    }
    
    /**
     * @treatAsPrivate
     * Returns true if this content panel is able to display the content ie
     * 1) fxomDocument != null
     * 2) fxomDocument.getFxomRoot() != null
     * 3) fxomDocument.getFxomRoot().isNode()
     * 
     * @return true if this content panel is able to display the content
     */
    public boolean isContentDisplayable() {
        final boolean result;
        
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        if ((fxomDocument == null) || (fxomDocument.getFxomRoot() == null)) {
            result = false;
        } else {
            result = fxomDocument.getFxomRoot().isNode();
        }
        
        return result;
    }
    
    /*
     * AbstractPanelController<TreeView>
     */

    /**
     * @treatAsPrivate fxom document has changed
     * @param oldDocument old fxom document
     */
    @Override
    protected void fxomDocumentDidChange(FXOMDocument oldDocument) {
        if (oldDocument != null) {
            assert oldDocument.getSceneGraphHolder() == this;
            oldDocument.endHoldingSceneGraph();
        }
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        if (fxomDocument != null) {
            assert fxomDocument.getSceneGraphHolder() == null;
            fxomDocument.beginHoldingSceneGraph(this);
        }
        
        workspaceController.setFxomDocument(fxomDocument);
        if (isOutlinesVisible()) {
            updateOutlines();
        }
        if (currentModeController != null) {
            currentModeController.fxomDocumentDidChange(oldDocument);
        }

        resetViewport();
    }

    /**
     * @treatAsPrivate user scene graph has changed
     */
    @Override
    protected void sceneGraphRevisionDidChange() {
        // Everything is done by fxomDocumentDidRefreshSceneGraph().
        // Nothing to do here.
    }

    /**
     * @treatAsPrivate
     */
    @Override
    protected void cssRevisionDidChange() {
        // Nothing to do here.
    }

    /**
     * @treatAsPrivate job manager revision has changed
     */
    @Override
    protected void jobManagerRevisionDidChange() {
        getEditorController().setPickModeEnabled(false);
        fxomDocumentDidRefreshSceneGraph(getEditorController().getFxomDocument());
    }

    /**
     * @treatAsPrivate selection has changed
     */
    @Override
    protected void editorSelectionDidChange() {
        if (currentModeController != null) {
            currentModeController.editorSelectionDidChange();
        }
    }

    /*
     * AbstractFxmlPanelController
     */
   
    /**
     * @treatAsPrivate controller did load fxml
     */
    @Override
    protected void controllerDidLoadFxml() {
        
        // Sanity checks
        assert scrollPane != null;
        assert workspacePane != null;
        assert workspacePane.getPrefWidth() == Region.USE_COMPUTED_SIZE;
        assert workspacePane.getPrefHeight() == Region.USE_COMPUTED_SIZE;
        assert workspacePane.getMaxWidth() == Double.MAX_VALUE;
        assert workspacePane.getMaxHeight() == Double.MAX_VALUE;
        assert workspacePane.getMinWidth() == Region.USE_PREF_SIZE;
        assert workspacePane.getMinHeight() == Region.USE_PREF_SIZE;
        assert extensionRect != null;
        assert extensionRect.getLayoutX() == 0.0;
        assert extensionRect.getLayoutY() == 0.0;
        assert backgroundPane != null;
        assert backgroundPane.getLayoutX() == 0.0;
        assert backgroundPane.getLayoutY() == 0.0;
        assert backgroundPane.getMaxWidth() == Region.USE_PREF_SIZE;
        assert backgroundPane.getMaxHeight() == Region.USE_PREF_SIZE;
        assert backgroundPane.getMinWidth() == Region.USE_PREF_SIZE;
        assert backgroundPane.getMinHeight() == Region.USE_PREF_SIZE;
        assert scalingGroup != null;
        assert contentGroup != null;
        assert contentGroup.getLayoutX() == 0.0;
        assert contentGroup.getLayoutY() == 0.0;
        assert contentGroup.getParent() == scalingGroup;
        assert glassLayer != null;
        assert glassLayer.isMouseTransparent() == false;
        assert glassLayer.isFocusTraversable();
        assert outlineLayer != null;
        assert outlineLayer.isMouseTransparent();
        assert outlineLayer.isFocusTraversable() == false;
        assert pringLayer != null;
        assert pringLayer.isMouseTransparent() == false;
        assert pringLayer.isFocusTraversable() == false;
        assert handleLayer != null;
        assert handleLayer.isMouseTransparent() == false;
        assert handleLayer.isFocusTraversable() == false;
        assert rudderLayer != null;
        assert rudderLayer.isMouseTransparent() == true;
        assert rudderLayer.isFocusTraversable() == false;
        
        outlineLayer.setManaged(false);
        pringLayer.setManaged(false);
        handleLayer.setManaged(false);
        rudderLayer.setManaged(false);
        
        // Replace plain group in "contentGroup" by a custom one
        // which isolates the user scene graph from SB owned styling.
        installStylingIsolationGroup();
        
        // Remove fake content used to help design
        backgroundPane.setText(""); //NOI18N
        
        // Setup our workspace controller
        workspaceController.panelControllerDidLoadFxml(
                scrollPane,
                scalingGroup, 
                contentGroup, 
                backgroundPane, 
                extensionRect);
        themeDidChange(); // To setup initial value of WorkspaceController.themeStyleSheet
        

        // Setup the mode controller
        pickModeDidChange();

        resetViewport();
        setupEventTracingFilter();
        
        // Setup the context menu
        final ContextMenuController contextMenuController
                = getEditorController().getContextMenuController();
        scrollPane.setContextMenu(contextMenuController.getContextMenu());
    }
    
    /*
     * FXOMDocument.SceneGraphHolder
     */
    
    /**
     * @treatAsPrivate fxom document will reconstruct the user scene graph
     */
    @Override
    public void fxomDocumentWillRefreshSceneGraph(FXOMDocument fxomDocument) {
        // Nothing special to do 
    }

    /**
     * @treatAsPrivate fxom document did reconstruct the user scene graph
     */
    @Override
    public void fxomDocumentDidRefreshSceneGraph(FXOMDocument fxomDocument) {
        // Scene graph has been reconstructed so:
        //  - new scene graph must replace the old one below contentHook
        //  - mode controller must be informed so that it can updates handles
        workspaceController.sceneGraphDidChange();
        if (isOutlinesVisible()) {
            updateOutlines();
        }
        if (currentModeController != null) {
            currentModeController.fxomDocumentDidRefreshSceneGraph();
        }
   }
    
    
    /*
     * Private
     */
    
    private void changeModeController(AbstractModeController nextModeController) {
        assert nextModeController != currentModeController;
        assert nextModeController != null;
        
        if (currentModeController != null) {
            currentModeController.willResignActive(nextModeController);
        }
        final AbstractModeController previousModeController = currentModeController;
        currentModeController = nextModeController;
        currentModeController.didBecomeActive(previousModeController);
    }
    
    /**
     * @treatAsPrivate lookup the driver adapted to an fxom object
     * @param fxomObject an fxom object (never null)
     * @return null or the driver adapted to fxomObject
     */
    public AbstractDriver lookupDriver(FXOMObject fxomObject) {
        final Object sceneGraphObject = fxomObject.getSceneGraphObject();
        final AbstractDriver result;
        
        if (sceneGraphObject instanceof HBox) {
            result = new HBoxDriver(this);
        } else if (sceneGraphObject instanceof VBox) {
            result = new VBoxDriver(this);
        } else if (sceneGraphObject instanceof GridPane) {
            result = new GridPaneDriver(this);
        } else if (sceneGraphObject instanceof BorderPane) {
            result = new BorderPaneDriver(this);
        } else if (sceneGraphObject instanceof Line) {
            result = new LineDriver(this);
        } else if (sceneGraphObject instanceof FlowPane) {
            result = new FlowPaneDriver(this);
        } else if (sceneGraphObject instanceof TextFlow) {
            result = new TextFlowDriver(this);
        } else if (sceneGraphObject instanceof ToolBar) {
            result = new ToolBarDriver(this);
        } else if (sceneGraphObject instanceof SplitPane) {
            result = new SplitPaneDriver(this);
        } else if (sceneGraphObject instanceof Tab) {
            result = new TabDriver(this);
        } else if (sceneGraphObject instanceof TabPane) {
            result = new TabPaneDriver(this);
        } else if (sceneGraphObject instanceof TableView) {
            result = new TableViewDriver(this);
        } else if (sceneGraphObject instanceof TableColumn) {
            result = new TableColumnDriver(this);
        } else if (sceneGraphObject instanceof TreeTableView) {
            result = new TreeTableViewDriver(this);
        } else if (sceneGraphObject instanceof TreeTableColumn) {
            result = new TreeTableColumnDriver(this);
        } else if (sceneGraphObject instanceof Node) {
            result = new GenericDriver(this);
        } else {
            result = null;
        }
        
        return result;
    }
    
    
    private void resetViewport() {
        if (scrollPane != null) {
            scrollPane.setHvalue(0.5);
            scrollPane.setVvalue(0.5);
        }
    }
    
    private void setupEventTracingFilter() {
        if (glassLayer != null) {
            if (tracingEvents) {
                glassLayer.addEventFilter(InputEvent.ANY, eventTracingFiler);
            } else {
                glassLayer.removeEventFilter(InputEvent.ANY, eventTracingFiler);
            }
        }
    }
    
    private void traceEvent(Event e) {
        final StringBuilder sb = new StringBuilder();
        
        sb.append("ContentPanelController: eventType="); //NOI18N
        sb.append(e.getEventType());
        sb.append(", target="); //NOI18N
        sb.append(e.getTarget());
        if (e instanceof KeyEvent) {
            final KeyEvent ke = (KeyEvent) e;
            sb.append(", keyCode="); //NOI18N
            sb.append(ke.getCode());
        }
        
        System.out.println(sb.toString());
    }
    
    private final EventHandler<Event> eventTracingFiler
            = new EventHandler<Event>() {
        @Override
        public void handle(Event e) {
            traceEvent(e);
        }
    };
    
    
    private void dragSourceDidChange() {
        getEditorController().setPickModeEnabled(false);
    }
    
    
    private void dropTargetDidChange() {
        if (currentModeController != null) {
            currentModeController.dropTargetDidChange();
        }
    }
    
    
    private void themeDidChange() {
        if (contentGroup != null) {
            final EditorPlatform.Theme theme = getEditorController().getTheme();
            final List<URL> themeStyleSheets = EditorPlatform.getThemeStylesheetURLs(theme);
            workspaceController.setThemeStyleSheets(themeStyleSheets);
        }
    }
    
    
    private void sceneStyleSheetsDidChange() {
        if (contentGroup != null) {
            final List<File> sceneStyleSheets = getEditorController().getSceneStyleSheets();
            final List<String> sceneStyleSheetURLs = new ArrayList<>();
            for (File f : sceneStyleSheets) {
                sceneStyleSheetURLs.add(f.toURI().toString());
            }
            workspaceController.setPreviewStyleSheets(sceneStyleSheetURLs);
        }
    }
    
    private void pickModeDidChange() {
        final AbstractModeController newModeController;
        if (getEditorController().isPickModeEnabled()) {
            newModeController = pickModeController;
        } else {
            newModeController = editModeController;
        }
        changeModeController(newModeController);
    }
    
    private void installStylingIsolationGroup() {
        assert contentGroup.getParent() == scalingGroup;
        
        /*
         * To isolate user content styling from SB's own styling we 
         * insert two custom groups between scalingGroup and contentGroup:
         * 
         * 1) original layout loaded from FXML:
         * 
         *      ...
         *          scalingGroup
         *              contentGroup
         *                  ... (user content)
         * 
         * 2) layout with isolation groups:
         * 
         *      ... 
         *          scalingGroup
         *              isolationGroupA     # impl_getAllParentStylesheets() overriden
         *                  isolationGroupB     # getStyleableParent() overriden 
         *                      contentGroup
         *                          ... (user content)
         *
         * isolationGroupA prevents styling from SB to apply to user content
         * isolationGroupB enables user content to have its own theme (modena, caspian...)
         */
        
        final Group isolationGroupA = Deprecation.makeStylingIsolationGroupA();
        final Group isolationGroupB = Deprecation.makeStylingIsolationGroupB();

        final int contentGroupIndex = scalingGroup.getChildren().indexOf(contentGroup);
        assert contentGroupIndex != -1;
        scalingGroup.getChildren().remove(contentGroup);
        scalingGroup.getChildren().add(contentGroupIndex, isolationGroupA);
        isolationGroupA.getChildren().add(isolationGroupB);
        isolationGroupB.getChildren().add(contentGroup);
    }
    
    
    /*
     * Private (outline layer)
     */
    
    private void beginShowingOutlines() {
        assert contentGroup.isVisible();
        
        contentGroup.setVisible(false);
        updateOutlines();
    }
    
    private void endShowingOutlines() {
        assert contentGroup.isVisible() == false;

        final List<Node> outlineNodes = outlineLayer.getChildren();
        for (NodeOutline o : outlines) {
            assert outlineNodes.contains(o.getRootNode());
            outlineNodes.remove(o.getRootNode());
        }
        outlines.clear();
        contentGroup.setVisible(true);
    }
    
    private void updateOutlines() {
        assert isOutlinesVisible();
        
        // Collects fxom objects associated to a node in the fxom document
        final List<FXOMObject> allNodes = collectNodes();
        
        for (int i = 0, count = allNodes.size(); i < count; i++) {
            assert allNodes.get(i) instanceof FXOMInstance;
            final FXOMInstance nodeInstance = (FXOMInstance) allNodes.get(i);
            if (i < outlines.size()) {
                final NodeOutline currentOutline = outlines.get(i);
                if (currentOutline.getFxomObject() != nodeInstance) {
                    replaceOutline(i, nodeInstance);
               } else {
                    switch(currentOutline.getState()) {
                        case CLEAN:
                            break;
                        case NEEDS_RECONCILE:
                            // scene graph associated to currentOutline has changed but h is still compatible
                            currentOutline.reconcile();
                            break;
                        case NEEDS_REPLACE:
                            // currentOutline is no longer compatible with the new scene graph object 
                            replaceOutline(i, nodeInstance);
                            break;
                    }
                }
            } else {
                addOutline(outlines.size(), nodeInstance);
            }
        }
        for (int i = allNodes.size(), count = outlines.size(); i < count; i++) {
            removeOutline(allNodes.size());
        }
        assert outlines.size() == allNodes.size();
    }
    
    private void addOutline(int i, FXOMInstance nodeInstance) {
        assert outlines.size() == outlineLayer.getChildren().size();
        
        final NodeOutline newOutline = new NodeOutline(this, nodeInstance);
        outlines.add(i, newOutline);
        outlineLayer.getChildren().add(i, newOutline.getRootNode());
        
        assert outlines.size() == outlineLayer.getChildren().size();
        assert outlines.get(i).getRootNode() == outlineLayer.getChildren().get(i);
    }
    
    private void replaceOutline(int i, FXOMInstance nodeInstance) {
        removeOutline(i);
        addOutline(i, nodeInstance);
    }
    
    
    private void removeOutline(int i) {
        assert outlines.size() == outlineLayer.getChildren().size();
        assert outlines.get(i).getRootNode() == outlineLayer.getChildren().get(i);
        
        outlines.remove(i);
        outlineLayer.getChildren().remove(i);
        
        assert outlines.size() == outlineLayer.getChildren().size();
    }
    
    private List<FXOMObject> collectNodes() {
        final List<FXOMObject> result = new ArrayList<>();
        
        final List<FXOMObject> candidates = new ArrayList<>();
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();
        if ((fxomDocument != null) && (fxomDocument.getFxomRoot() != null)) {
            candidates.add(fxomDocument.getFxomRoot());
        } 
        
        while (candidates.isEmpty() == false) {
            final FXOMObject candidate = candidates.get(0);
            candidates.remove(0);
            if (candidate.isNode()) {
                final Node sgo = (Node) candidate.getSceneGraphObject();
                if (sgo.getScene() == getPanelRoot().getScene()) {
                    result.add(candidate);
                }
            }
            final DesignHierarchyMask m = new DesignHierarchyMask(candidate);
            if (m.isAcceptingSubComponent()) {
                for (int i = 0, c = m.getSubComponentCount(); i < c; i++) {
                    final FXOMObject subComponent = m.getSubComponentAtIndex(i);
                    candidates.add(subComponent);
                }
            }
            for (DesignHierarchyMask.Accessory a : DesignHierarchyMask.Accessory.values()) {
                if (m.isAcceptingAccessory(a)) {
                    final FXOMObject accessoryObject = m.getAccessory(a);
                    if ((accessoryObject != null) && accessoryObject.isNode()) {
                        candidates.add(accessoryObject);
                    }
                }
            }
        }
        
        return result;
    }
}
