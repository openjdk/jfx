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
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.DragController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.DocumentDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.ExternalDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.editor.util.ContextMenuController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Cell;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TreeItem;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Window;

/**
 * This class creates and controls the <b>Hierarchy Panel</b> of Scene Builder
 * Kit.
 *
 *
 */
public abstract class AbstractHierarchyPanelController extends AbstractFxmlPanelController {

    public enum BorderSide {

        BOTTOM, RIGHT_BOTTOM_LEFT, RIGHT_LEFT, TOP_RIGHT_BOTTOM_LEFT, TOP_RIGHT_LEFT
    }

    // Style classes used for the TreeCell graphic part
    /**
     * @treatAsPrivate
     */
    public static final String TREE_CELL_GRAPHIC = "tree-cell-graphic";
    /**
     * @treatAsPrivate
     */
    public static final String HIERARCHY_READONLY_LABEL = "hierarchy-readonly-label";
    /**
     * @treatAsPrivate
     */
    public static final String HIERARCHY_READWRITE_LABEL = "hierarchy-readwrite-label";

    private final HierarchyDNDController dndController = new HierarchyDNDController(this);
    private final HierarchyAnimationScheduler animationScheduler = new HierarchyAnimationScheduler();
    private final ObjectProperty<DisplayOption> displayOptionProperty
            = new SimpleObjectProperty<>(DisplayOption.INFO);
    /**
     * @treatAsPrivate
     */
    protected TreeItem<HierarchyItem> rootTreeItem;
    private boolean parentRingEnabled = true;
    private Paint parentRingColor;
    private final Map<FXOMObject, Boolean> treeItemsExpandedMapProperty = new HashMap<>();
    private boolean shouldEndOnExit;
    protected final Label promptLabel = new Label();

    // When DND few pixels of the top or bottom of the Hierarchy 
    // the user can cause it to auto-scroll until the desired target node
    private static final double AUTO_SCROLLING_ZONE_HEIGHT = 40.0;

    private Border bottomBorder;
    private Border rightBottomLeftBorder;
    private Border rightLeftBorder;
    private Border topRightBottomLeftBorder;
    private Border topRightLeftBorder;
    
    private static final Color DEFAULT_PARENT_RING_COLOR = Color.rgb(238, 168, 47);
    private final BorderWidths borderWidths = new BorderWidths(2);
    private final Paint transparent = Color.TRANSPARENT;

    /**
     * @treatAsPrivate
     */
    protected final ListChangeListener<TreeItem<HierarchyItem>> treeItemSelectionListener = new ListChangeListener<TreeItem<HierarchyItem>>() {
        @Override
        public void onChanged(ListChangeListener.Change<? extends TreeItem<HierarchyItem>> change) {
            treeItemSelectionDidChange();
        }
    };

    /**
     * Used to define the type of information displayed in the hierarchy.
     */
    public enum DisplayOption {

        INFO {

                    @Override
                    public String toString() {
                        return I18N.getString("hierarchy.displayoption.info");
                    }
                },
        FXID {

                    @Override
                    public String toString() {
                        return I18N.getString("hierarchy.displayoption.fxid");
                    }
                },
        NODEID {

                    @Override
                    public String toString() {
                        return I18N.getString("hierarchy.displayoption.nodeid");
                    }
                }
    }

    /*
     * Public
     */
    public AbstractHierarchyPanelController(URL fxmlURL, EditorController editorController) {
        super(fxmlURL, I18N.getBundle(), editorController);
        promptLabel.getStyleClass().add("hierarchy-prompt-label");
        promptLabel.setMouseTransparent(true);
    }

    /**
     * Returns the root TreeItem.
     *
     * @return the root TreeItem.
     */
    public final TreeItem<HierarchyItem> getRoot() {
        return rootTreeItem;
    }

    /**
     * Returns the display option property.
     *
     * @return the display option property.
     */
    public ObservableValue<DisplayOption> displayOptionProperty() {
        return displayOptionProperty;
    }

    /**
     * Returns the type of information displayed in the hierarchy.
     *
     * @return the type of information displayed in the hierarchy.
     */
    public final DisplayOption getDisplayOption() {
        return displayOptionProperty.getValue();
    }

    /**
     * Sets the type of information displayed in the hierarchy.
     *
     * @param displayOption the type of information displayed in the hierarchy.
     */
    public final void setDisplayOption(DisplayOption displayOption) {
        displayOptionProperty.setValue(displayOption);
    }

    /**
     * @return the DND controller
     * @treatAsPrivate
     */
    public final HierarchyDNDController getDNDController() {
        return dndController;
    }

