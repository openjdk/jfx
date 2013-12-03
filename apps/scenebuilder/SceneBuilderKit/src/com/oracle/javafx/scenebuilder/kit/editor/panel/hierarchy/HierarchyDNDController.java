/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.editor.drag.DragController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AccessoryDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.ContainerZDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.RootDropTarget;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import javafx.scene.control.TreeItem;
import javafx.scene.input.DragEvent;

/**
 * Controller for all drag and drop gestures in hierarchy panel. This class does
 * not depend on the TreeView or TreeTableView control and handles only
 * TreeItems.
 *
 * p
 * @treatAsPrivate
 */
public class HierarchyDNDController {

    private final AbstractHierarchyPanelController panelController;
    private final HierarchyTaskScheduler scheduler;

    /**
     * Defines the mouse location within the cell when the dropping gesture
     * occurs.
     * @treatAsPrivate
     */
    public enum DroppingMouseLocation {

        BOTTOM, CENTER, TOP
    }

    public HierarchyDNDController(final AbstractHierarchyPanelController panelController) {
        this.panelController = panelController;
        this.scheduler = new HierarchyTaskScheduler(panelController);
    }

    public HierarchyTaskScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Called by the TreeCell/TreeTableRow event handler.
     *
     * @param treeItem the TreeItem
     * @param event the event
     * @param location the location
     */
    public void handleOnDragDropped(
            final TreeItem<HierarchyItem> treeItem,
            final DragEvent event,
            final DroppingMouseLocation location) {

        // Cancel timer if any
        scheduler.cancelTimer();
    }

    /**
     * Called by the TreeCell/TreeTableRow event handler.
     *
     * @param treeItem the TreeItem
     * @param event the event
     */
    public void handleOnDragEntered(
            final TreeItem<HierarchyItem> treeItem,
            final DragEvent event) {

        // Cancel timer if any
        scheduler.cancelTimer();

        if (treeItem == null) {
            return;
        }

        // Auto scrolling timeline has been started :
        // do not schedule any other task
        if (panelController.isTimelineRunning()) {
            return;
        }

        // Schedule expanding job for collapsed TreeItems
        if (!treeItem.isExpanded() && !treeItem.isLeaf()) {
            scheduler.scheduleExpandTask(treeItem);
        }
    }

    /**
     * Called by the TreeCell/TreeTableRow event handler.
     *
     * @param treeItem the TreeItem
     * @param event the event
     * @param location the location
     */
    public void handleOnDragExited(
            final TreeItem<HierarchyItem> treeItem,
            final DragEvent event,
            final DroppingMouseLocation location) {

        // Cancel timer if any
        scheduler.cancelTimer();

        // Remove empty tree item graphic if previously added by the scheduler
        // when we exit the empty graphic TreeItem by the bottom
        if (treeItem != null) {
            final HierarchyItem item = treeItem.getValue();
            assert item != null;
            if (item instanceof HierarchyItemGraphic
                    && item.isEmpty()
                    && location == DroppingMouseLocation.BOTTOM) {
                final TreeItem<HierarchyItem> parentTreeItem = treeItem.getParent();
                parentTreeItem.getChildren().remove(treeItem);
            }
        }
    }

