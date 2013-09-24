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
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 */
public abstract class Base3DDukeApplication extends BaseDukeApplication {
    private Stage stage;

    @Override
    protected final Node createUI() {
        // Rendering artifacts has been found while using depth buffer, that's why it was decided to start using
        // a new Stage(window) to display the content, that is why this method returns an empty Node and a new Stage is
        // being created in the overridden startApp method
        final Region node = new Region();
        node.setBackground(new Background(new BackgroundImage(
                loadImage("/images/background2.jpg"),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT
                )));
        // This method is required to move new stage smoothly and synchronously with the application node
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(createScene());
        stage.show();
        node.localToSceneTransformProperty().addListener((observableValue, transform, transform2) -> {
            stage.setX(node.localToScene(0, 0).getX() + 33); // 33 is width of splitter
        });
        return node;
    }

    protected abstract Scene createScene();

    @Override
    public void stopApp() {
        super.stopApp();
        if (stage != null) {
            stage.close();
            stage = null;
        }
    }
}
