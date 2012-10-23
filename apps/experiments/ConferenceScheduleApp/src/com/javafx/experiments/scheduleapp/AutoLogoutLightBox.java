/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.javafx.experiments.scheduleapp;

import com.javafx.experiments.scheduleapp.control.ResizableWrappingText;
import com.javafx.experiments.scheduleapp.data.DataService;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class AutoLogoutLightBox extends Region {
    private Box box;
    private Animation fadeAnimation = null;
    private final DataService dataService;

    public AutoLogoutLightBox(DataService dataService) {
        this.dataService = dataService;
        getStyleClass().setAll("light-box-veil");
        box = new Box();
        getChildren().add(box);
    }

    public void setSecondsLeft(int secondsLeft) {
        box.setSecondsLeft(secondsLeft);
    }

    public void show() {
        if (fadeAnimation != null || !isVisible()) {
            if (fadeAnimation != null) {
                fadeAnimation.stop();
                setVisible(true); // just to make sure
            } else {
                setOpacity(0);
                setVisible(true);
            }

            FadeTransition tx = new FadeTransition(Duration.seconds(.7), this);
            tx.setToValue(1.0);
            tx.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    fadeAnimation = null;
                }
            });
            fadeAnimation = tx;
            tx.play();
        }
    }

    public void hide() {
        if (fadeAnimation != null || isVisible()) {
            if (fadeAnimation != null) {
                fadeAnimation.stop();
                setVisible(true); // just to make sure
            } else {
                setOpacity(1);
                setVisible(true);
            }

            FadeTransition tx = new FadeTransition(Duration.seconds(.7), this);
            tx.setToValue(0.0);
            tx.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    fadeAnimation = null;
                    setVisible(false);
                }
            });
            fadeAnimation = tx;
            tx.play();
        }
    }

    @Override protected double computePrefWidth(double height) {
        final Insets insets = getInsets();
        return insets.getLeft() + box.prefWidth(-1) + insets.getRight();
    }

    @Override protected double computePrefHeight(double width) {
        final Insets insets = getInsets();
        return insets.getTop() + box.prefHeight(box.prefWidth(-1)) + insets.getBottom();
    }

    @Override protected void layoutChildren() {
        final Insets insets = getInsets();
        double width = getWidth() - insets.getLeft() - insets.getRight();
        double height = getHeight() - insets.getTop() - insets.getBottom();

        double boxWidth = box.prefWidth(-1);
        double boxHeight = box.prefHeight(boxWidth);
        box.resizeRelocate((int)((width - boxWidth) / 2), (int)((height - boxHeight) / 2), boxWidth, boxHeight);
    }

    private class Box extends Region {
        private Text message;

        public Box() {
            getStyleClass().setAll("light-box");
            message = new ResizableWrappingText();
            message.getStyleClass().setAll("auto-logout-text");
            message.setTextAlignment(TextAlignment.CENTER);
            setSecondsLeft(15);
            getChildren().add(message);
        }

        @Override protected double computePrefWidth(double height) {
            final Insets insets = getInsets();
            return insets.getLeft() + 400 + insets.getRight();
        }

        @Override protected double computePrefHeight(double width) {
            final Insets insets = getInsets();
            return insets.getTop() + message.prefHeight(400) + insets.getBottom();
        }

        @Override protected void layoutChildren() {
            final Insets insets = getInsets();
            final double top = insets.getTop();
            final double left = insets.getLeft();
            final double width = getWidth() - left - insets.getRight();
            final double height = getHeight() - top - insets.getBottom();

            message.setWrappingWidth(width);
            message.resizeRelocate((int) (left + .5), (int) (top + .5), (int) (width + .5), (int) (height + .5));
        }

        private void setSecondsLeft(int secondsLeft) {
            message.setText(
                    "Are you still there? The "+dataService.getName()+" Schedule Builder will auto-log you out in "
                            + secondsLeft + " seconds. Touch anywhere to continue.");
        }
    }
}
