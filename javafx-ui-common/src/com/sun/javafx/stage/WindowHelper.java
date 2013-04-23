/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.stage.Window;

/**
 * Used to access internal window methods.
 */
public final class WindowHelper {
    private static WindowAccessor windowAccessor;

    static {
        forceInit(Window.class);
    }

    private WindowHelper() {
    }

    public static void setWindowTranslate(final Window window,
                                          final double translateX,
                                          final double translateY) {
        windowAccessor.setWindowTranslate(window, translateX, translateY);
    }

    /**
     * Returns the x coordinate of the window's upper left corner on screen. The
     * value differs from Window.x only if winTranslateX is non-zero.
     */
    public static double getScreenX(final Window window) {
        return windowAccessor.getScreenX(window);
    }

    /**
     * Returns the y coordinate of the window's upper left corner on screen. The
     * value differs from Window.y only if winTranslateY is non-zero.
     */
    public static double getScreenY(final Window window) {
        return windowAccessor.getScreenY(window);
    }

    public static void setLocation(final Window window,
                                   final double x,
                                   final double y) {
        windowAccessor.setLocation(window, x, y);
    }

    public static void setSize(final Window window,
                               final double width,
                               final double height) {
        windowAccessor.setSize(window, width, height);
    }

    public static void setWindowAccessor(final WindowAccessor newAccessor) {
        if (windowAccessor != null) {
            throw new IllegalStateException();
        }

        windowAccessor = newAccessor;
    }

    public interface WindowAccessor {
        void setWindowTranslate(Window window,
                                double translateX,
                                double translateY);

        double getScreenX(Window window);

        double getScreenY(Window window);

        void setLocation(Window window, double x, double y);

        void setSize(Window window, double width, double height);
    }

    private static void forceInit(final Class<?> classToInit) {
        try {
            Class.forName(classToInit.getName(), true,
                          classToInit.getClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new AssertionError(e);  // Can't happen
        }
    }
}
