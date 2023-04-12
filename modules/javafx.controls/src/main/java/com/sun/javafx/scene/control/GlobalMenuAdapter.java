/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.List;

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

    private final ObservableList<MenuItemBase> items = new TrackableObservableList<>() {
        @Override protected void onChanged(Change<MenuItemBase> c) {
        }
    };

    private GlobalMenuAdapter(final Menu menu) {
        super(menu.getText());

        this.menu = menu;

        bindMenuItemProperties(this, menu);

        menu.showingProperty().addListener(property -> {
            if (menu.isShowing() && !isShowing()) {
                show();
            } else if (!menu.isShowing() && isShowing()) {
                hide();
            }
        });
        showingProperty().addListener(property -> {
            if (isShowing() && !menu.isShowing()) {
                menu.show();
            } else if (!isShowing() && menu.isShowing()) {
                menu.hide();
            }
        });

//        EventHandler<Event> showHideHandler = new EventHandler<Event>() {
//            public void handle(Event ev) {
//                Event.fireEvent(menu, new Event(ev.getEventType()));
//            }
//        };

        menu.getItems().addListener(new ListChangeListener<MenuItem>() {
            @Override public void onChanged(Change<? extends MenuItem> change) {
                while (change.next()) {
                    int from = change.getFrom();
                    int to = change.getTo();
                    List<? extends MenuItem> removed = change.getRemoved();
                    for (int i = from + removed.size() - 1; i >= from ; i--) {
                        items.remove(i);
                        getItems().remove(i);
                    }
                    for (int i = from; i < to; i++) {
                        MenuItem item = change.getList().get(i);
                        insertItem(item, i);
                    }
                }
            }
        });

        for (MenuItem menuItem : menu.getItems()) {
            insertItem(menuItem, items.size());
        }
    }

    private void insertItem(MenuItem menuItem, int pos) {
        MenuItemBase mib;

        if (menuItem instanceof Menu) {
            mib = new GlobalMenuAdapter((Menu)menuItem);
        } else if (menuItem instanceof CheckMenuItem) {
            mib = new CheckMenuItemAdapter((CheckMenuItem)menuItem);
        } else if (menuItem instanceof RadioMenuItem) {
            mib = new RadioMenuItemAdapter((RadioMenuItem)menuItem);
        } else if (menuItem instanceof SeparatorMenuItem) {
            mib = new SeparatorMenuItemAdapter((SeparatorMenuItem)menuItem);
        } else if (menuItem instanceof CustomMenuItem) {
            mib = new CustomMenuItemAdapter((CustomMenuItem)menuItem);
        } else {
            mib = new MenuItemAdapter(menuItem);
        }

        items.add(pos, mib);
        getItems().add(pos, (MenuItem)mib);
    }

    @Override
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

        adapter.setOnAction(ev -> {
            menuItem.fire();
        });
    }

    @Override
    public void fireValidation() {
        if (menu.getOnMenuValidation() != null) {
            Event.fireEvent(menu, new Event(GlobalMenuAdapter.MENU_VALIDATION_EVENT));
        }
        Menu target = menu.getParentMenu();
        if(target != null && target.getOnMenuValidation() != null) {
            Event.fireEvent(target, new Event(MenuItem.MENU_VALIDATION_EVENT));
        }
    }


    private static class MenuItemAdapter extends MenuItem implements MenuItemBase {
        private MenuItem menuItem;

        private MenuItemAdapter(final MenuItem menuItem) {
            super(menuItem.getText());

            this.menuItem = menuItem;

            bindMenuItemProperties(this, menuItem);
        }

        @Override
        public void fireValidation() {
            if (menuItem.getOnMenuValidation() != null) {
                Event.fireEvent(menuItem, new Event(MenuItem.MENU_VALIDATION_EVENT));
            }
            Menu target = menuItem.getParentMenu();
            if(target.getOnMenuValidation() != null) {
                Event.fireEvent(target, new Event(MenuItem.MENU_VALIDATION_EVENT));
            }
        }
    }

    private static class CheckMenuItemAdapter extends CheckMenuItem implements CheckMenuItemBase {
        private CheckMenuItem menuItem;

        private CheckMenuItemAdapter(final CheckMenuItem menuItem) {
            super(menuItem.getText());
            this.menuItem = menuItem;

            bindMenuItemProperties(this, menuItem);

            selectedProperty().bindBidirectional(menuItem.selectedProperty());
        }

        @Override
        public void fireValidation() {
            if (getOnMenuValidation() != null) {
                Event.fireEvent(menuItem, new Event(CheckMenuItemAdapter.MENU_VALIDATION_EVENT));
            }
            Menu target = menuItem.getParentMenu();
            if(target.getOnMenuValidation() != null) {
                Event.fireEvent(target, new Event(MenuItem.MENU_VALIDATION_EVENT));
            }
        }
    }

    private static class RadioMenuItemAdapter extends RadioMenuItem implements RadioMenuItemBase {
        private RadioMenuItem menuItem;

        private RadioMenuItemAdapter(final RadioMenuItem menuItem) {
            super(menuItem.getText());

            this.menuItem = menuItem;

            bindMenuItemProperties(this, menuItem);

            selectedProperty().bindBidirectional(menuItem.selectedProperty());
        }

        @Override
        public void fireValidation() {
            if (getOnMenuValidation() != null) {
                Event.fireEvent(menuItem, new Event(RadioMenuItemAdapter.MENU_VALIDATION_EVENT));
            }
            Menu target = menuItem.getParentMenu();
            if(target.getOnMenuValidation() != null) {
                Event.fireEvent(target, new Event(MenuItem.MENU_VALIDATION_EVENT));
            }
        }
    }

    private static class SeparatorMenuItemAdapter extends SeparatorMenuItem implements SeparatorMenuItemBase {
        private SeparatorMenuItem menuItem;

        private SeparatorMenuItemAdapter(final SeparatorMenuItem menuItem) {
            this.menuItem = menuItem;

            bindMenuItemProperties(this, menuItem);
        }

        @Override
        public void fireValidation() {
            if (getOnMenuValidation() != null) {
                Event.fireEvent(menuItem, new Event(SeparatorMenuItemAdapter.MENU_VALIDATION_EVENT));
            }
            Menu target = menuItem.getParentMenu();
            if(target.getOnMenuValidation() != null) {
                Event.fireEvent(target, new Event(MenuItem.MENU_VALIDATION_EVENT));
            }
        }
    }

    private static class CustomMenuItemAdapter extends CustomMenuItem implements CustomMenuItemBase {
        private CustomMenuItem menuItem;

        private CustomMenuItemAdapter(final CustomMenuItem menuItem) {
            this.menuItem = menuItem;

            bindMenuItemProperties(this, menuItem);
        }

        @Override
        public void fireValidation() {
            if (getOnMenuValidation() != null) {
                Event.fireEvent(menuItem, new Event(CustomMenuItemAdapter.MENU_VALIDATION_EVENT));
            }
            Menu target = menuItem.getParentMenu();
            if(target.getOnMenuValidation() != null) {
                Event.fireEvent(target, new Event(MenuItem.MENU_VALIDATION_EVENT));
            }
        }
    }
}
