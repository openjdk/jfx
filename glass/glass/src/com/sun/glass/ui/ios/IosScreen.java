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

package com.sun.glass.ui.ios;

import com.sun.glass.ui.Screen;
import java.util.Vector;

/**
 * iOS platform implementation class for Screen.
 * Facilitates access to display information.
 */
final class IosScreen {
    
    native private static double _getVideoRefreshPeriod();
    native private static Screen _getDeepestScreen(Screen screen);
    native private static Screen _getMainScreen(Screen screen);
    native private static Screen _getScreenForLocation(Screen screen, int x, int y);
    native private static Screen _getScreenForPtr(Screen screen, long screenPtr);
    native private static Vector<Screen> _getScreens(Vector<Screen> screens);

    private static final Screen mainScreen = new Screen();

    static double getVideoRefreshPeriod_impl() {
        return _getVideoRefreshPeriod();
    }

    static Screen getDeepestScreen_impl() {
        return _getDeepestScreen(new Screen());
    }

    static Screen getMainScreen_impl() {
        return _getMainScreen(mainScreen);
    }

    static Screen getScreenForLocation_impl(int x, int y) {
        return _getScreenForLocation(new Screen(), x, y);
    }

    static Screen getScreenForPtr_impl(long screenPtr) {
        return _getScreenForPtr(new Screen(), screenPtr);
    }

    static Vector<Screen> getScreens_impl() {
        return _getScreens(new Vector<Screen>());
    }
}

