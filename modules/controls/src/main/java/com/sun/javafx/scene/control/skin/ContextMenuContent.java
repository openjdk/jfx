/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.scene.control.skin;

import com.sun.javafx.scene.control.MultiplePropertyChangeListenerHandler;
import com.sun.javafx.scene.control.behavior.TwoLevelFocusPopupBehavior;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is a the SkinBase for ContextMenu based controls so that the CSS parts
 * work right, because otherwise we would have to copy the Keys from there to here.
 */
public class ContextMenuContent extends Region {

    private ContextMenu contextMenu;

    /***************************************************************************
     * UI subcomponents
     **************************************************************************/

    private double maxGraphicWidth = 0; // we keep this margin to left for graphic
    private double maxRightWidth = 0;
    private double maxLabelWidth = 0;
    private double maxRowHeight = 0;
    private double maxLeftWidth = 0;
    private double oldWidth = 0;

    private Rectangle clipRect;
    MenuBox itemsContainer;
    private ArrowMenuItem upArrow;
    private ArrowMenuItem downArrow;

    /*
     * We maintain a current focused index which is used
     * in keyboard navigation of menu items.
     */
    private int currentFocusedIndex = -1;
    
    private boolean itemsDirty = true;
    private InvalidationListener popupShowingListener = arg0 -> {
        updateItems();
    };
    private WeakInvalidationListener weakPopupShowingListener = 
            new WeakInvalidationListener(popupShowingListener);

    /***************************************************************************
     * Constructors
     **************************************************************************/
    public ContextMenuContent(final ContextMenu popupMenu) {
        this.contextMenu = popupMenu;
        clipRect = new Rectangle();
         clipRect.setSmooth(false);
        itemsContainer = new MenuBox();
//        itemsContainer = new VBox();
        itemsContainer.setClip(clipRect);
        
        upArrow = new ArrowMenuItem(this);
        upArrow.setUp(true);
        upArrow.setFocusTraversable(false);

        downArrow = new ArrowMenuItem(this);
        downArrow.setUp(false);
        downArrow.setFocusTraversable(false);
        getChildren().add(itemsContainer);
        getChildren().add(upArrow);
        getChildren().add(downArrow);
        initialize();
        setUpBinds();
        updateItems();
        // RT-20197 add menuitems only on first show.
        popupMenu.showingProperty().addListener(weakPopupShowingListener);

        /*
        ** only add this if we're on an embedded
        ** platform that supports 5-button navigation 
        */
        if (Utils.isTwoLevelFocus()) {
            new TwoLevelFocusPopupBehavior(this);
        }
    }
    
    //For access from controls
    public VBox getItemsContainer() {
        return itemsContainer;
    }
    //For testing purpose only
    int getCurrentFocusIndex() {
        return currentFocusedIndex;
    }
    //For testing purpose only
    void setCurrentFocusedIndex(int index) {
        if (index < itemsContainer.getChildren().size()) {
            currentFocusedIndex = index;
        }
    }
    
    private void updateItems() {
        if (itemsDirty) {
            updateVisualItems();
            itemsDirty = false;
        }
    }

    private void computeVisualMetrics() {
        maxRightWidth = 0;
        maxLabelWidth = 0;
        maxRowHeight = 0;
        maxGraphicWidth = 0;
        maxLeftWidth = 0;

        for (int i = 0; i < itemsContainer.getChildren().size(); i++) {
            Node child = itemsContainer.getChildren().get(i);
            if (child instanceof MenuItemContainer) {
                final MenuItemContainer menuItemContainer = (MenuItemContainer)itemsContainer.getChildren().get(i);
                
                if (! menuItemContainer.isVisible()) continue;
                
                double alt = -1;
                Node n = menuItemContainer.left;
                if (n != null) {
                    if (n.getContentBias() == Orientation.VERTICAL) { // width depends on height
                        alt = snapSize(n.prefHeight(-1));
                    } else alt = -1;
                    maxLeftWidth = Math.max(maxLeftWidth, snapSize(n.prefWidth(alt)));
                    maxRowHeight = Math.max(maxRowHeight, n.prefHeight(-1));
                }

                n = menuItemContainer.graphic;
                if (n != null) {
                    if (n.getContentBias() == Orientation.VERTICAL) { // width depends on height
                        alt = snapSize(n.prefHeight(-1));
                    } else alt = -1;
                    maxGraphicWidth = Math.max(maxGraphicWidth, snapSize(n.prefWidth(alt)));
                    maxRowHeight = Math.max(maxRowHeight, n.prefHeight(-1));
                }

                n = menuItemContainer.label;
                if (n != null) {
                    if (n.getContentBias() == Orientation.VERTICAL) {
                        alt = snapSize(n.prefHeight(-1));
                    } else alt = -1;
                    maxLabelWidth = Math.max(maxLabelWidth, snapSize(n.prefWidth(alt)));
                    maxRowHeight = Math.max(maxRowHeight, n.prefHeight(-1));
                }

                n = menuItemContainer.right;
                if (n != null) {
                    if (n.getContentBias() == Orientation.VERTICAL) { // width depends on height
                        alt = snapSize(n.prefHeight(-1));
                    } else alt = -1;
                    maxRightWidth = Math.max(maxRightWidth, snapSize(n.prefWidth(alt)));
                    maxRowHeight = Math.max(maxRowHeight, n.prefHeight(-1));
                }
            }
        }

        // Fix for RT-38838.
        // This fixes the issue where CSS is applied to a menu after it has been
        // showing, resulting in its bounds changing. In this case, we need to
        // shift the submenu such that it is properly aligned with its parent menu.
        //
        // To do this, we must firstly determine if the open submenu is shifted
        // horizontally to appear on the other side of this menu, as this is the
        // only situation where shifting has to happen. If so, we need to check
        // if we should shift the submenu due to changes in width.
        //
        // We need to get the parent menu of this contextMenu, so that we only
        // modify the X value in the following conditions:
        // 1) There exists a parent menu
        // 2) The parent menu is in the correct position (i.e. to the left of this
        //    menu in normal LTR systems).
        final double newWidth = maxRightWidth + maxLabelWidth + maxGraphicWidth + maxLeftWidth;
        Window ownerWindow = contextMenu.getOwnerWindow();
        if (ownerWindow instanceof ContextMenu) {
            if (contextMenu.getX() < ownerWindow.getX()) {
                if (oldWidth != newWidth) {
                    contextMenu.setX(contextMenu.getX() + oldWidth - newWidth);
                }
            }
        }

        oldWidth = newWidth;
    }
    
