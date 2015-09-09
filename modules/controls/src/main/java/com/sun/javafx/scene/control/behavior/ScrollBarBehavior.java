/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.util.Utils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.util.Duration;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;

/**
 * A Behavior implementation for ScrollBars.
 *
 */

public class ScrollBarBehavior extends BehaviorBase<ScrollBar> {

    private final InputMap<ScrollBar> inputMap;

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ScrollBarBehavior(ScrollBar scrollBar) {
        super(scrollBar);

        // create a map for scrollbar-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        inputMap = createInputMap();

        // scrollbar-specific mappings for key and mouse input
        addDefaultMapping(inputMap,
            new InputMap.KeyMapping(HOME, KEY_RELEASED, e -> home()),
            new InputMap.KeyMapping(END, KEY_RELEASED, e -> end())
        );

        // create two child input maps for horizontal and vertical scrollbars
        InputMap<ScrollBar> horizontalInputMap = new InputMap<>(scrollBar);
        horizontalInputMap.setInterceptor(e -> scrollBar.getOrientation() != Orientation.HORIZONTAL);
        horizontalInputMap.getMappings().addAll(
            new InputMap.KeyMapping(LEFT, e -> rtl(scrollBar, this::incrementValue, this::decrementValue)),
            new InputMap.KeyMapping(KP_LEFT, e -> rtl(scrollBar, this::incrementValue, this::decrementValue)),
            new InputMap.KeyMapping(RIGHT, e -> rtl(scrollBar, this::decrementValue, this::incrementValue)),
            new InputMap.KeyMapping(KP_RIGHT, e -> rtl(scrollBar, this::decrementValue, this::incrementValue))
        );
        addDefaultChildMap(inputMap, horizontalInputMap);

        InputMap<ScrollBar> verticalInputMap = new InputMap<>(scrollBar);
        verticalInputMap.setInterceptor(e -> scrollBar.getOrientation() != Orientation.VERTICAL);
        verticalInputMap.getMappings().addAll(
                new InputMap.KeyMapping(UP, e -> decrementValue()),
                new InputMap.KeyMapping(KP_UP, e -> decrementValue()),
                new InputMap.KeyMapping(DOWN, e -> incrementValue()),
                new InputMap.KeyMapping(KP_DOWN, e -> incrementValue())
        );
        addDefaultChildMap(inputMap, verticalInputMap);
    }

    /***************************************************************************
     *                                                                         *
     * Functions                                                               *
     *                                                                         *
     **************************************************************************/


    @Override public InputMap<ScrollBar> getInputMap() {
        return inputMap;
    }
    private void home() {
        getNode().setValue(getNode().getMin());
    }

    private void decrementValue() {
        getNode().adjustValue(0);
    }

    private void end() {
        getNode().setValue(getNode().getMax());
    }

    private void incrementValue() {
        getNode().adjustValue(1);
    }


    /***************************************************************************
     *                                                                         *
     * Mouse event handling                                                    *
     *                                                                         *
     **************************************************************************/

    /**
     * This timeline is used to adjust the value of the bar when the
     * track has been pressed but not released.
     */
    Timeline timeline;

    /**
     * Invoked by the ScrollBar {@link Skin} implementation whenever a mouse
     * press occurs on the "track" of the bar. This will cause the thumb to
     * be moved by some amount.
     *
     * @param position The mouse position on track with 0.0 being beginning of track and 1.0 being the end
     */
    public void trackPress(double position) {

        /* We can get a press if someone presses an end button.  In that
         * case, we don't want to start a timeline because the end button
         * will have already done so.  We can detect that because the timeline
         * will not be null.
         */
        if (timeline != null) return;

        // determine the percentage of the way between min and max
        // represented by this mouse event
        final ScrollBar bar = getNode();
        if (!bar.isFocused() && bar.isFocusTraversable()) bar.requestFocus();
        final double pos = position;
        final boolean incrementing = (pos > ((bar.getValue() - bar.getMin())/(bar.getMax() - bar.getMin())));
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);

        final EventHandler<ActionEvent> step =
                event -> {
                    boolean i = (pos > ((bar.getValue() - bar.getMin())/(bar.getMax() - bar.getMin())));
                    if (incrementing == i) {
                        // we started incrementing and still are, or we
                        // started decrementing and still are
                        bar.adjustValue(pos);
                    }
                    else {
                        stopTimeline();
                    }
                };

        final KeyFrame kf = new KeyFrame(Duration.millis(200), step);
        timeline.getKeyFrames().add(kf);
        // do the first step immediately
        timeline.play();
        step.handle(null);
    }

    /**
     */
    public void trackRelease() {
        stopTimeline();
    }

    /**
     * Invoked by the ScrollBar {@link Skin} implementation whenever a mouse
     * press occurs on the decrement button of the bar.
     */
    public void decButtonPressed() {
        final ScrollBar bar = getNode();
        if (!bar.isFocused() && bar.isFocusTraversable()) bar.requestFocus();
        stopTimeline();
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);

        final EventHandler<ActionEvent> dec =
                event -> {
                    if (bar.getValue() > bar.getMin()) {
                        bar.decrement();
                    }
                    else {
                        stopTimeline();
                    }
                };

        final KeyFrame kf = new KeyFrame(Duration.millis(200), dec);
        timeline.getKeyFrames().add(kf);
        // do the first step immediately
        timeline.play();
        dec.handle(null);
    }

    /**
     */
    public void decButtonReleased() {
        stopTimeline();
    }

    /**
     * Invoked by the ScrollBar {@link Skin} implementation whenever a mouse
     * press occurs on the increment button of the bar.
     */
    public void incButtonPressed() {
        final ScrollBar bar = getNode();
        if (!bar.isFocused() && bar.isFocusTraversable()) bar.requestFocus();
        stopTimeline();
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);

        final EventHandler<ActionEvent> inc =
                event -> {
                    if (bar.getValue() < bar.getMax()) {
                        bar.increment();
                    }
                    else {
                        stopTimeline();
                    }
                };

        final KeyFrame kf = new KeyFrame(Duration.millis(200), inc);
        timeline.getKeyFrames().add(kf);
        // do the first step immediately
        timeline.play();
        inc.handle(null);
    }

    /**
     */
    public void incButtonReleased() {
        stopTimeline();
    }

    /**
     * @param position The mouse position on track with 0.0 being begining of track and 1.0 being the end
     */
    //public function thumbPressed(e:MouseEvent, position:Number):Void {
    //}

    /**
     * @param position The mouse position on track with 0.0 being begining of track and 1.0 being the end
     */
    public void thumbDragged(double position) {
        final ScrollBar scrollbar = getNode();

        // Stop the timeline for continuous increments as drags take precedence
        stopTimeline();

        if (!scrollbar.isFocused() && scrollbar.isFocusTraversable()) scrollbar.requestFocus();
        double newValue = (position * (scrollbar.getMax() - scrollbar.getMin())) + scrollbar.getMin();
        if (!Double.isNaN(newValue)) {
            scrollbar.setValue(Utils.clamp(scrollbar.getMin(), newValue, scrollbar.getMax()));
        }
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }
}
