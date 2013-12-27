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
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treetableview;

import com.oracle.javafx.scenebuilder.kit.editor.drag.DragController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AccessoryDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.ContainerZDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.RootDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyDNDController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyItem;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.BorderSide;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyDNDController.DroppingMouseLocation;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyDNDController.DroppingMouseLocation.BOTTOM;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyDNDController.DroppingMouseLocation.CENTER;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyDNDController.DroppingMouseLocation.TOP;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treetableview.HierarchyTreeTableCell.Column;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

/**
 * TreeTableRows used by the hierarchy TreeTableView.
 *
 * p
 *
 * @param <T>
 */
public class HierarchyTreeTableRow<T extends HierarchyItem> extends TreeTableRow<HierarchyItem> {

    private final AbstractHierarchyPanelController panelController;

    // Style class used for lookup
    static final String HIERARCHY_TREE_TABLE_ROW = "hierarchy-tree-table-row";

    // Vertical line used when inserting an item in order to indicate 
    // the parent into which the item will be inserted.
    // Horizontal lines are handled directly by the cell and are built using CSS only.
    //
    // This line will be added to / removed from the skin of the panel control
    // during DND gestures.
    private final Line insertLineIndicator = new Line();

    public HierarchyTreeTableRow(final AbstractHierarchyPanelController c) {
        super();
        this.panelController = c;

        // Add style class used when invoking lookupAll
        this.getStyleClass().add(HIERARCHY_TREE_TABLE_ROW);

        // Update vertical insert line indicator stroke width
        insertLineIndicator.setStrokeWidth(2.0);

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
                        = HierarchyTreeTableRow.this.getTreeItem();
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
                panelController.clearBorderColor(HierarchyTreeTableRow.this);
                // Remove insert line indicator
                panelController.removeFromPanelControlSkin(insertLineIndicator);
            }
        });
        setOnDragEntered(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                final TreeItem<HierarchyItem> treeItem
                        = HierarchyTreeTableRow.this.getTreeItem();
                // Forward to the DND controller
                dndController.handleOnDragEntered(treeItem, event);
            }
        });
        setOnDragExited(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                final TreeItem<HierarchyItem> treeItem
                        = HierarchyTreeTableRow.this.getTreeItem();
                final Bounds bounds = HierarchyTreeTableRow.this.getLayoutBounds();
                final Point2D point = HierarchyTreeTableRow.this.localToScene(bounds.getMinX(), bounds.getMinY());
                final DroppingMouseLocation location;
                if (event.getSceneY() <= point.getY()) {
                    location = DroppingMouseLocation.TOP;
                } else {
                    location = DroppingMouseLocation.BOTTOM;
                }

                // Forward to the DND controller
                dndController.handleOnDragExited(treeItem, event, location);

                // CSS
                panelController.clearBorderColor(HierarchyTreeTableRow.this);
                // Remove insert line indicator
                panelController.removeFromPanelControlSkin(insertLineIndicator);
            }
        });
        setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                final TreeItem<HierarchyItem> treeItem
                        = HierarchyTreeTableRow.this.getTreeItem();
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
                    final TreeItem<?> rootTreeItem = getTreeTableView().getRoot();

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
                        final TreeTableRow<?> cell;

                        // TreeItem is null when dropping below the datas
                        // => the drop target is the root
                        if (treeItem == null) {
                            cell = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), rootTreeItem);
                        } else {
                            final HierarchyItem item = treeItem.getValue();
                            assert item != null;
                            final TreeItem<HierarchyItem> graphicTreeItem
                                    = dndController.getEmptyGraphicTreeItemFor(treeItem);

                            if (item.isPlaceHolder()) {
                                cell = HierarchyTreeTableRow.this;
                            } else if (graphicTreeItem != null) {
                                assert accessoryDropTarget.getAccessory() == Accessory.GRAPHIC;
                                cell = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), graphicTreeItem);
                            } else {
                                final TreeItem<HierarchyItem> accessoryOwnerTreeItem
                                        = panelController.lookupTreeItem(dropTargetObject);
                                cell = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), accessoryOwnerTreeItem);
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
                        TreeTableRow<?> startCell, stopCell;

                        // TreeItem is null when dropping below the datas
                        // => the drop target is the root
                        if (treeItem == null) {
                            if (rootTreeItem.isLeaf() || !rootTreeItem.isExpanded()) {
                                final TreeTableRow<?> rootCell = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), 0);
                                final Border border = panelController.getBorder(BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                                rootCell.setBorder(border);
                            } else {
                                final TreeItem<?> lastTreeItem = panelController.getLastVisibleTreeItem(rootTreeItem);
                                final TreeTableRow<?> lastCell = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), lastTreeItem);
                                // As we are dropping below the datas, the last cell is visible
                                assert lastCell != null;
                                final Border border = panelController.getBorder(BorderSide.BOTTOM);
                                lastCell.setBorder(border);

                                // Update vertical insert line
                                startTreeItem = rootTreeItem;
                                startCell = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), startTreeItem);
                                stopCell = lastCell;
                                updateInsertLineIndicator(startCell, stopCell);
                                panelController.addToPanelControlSkin(insertLineIndicator);
                            }

                        } else {
                            final HierarchyItem item = treeItem.getValue();
                            assert item != null;
                            assert item.isPlaceHolder() == false;

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
                                        HierarchyTreeTableRow.this.setBorder(border);
                                    } else {
                                        final int index = getIndex();
                                        // Retrieve the previous cell
                                        // Note : we set the border on the bottom of the previous cell 
                                        // instead of using the top of the current cell in order to avoid
                                        // visual gap when DND from one cell to another
                                        final TreeTableRow<?> previousCell
                                                = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), index - 1);
                                        // The previous cell is null when the item is not visible
                                        if (previousCell != null) {
                                            final Border border = panelController.getBorder(BorderSide.BOTTOM);
                                            previousCell.setBorder(border);
                                        }

                                        // Update vertical insert line
                                        startTreeItem = panelController.lookupTreeItem(dropTarget.getTargetObject());
                                        startCell = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), startTreeItem);
                                        stopCell = previousCell;
                                        updateInsertLineIndicator(startCell, stopCell);
                                        panelController.addToPanelControlSkin(insertLineIndicator);
                                    }
                                    break;

                                // REPARENT gesture
                                case CENTER:
                                    if (treeItem.isLeaf() || !treeItem.isExpanded()) {
                                        final Border border = panelController.getBorder(BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                                        HierarchyTreeTableRow.this.setBorder(border);
                                    } else {
                                        // Reparent to the treeItem as last child
                                        final TreeItem<?> lastTreeItem = panelController.getLastVisibleTreeItem(treeItem);
                                        final TreeTableRow<?> lastCell = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), lastTreeItem);
                                        // Last cell is null when the item is not visible
                                        if (lastCell != null) {
                                            final Border border = panelController.getBorder(BorderSide.BOTTOM);
                                            lastCell.setBorder(border);
                                        }

                                        // Update vertical insert line
                                        startTreeItem = getTreeItem();
                                        startCell = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), startTreeItem);
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
                                        HierarchyTreeTableRow.this.setBorder(border);
                                    } else {
                                        // Reparent to the treeItem as first child
                                        final Border border = panelController.getBorder(BorderSide.BOTTOM);
                                        HierarchyTreeTableRow.this.setBorder(border);

                                        // Update vertical insert line
                                        startTreeItem = panelController.lookupTreeItem(dropTarget.getTargetObject());
                                        startCell = HierarchyTreeTableViewUtils.getTreeTableRow(getTreeTableView(), startTreeItem);
                                        stopCell = HierarchyTreeTableRow.this;
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
        });
    }

    @Override
    public void updateItem(HierarchyItem item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty && item != null) {
            // Update parent ring when scrolling / resizing vertically / expanding and collapsing
            panelController.updateParentRing();
        } else {
            assert item == null;
            // Clear CSS for empty cells
            panelController.clearBorderColor(this);
        }
    }

    private void filterMouseEvent(final MouseEvent me) {

        if (me.getEventType() == MouseEvent.MOUSE_PRESSED
                && me.getButton() == MouseButton.PRIMARY) {

            // Mouse pressed on an empty cell
            // => we perform select none
            if (isEmpty() == true) {
                // We clear the TreeTableView selection.
                // Note that this is not the same as invoking selection.clear().
                // Indeed, when empty BorderPane place holders are selected,
                // the SB selection is empty whereas the TreeTableView selection is not.
                getTreeTableView().getSelectionModel().clearSelection();
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
            final TreeItem<HierarchyItem> rootTreeItem = getTreeTableView().getRoot();
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

    private void updateInsertLineIndicator(
            final TreeTableRow<?> startTreeTableRow,
            final TreeTableRow<?> stopTreeTableRow) {

        assert panelController instanceof HierarchyTreeTableViewController;
        double columnHeaderHeight
                = ((HierarchyTreeTableViewController) panelController).getColumnHeaderHeight();

        //----------------------------------------------------------------------
        // START POINT CALCULATION
        //----------------------------------------------------------------------
        // Retrieve the disclosure node from which the vertical line will start
        double startX, startY;
        if (startTreeTableRow != null) {
            final Node disclosureNode = startTreeTableRow.getDisclosureNode();
            final Bounds startBounds = startTreeTableRow.getLayoutBounds();
            final Point2D startCellPoint = startTreeTableRow.localToParent(
                    startBounds.getMinX(), startBounds.getMinY());

            final Bounds disclosureNodeBounds = disclosureNode.getLayoutBounds();
            final Point2D disclosureNodePoint = disclosureNode.localToParent(
                    disclosureNodeBounds.getMinX(), disclosureNodeBounds.getMinY());

            // Initialize start point to the disclosure node of the start cell
            startX = startCellPoint.getX()
                    + disclosureNodePoint.getX()
                    + disclosureNodeBounds.getWidth() / 2 + 1; // +1 px tuning
            startY = columnHeaderHeight
                    + startCellPoint.getY()
                    + disclosureNodePoint.getY()
                    + disclosureNodeBounds.getHeight() - 6; // -6 px tuning
        } else {
            // The start cell is not visible :
            // x is set to the current cell graphic
            // y is set to the top of the TreeView / TreeTableView
            final TreeTableCell<?, ?> treeTableCell
                    = HierarchyTreeTableViewUtils.getTreeTableCell(getTreeTableView(), Column.CLASS_NAME, getTreeItem());
            final Bounds graphicBounds = treeTableCell.getGraphic().getLayoutBounds();
            final Point2D graphicPoint = treeTableCell.getGraphic().localToParent(
                    graphicBounds.getMinX(), graphicBounds.getMinY());

            startX = graphicPoint.getX();
            startY = panelController.getContentTopY();
        }

        //----------------------------------------------------------------------
        // END POINT CALCULATION
        //----------------------------------------------------------------------
        double endX, endY;
        endX = startX;
        if (stopTreeTableRow != null) {
            final Bounds stopBounds = stopTreeTableRow.getLayoutBounds();
            final Point2D stopCellPoint = stopTreeTableRow.localToParent(
                    stopBounds.getMinX(), stopBounds.getMinY());

            // Initialize end point to the end cell
            endY = columnHeaderHeight
                    + stopCellPoint.getY()
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