    private void updateVisualItems() {
        ObservableList<Node> itemsContainerChilder = itemsContainer.getChildren();

        disposeVisualItems();
        
        for (int row = 0; row < getItems().size(); row++) {
            final MenuItem item = getItems().get(row);
            if (item instanceof CustomMenuItem && ((CustomMenuItem) item).getContent() == null) {
                continue;
            }
            
            if (item instanceof SeparatorMenuItem) {
                // we don't want the hover highlight for separators, so for
                // now this is the simplest approach - just remove the
                // background entirely. This may cause issues if people
                // intend to style the background differently.
                Node node = ((CustomMenuItem) item).getContent();
                node.visibleProperty().bind(item.visibleProperty());
                itemsContainerChilder.add(node);
                // Add the (separator) menu item to properties map of this node.
                // Special casing this for separator :
                // This allows associating this container with SeparatorMenuItem.
                node.getProperties().put(MenuItem.class, item);
            } else {
                MenuItemContainer menuItemContainer = new MenuItemContainer(item);
                menuItemContainer.visibleProperty().bind(item.visibleProperty());
                itemsContainerChilder.add(menuItemContainer);
            }
        }
        
        // Add the Menu to properties map of this skin. Used by QA for testing
        // This enables associating a parent menu for this skin showing menu items.
        if (getItems().size() > 0) {
            final MenuItem item = getItems().get(0);
            getProperties().put(Menu.class, item.getParentMenu());
        }

        // RT-36513 made this applyCss(). Modified by RT-36995 to impl_reapplyCSS()
        impl_reapplyCSS();
    }

    private void disposeVisualItems() {
        // clean up itemsContainer
        ObservableList<Node> itemsContainerChilder = itemsContainer.getChildren();
        for (int i = 0, max = itemsContainerChilder.size(); i < max; i++) {
            Node n = itemsContainerChilder.get(i);

            if (n instanceof MenuItemContainer) {
                MenuItemContainer container = (MenuItemContainer) n;
                container.visibleProperty().unbind();
                container.dispose();
            }
        }
        itemsContainerChilder.clear();
    }

    /**
     * Can be called by Skins when they need to clean up the content of any 
     * ContextMenu instances they might have created. This ensures that contents 
     * of submenus if any, also get cleaned up.
     */
    public void dispose() {
        disposeBinds();
        disposeVisualItems();

        disposeContextMenu(submenu);
        submenu = null;
        openSubmenu = null;
        selectedBackground = null;
        if (contextMenu != null) {
            contextMenu.getItems().clear();
            contextMenu = null;
        }
    }

    public void disposeContextMenu(ContextMenu menu) {
        if (menu == null) return;

        Skin<?> skin = menu.getSkin();
        if (skin == null) return;

        ContextMenuContent cmContent = (ContextMenuContent)skin.getNode();
        if (cmContent == null) return;

        cmContent.dispose(); // recursive call to dispose submenus.
    }

    @Override protected void layoutChildren() {
        if (itemsContainer.getChildren().size() == 0) return;
        final double x = snappedLeftInset();
        final double y = snappedTopInset();
        final double w = getWidth() - x - snappedRightInset();
        final double h = getHeight() - y - snappedBottomInset();
        final double contentHeight =  snapSize(getContentHeight()); // itemsContainer.prefHeight(-1);

        itemsContainer.resize(w,contentHeight);
        itemsContainer.relocate(x, y);

        if (isFirstShow && ty == 0) {
            upArrow.setVisible(false);
            isFirstShow = false;
        } else {
            upArrow.setVisible(ty < y && ty < 0);
        }
        downArrow.setVisible(ty + contentHeight > (y + h));

        clipRect.setX(0);
        clipRect.setY(0);
        clipRect.setWidth(w);
        clipRect.setHeight(h);

        if (upArrow.isVisible()) {
            final double prefHeight = snapSize(upArrow.prefHeight(-1));
            clipRect.setHeight(snapSize(clipRect.getHeight() - prefHeight));
            clipRect.setY(snapSize(clipRect.getY()) + prefHeight);
            upArrow.resize(snapSize(upArrow.prefWidth(-1)), prefHeight);
            positionInArea(upArrow, x, y, w, prefHeight, /*baseline ignored*/0,
                    HPos.CENTER, VPos.CENTER);
        }

        if (downArrow.isVisible()) {
            final double prefHeight = snapSize(downArrow.prefHeight(-1));
            clipRect.setHeight(snapSize(clipRect.getHeight()) - prefHeight);
            downArrow.resize(snapSize(downArrow.prefWidth(-1)), prefHeight);
            positionInArea(downArrow, x, (y + h - prefHeight), w, prefHeight, /*baseline ignored*/0,
                    HPos.CENTER, VPos.CENTER);
        }
    }

     @Override protected double computePrefWidth(double height) {
         computeVisualMetrics();
         double prefWidth = 0;
         if (itemsContainer.getChildren().size() == 0) return 0;
         for (Node n : itemsContainer.getChildren()) {
             if (! n.isVisible()) continue;
             prefWidth = Math.max(prefWidth, snapSize(n.prefWidth(-1)));
         }
         return snappedLeftInset() + snapSize(prefWidth) + snappedRightInset();
    }

    @Override protected double computePrefHeight(double width) {
        if (itemsContainer.getChildren().size() == 0) return 0;
        final double screenHeight = getScreenHeight();
        final double contentHeight = getContentHeight(); // itemsContainer.prefHeight(width);
        double totalHeight = snappedTopInset() + snapSize(contentHeight) + snappedBottomInset();
        // the pref height of this menu is the smaller value of the
        // actual pref height and the height of the screens _visual_ bounds.
        double prefHeight = (screenHeight <= 0) ? (totalHeight) : (Math.min(totalHeight, screenHeight));
        return prefHeight;
    }

    @Override protected double computeMinHeight(double width) {
        return 0.0;
    }

    @Override protected double computeMaxHeight(double height) {
        return getScreenHeight();
    }

    private double getScreenHeight() {
        if (contextMenu == null || contextMenu.getOwnerWindow() == null ||
                contextMenu.getOwnerWindow().getScene() == null) {
            return -1;
        }
        return snapSize(com.sun.javafx.util.Utils.getScreen(
            contextMenu.getOwnerWindow().getScene().getRoot()).getVisualBounds().getHeight());
        
    }

    private double getContentHeight() {
        double h = 0.0d;
        for (Node i : itemsContainer.getChildren()) {
            if (i.isVisible()) {
               h += snapSize(i.prefHeight(-1));
            }
        }
        return h;
    }
  
