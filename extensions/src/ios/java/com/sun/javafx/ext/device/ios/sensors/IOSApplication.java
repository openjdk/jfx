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
package com.sun.javafx.ext.device.ios.sensors;

import com.sun.glass.ui.ios.IosApplication;


/**
 * The class IOSApplication provides the API for managing the IOS-specific aspects
 * of the application appearance, such as status bar appearance and orientation.
 */
public class IOSApplication {

    private static IOSApplication INSTANCE = null;

    public static IOSApplication getSharedApplication() {
        synchronized (IOSApplication.class) {
            if (INSTANCE == null) INSTANCE = new IOSApplication();
            return INSTANCE;
        }
    }


    /**
     * Returns the visibility state of the status bar
     * @return <code>true</code> if the status bar is visible,
     *         <code>false</code> otherwise.
     */
    public boolean isStatusBarVisible() {
        return !IosApplication._getStatusBarHidden();
    }


    /**
     * Shows the status bar
     */
    public void showStatusBar() {
        IosApplication._setStatusBarHidden(false);
    }


    /**
     * Shows the status bar.
     * If the status bar was hidden, the transition can optionally be animated.
     * @param animation One of the constants in {@linkplain StatusBarAnimation}.
     */
    public void showStatusBarWithAnimation(final StatusBarAnimation animation) {
        IosApplication._setStatusBarHiddenWithAnimation(false, animation.ordinal());
    }


    /**
     * Hides the status bar.
     */
    public void hideStatusBar() {
        IosApplication._setStatusBarHidden(true);
    }


    /**
     * Hides the status bar.
     * If the status bar was visible, the transition can optionally be animated.
     * @param animation One of the constants in {@linkplain StatusBarAnimation}.
     */
    public void hideStatusBarWithAnimation(final StatusBarAnimation animation) {
        IosApplication._setStatusBarHiddenWithAnimation(true, animation.ordinal());
    }


    /**
     * Gets the status bar style.
     * @return one of the constants defined in {@linkplain StatusBarStyle}.
     */
    public StatusBarStyle getStatusBarStyle() {
        return StatusBarStyle.values()[IosApplication._getStatusBarStyle()];
    }

    /**
     * Sets the style of the application status bar,
     * with the possibility to animate the change of the style.
     * @param style         One of the styles defined in {@linkplain StatusBarStyle}.
     * @param animated      <code>true</code> if the style change should be animated,
     *                      <code>false</code> if the style change should be immediate.
     */
    public void setStatusBarStyleAnimated(final StatusBarStyle style, final boolean animated) {
        IosApplication._setStatusBarStyleAnimated(style.ordinal(), animated);
    }


    /**
     * Gets the status bar orientation.
     * @return one of the constants defined in {@linkplain StatusBarOrientation}.
     */
    public StatusBarOrientation getStatusBarOrientation() {
        return StatusBarOrientation.values()[IosApplication._getStatusBarOrientation() - 1];
    }


    /**
     * Sets the orientation of the application's status bar,
     * with the possibility to animate the transition to the new orientation.
     *
     * @param orientation   the status bar {@linkplain StatusBarOrientation orientation}.
     *                      The default orientation is {@linkplain javafx.ext.ios.StatusBarOrientation#PORTRAIT}.
     * @param animated      <code>true</code> if the orientation change should be animated,
     *                      <code>false</code> if the orientation change should be immediate.
     */
    public void setStatusBarOrientationAnimated(final StatusBarOrientation orientation, final boolean animated) {
        IosApplication._setStatusBarOrientationAnimated(orientation.value(), animated);
    }
}
