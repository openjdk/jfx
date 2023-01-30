/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.MenuItem;
import com.sun.glass.ui.MenuItem.Callback;
import com.sun.glass.ui.Window;

/*
 * Base Windows menu implementation.
 * Used by WinMenuDelegate & WindowsMenubarDelegate.
 * This is package private class.
 */
class WinMenuImpl {

    private static native void _initIDs();
    static {
        _initIDs();
    }

    private long ptr = 0;   // HMENU

    WinMenuImpl() {
    }

    long getHMENU() {
        return ptr;
    }

    boolean create() {
        ptr = _create();
        return (ptr != 0);
    }

    void destroy() {
        if (ptr != 0) {
            _destroy(ptr);
            ptr = 0;
        }
    }

    boolean insertSubmenu(WinMenuDelegate menu, int pos) {
        menu.setParent(this);
        if (!_insertSubmenu(ptr, pos, menu.getHMENU(),
                menu.getOwner().getTitle(), menu.getOwner().isEnabled())) {
            menu.setParent(null);
            return false;
        }
        return true;
    }


    boolean insertItem(WinMenuItemDelegate item, int pos) {
        if (item == null) {
            return _insertSeparator(ptr, pos);
        }

        item.setParent(this);   // to get cmdID

        if (!_insertItem(ptr, pos, item.getCmdID(),
                    item.getOwner().getTitle(),
                    item.getOwner().isEnabled(),
                    item.getOwner().isChecked(),
                    item.getOwner().getCallback(),
                    item.getOwner().getShortcutKey(),
                    item.getOwner().getShortcutModifiers())) {
            item.setParent(null);
            return false;
        }
        return true;
    }

    boolean removeMenu(WinMenuDelegate submenu, int pos) {
        if (_removeAtPos(ptr, pos)) {
            submenu.setParent(null);
            return true;
        }
        return false;
    }

    boolean removeItem(WinMenuItemDelegate item, int pos) {
        if (_removeAtPos(ptr, pos)) {
            if (item != null) {  // null means it's a separator
                item.setParent(null);
            }
            return true;
        }
        return false;
    }

    boolean setSubmenuTitle(WinMenuDelegate submenu, String title) {
        return _setSubmenuTitle(ptr, submenu.getHMENU(), title);
    }

    boolean setItemTitle(WinMenuItemDelegate submenu, String title) {
        return _setItemTitle(ptr, submenu.getCmdID(), title);
    }

    boolean enableSubmenu(WinMenuDelegate submenu, boolean enable) {
        return _enableSubmenu(ptr, submenu.getHMENU(), enable);
    }

    boolean enableItem(WinMenuItemDelegate item, boolean enable) {
        return _enableItem(ptr, item.getCmdID(), enable);
    }

    public boolean checkItem(WinMenuItemDelegate item, boolean check) {
        return _checkItem(ptr, item.getCmdID(), check);
    }

    // callback for native
    private static boolean notifyCommand(Window window, int cmdID) {
        WinMenuItemDelegate item = WinMenuItemDelegate.CommandIDManager.getHandler(cmdID);
        if (item != null) {
            MenuItem.Callback callback = item.getOwner().getCallback();
            if (callback != null) {
                callback.action();
                return true;
            }
        }
        return false;
    }

    // *****************************************************
    // native methods
    // all methods return true on success, false on failure

    // menu methods
    private native long _create();
    private native void _destroy(long ptr);

    private native boolean _insertItem(long ptr, int pos, int cmdID,
            String title, boolean enabled, boolean checked,
            Callback callback, int shortcut, int modifiers);
    private native boolean _insertSubmenu(long ptr, int pos, long subPtr,
            String title, boolean enabled);
    private native boolean _insertSeparator(long ptr, int pos);

    private native boolean _removeAtPos(long ptr, int pos);

    // element methods
    private native boolean _setItemTitle(long ptr, int cmdID, String title);
    private native boolean _setSubmenuTitle(long ptr, long subPtr, String title);

    private native boolean _enableItem(long ptr, int cmdID, boolean enabled);
    private native boolean _enableSubmenu(long ptr, long subPtr, boolean enabled);

    private native boolean _checkItem(long ptr, int cmdID, boolean checked);
}
