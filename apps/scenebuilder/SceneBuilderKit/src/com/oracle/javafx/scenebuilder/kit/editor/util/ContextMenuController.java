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
package com.oracle.javafx.scenebuilder.kit.editor.util;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.ControlAction;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.EditAction;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.GridPane;
import javafx.stage.WindowEvent;

/**
 * Controller used to handle contextual menu in hierarchy and content view.
 */
public class ContextMenuController {

    private final EditorController editorController;
    private ContextMenu contextMenu;

    private MenuItem cutMenuItem;
    private MenuItem copyMenuItem;
    private MenuItem pasteMenuItem;
    private MenuItem pasteIntoMenuItem;
    private MenuItem duplicateMenuItem;
    private MenuItem deleteMenuItem;
    private MenuItem selectParentMenuItem;
    private MenuItem fitToParentMenuItem;
    private MenuItem useComputedSizesMenuItem;
    private MenuItem editIncludedFileMenuItem;
    private MenuItem revealIncludedFileMenuItem;
    private MenuItem bringToFrontMenuItem;
    private MenuItem sendToBackMenuItem;
    private MenuItem bringForwardMenuItem;
    private MenuItem sendBackwardMenuItem;
    private Menu wrapInMenu;
    private MenuItem wrapInAnchorPaneMenuItem;
    private MenuItem wrapInGridPaneMenuItem;
    private MenuItem wrapInGroupMenuItem;
    private MenuItem wrapInHBoxMenuItem;
    private MenuItem wrapInPaneMenuItem;
    private MenuItem wrapInScrollPaneMenuItem;
    private MenuItem wrapInSplitPaneMenuItem;
    private MenuItem wrapInStackPaneMenuItem;
    private MenuItem wrapInTabPaneMenuItem;
    private MenuItem wrapInTitledPaneMenuItem;
    private MenuItem wrapInToolBarMenuItem;
    private MenuItem wrapInVBoxMenuItem;
    private MenuItem unwrapMenuItem;
    private Menu gridPaneMenu;
    private MenuItem moveRowAboveMenuItem;
    private MenuItem moveRowBelowMenuItem;
    private MenuItem moveColumnBeforeMenuItem;
    private MenuItem moveColumnAfterMenuItem;
    private MenuItem addRowAboveMenuItem;
    private MenuItem addRowBelowMenuItem;
    private MenuItem addColumnBeforeMenuItem;
    private MenuItem addColumnAfterMenuItem;
    private MenuItem increaseRowSpan;
    private MenuItem decreaseRowSpan;
    private MenuItem increaseColumnSpan;
    private MenuItem decreaseColumnSpan;

    private final EventHandler<Event> onShowingMenuEventHandler
            = new EventHandler<Event>() {
                @Override
                public void handle(Event t) {
                    assert t.getSource() instanceof Menu;
                    final Menu menu = (Menu) t.getSource();
                    handleOnShowing(menu.getItems());
                }
            };

