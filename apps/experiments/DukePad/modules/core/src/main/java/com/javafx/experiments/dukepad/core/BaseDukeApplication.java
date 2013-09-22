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

import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Base class for Duke Pad Applications
 */
public abstract class BaseDukeApplication extends DukeApplication {
    private static Image UNDER_CONSTRUCTION = loadImage("/images/under-construction.png");
    private Node ui;

    protected abstract Node createUI();

    protected static Image loadImage(String path) {
        return new Image(BaseDukeApplication.class.getResource(path).toExternalForm());
    }

    public void startApp() {
        super.startApp();
        Node ui = createUI();
        if (ui == null) {
            // show under construction
            if (UNDER_CONSTRUCTION == null) {
                UNDER_CONSTRUCTION = loadImage("/images/under-construction.png"); // weird
            }
            Region region = new Region();
            region.setBackground(new Background(
                    new BackgroundFill[]{new BackgroundFill(Color.web("#f4f4f4"), CornerRadii.EMPTY, Insets.EMPTY)},
                    new BackgroundImage[]{new BackgroundImage(UNDER_CONSTRUCTION,
                            BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                            new BackgroundPosition(Side.LEFT,.5,true,Side.TOP,.5,true), BackgroundSize.DEFAULT)}));
            ui = region;
        }
        this.ui = ui;
    }

    public boolean isRunning() {
        return ui != null;
    }

    public void stopApp() {
        ui = null;
    }

    public Node getUI() {
        return ui;
    }

    /**
     * Does this application support half screen mode
     *
     * @return True if half screen mode is supported
     */
    public boolean supportsHalfScreenMode() {
        return false;
    }
//
//    public void test() {
//        // get node "config/plugin.id"
//        // note: "config" identifies the configuration scope used here
//        final Preferences preferences = ConfigurationScope.INSTANCE.getNode("plugin.id");
//
//        // set key "a" on node "config/plugin.id"
//        preferences.put("a", "value");
//
//        // get node "config/plugin.id/node1"
//        final Preferences connections = preferences.node("node1");
//
//        // remove all keys from node "config/plugin.id/node1"
//        // note: this really on removed keys on the selected node
//        connections.clear();
//
//        // these calls are bogous and not necessary
//        // they get the same nodes as above
//        //preferences = ConfigurationScope.INSTANCE.getNode("plugin.id");
//        //connections = preferences.node("node1");
//
//        // store some values to separate child nodes of "config/plugin.id/node1"
//        for (Entry<String, ConnectionDetails> e : valuesToSave.entrySet()) {
//            String name = e.getKey();
//            ConnectionDetails d = e.getValue();
//            // get node "config/plugin.id/node1/<name>"
//            Preferences connection = connections.node(name);
//            // set keys "b" and "c"
//            connection.put("b", d.getServer());
//            connection.put("c", d.getPassword());
//        }
//
//        // flush changes to disk (if not already happend)
//        // note: this is required to make sure modifications are persisted
//        // flush always needs to be called after making modifications
//        preferences.flush();
//    }
}
