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

package com.sun.javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;

import com.sun.javafx.menu.CheckMenuItemBase;
import com.sun.javafx.menu.CustomMenuItemBase;
import com.sun.javafx.menu.MenuBase;
import com.sun.javafx.menu.MenuItemBase;
import com.sun.javafx.menu.RadioMenuItemBase;
import com.sun.javafx.menu.SeparatorMenuItemBase;

import com.sun.javafx.collections.TrackableObservableList;

public class GlobalMenuAdapter extends Menu implements MenuBase {
    private Menu menu;

    public static MenuBase adapt(Menu menu) {
        return new GlobalMenuAdapter(menu);
    }

    private GlobalMenuAdapter(final Menu menu) {
        super(menu.getText());

        this.menu = menu;

        bindMenuItemProperties(this, menu);

        menu.showingProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable property) {
                if (menu.isShowing()) {
                    show();
                } else {
                    hide();
                }
            }
        });
        showingProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable property) {
                if (isShowing()) {
                    menu.show();
                } else {
                    menu.hide();
                }
            }
        });

        EventHandler<Event> showHideHandler = new EventHandler<Event>() {
            public void handle(Event ev) {
                Event.fireEvent(menu, new Event(ev.getEventType()));
            }
        };

        setOnShowing(showHideHandler);
        setOnShown(showHideHandler);
        setOnHiding(showHideHandler);
        setOnHidden(showHideHandler);

        menu.getItems().addListener(new ListChangeListener<MenuItem>() {
            @Override public void onChanged(Change<? extends MenuItem> c) {
                updateItems();
            }
        });

        updateItems();
    }

    private final ObservableList<MenuItemBase> items = new TrackableObservableList<MenuItemBase>() {
        @Override protected void onChanged(Change<MenuItemBase> c) {
        }
    };

    private void updateItems() {
        items.clear();
        for (MenuItem menuItem : menu.getItems()) {
            if (menuItem instanceof Menu) {
                items.add(new GlobalMenuAdapter((Menu)menuItem));
            } else if (menuItem instanceof CheckMenuItem) {
                items.add(new CheckMenuItemAdapter((CheckMenuItem)menuItem));
            } else if (menuItem instanceof RadioMenuItem) {
                items.add(new RadioMenuItemAdapter((RadioMenuItem)menuItem));
            } else if (menuItem instanceof SeparatorMenuItem) {
                items.add(new SeparatorMenuItemAdapter((SeparatorMenuItem)menuItem));
            } else if (menuItem instanceof CustomMenuItem) {
                items.add(new CustomMenuItemAdapter((CustomMenuItem)menuItem));
            } else {
                items.add(new MenuItemAdapter(menuItem));
            }
        }
        
        // Set the items in super, so setShowing() can see that it's not empty.
        getItems().clear();
        for (MenuItemBase mib : items) {
            getItems().add((MenuItem)mib);
        }
    }

    public final ObservableList<MenuItemBase> getItemsBase() {
        return items;
    }


    private static void bindMenuItemProperties(MenuItem adapter, final MenuItem menuItem) {
        adapter.idProperty().bind(menuItem.idProperty());
        adapter.textProperty().bind(menuItem.textProperty());
        adapter.graphicProperty().bind(menuItem.graphicProperty());
        adapter.disableProperty().bind(menuItem.disableProperty());
        adapter.visibleProperty().bind(menuItem.visibleProperty());
        adapter.acceleratorProperty().bind(menuItem.acceleratorProperty());
        adapter.mnemonicParsingProperty().bind(menuItem.mnemonicParsingProperty());

        adapter.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent ev) {
                menuItem.fire();
            }
        });
    }


    private class MenuItemAdapter extends MenuItem implements MenuItemBase {
        private MenuItem menuItem;

        private MenuItemAdapter(final MenuItem menuItem) {
            super(menuItem.getText());

            this.menuItem = menuItem;

            bindMenuItemProperties(this, menuItem);
        }

    }

    private class CheckMenuItemAdapter extends CheckMenuItem implements CheckMenuItemBase {
        private CheckMenuItem menuItem;

        private CheckMenuItemAdapter(final CheckMenuItem menuItem) {
            super(menuItem.getText());

            this.menuItem = menuItem;

            bindMenuItemProperties(this, menuItem);

            selectedProperty().bindBidirectional(menuItem.selectedProperty());
        }

    }

    private class RadioMenuItemAdapter extends RadioMenuItem implements RadioMenuItemBase {
        private RadioMenuItem menuItem;

        private RadioMenuItemAdapter(final RadioMenuItem menuItem) {
            super(menuItem.getText());

            this.menuItem = menuItem;

            bindMenuItemProperties(this, menuItem);

            selectedProperty().bindBidirectional(menuItem.selectedProperty());
        }

    }

    private class SeparatorMenuItemAdapter extends SeparatorMenuItem implements SeparatorMenuItemBase {
        private SeparatorMenuItem menuItem;

        private SeparatorMenuItemAdapter(final SeparatorMenuItem menuItem) {
            this.menuItem = menuItem;

            bindMenuItemProperties(this, menuItem);
        }

    }

    private class CustomMenuItemAdapter extends CustomMenuItem implements CustomMenuItemBase {
        private CustomMenuItem menuItem;

        private CustomMenuItemAdapter(final CustomMenuItem menuItem) {
            this.menuItem = menuItem;

            bindMenuItemProperties(this, menuItem);
        }

    }
}
