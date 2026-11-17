/*
 * Copyright (c) 2025, Gluon. All rights reserved.
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
package com.sun.glass.ui.headless;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.Menu;
import com.sun.glass.ui.MenuBar;
import com.sun.glass.ui.MenuItem;
import com.sun.glass.ui.PlatformFactory;
import com.sun.glass.ui.SystemClipboard;
import com.sun.glass.ui.delegate.ClipboardDelegate;
import com.sun.glass.ui.delegate.MenuBarDelegate;
import com.sun.glass.ui.delegate.MenuDelegate;
import com.sun.glass.ui.delegate.MenuItemDelegate;
import java.util.HashMap;

public class HeadlessPlatformFactory extends PlatformFactory {

    @Override
    public Application createApplication() {
        return new HeadlessApplication();
    }

    @Override
    public MenuBarDelegate createMenuBarDelegate(MenuBar menubar) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MenuDelegate createMenuDelegate(Menu menu) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MenuItemDelegate createMenuItemDelegate(MenuItem menuItem) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ClipboardDelegate createClipboardDelegate() {
        return (String clipboardName) -> {
            if (Clipboard.SYSTEM.equals(clipboardName)) {
                return new HeadlessSystemClipboard();
            } else if (Clipboard.DND.equals(clipboardName)) {
                return new HeadlessDnDClipboard();
            } else {
                throw new IllegalArgumentException("No support for " + clipboardName + " clipboard in headless");
            }
        };

    }

    static class HeadlessSystemClipboard extends SystemClipboard {

        HashMap<String, Object> cacheData;
        int supportedActions;

        HeadlessSystemClipboard() {
            super(Clipboard.SYSTEM);
        }

        @Override
        protected boolean isOwner() {
            return true;
        }

        @Override
        protected void pushToSystem(HashMap<String, Object> cacheData, int supportedActions) {
            this.cacheData = cacheData;
            this.supportedActions = supportedActions;
        }

        @Override
        protected void pushTargetActionToSystem(int actionDone) {
        }

        @Override
        protected Object popFromSystem(String mimeType) {
            return null;
        }

        @Override
        protected int supportedSourceActionsFromSystem() {
            return Clipboard.ACTION_NONE;
        }

        @Override
        protected String[] mimesFromSystem() {
            return new String[0];
        }
    }

    static class HeadlessDnDClipboard extends SystemClipboard {

        HeadlessDnDClipboard() {
            super(Clipboard.DND);
        }

        @Override
        protected boolean isOwner() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected void pushToSystem(HashMap<String, Object> cacheData, int supportedActions) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected void pushTargetActionToSystem(int actionDone) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected Object popFromSystem(String mimeType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected int supportedSourceActionsFromSystem() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected String[] mimesFromSystem() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
