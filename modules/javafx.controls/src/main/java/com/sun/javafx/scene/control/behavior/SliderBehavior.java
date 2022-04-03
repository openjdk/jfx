/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Orientation;
import javafx.scene.control.Skin;
import javafx.scene.control.Slider;
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import com.sun.javafx.util.Utils;
import static javafx.scene.input.KeyCode.*;

public class SliderBehavior extends BehaviorBase<Slider> {

    private final InputMap<Slider> sliderInputMap;

    private TwoLevelFocusBehavior tlFocus;

    public SliderBehavior(Slider slider) {
        super(slider);

        // create a map for slider-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        sliderInputMap = createInputMap();

        // then slider-specific mappings for key input
        addDefaultMapping(sliderInputMap,
            new InputMap.KeyMapping(HOME, KeyEvent.KEY_RELEASED, e -> home()),
            new InputMap.KeyMapping(END, KeyEvent.KEY_RELEASED, e -> end())
        );

        // we split the rest of the mappings into vertical and horizontal slider
        // child input maps
        // -- horizontal
        InputMap<Slider> horizontalMappings = new InputMap<>(slider);
        horizontalMappings.setInterceptor(e -> slider.getOrientation() != Orientation.HORIZONTAL);
        horizontalMappings.getMappings().addAll(
            // we use the rtl method to translate depending on the RTL state of the UI
            new InputMap.KeyMapping(LEFT, e -> rtl(slider, this::incrementValue, this::decrementValue)),
            new InputMap.KeyMapping(KP_LEFT, e -> rtl(slider, this::incrementValue, this::decrementValue)),
            new InputMap.KeyMapping(RIGHT, e -> rtl(slider, this::decrementValue, this::incrementValue)),
            new InputMap.KeyMapping(KP_RIGHT, e -> rtl(slider, this::decrementValue, this::incrementValue))
        );
        addDefaultChildMap(sliderInputMap, horizontalMappings);

        // -- vertical
        InputMap<Slider> verticalMappings = new InputMap<>(slider);
        verticalMappings.setInterceptor(e -> slider.getOrientation() != Orientation.VERTICAL);
        verticalMappings.getMappings().addAll(
                new InputMap.KeyMapping(DOWN, e -> decrementValue()),
                new InputMap.KeyMapping(KP_DOWN, e -> decrementValue()),
                new InputMap.KeyMapping(UP, e -> incrementValue()),
                new InputMap.KeyMapping(KP_UP, e -> incrementValue())
        );
        addDefaultChildMap(sliderInputMap, verticalMappings);

        // Only add this if we're on an embedded platform that supports 5-button navigation
        if (com.sun.javafx.scene.control.skin.Utils.isTwoLevelFocus()) {
            tlFocus = new TwoLevelFocusBehavior(slider); // needs to be last.
        }
    }

    @Override public void dispose() {
        if (tlFocus != null) tlFocus.dispose();
        super.dispose();
    }

    @Override public InputMap<Slider> getInputMap() {
        return sliderInputMap;
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
        final Slider slider = getNode();
        slider.requestFocus();
        if (slider.getOrientation().equals(Orientation.HORIZONTAL)) {
            slider.adjustValue(position * (slider.getMax() - slider.getMin()) + slider.getMin());
        } else {
            slider.adjustValue((1-position) * (slider.getMax() - slider.getMin()) + slider.getMin());
        }
    }

     /**
     * @param position The mouse position on track with 0.0 being beginning of
      *       track and 1.0 being the end
     */
    public void thumbPressed(MouseEvent e, double position) {
        final Slider slider = getNode();
        slider.requestFocus();
        slider.setValueChanging(true);
    }

    /**
     * @param position The mouse position on track with 0.0 being beginning of
     *        track and 1.0 being the end
     */
    public void thumbDragged(MouseEvent e, double position) {
        final Slider slider = getNode();
        slider.setValue(Utils.clamp(slider.getMin(), (position * (slider.getMax() - slider.getMin())) + slider.getMin(), slider.getMax()));
    }

    /**
     * When thumb is released valueChanging should be set to false.
     */
    public void thumbReleased(MouseEvent e) {
        final Slider slider = getNode();
        slider.setValueChanging(false);
        // RT-15207 When snapToTicks is true, slider value calculated in drag
        // is then snapped to the nearest tick on mouse release.
        slider.adjustValue(slider.getValue());
    }

    void home() {
        final Slider slider = getNode();
        slider.adjustValue(slider.getMin());
    }

    void decrementValue() {
        final Slider slider = getNode();
        // RT-8634 If snapToTicks is true and block increment is less than
        // tick spacing, tick spacing is used as the decrement value.
        if (slider.isSnapToTicks()) {
            slider.adjustValue(slider.getValue() - computeIncrement());
        } else {
            slider.decrement();
        }

    }

    void end() {
        final Slider slider = getNode();
        slider.adjustValue(slider.getMax());
    }

    void incrementValue() {
        final Slider slider = getNode();
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
        final Slider slider = getNode();
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

//    public static class SliderKeyBinding extends OrientedKeyBinding {
//        public SliderKeyBinding(KeyCode code, String action) {
//            super(code, action);
//        }
//
//        public SliderKeyBinding(KeyCode code, EventType<KeyEvent> type, String action) {
//            super(code, type, action);
//        }
//
//        public @Override boolean getVertical(Control control) {
//            return ((Slider)control).getOrientation() == Orientation.VERTICAL;
//        }
//    }

}
