/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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


package com.sun.glass.ui.lens;

import com.sun.glass.ui.Screen;
import java.util.Vector;

final class LensScreen {

    private static Screen defaultScreen;

    protected LensScreen() {
        super();
    }

    native private static Screen _getMainScreen(Screen screen);

    static Screen getDeepestScreen_impl() {
        //No support for multiply screens, return the default
        return getMainScreen_impl();
    }

    static Screen getMainScreen_impl() {
        if (defaultScreen == null) {
            defaultScreen = _getMainScreen(new Screen());
        }

        return defaultScreen;
    }

    static Screen getScreenForLocation_impl(int x, int y) {

        //No support for multiply screens, return the default
        return getMainScreen_impl();
    }

    static Screen getScreenForPtr_impl(long screenPtr) {
        //No support for multiply screens, return the default
        return getMainScreen_impl();
    }

    static Vector<Screen> getScreens_impl() {
        //only main screen supported
        Vector<Screen> screens = new Vector<Screen>();
        screens.add(getMainScreen_impl());
        return screens;
    }

}
