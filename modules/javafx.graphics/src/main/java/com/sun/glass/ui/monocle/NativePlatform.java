/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.javafx.util.Logging;

/** Abstract of a platform on which JavaFX can run. */
public abstract class NativePlatform {

    private static InputDeviceRegistry inputDeviceRegistry;
    private final RunnableProcessor runnableProcessor;
    private final PlatformLogger logger = Logging.getJavaFXLogger();

    private NativeCursor cursor;
    protected List<NativeScreen> screens;
    protected AcceleratedScreen accScreen;


    @SuppressWarnings("removal")
    protected static final boolean useCursor =
        AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            final String str =
                System.getProperty("monocle.cursor.enabled", "true");
            return "true".equalsIgnoreCase(str);
        });



    protected NativePlatform() {
        runnableProcessor = new RunnableProcessor();
    }

    /**
     * Called once during JavaFX shutdown to release platform resources.
     */
    void shutdown() {
        runnableProcessor.shutdown();
        if (cursor != null) {
            cursor.shutdown();
        }
        if (screens != null) {
            for (NativeScreen screen: screens) {
                screen.shutdown();
            }
        }
    }

    /**
     * @return the RunnableProcessor used to post events to the JavaFX event queue.
     */
    RunnableProcessor getRunnableProcessor() {
        return runnableProcessor;
    }

    /**
     * @return the InputDeviceRegistry that maintains a list of input devices
     * for this platform.
     */
    synchronized InputDeviceRegistry getInputDeviceRegistry() {
        if (inputDeviceRegistry == null) {
            inputDeviceRegistry = createInputDeviceRegistry();
        }
        return inputDeviceRegistry;
    }

    /**
     * Creates the InputDeviceRegistry for this platform. Called once.
     *
     * @return a new InputDeviceRegistry
     */
    protected abstract InputDeviceRegistry createInputDeviceRegistry();

    /**
     * Creates the NativeCursor for this platform. Called once.
     *
     * @return a new NativeCursor
     */
    protected abstract NativeCursor createCursor();

    /** Obtains the singleton NativeCursor
     *
     * @return the NativeCursor
     */
    synchronized NativeCursor getCursor() {
        if (cursor == null) {
            cursor = createCursor();
        }
        return cursor;
    }

    /**
     * Creates the NativeScreen for this platform. Called once.
     *
     * @return a new NativeScreen
     */
    protected abstract NativeScreen createScreen();

    /**
     * Create <code>NativeScreen</code> instances for all
     * available native screens. The default implementation of
     * this method will invoke the <code>createScreen</code>
     * method and return a list with a single screen only.
     * Subclasses can override this and return a list
     * with all screens. The approach to get this list is
     * specific to the subclass and can be done in Java or
     * in native code. <p>
     * The first element in the returned list is considered
     * to be the primary screen.
     *
     * @return a list with all native screens. This list is
     * never <code>null</code>, but it can be empty.
     */
    protected synchronized List<NativeScreen> createScreens() {
        if (screens == null) {
            screens = new ArrayList<>(1);
            screens.add(createScreen());
        }
        return screens;
    }

    /**
     * Obtains the NativeScreen instances. This method will
     * create NativeScreen instances for all native screens,
     * and stores them internally. The main screen (primaryscreen)
     * is returned.
     *
     * @return the primaryscreen, or <code>null</code> if no
     * screen is found.
     */
    synchronized NativeScreen getScreen() {
        if (screens == null) {
            screens = createScreens();
        }
        return (screens.size() == 0 ? null : screens.get(0));
    }

    synchronized List<NativeScreen> getScreens() {
        if (screens == null) {
            screens = createScreens();
        }
        return screens;
    }

    /**
     * Gets the AcceleratedScreen for this platform
     *
     * @param attributes a sequence of pairs (GLAttibute, value)
     * @return an AcceleratedScreen for rendering using OpenGL
     * @throws GLException if no OpenGL surface could be created
     * @throws UnsatisfiedLinkError if native graphics libraries could not be loaded for this platform.
     */
    public synchronized AcceleratedScreen getAcceleratedScreen(int[] attributes)
            throws GLException, UnsatisfiedLinkError {
        if (accScreen == null) {
            accScreen = new AcceleratedScreen(attributes);
        }
        return accScreen;
    }


    /**
     * Log the name of the supplied native cursor class if required.
     *
     * @param cursor the native cursor in use, null is permitted
     * @return the passed in cursor
     */
    protected NativeCursor logSelectedCursor(final NativeCursor cursor) {
        if (logger.isLoggable(Level.FINE)) {
            final String name = cursor == null ? null : cursor.getClass().getSimpleName();
            logger.fine("Using native cursor: {0}", name);
        }
        return cursor;
    }

}