    // This handles shifting ty when doing keyboard navigation.
    private void ensureFocusedMenuItemIsVisible(Node node) {
        if (node == null) return;

        final Bounds nodeBounds = node.getBoundsInParent();
        final Bounds clipBounds = clipRect.getBoundsInParent();

        if (nodeBounds.getMaxY() >= clipBounds.getMaxY()) {
            // this is for moving down the menu
            scroll(-nodeBounds.getMaxY() + clipBounds.getMaxY());
        } else if (nodeBounds.getMinY() <= clipBounds.getMinY()) {
            // this is for moving up the menu
            scroll(-nodeBounds.getMinY() + clipBounds.getMinY());
        }
    }
    
    protected ObservableList<MenuItem> getItems() {
        return contextMenu.getItems();
    }

    /**
     * Finds the index of currently focused item.
     */
    private int findFocusedIndex() {
         for (int i = 0; i < itemsContainer.getChildren().size(); i++) {
            Node n = itemsContainer.getChildren().get(i);
            if (n.isFocused()) {
                return i;
            }
        }
        return -1;
    }

    private boolean isFirstShow = true;
    private double ty;

    private void initialize() {
        // keyboard navigation support. Initially focus goes to this ContextMenu,
        // but when the user first hits the up or down arrow keys, the focus
        // is transferred to the first or last item respectively. Once this
        // happens, it is up to the menu items to navigate between themselves.
        contextMenu.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // initialize the focused index for keyboard navigation.
                currentFocusedIndex = -1;
                requestFocus();
            }
        });
        
        // RT-19624 calling requestFocus inside layout was casuing repeated layouts.
        contextMenu.addEventHandler(Menu.ON_SHOWN, event -> {
            for (Node child : itemsContainer.getChildren()) {
                if (child instanceof MenuItemContainer) {
                    final MenuItem item = ((MenuItemContainer)child).item;
                    // When the choiceBox popup is shown, if this menu item is selected
                    // do a requestFocus so CSS kicks in and the item is highlighted.
                    if ("choice-box-menu-item".equals(item.getId())) {
                        if (((RadioMenuItem)item).isSelected()) {
                            child.requestFocus();
                            break;
                        }
                    }
                }

            }
        });

