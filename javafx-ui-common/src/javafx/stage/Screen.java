/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.stage;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;

import com.sun.javafx.tk.ScreenConfigurationAccessor;
import com.sun.javafx.tk.TKScreenConfigurationListener;
import com.sun.javafx.tk.Toolkit;

/**
 * Describes the characteristics of a graphics destination such as monitor.
 * In a virtual device multi-screen environment in which the desktop area
 * could span multiple physical screen devices, the bounds of the
 * {@code Screen} objects are relative to the {@code Screen.primary}.
 *
 * <p>
 * For example:
 * <pre><code>
 * Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
 *
 * //set Stage boundaries to visible bounds of the main screen
 * stage.setX(primaryScreenBounds.getMinX());
 * stage.setY(primaryScreenBounds.getMinY());
 * stage.setWidth(primaryScreenBounds.getWidth());
 * stage.setHeight(primaryScreenBounds.getHeight());
 *
 * stage.show();
 * </code></pre>
 * </p>
 */
public class Screen {

    private static AtomicBoolean configurationDirty = new AtomicBoolean(true);

    private static ScreenConfigurationAccessor accessor;

    private static Screen primary;
    private static ObservableList<Screen> screens = FXCollections.<Screen>observableArrayList();

    static {
        accessor = Toolkit.getToolkit().setScreenConfigurationListener(new TKScreenConfigurationListener() {
            @Override public void screenConfigurationChanged() {
                updateConfiguration();
            }
        });
    }

    private static void checkDirty() {
        if (configurationDirty.compareAndSet(true, false)) {
            updateConfiguration();
        }
    }

    private static void updateConfiguration() {
        Object primaryScreen = Toolkit.getToolkit().getPrimaryScreen();
        Screen screenTmp = nativeToScreen(primaryScreen, Screen.primary);
        if (screenTmp != null) {
            Screen.primary = screenTmp;
        }

        List<?> screens = Toolkit.getToolkit().getScreens();
        // go through the list of new screens, see if they match the
        // existing list; if they do reuse the list; if they don't
        // at least try to reuse some of the old ones
        ObservableList<Screen> newScreens = FXCollections.<Screen>observableArrayList();
        // if the size of the new and the old one are different just
        // recreate the list
        boolean canKeepOld = (Screen.screens.size() == screens.size());
        for (int i = 0; i < screens.size(); i++) {
            Object obj = screens.get(i);
            Screen origScreen = null;
            if (canKeepOld) {
                origScreen = Screen.screens.get(i);
            }
            Screen newScreen = nativeToScreen(obj, origScreen);
            if (newScreen != null) {
                if (canKeepOld) {
                    canKeepOld = false;
                    newScreens.clear();
                    newScreens.addAll(Screen.screens.subList(0, i));
                }
                newScreens.add(newScreen);
            }
        }
        if (!canKeepOld) {
            Screen.screens.clear();
            Screen.screens.addAll(newScreens);
        }

        configurationDirty.set(false);
    }

    // returns null if the new one is to be equal the old one
    private static Screen nativeToScreen(Object obj, Screen screen) {
        int minX = accessor.getMinX(obj);
        int minY = accessor.getMinY(obj);
        int width = accessor.getWidth(obj);
        int height = accessor.getHeight(obj);
        int visualMinX = accessor.getVisualMinX(obj);
        int visualMinY = accessor.getVisualMinY(obj);
        int visualWidth = accessor.getVisualWidth(obj);
        int visualHeight = accessor.getVisualHeight(obj);
        double dpi = accessor.getDPI(obj);
        if ((screen == null) ||
            (screen.bounds.getMinX() != minX) ||
            (screen.bounds.getMinY() != minY) ||
            (screen.bounds.getWidth() != width) ||
            (screen.bounds.getHeight() != height) ||
            (screen.visualBounds.getMinX() != visualMinX) ||
            (screen.visualBounds.getMinY() != visualMinY) ||
            (screen.visualBounds.getWidth() != visualWidth) ||
            (screen.visualBounds.getHeight() != visualHeight) ||
            (screen.dpi != dpi))
        {
            Screen s = new Screen();
            s.bounds = new Rectangle2D(minX, minY, width, height);
            s.visualBounds = new Rectangle2D(visualMinX, visualMinY, visualWidth, visualHeight);
            s.dpi = dpi;
            return s;
        } else {
            return null;
        }
    }