    /**
     * @return true if the timeline is running
     * @treatAsPrivate
     */
    public final boolean isTimelineRunning() {
        return animationScheduler.isTimelineRunning();
    }

    /**
     * @return true if the parent ring is enabled
     * @treatAsPrivate
     */
    public boolean isParentRingEnabled() {
        return parentRingEnabled;
    }

    /**
     * @param enabled the enabled value
     * @treatAsPrivate
     */
    public void setParentRingEnabled(boolean enabled) {
        parentRingEnabled = enabled;
    }

    public Paint getParentRingColor() {
        return parentRingColor;
    }

    public void setParentRingColor(Paint value) {
        parentRingColor = value;
        updateParentRingColor();
    }

    public Border getBorder(BorderSide side) {
        final Border border;
        switch (side) {
            case BOTTOM:
                border = bottomBorder;
                break;
            case RIGHT_BOTTOM_LEFT:
                border = rightBottomLeftBorder;
                break;
            case RIGHT_LEFT:
                border = rightLeftBorder;
                break;
            case TOP_RIGHT_BOTTOM_LEFT:
                border = topRightBottomLeftBorder;
                break;
            case TOP_RIGHT_LEFT:
                border = topRightLeftBorder;
                break;
            default:
                border = null;
                assert false;
                break;
        }
        assert border != null;
        return border;
    }

    /**
     * Returns the control used to represent the hierarchy. Either a TreeView or
     * a TreeTableView.
     *
     * @return the control used to represent the hierarchy.
     */
    public abstract Control getPanelControl();

    /**
     * Returns the selected TreeItems.
     *
     * @return the selected TreeItems.
     */
    public abstract ObservableList<TreeItem<HierarchyItem>> getSelectedItems();

    /**
     * Returns the panel control scrollbar for the specified orientation.
     *
     * @param orientation the scrollbar orientation
     * @return the panel control scrollbar for the specified orientation
     * @treatAsPrivate
     */
    public ScrollBar getScrollBar(final Orientation orientation) {
        final Control panelControl = getPanelControl();
        final Set<Node> scrollBars = panelControl.lookupAll(".scroll-bar"); //NOI18N
        for (Node node : scrollBars) {
            if (node instanceof ScrollBar) {
                final ScrollBar scrollBar = (ScrollBar) node;
                if (scrollBar.getOrientation() == orientation) {
                    return scrollBar;
                }
            }
        }
        return null;
    }

    /**
     * @treatAsPrivate
     */
    protected abstract void startEditingDisplayInfo();

    /**
     * @treatAsPrivate
     */
    protected abstract void updatePanel();

    /**
     * @treatAsPrivate
     */
    protected abstract void clearSelection();

    /**
     * @param treeItem the TreeItem
     * @treatAsPrivate
     */
    protected abstract void select(final TreeItem<HierarchyItem> treeItem);

    /**
     * @param treeItems the TreeItems
     * @treatAsPrivate
     */
    protected void select(final List<TreeItem<HierarchyItem>> treeItems) {
        for (TreeItem<HierarchyItem> treeItem : treeItems) {
            select(treeItem);
        }
    }

    /**
     * @param treeItem the TreeItem
     * @treatAsPrivate
     */
    public abstract void scrollTo(final TreeItem<HierarchyItem> treeItem);

    /**
     * @param treeItem the TreeItem
     * @return true if visible
     * @treatAsPrivate
     */
    protected boolean isVisible(final TreeItem<HierarchyItem> treeItem) {
        final Cell<?> cell = getCell(treeItem);
        return (cell == null ? false : cell.isVisible());
    }

    /**
     * @param treeItem the TreeItem
     * @return the cell corresponding to the specified TreeItem
     * @treatAsPrivate
     */
    public abstract Cell<?> getCell(final TreeItem<?> treeItem);

    /**
     * Returns the Y coordinate of the panel content TOP. Used to define the
     * zone for auto scrolling.
     *
     * @return the Y coordinate of the panel content TOP
     * @treatAsPrivate
     */
    public abstract double getContentTopY();

    /**
     * Returns the Y coordinate of the panel content BOTTOM. Used to define the
     * zone for auto scrolling.
     *
     * @return the Y coordinate of the panel content BOTTOM
     * @treatAsPrivate
     */
    public abstract double getContentBottomY();

    /**
     * @treatAsPrivate
     */
    public abstract void updateParentRing();

    /**
     * @treatAsPrivate
     */
    public abstract void updatePlaceHolder();

    /**
     * @treatAsPrivate
     */
    public abstract void clearBorderColor();

    /**
     * @param cell the cell
     * @treatAsPrivate
     */
    public void clearBorderColor(final Cell<?> cell) {
        assert cell != null;
        cell.setBorder(Border.EMPTY);
    }

