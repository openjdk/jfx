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

import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The home screen contains the grid of app icons. When an app icon is clicked, the
 * associated application is launched.
 */
class HomeScreen extends Region {
    private final Text dukePadText;
    private final Text dateTimeText;
    private final ImageView javaLogo;
    private final Region backgroundChangeHelper;
    private final AppContainer appContainer;
    private final Map<DukeApplication, Button> appToIconMapping = new HashMap<>();
    private final BooleanProperty showAppNames = new SimpleBooleanProperty(this, "showAppNames", false);
    private FadeTransition backgroundTransition;

    HomeScreen(AppContainer appContainer) {
        this.appContainer = appContainer;
        setId("appContainer");
        // When changing the background image, we use the backgroundChangeHelper. When
        // the image is to change, set the background to be the new image. Then set the backgroundChangeHelper
        // to hold the old image. Then fade the helper out, and when it reaches 0, set visible to false.
        backgroundChangeHelper = new Region();
        backgroundChangeHelper.setVisible(false);
        backgroundChangeHelper.setId("backgroundChangeHelper");
        // Setup the background
        final String randomImageName = String.format("/images/%02d.jpg",(int)(1+(Math.random()*4)));
        setBackground(new Background(new BackgroundImage(
                new Image(HomeScreen.class.getResourceAsStream(randomImageName)),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                new BackgroundPosition(Side.LEFT, .5, true, Side.TOP, .5, true),
                new BackgroundSize(1, 1, true, true, false, true)
        )));

        // Listen for a click on desktop when running an app and exit the app if that happens
        setOnMouseClicked(event -> {
            DukeApplication app = appContainer.getApplication();
            if (app != null && !event.isConsumed()) {
                appContainer.setApplication(null);
            }
        });

        dukePadText = new Text("Duke Pad");
        dukePadText.setFill(Color.WHITE);
        dukePadText.setFont(Fonts.dosisSemiBold(35));

        dateTimeText = new Text("Dude");
        dateTimeText.setFill(Color.WHITE);
        dateTimeText.setFont(Fonts.dosisExtraLight(50));
        dateTimeText.textProperty().bind(DateTimeHelper.LONG_DATE_TIME);

        javaLogo = new ImageView(new Image(HomeScreen.class.getResourceAsStream("/images/java-logo.png")));

        getChildren().addAll(backgroundChangeHelper, dukePadText, dateTimeText, javaLogo);
    }

    void add(DukeApplication app) {
        final Node homeIcon = app.createHomeIcon();
        final Button button = new Button(app.getName(), homeIcon);
        button.getStyleClass().setAll("homeIcon");
        button.setTextFill(Color.WHITE);
        button.contentDisplayProperty().bind(
                Bindings.when(showAppNames)
                        .then(ContentDisplay.TOP)
                        .otherwise(ContentDisplay.GRAPHIC_ONLY));

        if (app instanceof LockScreen) {
            button.setOnAction(event -> {
                ((LockScreen) app).setLocked(true);
                event.consume();
            });
        } else {
            button.setOnAction(event -> {
                appContainer.setApplication(app);
                event.consume();
            });
        }
        appToIconMapping.put(app, button);
        getChildren().add(button);
    }

    void remove(DukeApplication app) {
        getChildren().remove(appToIconMapping.get(app));
    }

    final BooleanProperty showAppNamesProperty() {
        return showAppNames;
    }

    final void changeBackground(Background newBackground) {
        if (newBackground != null && !newBackground.isEmpty()) {
            Background old = getBackground();
            setBackground(newBackground);
            backgroundChangeHelper.setBackground(old);
            backgroundChangeHelper.setVisible(true);
            if (backgroundTransition != null) {
                backgroundTransition.stop();
            }
            backgroundTransition = new FadeTransition(Duration.seconds(1), backgroundChangeHelper);
            backgroundTransition.setFromValue(1);
            backgroundTransition.setToValue(0);
            backgroundTransition.setOnFinished(e -> {
                backgroundChangeHelper.setVisible(false);
                backgroundChangeHelper.setBackground(null);
                backgroundTransition = null;
            });
            backgroundTransition.play();
        }
    }

    @Override protected void layoutChildren() {
        final double w = getWidth();
        final double h = getHeight();
        backgroundChangeHelper.resize(w, h);
        dukePadText.setLayoutX(70);
        dukePadText.setLayoutY(80);
        javaLogo.setLayoutX(70);
        javaLogo.setLayoutY(h - 60 - javaLogo.getLayoutBounds().getHeight());
        dateTimeText.setLayoutX(w-70-dateTimeText.getLayoutBounds().getWidth());
        dateTimeText.setLayoutY(h - 60);

        // First get the max width and max height
        double maxWidth = 0;
        double maxHeight = 0;
        final List<Node> children = getChildren();
        for (int i = 4; i < children.size(); i++) {
            final Node homeIcon = children.get(i);
            final double iconWidth = homeIcon.prefWidth(-1);
            final double iconHeight = homeIcon.prefHeight(-1);
            maxWidth = iconWidth > maxWidth ? iconWidth : maxWidth;
            maxHeight = iconHeight > maxHeight ? iconHeight : maxHeight;
        }

        // Iterate over all the apps and lay them out in a grid 5 wide with 10 pixel gap between
        final int gap = 30;
        final int tileWidth = (int) maxWidth;
        final int tileHeight = (int) maxHeight;
        final int rowIncrement = tileHeight + gap;
        final int colIncrement = tileWidth + gap;
        final int maxX = 70 + (tileWidth * 5 + gap * 5);
        int x = 70;
        int y = 130;
        for (int i = 4; i < children.size(); i++) {
            if (x >= maxX) {
                y += rowIncrement;
                x = 70;
            }
            final Node homeIcon = children.get(i);
            homeIcon.setLayoutX(x);
            homeIcon.setLayoutY(y);
            homeIcon.resize(tileWidth, tileHeight);
            x += colIncrement;
        }
    }
}