//        // FIXME For some reason getSkinnable()Behavior traversal functions don't
//        // get called as expected, so I've just put the important code below.
        // We use setOnKeyPressed here as we are not adding a listener to a public
        // event type (ContextMenuContent is not public API), and without this
        // we get the issue shown in RT-34429
        setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent ke) {
                switch (ke.getCode()) {
                    case LEFT:
                        if (getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                            processRightKey(ke);
                        } else {
                            processLeftKey(ke);
                        }
                        break;
                    case RIGHT:
                        if (getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                            processLeftKey(ke);
                        } else {
                            processRightKey(ke);
                        }
                        break;
                    case CANCEL:
                        ke.consume();
                        break;
                    case ESCAPE:
                        // if the owner is not a menubar button, just close the
                        // menu - this will move focus up to the parent menu
                        // as required. In the case of the parent being a
                        // menubar button we special case in the conditional code
                        // beneath this switch statement. See RT-34429 for more context.
                        final Node ownerNode = contextMenu.getOwnerNode();
                        if (! (ownerNode instanceof MenuBarSkin.MenuBarButton)) {
                            contextMenu.hide();
                            ke.consume();
                        }
                        break;
                    case DOWN:
                        // move to the next sibling
                        moveToNextSibling();
                        ke.consume();
                        break;
                    case UP:
                        // move to previous sibling
                        moveToPreviousSibling();
                        ke.consume();
                        break;
                    case SPACE:
                    case ENTER:
                        // select the menuitem
                        selectMenuItem();
                        ke.consume();
                        break;
                    default:
                        break;
                }

                if (!ke.isConsumed()) {
                    final Node ownerNode = contextMenu.getOwnerNode();
                    if (ownerNode instanceof MenuItemContainer) {
                        // Forward to parent menu
                        Parent parent = ownerNode.getParent();
                        while (parent != null && !(parent instanceof ContextMenuContent)) {
                            parent = parent.getParent();
                        }
                        if (parent instanceof ContextMenuContent) {
                            parent.getOnKeyPressed().handle(ke);
                        }
                    } else if (ownerNode instanceof MenuBarSkin.MenuBarButton) {
                        // This is a top-level MenuBar Menu, so forward event to MenuBar
                        MenuBarSkin mbs = ((MenuBarSkin.MenuBarButton)ownerNode).getMenuBarSkin();
                        if (mbs != null && mbs.getKeyEventHandler() != null) {
                            mbs.getKeyEventHandler().handle(ke);
                        }
                    }
                }
            }
        });

        addEventHandler(ScrollEvent.SCROLL, event -> {
            /*
             * we'll only scroll if the arrows are visible in the direction
             * that we're going, otherwise we go into empty space.
             */
            final double textDeltaY = event.getTextDeltaY();
            final double deltaY = event.getDeltaY();
            if ((downArrow.isVisible() && (textDeltaY < 0.0 || deltaY < 0.0)) ||
                (upArrow.isVisible() && (textDeltaY > 0.0 || deltaY > 0.0))) {

                switch(event.getTextDeltaYUnits()) {
                  case LINES:
                      /*
                      ** scroll lines, use the row height of selected row,
                      ** or row 0 if none selected
                      */
                      int focusedIndex = findFocusedIndex();
                      if (focusedIndex == -1) {
                          focusedIndex = 0;
                      }
                      double rowHeight = itemsContainer.getChildren().get(focusedIndex).prefHeight(-1);
                      scroll(textDeltaY * rowHeight);
                      break;
                  case PAGES:
                      /*
                      ** page scroll, scroll the menu height
                      */
                      scroll(textDeltaY * itemsContainer.getHeight());
                      break;
                  case NONE:
                      /*
                      ** pixel scroll
                      */
                      scroll(deltaY);
                      break;
                }
                event.consume();
            }
        });
    }

    private void processLeftKey(KeyEvent ke) {
        if (currentFocusedIndex != -1) {
            Node n = itemsContainer.getChildren().get(currentFocusedIndex);
            if (n instanceof MenuItemContainer) {
                MenuItem item = ((MenuItemContainer)n).item;
                if (item instanceof Menu) {
                    final Menu menu = (Menu) item;

                    // if the submenu for this menu is showing, hide it
                    if (menu == openSubmenu && submenu != null && submenu.isShowing()) {
                        hideSubmenu();
                        ke.consume();
                    }
                }
            }
        }
    }

    private void processRightKey(KeyEvent ke) {
        if (currentFocusedIndex != -1) {
            Node n = itemsContainer.getChildren().get(currentFocusedIndex);
            if (n instanceof MenuItemContainer) {
                MenuItem item = ((MenuItemContainer)n).item;
                if (item instanceof Menu) {
                    final Menu menu = (Menu) item;
                    if (menu.isDisable()) return;
                    selectedBackground = ((MenuItemContainer)n);

                    // RT-15103
                    // if submenu for this menu is already showing then do nothing
                    // Menubar will process the right key and move to the next menu
                    if (openSubmenu == menu && submenu != null && submenu.isShowing()) {
                        return;
                    }

                    showMenu(menu);
                    ke.consume();
                }
            }
        }
    }

    private void showMenu(Menu menu) {
        menu.show();
        // request focus on the first item of the submenu after it is shown
        ContextMenuContent cmContent = (ContextMenuContent)submenu.getSkin().getNode();
        if (cmContent != null) {
           if (cmContent.itemsContainer.getChildren().size() > 0) {
               cmContent.itemsContainer.getChildren().get(0).requestFocus();
               cmContent.currentFocusedIndex = 0;
           } else {
               cmContent.requestFocus();
           }
        }
    }

    private void selectMenuItem() {
        if (currentFocusedIndex != -1) {
            Node n = itemsContainer.getChildren().get(currentFocusedIndex);
            if (n instanceof MenuItemContainer) {
                MenuItem item = ((MenuItemContainer)n).item;
                if (item instanceof Menu) {
                    final Menu menu = (Menu) item;
                    if (openSubmenu != null) {
                        hideSubmenu();
                    }
                    if (menu.isDisable()) return;
                    selectedBackground = ((MenuItemContainer)n);
                    menu.show();
                } else {
                    ((MenuItemContainer)n).doSelect();
                }
            }
        }
    }
    /*
     * Find the index of the next MenuItemContainer in the itemsContainer children.
     */
    private int findNext(int from) {
        for (int i = from; i < itemsContainer.getChildren().size(); i++) {
            Node n = itemsContainer.getChildren().get(i);
            if (n instanceof MenuItemContainer) {
                return i;
            }
        }
        // find from top
        for (int i = 0; i < from; i++) {
            Node n = itemsContainer.getChildren().get(i);
            if (n instanceof MenuItemContainer) {
                return i;
            }
        }
        return -1; // should not happen
    }
    
    private void moveToNextSibling() {
        // If focusedIndex is -1 then start from 0th menu item.
        // Note that this will cycle through such that when you move to last item,
        // it will move to 1st item on the next Down key press.
        if (currentFocusedIndex != -1) {
            currentFocusedIndex = findNext(currentFocusedIndex + 1);
        } else if (currentFocusedIndex == -1 || currentFocusedIndex == (itemsContainer.getChildren().size() - 1)) {
            currentFocusedIndex = findNext(0);
        }

        // request focus on the next sibling which currentFocusIndex points to
        if (currentFocusedIndex != -1) {
            Node n = itemsContainer.getChildren().get(currentFocusedIndex);
            selectedBackground = ((MenuItemContainer)n);
            n.requestFocus();
            ensureFocusedMenuItemIsVisible(n);
        }
    }
    
    /*
     * Find the index the previous MenuItemContaner in the itemsContainer children.
     */
    private int findPrevious(int from) {
        for (int i = from; i >= 0; i--) {
            Node n = itemsContainer.getChildren().get(i);
            if (n instanceof MenuItemContainer) {
                return(i);
            }
        }
        for (int i = itemsContainer.getChildren().size() - 1 ; i > from; i--) {
            Node n = itemsContainer.getChildren().get(i);
            if (n instanceof MenuItemContainer) {
                return(i);
            }
        }
        return -1;
    }

     private void moveToPreviousSibling() {
        // If focusedIndex is -1 then start from the last menu item to go up.
        // Note that this will cycle through such that when you move to first item,
        // it will move to last item on the next Up key press.
        if (currentFocusedIndex != -1) {
            currentFocusedIndex = findPrevious(currentFocusedIndex - 1);
        } else if(currentFocusedIndex == -1 || currentFocusedIndex == 0) {
            currentFocusedIndex = findPrevious(itemsContainer.getChildren().size() - 1);
        }

        // request focus on the previous sibling which currentFocusIndex points to
        if (currentFocusedIndex != -1) {
            Node n = itemsContainer.getChildren().get(currentFocusedIndex);
            selectedBackground = ((MenuItemContainer)n);
            n.requestFocus();
            ensureFocusedMenuItemIsVisible(n);
        }
    }

    /*
     * Get the Y offset from the top of the popup to the menu item whose index
     * is given.
     */
    double getMenuYOffset(int menuIndex) {
        double offset = 0;
        if (itemsContainer.getChildren().size() > menuIndex) {
            offset = snappedTopInset();
            Node menuitem = itemsContainer.getChildren().get(menuIndex);
            offset += menuitem.getLayoutY() + menuitem.prefHeight(-1);
        }
        return offset;
    }

    private void setUpBinds() {
        updateMenuShowingListeners(contextMenu.getItems(), true);
        contextMenu.getItems().addListener(contextMenuItemsListener);
    }

    private void disposeBinds() {
        updateMenuShowingListeners(contextMenu.getItems(), false);
        contextMenu.getItems().removeListener(contextMenuItemsListener);
    }

    private ChangeListener<Boolean> menuShowingListener = (observable, wasShowing, isShowing) -> {
        ReadOnlyBooleanProperty isShowingProperty = (ReadOnlyBooleanProperty) observable;
        Menu menu = (Menu) isShowingProperty.getBean();

        if (wasShowing && ! isShowing) {
            // hide the submenu popup
            hideSubmenu();
        } else if (! wasShowing && isShowing) {
            // show the submenu popup
            showSubmenu(menu);
        }
    };

    private ListChangeListener<MenuItem> contextMenuItemsListener = (ListChangeListener<MenuItem>) c -> {
        // Add listeners to the showing property of all menus that have
        // been added, and remove listeners from menus that have been removed
        // FIXME this is temporary - we should be adding and removing
        // listeners such that they use the one listener defined above
        // - but that can't be done until we have the bean in the
        // ObservableValue
        while (c.next()) {
            updateMenuShowingListeners(c.getRemoved(), false);
            updateMenuShowingListeners(c.getAddedSubList(), true);
        }

        // Listener to items in PopupMenu to update items in PopupMenuContent
        itemsDirty = true;
        updateItems(); // RT-29761
    };

    private ChangeListener<Boolean> menuItemVisibleListener = (observable, oldValue, newValue) -> {
        // re layout as item's visibility changed
        requestLayout();
    };

    private void updateMenuShowingListeners(List<? extends MenuItem> items, boolean addListeners) {
        for (MenuItem item : items) {
            if (item instanceof Menu) {
                final Menu menu = (Menu) item;

                if (addListeners) {
                    menu.showingProperty().addListener(menuShowingListener);
                } else {
                    menu.showingProperty().removeListener(menuShowingListener);
                }
            }

             // listen to menu items's visible property.
            if (addListeners) {
                item.visibleProperty().addListener(menuItemVisibleListener);
            } else {
                item.visibleProperty().removeListener(menuItemVisibleListener);
            }
        }
    }

    // For test purpose only
    ContextMenu getSubMenu() {
        return submenu;
    }

    Menu getOpenSubMenu() {
        return openSubmenu;
    }

    private void createSubmenu() {
        if (submenu == null) {
            submenu = new ContextMenu();
            submenu.showingProperty().addListener(new ChangeListener<Boolean>() {
                @Override public void changed(ObservableValue<? extends Boolean> observable,
                                              Boolean oldValue, Boolean newValue) {
                    if (!submenu.isShowing()) {
                        // Maybe user clicked outside or typed ESCAPE.
                        // Make sure menus are in sync.
                        for (Node node : itemsContainer.getChildren()) {
                            if (node instanceof MenuItemContainer
                                  && ((MenuItemContainer)node).item instanceof Menu) {
                                Menu menu = (Menu)((MenuItemContainer)node).item;
                                if (menu.isShowing()) {
                                    menu.hide();
                                }
                            }
                        }
                    }
                }
            });
        }
    }
   
    private void showSubmenu(Menu menu) {
        openSubmenu = menu;
        createSubmenu();
        submenu.getItems().setAll(menu.getItems());
        submenu.show(selectedBackground, Side.RIGHT, 0, 0);
    }
    
    private void hideSubmenu() {
        if (submenu == null) return;

        submenu.hide();
        openSubmenu = null;

        // Fix for RT-37022 - we dispose content so that we do not process CSS
        // on hidden submenus
        disposeContextMenu(submenu);
        submenu = null;
    }
    
    private void hideAllMenus(MenuItem item) {
        if (contextMenu != null) contextMenu.hide();
        
        Menu parentMenu;
        while ((parentMenu = item.getParentMenu()) != null) {
            parentMenu.hide();
            item = parentMenu;
        }
        if (item.getParentPopup() != null) {
            item.getParentPopup().hide();
        }
    }
    
    private Menu openSubmenu;
    private ContextMenu submenu;
    
    // FIXME: HACKY. We use this so that a submenu knows where to open from
    // but this will only work for mouse hovers currently - and won't work
    // programmatically.
    // package protected for testing only!
    Region selectedBackground;
    
    void scroll(double delta) {
        double newTy = ty + delta;
        if (ty == newTy) return;

        // translation should never be positive (this would mean the top of the
        // menu content is detaching from the top of the menu!)
        if (newTy > 0.0) {
            newTy = 0.0;
        }

        // translation should never be greater than the preferred height of the
        // menu content (otherwise the menu content will be detaching from the
        // bottom of the menu).
        // RT-37185: We check the direction of the scroll, to prevent it locking
        // up when scrolling upwards from the very bottom (using the on-screen
        // up arrow).
        if (delta < 0 && (getHeight() - newTy) > itemsContainer.getHeight() - downArrow.getHeight()) {
            newTy = getHeight() - itemsContainer.getHeight() - downArrow.getHeight();
        }

        ty = newTy;
        itemsContainer.requestLayout();
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/
    @Override public Styleable getStyleableParent() {
        return contextMenu;
    }

     /** @treatAsPrivate */
    private static class StyleableProperties {

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Region.getClassCssMetaData());

            //
            // SkinBase only has Region's unique StlyleableProperty's, none of Nodes
            // So, we need to add effect back in. The effect property is in a
            // private inner class, so get the property from Node the hard way.
            final List<CssMetaData<? extends Styleable, ?>> nodeStyleables = Node.getClassCssMetaData();
            for(int n=0, max=nodeStyleables.size(); n<max; n++) {
                CssMetaData<? extends Styleable, ?> styleable = nodeStyleables.get(n);
                if ("effect".equals(styleable.getProperty())) {
                    styleables.add(styleable);
                    break;
                }
            }
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }
    
    protected Label getLabelAt(int index) {
        return ((MenuItemContainer)itemsContainer.getChildren().get(index)).getLabel();
    }

    /**
     * Custom VBox to enable scrolling of items. Scrolling effect is achieved by
     * controlling the translate Y coordinate of the menu item "ty" which is set by a
     * timeline when mouse is over up/down arrow.
     */
    class MenuBox extends VBox {
        MenuBox() {
            setAccessibleRole(AccessibleRole.CONTEXT_MENU);
        }

        @Override protected void layoutChildren() {
            double yOffset = ty;
            for (Node n : getChildren()) {
                if (n.isVisible()) {
                    final double prefHeight = snapSize(n.prefHeight(-1));
                    n.resize(snapSize(getWidth()), prefHeight);
                    n.relocate(snappedLeftInset(), yOffset);
                    yOffset += prefHeight;
                }
            }
        }

        @Override
        public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
            switch (attribute) {
                case VISIBLE: return contextMenu.isShowing();
                case PARENT_MENU: return contextMenu.getOwnerNode();
                default: return super.queryAccessibleAttribute(attribute, parameters); 
            }
        }
    }

    class ArrowMenuItem extends StackPane {
         private StackPane upDownArrow;
         private ContextMenuContent popupMenuContent;
         private boolean up = false;
         public final boolean isUp() { return up; }
         public void setUp(boolean value) {
            up = value;
            upDownArrow.getStyleClass().setAll(isUp() ? "menu-up-arrow" : "menu-down-arrow");
        }

        // used to automatically scroll through menu items when the user performs
        // certain interactions, e.g. pressing and holding the arrow buttons
        private Timeline scrollTimeline;

        public ArrowMenuItem(ContextMenuContent pmc) {
            getStyleClass().setAll("scroll-arrow");
            upDownArrow = new StackPane();
            this.popupMenuContent = pmc;
            upDownArrow.setMouseTransparent(true);
            upDownArrow.getStyleClass().setAll(isUp() ? "menu-up-arrow" : "menu-down-arrow");
    //        setMaxWidth(Math.max(upDownArrow.prefWidth(-1), getWidth()));
            addEventHandler(MouseEvent.MOUSE_ENTERED, me -> {
                if (scrollTimeline != null && (scrollTimeline.getStatus() != Status.STOPPED)) {
                    return;
                }
                startTimeline();
            });
            addEventHandler(MouseEvent.MOUSE_EXITED, me -> {
                stopTimeline();
            });
            setVisible(false);
            setManaged(false);
            getChildren().add(upDownArrow);
        }

        @Override protected double computePrefWidth(double height) {
//            return snapSize(getInsets().getLeft()) + snapSize(getInsets().getRight());
            return itemsContainer.getWidth();
        }

        @Override protected double computePrefHeight(double width) {
            return snappedTopInset() + upDownArrow.prefHeight(-1) + snappedBottomInset();
        }

        @Override protected void layoutChildren() {
            double w = snapSize(upDownArrow.prefWidth(-1));
            double h = snapSize(upDownArrow.prefHeight(-1));

            upDownArrow.resize(w, h);
            positionInArea(upDownArrow, 0, 0, getWidth(), getHeight(),
                    /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
        }

        private void adjust() {
            if(up) popupMenuContent.scroll(12); else popupMenuContent.scroll(-12);
        }

        private void startTimeline() {
            scrollTimeline = new Timeline();
            scrollTimeline.setCycleCount(Timeline.INDEFINITE);
            KeyFrame kf = new KeyFrame(
                Duration.millis(60),
                    event -> {
                        adjust();
                    }
            );
            scrollTimeline.getKeyFrames().clear();
            scrollTimeline.getKeyFrames().add(kf);
            scrollTimeline.play();
        }

        private void stopTimeline() {
            scrollTimeline.stop();
            scrollTimeline = null;
        }
    }
    
    /*
     * Container responsible for laying out a single row in the menu - in other
     * words, this contains and lays out a single MenuItem, regardless of it's 
     * specific subtype.
     */
    public class MenuItemContainer extends Region {

        private final MenuItem item;

        private Node left;
        private Node graphic;
        private Node label;
        private Node right;

        private final MultiplePropertyChangeListenerHandler listener = 
            new MultiplePropertyChangeListenerHandler(param -> {
                handlePropertyChanged(param);
                return null;
            });

        private EventHandler<MouseEvent> mouseEnteredEventHandler;
        private EventHandler<MouseEvent> mouseReleasedEventHandler;

        private EventHandler<ActionEvent> actionEventHandler;

        protected Label getLabel(){
            return (Label) label;
        }
        
        public MenuItem getItem() {
            return item;
        }

        public MenuItemContainer(MenuItem item){
            if (item == null) {
                throw new NullPointerException("MenuItem can not be null");
            }
            
            getStyleClass().addAll(item.getStyleClass());
            setId(item.getId());
            setFocusTraversable(!(item instanceof CustomMenuItem));
            this.item = item;

            createChildren();
            
            // listen to changes in the state of certain MenuItem types
            ReadOnlyBooleanProperty pseudoProperty;
            if (item instanceof Menu) {
                pseudoProperty = ((Menu)item).showingProperty();
                listener.registerChangeListener(pseudoProperty, "MENU_SHOWING");
                pseudoClassStateChanged(SELECTED_PSEUDOCLASS_STATE, pseudoProperty.get());
                setAccessibleRole(AccessibleRole.MENU);
            } else if (item instanceof RadioMenuItem) {
                pseudoProperty = ((RadioMenuItem)item).selectedProperty();
                listener.registerChangeListener(pseudoProperty, "RADIO_ITEM_SELECTED");
                pseudoClassStateChanged(CHECKED_PSEUDOCLASS_STATE, pseudoProperty.get());
                setAccessibleRole(AccessibleRole.RADIO_MENU_ITEM);
            } else if (item instanceof CheckMenuItem) {
                pseudoProperty = ((CheckMenuItem)item).selectedProperty();
                listener.registerChangeListener(pseudoProperty, "CHECK_ITEM_SELECTED");
                pseudoClassStateChanged(CHECKED_PSEUDOCLASS_STATE, pseudoProperty.get());
                setAccessibleRole(AccessibleRole.CHECK_MENU_ITEM);
            } else {
                setAccessibleRole(AccessibleRole.MENU_ITEM);
            }
            
            pseudoClassStateChanged(DISABLED_PSEUDOCLASS_STATE, item.disableProperty().get());
            listener.registerChangeListener(item.disableProperty(), "DISABLE");
            
            // Add the menu item to properties map of this node. Used by QA for testing
            // This allows associating this container with corresponding MenuItem.
            getProperties().put(MenuItem.class, item);
            
            listener.registerChangeListener(item.graphicProperty(), "GRAPHIC");

            actionEventHandler = e -> {
                if (item instanceof Menu) {
                    final Menu menu = (Menu) item;
                    if (openSubmenu == menu && submenu.isShowing()) return;
                    if (openSubmenu != null) {
                        hideSubmenu();
                    }

                    selectedBackground = MenuItemContainer.this;
                    showMenu(menu);
                } else {
                    doSelect();
                }
            };
            addEventHandler(ActionEvent.ACTION, actionEventHandler);
        }
        
        public void dispose() {
            if (item instanceof CustomMenuItem) {
                Node node = ((CustomMenuItem)item).getContent();
                if (node != null) {
                    node.removeEventHandler(MouseEvent.MOUSE_CLICKED, customMenuItemMouseClickedHandler);
                }
            }

            listener.dispose();
            removeEventHandler(ActionEvent.ACTION, actionEventHandler);
            
            if (label != null) {
                ((Label)label).textProperty().unbind();
            }
            
            left = null;
            graphic = null;
            label = null;
            right = null;
        }
        
        private void handlePropertyChanged(String p) {
            if ("MENU_SHOWING".equals(p)) {
                Menu menu = (Menu) item;
                pseudoClassStateChanged(SELECTED_PSEUDOCLASS_STATE, menu.isShowing());
            } else if ("RADIO_ITEM_SELECTED".equals(p)) {
                RadioMenuItem radioItem = (RadioMenuItem) item;
                pseudoClassStateChanged(CHECKED_PSEUDOCLASS_STATE, radioItem.isSelected());
            } else if ("CHECK_ITEM_SELECTED".equals(p)) {
                CheckMenuItem checkItem = (CheckMenuItem) item;
                pseudoClassStateChanged(CHECKED_PSEUDOCLASS_STATE, checkItem.isSelected());
            } else if ("DISABLE".equals(p)) {
                pseudoClassStateChanged(DISABLED_PSEUDOCLASS_STATE, item.isDisable());
            } else if ("GRAPHIC".equals(p)) {
                createChildren();
                computeVisualMetrics();
            } else if ("ACCELERATOR".equals(p)) {
                updateAccelerator();
            } else if ("FOCUSED".equals(p)) {
                if (isFocused()) {
                    currentFocusedIndex = itemsContainer.getChildren().indexOf(MenuItemContainer.this);
                }
            }
        }
        
        private void createChildren() {
            getChildren().clear();
            
            // draw background region for hover effects. All content (other
            // than Nodes from NodeMenuItems) are set to be mouseTransparent, so
            // this background also acts as the receiver of user input
            if (item instanceof CustomMenuItem) {
                createNodeMenuItemChildren((CustomMenuItem)item);

                if (mouseEnteredEventHandler == null) {
                    mouseEnteredEventHandler = event -> {
                        requestFocus(); // request Focus on hover
                    };
                } else {
                    removeEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnteredEventHandler);
                }
                addEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnteredEventHandler);
            } else {
                // --- add check / radio to left column
                Node leftNode = getLeftGraphic(item);
                if (leftNode != null) {
                    StackPane leftPane = new StackPane();
                    leftPane.getStyleClass().add("left-container");
                    leftPane.getChildren().add(leftNode);
                    left = leftPane;
                    getChildren().add(left);
                    left.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                }
                // -- add graphic to graphic pane
                if (item.getGraphic() != null) {
                    Node graphicNode = item.getGraphic();
                    StackPane graphicPane = new StackPane();
                    graphicPane.getStyleClass().add("graphic-container");
                    graphicPane.getChildren().add(graphicNode);
                    graphic = graphicPane;
                    getChildren().add(graphic);
                }
                
                // --- add text to center column
                label = new MenuLabel(item, this);  // make this a menulabel to handle mnemonics fire()
                label.setStyle(item.getStyle());

                // bind to text property in menu item
                ((Label)label).textProperty().bind(item.textProperty());

                label.setMouseTransparent(true);
                getChildren().add(label);

                listener.unregisterChangeListener(focusedProperty());
                // RT-19546 update currentFocusedIndex when MenuItemContainer gets focused.
                // e.g this happens when you press the Right key to open a submenu; the first
                // menuitem is focused.
                listener.registerChangeListener(focusedProperty(), "FOCUSED");

                // --- draw in right column - this depends on whether we are
                // a Menu or not. A Menu gets an arrow, whereas other MenuItems
                // get the ability to draw an accelerator
                if (item instanceof Menu) {
                    // --- add arrow / accelerator / mnemonic to right column
                    Region rightNode = new Region();
                    rightNode.setMouseTransparent(true);
                    rightNode.getStyleClass().add("arrow");
                    
                    StackPane rightPane = new StackPane();
                    rightPane.setMaxWidth(Math.max(rightNode.prefWidth(-1), 10));
                    rightPane.setMouseTransparent(true);
                    rightPane.getStyleClass().add("right-container");
                    rightPane.getChildren().add(rightNode);
                    right = rightPane;
                    getChildren().add(rightPane);

                    if (mouseEnteredEventHandler == null) {
                        mouseEnteredEventHandler = event -> {
                            if (openSubmenu != null && item != openSubmenu) {
                                // if a submenu of a different menu is already
                                // open then close it (RT-15049)
                                hideSubmenu();
                            }

                            final Menu menu = (Menu) item;
                            if (menu.isDisable()) return;
                            selectedBackground = MenuItemContainer.this;
                            menu.show();
                            requestFocus();  // request Focus on hover
                        };
                    } else {
                        removeEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnteredEventHandler);
                    }

                    if (mouseReleasedEventHandler == null) {
                        mouseReleasedEventHandler = event -> {
                            item.fire();
                        };
                    } else {
                        removeEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedEventHandler);
                    }
                    
                    // show submenu when the menu is hovered over
                    addEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnteredEventHandler);
                    addEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedEventHandler);
                } else { // normal MenuItem
                    // remove old listeners
                    listener.unregisterChangeListener(item.acceleratorProperty());

                    // accelerator support
                    updateAccelerator();

                    if (mouseEnteredEventHandler == null) {
                        mouseEnteredEventHandler = event -> {
                            if (openSubmenu != null) {
                                openSubmenu.hide();
                            }
                            requestFocus();  // request Focus on hover
                        };
                    } else {
                        removeEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnteredEventHandler);
                    }

                    if (mouseReleasedEventHandler == null) {
                        mouseReleasedEventHandler = event -> {
                            doSelect();
                        };
                    } else {
                        removeEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedEventHandler);
                    }

                    addEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnteredEventHandler);
                    addEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedEventHandler);

                    listener.registerChangeListener(item.acceleratorProperty(), "ACCELERATOR");
                }
            }
        }
        
        private void updateAccelerator() {
            if (item.getAccelerator() != null) {
                if (right != null) {
                    getChildren().remove(right);
                }

                String text = item.getAccelerator().getDisplayText();
                right = new Label(text);
                right.setStyle(item.getStyle());
                right.getStyleClass().add("accelerator-text");
                getChildren().add(right);
            } else {
                getChildren().remove(right);
            }
        }

        void doSelect() {
            // don't do anything on disabled menu items
            if (item.isDisable()) return;
            // toggle state of check or radio items
            if (item instanceof CheckMenuItem) {
                CheckMenuItem checkItem = (CheckMenuItem)item;
                checkItem.setSelected(!checkItem.isSelected());
            } else if (item instanceof RadioMenuItem) {
                // this is a radio button. If there is a toggleGroup specified, we
                // simply set selected to true. If no toggleGroup is specified, we
                // toggle the selected state, as there is no assumption of mutual
                // exclusivity when no toggleGroup is set.
                final RadioMenuItem radioItem = (RadioMenuItem) item;
                radioItem.setSelected(radioItem.getToggleGroup() != null ? true : !radioItem.isSelected());
            }

            // fire the action before hiding the menu
            item.fire();

            if (item instanceof CustomMenuItem) {
                CustomMenuItem customMenuItem = (CustomMenuItem) item;
                if (customMenuItem.isHideOnClick()) {
                    hideAllMenus(item);
                }
            } else {
                hideAllMenus(item);
            }
        }

        private EventHandler<MouseEvent> customMenuItemMouseClickedHandler;
        
        private void createNodeMenuItemChildren(final CustomMenuItem item) {
            Node node = item.getContent();
            getChildren().add(node);

            // handle hideOnClick
            customMenuItemMouseClickedHandler = event -> {
                if (item == null || item.isDisable()) return;

                item.fire();
                if (item.isHideOnClick()) {
                    hideAllMenus(item);
                }
            };
            node.addEventHandler(MouseEvent.MOUSE_CLICKED, customMenuItemMouseClickedHandler);
        }
        
        @Override protected void layoutChildren() {
            double xOffset;
        
            final double prefHeight = prefHeight(-1);
            if (left != null) {
                xOffset = snappedLeftInset();
                left.resize(left.prefWidth(-1), left.prefHeight(-1));
                positionInArea(left, xOffset, 0,
                        maxLeftWidth, prefHeight, 0, HPos.LEFT, VPos.CENTER);
            }
            if (graphic != null) {
                xOffset = snappedLeftInset() + maxLeftWidth;
                graphic.resize(graphic.prefWidth(-1), graphic.prefHeight(-1));
                positionInArea(graphic, xOffset, 0,
                        maxGraphicWidth, prefHeight, 0, HPos.LEFT, VPos.CENTER);
            }
            
            if (label != null) {
                xOffset = snappedLeftInset() + maxLeftWidth + maxGraphicWidth;
                label.resize(label.prefWidth(-1), label.prefHeight(-1));
                positionInArea(label, xOffset, 0,
                        maxLabelWidth, prefHeight, 0, HPos.LEFT, VPos.CENTER);
            }
            
            if (right != null) {
                xOffset = snappedLeftInset() + maxLeftWidth + maxGraphicWidth + maxLabelWidth;
                right.resize(right.prefWidth(-1), right.prefHeight(-1));
                positionInArea(right, xOffset, 0,
                    maxRightWidth, prefHeight, 0, HPos.RIGHT, VPos.CENTER);
            }
            
            if ( item instanceof CustomMenuItem) {
                Node n = ((CustomMenuItem) item).getContent();
                if (item instanceof SeparatorMenuItem) {
                    double width = prefWidth(-1) - (snappedLeftInset() + maxGraphicWidth + snappedRightInset());
                    n.resize(width, n.prefHeight(-1));
                    positionInArea(n, snappedLeftInset() + maxGraphicWidth, 0, prefWidth(-1), prefHeight, 0, HPos.LEFT, VPos.CENTER);
                } else {
                    n.resize(n.prefWidth(-1), n.prefHeight(-1));
                    //the node should be left aligned 
                    positionInArea(n, snappedLeftInset(), 0, getWidth(), prefHeight, 0, HPos.LEFT, VPos.CENTER);
                }
            }
        }
        
        @Override protected double computePrefHeight(double width) {
            double prefHeight = 0;
            if (item instanceof CustomMenuItem || item instanceof SeparatorMenuItem) {
                prefHeight = (getChildren().isEmpty()) ? 0 : getChildren().get(0).prefHeight(-1);
            } else {
                prefHeight = Math.max(prefHeight, (left != null) ? left.prefHeight(-1) : 0);
                prefHeight = Math.max(prefHeight, (graphic != null) ? graphic.prefHeight(-1) : 0);
                prefHeight = Math.max(prefHeight, (label != null) ? label.prefHeight(-1) : 0);
                prefHeight = Math.max(prefHeight, (right != null) ? right.prefHeight(-1) : 0);
            }
             return snappedTopInset() + prefHeight + snappedBottomInset();
        }

        @Override protected double computePrefWidth(double height) {
            double nodeMenuItemWidth = 0;
            if (item instanceof CustomMenuItem && !(item instanceof SeparatorMenuItem)) {
                nodeMenuItemWidth = snappedLeftInset() + ((CustomMenuItem) item).getContent().prefWidth(-1) +
                        snappedRightInset();
            }
            return Math.max(nodeMenuItemWidth,
                    snappedLeftInset() + maxLeftWidth + maxGraphicWidth +
                    maxLabelWidth + maxRightWidth + snappedRightInset());
        }

        // Responsible for returning a graphic (if necessary) to position in the
        // left column of the menu. This may be a Node from the MenuItem.graphic
        // property, or it may be a check/radio item if necessary.
        private Node getLeftGraphic(MenuItem item) {
            if (item instanceof RadioMenuItem) {
                 final Region _graphic = new Region();
                _graphic.getStyleClass().add("radio");
                return _graphic;
            } else if (item instanceof CheckMenuItem) {
                final StackPane _graphic = new StackPane();
                _graphic.getStyleClass().add("check");
                return _graphic;
            } 
            
            return null;
        }

        @Override
        public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
            switch (attribute) {
                case SELECTED:
                    if (item instanceof CheckMenuItem) {
                        return ((CheckMenuItem)item).isSelected();
                    }
                    if (item instanceof RadioMenuItem) {
                        return ((RadioMenuItem) item).isSelected();
                    }
                    return false;
                case ACCELERATOR: return item.getAccelerator();
                case TEXT: {
                    String title = "";
                    if (graphic != null) {
                        String t = (String)graphic.queryAccessibleAttribute(AccessibleAttribute.TEXT);
                        if (t != null) title += t;
                    }                  
                    final Label label = getLabel();
                    if (label != null) {
                        String t = (String)label.queryAccessibleAttribute(AccessibleAttribute.TEXT);
                        if (t != null) title += t;
                    }
                    if (item instanceof CustomMenuItem) {
                        Node content = ((CustomMenuItem) item).getContent();
                        if (content != null) {
                            String t = (String)content.queryAccessibleAttribute(AccessibleAttribute.TEXT);
                            if (t != null) title += t;
                        }
                    }
                    return title;
                }
                case MNEMONIC: {
                    final Label label = getLabel();
                    if (label != null) {
                        String mnemonic = (String)label.queryAccessibleAttribute(AccessibleAttribute.MNEMONIC);
                        if (mnemonic != null) return mnemonic;
                    }
                    return null;
                }
                case DISABLED: return item.isDisable();
                case SUBMENU:
                    createSubmenu();
                    // Accessibility might need to see the menu node before the window
                    // is visible (i.e. before the skin is applied).
                    if (submenu.getSkin() == null) {
                        submenu.impl_styleableGetNode().impl_processCSS(true);
                    }
                    ContextMenuContent cmContent = (ContextMenuContent)submenu.getSkin().getNode();
                    return cmContent.itemsContainer;
                default: return super.queryAccessibleAttribute(attribute, parameters); 
            }
        }

        @Override
        public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
            switch (action) {
                case SHOW_MENU:{
                    if (item instanceof Menu) {
                        final Menu menuItem = (Menu) item;
                        if (menuItem.isShowing()) {
                            menuItem.hide();
                        } else {
                            menuItem.show();
                        }
                    }
                    break;
                }
                case FIRE: 
                    doSelect();
                    break;
                default: super.executeAccessibleAction(action);
            }
        }
    }


    private static final PseudoClass SELECTED_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("selected");
    private static final PseudoClass DISABLED_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("disabled");
    private static final PseudoClass CHECKED_PSEUDOCLASS_STATE =
            PseudoClass.getPseudoClass("checked");

    private class MenuLabel extends Label {

        public MenuLabel(MenuItem item, MenuItemContainer mic) {
            super(item.getText());
            setMnemonicParsing(item.isMnemonicParsing());
            setLabelFor(mic);
        }
    }
        
}
