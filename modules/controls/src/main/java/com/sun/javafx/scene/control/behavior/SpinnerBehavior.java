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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static javafx.scene.input.KeyCode.*;

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



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public SpinnerBehavior(Spinner<T> spinner) {
        super(spinner, SPINNER_BINDINGS);
    }



    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    /**
     * Indicates that a keyboard key has been pressed which represents the
     * event (this could be space bar for example). As long as keyDown is true,
     * we are also armed, and will ignore mouse events related to arming.
     * Note this is made package private solely for the sake of testing.
     */
    private boolean keyDown;

    protected static final List<KeyBinding> SPINNER_BINDINGS = new ArrayList<KeyBinding>();
    static {
        SPINNER_BINDINGS.add(new KeyBinding(UP, "increment-up"));
        SPINNER_BINDINGS.add(new KeyBinding(RIGHT, "increment-right"));
        SPINNER_BINDINGS.add(new KeyBinding(LEFT, "decrement-left"));
        SPINNER_BINDINGS.add(new KeyBinding(DOWN, "decrement-down"));
    }

    @Override protected void callAction(String name) {
        boolean vertical = arrowsAreVertical();

        switch (name) {
            case "increment-up": {
                if (vertical) increment(1); else traverseUp(); break;
            }
            case "increment-right": {
                if (! vertical) increment(1); else traverseRight(); break;
            }
            case "decrement-down": {
                if (vertical) decrement(1); else traverseDown(); break;
            }
            case "decrement-left": {
                if (! vertical) decrement(1); else traverseLeft(); break;
            }
            default: super.callAction(name); break;
        }
    }


    /***************************************************************************
     *                                                                         *
     * API                                                                     *
     *                                                                         *
     **************************************************************************/

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



    /***************************************************************************
     *                                                                         *
     * Implementation                                                          *
     *                                                                         *
     **************************************************************************/

    private boolean arrowsAreVertical() {
        final List<String> styleClass = getControl().getStyleClass();

        return ! (styleClass.contains(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL)  ||
                  styleClass.contains(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL) ||
                  styleClass.contains(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL));
    }
}