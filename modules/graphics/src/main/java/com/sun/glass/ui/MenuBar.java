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

import com.sun.glass.ui.delegate.MenuBarDelegate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MenuBar {

    final private MenuBarDelegate delegate;

    private final List<Menu> menus = new ArrayList<Menu>();

    protected MenuBar() {
        Application.checkEventThread();
        delegate = PlatformFactory.getPlatformFactory().createMenuBarDelegate(this);
        if (!delegate.createMenuBar()) {
            throw new RuntimeException("MenuBar creation error.");
        }
    }

    long getNativeMenu() {
        return delegate.getNativeMenu();
    }

    public void add(Menu menu) {
        Application.checkEventThread();
        insert(menu, menus.size());
    }

    public void insert(Menu menu, int pos) {
        Application.checkEventThread();
        synchronized (menus) {
            if (delegate.insert(menu.getDelegate(), pos)) {
                menus.add(pos, menu);
            }
        }
    }

    public void remove(int pos) {
        Application.checkEventThread();
        synchronized (menus) {
            Menu menu = menus.get(pos);
            if (delegate.remove(menu.getDelegate(), pos)) {
                menus.remove(pos);
            }
        }
    }

    public void remove(Menu menu) {
        Application.checkEventThread();
        synchronized (menus) {
            int pos = menus.indexOf(menu);
            if (pos >= 0) {
                if (delegate.remove(menu.getDelegate(), pos)) {
                    menus.remove(pos);
                }
            } else {
                // throw some exception?
            }
        }
    }

    public List<Menu> getMenus() {
        Application.checkEventThread();
        return Collections.unmodifiableList(menus);
    }
}
