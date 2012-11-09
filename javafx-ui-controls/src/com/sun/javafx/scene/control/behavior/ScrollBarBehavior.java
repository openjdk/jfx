/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.END;
import static javafx.scene.input.KeyCode.F4;
import static javafx.scene.input.KeyCode.HOME;
import static javafx.scene.input.KeyCode.KP_DOWN;
import static javafx.scene.input.KeyCode.KP_LEFT;
import static javafx.scene.input.KeyCode.KP_RIGHT;
import static javafx.scene.input.KeyCode.KP_UP;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import com.sun.javafx.Utils;

/**
 * A Behavior implementation for ScrollBars.
 *
 */

public class ScrollBarBehavior extends BehaviorBase<ScrollBar> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    public ScrollBarBehavior(ScrollBar scrollbar) {
        super(scrollbar);
    }

    /***************************************************************************
     *                                                                         *
     * Functions                                                               *
     *                                                                         *
     **************************************************************************/

    void home() {
        getControl().setValue(getControl().getMin());
    }

    void decrementValue() {
        getControl().adjustValue(0);
    }

    void end() {
        getControl().setValue(getControl().getMax());
    }

    void incrementValue() {
        getControl().adjustValue(1);
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    /* We manually specify the focus traversal keys because Slider has
     * different usage for up/down arrow keys.
     */
    protected static final List<KeyBinding> SCROLLBAR_BINDINGS = new ArrayList<KeyBinding>();
    static {
        SCROLLBAR_BINDINGS.add(new KeyBinding(TAB, "TraverseNext"));
        SCROLLBAR_BINDINGS.add(new KeyBinding(TAB, "TraversePrevious").shift());
        // TODO XXX DEBUGGING ONLY
        SCROLLBAR_BINDINGS.add(new KeyBinding(F4, "TraverseDebug").alt().ctrl().shift());

        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(LEFT, "DecrementValue"));
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(KP_LEFT, "DecrementValue"));
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(UP, "DecrementValue").vertical());
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(KP_UP, "DecrementValue").vertical());
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(RIGHT, "IncrementValue"));
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(KP_RIGHT, "IncrementValue"));
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(DOWN, "IncrementValue").vertical());
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(KP_DOWN, "IncrementValue").vertical());

        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(LEFT, "TraverseLeft").vertical());
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(KP_LEFT, "TraverseLeft").vertical());
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(UP, "TraverseUp"));
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(KP_UP, "TraverseUp"));
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(RIGHT, "TraverseRight").vertical());
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(KP_RIGHT, "TraverseRight").vertical());
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(DOWN, "TraverseDown"));
        SCROLLBAR_BINDINGS.add(new ScrollBarKeyBinding(KP_DOWN, "TraverseDown"));

        SCROLLBAR_BINDINGS.add(new KeyBinding(HOME, KEY_RELEASED, "Home"));
        SCROLLBAR_BINDINGS.add(new KeyBinding(END, KEY_RELEASED, "End"));
    }

    @Override protected List<KeyBinding> createKeyBindings() {
        return SCROLLBAR_BINDINGS;
    }
    
    protected /*final*/ String matchActionForEvent(KeyEvent e) {
        String action = super.matchActionForEvent(e);
        if (action != null) {
            if (e.getCode() == LEFT || e.getCode() == KP_LEFT) {
                if (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                    action = getControl().getOrientation() == Orientation.HORIZONTAL ? "IncrementValue" : "DecrementValue";
                }
            } else if (e.getCode() == RIGHT || e.getCode() == KP_RIGHT) {
                if (getControl().getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                    action = getControl().getOrientation() == Orientation.HORIZONTAL ? "DecrementValue" : "IncrementValue";
                }
            }
        }
        return action;
    }

    @Override protected void callAction(String name) {
        if ("Home".equals(name)) home();
        else if ("End".equals(name)) end();
        else if ("IncrementValue".equals(name)) incrementValue();
        else if ("DecrementValue".equals(name)) decrementValue();
        else super.callAction(name);
        super.callAction(name);
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
    public void trackPress(MouseEvent e, double position) {

        /* We can get a press if someone presses an end button.  In that
         * case, we don't want to start a timeline because the end button
         * will have already done so.  We can detect that because the timeline
         * will not be null.
         */
        if (timeline != null) return;

        // determine the percentage of the way between min and max
        // represented by this mouse event
        final ScrollBar bar = getControl();
        // If not already focused, request focus
        final double pos = position;
        if (!bar.isFocused()) bar.requestFocus();
        final boolean incrementing = (pos > ((bar.getValue() - bar.getMin())/(bar.getMax() - bar.getMin())));
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);

        final EventHandler<ActionEvent> step =
                new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    boolean i = (pos > ((bar.getValue() - bar.getMin())/(bar.getMax() - bar.getMin())));
                    if (incrementing == i) {
                        // we started incrementing and still are, or we
                        // started decrementing and still are
                        bar.adjustValue(pos);
                    }
                    else if (timeline != null) {
                        // we've gone to far! just stop already
                        timeline.stop();
                        timeline = null;
                    }
                }
            };

        final KeyFrame kf = new KeyFrame(Duration.millis(200), step);
        timeline.getKeyFrames().add(kf);
        // do the first step immediately
        timeline.play();
        step.handle(null);
    }

    /**
     * @param position The mouse position on track with 0.0 being begining of track and 1.0 being the end
     */
    public void trackRelease(MouseEvent e, double position) {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    /**
     * Invoked by the ScrollBar {@link Skin} implementation whenever a mouse
     * press occurs on the decrement button of the bar.
     */
    public void decButtonPressed(MouseEvent e) {
        final ScrollBar bar = getControl();
        if (timeline != null) {
            com.sun.javafx.Logging.getJavaFXLogger().warning("timeline is not null");
            timeline.stop();
            timeline = null;
        }
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);

        final EventHandler<ActionEvent> dec =
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (bar.getValue() > bar.getMin()) {
                        bar.decrement();
                    }
                    else if (timeline != null) {
                        timeline.stop();
                        timeline = null;
                    }
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
    public void decButtonReleased(MouseEvent e) {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    /**
     * Invoked by the ScrollBar {@link Skin} implementation whenever a mouse
     * press occurs on the increment button of the bar.
     */
    public void incButtonPressed(MouseEvent e) {
        final ScrollBar bar = getControl();
        if (timeline != null) {
            com.sun.javafx.Logging.getJavaFXLogger().warning("timeline is not null");
            timeline.stop();
            timeline = null;
        }
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);

        final EventHandler<ActionEvent> inc =
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (bar.getValue() < bar.getMax()) {
                        bar.increment();
                    }
                    else if (timeline != null) {
                        timeline.stop();
                        timeline = null;
                    }
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
    public void incButtonReleased(MouseEvent e) {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    /**
     * @param position The mouse position on track with 0.0 being begining of track and 1.0 being the end
     */
    //public function thumbPressed(e:MouseEvent, position:Number):Void {
    //}

    /**
     * @param position The mouse position on track with 0.0 being begining of track and 1.0 being the end
     */
    public void thumbDragged(MouseEvent e, double position) {
        final ScrollBar scrollbar = getControl();
        double newValue = (position * (scrollbar.getMax() - scrollbar.getMin())) + scrollbar.getMin();
        if (!Double.isNaN(newValue)) {
            scrollbar.setValue(Utils.clamp(scrollbar.getMin(), newValue, scrollbar.getMax()));
        }
    }

    /**
     * @param position The mouse position on track with 0.0 being begining of track and 1.0 being the end
     */
    public void thumbReleased(MouseEvent e, double position) {
        // snap to the correct position on the scrollbar
        (getControl()).adjustValue(position);
    }

    /**
     * Class to handle key bindings based upon the orientation of the control.
     */
    public static class ScrollBarKeyBinding extends OrientedKeyBinding {
        public ScrollBarKeyBinding(KeyCode code, String action) {
            super(code, action);
        }

        public ScrollBarKeyBinding(KeyCode code, EventType<KeyEvent> type, String action) {
            super(code, type, action);
        }

        public @Override boolean getVertical(Control control) {
            return ((ScrollBar)control).getOrientation() == Orientation.VERTICAL;
        }
    }
}
