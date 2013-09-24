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

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;

/**
 * Hosts the application, and is an implementation of an AppContainer.
 */
class AppPane extends Region implements AppContainer {
    private final ReadOnlyDoubleProperty totalWidth;
    private final ReadOnlyObjectWrapper<DukeApplication> currentApp = new ReadOnlyObjectWrapper<>(this, "currentApp");
    private Node ui;
    private final Region splitter;
    private final Image splitterImage;
    private DoubleProperty splitterX = new SimpleDoubleProperty(this, "splitterX");
    // When you click within the splitter, we remember how far off the origin
    // you click so that we can resize the splitterX appropriately
    private double mouseClickOffsetX;
    private boolean dragged = false;
    private boolean tabPressed = false;
    // The animation for moving the splitter when apps are shown, hidden, or resized.
    private SplitterAnimation splitterAnimation;

    public AppPane(ReadOnlyDoubleProperty totalWidth, ReadOnlyDoubleProperty totalHeight) {
        this.totalWidth = totalWidth;
        // The Root is not going to lay this guy out, we manage our own size and location,
        // thank you very much.
        setManaged(false);
        // Keep our height in sync with the root pane (in reality this shouldn't ever change, until
        // we support rotating the tablet)
        totalHeight.addListener((r, old, value) -> resize(getWidth(), totalHeight.get()));
        // The X position is the splitterX, but kept within bounds
        DoubleBinding xPosition = new DoubleBinding() {
            { bind(totalWidth, splitterX); }
            @Override protected double computeValue() {
                // We need to make sure the xPosition is always somewhere valid. If we are, say,
                // animating the splitterX at the same time the root dimensions change because
                // the tablet is being rotated then we'd run into problems.
                final double rootWidth = totalWidth.get();
                final double x = splitterX.get();
                // Clamp x between 0 and rootWidth
                return x < 0 ? 0 : x > rootWidth ? rootWidth : x;
            }
        };
        translateXProperty().bind(xPosition);
        // Be sure to also update the width whenever the xPosition changes
        xPosition.addListener((o, old, value) -> resize(totalWidth.get() - xPosition.get(), getHeight()));
        // We start off invisible, and only become visible when we have some kind of app added
        setVisible(false);
        // Setup the splitter, which will always be on the left-hand side.
        splitterImage = new Image(AppPane.class.getResourceAsStream("/images/home-bar.png"));
        splitter = new Region();
        splitter.setBackground(new Background(new BackgroundImage(
                splitterImage,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT
        )));

        splitter.setOnMousePressed(this::onSplitterPressed);
        splitter.setOnMouseDragged(this::onSplitterDragged);
        splitter.setOnMouseReleased(this::onSplitterReleased);
        getChildren().add(splitter);
    }

    @Override
    public void setApplication(DukeApplication app) {
        final DukeApplication current = currentApp.get();
        if (app == current) return;

        if (splitterAnimation != null) {
            splitterAnimation.stop();
        }

        final boolean hidingOld = current != null;
        final boolean showingNew = app != null;
        if (hidingOld && !showingNew) {
            splitterAnimation = new SplitterAnimation(totalWidth.get(), () -> {
                hideApp(current);
                splitterAnimation = null;
            });
        } else if (hidingOld) {
            hideApp(current);
            showApp(app);
            final double targetWidth = app.isFullScreen() || !app.supportsHalfScreenMode() ? 0 : totalWidth.get() / 2;
            splitterAnimation = new SplitterAnimation(targetWidth, () -> {
                splitterAnimation = null;
            });
        } else {
            splitterX.set(totalWidth.get());
            showApp(app);
            final double targetWidth = app.isFullScreen() || !app.supportsHalfScreenMode() ? 0 : totalWidth.get() / 2;
            splitterAnimation = new SplitterAnimation(targetWidth, () -> {
                splitterAnimation = null;
            });
        }
    }

    @Override
    public DukeApplication getApplication() {
        return currentApp.get();
    }

    @Override
    public ReadOnlyObjectProperty<DukeApplication> applicationProperty() {
        return currentApp;
    }

