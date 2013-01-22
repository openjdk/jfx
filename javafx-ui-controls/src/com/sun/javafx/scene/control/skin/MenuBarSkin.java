/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import static com.sun.javafx.scene.traversal.Direction.DOWN;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.beans.value.WeakChangeListener;

import com.sun.javafx.menu.MenuBase;
import com.sun.javafx.scene.control.GlobalMenuAdapter;
import javafx.event.WeakEventHandler;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.TraversalEngine;
import com.sun.javafx.scene.traversal.TraverseListener;
import com.sun.javafx.stage.StageHelper;
import com.sun.javafx.tk.Toolkit;
import javafx.event.ActionEvent;
import javafx.scene.input.*;


/**
 * The skin for the MenuBar. In essence it is a simple toolbar. For the time
 * being there is no overflow behavior and we just hide nodes which fall
 * outside the bounds.
 */
public class MenuBarSkin extends BehaviorSkinBase<MenuBar, BehaviorBase<MenuBar>> implements TraverseListener {
    
    private final HBox container;

    private Menu openMenu;
    private MenuBarButton openMenuButton;
    private int focusedMenuIndex = -1;
    private TraversalEngine engine;
    private Direction direction;
    private boolean firstF10 = true;

    private static WeakHashMap<Stage, MenuBarSkin> systemMenuMap;
    private static List<MenuBase> wrappedDefaultMenus = new ArrayList<MenuBase>();
    private static Stage currentMenuBarStage;
    private List<MenuBase> wrappedMenus;

    public static void setDefaultSystemMenuBar(final MenuBar menuBar) {
        if (Toolkit.getToolkit().getSystemMenu().isSupported()) {
            wrappedDefaultMenus.clear();
            for (Menu menu : menuBar.getMenus()) {
                wrappedDefaultMenus.add(GlobalMenuAdapter.adapt(menu));
            }
            menuBar.getMenus().addListener(new ListChangeListener<Menu>() {
                @Override public void onChanged(Change<? extends Menu> c) {
                    wrappedDefaultMenus.clear();
                    for (Menu menu : menuBar.getMenus()) {
                        wrappedDefaultMenus.add(GlobalMenuAdapter.adapt(menu));
                    }
                }
            });
        }
    }

    private static void setSystemMenu(Stage stage) {
        if (stage != null && stage.isFocused()) {
            while (stage != null && stage.getOwner() instanceof Stage) {
                MenuBarSkin skin = systemMenuMap.get(stage);
                if (skin != null && skin.wrappedMenus != null) {
                    break;
                } else {
                    // This is a secondary stage (dialog) that doesn't
                    // have own menu bar.
                    //
                    // Continue looking for a menu bar in the parent stage.
                    stage = (Stage)stage.getOwner();
                }
            }
        } else {
            stage = null;
        }

        if (stage != currentMenuBarStage) {
            List<MenuBase> menuList = null;
            if (stage != null) {
                MenuBarSkin skin = systemMenuMap.get(stage);
                if (skin != null) {
                    menuList = skin.wrappedMenus;
                }
            }
            if (menuList == null) {
                menuList = wrappedDefaultMenus;
            }
            Toolkit.getToolkit().getSystemMenu().setMenus(menuList);
            currentMenuBarStage = stage;
        }
    }

    private static void initSystemMenuBar() {
        systemMenuMap = new WeakHashMap<Stage, MenuBarSkin>();

        final InvalidationListener focusedStageListener = new InvalidationListener() {
            @Override public void invalidated(Observable ov) {
                setSystemMenu((Stage)((ReadOnlyProperty<?>)ov).getBean());
            }
        };

        final ObservableList<Stage> stages = StageHelper.getStages();
        for (Stage stage : stages) {
            stage.focusedProperty().addListener(focusedStageListener);
        }
        stages.addListener(new ListChangeListener<Stage>() {
            @Override public void onChanged(Change<? extends Stage> c) {
                while (c.next()) {
                    for (Stage stage : c.getRemoved()) {
                        stage.focusedProperty().removeListener(focusedStageListener);
                    }
                    for (Stage stage : c.getAddedSubList()) {
                        stage.focusedProperty().addListener(focusedStageListener);
                        setSystemMenu(stage);
                    }
                }
            }
        });
    }

