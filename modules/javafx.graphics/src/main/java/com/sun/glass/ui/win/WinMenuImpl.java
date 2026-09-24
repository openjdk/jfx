/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Base Windows menu implementation.
 * Used by WinMenuDelegate & WindowsMenubarDelegate.
 * This is package private class.
 *
 * Every menu operation is a call into WinGlassNative, which binds the user32 entry point
 * GlassMenu.cpp used to wrap: CreateMenu, DestroyMenu, InsertMenuItemW, InsertMenuW, RemoveMenu,
 * SetMenuItemInfoW, EnableMenuItem and CheckMenuItem. The HMENU is unchanged - an opaque handle
 * value this class owns as a long and hands to WinMenuBarDelegate.getNativeMenu(), which
 * Window.setMenuBar still passes to the JNI of GlassWindow.cpp.
 *
 * No native is left. The last one was _initIDs, which was not a menu operation at all: it cached the
 * jclass and the method id that GlassMenu.cpp's HandleMenuCommand used to deliver WM_COMMAND to
 * notifyCommand below. That upcall runs inside DispatchMessage, in GlassWindow::WindowProc, so it
 * could never become a downcall; what replaced it is the callback table gwin_menu_set_callbacks,
 * installed by the static block below, at exactly the point _initIDs used to run. That timing is
 * part of the behaviour, not an implementation detail: until something touches this class there is
 * no target, and every WM_COMMAND is silently "not handled" and goes to DefWindowProc - which is
 * precisely what jMenuClass == NULL did.
 * <p>
 * Line numbers into {@code GlassMenu.cpp} and {@code GlassWindow.cpp} refer to those files at commit
 * {@code 8492cb03b0} ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>}).
 */
class WinMenuImpl {

    static {
        WinGlassNative.installMenuCallback();
    }

    private long ptr = 0;   // HMENU

    WinMenuImpl() {
    }

    long getHMENU() {
        return ptr;
    }

    boolean create() {
        ptr = WinGlassNative.menuCreate();
        return (ptr != 0);
    }

    void destroy() {
        if (ptr != 0) {
            WinGlassNative.menuDestroy(ptr);
            ptr = 0;
        }
    }

    boolean insertSubmenu(WinMenuDelegate menu, int pos) {
        menu.setParent(this);
        if (!WinGlassNative.menuInsertSubmenu(ptr, pos, menu.getHMENU(),
                menu.getOwner().getTitle(), menu.getOwner().isEnabled())) {
            menu.setParent(null);
            return false;
        }
        return true;
    }


    boolean insertItem(WinMenuItemDelegate item, int pos) {
        if (item == null) {
            return WinGlassNative.menuInsertSeparator(ptr, pos);
        }

        item.setParent(this);   // to get cmdID

        MenuItem owner = item.getOwner();
        String title = owner.getTitle();
        boolean enabled = owner.isEnabled();
        boolean checked = owner.isChecked();
        // The native this replaces took three more arguments and used none of them: the callback
        // became a NewGlobalRef that was never stored and never deleted (GlassMenu.cpp:167,178-179,
        // one leaked reference per inserted item), and the shortcut pair was never rendered. They
        // are still read here, and the six owner getters keep their order (only item.getCmdID(), a
        // field read with no check, moved after them), because each of these getters calls
        // Application.checkEventThread() - dropping the calls would drop that check, which is a
        // behaviour change however invisible the values are.
        Callback unusedCallback = owner.getCallback();
        int unusedShortcutKey = owner.getShortcutKey();
        int unusedShortcutModifiers = owner.getShortcutModifiers();

        if (!WinGlassNative.menuInsertItem(ptr, pos, item.getCmdID(), title, enabled, checked)) {
            item.setParent(null);
            return false;
        }
        return true;
    }

    boolean removeMenu(WinMenuDelegate submenu, int pos) {
        if (WinGlassNative.menuRemoveAtPos(ptr, pos)) {
            submenu.setParent(null);
            return true;
        }
        return false;
    }

    boolean removeItem(WinMenuItemDelegate item, int pos) {
        if (WinGlassNative.menuRemoveAtPos(ptr, pos)) {
            if (item != null) {  // null means it's a separator
                item.setParent(null);
            }
            return true;
        }
        return false;
    }

    boolean setSubmenuTitle(WinMenuDelegate submenu, String title) {
        return WinGlassNative.menuSetSubmenuTitle(ptr, submenu.getHMENU(), title);
    }

    boolean setItemTitle(WinMenuItemDelegate submenu, String title) {
        return WinGlassNative.menuSetItemTitle(ptr, submenu.getCmdID(), title);
    }

    boolean enableSubmenu(WinMenuDelegate submenu, boolean enable) {
        return WinGlassNative.menuEnableSubmenu(ptr, submenu.getHMENU(), enable);
    }

    boolean enableItem(WinMenuItemDelegate item, boolean enable) {
        return WinGlassNative.menuEnableItem(ptr, item.getCmdID(), enable);
    }

    public boolean checkItem(WinMenuItemDelegate item, boolean check) {
        return WinGlassNative.menuCheckItem(ptr, item.getCmdID(), check);
    }

    /*
     * WM_COMMAND, delivered by GlassMenu.cpp's HandleMenuCommand through the GwinMenuCallbacks table
     * and WinGlassNative.onNotifyCommand. Runs on the Glass toolkit thread, inside DispatchMessage,
     * and frequently inside the system's modal menu loop; callback.action() below is arbitrary
     * application code that may open a dialog or nest another message loop.
     *
     * The return value is load-bearing: true means handled, and GlassWindow::WindowProc then answers
     * return 0 instead of falling through to DefWindowProc (GlassWindow.cpp:461-465). The C tests
     * "non-zero", never "== 1".
     *
     * The com.sun.glass.ui.Window this took while it was a JNI callback is gone. It was computed
     * natively (GlassWindow::FromHandle(hWnd)->GetJObject()) purely to satisfy the signature
     * GlassMenu.cpp asserted, and the body below never mentioned it: the map from cmdID to peer is
     * CommandIDManager, which the id alone indexes.
     *
     * Package-private rather than private because WinGlassNative.onNotifyCommand is the upcall stub's
     * target and calls this - the same shape as WinPreferences.update behind onPreferencesChanged.
     */
    static boolean notifyCommand(int cmdID) {
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

}
