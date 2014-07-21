/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.traversal.ParentTraversalEngine;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.sun.javafx.menu.MenuBase;
import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.scene.control.GlobalMenuAdapter;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.traversal.TraverseListener;
import com.sun.javafx.stage.StageHelper;
import com.sun.javafx.tk.Toolkit;
import javafx.stage.Window;


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

    private static WeakHashMap<Stage, Reference<MenuBarSkin>> systemMenuMap;
    private static List<MenuBase> wrappedDefaultMenus = new ArrayList<MenuBase>();
    private static Stage currentMenuBarStage;
    private List<MenuBase> wrappedMenus;

    public static void setDefaultSystemMenuBar(final MenuBar menuBar) {
        if (Toolkit.getToolkit().getSystemMenu().isSupported()) {
            wrappedDefaultMenus.clear();
            for (Menu menu : menuBar.getMenus()) {
                wrappedDefaultMenus.add(GlobalMenuAdapter.adapt(menu));
            }
            menuBar.getMenus().addListener((ListChangeListener<Menu>) c -> {
                wrappedDefaultMenus.clear();
                for (Menu menu : menuBar.getMenus()) {
                    wrappedDefaultMenus.add(GlobalMenuAdapter.adapt(menu));
                }
            });
        }
    }

    private static MenuBarSkin getMenuBarSkin(Stage stage) {
        if (systemMenuMap == null) return null;
        Reference<MenuBarSkin> skinRef = systemMenuMap.get(stage);
        return skinRef == null ? null : skinRef.get();
    }

    private static void setSystemMenu(Stage stage) {
        if (stage != null && stage.isFocused()) {
            while (stage != null && stage.getOwner() instanceof Stage) {
                MenuBarSkin skin = getMenuBarSkin(stage);
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
                MenuBarSkin skin = getMenuBarSkin(stage);
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
        systemMenuMap = new WeakHashMap<>();

        final InvalidationListener focusedStageListener = ov -> {
            setSystemMenu((Stage)((ReadOnlyProperty<?>)ov).getBean());
        };

        final ObservableList<Stage> stages = StageHelper.getStages();
        for (Stage stage : stages) {
            stage.focusedProperty().addListener(focusedStageListener);
        }
        stages.addListener((ListChangeListener<Stage>) c -> {
            while (c.next()) {
                for (Stage stage : c.getRemoved()) {
                    stage.focusedProperty().removeListener(focusedStageListener);
                }
                for (Stage stage : c.getAddedSubList()) {
                    stage.focusedProperty().addListener(focusedStageListener);
                    setSystemMenu(stage);
                }
            }
        });
    }

    private WeakEventHandler<KeyEvent> weakSceneKeyEventHandler;
    private WeakEventHandler<MouseEvent> weakSceneMouseEventHandler;
    private WeakChangeListener<Boolean> weakWindowFocusListener;
    private WeakChangeListener<Window> weakWindowSceneListener;
    private EventHandler<KeyEvent> keyEventHandler;
    private EventHandler<MouseEvent> mouseEventHandler;
    private ChangeListener<Boolean> menuBarFocusedPropertyListener;
    private ChangeListener<Scene> sceneChangeListener;

    EventHandler<KeyEvent> getKeyEventHandler() {
        return keyEventHandler;
    }

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public MenuBarSkin(final MenuBar control) {
        super(control, new BehaviorBase<>(control, Collections.emptyList()));
        
        container = new HBox();
        container.getStyleClass().add("container");
        getChildren().add(container);
        
        // Key navigation 
        keyEventHandler = event -> {
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
                    {
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
                                openMenu = getSkinnable().getMenus().get(focusedMenuIndex);
                                if (!isMenuEmpty(getSkinnable().getMenus().get(focusedMenuIndex))) {
                                    openMenu.show();
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
        };
        menuBarFocusedPropertyListener = (ov, t, t1) -> {
            if (t1) {
                // RT-23147 when MenuBar's focusTraversable is true the first
                // menu will visually indicate focus
                unSelectMenus();
                menuModeStart(0);
                openMenuButton = ((MenuBarButton)container.getChildren().get(0));
                openMenu = getSkinnable().getMenus().get(0);
                openMenuButton.setHover();
            } else {
                unSelectMenus();
             }
         };
        weakSceneKeyEventHandler = new WeakEventHandler<KeyEvent>(keyEventHandler);
        Utils.executeOnceWhenPropertyIsNonNull(control.sceneProperty(), (Scene scene) -> {
            scene.addEventFilter(KeyEvent.KEY_PRESSED, weakSceneKeyEventHandler);
        });
        
        // When we click else where in the scene - menu selection should be cleared.
        mouseEventHandler = t -> {
            if (!container.localToScreen(container.getLayoutBounds()).contains(t.getScreenX(), t.getScreenY())) {
                unSelectMenus();
            }
        };
        weakSceneMouseEventHandler = new WeakEventHandler<MouseEvent>(mouseEventHandler);
        Utils.executeOnceWhenPropertyIsNonNull(control.sceneProperty(), (Scene scene) -> {
            scene.addEventFilter(MouseEvent.MOUSE_CLICKED, weakSceneMouseEventHandler);
        });
        
        weakWindowFocusListener = new WeakChangeListener<Boolean>((ov, t, t1) -> {
            if (!t1) {
              unSelectMenus();
            }
        });
        // When the parent window looses focus - menu selection should be cleared
        Utils.executeOnceWhenPropertyIsNonNull(control.sceneProperty(), (Scene scene) -> {
            if (scene.getWindow() != null) {
                scene.getWindow().focusedProperty().addListener(weakWindowFocusListener);
            } else {
                ChangeListener<Window> sceneWindowListener = (observable, oldValue, newValue) -> {
                    if (oldValue != null)
                        oldValue.focusedProperty().removeListener(weakWindowFocusListener);
                    if (newValue != null)
                        newValue.focusedProperty().addListener(weakWindowFocusListener);
                };
                weakWindowSceneListener = new WeakChangeListener<>(sceneWindowListener);
                scene.windowProperty().addListener(weakWindowSceneListener);
            }
        });

        rebuildUI();
        control.getMenus().addListener((ListChangeListener<Menu>) c -> {
            rebuildUI();
        });
        for (final Menu menu : getSkinnable().getMenus()) {
            menu.visibleProperty().addListener((ov, t, t1) -> {
                rebuildUI();
            });
        }

        if (Toolkit.getToolkit().getSystemMenu().isSupported()) {
            control.useSystemMenuBarProperty().addListener(valueModel -> {
                rebuildUI();
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
        final KeyCombination acceleratorKeyCombo;
        final String os = System.getProperty("os.name");
        if (os != null && os.startsWith("Mac")) {
           acceleratorKeyCombo = KeyCombination.keyCombination("ctrl+F10");
        } else {
           acceleratorKeyCombo = KeyCombination.keyCombination("F10");
        }
        Utils.executeOnceWhenPropertyIsNonNull(control.sceneProperty(), (Scene scene) -> {
            scene.getAccelerators().put(acceleratorKeyCombo, firstMenuRunnable);
        });

        ParentTraversalEngine engine = new ParentTraversalEngine(getSkinnable());
        engine.addTraverseListener(this);
        getSkinnable().setImpl_traversalEngine(engine);

        control.sceneProperty().addListener((ov, t, t1) -> {
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

            /**
             * remove the f10 accelerator from the old scene
             * add it to the new scene
             */
            if (t != null) {
                t.getAccelerators().remove(acceleratorKeyCombo);
            }
            if (t1 != null ) {
                t1.getAccelerators().put(acceleratorKeyCombo, firstMenuRunnable);
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
                        if (focusedMenuIndex != 0) {
                            unSelectMenus();
                            menuModeStart(0);
                            openMenuButton = ((MenuBarButton)container.getChildren().get(0));
                            openMenu = getSkinnable().getMenus().get(0);
                            openMenuButton.setHover();
                        }
                        else {
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
    private EventHandler<ActionEvent> menuActionEventHandler = t -> {
        if (t.getSource() instanceof CustomMenuItem) {
            // RT-29614 If CustomMenuItem hideOnClick is false, dont hide
            CustomMenuItem cmi = (CustomMenuItem)t.getSource();
            if (!cmi.isHideOnClick()) return;
        }
        unSelectMenus();
    };

    private ListChangeListener<MenuItem> menuItemListener = (c) -> {
        while (c.next()) {
            for (MenuItem mi : c.getAddedSubList()) {
                mi.addEventHandler(ActionEvent.ACTION, menuActionEventHandler);
            }
            for (MenuItem mi: c.getRemoved()) {
                mi.removeEventHandler(ActionEvent.ACTION, menuActionEventHandler);
            }
        }
    };

    private void updateActionListeners(Menu m, boolean add) {
        if (add) {
            m.getItems().addListener(menuItemListener);
        } else {
            m.getItems().removeListener(menuItemListener);
        }
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
        getSkinnable().focusedProperty().removeListener(menuBarFocusedPropertyListener);
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
            // RT-29729 : old instance of context menu window/popup for this MenuButton needs 
            // to be cleaned up. Setting the skin to null - results in a call to dispose() 
            // on the skin which in this case MenuButtonSkinBase - does the subsequent 
            // clean up to ContextMenu/popup window.
            menuButton.setSkin(null);
            menuButton = null;
        }
        container.getChildren().clear();

        if (Toolkit.getToolkit().getSystemMenu().isSupported()) {

            final Scene scene = getSkinnable().getScene();
            if (scene != null) {
                // RT-36554 - make sure system menu is updated when this MenuBar's scene changes.
                if (sceneChangeListener == null) {
                    sceneChangeListener = (observable, oldValue, newValue) -> {

                        if (oldValue != null) {
                            if (oldValue.getWindow() instanceof Stage) {
                                final Stage stage = (Stage) oldValue.getWindow();
                                final MenuBarSkin curMBSkin = getMenuBarSkin(stage);
                                if (curMBSkin == MenuBarSkin.this) {
                                    curMBSkin.wrappedMenus = null;
                                    systemMenuMap.remove(stage);
                                    if (currentMenuBarStage == stage) {
                                        currentMenuBarStage = null;
                                        setSystemMenu(stage);
                                    }
                                }
                            }
                        }

                        if (newValue != null) {
                            if (getSkinnable().isUseSystemMenuBar() && !menusContainCustomMenuItem()) {
                                if (newValue.getWindow() instanceof Stage) {
                                    final Stage stage = (Stage) newValue.getWindow();
                                    if (systemMenuMap == null) {
                                        initSystemMenuBar();
                                    }
                                    if (wrappedMenus == null) {
                                        wrappedMenus = new ArrayList<>();
                                        systemMenuMap.put(stage, new WeakReference<>(this));
                                    } else {
                                        wrappedMenus.clear();
                                    }
                                    for (Menu menu : getSkinnable().getMenus()) {
                                        wrappedMenus.add(GlobalMenuAdapter.adapt(menu));
                                    }
                                    currentMenuBarStage = null;
                                    setSystemMenu(stage);

                                    // TODO: Why two request layout calls here?
                                    getSkinnable().requestLayout();
                                    javafx.application.Platform.runLater(() -> getSkinnable().requestLayout());
                                }
                            }
                        }
                    };
                    getSkinnable().sceneProperty().addListener(sceneChangeListener);
                }

                // Fake a change event to trigger an update to the system menu.
                sceneChangeListener.changed(getSkinnable().sceneProperty(), scene, scene);

                // If the system menu references this MenuBarSkin, then we're done with rebuilding the UI.
                // If the system menu does not reference this MenuBarSkin, then the MenuBar is a child of the scene
                // and we continue with the update.
                // If there is no system menu but this skinnable uses the system menu bar, then the
                // stage just isn't focused yet (see setSystemMenu) and we're done rebuilding the UI.
                if (currentMenuBarStage != null ? getMenuBarSkin(currentMenuBarStage) == MenuBarSkin.this : getSkinnable().isUseSystemMenuBar()) {
                    return;
                }

            } else {
                // if scene is null, make sure this MenuBarSkin isn't left behind as the system menu
                if (currentMenuBarStage != null) {
                    final MenuBarSkin curMBSkin = getMenuBarSkin(currentMenuBarStage);
                    if (curMBSkin == MenuBarSkin.this) {
                        setSystemMenu(null);
                    }
                }
            }
        }

        getSkinnable().focusedProperty().addListener(menuBarFocusedPropertyListener);
        for (final Menu menu : getSkinnable().getMenus()) {
            if (!menu.isVisible()) continue;
            final MenuBarButton menuButton = new MenuBarButton(this, menu.getText(), menu.getGraphic());
            menuButton.setFocusTraversable(false);
            menuButton.getStyleClass().add("menu");
            menuButton.setStyle(menu.getStyle()); // copy style
            menuButton.setId(menu.getId());

            menuButton.getItems().setAll(menu.getItems());
            container.getChildren().add(menuButton);
            // listen to changes in menu items & update menuButton items
            menu.getItems().addListener((ListChangeListener<MenuItem>) c -> {
                while (c.next()) {
                    menuButton.getItems().removeAll(c.getRemoved());
                    menuButton.getItems().addAll(c.getFrom(), c.getAddedSubList());
                }
            });
            menu.getStyleClass().addListener((ListChangeListener<String>) c -> {
                while(c.next()) {
                    for(int i=c.getFrom(); i<c.getTo(); i++) {
                        menuButton.getStyleClass().add(menu.getStyleClass().get(i));
                    }
                    for (String str : c.getRemoved()) {
                        menuButton.getStyleClass().remove(str);
                    }
                }
            });
            menu.idProperty().addListener((observableValue, s, s2) -> {
                menuButton.setId(s2);
            });
            menuButton.menuListener = (observable, oldValue, newValue) -> {
                if (menu.isShowing()) {
                    menuButton.show();
                    menuModeStart(container.getChildren().indexOf(menuButton));
                } else {
                    menuButton.hide();
                }
            };
            menuButton.menu = menu;
            menu.showingProperty().addListener(menuButton.menuListener);
            menuButton.disableProperty().bindBidirectional(menu.disableProperty());
            menuButton.textProperty().bind(menu.textProperty());
            menuButton.graphicProperty().bind(menu.graphicProperty());
            menuButton.styleProperty().bind(menu.styleProperty());
            menuButton.getProperties().addListener((MapChangeListener<Object, Object>) c -> {
                 if (c.wasAdded() && MenuButtonSkin.AUTOHIDE.equals(c.getKey())) {
                    menuButton.getProperties().remove(MenuButtonSkin.AUTOHIDE);
                    menu.hide();
                }
            });
            menuButton.showingProperty().addListener((observable, oldValue, isShowing) -> {
                if (isShowing) {
                    if (openMenuButton != null && openMenuButton != menuButton) {
                        openMenuButton.hide();
                    }
                    openMenuButton = menuButton;
                    openMenu = menu;
                    if (!menu.isShowing())menu.show();
                }
            });

            menuButton.setOnMousePressed(event -> {
                pendingDismiss = menuButton.isShowing();

                // check if the owner window has focus
                if (menuButton.getScene().getWindow().isFocused()) {
                    openMenu = menu;
                    if (!isMenuEmpty(menu)){
                        openMenu.show();
                    }
                    // update FocusedIndex
                    menuModeStart(getMenuBarButtonIndex(menuButton));
                }
            });
            
            menuButton.setOnMouseReleased(event -> {
                // check if the owner window has focus
                if (menuButton.getScene().getWindow().isFocused()) {
                    if (pendingDismiss) {
                        resetOpenMenu();
//                            menuButton.hide();
                    }
                }
                pendingDismiss = false;
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
            menuButton.setOnMouseEntered(event -> {
                // check if the owner window has focus
                if (menuButton.getScene() != null && menuButton.getScene().getWindow() != null &&
                        menuButton.getScene().getWindow().isFocused()) {
                    if (openMenuButton != null && openMenuButton != menuButton) {
                            openMenuButton.clearHover();
                            openMenuButton = null;
                            openMenuButton = menuButton;
                    }
                    updateFocusedIndex();
                    if (openMenu != null && openMenu != menu) {
                     // hide the currently visible menu, and move to the new one
                        openMenu.hide();
                        openMenu = menu;
                        updateFocusedIndex();
                        if (!isMenuEmpty(menu)) {
                            openMenu.show();
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

    @Override
    public void dispose() {
        cleanUpSystemMenu();
        // call super.dispose last since it sets control to null
        super.dispose();
    }

    private void cleanUpSystemMenu() {

        if (sceneChangeListener != null && getSkinnable() != null) {
            getSkinnable().sceneProperty().removeListener(sceneChangeListener);
            // rebuildUI creates sceneChangeListener and adds sceneChangeListener to sceneProperty,
            // so sceneChangeListener needs to be reset to null in the off chance that this
            // skin instance is reused.
            sceneChangeListener = null;
        }

        if (currentMenuBarStage != null && getMenuBarSkin(currentMenuBarStage) == MenuBarSkin.this) {
            setSystemMenu(null);
        }

        if (systemMenuMap != null) {
            Iterator<Map.Entry<Stage,Reference<MenuBarSkin>>> iterator = systemMenuMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Stage,Reference<MenuBarSkin>> entry = iterator.next();
                Reference<MenuBarSkin> ref = entry.getValue();
                MenuBarSkin skin = ref != null ? ref.get() : null;
                if (skin == null || skin == MenuBarSkin.this) {
                    iterator.remove();
                }
            }
        }
    }

    private boolean isMenuEmpty(Menu menu) {
        boolean retVal = true;
        if (menu != null) {
            for (MenuItem m : menu.getItems()) {
                if (m != null && m.isVisible()) retVal = false;
            }
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
            menuModeEnd();
        }
    }
    
    private void unSelectMenus() {
        clearMenuButtonHover();
        if (focusedMenuIndex == -1) return;
        if (openMenu != null) {
            openMenu.hide();
            openMenu = null;
        }
        if (openMenuButton != null) {
            openMenuButton.clearHover();
            openMenuButton = null;
        }
        menuModeEnd();
    }

    private void menuModeStart(int newIndex) {
        if (focusedMenuIndex == -1) {
            SceneHelper.getSceneAccessor().setTransientFocusContainer(getSkinnable().getScene(), getSkinnable());
        }
        focusedMenuIndex = newIndex;
    }

    private void menuModeEnd() {
        if (focusedMenuIndex != -1) {
            SceneHelper.getSceneAccessor().setTransientFocusContainer(getSkinnable().getScene(), null);

            /* Return the a11y focus to a control in the scene. */
            getSkinnable().notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_NODE);
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
        openMenu = nextMenu;
        if (!isMenuEmpty(nextMenu)) {
            openMenu.show();
        } 
    }

    private void showPrevMenu() {
        Menu prevMenu = findPreviousSibling();
        // hide the currently visible menu, and move to the next one
        if (openMenu != null) openMenu.hide();
        openMenu = prevMenu;
        if (!isMenuEmpty(prevMenu)) {
            openMenu.show();
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
        menuModeEnd();
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
        if (openMenu != null) openMenu.hide();
        focusedMenuIndex = 0;
    }

    static class MenuBarButton extends MenuButton {
        private ChangeListener<Boolean> menuListener;
        private MenuBarSkin menuBarSkin;
        private Menu menu;

        public MenuBarButton(MenuBarSkin menuBarSkin, String text, Node graphic) {
            super(text, graphic);
            this.menuBarSkin = menuBarSkin;
            setRole(AccessibleRole.MENU);
        }

        public MenuBarSkin getMenuBarSkin() {
            return menuBarSkin;
        }

        private void clearHover() {
            setHover(false);
        }
        
        private void setHover() {
            setHover(true);

            /* Transfer the a11y focus to an item in the menu bar. */
            menuBarSkin.getSkinnable().notifyAccessibleAttributeChanged(AccessibleAttribute.FOCUS_NODE);
        }

        @Override
        public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
            switch (attribute) {
                case FOCUS_ITEM: return MenuBarButton.this;
                default: return super.queryAccessibleAttribute(attribute, parameters);
            }
        }
    }

    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/

    // Return empty insets when "container" is empty, which happens
    // when using the system menu bar.

    @Override protected double snappedTopInset() {
        return container.getChildren().isEmpty() ? 0 : super.snappedTopInset();
    }
    @Override protected double snappedBottomInset() {
        return container.getChildren().isEmpty() ? 0 : super.snappedBottomInset();
    }
    @Override protected double snappedLeftInset() {
        return container.getChildren().isEmpty() ? 0 : super.snappedLeftInset();
    }
    @Override protected double snappedRightInset() {
        return container.getChildren().isEmpty() ? 0 : super.snappedRightInset();
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

    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return container.minWidth(height) + snappedLeftInset() + snappedRightInset();
    }

    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return container.prefWidth(height) + snappedLeftInset() + snappedRightInset();
    }

    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return container.minHeight(width) + snappedTopInset() + snappedBottomInset();
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return container.prefHeight(width) + snappedTopInset() + snappedBottomInset();
    }

    // grow horizontally, but not vertically
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefHeight(-1);
    }



    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    @Override
    protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case FOCUS_NODE: return openMenuButton;
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
}