    /**
     * @param node the node
     * @treatAsPrivate
     */
    public void addToPanelControlSkin(final Node node) {
        final Skin<?> skin = getPanelControl().getSkin();
        assert skin instanceof SkinBase;
        final SkinBase<?> skinbase = (SkinBase) skin;
        skinbase.getChildren().add(node);
    }

    /**
     * @param node the node
     * @treatAsPrivate
     */
    public void removeFromPanelControlSkin(final Node node) {
        final Skin<?> skin = getPanelControl().getSkin();
        assert skin instanceof SkinBase;
        final SkinBase<?> skinbase = (SkinBase) skin;
        skinbase.getChildren().remove(node);
    }

    /**
     * @treatAsPrivate
     */
    protected abstract void startListeningToTreeItemSelection();

    /**
     * @treatAsPrivate
     */
    protected abstract void stopListeningToTreeItemSelection();

    /*
     * AbstractPanelController
     * @treatAsPrivate
     */
    @Override
    protected void fxomDocumentDidChange(FXOMDocument oldDocument) {
        // Clear the map containing the TreeItems expanded property values
        treeItemsExpandedMapProperty.clear();
        updatePanel();
    }

    /**
     * @treatAsPrivate
     */
    @Override
    protected void sceneGraphRevisionDidChange() {
        if (getPanelControl() != null) {
            // Update the map containing the TreeItems expanded property values
            // This map will be used after rebuilding the tree, 
            // in order to update the TreeItems expanded property to their previous value
            if (rootTreeItem != null) { // Root TreeItem may be null
                updateTreeItemsExpandedMap(rootTreeItem);
            }
            // FXOM document has rebuilt the scene graph. Tree items must all
            // be updated because:
            //  - classes of scene graph objects may have mutated
            //  - infos displayed in the tree items may be obsoletes
            updatePanel();
            editorSelectionDidChange();
        }
    }

    /**
     * @treatAsPrivate
     */
    @Override
    protected void cssRevisionDidChange() {
        // Ignored
    }

    private void updateTreeItemsExpandedMap(TreeItem<HierarchyItem> treeItem) {
        assert treeItem != null;
        final HierarchyItem item = treeItem.getValue();
        if (!item.isEmpty()) {
            final FXOMObject fxomObject = item.getFxomObject();
            assert fxomObject != null;
            treeItemsExpandedMapProperty.put(fxomObject, treeItem.isExpanded());
            // Inspect TreeItem chidren
            for (TreeItem<HierarchyItem> treeItemChild : treeItem.getChildren()) {
                updateTreeItemsExpandedMap(treeItemChild);
            }
        }
    }

    /**
     * @treatAsPrivate
     */
    @Override
    protected void jobManagerRevisionDidChange() {
        // FXOMDocument has been modified by a job.
        // Tree items must all be updated.
        sceneGraphRevisionDidChange();
    }

    /**
     * @treatAsPrivate
     */
    @Override
    protected void controllerDidLoadFxml() {
        assert getPanelControl() != null;

        // Initialize and configure the hierarchy panel
        initializePanel();

        // Add listener on the selection
        // Used to update global selection + update parent ring
        startListeningToTreeItemSelection();

        // Populate panel
        updatePanel();
    }

    /**
     * @treatAsPrivate
     */
    @Override
    protected void editorSelectionDidChange() {
        final Selection selection = getEditorController().getSelection();
        final List<FXOMObject> selectedFxomObjects = new ArrayList<>();

        if (getPanelControl() != null) {
            if (selection.getGroup() instanceof ObjectSelectionGroup) {
                final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
                selectedFxomObjects.addAll(osg.getItems());
            } else if (selection.getGroup() instanceof GridSelectionGroup) {
                final GridSelectionGroup gsg = (GridSelectionGroup) selection.getGroup();
                selectedFxomObjects.add(gsg.getParentObject());
            }

            // Update selected items
            stopListeningToTreeItemSelection();
            clearSelection();
            // Root TreeItem may be null
            if (getRoot() != null && selectedFxomObjects.isEmpty() == false) {
                final List<TreeItem<HierarchyItem>> selectedTreeItems
                        = lookupTreeItem(selectedFxomObjects);
                if (selectedTreeItems.isEmpty() == false) {
                    select(selectedTreeItems);
                    // Scroll to the last TreeItem
                    final TreeItem<HierarchyItem> lastTreeItem
                            = selectedTreeItems.get(selectedTreeItems.size() - 1);
                    // Call scrollTo only if the item is not visible.
                    // This avoid unexpected scrolling to occur in the hierarchy 
                    // TreeView / TreeTableView while changing some property in the inspector.
                    if (isVisible(lastTreeItem) == false) {
                        scrollTo(lastTreeItem);
                    }
                }
            }
            startListeningToTreeItemSelection();

            // Update parent ring when selection did change
            updateParentRing();
        }
    }

