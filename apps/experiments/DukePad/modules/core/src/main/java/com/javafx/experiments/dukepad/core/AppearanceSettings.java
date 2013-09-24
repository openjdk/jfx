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

import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.TilePane;
import java.net.URL;

/**
 * Settings related to the appearance, which can be used to impact the core
 * UI, or in this case the home screen.
 */
public class AppearanceSettings extends BaseSettings {
    private HomeScreen homeScreen;

    public AppearanceSettings(HomeScreen homeScreen) {
        super("Appearance");
        this.homeScreen = homeScreen;
    }

    @Override
    protected void buildUI() {
        final CheckBox showIconText = new CheckBox();
        showIconText.selectedProperty().bindBidirectional(homeScreen.showAppNamesProperty());
        addRow("Show Icon Text", showIconText);

        final TilePane backgrounds = new TilePane();
        backgrounds.setVgap(8);
        backgrounds.setHgap(8);
        backgrounds.setPrefWidth((105+8)*3);
        for (int index = 1; index <= 10; index++) {
            final URL thumbnail = DukeApplication.class.getResource(String.format("/images/%02dthumb.png", index));
            final URL image = DukeApplication.class.getResource(String.format("/images/%02d.jpg", index));
            if (thumbnail != null && image != null) {
                final Button button = new Button();
                final ImageView icon = new ImageView(new Image(thumbnail.toExternalForm()));
                button.setGraphic(icon);
                button.getStyleClass().clear();
                button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                button.setOnAction(action -> {
                    homeScreen.changeBackground(new Background(new BackgroundImage(
                            new Image(image.toExternalForm()),
                            BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                            new BackgroundPosition(Side.LEFT, .5, true, Side.TOP, .5, true),
                            new BackgroundSize(1, 1, true, true, false, true)
                    )));
                });
                backgrounds.getChildren().add(button);

            }
        }
        addRow("Background", backgrounds);
    }

    @Override public Type getType() {
        return Type.SYSTEM;
    }

    @Override public int getSortOrder() {
        return Integer.MIN_VALUE;
    }
}
