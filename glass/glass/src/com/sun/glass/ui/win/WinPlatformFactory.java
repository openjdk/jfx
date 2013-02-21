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
import com.sun.glass.ui.MenuBar;
import com.sun.glass.ui.MenuItem;
import com.sun.glass.ui.PlatformFactory;
import com.sun.glass.ui.accessible.AccessibleBaseProvider;
import com.sun.glass.ui.accessible.AccessibleRoot;
import com.sun.glass.ui.accessible.win.WinAccessibleBaseProvider;
import com.sun.glass.ui.accessible.win.WinAccessibleGridItemProvider;
import com.sun.glass.ui.accessible.win.WinAccessibleGridProvider;
import com.sun.glass.ui.accessible.win.WinAccessibleRangeValueProvider;
import com.sun.glass.ui.accessible.win.WinAccessibleRoot;
import com.sun.glass.ui.accessible.win.WinAccessibleSelectionItemProvider;
import com.sun.glass.ui.accessible.win.WinAccessibleSelectionProvider;
import com.sun.glass.ui.accessible.win.WinAccessibleToggleProvider;
import com.sun.glass.ui.delegate.MenuDelegate;
import com.sun.glass.ui.delegate.ClipboardDelegate;
import com.sun.glass.ui.delegate.MenuBarDelegate;
import com.sun.glass.ui.delegate.MenuItemDelegate;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.providers.GridItemProvider;
import com.sun.javafx.accessible.providers.GridProvider;
import com.sun.javafx.accessible.providers.RangeValueProvider;
import com.sun.javafx.accessible.providers.SelectionItemProvider;
import com.sun.javafx.accessible.providers.SelectionProvider;
import com.sun.javafx.accessible.providers.ToggleProvider;

public final class WinPlatformFactory extends PlatformFactory {

    @Override public WinApplication createApplication() {
        return new WinApplication();
    }
    
    @Override public AccessibleRoot createAccessibleRoot(Object node, long ptr) {
        return new WinAccessibleRoot(node, ptr);
    }
    
    @Override public AccessibleBaseProvider createAccessibleProvider(Object node) {
        WinAccessibleBaseProvider element = null;
        if (node instanceof AccessibleProvider) {
            element = new WinAccessibleBaseProvider(node);
        }
        if (node instanceof GridItemProvider) { 
            element.addPatternProviders(
                new WinAccessibleGridItemProvider(node, element.getNativeAccessible()));
        }
        if (node instanceof GridProvider) { 
            element.addPatternProviders(
                new WinAccessibleGridProvider(node, element.getNativeAccessible()));
        }
        if (node instanceof ToggleProvider) { 
            element.addPatternProviders(
                new WinAccessibleToggleProvider(node, element.getNativeAccessible()));
        }
        if (node instanceof SelectionProvider) { 
            element.addPatternProviders(
                new WinAccessibleSelectionProvider(node, element.getNativeAccessible()));
        }
        if (node instanceof SelectionItemProvider) { 
            element.addPatternProviders(
                new WinAccessibleSelectionItemProvider(node, element.getNativeAccessible()));
        }
        if (node instanceof RangeValueProvider) { 
            element.addPatternProviders(
                new WinAccessibleRangeValueProvider(node, element.getNativeAccessible()));
        }
        return element;
    }

    @Override public MenuBarDelegate createMenuBarDelegate(MenuBar menubar) {
        return new WinMenuBarDelegate(menubar);
    }

    @Override public MenuDelegate createMenuDelegate(Menu menu) {
        return new WinMenuDelegate(menu);
    }

    @Override public MenuItemDelegate createMenuItemDelegate(MenuItem item) {
        return new WinMenuItemDelegate(item);
    }

    @Override public ClipboardDelegate createClipboardDelegate() {
        return new WinClipboardDelegate();
    }
}
