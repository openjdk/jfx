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
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treeview;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.DragController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AccessoryDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.ContainerZDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.RootDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyFxIdJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyDNDController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyItem;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.BorderSide;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.HIERARCHY_READWRITE_LABEL;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.HIERARCHY_READONLY_LABEL;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.TREE_CELL_GRAPHIC;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyDNDController.DroppingMouseLocation;
import com.oracle.javafx.scenebuilder.kit.editor.util.InlineEditController;
import com.oracle.javafx.scenebuilder.kit.editor.util.InlineEditController.Type;
import com.oracle.javafx.scenebuilder.kit.editor.report.ErrorReport;
import com.oracle.javafx.scenebuilder.kit.editor.report.ErrorReportEntry;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.net.URL;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.util.Callback;

/**
 * TreeCells used by the hierarchy TreeView.
 *
 * p
 *
 * @param <T>
 */
public class HierarchyTreeCell<T extends HierarchyItem> extends TreeCell<HierarchyItem> {

    private final AbstractHierarchyPanelController panelController;

    // Style class used for lookup
    static final String HIERARCHY_TREE_CELL = "hierarchy-tree-cell";

    private final HBox graphic = new HBox();
    private final Label placeHolderLabel = new Label();
    private final Label classNameInfoLabel = new Label();
    private final Label displayInfoLabel = new Label();
    private final ImageView placeHolderImageView = new ImageView();
    private final ImageView classNameImageView = new ImageView();
    private final ImageView warningBadgeImageView = new ImageView();
    private final ImageView includedFileImageView = new ImageView();
    // Stack used to add badges over the top of the node icon
    private final StackPane iconsStack = new StackPane();
    // We use a label to set a tooltip over the node icon 
    // (StackPane does not allow to set tooltips)
    private final Label iconsLabel = new Label();
    private final Tooltip warningBadgeTooltip = new Tooltip();

    // Vertical line used when inserting an item in order to indicate 
    // the parent into which the item will be inserted.
    // Horizontal lines are handled directly by the cell and are built using CSS only.
    //
    // This line will be added to / removed from the skin of the panel control
    // during DND gestures.
    private final Line insertLineIndicator = new Line();

    // Listener for the display option used to update the display info label
    final ChangeListener<DisplayOption> displayOptionListener = new ChangeListener<DisplayOption>() {

        @Override
        public void changed(ObservableValue<? extends DisplayOption> ov, DisplayOption t, DisplayOption t1) {
            // Update display info for non empty cells
            if (!isEmpty() && getItem() != null && !getItem().isEmpty()) {
                final String displayInfo = getItem().getDisplayInfo(t1);
                displayInfoLabel.setText(displayInfo);
                displayInfoLabel.setManaged(getItem().hasDisplayInfo(t1));
                displayInfoLabel.setVisible(getItem().hasDisplayInfo(t1));
            }
        }
    };

    public HierarchyTreeCell(final AbstractHierarchyPanelController c) {
        super();
        this.panelController = c;

        iconsStack.getChildren().setAll(
                classNameImageView,
                warningBadgeImageView);
        iconsLabel.setGraphic(iconsStack);
        // RT-31645 : we cannot dynamically update the HBox graphic children 
        // in the cell.updateItem method.
        // We set once the graphic children, then we update the managed property
        // of the children depending on the cell item. 
        graphic.getChildren().setAll(
                includedFileImageView,
                placeHolderImageView,
                iconsLabel,
                placeHolderLabel,
                classNameInfoLabel,
                displayInfoLabel);

        // Add style class used when invoking lookupAll
        this.getStyleClass().add(HIERARCHY_TREE_CELL);

        // Update vertical insert line indicator stroke width
        insertLineIndicator.setStrokeWidth(2.0);

        // CSS
        graphic.getStyleClass().add(TREE_CELL_GRAPHIC);
        updatePlaceHolder();
        classNameInfoLabel.getStyleClass().add(HIERARCHY_READONLY_LABEL);
        displayInfoLabel.getStyleClass().add(HIERARCHY_READWRITE_LABEL);
        // Layout
        classNameInfoLabel.setMinWidth(Control.USE_PREF_SIZE);
        displayInfoLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(displayInfoLabel, Priority.ALWAYS);

        panelController.displayOptionProperty().addListener(
                new WeakChangeListener<>(displayOptionListener));

        // Key events
        //----------------------------------------------------------------------
        final EventHandler<KeyEvent> keyEventHandler = new EventHandler<KeyEvent>() {
            @Override
            public void handle(final KeyEvent e) {
                filterKeyEvent(e);
            }
        };
        this.addEventFilter(KeyEvent.ANY, keyEventHandler);

        // Mouse events
        //----------------------------------------------------------------------
        final EventHandler<MouseEvent> mouseEventHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(final MouseEvent e) {
                filterMouseEvent(e);
            }
        };
        this.addEventFilter(MouseEvent.ANY, mouseEventHandler);

