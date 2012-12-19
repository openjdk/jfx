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

import javafx.event.EventType;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import com.sun.javafx.Utils;
import com.sun.javafx.css.PseudoClass;

public class SliderBehavior extends BehaviorBase<Slider> {
    /**************************************************************************
     *                          Setup KeyBindings                             *
     *                                                                        *
     * We manually specify the focus traversal keys because Slider has        *
     * different usage for up/down arrow keys.                                *
     *************************************************************************/
    protected static final List<KeyBinding> SLIDER_BINDINGS = new ArrayList<KeyBinding>();
    static {
        SLIDER_BINDINGS.add(new KeyBinding(F4, "TraverseDebug").alt().ctrl().shift());

        SLIDER_BINDINGS.add(new SliderKeyBinding(LEFT, "DecrementValue"));
        SLIDER_BINDINGS.add(new SliderKeyBinding(KP_LEFT, "DecrementValue"));
        SLIDER_BINDINGS.add(new SliderKeyBinding(UP, "IncrementValue").vertical());
        SLIDER_BINDINGS.add(new SliderKeyBinding(KP_UP, "IncrementValue").vertical());
        SLIDER_BINDINGS.add(new SliderKeyBinding(RIGHT, "IncrementValue"));
        SLIDER_BINDINGS.add(new SliderKeyBinding(KP_RIGHT, "IncrementValue"));
        SLIDER_BINDINGS.add(new SliderKeyBinding(DOWN, "DecrementValue").vertical());
        SLIDER_BINDINGS.add(new SliderKeyBinding(KP_DOWN, "DecrementValue").vertical());

        SLIDER_BINDINGS.add(new KeyBinding(HOME, KEY_RELEASED, "Home"));
        SLIDER_BINDINGS.add(new KeyBinding(END, KEY_RELEASED, "End"));
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
    
    @Override
    protected void callAction(String name) {
        if ("Home".equals(name)) home();
        else if ("End".equals(name)) end();
        else if ("IncrementValue".equals(name)) incrementValue();
        else if ("DecrementValue".equals(name)) decrementValue();
        else super.callAction(name);
    }

    private TwoLevelFocusBehavior tlFocus;

    public SliderBehavior(Slider slider) {
        super(slider);
        /*
        ** only add this if we're on an embedded
        ** platform that supports 5-button navigation 
        */
        if (com.sun.javafx.scene.control.skin.Utils.isEmbeddedNonTouch()) {
            tlFocus = new TwoLevelFocusBehavior(slider); // needs to be last.
        }
    }

    @Override protected List<KeyBinding> createKeyBindings() {
        return SLIDER_BINDINGS;
    }

    /**************************************************************************
     *                         State and Functions                            *
     *************************************************************************/

    /**
     * Invoked by the Slider {@link Skin} implementation whenever a mouse press
     * occurs on the "track" of the slider. This will cause the thumb to be
     * moved by some amount.
     *
     * @param position The mouse position on track with 0.0 being beginning of
     *        track and 1.0 being the end
     */
    public void trackPress(MouseEvent e, double position) {
        // determine the percentage of the way between min and max
        // represented by this mouse event
        final Slider slider = getControl();
        // If not already focused, request focus
        if (!slider.isFocused()) slider.requestFocus();
        if (slider.getOrientation().equals(Orientation.HORIZONTAL)) {
            slider.adjustValue(position * (slider.getMax() - slider.getMin()) + slider.getMin());
        } else {
            slider.adjustValue((1-position) * (slider.getMax() - slider.getMin()) + slider.getMin());
        }
    }

    /**
     */
    public void trackRelease(MouseEvent e, double position) {
    }

     /**
     * @param position The mouse position on track with 0.0 being beginning of
      *       track and 1.0 being the end
     */
    public void thumbPressed(MouseEvent e, double position) {
        // If not already focused, request focus
        final Slider slider = getControl();
        if (!slider.isFocused())  slider.requestFocus();
        slider.setValueChanging(true);
    }

    /**
     * @param position The mouse position on track with 0.0 being beginning of
     *        track and 1.0 being the end
     */
    public void thumbDragged(MouseEvent e, double position) {
        final Slider slider = getControl();
        slider.setValue(Utils.clamp(slider.getMin(), (position * (slider.getMax() - slider.getMin())) + slider.getMin(), slider.getMax()));
    }

    private double snapValueToTicks(double val) {
        final Slider slider = getControl();
        double v = val;
        double tickSpacing = 0;
        // compute the nearest tick to this value
        if (slider.getMinorTickCount() != 0) {
            tickSpacing = slider.getMajorTickUnit() / (Math.max(slider.getMinorTickCount(),0)+1);
        } else {
            tickSpacing = slider.getMajorTickUnit();
        }
        int prevTick = (int)((v - slider.getMin())/ tickSpacing);
        double prevTickValue = (prevTick) * tickSpacing + slider.getMin();
        double nextTickValue = (prevTick + 1) * tickSpacing + slider.getMin();
        v = Utils.nearest(prevTickValue, v, nextTickValue);
        return Utils.clamp(slider.getMin(), v, slider.getMax());
    }


    /**
     * When thumb is released valueChanging should be set to false.
     */
    public void thumbReleased(MouseEvent e) {
        final Slider slider = getControl();
        slider.setValueChanging(false);
        // RT-15207 When snapToTicks is true, slider value calculated in drag
        // is then snapped to the nearest tick on mouse release.
        if (slider.isSnapToTicks()) {
            slider.setValue(snapValueToTicks(slider.getValue()));
        }
    }

    void home() {
        final Slider slider = getControl();
        slider.adjustValue(slider.getMin());
    }

    void decrementValue() {
        final Slider slider = getControl();
        // RT-8634 If snapToTicks is true and block increment is less than
        // tick spacing, tick spacing is used as the decrement value.
        if (slider.isSnapToTicks()) {
            slider.adjustValue(slider.getValue() - computeIncrement());
        } else {
            slider.decrement();
        }
        
    }

    void end() {
        final Slider slider = getControl();
        slider.adjustValue(slider.getMax());
    }

    void incrementValue() {
        final Slider slider = getControl();
        // RT-8634 If snapToTicks is true and block increment is less than
        // tick spacing, tick spacing is used as the increment value.
        if (slider.isSnapToTicks()) {
            slider.adjustValue(slider.getValue()+ computeIncrement());
        } else {
            slider.increment();
        }
    }

    // Used only if snapToTicks is true.
    double computeIncrement() {
        final Slider slider = getControl();
        double tickSpacing = 0;
        if (slider.getMinorTickCount() != 0) {
            tickSpacing = slider.getMajorTickUnit() / (Math.max(slider.getMinorTickCount(),0)+1);
        } else {
            tickSpacing = slider.getMajorTickUnit();
        }

        if (slider.getBlockIncrement() > 0 && slider.getBlockIncrement() < tickSpacing) {
                return tickSpacing;
        }

        return slider.getBlockIncrement();
    }

    public static class SliderKeyBinding extends OrientedKeyBinding {
        public SliderKeyBinding(KeyCode code, String action) {
            super(code, action);
        }

        public SliderKeyBinding(KeyCode code, EventType<KeyEvent> type, String action) {
            super(code, type, action);
        }

        public @Override boolean getVertical(Control control) {
            return ((Slider)control).getOrientation() == Orientation.VERTICAL;
        }
    }


    private static final PseudoClass.State INTERNAL_PSEUDOCLASS_STATE = 
            PseudoClass.getState("internal-focus");
    private static final PseudoClass.State EXTERNAL_PSEUDOCLASS_STATE = 
            PseudoClass.getState("external-focus");

    /**
     * {@inheritDoc}
     */
    @Override public PseudoClass.States getPseudoClassStates() {
        PseudoClass.States states = super.getPseudoClassStates();
        if (tlFocus != null) {
            if (tlFocus.isExternalFocus()) states.addState(EXTERNAL_PSEUDOCLASS_STATE);
            else states.addState(INTERNAL_PSEUDOCLASS_STATE);
        }
        return states;
    }

}