    /**
     * Called by the TreeCell/TreeTableRow event handler.
     *
     * @param treeItem the TreeItem
     * @param event the event
     * @param location the location
     */
    public void handleOnDragOver(
            final TreeItem<HierarchyItem> treeItem,
            final DragEvent event,
            final DroppingMouseLocation location) {

        // Remove empty tree item graphic if previously added by the scheduler
        // when we hover the empty graphic owner TreeItem on the top area
        if (treeItem != null) {
            final HierarchyItem item = treeItem.getValue();
            assert item != null;
            final TreeItem<HierarchyItem> graphicTreeItem = getEmptyGraphicTreeItemFor(treeItem);
            if (graphicTreeItem != null && location == DroppingMouseLocation.TOP) {
                treeItem.getChildren().remove(graphicTreeItem);
            }
        }

        // First update drop target
        final DragController dragController
                = panelController.getEditorController().getDragController();
        final AbstractDropTarget dropTarget = makeDropTarget(treeItem, location);
        dragController.setDropTarget(dropTarget);

        // Then update transfer mode
        event.acceptTransferModes(dragController.getAcceptedTransferModes());

        // Schedule adding empty graphic place holder job :
        // The drop target must be a GRAPHIC AccessoryDropTarget
        if (dragController.isDropAccepted()
                && dropTarget instanceof AccessoryDropTarget
                && ((AccessoryDropTarget) dropTarget).getAccessory() == Accessory.GRAPHIC) {
            // GRAPHIC accessories cannot be dropped on empty TreeItems
            // They can be dropped :
            // - either on the accessory owner 
            // - or on its empty graphic place holder if already added
            assert treeItem != null;
            // Add empty graphic place holder if not already done :
            // - we are not over an already added empty graphic place holder 
            // - an empty graphic place holder has not yet been added
            // - we do not already scheduled a job to add the empty graphic place holder
            if (treeItem.getValue().isEmpty() == false
                    && getEmptyGraphicTreeItemFor(treeItem) == null
                    && scheduler.isAddEmptyGraphicTaskScheduled() == false) {
                scheduler.scheduleAddEmptyGraphicTask(treeItem);
            }
        }
    }

    /**
     * Returns the empty graphic TreeItem (if any) of the specified TreeItem,
     * null otherwise.
     *
     * @param treeItem the TreeItem
     * @return the empty graphic TreeItem
     */
    public TreeItem<HierarchyItem> getEmptyGraphicTreeItemFor(final TreeItem<HierarchyItem> treeItem) {
        assert treeItem != null;
        for (TreeItem<HierarchyItem> childTreeItem : treeItem.getChildren()) {
            final HierarchyItem child = childTreeItem.getValue();
            if (child instanceof HierarchyItemGraphic && child.isEmpty()) {
                return childTreeItem;
            }
        }
        return null;
    }

    private AbstractDropTarget makeDropTarget(
            final TreeItem<HierarchyItem> treeItem,
            final DroppingMouseLocation location) {

        assert location != null;

        final TreeItem<HierarchyItem> rootTreeItem = panelController.getRoot();
        final FXOMObject dropTargetObject;
        final AbstractDropTarget result;
        Accessory accessory = null; // Used if we insert as accessory (drop over a place holder)
        int targetIndex = -1; // Used if we insert as sub components

        final FXOMDocument document = panelController.getEditorController().getFxomDocument();
        if (document == null || document.getFxomRoot() == null) {
            return new RootDropTarget();
        }
        // TreeItem is null when dropping below the datas
        // => the drop target is the root
        if (treeItem == null) {
            dropTargetObject = rootTreeItem.getValue().getFxomObject();

        } else {
            final HierarchyItem item = treeItem.getValue();
            assert item != null;

            // When the TreeItem is a place holder :
            // - if the place holder is empty
            //      the drop target is the place holder parent
            //      the accessory is set to the place holder value
            // whatever the location value is.
            // - otherwise
            //      the drop target is the place holder item 
            //      the accessory is set to null
            //      the target index is set depending on the location value
            //------------------------------------------------------------------
            if (item.isPlaceHolder()) { // (1)

                assert treeItem != rootTreeItem;
                assert item instanceof HierarchyItemBorderPane
                        || item instanceof HierarchyItemGraphic;

                if (item.isEmpty()) {
                    // Set the drop target
                    final TreeItem<HierarchyItem> parentTreeItem = treeItem.getParent();
                    assert parentTreeItem != null; // Because of (1)
                    dropTargetObject = parentTreeItem.getValue().getFxomObject();
                    // Set the accessory
                    if (item instanceof HierarchyItemBorderPane) {
                        accessory = ((HierarchyItemBorderPane) item).getPosition();
                    } else {
                        accessory = Accessory.GRAPHIC;
                    }
                } else {
                    // Set the drop target
                    dropTargetObject = item.getFxomObject();
                    // Set the accessory
                    accessory = null;
                    // Set the target index
                    switch (location) {
                        case CENTER:
                        case TOP:
                            targetIndex = -1; // Insert at last position
                            break;
                        case BOTTOM:
                            if (treeItem.isLeaf() || !treeItem.isExpanded()) {
                                targetIndex = -1; // Insert at last position
                            } else {
                                targetIndex = 0; // Insert at first position
                            }
                            break;
                        default:
                            assert false;
                            break;

                    }
                }
            } //
            // TreeItem is not a place holder:
            // we set the drop target, accessory and target index 
            // depending on the mouse location value
            //------------------------------------------------------------------
            else {
                switch (location) {

                    // REPARENTING
                    case CENTER:
                        dropTargetObject = item.getFxomObject();
                        targetIndex = -1; // Insert at last position
                        break;

                    // REORDERING ABOVE
                    case TOP:
                        // Dropping on TOP of the root TreeItem
                        if (treeItem == rootTreeItem) { // (2)
                            dropTargetObject = item.getFxomObject();
                            targetIndex = -1; // Insert at last position
                        } else {
                            final TreeItem<HierarchyItem> parentTreeItem = treeItem.getParent();
                            assert parentTreeItem != null; // Because of (2)
                            dropTargetObject = parentTreeItem.getValue().getFxomObject();
                            targetIndex = item.getFxomObject().getIndexInParentProperty();
                        }
                        break;

                    // REORDERING BELOW
                    case BOTTOM:
                        // Dropping on BOTTOM of the root TreeItem
                        if (treeItem == rootTreeItem) { // (3)
                            dropTargetObject = item.getFxomObject();
                            targetIndex = 0; // Insert at first position
                        } else {
                            if (treeItem.isLeaf() || !treeItem.isExpanded()) {
                                final TreeItem<HierarchyItem> parentTreeItem = treeItem.getParent();
                                assert parentTreeItem != null; // Because of (3)
                                dropTargetObject = parentTreeItem.getValue().getFxomObject();
                                targetIndex = item.getFxomObject().getIndexInParentProperty() + 1;
                            } else {
                                dropTargetObject = item.getFxomObject();
                                targetIndex = 0; // Insert at first position
                            }
                        }
                        break;
                    default:
                        assert false;
                        dropTargetObject = null;
                        break;
                }
            }
        }

        result = makeDropTarget(dropTargetObject, accessory, targetIndex);
        return result;
    }