    @Override
    public void remove(DukeApplication app) {
        if (app == currentApp.get()) {
            if (splitterAnimation != null) {
                splitterAnimation.stop();
                hideApp(app);
            }
        }
    }

    @Override
    protected void layoutChildren() {
        splitter.resize(splitterImage.getWidth(), getHeight());

        final double width = totalWidth.get();
        final double halfWidth = width / 2;
        final List<Node> children = getChildren();
        if (children.size() >= 2) {
            final Node child = children.get(0);
            final DukeApplication app = getApplication();
            final double minWidth = app.supportsHalfScreenMode() ? halfWidth : width;
            child.setLayoutX(0);
            child.setLayoutY(0);
            child.resize(Math.max(getWidth(), minWidth), getHeight());
        }
    }

    /**
     * When the splitter is pressed, we determine whether the press occurred over the tab or not.
     * If it occurred over the tab, then on a release, we may decide that we should close the app and
     * go to the home screen. If some other part of the splitter were dragged, then we will use the
     * drag logic section of the released event handler instead.
     *
     * @param event the MouseEvent
     */
    private void onSplitterPressed(MouseEvent event) {
        final double mouseY = event.getY();
        final double centerY = getHeight() / 2;
        final double delta = Math.abs(centerY - mouseY);
        tabPressed = delta < 30;
        dragged = false;
        mouseClickOffsetX = event.getX();
    }

    /**
     * Updates the splitterX with the new location of the splitter based on the location of this
     * dragged event.
     *
     * @param event the MouseEvent
     */
    private void onSplitterDragged(MouseEvent event) {
        dragged = true;
        splitterX.set(event.getSceneX() - mouseClickOffsetX);
    }

    /**
     * Resizes the app, or closes it, depending on the splitter mouse gesture.
     *
     * @param event the MouseEvent
     */
    private void onSplitterReleased(MouseEvent event) {
        if (tabPressed && !dragged) {
            setApplication(null);
            return;
        }

        final int width = (int)totalWidth.get();
        final int halfWidth = width / 2;
        final double x = event.getSceneX();
        if (x > halfWidth) {
            // We're more than half way closed, so close the app
            setApplication(null);
        } else if (x < halfWidth) {
            // We're more than half way opened. What we do here depends on whether
            // we support half-screen mode for this app
            final DukeApplication app = getApplication();
            if (app.supportsHalfScreenMode()) {
                // Toggle between half-screen and full screen
                if (splitterAnimation != null) splitterAnimation.stop();
                int targetWidth = app.isFullScreen() ? halfWidth : 0;
                splitterAnimation = new SplitterAnimation(targetWidth, () -> { splitterAnimation = null; });
                app.setFullScreen(!app.isFullScreen());
            } else {
                // Close the app
                setApplication(null);
            }
        }
    }

    private void hideApp(DukeApplication oldApp) {
        oldApp.stopApp();
        getChildren().remove(ui);
        setVisible(false);
        currentApp.set(null);
        ui = null;
    }

    private void showApp(DukeApplication app) {
        app.startApp();
        ui = app.getUI();
        getChildren().add(0, ui);
        setVisible(true);
        currentApp.set(app);
    }

    /**
     * I have my own class so that I can easily and correctly handle the case of having to perform
     * some action at the end of the animation, even if the animation is terminated abruptly
     */
    private final class SplitterAnimation {
        private final Runnable onFinished;
        private final Timeline animation;

        public SplitterAnimation(double end, Runnable onFinished) {
            this.onFinished = () -> {
                if (ui != null) {
                    ui.setCache(false);
                }
                onFinished.run();
            };
            animation = new Timeline(
                    new KeyFrame(Duration.millis(400), new KeyValue(splitterX, end)),
                    new KeyFrame(Duration.millis(400), action -> {
                        if (onFinished != null) onFinished.run();
                    })
            );
            animation.play();

            if (ui != null) {
                ui.setCache(true);
                ui.setCacheHint(CacheHint.SPEED);
            }
        }

        public void stop() {
            Animation.Status status = animation.getStatus();
            if (status == Animation.Status.RUNNING) {
                animation.stop();
                if (onFinished != null) onFinished.run();
            }
        }
    }
}