    private void treeItemSelectionDidChange() {

        /*
         * Before updating the selection, we test if a text session is on-going
         * and can be completed cleanly. If not, we do not update the selection.
         */
        if (getEditorController().canGetFxmlText()) {
            final Set<FXOMObject> selectedFxomObjects = new HashSet<>();
            for (TreeItem<HierarchyItem> selectedItem : getSelectedItems()) {
                // TreeItems may be null when selection is updating
                if (selectedItem != null) {
                    final FXOMObject fxomObject = selectedItem.getValue().getFxomObject();
                    // Placeholders may have a null fxom object
                    if (fxomObject != null) {
                        selectedFxomObjects.add(fxomObject);
                    }
                }
            }

            // Update selection
            stopListeningToEditorSelection();
            getEditorController().getSelection().select(selectedFxomObjects);
            startListeningToEditorSelection();

            // Update parent ring when selection did change
            updateParentRing();
        } /*
         * If a text session is on-going and cannot be completed cleanly,
         * we go back to previous TreeItem selection.
         */ else {
            editorSelectionDidChange();
        }
    }

    private TreeItem<HierarchyItem> makeTreeItem(final FXOMObject fxomObject) {
        final HierarchyItem item = new HierarchyItem(fxomObject);
        final TreeItem<HierarchyItem> treeItem = new TreeItem<>(item);
        // Set back the TreeItem expanded property if any
        Boolean expanded = treeItemsExpandedMapProperty.get(fxomObject);
        if (expanded != null) {
            treeItem.setExpanded(expanded);
        }
        updateTreeItem(treeItem);
        return treeItem;
    }

    private TreeItem<HierarchyItem> makeTreeItemBorderPane(
            final DesignHierarchyMask owner,
            final FXOMObject fxomObject,
            final Accessory accessory) {
        final HierarchyItemBorderPane item
                = new HierarchyItemBorderPane(owner, fxomObject, getDisplayOption(), accessory);
        final TreeItem<HierarchyItem> treeItem = new TreeItem<>(item);
        // Set back the TreeItem expanded property if any
        Boolean expanded = treeItemsExpandedMapProperty.get(fxomObject);
        if (expanded != null) {
            treeItem.setExpanded(expanded);
        }
        // Mask may be null for empty place holder
        if (item.getMask() != null) {
            updateTreeItem(treeItem);
        }
        return treeItem;
    }

    /**
     * @param owner the mask owner
     * @param fxomObject the FXOMObject
     * @return the new TreeItem
     * @treatAsPrivate
     */
    protected TreeItem<HierarchyItem> makeTreeItemGraphic(
            final DesignHierarchyMask owner,
            final FXOMObject fxomObject) {
        final HierarchyItemGraphic item
                = new HierarchyItemGraphic(owner, fxomObject, getDisplayOption());
        final TreeItem<HierarchyItem> treeItem = new TreeItem<>(item);
        Boolean expanded = treeItemsExpandedMapProperty.get(fxomObject);
        if (expanded != null) {
            treeItem.setExpanded(expanded);
        }
        // Mask may be null for empty place holder
        if (item.getMask() != null) {
            updateTreeItem(treeItem);
        }
        return treeItem;
    }

    protected void updateTreeItems() {
        assert getPanelControl() != null;
        final Parent parent = getPanelControl().getParent();
        assert parent instanceof Pane;
        final Pane pane = (Pane) parent;
        final FXOMDocument fxomDocument = getEditorController().getFxomDocument();

        if (fxomDocument == null || fxomDocument.getFxomRoot() == null) {
            rootTreeItem = null;
            // Add place holder to the parent
            if (fxomDocument == null) {
                promptLabel.setText(I18N.getString("contant.label.status.fxomdocument.null"));
            } else {
                promptLabel.setText(I18N.getString("content.label.status.invitation"));
            }
            if (pane.getChildren().contains(promptLabel) == false) {
                // This may occur when closing en empty document
                // => we switch from null FXOM root to null FXOM document
                pane.getChildren().add(promptLabel);
            }
        } else {
            rootTreeItem = makeTreeItem(fxomDocument.getFxomRoot());
            rootTreeItem.setExpanded(true);
            // Remove place holder from the parent
            ((Pane) parent).getChildren().remove(promptLabel);
        }
    }
    
