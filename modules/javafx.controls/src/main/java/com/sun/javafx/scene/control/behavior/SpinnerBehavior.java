/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

import java.util.List;

import static javafx.scene.input.KeyCode.*;
import static com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;

public class SpinnerBehavior<T> extends BehaviorBase<Spinner<T>> {

    // this specifies how long the mouse has to be pressed on a button
    // before the value steps. As the mouse is held down longer, we begin
    // to cut down the duration of subsequent steps (and also increase the
    // step size)
    private static final double INITIAL_DURATION_MS = 750;

    private final InputMap<Spinner<T>> spinnerInputMap;

    private static final int STEP_AMOUNT = 1;

    private boolean isIncrementing = false;

    /* Package-private for testing purposes */
    Timeline timeline;

    final EventHandler<ActionEvent> spinningKeyFrameEventHandler = event -> {
        final SpinnerValueFactory<T> valueFactory = getNode().getValueFactory();
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
        super(spinner);

        // create a map for spinner-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        spinnerInputMap = createInputMap();

        // then spinner-specific mappings for key and mouse input
        addDefaultMapping(spinnerInputMap,
            new KeyMapping(UP, KeyEvent.KEY_PRESSED, e -> {
                if (arrowsAreVertical()) increment(1); else FocusTraversalInputMap.traverseUp(e);
            }),
            new KeyMapping(RIGHT, KeyEvent.KEY_PRESSED, e -> {
                if (! arrowsAreVertical()) increment(1); else FocusTraversalInputMap.traverseRight(e);
            }),
            new KeyMapping(LEFT, KeyEvent.KEY_PRESSED, e -> {
                if (! arrowsAreVertical()) decrement(1); else FocusTraversalInputMap.traverseLeft(e);
            }),
            new KeyMapping(DOWN, KeyEvent.KEY_PRESSED, e -> {
                if (arrowsAreVertical()) decrement(1); else FocusTraversalInputMap.traverseDown(e);
            })
        );
    }



    /***************************************************************************
     *                                                                         *
     * API                                                                     *
     *                                                                         *
     **************************************************************************/

    @Override public InputMap<Spinner<T>> getInputMap() {
        return spinnerInputMap;
    }

    public void increment(int steps) {
        getNode().increment(steps);
    }

    public void decrement(int steps) {
        getNode().decrement(steps);
    }

    public void startSpinning(boolean increment) {
        isIncrementing = increment;

        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setDelay(getNode().getInitialDelay());
        final KeyFrame start = new KeyFrame(Duration.ZERO, spinningKeyFrameEventHandler);
        final KeyFrame repeat = new KeyFrame(getNode().getRepeatDelay());
        timeline.getKeyFrames().setAll(start, repeat);
        timeline.playFromStart();

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

    public boolean arrowsAreVertical() {
        final List<String> styleClass = getNode().getStyleClass();

        return ! (styleClass.contains(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL)  ||
                  styleClass.contains(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL) ||
                  styleClass.contains(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL));
    }
}
