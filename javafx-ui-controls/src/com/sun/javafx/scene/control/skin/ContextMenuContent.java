/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.javafx.scene.control.skin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import com.sun.javafx.css.CssMetaData;
import com.sun.javafx.css.PseudoClass;
import java.util.Iterator;
import javafx.geometry.NodeOrientation;
import javafx.stage.Window;

import com.sun.javafx.scene.control.behavior.TwoLevelFocusPopupBehavior;

/**
 * This is a the SkinBase for PopupMenu based controls so that the CSS parts
 * work right, because otherwise we would have to copy the Keys from there to here.
 * {@link PopupControlSkin} should be changed to use SkinBase instead of Resizable
 * for its content and popup content.
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
    
    private TwoLevelFocusPopupBehavior tlFocus;

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
        computeInitialSize();
        initialize();
        setUpBinds();
        // RT-20197 add menuitems only on first show.
        popupMenu.showingProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable arg0) {
                updateItems();
            }
        });

        /*
        ** only add this if we're on an embedded
        ** platform that supports 5-button navigation 
        */
        if (Utils.isEmbeddedNonTouch()) {
            tlFocus = new TwoLevelFocusPopupBehavior(this);
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

            if (getScene() != null) {
                impl_processCSS(true);
            }
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
    }
    
    private void updateVisualItems() {
        itemsContainer.getChildren().clear();
        for (int row = 0; row < getItems().size(); row++) {
            final MenuItem item = getItems().get(row);
            if (item instanceof CustomMenuItem && ((CustomMenuItem) item).getContent() == null) continue;
            MenuItemContainer menuItemContainer = new MenuItemContainer(item);
            menuItemContainer.visibleProperty().bind(item.visibleProperty());
            itemsContainer.getChildren().add(menuItemContainer);
            if (item instanceof SeparatorMenuItem) {
                // TODO
                // we don't want the hover highlight for separators, so for
                // now this is the simplest approach - just remove the
                // background entirely. This may cause issues if people
                // intend to style the background differently.
                 Node node = ((CustomMenuItem) item).getContent();
                 itemsContainer.getChildren().remove(menuItemContainer);
                 itemsContainer.getChildren().add(node);
                 // Add the (separator) menu item to properties map of this node.
                // Special casing this for separator :
                // This allows associating this container with SeparatorMenuItem.
                node.getProperties().put(MenuItem.class, item);
            }
        }
        // Add the Menu to properties map of this skin. Used by QA for testing
        // This enables associating a parent menu for this skin showing menu items.
        if (getItems().size() > 0) {
            final MenuItem item = getItems().get(0);
            getProperties().put(Menu.class, item.getParentMenu());
        }
    }

    @Override protected void layoutChildren() {
        if (itemsContainer.getChildren().size() == 0) return;
        final double x = snapSpace(getInsets().getLeft());
        final double y = snapSpace(getInsets().getTop());
        final double w = snapSize(getWidth()) - (snapSpace(getInsets().getLeft()) + snapSpace(getInsets().getRight()));
        final double h = snapSize(getHeight()) - (snapSpace(getInsets().getTop()) + snapSpace(getInsets().getBottom()));
        final double contentHeight =  snapSize(getContentHeight()); // itemsContainer.prefHeight(-1);

        itemsContainer.resize(w,contentHeight);
        itemsContainer.relocate(snapSpace(getInsets().getLeft()), y);

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
         return snapSize(getInsets().getLeft()) + snapSize(prefWidth) + snapSize(getInsets().getRight());
    }

    @Override protected double computePrefHeight(double width) {
        if (itemsContainer.getChildren().size() == 0) return 0;
        final double screenHeight = getScreenHeight();
        final double contentHeight = getContentHeight(); // itemsContainer.prefHeight(width);
        double totalHeight = snapSpace(getInsets().getTop()) + snapSize(contentHeight) + snapSpace(getInsets().getBottom());
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
        return snapSize(com.sun.javafx.Utils.getScreen(
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
  
//    // FIXME: This handles shifting ty when doing keyboard navigation.
//    // By no means is this the best way to do this, but it works for now.
//    private Node focusedItem;
//    public Node getFocusedItem() { return focusedItem; }
//
//    private void setFocusedItem(Node node) {
//        focusedItem = node;
//        if (focusedItem != null) {
//            // this is for moving down the menu
//            if (focusedItem.getBoundsInParent().getMaxY() >= clipRect.getBoundsInParent().getMaxY()) {
//                watchMouseHover = false;
//                ty = ty - focusedItem.getBoundsInParent().getMaxY() + clipRect.getBoundsInParent().getMaxY();
//            } else // this is for moving up the menu
//            if (focusedItem.getBoundsInParent().getMinY() <= clipRect.getBoundsInParent().getMinY()) {
//                watchMouseHover = false;
//                ty = ty - focusedItem.getBoundsInParent().getMinY() + clipRect.getBoundsInParent().getMinY();
//            }
//        }
//    }
    
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
    private void setTy(double value) {
        if (ty == value) return;
        ty = value;
        itemsContainer.requestLayout();
    }

    /**
    * Optimization part of RT-20197. In order to match the width of the choiceBox
    * with the width of the widest menu item, we get the index of the widest 
    * menuItem and add it to the itemsContainer so the visual metrics is calculated 
    * correctly even though all the items are not added before show.
    * This item will be removed when the first show happens.
    */
    private void computeInitialSize() {
        int index = getLongestLabel();
        itemsContainer.getChildren().clear();
        if (!getItems().isEmpty()) {
            final MenuItem item = getItems().get(index);
            MenuItemContainer menuItemContainer = new MenuItemContainer(item);
            itemsContainer.getChildren().add(menuItemContainer);
        }
    }
    
    private int getLongestLabel() {
        int len = 0;
        int index = 0;
        for (int row = 0; row < getItems().size(); row++) {
            final MenuItem item = getItems().get(row);
            if ((item instanceof CustomMenuItem && ((CustomMenuItem) item).getContent() == null) ||
                    item instanceof SeparatorMenuItem)  continue;
            if ( item != null && item.getText() != null && item.getText().length() > len) {
                index = row;
                len =  item.getText().length();
            }
        }
        return index;
    }
//    /**
//     * When we have a scrollable menu, and the mouse is left hovering over a menu
//     * item when the user is scrolling using keyboard up/down arrows, as soon as
//     * the user forces a scroll we get a new node hover event, which makes the
//     * selection jump back to whatever is under the mouse. This prevents keyboard
//     * navigation from working in this circumstance.
//     *
//     * To work around this we use this boolean. Just prior to doing a scroll
//     * we turn off mouse hover watching. When this is false, we don't look for
//     * new hoveredItem's. As soon as the mouse moves over the skin we start
//     * watching for hover events again.
//     */
//    private boolean watchMouseHover = true;
   
    private void initialize() {
        // keyboard navigation support. Initially focus goes to this ContextMenu,
        // but when the user first hits the up or down arrow keys, the focus
        // is transferred to the first or last item respectively. Once this
        // happens, it is up to the menu items to navigate between themselves.
        contextMenu.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    // initialize the focused index for keyboard navigation.
                    currentFocusedIndex = -1;
                    requestFocus();
                }
            }
        });
        
        // RT-19624 calling requestFocus inside layout was casuing repeated layouts.
        contextMenu.addEventHandler(Menu.ON_SHOWN, new EventHandler() {
            @Override public void handle(Event event) {
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
            }
        });