    protected void updateParentRingColor() {
        // Update border items used to build the hierarchy parent ring
        BorderStroke bs;
        // bottom border
        bs = new BorderStroke(transparent, transparent, parentRingColor, transparent,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, borderWidths, Insets.EMPTY);
        bottomBorder = new Border(bs);
        // right bottom and left border
        bs = new BorderStroke(transparent, parentRingColor, parentRingColor, parentRingColor,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, borderWidths, Insets.EMPTY);
        rightBottomLeftBorder = new Border(bs);
        // right and left border
        bs = new BorderStroke(transparent, parentRingColor, transparent, parentRingColor,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, borderWidths, Insets.EMPTY);
        rightLeftBorder = new Border(bs);
        // top right bottom and left border
        bs = new BorderStroke(parentRingColor, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, borderWidths);
        topRightBottomLeftBorder = new Border(bs);
        // top right and left border
        bs = new BorderStroke(parentRingColor, parentRingColor, transparent, parentRingColor,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, borderWidths, Insets.EMPTY);
        topRightLeftBorder = new Border(bs);
        
        updateParentRing();
        updatePlaceHolder();
    }

    private void updateTreeItem(final TreeItem<HierarchyItem> treeItem) {

        final DesignHierarchyMask mask = treeItem.getValue().getMask();
        assert mask != null;

        // Graphic (displayed at first position)
        //---------------------------------
        if (mask.isAcceptingAccessory(Accessory.GRAPHIC)) {
            final FXOMObject value = mask.getAccessory(Accessory.GRAPHIC);
            if (value != null) {
                treeItem.getChildren().add(makeTreeItemGraphic(mask, value));
            }
        }

        // Tooltip (displayed at second position)
        //---------------------------------
        if (mask.isAcceptingAccessory(Accessory.TOOLTIP)) {
            final FXOMObject value = mask.getAccessory(Accessory.TOOLTIP);
            if (value != null) {
                treeItem.getChildren().add(makeTreeItem(value));
            }
        }

        // Context menu (displayed at third position)
        //---------------------------------
        if (mask.isAcceptingAccessory(Accessory.CONTEXT_MENU)) {
            final FXOMObject value = mask.getAccessory(Accessory.CONTEXT_MENU);
            if (value != null) {
                treeItem.getChildren().add(makeTreeItem(value));
            }
        }

        // Axis (chart)
        //---------------------------------
        if (mask.isAcceptingAccessory(Accessory.XAXIS)) {
            final FXOMObject value = mask.getAccessory(Accessory.XAXIS);
            if (value != null) {
                treeItem.getChildren().add(makeTreeItem(value));
            }
        }
        if (mask.isAcceptingAccessory(Accessory.YAXIS)) {
            final FXOMObject value = mask.getAccessory(Accessory.YAXIS);
            if (value != null) {
                treeItem.getChildren().add(makeTreeItem(value));
            }
        }
        
        // Content (ScrollPane, Tab...)
        //---------------------------------
        if (mask.isAcceptingAccessory(Accessory.CONTENT)) {
            final FXOMObject value = mask.getAccessory(Accessory.CONTENT);
            if (value != null) {
                treeItem.getChildren().add(makeTreeItem(value));
            }
        }

        // Positionning
        //---------------------------------
        for (Accessory accessory : new Accessory[]{
            Accessory.TOP,
            Accessory.LEFT,
            Accessory.CENTER,
            Accessory.RIGHT,
            Accessory.BOTTOM}) {
            if (mask.isAcceptingAccessory(accessory)) {
                final FXOMObject value = mask.getAccessory(accessory);
                treeItem.getChildren().add(makeTreeItemBorderPane(mask, value, accessory));
            }
        }

        // Sub components
        //---------------------------------
        if (mask.isAcceptingSubComponent()) {
            for (int i = 0, count = mask.getSubComponentCount(); i < count; i++) {
                final FXOMObject value = mask.getSubComponentAtIndex(i);
                treeItem.getChildren().add(makeTreeItem(value));
            }
        }
    }

    private List<TreeItem<HierarchyItem>> lookupTreeItem(List<FXOMObject> fxomObjects) {
        final List<TreeItem<HierarchyItem>> result = new ArrayList<>();
        for (FXOMObject fxomObject : fxomObjects) {
            result.add(lookupTreeItem(fxomObject));
        }
        return result;
    }

    /**
     * @param fxomObject the FXOMObject
     * @return the TreeItem corresponding to the specified FXOMObject
     * @treatAsPrivate
     */
    public TreeItem<HierarchyItem> lookupTreeItem(FXOMObject fxomObject) {
        return lookupTreeItem(fxomObject, getRoot());
    }

