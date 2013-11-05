/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class WindowManager {
    private WindowManager() {
    }

    public static void closeApplicationWindows(
            final ClassLoader appClassLoader) {
        final List<Window> selectedWindows = new ArrayList<Window>();
        final Iterator<Window> allWindows = Window.impl_getWindows();
        while (allWindows.hasNext()) {
            final Window window = allWindows.next();
            if (matches(window, appClassLoader)) {
                selectedWindows.add(window);
            }
        }

        for (int i = selectedWindows.size() - 1; i >= 0; --i) {
            selectedWindows.get(i).hide();
        }
    }

    private static boolean matches(final Window window,
                                   final ClassLoader appClassLoader) {
        // hack to be used until context class loader association with FX
        // windows is pushed to JavaFX codebase
        if (window instanceof Stage) {
            return !"Mobile Center".equals(((Stage) window).getTitle());
        }

        return true;

        /*
        return isEqualToOrAncestorOf(
                   appClassLoader,
                   WindowHelper.getContextClassLoader(window));
         */
    }

    /*
    private static boolean isEqualToOrAncestorOf(
            final ClassLoader fixedClassLoader,
            ClassLoader testedClassLoader) {
        if (fixedClassLoader == null) {
            return true;
        }

        while (testedClassLoader != null) {
            if (fixedClassLoader == testedClassLoader) {
                return true;
            }
            testedClassLoader = testedClassLoader.getParent();
        }

        return false;
    }
     */
}
