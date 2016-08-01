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
package com.sun.glass.ui.win;

import com.sun.glass.ui.Menu;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.delegate.MenuDelegate;
import com.sun.glass.ui.delegate.MenuItemDelegate;

final class WinMenuDelegate extends WinMenuImpl implements MenuDelegate {

    final private Menu owner;
    private WinMenuImpl parent = null;

    public WinMenuDelegate(Menu menu) {
        this.owner = menu;
    }


    public Menu getOwner() {
        return owner;
    }

    @Override public boolean createMenu(String title, boolean enabled) {
        return create();
    }

    public void dispose() {
        destroy();
    }

    // all methods report success status (true - success, false - failure)

    @Override public boolean setTitle(String title) {
        if (parent != null) {
            return parent.setSubmenuTitle(this, title);
        }
        return true;
    }

    @Override public boolean setEnabled(boolean enabled) {
        if (parent != null) {
            return parent.enableSubmenu(this, enabled);
        }
        return true;
    }

    @Override public boolean setPixels(Pixels pixels) {
        // TODO: implement images in menuItem
        return false;
    }

    @Override public boolean insert(MenuDelegate menu, int pos) {
        return insertSubmenu((WinMenuDelegate)menu, pos);
    }

    // if item == null => insert Separator
    @Override public boolean insert(MenuItemDelegate item, int pos) {
        return insertItem((WinMenuItemDelegate)item, pos);
    }

    // removes submenu at {@code pos} which delegate is {@code menu} parameter
    @Override public boolean remove(MenuDelegate menu, int pos) {
        return removeMenu((WinMenuDelegate)menu, pos);
    }

    // removes submenu at {@code pos} which delegate is {@code item} parameter
    @Override public boolean remove(MenuItemDelegate item, int pos) {
        return removeItem((WinMenuItemDelegate)item, pos);
    }

    WinMenuImpl getParent() {
        return parent;
    }

    void setParent(WinMenuImpl newParent) {
        parent = newParent;
    }
}