    private TreeItem<HierarchyItem> lookupTreeItem(FXOMObject fxomObject, TreeItem<HierarchyItem> fromTreeItem) {
        TreeItem<HierarchyItem> result;
        assert fxomObject != null;

        // ROOT TreeItem may be null when no document is loaded
        if (fromTreeItem != null) {
            assert fromTreeItem.getValue() != null;
            if (fromTreeItem.getValue().getFxomObject() == fxomObject) {
                result = fromTreeItem;
            } else {
                Iterator<TreeItem<HierarchyItem>> it = fromTreeItem.getChildren().iterator();
                result = null;
                while ((result == null) && it.hasNext()) {
                    TreeItem<HierarchyItem> childItem = it.next();
                    result = lookupTreeItem(fxomObject, childItem);
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Returns the list of all descendant from the specified parent TreeItem.
     * The specified parent TreeItem is excluded from the returned list.
     *
     * @param <T> type
     * @param parentTreeItem the parent TreeItem
     * @return the list of all descendant
     */
    private <T> List<TreeItem<T>> getAllTreeItems(final TreeItem<T> parentTreeItem) {
        assert parentTreeItem != null;
        final List<TreeItem<T>> treeItems = new ArrayList<>();
        for (TreeItem<T> child : parentTreeItem.getChildren()) {
            treeItems.add(child);
            treeItems.addAll(getAllTreeItems(child));
        }
        return treeItems;
    }

    /**
     * Returns the last visible TreeItem descendant of the specified parent
     * TreeItem.
     *
     * @param <T> type
     * @param parentTreeItem the parent TreeItem
     * @return the last visible TreeItem
     * @treatAsPrivate
     */
    public <T> TreeItem<T> getLastVisibleTreeItem(final TreeItem<T> parentTreeItem) {
        assert parentTreeItem != null;
        TreeItem<T> result = parentTreeItem;
        int size = result.getChildren().size();
        while (size != 0) {
            if (result.isExpanded()) {
                result = result.getChildren().get(size - 1);
                size = result.getChildren().size();
            } else {
                size = 0;
            }
        }
        return result;
    }

    /**
     * Returns the next visible TreeItem of the specified TreeItem.
     *
     * @param <T> type
     * @param treeItem the TreeItem
     * @return the next visible TreeItem
     * @treatAsPrivate
     */
    public <T> TreeItem<T> getNextVisibleTreeItem(final TreeItem<T> treeItem) {
        assert treeItem != null;
        if (treeItem == getRoot()) {
            // Root TreeItem has no next TreeItem
            return null;
        } else if (treeItem.isExpanded() && !treeItem.getChildren().isEmpty()) {
            // Return first child
            return treeItem.getChildren().get(0);
        } else {
            TreeItem<T> parentTreeItem = treeItem.getParent();
            TreeItem<T> result = treeItem.nextSibling();
            while (result == null && parentTreeItem != getRoot()) {
                result = parentTreeItem.nextSibling();
                parentTreeItem = parentTreeItem.getParent();
            }
            return result;
        }
    }

    /**
     * Returns the previous visible TreeItem of the specified TreeItem.
     *
     * @param <T> type
     * @param treeItem the TreeItem
     * @return the previous visible TreeItem
     * @treatAsPrivate
     */
    public <T> TreeItem<T> getPreviousVisibleTreeItem(final TreeItem<T> treeItem) {
        assert treeItem != null;
        if (treeItem == getRoot()) {
            // Root TreeItem has no previous TreeItem
            return null;
        } else {
            TreeItem<T> parentTreeItem = treeItem.getParent();
            TreeItem<T> result = treeItem.previousSibling();
            while (result == null && parentTreeItem != getRoot()) {
                result = parentTreeItem.previousSibling();
                parentTreeItem = parentTreeItem.getParent();
            }
            return result;
        }
    }

    /**
     * @treatAsPrivate
     */
    protected void initializePanel() {
        // Panel may be either a TreeView or a TreeTableView
        assert getPanelControl() != null;

        // Drag events
        //----------------------------------------------------------------------
        // DRAG_DONE event received when drag gesture 
        // started from the hierarchy panel ends
        getPanelControl().setOnDragDone(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                handleOnDragDone(event);
            }
        });
        getPanelControl().setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                handleOnDragDropped(event);
            }
        });
        getPanelControl().setOnDragEntered(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                handleOnDragEntered(event);
            }
        });
        getPanelControl().setOnDragExited(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                handleOnDragExited(event);
            }
        });
        getPanelControl().setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                handleOnDragOver(event);
            }
        });

        // Key events
        //----------------------------------------------------------------------
        getPanelControl().setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                handleOnKeyPressed(event);
            }
        });

        // Mouse events
        //----------------------------------------------------------------------
        // DRAG_DETECTED event received when drag gesture 
        // starts from the hierarchy panel
        getPanelControl().setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                handleOnDragDetected(event);
            }
        });
        getPanelControl().setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                handleOnMousePressed(event);
            }
        });

        // Setup the context menu
        final ContextMenuController contextMenuController
                = getEditorController().getContextMenuController();
        getPanelControl().setContextMenu(contextMenuController.getContextMenu());

        // Set default parent ring color
        setParentRingColor(DEFAULT_PARENT_RING_COLOR);
    }

    private void handleOnDragDetected(final MouseEvent event) {
        final ObservableList<TreeItem<HierarchyItem>> selectedTreeItems = getSelectedItems();

        // Do not start a DND gesture if there is an editing session on-going
        if (!getEditorController().canGetFxmlText()) {
            return;
        }

        final Selection selection = getEditorController().getSelection();
        if (selection.isEmpty() == false) { // (1)
            if (selection.getGroup() instanceof ObjectSelectionGroup) {
                // A set of regular component (ie fxom objects) are selected
                final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
                
                // Abort dragging an empty place holder
                for (TreeItem<HierarchyItem> selectedTreeItem : selectedTreeItems) {
                    final HierarchyItem item = selectedTreeItem.getValue();
                    if (item.isEmpty()) {
                        return;
                    }
                }
                // Abort dragging FXML include
                for (FXOMObject fxomObject : osg.getItems()) {
                    if (fxomObject instanceof FXOMIntrinsic) {
                        return;
                    }
                }

                // Retrieve the hit object
                final Cell<?> cell = lookupCell(event.getTarget());
                final Object item = cell.getItem();
                assert item instanceof HierarchyItem;
                final HierarchyItem hierarchyItem = (HierarchyItem) item;
                final FXOMObject hitObject = hierarchyItem.getFxomObject();
                assert (hitObject != null); // Because we cannot drag placeholders
                // Build drag source
                final Window ownerWindow = getPanelRoot().getScene().getWindow();
                final DocumentDragSource dragSource = new DocumentDragSource(
                        osg.getSortedItems(), hitObject, ownerWindow);
                // Start drag and drop
                final Dragboard db = getPanelControl().startDragAndDrop(TransferMode.MOVE);
                db.setContent(dragSource.makeClipboardContent());
                db.setDragView(dragSource.makeDragView());
                // DragController.begin
                assert getEditorController().getDragController().getDragSource() == null;
                getEditorController().getDragController().begin(dragSource);

            } else {
                // Emergency code : a new type of AbstractSelectionGroup
                // exists but is not managed by this code yet.
                assert false : "Add implementation for " + selection.getGroup().getClass();
            }
        }
    }

    private void handleOnDragDone(final DragEvent event) {
        // DragController update
        final DragController dragController
                = getEditorController().getDragController();
        assert shouldEndOnExit == false;
        dragController.end();
        event.getDragboard().clear();
    }

    private void handleOnDragDropped(final DragEvent event) {
        // If there is no document loaded
        // Should we allow to start with empty document in SB 2.0 ?
        if (getEditorController().getFxomDocument() == null) {
            return;
        }

        // DragController update
        final DragController dragController
                = getEditorController().getDragController();
        dragController.commit();
        // Do not invoke dragController.end here because we always receive a
        // DRAG_EXITED event which will perform the termination
        event.setDropCompleted(true);
        
        // Give the focus to the hierarchy
        getPanelControl().requestFocus();
    }

    private void handleOnDragEntered(final DragEvent event) {
        // CSS
        clearBorderColor();
        // When starting a DND gesture, disable parent ring updates
        setParentRingEnabled(false);

        // DragController update
        // The drag source is null if the drag gesture 
        // has been started from outside (from the explorer / finder)
        final DragController dragController
                = getEditorController().getDragController();
        if (dragController.getDragSource() == null) { // Drag started externally
            final FXOMDocument fxomDocument
                    = getEditorController().getFxomDocument();
            // Build drag source
            final Window ownerWindow = getPanelRoot().getScene().getWindow();
            final ExternalDragSource dragSource = new ExternalDragSource(
                    event.getDragboard(), fxomDocument, ownerWindow);
            dragController.begin(dragSource);
            shouldEndOnExit = true;
        }
    }

    private void handleOnDragExited(final DragEvent event) {
        // CSS
        clearBorderColor();
        // When ending a DND gesture, enable parent ring updates
        setParentRingEnabled(true);

        // Cancel timeline animation if any
        animationScheduler.stopTimeline();

        // Retrieve the vertical scroll bar value before updating the TreeItems
        double verticalScrollBarValue = 0.0;
        final ScrollBar scrollBar = getScrollBar(Orientation.VERTICAL);
        if (scrollBar != null) {
            verticalScrollBarValue = scrollBar.getValue();
        }

        // DragController update
        final DragController dragController
                = getEditorController().getDragController();
        dragController.setDropTarget(null);
        if (shouldEndOnExit) {
            dragController.end();
            shouldEndOnExit = false;
        }
        // Set back the vertical scroll bar value after the TreeItems have been updated
        if (scrollBar != null) {
            scrollBar.setValue(verticalScrollBarValue);
        }
    }

    private void handleOnDragOver(final DragEvent event) {
        final ScrollBar verticalScrollBar = getScrollBar(Orientation.VERTICAL);

        // By dragging and hovering the cell within a few pixels 
        // of the top or bottom of the Hierarchy,
        // the user can cause it to auto-scroll until the desired cell is in view.
        if (verticalScrollBar != null && verticalScrollBar.isVisible()) {
            final double eventY = event.getY();
            final double topY = getContentTopY();
            final double bottomY = getContentBottomY();

            // TOP auto scrolling zone
            if (topY <= eventY && eventY < topY + AUTO_SCROLLING_ZONE_HEIGHT) {
                // Start the timeline if not already playing
                if (!animationScheduler.isTimelineRunning()) {
                    animationScheduler.playDecrementAnimation(verticalScrollBar);
                }
            } // BOTTOM auto scrolling zone
            else if (bottomY >= eventY && eventY > bottomY - AUTO_SCROLLING_ZONE_HEIGHT) {
                // Start the timeline if not already playing
                if (!animationScheduler.isTimelineRunning()) {
                    animationScheduler.playIncrementAnimation(verticalScrollBar);
                }
            } else if (animationScheduler.isTimelineRunning()) {
                animationScheduler.stopTimeline();
            }
        }
    }

    private void handleOnKeyPressed(final KeyEvent event) {
        switch (event.getCode()) {

            // Handle edit actions
            case BACK_SPACE:
            case DELETE:
                final EditorController editorController = getEditorController();
                if (editorController.canPerformEditAction(EditorController.EditAction.DELETE)) {
                    editorController.performEditAction(EditorController.EditAction.DELETE);
                } else {
                    System.out.println("Beep... :(");
                }
                event.consume();
                break;

            // Handle Inline editing
            case ENTER:
                startEditingDisplayInfo();
                break;

            // Handle collapse all
            case LEFT:
                if (event.isAltDown()) {
                    final List<TreeItem<HierarchyItem>> treeItems = getSelectedItems();
                    if (!treeItems.isEmpty()) {
                        for (TreeItem<HierarchyItem> treeItem : treeItems) {
                            collapseAllTreeItems(treeItem);
                        }
                    }
                }
                break;

            // Handle expand all
            case RIGHT:
                if (event.isAltDown()) {
                    final List<TreeItem<HierarchyItem>> treeItems = getSelectedItems();
                    if (!treeItems.isEmpty()) {
                        for (TreeItem<HierarchyItem> treeItem : treeItems) {
                            expandAllTreeItems(treeItem);
                        }
                    }
                }
                break;
        }
    }

    private void handleOnMousePressed(final MouseEvent event) {

        if (event.getButton() == MouseButton.SECONDARY) {
            final ContextMenuController contextMenuController
                    = getEditorController().getContextMenuController();
            // The context menu items depend on the selection so
            // we need to rebuild it each time it is invoked.
            contextMenuController.updateContextMenuItems();
        }
    }

    private <T> void expandAllTreeItems(final TreeItem<T> parentTreeItem) {
        assert parentTreeItem != null;
        parentTreeItem.setExpanded(true);
        final List<TreeItem<T>> treeItems = getAllTreeItems(parentTreeItem);
        assert treeItems != null;
        for (TreeItem<T> treeItem : treeItems) {
            treeItem.setExpanded(true);
        }
    }

    private <T> void collapseAllTreeItems(final TreeItem<T> parentTreeItem) {
        assert parentTreeItem != null;
        parentTreeItem.setExpanded(false);
        final List<TreeItem<T>> treeItems = getAllTreeItems(parentTreeItem);
        assert treeItems != null;
        for (TreeItem<T> treeItem : treeItems) {
            treeItem.setExpanded(false);
        }
    }

    /**
     * Returns the cell ancestor of the specified event target. Indeed,
     * depending on the mouse click position, the event target may be the cell
     * node itself, the cell graphic or the cell labeled text.
     *
     * @param target
     * @return
     */
    private Cell<?> lookupCell(EventTarget target) {
        assert target instanceof Node;
        Node node = (Node) target;
        while ((node instanceof Cell) == false) {
            node = node.getParent();
        }
        return (Cell<?>) node;
    }
}