    private WeakEventHandler<KeyEvent> weakSceneKeyEventHandler;
    private WeakEventHandler<MouseEvent> weakSceneMouseEventHandler;
    private EventHandler<KeyEvent> keyEventHandler;
    private EventHandler<MouseEvent> mouseEventHandler;
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public MenuBarSkin(final MenuBar control) {
        super(control, new BehaviorBase<MenuBar>(control));
        
        container = new HBox();
        container.getStyleClass().add("container");
        getChildren().add(container);
        
        // Key navigation 
        keyEventHandler = new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent event) {
                // process right left and may be tab key events
                if (openMenu != null) {
                    switch (event.getCode()) {
                        case LEFT: {
                            boolean isRTL = control.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT;
                            if (control.getScene().getWindow().isFocused()) {
                                if (openMenu == null) return;
                                if ( !openMenu.isShowing()) {
                                    if (isRTL) {
                                        selectNextMenu(); // just move the selection bar
                                    } else {
                                        selectPrevMenu(); // just move the selection bar
                                    }
                                    event.consume();
                                    return;
                                }
                                if (isRTL) {
                                    showNextMenu();
                                } else {
                                    showPrevMenu();
                                }
                            }
                            event.consume();
                            break;
                        }
                        case RIGHT:
                        case TAB: {
                            boolean isRTL = control.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT;
                            if (control.getScene().getWindow().isFocused()) {
                                if (openMenu == null) return;
                                if (! openMenu.isShowing()) {
                                    if (isRTL) {
                                        selectPrevMenu(); // just move the selection bar
                                    } else {
                                        selectNextMenu(); // just move the selection bar
                                    }
                                    event.consume();
                                    return;
                                }
                                if (isRTL) {
                                    showPrevMenu();
                                } else {
                                    showNextMenu();
                                }
                            }
                            event.consume();
                            break;
                        }
                        case DOWN:
                        //case SPACE:
                        //case ENTER:
                            // RT-18859: Doing nothing for space and enter 
                            if (control.getScene().getWindow().isFocused()) {
                                if (focusedMenuIndex != -1 && openMenu != null) {
                                    if (!isMenuEmpty(getSkinnable().getMenus().get(focusedMenuIndex))) {
                                        openMenu = getSkinnable().getMenus().get(focusedMenuIndex);
                                        openMenu.show();
                                    } else {
                                        openMenu = null;
                                    }
                                    event.consume();
                                }
                            }
                            break;
                        case ESCAPE:
                            unSelectMenus();
                            event.consume();
                            break;
                    default:
                        break;
                    }
                }
            }
        };
        weakSceneKeyEventHandler = new WeakEventHandler<KeyEvent>(keyEventHandler);
        control.getScene().addEventFilter(KeyEvent.KEY_PRESSED, weakSceneKeyEventHandler);
        
        // When we click else where in the scene - menu selection should be cleared.
        mouseEventHandler = new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent t) {
                if (!container.localToScene(container.getLayoutBounds()).contains(t.getX(), t.getY())) {
                    unSelectMenus();
                    firstF10 = true;
                }
            }
        };
        weakSceneMouseEventHandler = new WeakEventHandler<MouseEvent>(mouseEventHandler);
        control.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, weakSceneMouseEventHandler);
        
        // When the parent window looses focus - menu selection should be cleared
        control.getScene().getWindow().focusedProperty().addListener(new WeakChangeListener<Boolean>(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
              if (!t1) {
                  unSelectMenus();
                  firstF10 = true;
              }
            }
        }));
        
        rebuildUI();
        control.getMenus().addListener(new ListChangeListener<Menu>() {
            @Override public void onChanged(Change<? extends Menu> c) {
                rebuildUI();
            }
        });
        for (final Menu menu : getSkinnable().getMenus()) {
            menu.visibleProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                    rebuildUI();
                }
            });
        }

        if (Toolkit.getToolkit().getSystemMenu().isSupported()) {
            control.useSystemMenuBarProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable valueModel) {
                    rebuildUI();
                }
            });
        }

        // When the mouse leaves the menu, the last hovered item should lose
        // it's focus so that it is no longer selected. This code returns focus
        // to the MenuBar itself, such that keyboard navigation can continue.
          // fix RT-12254 : menu bar should not request focus on mouse exit.
