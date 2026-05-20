/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * This Pane shows the parent Stage information.
 */
public class StageInfoPane extends BorderPane {

    private final Label text;

    public StageInfoPane() {
        text = new Label();
        text.setWrapText(true);
        setCenter(text);

        sceneProperty().
            flatMap(Scene::windowProperty).
            map(w -> w instanceof Stage stage ? stage : null).
            subscribe(this::handleStageChanged);
    }

    private void handleStageChanged(Stage s) {
        if (s == null) {
            text.textProperty().unbind();
        } else {
            text.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    return
                        "location: " + s.getX() + ", " + s.getY() + "\n" +
                        "size: " + s.getWidth() + " x " + s.getHeight() + "\n" +
                        getScale(s);
                },
                s.xProperty(),
                s.yProperty(),
                s.widthProperty(),
                s.heightProperty()));
        }
    }

    private String getScale(Stage s) {
        if (s.getRenderScaleX() == s.getRenderScaleY()) {
            return "scale: " + s.getRenderScaleX();
        } else {
            return "scaleX: " + s.getRenderScaleX() + " scaleY: " + s.getRenderScaleY();
        }
    }
}
