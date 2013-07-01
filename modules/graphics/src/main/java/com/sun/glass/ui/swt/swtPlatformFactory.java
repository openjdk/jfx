/*
 * Copyright (c) 2012, 2013, Oracle  and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.*;
import com.sun.glass.ui.accessible.AccessibleBaseProvider;
import com.sun.glass.ui.accessible.AccessibleRoot;
import com.sun.glass.ui.delegate.ClipboardDelegate;
import com.sun.glass.ui.delegate.MenuBarDelegate;
import com.sun.glass.ui.delegate.MenuDelegate;
import com.sun.glass.ui.delegate.MenuItemDelegate;
import com.sun.javafx.accessible.providers.AccessibleProvider;

public final class swtPlatformFactory extends PlatformFactory {

    @Override
    public Application createApplication(){
        return new SWTApplication();
    }

    @Override
    public MenuBarDelegate createMenuBarDelegate(final MenuBar menubar) {
        return new SWTMenuBarDelegate();
    }

    @Override
    public MenuDelegate createMenuDelegate(final Menu menu) {
        SWTMenuDelegate result = new SWTMenuDelegate(menu);
        return result;
    }

    @Override
    public MenuItemDelegate createMenuItemDelegate(final MenuItem item) {
        return new SWTMenuDelegate();
    }

    public ClipboardDelegate createClipboardDelegate() {
        return new ClipboardDelegate() {
            public Clipboard createClipboard(String clipboardName) {
                if (Clipboard.SYSTEM.equals(clipboardName)) {
                    return new SWTClipboard(clipboardName);
                }
                if (Clipboard.DND.equals(clipboardName)) {
                    return new SWTClipboard(clipboardName);
                }
                return null;
            }
        };
    }

    @Override
    public AccessibleRoot createAccessibleRoot(Object node, Window window) {
        //TODO - implement accessibility
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AccessibleBaseProvider createAccessibleProvider(Object node) {
        //TODO - implement accessibility
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
