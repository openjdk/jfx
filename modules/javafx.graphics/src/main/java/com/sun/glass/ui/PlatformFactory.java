/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;

import com.sun.glass.ui.delegate.ClipboardDelegate;
import com.sun.glass.ui.delegate.MenuBarDelegate;
import com.sun.glass.ui.delegate.MenuDelegate;
import com.sun.glass.ui.delegate.MenuItemDelegate;

public abstract class PlatformFactory {
    private static PlatformFactory instance;
    public static synchronized PlatformFactory getPlatformFactory() {
        if (instance == null) {
            try {
                String platform = Platform.determinePlatform();
                String factory = "com.sun.glass.ui." +  platform.toLowerCase(Locale.ROOT) + "."+ platform + "PlatformFactory";
                // System.out.println("Loading Glass Factory " + factory);
                Class c = Class.forName(factory);
                instance = (PlatformFactory) c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to load Glass factory class");
            }
        }
        return instance;
    }

    public abstract Application createApplication();
    public abstract MenuBarDelegate createMenuBarDelegate(MenuBar menubar);
    public abstract MenuDelegate createMenuDelegate(Menu menu);
    public abstract MenuItemDelegate createMenuItemDelegate(MenuItem menuItem);
    public abstract ClipboardDelegate createClipboardDelegate();
}
