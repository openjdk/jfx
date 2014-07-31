/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.behavior;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.Duration;

import java.util.Collections;

public class SpinnerBehavior<T> extends BehaviorBase<Spinner<T>> {

    // this specifies how long the mouse has to be pressed on a button
    // before the value steps. As the mouse is held down longer, we begin
    // to cut down the duration of subsequent steps (and also increase the
    // step size)
    private static final double INITIAL_DURATION_MS = 750;

    private final int STEP_AMOUNT = 1;

    private boolean isIncrementing = false;

    private Timeline timeline;

    final EventHandler<ActionEvent> spinningKeyFrameEventHandler = event -> {
        final SpinnerValueFactory<T> valueFactory = getControl().getValueFactory();
        if (valueFactory == null) {
            return;
        }

        if (isIncrementing) {
            increment(STEP_AMOUNT);
        } else {
            decrement(STEP_AMOUNT);
        }
    };


    public SpinnerBehavior(Spinner<T> spinner) {
        super(spinner, Collections.emptyList());
    }

    public void increment(int steps) {
        getControl().increment(steps);
    }

    public void decrement(int steps) {
        getControl().decrement(steps);
    }

    public void startSpinning(boolean increment) {
        isIncrementing = increment;

        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        final KeyFrame kf = new KeyFrame(Duration.millis(INITIAL_DURATION_MS), spinningKeyFrameEventHandler);
        timeline.getKeyFrames().setAll(kf);
        timeline.playFromStart();
        timeline.play();
        spinningKeyFrameEventHandler.handle(null);
    }

    public void stopSpinning() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}