//        // FIXME For some reason getSkinnable()Behavior traversal functions don't
//        // get called as expected, so I've just put the important code below.
        setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent ke) {
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
                        // event not consumed for ESCAPE to let the Popup hide
                        // on it
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
                }
            }
        });

        setOnScroll(new EventHandler<javafx.scene.input.ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                /*
                ** we'll only scroll is the arrows are visible in the direction
                ** that we're going, otherwise we go into empty space.
                */
                if ((downArrow.isVisible() && (event.getTextDeltaY() < 0.0 || event.getDeltaY() < 0.0)) ||
                    (upArrow.isVisible() && (event.getTextDeltaY() > 0.0 || event.getDeltaY() > 0.0))) {

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
                          scroll(event.getTextDeltaY()*rowHeight);
                          break;
                      case PAGES:
                          /*
                          ** page scroll, scroll the menu height
                          */
                          scroll(event.getTextDeltaY()*itemsContainer.getHeight());
                          break;
                      case NONE:
                          /*
                          ** pixel scroll
                          */
                          scroll(event.getDeltaY());
                          break;
                    }
                    event.consume();
                }
            }
        });

        addEventFilter(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                // RT-15050 : if a submenu is open then don't do request focus
                // on this StackPane, do nothing so that focus continues to stay
                // on the Menu whose submenu is open.
                if (submenu != null && submenu.isShowing()) return;
                  
                // RT-15339 : if this context menu has another context menu part 
                // of its content, then do nothing similar to the submenu case.
                // walk thru all windows open to see if if this context menu is
                // the owner window for another context menu, if so return.
                Iterator<Window> iter = Window.impl_getWindows(); 
                while(iter.hasNext()) {
                    Window w = iter.next();
                    if (w instanceof ContextMenu && !(contextMenu == w)) {
                        Window ownerWin = ((ContextMenu)w).getOwnerWindow();
                        if (contextMenu == ownerWin && ownerWin.isShowing()) {
                            return;
                        }
                        
                    }
                    
                }
                requestFocus();
            }
        });