    private final EventHandler<WindowEvent> onShowingContextMenuEventHandler
            = new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    assert t.getSource() instanceof ContextMenu;
                    final ContextMenu contextMenu = (ContextMenu) t.getSource();
                    handleOnShowing(contextMenu.getItems());
                }
            };

    private final EventHandler<ActionEvent> onActionEventHandler
            = new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    assert t.getSource() instanceof MenuItem;
                    handleOnActionMenu((MenuItem) t.getSource());
                }
            };

    private final ChangeListener<Number> jobManagerRevisionListener
            = new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    jobManagerRevisionDidChange();
                }
            };

    public ContextMenuController(final EditorController editorController) {
        this.editorController = editorController;
        this.editorController.getJobManager().revisionProperty().addListener(jobManagerRevisionListener);
    }

    public ContextMenu getContextMenu() {
        if (contextMenu == null) {
            makeContextMenu();
        }
        return contextMenu;
    }

    /**
     * Updates the context menu items depending on the selection.
     */
    public void updateContextMenuItems() {

        getContextMenu().getItems().clear();

        final Selection selection = editorController.getSelection();
        if (selection.isEmpty() == false) {
            final AbstractSelectionGroup asg = selection.getGroup();
            if (asg instanceof ObjectSelectionGroup) {

                // Common editing actions
                getContextMenu().getItems().addAll(
                        cutMenuItem,
                        copyMenuItem,
                        pasteMenuItem,
                        pasteIntoMenuItem,
                        duplicateMenuItem,
                        deleteMenuItem,
                        new SeparatorMenuItem(),
                        selectParentMenuItem,
                        new SeparatorMenuItem(),
                        fitToParentMenuItem,
                        useComputedSizesMenuItem);
                // GridPane specific actions
                if (canPerformGridPaneActions() || canPerformGridPaneChildActions()) {
                    updateGridPaneMenuItems();
                    getContextMenu().getItems().add(gridPaneMenu);
                }
                // Common editing actions on going
                getContextMenu().getItems().addAll(
                        editIncludedFileMenuItem,
                        revealIncludedFileMenuItem,
                        new SeparatorMenuItem(),
                        bringToFrontMenuItem,
                        sendToBackMenuItem,
                        bringForwardMenuItem,
                        sendBackwardMenuItem,
                        new SeparatorMenuItem(),
                        wrapInMenu,
                        unwrapMenuItem);

            } else {
                assert asg instanceof GridSelectionGroup;
                getContextMenu().getItems().addAll(
                        deleteMenuItem,
                        new SeparatorMenuItem(),
                        moveRowAboveMenuItem,
                        moveRowBelowMenuItem,
                        moveColumnBeforeMenuItem,
                        moveColumnAfterMenuItem,
                        new SeparatorMenuItem(),
                        addRowAboveMenuItem,
                        addRowBelowMenuItem,
                        addColumnBeforeMenuItem,
                        addColumnAfterMenuItem);
            }
        }
    }

    private void handleOnShowing(final ObservableList<MenuItem> menuItems) {
        for (MenuItem menuItem : menuItems) {
            final boolean disable, selected;
            final String title;
            if (menuItem.getUserData() instanceof MenuItemController) {
                final MenuItemController c = (MenuItemController) menuItem.getUserData();
                disable = !c.canPerform();
                title = c.getTitle();
                selected = c.isSelected();
            } else {
                if (menuItem instanceof Menu) {
                    disable = false;
                    selected = false;
                    title = null;
                } else {
                    disable = true;
                    selected = false;
                    title = null;
                }
            }
            menuItem.setDisable(disable);
            if (title != null) {
                menuItem.setText(title);
            }
            if (menuItem instanceof RadioMenuItem) {
                final RadioMenuItem ri = (RadioMenuItem) menuItem;
                ri.setSelected(selected);
            }
        }
    }

    private void handleOnActionMenu(MenuItem i) {
        assert i.getUserData() instanceof MenuItemController;
        final MenuItemController c = (MenuItemController) i.getUserData();
        c.perform();
    }
    
    private void jobManagerRevisionDidChange() {
        // FXOMDocument has been modified by a job.
        if (contextMenu != null && contextMenu.isShowing()) {
            contextMenu.hide();
        }
    }


    /**
     * Returns true if all the selected items are GridPanes.
     *
     * @return
     */
    private boolean canPerformGridPaneActions() {
        boolean result = false;
        final Selection selection = editorController.getSelection();
        final AbstractSelectionGroup asg = selection.getGroup();

        if (asg instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) asg;
            result = true;
            for (FXOMObject obj : osg.getItems()) {
                if ((obj.getSceneGraphObject() instanceof GridPane) == false) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Returns true if all the selected items are children of 1 or more GridPanes.
     *
     * @return
     */
    private boolean canPerformGridPaneChildActions() {
        boolean result = false;
        final Selection selection = editorController.getSelection();
        final AbstractSelectionGroup asg = selection.getGroup();

        if (asg instanceof ObjectSelectionGroup) {
            final ObjectSelectionGroup osg = (ObjectSelectionGroup) asg;
            result = true;
            for (FXOMObject obj : osg.getItems()) {
                final FXOMObject parent = obj.getParentObject();
                if (parent == null
                        || (parent.getSceneGraphObject() instanceof GridPane) == false) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    private void updateGridPaneMenuItems() {
        assert canPerformGridPaneActions() || canPerformGridPaneChildActions();
        gridPaneMenu.getItems().clear();
        // Add actions on the GridPane rows/columns
        if (canPerformGridPaneActions()) {
            gridPaneMenu.getItems().addAll(
                    moveRowAboveMenuItem,
                    moveRowBelowMenuItem,
                    moveColumnBeforeMenuItem,
                    moveColumnAfterMenuItem,
                    new SeparatorMenuItem(),
                    addRowAboveMenuItem,
                    addRowBelowMenuItem,
                    addColumnBeforeMenuItem,
                    addColumnAfterMenuItem);
        }
        // Add actions on the GridPane row/column span
        if (canPerformGridPaneChildActions()) {
            // The selection is a GridPane child of another GridPane
            if (gridPaneMenu.getItems().isEmpty() == false) {
                gridPaneMenu.getItems().add(new SeparatorMenuItem());
            }
            gridPaneMenu.getItems().addAll(
                    increaseRowSpan,
                    decreaseRowSpan,
                    increaseColumnSpan,
                    decreaseColumnSpan);
        }
    }

    private void makeContextMenu() {
        this.contextMenu = new ContextMenu();
        this.contextMenu.setConsumeAutoHidingEvents(false);

        copyMenuItem = new MenuItem(I18N.getString("menu.title.copy"));
        copyMenuItem.setOnAction(onActionEventHandler);
        copyMenuItem.setUserData(new ControlActionController(ControlAction.COPY));
        selectParentMenuItem = new MenuItem(I18N.getString("menu.title.select.parent"));
        selectParentMenuItem.setOnAction(onActionEventHandler);
        selectParentMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_PARENT));
        editIncludedFileMenuItem = new MenuItem(I18N.getString("menu.title.edit.included.default"));
        editIncludedFileMenuItem.setOnAction(onActionEventHandler);
        editIncludedFileMenuItem.setUserData(new ControlActionController(ControlAction.EDIT_INCLUDED_FILE));
        revealIncludedFileMenuItem = new MenuItem(I18N.getString("menu.title.reveal.included.default"));
        revealIncludedFileMenuItem.setOnAction(onActionEventHandler);
        revealIncludedFileMenuItem.setUserData(new ControlActionController(ControlAction.REVEAL_INCLUDED_FILE));
        cutMenuItem = new MenuItem(I18N.getString("menu.title.cut"));
        cutMenuItem.setOnAction(onActionEventHandler);
        cutMenuItem.setUserData(new EditActionController(EditAction.CUT));
        pasteMenuItem = new MenuItem(I18N.getString("menu.title.paste"));
        pasteMenuItem.setOnAction(onActionEventHandler);
        pasteMenuItem.setUserData(new EditActionController(EditAction.PASTE));
        pasteIntoMenuItem = new MenuItem(I18N.getString("menu.title.paste.into"));
        pasteIntoMenuItem.setOnAction(onActionEventHandler);
        pasteIntoMenuItem.setUserData(new EditActionController(EditAction.PASTE_INTO));
        duplicateMenuItem = new MenuItem(I18N.getString("menu.title.duplicate"));
        duplicateMenuItem.setOnAction(onActionEventHandler);
        duplicateMenuItem.setUserData(new EditActionController(EditAction.DUPLICATE));
        deleteMenuItem = new MenuItem(I18N.getString("menu.title.delete"));
        deleteMenuItem.setOnAction(onActionEventHandler);
        deleteMenuItem.setUserData(new EditActionController(EditAction.DELETE));
        fitToParentMenuItem = new MenuItem(I18N.getString("menu.title.fit"));
        fitToParentMenuItem.setOnAction(onActionEventHandler);
        fitToParentMenuItem.setUserData(new EditActionController(EditAction.FIT_TO_PARENT));
        useComputedSizesMenuItem = new MenuItem(I18N.getString("menu.title.use.computed.sizes"));
        useComputedSizesMenuItem.setOnAction(onActionEventHandler);
        useComputedSizesMenuItem.setUserData(new EditActionController(EditAction.USE_COMPUTED_SIZES));
        bringToFrontMenuItem = new MenuItem(I18N.getString("menu.title.front"));
        bringToFrontMenuItem.setOnAction(onActionEventHandler);
        bringToFrontMenuItem.setUserData(new EditActionController(EditAction.BRING_TO_FRONT));
        sendToBackMenuItem = new MenuItem(I18N.getString("menu.title.back"));
        sendToBackMenuItem.setOnAction(onActionEventHandler);
        sendToBackMenuItem.setUserData(new EditActionController(EditAction.SEND_TO_BACK));
        bringForwardMenuItem = new MenuItem(I18N.getString("menu.title.forward"));
        bringForwardMenuItem.setOnAction(onActionEventHandler);
        bringForwardMenuItem.setUserData(new EditActionController(EditAction.BRING_FORWARD));
        sendBackwardMenuItem = new MenuItem(I18N.getString("menu.title.backward"));
        sendBackwardMenuItem.setOnAction(onActionEventHandler);
        sendBackwardMenuItem.setUserData(new EditActionController(EditAction.SEND_BACKWARD));
        wrapInMenu = new Menu(I18N.getString("menu.title.wrap"));
        wrapInAnchorPaneMenuItem = new MenuItem("AnchorPane");
        wrapInAnchorPaneMenuItem.setOnAction(onActionEventHandler);
        wrapInAnchorPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_ANCHOR_PANE));
        wrapInGridPaneMenuItem = new MenuItem("GridPane");
        wrapInGridPaneMenuItem.setOnAction(onActionEventHandler);
        wrapInGridPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_GRID_PANE));
        wrapInHBoxMenuItem = new MenuItem("HBox");
        wrapInHBoxMenuItem.setOnAction(onActionEventHandler);
        wrapInHBoxMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_HBOX));
        wrapInPaneMenuItem = new MenuItem("Pane");
        wrapInPaneMenuItem.setOnAction(onActionEventHandler);
        wrapInPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_PANE));
        wrapInScrollPaneMenuItem = new MenuItem("ScrollPane");
        wrapInScrollPaneMenuItem.setOnAction(onActionEventHandler);
        wrapInScrollPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_SCROLL_PANE));
        wrapInSplitPaneMenuItem = new MenuItem("SplitPane");
        wrapInSplitPaneMenuItem.setOnAction(onActionEventHandler);
        wrapInSplitPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_SPLIT_PANE));
        wrapInStackPaneMenuItem = new MenuItem("StackPane");
        wrapInStackPaneMenuItem.setOnAction(onActionEventHandler);
        wrapInStackPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_STACK_PANE));
        wrapInTabPaneMenuItem = new MenuItem("TabPane");
        wrapInTabPaneMenuItem.setOnAction(onActionEventHandler);
        wrapInTabPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TAB_PANE));
        wrapInTitledPaneMenuItem = new MenuItem("TitledPane");
        wrapInTitledPaneMenuItem.setOnAction(onActionEventHandler);
        wrapInTitledPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TITLED_PANE));
        wrapInToolBarMenuItem = new MenuItem("ToolBar");
        wrapInToolBarMenuItem.setOnAction(onActionEventHandler);
        wrapInToolBarMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TOOL_BAR));
        wrapInVBoxMenuItem = new MenuItem("VBox");
        wrapInVBoxMenuItem.setOnAction(onActionEventHandler);
        wrapInVBoxMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_VBOX));
        wrapInGroupMenuItem = new MenuItem("Group");
        wrapInGroupMenuItem.setOnAction(onActionEventHandler);
        wrapInGroupMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_GROUP));
        wrapInMenu.getItems().setAll(
                wrapInAnchorPaneMenuItem,
                wrapInGridPaneMenuItem,
                wrapInGroupMenuItem,
                wrapInHBoxMenuItem,
                wrapInPaneMenuItem,
                wrapInScrollPaneMenuItem,
                wrapInSplitPaneMenuItem,
                wrapInStackPaneMenuItem,
                wrapInTabPaneMenuItem,
                wrapInTitledPaneMenuItem,
                wrapInToolBarMenuItem,
                wrapInVBoxMenuItem);
        unwrapMenuItem = new MenuItem(I18N.getString("menu.title.unwrap"));
        unwrapMenuItem.setOnAction(onActionEventHandler);
        unwrapMenuItem.setUserData(new EditActionController(EditAction.UNWRAP));
        // GridPane specifics
        gridPaneMenu = new Menu(I18N.getString("menu.title.grid"));
        moveRowAboveMenuItem = new MenuItem(I18N.getString("menu.title.grid.move.row.above"));
        moveRowAboveMenuItem.setOnAction(onActionEventHandler);
        moveRowAboveMenuItem.setUserData(new EditActionController(EditAction.MOVE_ROW_ABOVE));
        moveRowBelowMenuItem = new MenuItem(I18N.getString("menu.title.grid.move.row.below"));
        moveRowBelowMenuItem.setOnAction(onActionEventHandler);
        moveRowBelowMenuItem.setUserData(new EditActionController(EditAction.MOVE_ROW_BELOW));
        moveColumnBeforeMenuItem = new MenuItem(I18N.getString("menu.title.grid.move.column.before"));
        moveColumnBeforeMenuItem.setOnAction(onActionEventHandler);
        moveColumnBeforeMenuItem.setUserData(new EditActionController(EditAction.MOVE_COLUMN_BEFORE));
        moveColumnAfterMenuItem = new MenuItem(I18N.getString("menu.title.grid.move.column.after"));
        moveColumnAfterMenuItem.setOnAction(onActionEventHandler);
        moveColumnAfterMenuItem.setUserData(new EditActionController(EditAction.MOVE_COLUMN_AFTER));
        addRowAboveMenuItem = new MenuItem(I18N.getString("menu.title.grid.add.row.above"));
        addRowAboveMenuItem.setOnAction(onActionEventHandler);
        addRowAboveMenuItem.setUserData(new EditActionController(EditAction.ADD_ROW_ABOVE));
        addRowBelowMenuItem = new MenuItem(I18N.getString("menu.title.grid.add.row.below"));
        addRowBelowMenuItem.setOnAction(onActionEventHandler);
        addRowBelowMenuItem.setUserData(new EditActionController(EditAction.ADD_ROW_BELOW));
        addColumnBeforeMenuItem = new MenuItem(I18N.getString("menu.title.grid.add.column.before"));
        addColumnBeforeMenuItem.setOnAction(onActionEventHandler);
        addColumnBeforeMenuItem.setUserData(new EditActionController(EditAction.ADD_COLUMN_BEFORE));
        addColumnAfterMenuItem = new MenuItem(I18N.getString("menu.title.grid.add.column.after"));
        addColumnAfterMenuItem.setOnAction(onActionEventHandler);
        addColumnAfterMenuItem.setUserData(new EditActionController(EditAction.ADD_COLUMN_AFTER));
        increaseRowSpan = new MenuItem(I18N.getString("menu.title.grid.increase.row.span"));
        increaseRowSpan.setOnAction(onActionEventHandler);
        increaseRowSpan.setUserData(new EditActionController(EditAction.INCREASE_ROW_SPAN));
        decreaseRowSpan = new MenuItem(I18N.getString("menu.title.grid.decrease.row.span"));
        decreaseRowSpan.setOnAction(onActionEventHandler);
        decreaseRowSpan.setUserData(new EditActionController(EditAction.DECREASE_ROW_SPAN));
        increaseColumnSpan = new MenuItem(I18N.getString("menu.title.grid.increase.column.span"));
        increaseColumnSpan.setOnAction(onActionEventHandler);
        increaseColumnSpan.setUserData(new EditActionController(EditAction.INCREASE_COLUMN_SPAN));
        decreaseColumnSpan = new MenuItem(I18N.getString("menu.title.grid.decrease.column.span"));
        decreaseColumnSpan.setOnAction(onActionEventHandler);
        decreaseColumnSpan.setUserData(new EditActionController(EditAction.DECREASE_COLUMN_SPAN));

        contextMenu.setOnShowing(onShowingContextMenuEventHandler);
        wrapInMenu.setOnShowing(onShowingMenuEventHandler);
        gridPaneMenu.setOnShowing(onShowingMenuEventHandler);

        for (MenuItem child : contextMenu.getItems()) {
            child.setOnAction(onActionEventHandler);
        }
    }

    class EditActionController extends MenuItemController {

        private final EditAction editAction;

        public EditActionController(EditAction editAction) {
            this.editAction = editAction;
        }

        @Override
        public boolean canPerform() {
            boolean result;
            if (editorController.getFxomDocument() == null) {
                result = false;
            } else {
                result = editorController.canPerformEditAction(editAction);
            }
            return result;
        }

        @Override
        public void perform() {
            assert canPerform() : "editAction=" + editAction;
            editorController.performEditAction(editAction);
        }
    }

    class ControlActionController extends MenuItemController {

        private final ControlAction controlAction;

        public ControlActionController(ControlAction controlAction) {
            this.controlAction = controlAction;
        }

        @Override
        public boolean canPerform() {
            boolean result;
            if (editorController.getFxomDocument() == null) {
                result = false;
            } else {
                result = editorController.canPerformControlAction(controlAction);
            }
            return result;
        }

        @Override
        public void perform() {
            assert canPerform() : "controlAction=" + controlAction;
            editorController.performControlAction(controlAction);
        }
    }

    abstract class MenuItemController {

        public abstract boolean canPerform();

        public abstract void perform();

        public String getTitle() {
            return null;
        }

        public boolean isSelected() {
            return false;
        }
    }
}