//        addEventFilter(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                requestFocus();
//            }
//        });

        /*
        ** add an accelerator for F10 on windows and ctrl+F10 on mac/linux
        ** pressing f10 will select the first menu button on a menubar
        */
        KeyCombination acceleratorKeyCombo;
        final String os = System.getProperty("os.name");
        if (os != null && os.startsWith("Mac")) {
           acceleratorKeyCombo = KeyCombination.keyCombination("ctrl+F10");
        } else {
           acceleratorKeyCombo = KeyCombination.keyCombination("F10");
        }
        getSkinnable().getScene().getAccelerators().put(acceleratorKeyCombo, firstMenuRunnable);
        engine = new TraversalEngine(getSkinnable(), false) {
            @Override public void trav(Node node, Direction dir) {
                direction = dir;
                super.trav(node,dir);
            }
        };
        engine.addTraverseListener(this);
        getSkinnable().setImpl_traversalEngine(engine);
        
        control.sceneProperty().addListener(new ChangeListener<Scene>() {
            @Override
            public void changed(ObservableValue<? extends Scene> ov, Scene t, Scene t1) {
                if (weakSceneKeyEventHandler != null) {
                    // remove event filter from the old scene (t)
                    if (t != null)
                        t.removeEventFilter(KeyEvent.KEY_PRESSED, weakSceneKeyEventHandler);
                }
                if (weakSceneMouseEventHandler != null) {
                    // remove event filter from the old scene (t)
                    if (t != null)
                        t.removeEventFilter(MouseEvent.MOUSE_CLICKED, weakSceneMouseEventHandler);
                }
            }
        });
    }
    
    
    Runnable firstMenuRunnable = new Runnable() {
            public void run() {
                /*
                ** check that this menubar's container has contents,
                ** and that the first item is a MenuButton.... 
                ** otherwise the transfer is off!
                */
                if (container.getChildren().size() > 0) {
                    if (container.getChildren().get(0) instanceof MenuButton) {
//                        container.getChildren().get(0).requestFocus();
                        if (firstF10) { 
                            firstF10 = false;
                            unSelectMenus();
                            focusedMenuIndex = 0;
                            openMenuButton = ((MenuBarButton)container.getChildren().get(0));
                            openMenu = getSkinnable().getMenus().get(0);
                            openMenuButton.setHover();
                        } else {
                            firstF10 = true;
                            unSelectMenus();
                        }
                    }
                }
            }
        };


    private boolean pendingDismiss = false;

    // For testing purpose only. 
    MenuButton getNodeForMenu(int i) {
        if (i < container.getChildren().size()) {
            return (MenuBarButton)container.getChildren().get(i);
        }
        return null;
    }
    
    int getFocusedMenuIndex() {
        return focusedMenuIndex;
    }
    
    private boolean menusContainCustomMenuItem() {
        for (Menu menu : getSkinnable().getMenus()) {
            if (menuContainsCustomMenuItem(menu)) {
                System.err.println("Warning: MenuBar ignored property useSystemMenuBar because menus contain CustomMenuItem");
                return true;
            }
        }
        return false;
    }

    private boolean menuContainsCustomMenuItem(Menu menu) {
        for (MenuItem mi : menu.getItems()) {
            if (mi instanceof CustomMenuItem && !(mi instanceof SeparatorMenuItem)) {
                return true;
            } else if (mi instanceof Menu) {
                if (menuContainsCustomMenuItem((Menu)mi)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getMenuBarButtonIndex(MenuBarButton m) {
        for (int i= 0; i < container.getChildren().size(); i++) {
            MenuBarButton menuButton = (MenuBarButton)container.getChildren().get(i);
            if (m == menuButton) {
                return i;
            }
        }
        return -1;
    }
    
    // RT-20411 : reset menu selected/focused state 
    private EventHandler<ActionEvent> menuActionEventHandler = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent t) {
            unSelectMenus();
        }
    };
    
    private void updateActionListeners(Menu m, boolean add) {
        for (MenuItem mi : m.getItems()) {
            if (mi instanceof Menu) {
                updateActionListeners((Menu)mi, add);
            } else {
                if (add) {
                    mi.addEventHandler(ActionEvent.ACTION, menuActionEventHandler);
                } else {
                    mi.removeEventHandler(ActionEvent.ACTION, menuActionEventHandler);
                }
            }
        }
    }
    
    private void rebuildUI() {
        for (Menu m : getSkinnable().getMenus()) {
            // remove action listeners 
            updateActionListeners(m, false);
        }
        for(Node n : container.getChildren()) {
            //Stop observing menu's showing & disable property for changes.
            //Need to unbind before clearing container's children.
            MenuBarButton menuButton = (MenuBarButton)n;
            menuButton.hide();
            menuButton.menu.showingProperty().removeListener(menuButton.menuListener);
            menuButton.disableProperty().unbind();
            menuButton.textProperty().unbind();
            menuButton.graphicProperty().unbind();
            menuButton.styleProperty().unbind();
        }
        container.getChildren().clear();


        if (Toolkit.getToolkit().getSystemMenu().isSupported() && getSkinnable().getScene() != null) {
            Scene scene = getSkinnable().getScene();
            if (scene.getWindow() instanceof Stage) {
                Stage stage = (Stage)scene.getWindow();
                MenuBarSkin curMBSkin = (systemMenuMap != null) ? systemMenuMap.get(stage) : null;
                if (getSkinnable().isUseSystemMenuBar() && !menusContainCustomMenuItem()) {
                    if (curMBSkin != null &&
                        (curMBSkin.getSkinnable().getScene() == null || curMBSkin.getSkinnable().getScene().getWindow() == null)) {
                        // Fix for RT-20951. The MenuBar may have been removed from the Stage.
                        systemMenuMap.remove(stage);
                        curMBSkin = null;
                    }

                    // Set the system menu bar if not set by another
                    // MenuBarSkin instance on this stage.
                    if (systemMenuMap == null || curMBSkin == null || curMBSkin == this) {
                        if (systemMenuMap == null) {
                            initSystemMenuBar();
                        }
                        if (wrappedMenus == null) {
                            wrappedMenus = new ArrayList<MenuBase>();
                            systemMenuMap.put(stage, this);
                        } else {
                            wrappedMenus.clear();
                        }
                        for (Menu menu : getSkinnable().getMenus()) {
                            wrappedMenus.add(GlobalMenuAdapter.adapt(menu));
                        }
                        currentMenuBarStage = null;
                        setSystemMenu(stage);

                        getSkinnable().requestLayout();
                        javafx.application.Platform.runLater(new Runnable() {
                            public void run() {
                                getSkinnable().requestLayout();
                            }
                        });
                        return;
                    }
                }

                if (curMBSkin == this) {
                    // This MenuBar was previously installed in the
                    // system menu bar. Remove it.
                    wrappedMenus = null;
                    systemMenuMap.remove(stage);
                    currentMenuBarStage = null;
                    setSystemMenu(stage);
                }
            }
        }


        for (final Menu menu : getSkinnable().getMenus()) {
            if (!menu.isVisible()) continue;
            final MenuBarButton menuButton = new MenuBarButton(menu.getText(), menu.getGraphic());
            menuButton.setFocusTraversable(false);
            menuButton.getStyleClass().add("menu");
            menuButton.setStyle(menu.getStyle()); // copy style 

            menuButton.getItems().setAll(menu.getItems());
            container.getChildren().add(menuButton);
            // listen to changes in menu items & update menuButton items
            menu.getItems().addListener(new ListChangeListener<MenuItem>() {
                @Override public void onChanged(Change<? extends MenuItem> c) {
                    while (c.next()) {
                        menuButton.getItems().removeAll(c.getRemoved());
                        menuButton.getItems().addAll(c.getFrom(), c.getAddedSubList());
                    }
                }
            });
            menu.getStyleClass().addListener(new ListChangeListener<String>() {
                @Override
                public void onChanged(Change<? extends String> c) {
                    while(c.next()) {
                        for(int i=c.getFrom(); i<c.getTo(); i++) {
                            menuButton.getStyleClass().add(menu.getStyleClass().get(i));
                        }
                        for (String str : c.getRemoved()) {
                            menuButton.getStyleClass().remove(str);
                        }
                    }
                }
            });
            menuButton.menuListener = new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (menu.isShowing()) {
                        menuButton.show();
                    } else {
                        menuButton.hide();
                    }
                }

            };
            menuButton.menu = menu;
            menu.showingProperty().addListener(menuButton.menuListener);
            menuButton.disableProperty().bindBidirectional(menu.disableProperty());
            menuButton.textProperty().bind(menu.textProperty());
            menuButton.graphicProperty().bind(menu.graphicProperty());
            menuButton.styleProperty().bind(menu.styleProperty());
            menuButton.getProperties().addListener(new MapChangeListener<Object, Object>() {
                @Override
                public void onChanged(Change<? extends Object, ? extends Object> c) {
                     if (c.wasAdded() && MenuButtonSkin.AUTOHIDE.equals(c.getKey())) {
                        menuButton.getProperties().remove(MenuButtonSkin.AUTOHIDE);
                        menu.hide();
                    }
                }
            });
            menuButton.showingProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean isShowing) {
                    if (isShowing) {
                        if (openMenuButton != null && openMenuButton != menuButton) {
                            openMenuButton.hide();
                        }
                        openMenuButton = menuButton;
                        openMenu = menu;
                        if (!menu.isShowing())menu.show();
                    }
                }
            });

            menuButton.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    pendingDismiss = menuButton.isShowing();

                    // check if the owner window has focus
                    if (menuButton.getScene().getWindow().isFocused()) {
                        if (!isMenuEmpty(menu)){
                            openMenu = menu;
                            openMenu.show();
                        } else {
                            openMenu = null;
                        }
                        // update FocusedIndex
                        focusedMenuIndex = getMenuBarButtonIndex(menuButton);
                    }
                }
            });
            
            menuButton.setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    // check if the owner window has focus
                    if (menuButton.getScene().getWindow().isFocused()) {
                        if (pendingDismiss) {
                            resetOpenMenu();
//                            menuButton.hide();
                        }
                    }
                    pendingDismiss = false;
                }
            });

