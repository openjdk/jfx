/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.swt;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.Menu;
import com.sun.glass.ui.MenuItem;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.delegate.MenuDelegate;
import com.sun.glass.ui.delegate.MenuItemDelegate;

final class SWTMenuDelegate implements MenuDelegate, MenuItemDelegate {
    
    String title = "";
    MenuItem.Callback callback;
    int shortcutKey;
    int shortcutModifiers;
    Pixels pixels;
    boolean enabled;
    boolean checked;
    
    org.eclipse.swt.widgets.Menu menu;
    org.eclipse.swt.widgets.MenuItem item;
    
    Menu menu2;
    
    public SWTMenuDelegate(Menu menu2) {
        this.menu2 = menu2;
    }

    public SWTMenuDelegate() {
    }

    public boolean createMenu(String title, boolean enabled) {
        this.title = title;
        this.enabled = enabled;
        this.menu = new org.eclipse.swt.widgets.Menu(SWTView.hiddenShell, SWT.DROP_DOWN);
        this.menu.addListener(SWT.Show, new Listener () {
            public void handleEvent(Event event) {
                Menu menu2 = SWTMenuDelegate.this.menu2;
                if (menu2.getEventHandler() != null) {
                    menu2.getEventHandler().handleMenuOpening(menu2, System.nanoTime());
                }
            }
        });
        this.menu.addListener(SWT.Hide, new Listener () {
            public void handleEvent(Event event) {
                Menu menu2 = SWTMenuDelegate.this.menu2;
                if (menu2.getEventHandler() != null) {
                    menu2.getEventHandler().handleMenuClosed(menu2, System.nanoTime());
                }
            }
        });
        return true;
    }

    public boolean createMenuItem(String title, MenuItem.Callback callback, int shortcutKey, int shortcutModifiers, Pixels pixels, boolean enabled, boolean checked) {
        this.title = title;
        this.callback = callback;
        this.shortcutKey = shortcutKey;
        this.shortcutModifiers = shortcutModifiers;
        this.pixels = pixels;
        this.enabled = enabled;
        this.checked = checked;
        return true;
    }

    public boolean setTitle(String title) {
        this.title = title;
        if (item != null) item.setText(title);
        return true;
    }

    public boolean setCallback(final MenuItem.Callback callback) {
        this.callback = callback;
        if (item !=  null) {
            item.addListener(SWT.Selection, event -> {
                item.setSelection(false);
                callback.action();
            });
        }
        return true;
    }

    public boolean setShortcut(int shortcutKey, int shortcutModifiers) {
        this.shortcutKey = shortcutKey;
        this.shortcutModifiers = shortcutModifiers;
        if (item != null) {
            int modifier = 0;
            if ((shortcutModifiers & KeyEvent.MODIFIER_SHIFT) != 0) modifier |= SWT.SHIFT;
            if ((shortcutModifiers & KeyEvent.MODIFIER_CONTROL) != 0) modifier |= SWT.CONTROL;
            if ((shortcutModifiers & KeyEvent.MODIFIER_ALT) != 0) modifier |= SWT.ALT;
            if ((shortcutModifiers & KeyEvent.MODIFIER_COMMAND) != 0) modifier |= SWT.COMMAND;
            int key = SWTApplication.getSWTKeyCode(shortcutKey);
            item.setAccelerator(modifier | key);
        }
        return true;
    }

    public boolean setPixels(Pixels pixels) {
        this.pixels = pixels;
        if (item != null) {
            Image oldImage = item.getImage();
            if (oldImage != null) {
                item.setImage(null);
                oldImage.dispose();
            }
            Image newImage = SWTApplication.createImage(pixels);
            item.setImage(newImage);
        }
        return true;
    }

    public boolean setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (item != null) item.setEnabled(enabled);
        return true;
    }

    public boolean setChecked(boolean checked) {
        this.checked = checked;
        if (item != null) item.setSelection(checked);
        return true;
    }

    public boolean insert(final MenuDelegate menu, int pos) {
        ((SWTMenuDelegate)menu).item = new org.eclipse.swt.widgets.MenuItem(this.menu, SWT.CASCADE, pos);
        ((SWTMenuDelegate)menu).item.setMenu(((SWTMenuDelegate)menu).menu);
        ((SWTMenuDelegate)menu).setTitle(((SWTMenuDelegate)menu).title);
        ((SWTMenuDelegate)menu).setPixels(((SWTMenuDelegate)menu).pixels);
        ((SWTMenuDelegate)menu).setEnabled(((SWTMenuDelegate)menu).enabled);
        ((SWTMenuDelegate)menu).item.addListener(SWT.Dispose, event -> {
            Image oldImage = ((SWTMenuDelegate)menu).item.getImage();
            if (oldImage != null) {
                ((SWTMenuDelegate)menu).item.setImage(null);
                oldImage.dispose();
            }
            ((SWTMenuDelegate)menu).item = null;
        });
        return true;
    }

    public boolean insert(final MenuItemDelegate item, int pos) {
        if (item == null) {
            new org.eclipse.swt.widgets.MenuItem(this.menu, SWT.SEPARATOR, pos);
        } else {
            ((SWTMenuDelegate)item).item = new org.eclipse.swt.widgets.MenuItem(this.menu, SWT.CHECK, pos);
            ((SWTMenuDelegate)item).item.addListener(SWT.Dispose, event -> {
                Image oldImage = ((SWTMenuDelegate)item).item.getImage();
                if (oldImage != null) {
                    ((SWTMenuDelegate)item).item.setImage(null);
                    oldImage.dispose();
                }
                ((SWTMenuDelegate)item).item = null;
            });
            ((SWTMenuDelegate)item).setTitle(((SWTMenuDelegate)item).title);
            ((SWTMenuDelegate)item).setPixels(((SWTMenuDelegate)item).pixels);
            ((SWTMenuDelegate)item).setEnabled(((SWTMenuDelegate)item).enabled);
            ((SWTMenuDelegate)item).setChecked(((SWTMenuDelegate)item).checked);
            ((SWTMenuDelegate)item).setShortcut(((SWTMenuDelegate)item).shortcutKey, ((SWTMenuDelegate)item).shortcutModifiers);
            ((SWTMenuDelegate)item).setCallback(((SWTMenuDelegate)item).callback);
        }
        return true;
    }

    public boolean remove(MenuDelegate menu, int pos) {
        if (0 <= pos && pos < ((SWTMenuDelegate)menu).menu.getItemCount()) {
            ((SWTMenuDelegate)menu).menu.getItem(pos).dispose();
            ((SWTMenuDelegate)menu).menu = null;
            ((SWTMenuDelegate)menu).item = null;
        }
        return true;
    }

    public boolean remove(MenuItemDelegate item, int pos) {
        if (0 <= pos && pos < this.menu.getItemCount()) {
            this.menu.getItem(pos).dispose();
            ((SWTMenuDelegate)item).item = null;
        }
        return true;
    }
}
