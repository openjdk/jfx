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
import javafx.scene.Scene;
import javafx.scene.layout.Region;

/**
 * The root pane of the DukePad, comprising 3 specific layers:
 *   - The HomeScreen, which contains all the app icons
 *   - The AppPane, which contains the current application
 *   - The LockScreen
 *
 * Each of these layers always exists, but may be visible / invisible
 * depending on the current application state. For example, if there
 * is no current application, then the app pane is invisible.
 *
 * The HomeScreen size is managed by the Root, such that the HomeScreen is
 * always sized to match the Root (which, itself, has no visuals). The
 * AppPane manages its own size and visibility, depending completely on the
 * state of the current app. The LockScreen size is managed by the Root, and is
 * always the same size as the Root.
 */
public class Root extends Region {
    private final HomeScreen homeScreen;
    private final AppPane appPane;
    private LockScreen lockScreen;
    private Scene scene;

    public Root(Scene scene) {
        this.scene = scene;
        appPane = new AppPane(widthProperty(), heightProperty());
        homeScreen = new HomeScreen(appPane);
        getChildren().addAll(homeScreen, appPane);
    }

    public void add(DukeApplication app) {
        homeScreen.add(app);
        if (lockScreen == null && app instanceof LockScreen) {
            lockScreen = (LockScreen) app;
            homeScreen.setVisible(false);
            lockScreen.startApp();
            getChildren().add(lockScreen.getUI());
            lockScreen.lockedProperty().addListener((screen, wasLocked, locked) -> {
                if (!locked) {
                    homeScreen.setVisible(true);
                    getChildren().remove(lockScreen.getUI());
                    lockScreen.stopApp();
                } else {
                    homeScreen.setVisible(false);
                    lockScreen.startApp();
                    getChildren().add(lockScreen.getUI());
                }
            });

            // don't add us to scene root till now so that we go direct from splash screen to lock screen
            scene.setRoot(this);
        }
    }

    public void remove(DukeApplication app) {
        DukeApplication currentApp = appPane.getApplication();
        if (currentApp == app) {
            appPane.remove(app);
        }
        if (app == lockScreen) {
            getChildren().remove(getChildren().size() - 1);
        }
        homeScreen.remove(app);
    }

    public final AppContainer getAppContainer() {
        return appPane;
    }

    public final HomeScreen getHomeScreen() {
        return homeScreen;
    }

    @Override
    protected void layoutChildren() {
        final double w = getWidth();
        final double h = getHeight();

        homeScreen.resize(w, h);

        final Node lockUI = lockScreen == null ? null : lockScreen.getUI();
        if (lockUI != null) {
            lockUI.resize(w, h);
        }
    }
}
