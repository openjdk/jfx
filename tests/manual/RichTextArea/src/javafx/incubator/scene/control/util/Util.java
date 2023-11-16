/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.incubator.scene.control.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.stage.Screen;

/**
 * Public utility methods.
 * 
 * TODO to be moved to javafx.scene  or  javafx.scene.control
 */
public class Util {
    private Util() {
    }

    /**
     * Combines CssMetaData items in one unmodifiable list with the size equal to the number
     * of items it holds (i.e. with no unnecessary overhead).
     *
     * @param list css metadata items, usually from the parent
     * @param items additional items
     * @return unmodifiable list containing all the items
     */
    public static List<CssMetaData<? extends Styleable, ?>> initStyleables(
            List<CssMetaData<? extends Styleable, ?>> list,
            CssMetaData<? extends Styleable, ?>... items) {

        int sz = list.size() + items.length;
        ArrayList<CssMetaData<? extends Styleable, ?>> rv = new ArrayList<>(sz);
        rv.addAll(list);
        for (CssMetaData<? extends Styleable, ?> p : items) {
            rv.add(p);
        }
        return Collections.unmodifiableList(rv);
    }

    /**
     * Simple utility function which clamps the given value to be strictly
     * between the min and max values.
     *
     * @param min minimum allowed value
     * @param value value to check against min,max
     * @param max maximum allowed value
     * @return value guaranteed to be in the range of [min .. max] (inclusive)
     */
    // see com.sun.javafx.util.Utils:77
    public static int clamp(int min, int value, int max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }

    /**
     * Determines the {@link Screen} for the given screen coordinates.
     * @param x the screen x coordinate
     * @param y the screen y coordinate
     * @return the Screen instance
     */
    // com.sun.javafx.util.Utils:769
    public static Screen getScreenForPoint(double x, double y) {
        ObservableList<Screen> screens = Screen.getScreens();

        // first check whether the point is inside some screen
        for (Screen screen: screens) {
            // can't use screen.bounds.contains, because it returns true for
            // the min + width point
            Rectangle2D r = screen.getBounds();
            if (
                (x >= r.getMinX()) && 
                (x < r.getMaxX()) && 
                (y >= r.getMinY()) && 
                (y < r.getMaxY())
            ) {
                return screen;
            }
        }

        // the point is not inside any screen, find the closest screen now
        Screen selectedScreen = Screen.getPrimary();
        double minDistance = Double.MAX_VALUE;
        for (Screen screen: screens) {
            Rectangle2D r = screen.getBounds();
            double dx = getOuterDistance(r.getMinX(), r.getMaxX(), x);
            double dy = getOuterDistance(r.getMinY(), r.getMaxY(), y);
            double distance = dx * dx + dy * dy;
            if (minDistance >= distance) {
                minDistance = distance;
                selectedScreen = screen;
            }
        }

        return selectedScreen;
    }

    // com.sun.javafx.util.Utils:839
    private static double getOuterDistance(double v0, double v1, double v) {
        if (v <= v0) {
            return v0 - v;
        }
        if (v >= v1) {
            return v - v1;
        }
        return 0.0;
    }

    /**
     * Invoked when the user attempts an invalid operation,
     * such as pasting into an uneditable <code>TextInputControl</code>
     * that has focus. The default implementation beeps.
     *
     * @param originator the <code>Node</code> the error occurred in, may be <code>null</code>
     *                   indicating the error condition is not directly associated with a <code>Node</code>
     * @param error the exception thrown (can be null)
     */
    // TODO this probably should be in Platform
    public static void provideErrorFeedback(Node originator, Throwable error) {
        beep();
        if (error != null) {
            // TODO should be using logging
            error.printStackTrace();
        }
    }

    /** Emits a short audible alert, if supported by the platform. */
    public static void beep() {
        // TODO not supported in FX
    }

    /**
     * Writes an Image to a byte array in PNG format.
     *
     * @param im source image
     * @return byte array containing PNG image
     * @throws IOException if an I/O error occurs
     */
    public static byte[] writePNG(Image im) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(65536);
        // this might conflict with user-set value
        ImageIO.setUseCache(false);
        ImageIO.write(ImgUtil.fromFXImage(im, null), "PNG", out);
        return out.toByteArray();
    }
    
    /**
     * To force initialization of a class
     * @param classToInit
     */
    // com.sun.javafx.util.Utils
    public static void forceInit(final Class<?> classToInit) {
        try {
            Class.forName(classToInit.getName(), true, classToInit.getClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    // PlatformUtil
    private static final String os = System.getProperty("os.name");
    private static String javafxPlatform = System.getProperty("javafx.platform");
    private static final boolean ANDROID = "android".equals(javafxPlatform) || "Dalvik".equals(System.getProperty("java.vm.name"));
    private static final boolean MAC = os.startsWith("Mac");
    private static final boolean LINUX = os.startsWith("Linux") && !ANDROID;
    private static final boolean WINDOWS = os.startsWith("Windows");
    
    /**
     * Returns true if the operating system is a form of Mac OS.
     */
    // PlatformUtil
    public static boolean isMac(){
        return MAC;
    }

    /**
     * Returns true if the operating system is a form of Linux.
     */
    // PlatformUtil
    public static boolean isLinux(){
        return LINUX;
    }

    /**
     * Returns true if the operating system is a form of Windows.
     */
    public static boolean isWindows(){
        return WINDOWS;
    }
}
