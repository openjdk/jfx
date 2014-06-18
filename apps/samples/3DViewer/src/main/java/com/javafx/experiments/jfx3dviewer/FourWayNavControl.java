/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
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
package com.javafx.experiments.jfx3dviewer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * A four way control with 4 direction arrow buttons.
 */
public class FourWayNavControl extends GridPane {

    private FourWayListener listener;
    private Side currentDirection = null;
    private Timeline eventFiringTimeline;
    private boolean hasFired = false;

    public FourWayNavControl() {
        getStyleClass().addAll("button", "four-way");
        Region upIcon = new Region();
        upIcon.getStyleClass().add("up");
        Region downIcon = new Region();
        downIcon.getStyleClass().add("down");
        Region leftIcon = new Region();
        leftIcon.getStyleClass().add("left");
        Region rightIcon = new Region();
        rightIcon.getStyleClass().add("right");
        Region centerIcon = new Region();
        centerIcon.getStyleClass().add("center");

        GridPane.setConstraints(upIcon,1,0);
        GridPane.setConstraints(leftIcon,0,1);
        GridPane.setConstraints(centerIcon,1,1);
        GridPane.setConstraints(rightIcon,2,1);
        GridPane.setConstraints(downIcon, 1, 2);

        getChildren().addAll(upIcon,downIcon,leftIcon,rightIcon,centerIcon);

        eventFiringTimeline = new Timeline(
            new KeyFrame(Duration.millis(80), event -> {
                if (listener != null && currentDirection != null) listener.navigateStep(currentDirection,0.5);
                hasFired = true;
            })
        );
        eventFiringTimeline.setDelay(Duration.millis(300));
        eventFiringTimeline.setCycleCount(Timeline.INDEFINITE);

        upIcon.setOnMousePressed(
                event -> {
                    currentDirection = Side.TOP;
                    hasFired = false;
                    eventFiringTimeline.playFromStart();
                });
        downIcon.setOnMousePressed(
                event -> {
                    currentDirection = Side.BOTTOM;
                    hasFired = false;
                    eventFiringTimeline.playFromStart();
                });
        leftIcon.setOnMousePressed(
                event -> {
                    currentDirection = Side.LEFT;
                    hasFired = false;
                    eventFiringTimeline.playFromStart();
                });
        rightIcon.setOnMousePressed(
                event -> {
                    currentDirection = Side.RIGHT;
                    hasFired = false;
                    eventFiringTimeline.playFromStart();
                });

        EventHandler<MouseEvent> stopHandler = event -> {
            if (listener != null && currentDirection != null && !hasFired) {
                listener.navigateStep(currentDirection,10);
            }
            currentDirection = null;
            eventFiringTimeline.stop();
        };
        upIcon.setOnMouseReleased(stopHandler);
        downIcon.setOnMouseReleased(stopHandler);
        rightIcon.setOnMouseReleased(stopHandler);
        leftIcon.setOnMouseReleased(stopHandler);
    }

    public FourWayListener getListener() {
        return listener;
    }

    public void setListener(FourWayListener listener) {
        this.listener = listener;
    }

    public static interface FourWayListener {
        public void navigateStep(Side direction, double amount);
    }
}
