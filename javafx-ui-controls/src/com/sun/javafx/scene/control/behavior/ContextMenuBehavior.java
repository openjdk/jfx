/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.behavior;

/**
 * ContextMenuBehavior - default implementation
 *
 * @profile common
 */
public class ContextMenuBehavior { //extends BehaviorBase<ContextMenu> {

    /**************************************************************************
     *                          Setup KeyBindings                             *
     *************************************************************************/
//    @Override protected void callAction(String name) {
//        if (name.equals("Cancel")) cancel();
//        else if (name.equals("Press")) keyPressed();
//        else if (name.equals("Release")) keyReleased();
//        else super.callAction(name);
//    }
    
//    public ContextMenuBehavior(ContextMenu control) {
//        super(control);
//    }

//   public static MenuBar getMenuBar(Menu menu) {
//        Menu rootMenu = getRootMenu(menu);
//        if (rootMenu.getParent() instanceof MenuBar) {
//            return (MenuBar) rootMenu.getParent();
//        }
//        if (rootMenu.getParent() instanceof MenuBarSkin) {
//            return (MenuBar) ((MenuBarSkin) rootMenu.getParent()).getControl();
//        }
//        // We basically assume if the Menu doesn't have a parentMenu, then it
//        // might be in a MenuBar, so we walk up until we find it.
//        if (menu.getParentMenu() == null) {
//            javafx.scene.Parent p = menu.getParent();
//            while (p != null) {
//                if (p instanceof MenuBar) {
//                    return (MenuBar) p;
//                }
//                p = p.getParent();
//            }
//        }
//        return null;
//    }
//
//    public static void moveToPrevMenu(Menu menu, boolean show) {
//        if (menu == null) {
//            return;
//        }
//        Menu rootMenu = getRootMenu(menu);
////        final MenuBar mb = getMenuBar(rootMenu);
////        if (mb == null) {
////            return;
////        }
//        final int pos = mb.getMenus().indexOf(rootMenu);
//        if (pos == -1) {
//            return;
//        }
//        int newPos = pos;
//        Menu newMenu;
//        while (true) {
//            newPos = (newPos == 0) ? (mb.getMenus().size() - 1) : (--newPos);
//            if (newPos == pos) {
//                newMenu = null;
//                break;
//            }
//            newMenu = mb.getMenus().get(newPos);
//            if (newMenu.isVisible() && !newMenu.isDisabled()) {
//                break;
//            }
//        }
//        if (newMenu != null) {
//            rootMenu.hide();
//            newMenu.requestFocus();
//            if (show) {
//                newMenu.show();
//            }
//        }
//    }
//
//    public static void moveToNextMenu(Menu menu, boolean show) {
//        if (menu == null) {
//            return;
//        }
//        Menu rootMenu = getRootMenu(menu);
//        final MenuBar mb = getMenuBar(rootMenu);
//        if (mb == null) {
//            return;
//        }
//        final int pos = mb.getMenus().indexOf(rootMenu);
//        if (pos == -1) {
//            return;
//        }
//        int newPos = pos;
//        Menu newMenu;
//        while (true) {
//            newPos = (newPos == mb.getMenus().size() - 1) ? (0) : (++newPos);
//            if (newPos == pos) {
//                newMenu = null;
//                break;
//            }
//            newMenu = mb.getMenus().get(newPos);
//            if (newMenu.isVisible() && !newMenu.isDisabled()) {
//                break;
//            }
//        }
//        if (newMenu != null) {
//            rootMenu.hide();
//            newMenu.requestFocus();
//            if (show) {
//                newMenu.show();
//            }
//        }
//    }
//
//    static ObservableList<Node> getSiblings(MenuItemBase item) {
//        ObservableList<Node> siblings = null;
//        Menu menu = item.getParentMenu();
//        if (menu != null) siblings = item.getParentMenu().getItems();
//        if (siblings == null) {
//            PopupMenu popup = item.getParentPopup();
//            if (popup != null) siblings = item.getParentPopup().getItems();
//        }
//        return siblings;
//    }
//
//    private static Menu getRootMenu(Menu menu) {
//        if (menu == null || menu.getParentMenu() == null) {
//            return menu;
//        }
//        Menu parentMenu = menu.getParentMenu();
//        while (parentMenu.getParentMenu() != null) {
//            parentMenu = parentMenu.getParentMenu();
//        }
//        return parentMenu;
//    }
//
//    public static Node getFirstValidMenuItem(ObservableList<Node> items) {
//        return getNextValidMenuItem(-1, items);
//    }
//
//    public static Node getLastValidMenuItem(ObservableList<Node> items) {
//        return getPrevValidMenuItem(items.size(), items);
//    }
//
//    public static Node getNextValidMenuItem(int pos, ObservableList<Node> items) {
//        int newPos = pos + 1;
//        int size = items.size();
//        while (newPos < size) {
//            final Node item = items.get(newPos);
//            if (!item.isDisabled() && item instanceof MenuItemBase) {
//                return item;
//            }
//            newPos++;
//        }
//        return null;
//    }
//
//    public static Node getPrevValidMenuItem(int pos, ObservableList<Node> items) {
//        int newPos = pos - 1;
//        while (newPos != -1) {
//            final Node item = items.get(newPos);
//            if (!item.isDisabled() && item instanceof MenuItemBase) {
//                return item;
//            }
//            newPos--;
//        }
//        return null;
//    }
//
//    public static void moveToNextMenuItem(MenuItemBase menuItemBase) {
//        ObservableList<Node> siblings = getSiblings(menuItemBase);
//        if (siblings == null) {
//            return;
//        }
//        int pos = siblings.indexOf(menuItemBase);
//        final int size = siblings.size() - 1;
//        if (pos == size) {
//            // at this point we should wrap to the top of the menu, but we can't go
//            // to a separator, so we special-case this here
//            getFirstValidMenuItem(siblings).requestFocus();
//        } else {
//            getNextValidMenuItem(pos, siblings).requestFocus();
//        }
//    }
//
//    public static void moveToPrevMenuItem(MenuItemBase menuItemBase) {
//        ObservableList<Node> siblings = getSiblings(menuItemBase);
//        if (siblings == null) {
//            return;
//        }
//        int pos = siblings.indexOf(menuItemBase);
//        if (pos == 0) {
//            // at this point we should wrap to the bottom of the menu, but we can't
//            // go to a separator, so we special-case this here
//            getLastValidMenuItem(siblings).requestFocus();
//        } else {
//            getPrevValidMenuItem(pos, siblings).requestFocus();
//        }
//    }
//
//    public static void hideSubmenus(ObservableList<Node> items) {
//        for (Node m : items) {
//            if (m instanceof Menu) {
//                ((Menu) m).hide();
//            }
//        }
//    }
//    /**
//     * These are the default bindings used for Menus.
//     */
//    protected static final ObservableList<KeyBinding> MENU_BINDINGS;
//
//    static {
//        MENU_BINDINGS = FXCollections.<KeyBinding>observableArrayList();
//        /*
//         * NOTE: Currently the menus bindings override the traverse* functions
//         * further down this file. Whether this is wise in the medium/long-terms is
//         * unclear, but this note just serves as a reminder for why this bindings
//         * observableArrayList is relatively bare.
//         */
//        KeyBinding keybinding = new KeyBinding(KeyCode.VK_ESCAPE, "hide");
//        KeyBinding keybinding2 = new KeyBinding(KeyCode.VK_CANCEL, "hide");
//        KeyBinding keybinding3 = new KeyBinding(KeyCode.VK_ENTER, "show");
//
//        ObservableList<KeyBinding> keybindings2 = FXCollections.<KeyBinding>observableArrayList();
//
//        //MENU_BINDINGS.addAll(TraversalBindings.BINDINGS);
//        MENU_BINDINGS.add(keybinding);
//        MENU_BINDINGS.add(keybinding2);
//        MENU_BINDINGS.add(keybinding3);
//        MENU_BINDINGS.addAll(keybindings2);
//    }
//    /**************************************************************************
//     *                          Setup KeyBindings                             *
//     *************************************************************************/
//    ObservableList<KeyBinding> keyBindings = MENU_BINDINGS;
//
//    @Override protected void callAction(String name) {
//        if (name.equals("hide")) {
//            hide();
//        } else if (name.equals("show")) {
//            show();
//        } else {
//            super.callAction(name);
//        }
//    }
//    /**************************************************************************
//     *                         State and Functions                            *
//     *************************************************************************/
//    private final Menu menu = getControl();
//
//    /**
//     * Invoked when a mouse press has occurred over the menu.
//     */
//    @Override public void mousePressed(MouseEvent e) {
//        super.mousePressed(e);
//        if (menu.isShowing() && menu.getParentMenu() == null) {
//            hide();
//        } else {
//            show();
//        }
//    }
//
//    /**
//     * Invoked when a mouse enter event has occurred over the menu.
//     */
//    @Override public void mouseEntered(MouseEvent e) {
//        super.mouseEntered(e);
//        // If the current menu is already showing, then we need not bother
//        // anything here
//        if (menu.isShowing()) {
//            return;
//        }
//        // If this menu is on a menu bar, and if a sibling menu is currently
//        // showing, then we will close that menu and show this one. However,
//        // if this Menu is already showing, then we don't bother.
//        final MenuBar bar = getMenuBar(menu);
//        if (menu.getParentMenu() == null && bar != null) {
//            boolean menuShowing = false;
//            for (Menu m : bar.getMenus()) {
//                if (m.isShowing() && !m.equals(menu)) {
//                    menuShowing = true;
//                    break;
//                }
//            }
//            if (menuShowing) {
//                for (Menu m : bar.getMenus()) {
//                    if (!m.equals(menu)) {
//                        m.hide();
//                    }
//                }
//                // Show this menu and give it focus!
//                show();
//            }
//        } else if (menu.getParentMenu() != null) {
//            // If this wasn't on a bar, but instead, is a sub menu, then we
//            // will close any siblings and show this menu.
//            hideSubmenus(menu.getParentMenu().getItems());
//            show();
//        }
//    }
//
//    @Override
//    public void traverseUp() {
//        moveToPrevMenuItem(menu);
//    }
//
//    @Override
//    public void traverseDown() {
//        moveToNextMenuItem(menu);
//    }
//
//    @Override
//    public void traverseLeft() {
//        if (menu.getParentMenu().getParentMenu() != null) {
//            // just go up to the parent menu
//            hide();
//        } else {
//            moveToPrevMenu(menu, menu.isShowing());
//        }
//    }
//
//    @Override
//    public void traverseRight() {
//        // get the next menu to move to. This will either be a submenu,
//        // or the next item in a menubar, depending on whether this menu is
//        // current showing.
//        if (menu.getParentMenu().isShowing()) {
//            show();
//        } else {
//            moveToNextMenu(menu, false);
//        }
//    }
//
//    private void show() {
//        menu.requestFocus();
//        menu.show();
//    }
//
//    public void hide() {
//        menu.hide();
//        if (menu.getParentMenu() != null) {
//            menu.getParentMenu().hide();
//        }
//        if (menu.getParentPopup() != null) {
//            menu.getParentPopup().hide();
//        }
//    }
}
