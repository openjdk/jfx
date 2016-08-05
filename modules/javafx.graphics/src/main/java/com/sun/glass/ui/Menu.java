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
package com.sun.glass.ui;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.delegate.MenuDelegate;
import com.sun.glass.ui.delegate.MenuItemDelegate;

public final class Menu {

    public static class EventHandler {
        // currently used only on Mac OS X
        public void handleMenuOpening(Menu menu, long time) {
        }

        // currently used only on Mac OS X
        public void handleMenuClosed(Menu menu, long time) {
        }
    }

    public EventHandler getEventHandler() {
        Application.checkEventThread();
        return eventHandler;
    }

    public void setEventHandler(EventHandler eventHandler) {
        Application.checkEventThread();
        this.eventHandler = eventHandler;
    }

    private final MenuDelegate delegate;

    private String title;
    private boolean enabled;
    private final List<Object> items = new ArrayList<Object>();

    private EventHandler eventHandler;

    protected Menu(String title) {
        this(title, true);
    }

    protected Menu(String title, boolean enabled) {
        Application.checkEventThread();
        this.title = title;
        this.enabled = enabled;
        delegate = PlatformFactory.getPlatformFactory().createMenuDelegate(this);
        if (!delegate.createMenu(title, enabled)) {
            throw new RuntimeException("Menu creation error.");
        }
    }

    public String getTitle() {
        Application.checkEventThread();
        return title;
    }

    public void setTitle(String title) {
        Application.checkEventThread();
        if (delegate.setTitle(title)) {
            this.title = title;
        }
    }

    public boolean isEnabled() {
        Application.checkEventThread();
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        Application.checkEventThread();
        if (delegate.setEnabled(enabled)) {
            this.enabled = enabled;
        }
    }

    public boolean setPixels(Pixels pixels) {
        Application.checkEventThread();
        return (delegate.setPixels(pixels));
    }

    /**
     * Gets list of menu elements.
     * Objects in the list returned are instances of Menu or MenuItem.
     */
    public List<Object> getItems() {
        Application.checkEventThread();
        return Collections.unmodifiableList(items);
    }

    public void add(Menu menu) {
        Application.checkEventThread();
        insert(menu, items.size());
    }

    public void add(MenuItem item) {
        Application.checkEventThread();
        insert(item, items.size());
    }

    public void insert(Menu menu, int pos) throws IndexOutOfBoundsException {
        Application.checkEventThread();
        if (menu == null) {
            throw new IllegalArgumentException();
        }
        synchronized (items) {
            if (pos < 0 || pos > items.size()) {
                throw new IndexOutOfBoundsException();
            }
            MenuDelegate menuDelegate = menu.getDelegate();
            if (delegate.insert(menuDelegate, pos)) {
                items.add(pos, menu);
            }
        }
    }

    public void insert(MenuItem item, int pos) throws IndexOutOfBoundsException {
        Application.checkEventThread();
        synchronized (items) {
            if (pos < 0 || pos > items.size()) {
                throw new IndexOutOfBoundsException();
            }
            MenuItemDelegate itemDelegate = item != null ? item.getDelegate() : null;
            if (delegate.insert(itemDelegate, pos)) {
                items.add(pos, item);
            }
        }
    }

    public void remove(int pos) throws IndexOutOfBoundsException {
        Application.checkEventThread();
        synchronized (items) {
            Object item = items.get(pos);   // throws IndexOutOfBoundsException
            boolean success = false;
            if (item == MenuItem.Separator) {
                success = delegate.remove((MenuItemDelegate)null, pos);
            } else if (item instanceof MenuItem) {
                success = delegate.remove(((MenuItem)item).getDelegate(), pos);
            } else { // Menu
                success = delegate.remove(((Menu)item).getDelegate(), pos);
            }
            if (success) {
                items.remove(pos);
            }
        }
    }

    // public void remove(Menu menu)
    // public void remove(MenuItem item);

    // package private
    MenuDelegate getDelegate() {
        return delegate;
    }

    // *****************************************************
    // notification callbacks
    // *****************************************************
    protected void notifyMenuOpening() {
        if (this.eventHandler != null) {
            eventHandler.handleMenuOpening(this, System.nanoTime());
        }
    }

    protected void notifyMenuClosed() {
        if (this.eventHandler != null) {
            eventHandler.handleMenuClosed(this, System.nanoTime());
        }
    }
}