    private AbstractDropTarget makeDropTarget(
            final FXOMObject dropTargetObject,
            final Accessory accessory,
            int targetIndex) {

        AbstractDropTarget result = null;

        if (dropTargetObject != null) {
            final DragController dragController
                    = panelController.getEditorController().getDragController();
            final AbstractDragSource dragSource = dragController.getDragSource();
            assert dragSource != null;
            assert dropTargetObject instanceof FXOMInstance;
            final FXOMInstance dropTargetInstance = (FXOMInstance) dropTargetObject;
            if (accessory != null) {
                result = new AccessoryDropTarget(dropTargetInstance, accessory);
            } else {
                final DesignHierarchyMask dropTargetMask
                        = new DesignHierarchyMask(dropTargetInstance);
                // Check if the drop target accepts sub components
                if (dropTargetMask.isAcceptingSubComponent()) {
                    final FXOMObject beforeChild;
                    if (targetIndex == -1) {
                        beforeChild = null;
                    } else {
                        // targetIndex is the last sub component
                        if (targetIndex == dropTargetMask.getSubComponentCount()) {
                            beforeChild = null;
                        } else {
                            beforeChild = dropTargetMask.getSubComponentAtIndex(targetIndex);
                        }
                    }
                    result = new ContainerZDropTarget(dropTargetInstance, beforeChild);
                } //
                // Check if the drop target accepts accessories
                else {
                    // Check if there is an accessory that can be accepted by the drop target.
                    // First we build the list of accessories that can be set by DND gesture.
                    final Accessory[] accessories = {
                        Accessory.TOP,
                        Accessory.LEFT,
                        Accessory.CENTER,
                        Accessory.RIGHT,
                        Accessory.BOTTOM,
                        Accessory.CONTENT,
                        Accessory.CONTEXT_MENU,
                        Accessory.GRAPHIC,
                        Accessory.TOOLTIP};
                    for (Accessory a : accessories) {
                        final AccessoryDropTarget dropTarget
                                = new AccessoryDropTarget(dropTargetInstance, a);
                        // If the accessory drop target accepts the dragged objects, 
                        // we return this drop target.
                        // Otherwise, we look for the next accessory.
                        if (dropTarget.acceptDragSource(dragSource)) {
                            result = dropTarget;
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }
}