//            menuButton. setOnKeyPressed(new EventHandler<javafx.scene.input.KeyEvent>() {
//                @Override public void handle(javafx.scene.input.KeyEvent ke) {
//                    switch (ke.getCode()) {
//                        case LEFT:
//                            if (menuButton.getScene().getWindow().isFocused()) {
//                                Menu prevMenu = findPreviousSibling();
//                                if (openMenu == null || ! openMenu.isShowing()) {
//                                    return;
//                                }
////                                if (focusedMenuIndex == container.getChildren().size() - 1) {
////                                   ((MenuBarButton)container.getChildren().get(focusedMenuIndex)).requestFocus();
////                                }
//                                 // hide the currently visible menu, and move to the previous one
//                                openMenu.hide();
//                                if (!isMenuEmpty(prevMenu)) {
//                                    openMenu = prevMenu;
//                                    openMenu.show();
//                                } else {
//                                    openMenu = null;
//                                }
//                            }
//                            ke.consume();
//                            break;
//                        case RIGHT:
//                            if (menuButton.getScene().getWindow().isFocused()) {
//                                Menu nextMenu = findNextSibling();
//                                if (openMenu == null || ! openMenu.isShowing()) {
//                                    return;
//                                }
////                                if (focusedMenuIndex == 0) {
////                                    ((MenuBarButton)container.getChildren().get(focusedMenuIndex)).requestFocus();
////                                }
//                                 // hide the currently visible menu, and move to the next one
//                                openMenu.hide();
//                                if (!isMenuEmpty(nextMenu)) {
//                                    openMenu = nextMenu;
//                                    openMenu.show();
//                                } else {
//                                    openMenu = null;
//                                }
//                            }
//                            ke.consume();
//                            break;
//
//                        case DOWN:
//                        case SPACE:
//                        case ENTER:
//                            if (menuButton.getScene().getWindow().isFocused()) {
//                                if (focusedMenuIndex != -1) {
//                                    if (!isMenuEmpty(getSkinnable().getMenus().get(focusedMenuIndex))) {
//                                        openMenu = getSkinnable().getMenus().get(focusedMenuIndex);
//                                        openMenu.show();
//                                    } else {
//                                        openMenu = null;
//                                    }
//                                    ke.consume();
//                                }
//                            }
//                            break;
//                    }
//                }
//            });
            menuButton.setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    // check if the owner window has focus
                    if (menuButton.getScene() != null && menuButton.getScene().getWindow() != null && 
                            menuButton.getScene().getWindow().isFocused()) { 
                        if (openMenuButton != null && openMenuButton != menuButton) {
                                openMenuButton.clearHover();
                                openMenuButton = null;
                                openMenuButton = menuButton;
                        }
                        if (openMenu == null) return;
                        updateFocusedIndex();
                        if (openMenu.isShowing() && openMenu != menu) {
                         // hide the currently visible menu, and move to the new one
                            openMenu.hide();
                            if (!isMenuEmpty(menu)) {
                                openMenu = menu;
                                updateFocusedIndex();
                                openMenu.show();
                            } else {
                                openMenu = null;
                            }
                        }
                    }
                }
            });
            updateActionListeners(menu, true);
        }
        getSkinnable().requestLayout();
    }
    
    /*
     *  if (openMenu == null) return;
                            if ( !openMenu.isShowing()) {
                                selectPrevMenu(); // just move the selection bar
                                return;
                            }
                            showPrevMenu();
                        }
     */

    private boolean isMenuEmpty(Menu menu) {
        boolean retVal = true;
        for (MenuItem m : menu.getItems()) {
            if (m.isVisible()) retVal = false;
        }
        return retVal;
    }

    private void resetOpenMenu() {
        if (openMenu != null) {
            openMenu.hide();
            openMenu = null;
            openMenuButton = (MenuBarButton)container.getChildren().get(focusedMenuIndex);
            openMenuButton.clearHover();
            openMenuButton = null;
            focusedMenuIndex = -1;
        }
    }
    
    private void unSelectMenus() {
        if (focusedMenuIndex == -1) return;
        clearMenuButtonHover();
        if (openMenu != null) {
            openMenu.hide();
            openMenu = null;
        }
        if (openMenuButton != null) {
            openMenuButton.clearHover();
            openMenuButton = null;
        }
        focusedMenuIndex = -1;
    }
    
    private void selectNextMenu() {
        Menu nextMenu = findNextSibling();
        if (nextMenu != null && focusedMenuIndex != -1) {
            openMenuButton = (MenuBarButton)container.getChildren().get(focusedMenuIndex);
            openMenuButton.setHover();
            openMenu = nextMenu;
        }
    }
    
    private void selectPrevMenu() {
        Menu prevMenu = findPreviousSibling();
        if (prevMenu != null && focusedMenuIndex != -1) {
            openMenuButton = (MenuBarButton)container.getChildren().get(focusedMenuIndex);
            openMenuButton.setHover();
            openMenu = prevMenu;
        }
    }
    
    private void showNextMenu() {
        Menu nextMenu = findNextSibling();
        // hide the currently visible menu, and move to the next one
        if (openMenu != null) openMenu.hide();
        if (!isMenuEmpty(nextMenu)) {
            openMenu = nextMenu;
            openMenu.show();
        } else {
            openMenu = null;
        }
    }

    private void showPrevMenu() {
        Menu prevMenu = findPreviousSibling();
        // hide the currently visible menu, and move to the next one
        if (openMenu != null) openMenu.hide();
        if (!isMenuEmpty(prevMenu)) {
            openMenu = prevMenu;
            openMenu.show();
        } else {
            openMenu = null;
        }
    }
    
    private Menu findPreviousSibling() {
        if (focusedMenuIndex == -1) return null;
        if (focusedMenuIndex == 0) {
            focusedMenuIndex = container.getChildren().size() - 1;
        } else {
            focusedMenuIndex--;
        }
        // RT-19359
        if (getSkinnable().getMenus().get(focusedMenuIndex).isDisable()) return findPreviousSibling();
        clearMenuButtonHover();
        return getSkinnable().getMenus().get(focusedMenuIndex);
    }

    private Menu findNextSibling() {
        if (focusedMenuIndex == -1) return null;
        if (focusedMenuIndex == container.getChildren().size() - 1) {
            focusedMenuIndex = 0;
        } else {
            focusedMenuIndex++;
        }
        // RT_19359
        if (getSkinnable().getMenus().get(focusedMenuIndex).isDisable()) return findNextSibling();
        clearMenuButtonHover();
        return getSkinnable().getMenus().get(focusedMenuIndex);
    }

    private void updateFocusedIndex() {
        int index = 0;
        for(Node n : container.getChildren()) {
            if (n.isHover()) {
                focusedMenuIndex = index;
                return;
            }
            index++;
        }
        focusedMenuIndex = -1;
    }

    private void clearMenuButtonHover() {
         for(Node n : container.getChildren()) {
            if (n.isHover()) {
                ((MenuBarButton)n).clearHover();
                return;
            }
        }
    }

    @Override
    public void onTraverse(Node node, Bounds bounds) {
        if (direction.equals(Direction.NEXT)) {
            if (openMenu != null) openMenu.hide();
            focusedMenuIndex = 0;
            new TraversalEngine(getSkinnable(), false).trav(getSkinnable(), Direction.NEXT);
        } else if (direction.equals(DOWN)) {
            // do nothing 
        }
    }

    static class MenuBarButton extends MenuButton {
        private ChangeListener<Boolean> menuListener;
        private Menu menu;

        public MenuBarButton() {
            super();
        }

        public MenuBarButton(String text) {
            super(text);
        }

        public MenuBarButton(String text, Node graphic) {
            super(text, graphic);
        }

        private void clearHover() {
            setHover(false);
        }
        
        private void setHover() {
            setHover(true);
        }
      
    }

    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/

    // Return empty insets when "container" is empty, which happens
    // when using the system menu bar.
    private Insets getInsets() {
        if (container.getChildren().isEmpty()) {
            return Insets.EMPTY;
        } else {
            return getSkinnable().getInsets();
        }
    }

    /**
     * Layout the menu bar. This is a simple horizontal layout like an hbox.
     * Any menu items which don't fit into it will simply be made invisible.
     */
    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        // layout the menus one after another
        container.resizeRelocate(x, y, w, h);
    }

    @Override protected double computeMinWidth(double height) {
        Insets insets = getInsets();
        return container.minWidth(height) + insets.getLeft() + insets.getRight();
    }

    @Override protected double computePrefWidth(double height) {
        Insets insets = getInsets();
        return container.prefWidth(height) + insets.getLeft() + insets.getRight();
    }

    @Override protected double computeMinHeight(double width) {
        Insets insets = getInsets();
        return container.minHeight(width) + insets.getTop() + insets.getBottom();
    }

    @Override protected double computePrefHeight(double width) {
        Insets insets = getInsets();
        return container.prefHeight(width) + insets.getTop() + insets.getBottom();
    }

    // grow horizontally, but not vertically
    @Override protected double computeMaxHeight(double width) {
        return getSkinnable().prefHeight(-1);
    }
}
