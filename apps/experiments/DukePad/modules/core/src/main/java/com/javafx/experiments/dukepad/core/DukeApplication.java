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

package com.javafx.experiments.dukepad.core;

import javafx.scene.Node;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Interface for Duke Pad Applications
 */
public abstract class DukeApplication {
    private static final String FULL_SCREEN_KEY = "DukeApplication.FullScreen";
    private Preferences preferences;
    private boolean isFullScreen = !supportsHalfScreenMode();

    public abstract String getName();
    public abstract Node createHomeIcon();
    public abstract boolean isRunning();
    public abstract Node getUI();

    public void startApp() {
        isFullScreen = getPreferences().getBoolean(FULL_SCREEN_KEY,true);
    }

    public abstract void stopApp();

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public void setFullScreen(boolean fullScreen){
        if (fullScreen) {
            isFullScreen = true;
        } else if(supportsHalfScreenMode()) {
            isFullScreen = false;
        }
        try {
            Preferences prefs = getPreferences();
            prefs.put(FULL_SCREEN_KEY,Boolean.toString(isFullScreen));
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Does this application support half screen mode
     *
     * @return True if half screen mode is supported
     */
    public abstract boolean supportsHalfScreenMode();

    /**
     * Get preferences for this application
     *
     * @return Application preferences
     */
    protected final Preferences getPreferences() {
        if (preferences == null) preferences = ConfigurationScope.INSTANCE.getNode(getName());
        return preferences;
    }
}
