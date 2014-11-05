/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.input.KeyCombination;

import java.util.List;
import java.util.Map;

public class ControlAcceleratorSupport {

    // --- Add

    public static void addAcceleratorsIntoScene(ObservableList<MenuItem> items, Tab anchor) {
        // with Tab, we first need to wait until the Tab has a TabPane associated with it
        addAcceleratorsIntoScene(items, (Object)anchor);
    }

    public static void addAcceleratorsIntoScene(ObservableList<MenuItem> items, TableColumnBase<?,?> anchor) {
        // with TableColumnBase, we first need to wait until it has a TableView/TreeTableView associated with it
        addAcceleratorsIntoScene(items, (Object)anchor);
    }

    public static void addAcceleratorsIntoScene(ObservableList<MenuItem> items, Node anchor) {
        // we allow an empty items list as we install listeners later on - if
        // we return on empty, the listener is never installed (leading to RT-39249)
        if (items == null/* || items.isEmpty()*/) {
            return;
        }

        if (anchor == null) {
            throw new IllegalArgumentException("Anchor cannot be null");
        }

        final Scene scene = anchor.getScene();
        if (scene == null) {
            // listen to the scene property on the anchor until it is set, and
            // then install the accelerators
            anchor.sceneProperty().addListener(new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    Scene scene = anchor.getScene();
                    if (scene != null) {
                        anchor.sceneProperty().removeListener(this);
                        doAcceleratorInstall(items, scene);
                    }
                }
            });
        } else {
            doAcceleratorInstall(items, scene);
        }
    }

    private static void addAcceleratorsIntoScene(ObservableList<MenuItem> items, Object anchor) {
        // with TableColumnBase, we first need to wait until it has a TableView/TreeTableView associated with it
        if (anchor == null) {
            throw new IllegalArgumentException("Anchor cannot be null");
        }

        final ReadOnlyObjectProperty<? extends Control> controlProperty = getControlProperty(anchor);
        final Control control = controlProperty.get();
        if (control == null) {
            controlProperty.addListener(new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    final Control control = controlProperty.get();
                    if (control != null) {
                        controlProperty.removeListener(this);
                        addAcceleratorsIntoScene(items, control);
                    }
                }
            });
        } else {
            addAcceleratorsIntoScene(items, control);
        }
    }

    private static void doAcceleratorInstall(final ObservableList<MenuItem> items, final Scene scene) {
        // we're given an observable list of menu items, which we will add an observer to
        // so that when menu items are added or removed we can properly handle
        // the addition or removal of accelerators into the scene.
        items.addListener((ListChangeListener<MenuItem>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    // remove accelerators from the scene
                    removeAcceleratorsFromScene(c.getRemoved(), scene);
                }

                if (c.wasAdded()) {
                    ControlAcceleratorSupport.doAcceleratorInstall(c.getAddedSubList(), scene);
                }
            }
        });

        doAcceleratorInstall((List<MenuItem>)items, scene);
    }


    private static void doAcceleratorInstall(final List<? extends MenuItem> items, final Scene scene) {
        for (final MenuItem menuitem : items) {
            if (menuitem instanceof Menu) {
                // add accelerators for this Menu's menu items, by calling recursively.
                doAcceleratorInstall(((Menu) menuitem).getItems(), scene);
            } else {
                // check if there is any accelerator on this menuitem right now.
                // If there is, then we create a Runnable and set it into the
                // scene straight away
                if (menuitem.getAccelerator() != null) {
                    final Map<KeyCombination, Runnable> accelerators = scene.getAccelerators();

                    Runnable acceleratorRunnable = () -> {
                        if (menuitem.getOnMenuValidation() != null) {
                            Event.fireEvent(menuitem, new Event(MenuItem.MENU_VALIDATION_EVENT));
                        }
                        Menu target = menuitem.getParentMenu();
                        if(target!= null && target.getOnMenuValidation() != null) {
                            Event.fireEvent(target, new Event(MenuItem.MENU_VALIDATION_EVENT));
                        }
                        if (!menuitem.isDisable()) {
                            if (menuitem instanceof RadioMenuItem) {
                                ((RadioMenuItem)menuitem).setSelected(!((RadioMenuItem)menuitem).isSelected());
                            }
                            else if (menuitem instanceof CheckMenuItem) {
                                ((CheckMenuItem)menuitem).setSelected(!((CheckMenuItem)menuitem).isSelected());
                            }

                            menuitem.fire();
                        }
                    };
                    accelerators.put(menuitem.getAccelerator(), acceleratorRunnable);
                }

                // We also listen to the accelerator property for changes, such
                // that we can update the scene when a menu item accelerator changes.
                menuitem.acceleratorProperty().addListener((observable, oldValue, newValue) -> {
                    final Map<KeyCombination, Runnable> accelerators = scene.getAccelerators();

                    // remove the old KeyCombination from the accelerators map
                    Runnable _acceleratorRunnable = accelerators.remove(oldValue);

                    // and put in the new accelerator KeyCombination, if it is not null
                    if (newValue != null) {
                        accelerators.put(newValue, _acceleratorRunnable);
                    }
                });
            }
        }
    }



    // --- Remove

    public static void removeAcceleratorsFromScene(List<? extends MenuItem> items, Tab anchor) {
        TabPane tabPane = anchor.getTabPane();
        if (tabPane == null) return;

        Scene scene = tabPane.getScene();
        removeAcceleratorsFromScene(items, scene);
    }

    public static void removeAcceleratorsFromScene(List<? extends MenuItem> items, TableColumnBase<?,?> anchor) {
        ReadOnlyObjectProperty<? extends Control> controlProperty = getControlProperty(anchor);
        if (controlProperty == null) return;

        Control control = controlProperty.get();
        if (control == null) return;

        Scene scene = control.getScene();
        removeAcceleratorsFromScene(items, scene);
    }

    public static void removeAcceleratorsFromScene(List<? extends MenuItem> items, Node anchor) {
        Scene scene = anchor.getScene();
        removeAcceleratorsFromScene(items, scene);
    }

    public static void removeAcceleratorsFromScene(List<? extends MenuItem> items, Scene scene) {
        if (scene == null) {
            return;
        }

        for (final MenuItem menuitem : items) {
            if (menuitem instanceof Menu) {
                // TODO remove the menu listener from the menu.items list

                // remove the accelerators of items contained within the menu
                removeAcceleratorsFromScene(((Menu)menuitem).getItems(), scene);
            } else {
                // remove the removed MenuItem accelerator KeyCombination from
                // the scene accelerators map
                final Map<KeyCombination, Runnable> accelerators = scene.getAccelerators();
                accelerators.remove(menuitem.getAccelerator());
            }
        }
    }



    // --- Utilities

    private static ReadOnlyObjectProperty<? extends Control> getControlProperty(Object obj) {
        if (obj instanceof TableColumn) {
            return ((TableColumn)obj).tableViewProperty();
        } else if (obj instanceof TreeTableColumn) {
            return ((TreeTableColumn)obj).treeTableViewProperty();
        } else if (obj instanceof Tab) {
            return ((Tab)obj).tabPaneProperty();
        }

        return null;
    }
}