        // Drag events
        //----------------------------------------------------------------------
        final HierarchyDNDController dndController = panelController.getDNDController();

        setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                final TreeItem<HierarchyItem> treeItem
                        = HierarchyTreeCell.this.getTreeItem();
                final DroppingMouseLocation location;
                if (treeItem != null) {
                    // REORDER ABOVE gesture
                    if ((getHeight() * 0.25) > event.getY()) {
                        location = DroppingMouseLocation.TOP;
                    } //
                    // REORDER BELOW gesture
                    else if ((getHeight() * 0.75) < event.getY()) {
                        location = DroppingMouseLocation.BOTTOM;
                    } //
                    // REPARENT gesture
                    else {
                        location = DroppingMouseLocation.CENTER;
                    }
                } else {
                    // TreeItem is null when dropping below the datas
                    location = DroppingMouseLocation.BOTTOM;
                }

                // Forward to the DND controller
                dndController.handleOnDragDropped(treeItem, event, location);

                // CSS
                panelController.clearBorderColor(HierarchyTreeCell.this);
                // Remove insert line indicator
                panelController.removeFromPanelControlSkin(insertLineIndicator);
            }
        });
        setOnDragEntered(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                final TreeItem<HierarchyItem> treeItem
                        = HierarchyTreeCell.this.getTreeItem();
                // Forward to the DND controller
                dndController.handleOnDragEntered(treeItem, event);
            }
        });
        setOnDragExited(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                final TreeItem<HierarchyItem> treeItem
                        = HierarchyTreeCell.this.getTreeItem();
                final Bounds bounds = HierarchyTreeCell.this.getLayoutBounds();
                final Point2D point = HierarchyTreeCell.this.localToScene(bounds.getMinX(), bounds.getMinY());
                final DroppingMouseLocation location;
                if (event.getSceneY() <= point.getY()) {
                    location = DroppingMouseLocation.TOP;
                } else {
                    location = DroppingMouseLocation.BOTTOM;
                }

                // Forward to the DND controller
                dndController.handleOnDragExited(treeItem, event, location);

                // CSS
                panelController.clearBorderColor(HierarchyTreeCell.this);
                // Remove insert line indicator
                panelController.removeFromPanelControlSkin(insertLineIndicator);
            }
        });
        setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                final TreeItem<HierarchyItem> treeItem
                        = HierarchyTreeCell.this.getTreeItem();
                final DragController dragController
                        = panelController.getEditorController().getDragController();
                final DroppingMouseLocation location = getDroppingMouseLocation(event);

                // Forward to the DND controller
                dndController.handleOnDragOver(treeItem, event, location); // (1)

                panelController.clearBorderColor();
                // Update vertical insert line indicator stroke color
                final Paint paint = panelController.getParentRingColor();
                insertLineIndicator.setStroke(paint);
                // Remove insert line indicator
                panelController.removeFromPanelControlSkin(insertLineIndicator);

                // If an animation timeline is running 
                // (auto-scroll when DND to the top or bottom of the Hierarchy),
                // we do not display insert indicators.
                if (panelController.isTimelineRunning()) {
                    return;
                }

                // Drop target has been updated because of (1)
                if (dragController.isDropAccepted()) {

                    final AbstractDropTarget dropTarget = dragController.getDropTarget();
                    final FXOMObject dropTargetObject = dropTarget.getTargetObject();
                    final TreeItem<?> rootTreeItem = getTreeView().getRoot();

                    if (dropTarget instanceof RootDropTarget) {
                        // No visual feedback in case of dropping the root node
                        return;
                    }

                    //==========================================================
                    // ACCESSORIES :
                    //
                    // No need to handle the insert line indicator.
                    // Border is set either on the accessory place holder cell
                    // or on the accessory owner cell.
                    //==========================================================
                    if (dropTarget instanceof AccessoryDropTarget) {

                        final AccessoryDropTarget accessoryDropTarget = (AccessoryDropTarget) dropTarget;
                        final TreeCell<?> cell;

                        // TreeItem is null when dropping below the datas
                        // => the drop target is the root
                        if (treeItem == null) {
                            cell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), rootTreeItem);
                        } else {
                            final HierarchyItem item = treeItem.getValue();
                            assert item != null;
                            final TreeItem<HierarchyItem> graphicTreeItem
                                    = dndController.getEmptyGraphicTreeItemFor(treeItem);

                            if (item.isPlaceHolder()) {
                                cell = HierarchyTreeCell.this;
                            } else if (graphicTreeItem != null) {
                                assert accessoryDropTarget.getAccessory() == Accessory.GRAPHIC;
                                cell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), graphicTreeItem);
                            } else {
                                final TreeItem<HierarchyItem> accessoryOwnerTreeItem
                                        = panelController.lookupTreeItem(dropTargetObject);
                                cell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), accessoryOwnerTreeItem);
                            }
                        }

                        final Border border = panelController.getBorder(BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                        cell.setBorder(border);
                    }//
                    //==========================================================
                    // SUB COMPONENTS :
                    //
                    // Need to handle the insert line indicator.
                    //==========================================================
                    else {
                        assert dropTarget instanceof ContainerZDropTarget;
                        TreeItem<?> startTreeItem;
                        TreeCell<?> startCell, stopCell;

                        // TreeItem is null when dropping below the datas
                        // => the drop target is the root
                        if (treeItem == null) {
                            if (rootTreeItem.isLeaf() || !rootTreeItem.isExpanded()) {
                                final TreeCell<?> rootCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), 0);
                                final Border border = panelController.getBorder(BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                                rootCell.setBorder(border);
                            } else {
                                final TreeItem<?> lastTreeItem = panelController.getLastVisibleTreeItem(rootTreeItem);
                                final TreeCell<?> lastCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), lastTreeItem);
                                // As we are dropping below the datas, the last cell is visible
                                assert lastCell != null;
                                final Border border = panelController.getBorder(BorderSide.BOTTOM);
                                lastCell.setBorder(border);

                                // Update vertical insert line
                                startTreeItem = rootTreeItem;
                                startCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), startTreeItem);
                                stopCell = lastCell;
                                updateInsertLineIndicator(startCell, stopCell);
                                panelController.addToPanelControlSkin(insertLineIndicator);
                            }

                        } else {
                            final HierarchyItem item = treeItem.getValue();
                            assert item != null;

                            if (item.isPlaceHolder()) {
                                // The place holder item is filled with a container
                                // accepting sub components
                                final Border border = panelController.getBorder(BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                                HierarchyTreeCell.this.setBorder(border);
                            } else {
                                // REORDERING :
                                // To avoid visual movement of the horizontal border when
                                // dragging from one cell to another,
                                // we always set the border on the cell bottom location :
                                // - if we handle REORDER BELOW gesture, just set the bottom 
                                // border on the current cell
                                // - if we handle REORDER ABOVE gesture, we set the bottom 
                                // border on the previous cell
                                //
                                switch (location) {

                                    // REORDER ABOVE gesture
                                    case TOP:
                                        if (treeItem == rootTreeItem) {
                                            final Border border = panelController.getBorder(BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                                            HierarchyTreeCell.this.setBorder(border);
                                        } else {
                                            final int index = getIndex();
                                            // Retrieve the previous cell
                                            // Note : we set the border on the bottom of the previous cell 
                                            // instead of using the top of the current cell in order to avoid
                                            // visual gap when DND from one cell to another
                                            final TreeCell<?> previousCell
                                                    = HierarchyTreeViewUtils.getTreeCell(getTreeView(), index - 1);
                                            // The previous cell is null when the item is not visible
                                            if (previousCell != null) {
                                                final Border border = panelController.getBorder(BorderSide.BOTTOM);
                                                previousCell.setBorder(border);
                                            }

                                            // Update vertical insert line
                                            startTreeItem = panelController.lookupTreeItem(dropTarget.getTargetObject());
                                            startCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), startTreeItem);
                                            stopCell = previousCell;
                                            updateInsertLineIndicator(startCell, stopCell);
                                            panelController.addToPanelControlSkin(insertLineIndicator);
                                        }
                                        break;

                                    // REPARENT gesture
                                    case CENTER:
                                        if (treeItem.isLeaf() || !treeItem.isExpanded()) {
                                            final Border border = panelController.getBorder(BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                                            HierarchyTreeCell.this.setBorder(border);
                                        } else {
                                            // Reparent to the treeItem as last child
                                            final TreeItem<?> lastTreeItem = panelController.getLastVisibleTreeItem(treeItem);
                                            final TreeCell<?> lastCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), lastTreeItem);
                                            // Last cell is null when the item is not visible
                                            if (lastCell != null) {
                                                final Border border = panelController.getBorder(BorderSide.BOTTOM);
                                                lastCell.setBorder(border);
                                            }

                                            // Update vertical insert line
                                            startTreeItem = getTreeItem();
                                            startCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), startTreeItem);
                                            stopCell = lastCell;
                                            updateInsertLineIndicator(startCell, stopCell);
                                            panelController.addToPanelControlSkin(insertLineIndicator);
                                        }
                                        break;

                                    // REORDER BELOW gesture
                                    case BOTTOM:
                                        if (treeItem == rootTreeItem
                                                && (treeItem.isLeaf() || !treeItem.isExpanded())) {
                                            final Border border = panelController.getBorder(BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                                            HierarchyTreeCell.this.setBorder(border);
                                        } else {
                                            // Reparent to the treeItem as first child
                                            final Border border = panelController.getBorder(BorderSide.BOTTOM);
                                            HierarchyTreeCell.this.setBorder(border);

                                            // Update vertical insert line
                                            startTreeItem = panelController.lookupTreeItem(dropTarget.getTargetObject());
                                            startCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), startTreeItem);
                                            stopCell = HierarchyTreeCell.this;
                                            updateInsertLineIndicator(startCell, stopCell);
                                            panelController.addToPanelControlSkin(insertLineIndicator);
                                        }
                                        break;

                                    default:
                                        assert false;
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void updateItem(HierarchyItem item, boolean empty) {
        super.updateItem(item, empty);

        // The cell is not empty (TreeItem is not null) 
        // AND the TreeItem value is not null
        if (!empty && item != null) {
            updateLayout(item);
            setGraphic(graphic);
            setText(null);
            // Update parent ring when scrolling / resizing vertically / expanding and collapsing
            panelController.updateParentRing();
        } else {
            assert item == null;
            setGraphic(null);
            setText(null);
            // Clear CSS for empty cells
            panelController.clearBorderColor(this);
        }
    }

    public final void updatePlaceHolder() {
        final Paint paint = panelController.getParentRingColor();
        placeHolderLabel.setTextFill(paint);
        final BorderWidths bw = new BorderWidths(1);
        final BorderStroke bs = new BorderStroke(paint, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, bw);
        final Border b = new Border(bs);
        placeHolderLabel.setBorder(b);
    }
    
    private void filterKeyEvent(final KeyEvent ke) {
        // empty
    }

    private void filterMouseEvent(final MouseEvent me) {

        if (me.getEventType() == MouseEvent.MOUSE_PRESSED
                && me.getButton() == MouseButton.PRIMARY) {

            // Mouse pressed on a non empty cell :
            // => we may start inline editing
            if (isEmpty() == false) { // (1)
                if (me.getClickCount() >= 2) {
                    // Start inline editing the display info on double click OVER the display info label
                    // Double click over the class name label will end up with the native expand/collapse behavior
                    final HierarchyItem item = getItem();
                    assert item != null; // Because of (1)
                    final DisplayOption option = panelController.getDisplayOption();
                    if (item.hasDisplayInfo(option) && displayInfoLabel.isHover()) {
                        startEditingDisplayInfo();
                        // Consume the event so the native expand/collapse behavior is not performed
                        me.consume();
                    }
                }
            } //
            // Mouse pressed on an empty cell
            // => we perform select none
            else {
                // We clear the TreeView selection.
                // Note that this is not the same as invoking selection.clear().
                // Indeed, when empty BorderPane place holders are selected,
                // the SB selection is empty whereas the TreeView selection is not.
                getTreeView().getSelectionModel().clearSelection();
            }
        }
        updateCursor(me);
    }

    private void updateCursor(final MouseEvent me) {
        final Scene scene = getScene();

        if (scene == null) {
            // scene may be null when tree view is collapsed
            return;
        }
        // When another window is focused (just like the preview window), 
        // we use default cursor
        if (!getScene().getWindow().isFocused()) {
            scene.setCursor(Cursor.DEFAULT);
            return;
        }
        if (isEmpty()) {
            scene.setCursor(Cursor.DEFAULT);
        } else {
            final TreeItem<HierarchyItem> rootTreeItem = getTreeView().getRoot();
            final HierarchyItem item = getTreeItem().getValue();
            assert item != null;
            boolean isRoot = getTreeItem() == rootTreeItem;
            boolean isEmpty = item.isEmpty();

            if (me.getEventType() == MouseEvent.MOUSE_ENTERED) {
                if (!me.isPrimaryButtonDown()) {
                    // Cannot DND root or place holder items
                    if (isRoot || isEmpty) {
                        setCursor(Cursor.DEFAULT);
                    } else {
                        setCursor(Cursor.OPEN_HAND);
                    }
                }
            } else if (me.getEventType() == MouseEvent.MOUSE_PRESSED) {
                // Cannot DND root or place holder items
                if (isRoot || isEmpty) {
                    setCursor(Cursor.DEFAULT);
                } else {
                    setCursor(Cursor.CLOSED_HAND);
                }
            } else if (me.getEventType() == MouseEvent.MOUSE_RELEASED) {
                // Cannot DND root or place holder items
                if (isRoot || isEmpty) {
                    setCursor(Cursor.DEFAULT);
                } else {
                    setCursor(Cursor.OPEN_HAND);
                }
            } else if (me.getEventType() == MouseEvent.MOUSE_EXITED) {
                setCursor(Cursor.DEFAULT);
            }
        }
    }

    /**
     * *************************************************************************
     * Inline editing
     *
     * We cannot use the FX inline editing because it occurs on selection +
     * simple mouse click
     * *************************************************************************
     */
    public void startEditingDisplayInfo() {
        assert getItem().hasDisplayInfo(panelController.getDisplayOption());
        final InlineEditController inlineEditController
                = panelController.getEditorController().getInlineEditController();
        final TextInputControl editor;
        final Type type;
        final String initialValue;

        // 1) Build the TextInputControl used to inline edit
        //----------------------------------------------------------------------
        // INFO display option may use either a TextField or a TextArea
        if (panelController.getDisplayOption() == DisplayOption.INFO) {
            final String info = getItem().getDescription();
            final Object sceneGraphObject = getItem().getFxomObject().getSceneGraphObject();
            if (sceneGraphObject instanceof TextArea || DesignHierarchyMask.containsLineFeed(info)) {
                type = Type.TEXT_AREA;
            } else {
                type = Type.TEXT_FIELD;
            }
            // displayInfoLabel.getText() may be truncated to a single line description
            // We set the initial value with the complete description value
            initialValue = getItem().getDescription();
        } //
        // FXID and NODEID options use a TextField
        else {
            type = Type.TEXT_FIELD;
            initialValue = displayInfoLabel.getText();
        }
        editor = inlineEditController.createTextInputControl(type, displayInfoLabel, initialValue);
        // CSS
        final ObservableList<String> styleSheets
                = panelController.getPanelRoot().getStylesheets();
        editor.getStylesheets().addAll(styleSheets);
        editor.getStyleClass().add(InlineEditController.INLINE_EDITOR);

        // 2) Build the COMMIT Callback
        // This callback will be invoked to commit the new value
        // It returns true if the value is unchanged or if the commit succeeded, 
        // false otherwise
        //----------------------------------------------------------------------
        final Callback<String, Boolean> requestCommit = new Callback<String, Boolean>() {

            @Override
            public Boolean call(String newValue) {
                // TODO fix DTL-5881 : Inline editing must handle not valid input value
                // 1) Check the input value is valid
                // 2) If valid, commit the new value and return true
                // 3) Otherwise, return false
                final HierarchyItem item = getItem();
                final FXOMObject fxomObject = item.getFxomObject();
                final DisplayOption option = panelController.getDisplayOption();
                final EditorController editorController = panelController.getEditorController();
                switch (option) {
                    case INFO:
                    case NODEID:
                        if (fxomObject instanceof FXOMInstance) {
                            final FXOMInstance fxomInstance = (FXOMInstance) fxomObject;
                            final PropertyName propertyName = item.getPropertyNameForDisplayInfo(option);
                            assert propertyName != null;
                            final ValuePropertyMetadata vpm
                                    = Metadata.getMetadata().queryValueProperty(fxomInstance, propertyName);
                            final ModifyObjectJob job
                                    = new ModifyObjectJob(fxomInstance, vpm, newValue, editorController);
                            if (job.isExecutable()) {
                                editorController.getJobManager().push(job);
                            }
                        }
                        break;
                    case FXID:
                        final ModifyFxIdJob job
                                = new ModifyFxIdJob(fxomObject, newValue, editorController);
                        if (job.isExecutable()) {
                            editorController.getJobManager().push(job);
                        }
                        break;
                    default:
                        assert false;
                        return false;
                }
                return true;
            }
        };

        inlineEditController.startEditingSession(editor, displayInfoLabel, requestCommit);
    }

    private void updateLayout(HierarchyItem item) {
        assert item != null;
        final FXOMObject fxomObject = item.getFxomObject();

        // Update ImageViews
        final Image placeHolderImage = item.getPlaceHolderImage();
        placeHolderImageView.setImage(placeHolderImage);
        placeHolderImageView.setManaged(placeHolderImage != null);

        final Image classNameImage = item.getClassNameIcon();
        classNameImageView.setImage(classNameImage);
        classNameImageView.setManaged(classNameImage != null);

        // Included file
        if (fxomObject instanceof FXOMIntrinsic
                && ((FXOMIntrinsic) fxomObject).getType() == FXOMIntrinsic.Type.FX_INCLUDE) {
            final URL resource = ImageUtils.getNodeIconURL("Included.png"); //NOI18N
            includedFileImageView.setImage(ImageUtils.getImage(resource));
            includedFileImageView.setManaged(true);
        } else {
            includedFileImageView.setImage(null);
            includedFileImageView.setManaged(false);
        }

        final List<ErrorReportEntry> entries = getErrorReportEntries(item);
        if (entries != null) {
            assert !entries.isEmpty();
            // Update tooltip with the first entry
            warningBadgeTooltip.setText(entries.get(0).toString());
            warningBadgeImageView.setImage(ImageUtils.getWarningBadgeImage());
            warningBadgeImageView.setManaged(true);
            iconsLabel.setTooltip(warningBadgeTooltip);
        } else {
            warningBadgeTooltip.setText(null);
            warningBadgeImageView.setImage(null);
            warningBadgeImageView.setManaged(false);
            iconsLabel.setTooltip(null);
        }

        // Update Labels
        final String placeHolderInfo = item.getPlaceHolderInfo();
        placeHolderLabel.setText(placeHolderInfo);
        placeHolderLabel.setManaged(item.isEmpty());
        placeHolderLabel.setVisible(item.isEmpty());

        final String classNameInfo = item.getClassNameInfo();
        classNameInfoLabel.setText(classNameInfo);
        classNameInfoLabel.setManaged(classNameInfo != null);
        classNameInfoLabel.setVisible(classNameInfo != null);

        final DisplayOption option = panelController.getDisplayOption();
        final String displayInfo = item.getDisplayInfo(option);
        displayInfoLabel.setText(displayInfo);
        displayInfoLabel.setManaged(item.hasDisplayInfo(option));
        displayInfoLabel.setVisible(item.hasDisplayInfo(option));
    }

    private List<ErrorReportEntry> getErrorReportEntries(HierarchyItem item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        final EditorController editorController = panelController.getEditorController();
        final ErrorReport errorReport = editorController.getErrorReport();
        final FXOMObject fxomObject = item.getFxomObject();
        assert fxomObject != null;
        return errorReport.query(fxomObject, !getTreeItem().isExpanded());
    }

    private void updateInsertLineIndicator(
            final TreeCell<?> startTreeCell,
            final TreeCell<?> stopTreeCell) {

        //----------------------------------------------------------------------
        // START POINT CALCULATION
        //----------------------------------------------------------------------
        // Retrieve the disclosure node from which the vertical line will start
        double startX, startY;
        if (startTreeCell != null) {
            final Node disclosureNode = startTreeCell.getDisclosureNode();
            final Bounds startBounds = startTreeCell.getLayoutBounds();
            final Point2D startCellPoint = startTreeCell.localToParent(
                    startBounds.getMinX(), startBounds.getMinY());

            final Bounds disclosureNodeBounds = disclosureNode.getLayoutBounds();
            final Point2D disclosureNodePoint = disclosureNode.localToParent(
                    disclosureNodeBounds.getMinX(), disclosureNodeBounds.getMinY());

            // Initialize start point to the disclosure node of the start cell
            startX = startCellPoint.getX()
                    + disclosureNodePoint.getX()
                    + disclosureNodeBounds.getWidth() / 2 + 1; // +1 px tuning
            startY = startCellPoint.getY()
                    + disclosureNodePoint.getY()
                    + disclosureNodeBounds.getHeight() - 6; // -6 px tuning
        } else {
            // The start cell is not visible :
            // x is set to the current cell graphic
            // y is set to the top of the TreeView / TreeTableView
            final Bounds graphicBounds = getGraphic().getLayoutBounds();
            final Point2D graphicPoint = getGraphic().localToParent(
                    graphicBounds.getMinX(), graphicBounds.getMinY());

            startX = graphicPoint.getX();
            startY = panelController.getContentTopY();
        }

        //----------------------------------------------------------------------
        // END POINT CALCULATION
        //----------------------------------------------------------------------
        double endX, endY;
        endX = startX;
        if (stopTreeCell != null) {
            final Bounds stopBounds = stopTreeCell.getLayoutBounds();
            final Point2D stopCellPoint = stopTreeCell.localToParent(
                    stopBounds.getMinX(), stopBounds.getMinY());

            // Initialize end point to the end cell
            endY = stopCellPoint.getY()
                    + stopBounds.getHeight() // Add the stop cell height
                    - 1; // -1 px tuning
        } else {
            // The stop cell is not visisble :
            // y is set to the bottom of the TreeView / TreeTableView
            endY = panelController.getContentBottomY();
        }

        insertLineIndicator.setStartX(startX);
        insertLineIndicator.setStartY(startY);
        insertLineIndicator.setEndX(endX);
        insertLineIndicator.setEndY(endY);
    }

    private DroppingMouseLocation getDroppingMouseLocation(final DragEvent event) {
        final DroppingMouseLocation location;
        if (this.getTreeItem() != null) {
            if ((getHeight() * 0.25) > event.getY()) {
                location = DroppingMouseLocation.TOP;
            } else if ((getHeight() * 0.75) < event.getY()) {
                location = DroppingMouseLocation.BOTTOM;
            } else {
                location = DroppingMouseLocation.CENTER;
            }
        } else {
            location = DroppingMouseLocation.BOTTOM;
        }
        return location;
    }
}
