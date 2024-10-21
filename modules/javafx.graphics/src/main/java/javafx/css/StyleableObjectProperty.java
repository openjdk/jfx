/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css;

import com.sun.javafx.css.TransitionMediator;
import com.sun.javafx.css.TransitionDefinition;
import com.sun.javafx.css.SubPropertyConverter;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.InterpolationUtils;
import javafx.animation.Interpolatable;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class extends {@code ObjectPropertyBase} and provides a partial
 * implementation of a {@code StyleableProperty}. The method
 * {@link StyleableProperty#getCssMetaData()} is not implemented.
 *
 * This class is used to make a {@link javafx.beans.property.ObjectProperty},
 * that would otherwise be implemented as a {@link ObjectPropertyBase},
 * styleable by CSS.
 *
 * @param <T> the property value type
 * @see javafx.beans.property.ObjectPropertyBase
 * @see CssMetaData
 * @see StyleableProperty
 * @since JavaFX 8.0
 */
public abstract class StyleableObjectProperty<T>
    extends ObjectPropertyBase<T> implements StyleableProperty<T> {

    /**
     * The constructor of the {@code StyleableObjectProperty}.
     */
    public StyleableObjectProperty() {
        super();
    }

    /**
     * The constructor of the {@code StyleableObjectProperty}.
     *
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public StyleableObjectProperty(T initialValue) {
        super(initialValue);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void applyStyle(StyleOrigin origin, T newValue) {
        CssMetaData<? extends Styleable, T> metadata = getCssMetaData();
        StyleConverter<?, T> converter = metadata.getConverter();
        T oldValue = get();

        if (oldValue != null && newValue != null && converter instanceof SubPropertyConverter c) {
            applyComponents(oldValue, newValue, metadata, c);
        } else {
            applyValue(oldValue, newValue, metadata);
        }

        this.origin = origin;
    }

    /**
     * Sets the value of the property, and potentially starts a transition.
     * This method is used for values that don't support component-wise transitions, and for cases
     * where one of the values is {@code null} and we fall back to a discrete transition.
     *
     * @param oldValue the old value
     * @param newValue the new value
     * @param metadata the CSS metadata of the value
     */
    private void applyValue(T oldValue, T newValue, CssMetaData<? extends Styleable, T> metadata) {
        // If the value is applied for the first time, we don't start a transition.
        TransitionDefinition transition =
            getBean() instanceof Node node && !NodeHelper.isInitialCssState(node) ?
            NodeHelper.findTransitionDefinition(node, metadata) : null;

        // We only start a new transition if the new target value is different from the target
        // value of the existing transition. This scenario can sometimes happen when a CSS value
        // is redundantly applied, which would cause unexpected animations if we allowed the new
        // transition to interrupt the existing transition.
        if (transition == null) {
            set(newValue);
        } else if (controller == null || !Objects.equals(newValue, controller.getTargetValue())) {
            TransitionControllerBase controller;

            // 'oldValue' and 'newValue' could be objects that both implement Interpolatable, but with
            // different type arguments. We detect this case by checking whether 'newValue' is an instance
            // of 'oldValue' (so that oldValue.interpolate(newValue, t) succeeds), and only applying the
            // transition when the test succeeds.
            if (oldValue instanceof Interpolatable<?>
                    && newValue instanceof Interpolatable<?>
                    && newValue.getClass().isInstance(oldValue)) {
                controller = new InterpolatableTransitionController(oldValue, newValue);
            } else {
                controller = new DiscreteTransitionController(oldValue, newValue);
            }

            this.controller = controller; // needs to be set before calling run()
            controller.run(transition, metadata.getProperty(), Toolkit.getToolkit().getPrimaryTimer().nanos());
        }
    }

    /**
     * Sets the value of the property, and potentially starts a transition.
     * This method is used for values that support component-wise transitions.
     *
     * @param newValue the new value
     * @param metadata the CSS metadata of the value
     * @param converter the style converter of the value
     */
    private void applyComponents(T oldValue, T newValue,
                                 CssMetaData<? extends Styleable, T> metadata,
                                 SubPropertyConverter<T> converter) {
        // If the value is applied for the first time, we don't start a transition.
        Map<CssMetaData<? extends Styleable, ?>, TransitionDefinition> transitions =
            getBean() instanceof Node node && !NodeHelper.isInitialCssState(node) ?
            NodeHelper.findTransitionDefinitions(node, metadata) : null;

        List<CssMetaData<? extends Styleable, ?>> subMetadata = metadata.getSubProperties();

        if (transitions == null || transitions.isEmpty() || subMetadata == null || subMetadata.isEmpty()) {
            set(newValue);
        } else if (controller == null || !Objects.equals(newValue, controller.getTargetValue())) {
            var oldCssValues = converter.convertBack(oldValue);
            var newCssValues = converter.convertBack(newValue);
            var controller = new AggregatingTransitionController(newValue);

            for (int i = 0, max = subMetadata.size(); i < max; ++i) {
                processComponent(controller, subMetadata.get(i), transitions, oldCssValues, newCssValues);
            }

            this.controller = controller; // needs to be set before calling run()
            controller.run();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processComponent(AggregatingTransitionController controller,
                                  CssMetaData<? extends Styleable, ?> metadata,
                                  Map<CssMetaData<? extends Styleable, ?>, TransitionDefinition> transitions,
                                  Map<CssMetaData<? extends Styleable, ?>, Object> oldCssValues,
                                  Map<CssMetaData<? extends Styleable, ?>, Object> newCssValues) {
        Object oldCssValue = oldCssValues.get(metadata);
        Object newCssValue = newCssValues.get(metadata);

        // If the old and new CSS value is equal, we don't need to bother checking for a specified
        // transition, as it would not be noticeable anyway. Note that we're using deepEquals, as
        // the value might be an array of CSS values.
        if (Objects.deepEquals(oldCssValue, newCssValue)) {
            controller.addValue(metadata, oldCssValue);
        } else {
            // The following code accounts for pre-existing transition mediators that can occur when more
            // than two states are involved. Consider the following scenario:
            //
            //     .button {
            //       -fx-border-color: red;
            //       -fx-border-width: 5;
            //       transition: all 4s;
            //     }
            //
            //     .button:hover {
            //       -fx-border-color: green;
            //     }
            //
            //     .button:pressed {
            //       -fx-border-color: blue;
            //       -fx-border-width: 20;
            //     }
            //
            // Now assume the following interactions:
            //   1. Move the cursor over the button (:hover)
            //   2. Press the mouse button (:hover:pressed)
            //   3. Release the mouse button (:hover)
            //   4. Move the cursor away from the button.
            //
            // When the mouse button is released (step 3), the -fx-border-width sub-property transitions
            // from 20 (the current value) to 5 (the target value). Then, when the cursor is moved away
            // from the button while the -fx-border-width transition is still running, we need to preserve
            // the running transition by adding its mediator to the newly created transition controller
            // that manages the hover->base transition. In this way, the new transition controller will
            // continue to aggregate the effects of the pre-existing transition.
            //
            var transition = transitions.get(metadata);
            var existingTimer = NodeHelper.findTransitionTimer((Node)getBean(), metadata.getProperty());
            var existingMediator = existingTimer != null && existingTimer.getMediator()
                instanceof StyleableObjectProperty.ComponentTransitionMediator m ? m : null;

            if (existingMediator != null) {
                if (transition == null) {
                    existingMediator.cancel();
                    controller.addValue(metadata, newCssValue);
                } else if (Objects.deepEquals(newCssValue, existingMediator.endValue)) {
                    controller.addExistingMediator(existingMediator);
                } else {
                    controller.addMediator(oldCssValue, newCssValue, metadata, transition);
                }
            } else if (transition != null) {
                controller.addMediator(oldCssValue, newCssValue, metadata, transition);
            } else {
                controller.addValue(metadata, newCssValue);
            }
        }

        List<CssMetaData<? extends Styleable, ?>> subMetadata = metadata.getSubProperties();
        if (subMetadata != null) {
            for (int i = 0, max = subMetadata.size(); i < max; ++i) {
                processComponent(controller, subMetadata.get(i), transitions, oldCssValues, newCssValues);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void bind(ObservableValue<? extends T> observable) {
        super.bind(observable);
        onUserChange();
    }

    /** {@inheritDoc} */
    @Override
    public void set(T v) {
        super.set(v);
        onUserChange();
    }

    /** {@inheritDoc} */
    @Override
    public StyleOrigin getStyleOrigin() { return origin; }

    private void onUserChange() {
        origin = StyleOrigin.USER;

        if (controller != null) {
            controller.cancel();
        }
    }

    private StyleOrigin origin;
    private TransitionController<T> controller;

    /**
     * Common interface for transition controllers:
     * <ol>
     *     <li>{@link DiscreteTransitionController}
     *     <li>{@link InterpolatableTransitionController}
     *     <li>{@link AggregatingTransitionController}
     * </ol>
     *
     * @param <T> the property value type
     */
    private interface TransitionController<T> {
        T getTargetValue();
        void cancel();
    }

    /**
     * Base class for transition controllers that don't support component-wise transitions.
     */
    private abstract class TransitionControllerBase extends TransitionMediator implements TransitionController<T> {
        final T startValue;
        final T endValue;
        private T reversingAdjustedStartValue;

        TransitionControllerBase(T startValue, T endValue) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.reversingAdjustedStartValue = startValue;
        }

        @Override
        public void onStop() {
            controller = null;
        }

        @Override
        public StyleableProperty<?> getStyleableProperty() {
            return StyleableObjectProperty.this;
        }

        @Override
        public T getTargetValue() {
            return endValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean updateReversingAdjustedStartValue(TransitionMediator existingMediator) {
            var mediator = (TransitionControllerBase)existingMediator;

            if (Objects.deepEquals(mediator.reversingAdjustedStartValue, endValue)) {
                reversingAdjustedStartValue = mediator.endValue;
                return true;
            }

            return false;
        }
    }

    /**
     * Controller for transitions of non-interpolatable values using discrete interpolation.
     */
    private final class DiscreteTransitionController extends TransitionControllerBase {
        DiscreteTransitionController(T startValue, T endValue) {
            super(startValue, endValue);
        }

        @Override
        public void onUpdate(double progress) {
            StyleableObjectProperty.super.set(progress < 0.5 ? startValue : endValue);
        }
    }

    /**
     * Controller for transitions of {@link Interpolatable} values.
     */
    private final class InterpolatableTransitionController extends TransitionControllerBase {
        InterpolatableTransitionController(T startValue, T endValue) {
            super(startValue, endValue);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onUpdate(double progress) {
            StyleableObjectProperty.super.set(
                progress < 1 ? ((Interpolatable<T>)startValue).interpolate(endValue, progress) : endValue);
        }
    }

    /**
     * Controller for component-wise transitions that aggregates the effects of its component transitions.
     * <p>
     * For each animation frame, this controller waits until all its component mediators have updated
     * their current value, and then converts the collected component values into a new object of type
     * {@code T} using the {@link StyleConverter} of this {@link StyleableObjectProperty}.
     */
    private final class AggregatingTransitionController implements TransitionController<T> {
        private final T newValue;
        private final Map<CssMetaData<? extends Styleable, ?>, Object> cssValues;
        private final List<ComponentTransitionMediator<?>> mediators = new ArrayList<>(5);
        private int remainingValues;

        AggregatingTransitionController(T newValue) {
            this.newValue = newValue;
            this.cssValues = new HashMap<>();
        }

        @Override
        public T getTargetValue() {
            return newValue;
        }

        @Override
        public void cancel() {
            // Cancelling a mediator removes it from the 'mediators' list, so we need
            // to make a copy of the list before we iterate on it.
            for (var mediator : List.copyOf(mediators)) {
                mediator.cancel();
            }
        }

        /**
         * Starts all transition timers managed by this controller.
         */
        public void run() {
            remainingValues = mediators.size();
            long nanoNow = Toolkit.getToolkit().getPrimaryTimer().nanos();

            // Starting a transition may result in instant cancellation (if the combined duration
            // is zero), which would instantly remove the mediator from the list. This is why we
            // need to make a copy of the 'mediators' list before we iterate on it.
            for (var mediator : List.copyOf(mediators)) {
                mediator.run(nanoNow);
            }
        }

        /**
         * Adds a component value to the value cache.
         *
         * @param metadata the {@code CssMetaData} of the component
         * @param value the new value
         */
        public void addValue(CssMetaData<? extends Styleable, ?> metadata, Object value) {
            cssValues.put(metadata, value);
        }

        /**
         * Adds a new component transition mediator to this controller.
         *
         * @param oldValue the old component value
         * @param newValue the new component value
         * @param metadata the component metadata
         * @param transition the transition definition
         * @param <U> the component value type
         */
        public <U> void addMediator(U oldValue, U newValue,
                                    CssMetaData<? extends Styleable, ?> metadata,
                                    TransitionDefinition transition) {
            mediators.add(new ComponentTransitionMediator<>(oldValue, newValue, this, metadata, transition));
        }

        /**
         * Adds an existing component transition mediator to this controller.
         * After calling this method, the existing mediator will be associated with this controller.
         *
         * @param mediator the existing component transition mediator
         * @param <U> the component value type
         */
        public <U> void addExistingMediator(ComponentTransitionMediator<U> mediator) {
            mediator.associatedController = this;
            mediators.add(mediator);
        }

        /**
         * This method is called when a component transition mediator updates its current value.
         * When all component values have been collected, they are converted to an object of type
         * {@code T} using the {@link StyleConverter} of this {@link StyleableObjectProperty}.
         *
         * @param metadata the {@code CssMetaData} of the component
         * @param value the new value
         */
        public void onUpdate(CssMetaData<? extends Styleable, ?> metadata, Object value) {
            cssValues.put(metadata, value);

            if (--remainingValues == 0) {
                remainingValues = mediators.size();
                StyleableObjectProperty.super.set(getCssMetaData().getConverter().convert(cssValues));
            }
        }

        /**
         * This method is called when a component transition mediator is stopped.
         *
         * @param mediator the component transition mediator
         */
        public void onStop(ComponentTransitionMediator<?> mediator) {
            for (int i = 0, max = mediators.size(); i < max; ++i) {
                if (mediators.get(i) == mediator) {
                    mediators.remove(i);
                    break;
                }
            }

            if (mediators.isEmpty()) {
                StyleableObjectProperty.this.controller = null;
            }
        }
    }

    /**
     * Transition mediator for CSS sub-properties.
     * <p>
     * This transition mediator does not interact with the {@link StyleableObjectProperty} directly,
     * but instead feeds its effects into the associated {@link AggregatingTransitionController}.
     *
     * @param <U> the type of the sub-property
     */
    private final class ComponentTransitionMediator<U> extends TransitionMediator {
        private final U startValue;
        private final U endValue;
        private final CssMetaData<? extends Styleable, ?> metadata;
        private final TransitionDefinition definition;
        private AggregatingTransitionController associatedController;
        private U reversingAdjustedStartValue;
        private boolean running;

        ComponentTransitionMediator(U startValue, U endValue,
                                    AggregatingTransitionController associatedController,
                                    CssMetaData<? extends Styleable, ?> metadata,
                                    TransitionDefinition definition) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.reversingAdjustedStartValue = startValue;
            this.associatedController = associatedController;
            this.metadata = metadata;
            this.definition = definition;
        }

        @Override
        public StyleableProperty<?> getStyleableProperty() {
            return StyleableObjectProperty.this;
        }

        public void run(long now) {
            if (!running) {
                running = true;
                run(definition, metadata.getProperty(), now);
            }
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void onUpdate(double progress) {
            Object value;

            if (progress < 1) {
                if (startValue instanceof Interpolatable[][] ov && endValue instanceof Interpolatable[][] nv) {
                    value = InterpolationUtils.interpolateArraySeriesPairwise(ov, nv, progress);
                } else if (startValue instanceof Interpolatable[] ov && endValue instanceof Interpolatable[] nv) {
                    value = InterpolationUtils.interpolateArraysPairwise(ov, nv, progress);
                } else if (startValue instanceof Interpolatable && endValue instanceof Interpolatable) {
                    value = ((Interpolatable<U>)startValue).interpolate(endValue, progress);
                } else {
                    value = InterpolationUtils.interpolateDiscrete(startValue, endValue, progress);
                }
            } else {
                value = endValue;
            }

            associatedController.onUpdate(metadata, value);
        }

        @Override
        public void onStop() {
            associatedController.onStop(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean updateReversingAdjustedStartValue(TransitionMediator existingMediator) {
            var mediator = (ComponentTransitionMediator<U>)existingMediator;

            if (Objects.deepEquals(mediator.reversingAdjustedStartValue, endValue)) {
                reversingAdjustedStartValue = mediator.endValue;
                return true;
            }

            return false;
        }
    }
}