    /**
     * The primary {@code Screen}.
     */
    public static Screen getPrimary() {
        checkDirty();
        return primary;
    }

    /**
      * The observable list of currently available {@code Screens}.
      */
    public static ObservableList<Screen> getScreens() {
        checkDirty();
        return screens;
    }

    /**
      * Returns a ObservableList of {@code Screens} that intersects the provided rectangle.
      *
      * @param x the x coordinate of the upper-left corner of the specified
      *   rectangular area
      * @param y the y coordinate of the upper-left corner of the specified
      *   rectangular area
      * @param width the width of the specified rectangular area
      * @param height the height of the specified rectangular area
      * @return a ObservableList of {@code Screens} for which {@code Screen.bounds}
      *   intersects the provided rectangle
      */
    public static ObservableList<Screen> getScreensForRectangle(
            double x, double y, double width, double height)
    {
        checkDirty();
        ObservableList<Screen> results = FXCollections.<Screen>observableArrayList();
        for (Screen screen : screens) {
            if (screen.bounds.intersects(x, y, width, height)) {
                results.add(screen);
            }
        }
        return results;
    }

    /**
      * Returns a ObservableList of {@code Screens} that intersects the provided rectangle.
      *
      * @param r The specified {@code Rectangle2D}
      * @return a ObservableList of {@code Screens} for which {@code Screen.bounds}
      *   intersects the provided rectangle
      */
    public static ObservableList<Screen> getScreensForRectangle(Rectangle2D r) {
        checkDirty();
        return getScreensForRectangle(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
    }

    /**
     * The bounds of this {@code Screen}.
     */
    private Rectangle2D bounds = Rectangle2D.EMPTY;
    /**
     * Gets the bounds of this {@code Screen}.
     * @return The bounds of this {@code Screen}
     */
    public Rectangle2D getBounds() {
        return bounds;
    }

    /**
     * The visual bounds of this {@code Screen}.
     * 
     * These bounds account for objects in the native windowing system such as
     * task bars and menu bars. These bounds are contained by {@code Screen.bounds}.
     */
    private Rectangle2D visualBounds = Rectangle2D.EMPTY;
    /**
     * Gets the visual bounds of this {@code Screen}.
     * 
     * These bounds account for objects in the native windowing system such as
     * task bars and menu bars. These bounds are contained by {@code Screen.bounds}.
     * @return The visual bounds of this {@code Screen}
     */
    public final Rectangle2D getVisualBounds() {
        return visualBounds;
    }

    /**
      * The resolution (dots per inch) of this @{code Screen}.
      */
    private double dpi;
    /**
     * Gets the resolution (dots per inch) of this @{code Screen}.
     * @return The resolution of this @{code Screen}
     */
    public final double getDpi() {
        return dpi;
    }

    /**
     * Returns a hash code for this {@code Screen} object.
     * @return a hash code for this {@code Screen} object.
     */ 
    @Override public int hashCode() {
        long bits = 7L;
        bits = 37L * bits + bounds.hashCode();
        bits = 37L * bits + visualBounds.hashCode();
        bits = 37L * bits + Double.doubleToLongBits(dpi);
        return (int) (bits ^ (bits >> 32));
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is equal to the {@code obj} argument; {@code false} otherwise.
     */
    @Override public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Screen) {
            Screen other = (Screen) obj;
            return (bounds == null ? other.bounds == null : bounds.equals(other.bounds))
              && (visualBounds == null ? other.visualBounds == null : visualBounds.equals(other.visualBounds))
              && other.dpi == dpi;
        } else return false;
    }

    /**
     * Returns a string representation of this {@code Screen} object.
     * @return a string representation of this {@code Screen} object.
     */ 
    @Override public String toString() {
        return super.toString() + "bounds:" + bounds + " visualBounds:" + visualBounds + " dpi:" + dpi;
    }
}