//        addEventFilter(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                watchMouseHover = true;
//            }
//        });
    }

    private void processLeftKey(KeyEvent ke) {
        if (currentFocusedIndex != -1) {
            Node n = itemsContainer.getChildren().get(currentFocusedIndex);
            if (n instanceof MenuItemContainer) {
                MenuItem item = ((MenuItemContainer)n).item;
                if (item instanceof Menu) {
                    final Menu menu = (Menu) item;
                    if (menu == openSubmenu) {
                        if( submenu != null && submenu.isShowing() ) {
                            hideSubmenu();
                            ke.consume();
                        }
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
                    if (openSubmenu == menu && submenu.isShowing()) return;
                    menu.show();
                    // request focus on the first item of the submenu after it is shown
                    ContextMenuContent cmContent = (ContextMenuContent)submenu.getSkin().getNode();
                    if (cmContent != null) {
                       if (cmContent.itemsContainer.getChildren().size() > 0) {
                           ((MenuItemContainer)(cmContent.itemsContainer.getChildren().get(0))).requestFocus();
                       } else {
                           cmContent.requestFocus();
                       }
                    }
                    ke.consume();
                }
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
                return(i);
            } else {
                continue;
            }
        }
        // find from top
        for (int i = 0; i < from; i++) {
            Node n = itemsContainer.getChildren().get(i);
            if (n instanceof MenuItemContainer) {
                return(i);
            } else {
                continue;
            }
        }
        return -1; // should not happen
    }
    
    private void moveToNextSibling() {
        currentFocusedIndex = findFocusedIndex();
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
            ((MenuItemContainer)(itemsContainer.getChildren().get(currentFocusedIndex))).requestFocus();
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
            } else {
                continue;
            }
        }
        for (int i = itemsContainer.getChildren().size() - 1 ; i > from; i--) {
            Node n = itemsContainer.getChildren().get(i);
            if (n instanceof MenuItemContainer) {
                return(i);
            } else {
                continue;
            }
        }
        return -1;
    }

     private void moveToPreviousSibling() {
         currentFocusedIndex = findFocusedIndex();
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
            ((MenuItemContainer)(itemsContainer.getChildren().get(currentFocusedIndex))).requestFocus();
         }
    }

    private Menu getRootMenu(Menu menu) {
        if (menu == null || menu.getParentMenu() == null) {
            return menu;
        }
        Menu parentMenu = menu.getParentMenu();
        while (parentMenu.getParentMenu() != null) {
            parentMenu = parentMenu.getParentMenu();
        }
        return parentMenu;
    }

    /*
     * Get the Y offset from the top of the popup to the menu item whose index
     * is given.
     */
    double getMenuYOffset(int menuIndex) {
        double offset = 0;
        if (itemsContainer.getChildren().size() >= menuIndex) {
            offset = getInsets().getTop();
            Node menuitem = itemsContainer.getChildren().get(menuIndex);
            offset += menuitem.getLayoutY() + menuitem.prefHeight(-1);
        }
        return offset;
    }

    private void setUpBinds() {
        updateMenuShowingListeners(contextMenu.getItems());
        contextMenu.getItems().addListener(new ListChangeListener<MenuItem>() {
            @Override public void onChanged(Change<? extends MenuItem> c) {
                // Add listeners to the showing property of all menus that have
                // been added, and remove listeners from menus that have been removed
                // FIXME this is temporary - we should be adding and removing
                // listeners such that they use the one listener defined above
                // - but that can't be done until we have the bean in the 
                // ObservableValue
                while (c.next()) {
                    updateMenuShowingListeners(c.getAddedSubList());
                }

                // Listener to items in PopupMenu to update items in PopupMenuContent
                itemsDirty = true;
            }
        });
    }

    private void updateMenuShowingListeners(List<? extends MenuItem> added) {
        for (MenuItem item : added) {
            if (item instanceof Menu) {
                final Menu menuItem = (Menu) item;
                menuItem.showingProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue observable, Boolean wasShowing, Boolean isShowing) {
                        if (wasShowing && ! isShowing) {
                            // hide the submenu popup
                            hideSubmenu();
                        } else if (! wasShowing && isShowing) {
                            // show the submenu popup
                            showSubmenu(menuItem);
                        }
                    }
                });
            }
             // listen to menu items's visible property.
            item.visibleProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue observable, Boolean oldValue, Boolean newValue) {
                    // re layout as item's visibility changed
                    requestLayout();
                }
            });
        }
    }

    // For test purpose only
    ContextMenu getSubMenu() {
        return submenu;
    }

    private void showSubmenu(Menu menu) {
        openSubmenu = menu;

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

        submenu.getItems().setAll(menu.getItems());
        submenu.show(selectedBackground, Side.RIGHT, 0, 0);
    }
    
    private void hideSubmenu() {
        if (submenu == null) return;

        submenu.hide();
    }
    
    private void hideAllMenus(MenuItem item) {
        contextMenu.hide();
        
        Menu parentMenu;
        while ((parentMenu = item.getParentMenu()) != null) {
            parentMenu.hide();
            item = parentMenu;
        }
        if (parentMenu == null && item.getParentPopup() != null) {
            item.getParentPopup().hide();
        }
    }
    
    private Menu openSubmenu;
    private ContextMenu submenu;
    
    // FIXME: HACKY. We use this so that a submenu knows where to open from
    // but this will only work for mouse hovers currently - and won't work
    // programmatically.
    private Region selectedBackground;
    
    void scroll(double delta) {
        setTy(ty+delta);
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

     /** @treatAsPrivate */
    private static class StyleableProperties {

        private static final List<CssMetaData> STYLEABLES;
        static {

            final List<CssMetaData> styleables =
                new ArrayList<CssMetaData>(Region.getClassCssMetaData());

            //
            // SkinBase only has Region's unique StlyleableProperty's, none of Nodes
            // So, we need to add effect back in. The effect property is in a
            // private inner class, so get the property from Node the hard way.
            final List<CssMetaData> nodeStyleables = Node.getClassCssMetaData();
            for(int n=0, max=nodeStyleables.size(); n<max; n++) {
                CssMetaData styleable = nodeStyleables.get(n);
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
    public static List<CssMetaData> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData> getCssMetaData() {
        return getClassCssMetaData();
    }
    
    protected Label getLabelAt(int index) {
        return (Label)((MenuItemContainer)itemsContainer.getChildren().get(index)).getLabel();
    }

    /**
     * Custom VBox to enable scrolling of items. Scrolling effect is achieved by
     * controlling the translate Y coordinate of the menu item "ty" which is set by a
     * timeline when mouse is over up/down arrow.
     */
    class MenuBox extends VBox {

        @Override protected void layoutChildren() {
            double yOffset = ty;
            for (Node n : itemsContainer.getChildren()) {
                if (n.isVisible()) {
                    final double prefHeight = snapSize(n.prefHeight(-1));
                    n.resize(snapSize(getWidth()), prefHeight);
                    n.relocate(snapSpace(getInsets().getLeft()), yOffset);
                    yOffset += prefHeight;
                }
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
            setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent me) {
                    if (scrollTimeline != null && (scrollTimeline.getStatus() != Status.STOPPED)) {
                        return;
                    }
                    startTimeline();
                }
            });
             setOnMouseExited(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent me) {
                    stopTimeline();
                }
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
            return snapSize(getInsets().getTop()) + upDownArrow.prefHeight(-1) + snapSize(getInsets().getBottom());
        }

        @Override protected void layoutChildren() {
            double w = snapSize(upDownArrow.prefWidth(-1));
            double h = snapSize(upDownArrow.prefHeight(-1));

            upDownArrow.resize(w, h);
            positionInArea(upDownArrow, 0, 0, getWidth(), getHeight(),
                    /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
        }

        public Region getArrowRegion() {
            return upDownArrow;
        }

        private void adjust() {
            if(up) popupMenuContent.scroll(12); else popupMenuContent.scroll(-12);
        }

        private void startTimeline() {
            scrollTimeline = new Timeline();
            scrollTimeline.setCycleCount(Timeline.INDEFINITE);
            KeyFrame kf = new KeyFrame(
                Duration.millis(60),
                new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent event) {
                        adjust();
                    }
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
     * Container responsible for laying out a singel row in the menu - in other
     * words, this contains and lays out a single MenuItem, regardless of it's 
     * specific subtype.
     */
    public class MenuItemContainer extends Region {

        private final MenuItem item;

        private Node left;
        private Node graphic;
        private Node label;
        private Node right;

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
            this.item = item;

            createChildren();
            
            // listen to changes in the state of certain MenuItem types
            if (item instanceof Menu) {
                listen(((Menu)item).showingProperty(), PSEUDO_CLASS_SELECTED);
            } else if (item instanceof RadioMenuItem) {
                listen(((RadioMenuItem)item).selectedProperty(), PSEUDO_CLASS_CHECKED);
            } else if (item instanceof CheckMenuItem) {
                listen(((CheckMenuItem)item).selectedProperty(), PSEUDO_CLASS_CHECKED);
            }
            listen(item.disableProperty(), PSEUDO_CLASS_DISABLED);
            // Add the menu item to properties map of this node. Used by QA for testing
            // This allows associating this container with corresponding MenuItem.
            getProperties().put(MenuItem.class, item);
        }
        
        private void createChildren() {
            // draw background region for hover effects. All content (other
            // than Nodes from NodeMenuItems) are set to be mouseTransparent, so
            // this background also acts as the receiver of user input
            if (item instanceof CustomMenuItem) {
                createNodeMenuItemChildren((CustomMenuItem)item);
                setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        requestFocus(); // request Focus on hover
                    }
                });
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
                setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override public void handle(MouseEvent event) {
                        requestFocus();  // request Focus on hover
                    }
                });


                // --- draw in right column - this depends on whether we are
                // a Menu or not. A Menu gets an arrow, whereas other MenuItems
                // get the ability to draw an accelerator
                if (item instanceof Menu) {
                    final Menu menu = (Menu) item;
                    
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
                    
                    // show submenu when the menu is hovered over
                    setOnMouseEntered(new EventHandler<MouseEvent>() {
                        @Override public void handle(MouseEvent event) {
                            if (openSubmenu != null && item != openSubmenu) {
                                // if a submenu of a different menu is already
                                // open then close it (RT-15049)
                                hideSubmenu();
                            }
                            
                            if (menu.isDisable()) return;
                            selectedBackground = MenuItemContainer.this;
                            menu.show();
                            requestFocus();  // request Focus on hover
                        }
                    });
                } else { // normal MenuItem
                    // accelerator text
//                    Label rightNode = new Label("Ctrl+x");
//                    rightNode.getStyleClass().add("accelerator-text");
//
//                    StackPane rightPane = new StackPane();
//                    rightPane.setMaxWidth(Math.max(rightNode.prefWidth(-1), 10));
//                    rightPane.setMouseTransparent(true);
//                    rightPane.getStyleClass().add("right-container");
//                    rightPane.getChildren().add(rightNode);
//                    right = rightPane;
//                    getChildren().add(rightPane);
                    
                    // accelerator support
                    updateAccelerator();
                    item.acceleratorProperty().addListener(new InvalidationListener() {
                        @Override
                        public void invalidated(Observable observable) {
                            updateAccelerator();
                        }
                    });
                    
                    setOnMouseEntered(new EventHandler<MouseEvent>() {
                        @Override public void handle(MouseEvent event) {
                            if (openSubmenu != null) {
                                openSubmenu.hide();
                            }
                            requestFocus();  // request Focus on hover
                        }
                    });
                    setOnMouseReleased(new EventHandler<MouseEvent>() {
                        @Override public void handle(MouseEvent event) {
                            doSelect();
                        }
                    });
                    // RT-19546 update currentFocusedIndex when MenuItemContainer gets focused.
                    // e.g this happens when you press the Right key to open a submenu; the first
                    // menuitem is focused.
                    focusedProperty().addListener(new ChangeListener<Boolean>() {
                        @Override public void changed(ObservableValue<? extends Boolean> ov,
                                                                    Boolean t, Boolean t1) {
                            if (t1 && !t) {
                                currentFocusedIndex =
                                    itemsContainer.getChildren().indexOf(MenuItemContainer.this);
                            }
                        }
                    });
                }
            }
        }
        
        private void updateAccelerator() {
            if (item.getAccelerator() != null) {
                String text = KeystrokeUtils.toString(item.getAccelerator());
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
            if (item == null || item.isDisable()) return;
            
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
            hideAllMenus(item);
        }
        
        private void createNodeMenuItemChildren(final CustomMenuItem item) {
            Node node = ((CustomMenuItem) item).getContent();
            getChildren().add(node);
            node.getStyleClass().addAll(item.getStyleClass());
            // handle hideOnClick
            node.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    if (item == null || item.isDisable()) return;

                    item.fire();
                    if (item.isHideOnClick()) {
                        hideAllMenus(item);
                    }
                }
            });
        }
        
        @Override protected void layoutChildren() {
            double xOffset;
        
            final double prefHeight = prefHeight(-1);
            if (left != null) {
                xOffset = getInsets().getLeft();
                left.resize(left.prefWidth(-1), left.prefHeight(-1));
                positionInArea(left, xOffset, 0,
                        maxLeftWidth, prefHeight, 0, HPos.LEFT, VPos.CENTER);
            }
            if (graphic != null) {
                xOffset = getInsets().getLeft() + maxLeftWidth;
                graphic.resize(graphic.prefWidth(-1), graphic.prefHeight(-1));
                positionInArea(graphic, xOffset, 0,
                        maxGraphicWidth, prefHeight, 0, HPos.LEFT, VPos.CENTER);
            }
            
            if (label != null) {
                xOffset = getInsets().getLeft() + maxLeftWidth + maxGraphicWidth;
                label.resize(label.prefWidth(-1), label.prefHeight(-1));
                positionInArea(label, xOffset, 0,
                        maxLabelWidth, prefHeight, 0, HPos.LEFT, VPos.CENTER);
            }
            
            if (right != null) {
                xOffset = getInsets().getLeft() + maxLeftWidth + maxGraphicWidth + maxLabelWidth;
                right.resize(right.prefWidth(-1), right.prefHeight(-1));
                positionInArea(right, xOffset, 0,
                    maxRightWidth, prefHeight, 0, HPos.RIGHT, VPos.CENTER);
            }
            
            if ( item instanceof CustomMenuItem) {
                Node n = ((CustomMenuItem) item).getContent();
                if (item instanceof SeparatorMenuItem) {
                    double width = prefWidth(-1) - (getInsets().getLeft() + maxGraphicWidth + getInsets().getRight());
                    n.resize(width, n.prefHeight(-1));
                    positionInArea(n, getInsets().getLeft() + maxGraphicWidth, 0, prefWidth(-1), prefHeight, 0, HPos.LEFT, VPos.CENTER);
                } else {
                    n.resize(n.prefWidth(-1), n.prefHeight(-1));
                    //the node should be left aligned 
                    positionInArea(n, getInsets().getLeft(), 0, getWidth(), prefHeight, 0, HPos.LEFT, VPos.CENTER);
                }
            }
        }

        @Override
        public PseudoClass.States getPseudoClassStates() {
            PseudoClass.States states = super.getPseudoClassStates();
            if (item instanceof Menu && item.equals(openSubmenu) && submenu.isShowing()) {
                states.addState(SELECTED_PSEUDOCLASS_STATE);
            } else if (item instanceof RadioMenuItem && ((RadioMenuItem)item).isSelected()) {
                states.addState(CHECKED_PSEUDOCLASS_STATE);
            } else if (item instanceof CheckMenuItem && ((CheckMenuItem)item).isSelected()) {
                states.addState(CHECKED_PSEUDOCLASS_STATE);
            }
            if (item.isDisable()) states.addState(DISABLED_PSEUDOCLASS_STATE);
            return states;
        }


        
        @Override protected double computePrefHeight(double width) {
            double prefHeight = 0;
            if (item instanceof CustomMenuItem || item instanceof SeparatorMenuItem) {
                prefHeight = getChildren().get(0).prefHeight(-1);
            } else {
                prefHeight = Math.max(prefHeight, (left != null) ? left.prefHeight(-1) : 0);
                prefHeight = Math.max(prefHeight, (graphic != null) ? graphic.prefHeight(-1) : 0);
                prefHeight = Math.max(prefHeight, (label != null) ? label.prefHeight(-1) : 0);
                prefHeight = Math.max(prefHeight, (right != null) ? right.prefHeight(-1) : 0);
            }
             return getInsets().getTop() + prefHeight + getInsets().getBottom();
        }

        @Override protected double computePrefWidth(double height) {
            double nodeMenuItemWidth = 0;
            if (item instanceof CustomMenuItem && !(item instanceof SeparatorMenuItem)) {
                nodeMenuItemWidth = getInsets().getLeft() + ((CustomMenuItem) item).getContent().prefWidth(-1) +
                        getInsets().getRight();
            }
            return Math.max(nodeMenuItemWidth,
                    getInsets().getLeft() + maxLeftWidth + maxGraphicWidth +
                    maxLabelWidth + maxRightWidth + getInsets().getRight());
        }

        private void listen(ObservableBooleanValue property, final String pseudoClass) {
            property.addListener(new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    PseudoClass.State state = PseudoClass.getState(pseudoClass);
                    pseudoClassStateChanged(state);
                }
            });
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

        /** Pseudoclass indicating this Menu is selected. */
        private static final String PSEUDO_CLASS_SELECTED = "selected";
        private static final String PSEUDO_CLASS_DISABLED = "disabled";
        
        /** 
         * Pseudoclass indicating that a RadioMenuItem or CheckMenuItem is 
         * is 'checked'. Despite the 'checked' term sounding a little odd in the
         * RadioMenuItem context, this is apparently the w3c standard:
         * http://www.w3.org/TR/css3-selectors/#UIstates
         */
        private static final String PSEUDO_CLASS_CHECKED = "checked";

    }


    private static final PseudoClass.State SELECTED_PSEUDOCLASS_STATE =
            PseudoClass.getState("selected");
    private static final PseudoClass.State DISABLED_PSEUDOCLASS_STATE =
            PseudoClass.getState("disabled");
    private static final PseudoClass.State CHECKED_PSEUDOCLASS_STATE =
            PseudoClass.getState("checked");

    private class MenuLabel extends Label {

        final MenuItem menuitem;
        MenuItemContainer menuItemContainer;
        public MenuLabel(MenuItem item, MenuItemContainer mic) {
            super(item.getText());
            setMnemonicParsing(item.isMnemonicParsing());
            setFocusTraversable(true);
            setLabelFor(mic);

            menuitem = item;
            menuItemContainer = mic;

            addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    Event.fireEvent(menuitem, new ActionEvent());
                    e.consume();
                }
            });
        }
        
        /**
         * Fires a new ActionEvent.
         */
        public void fire() {
            menuItemContainer.doSelect();
        }
    }
}
