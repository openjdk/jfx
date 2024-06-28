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
import com.sun.javafx.scene.NodeHelper;
import javafx.animation.Interpolatable;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
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
    public void applyStyle(StyleOrigin origin, T newValue) {
        T oldValue;

        if (newValue == null) {
            set(null);
        } else if (!(newValue instanceof Interpolatable<?>)
                   || ((oldValue = get()) == null)
                   || !(newValue.getClass().isInstance(oldValue))) {
            // Consider a case where T := Paint. Now 'oldValue' could be a Color instance, while 'newValue' could
            // be a LinearGradient instance. Both types implement Interpolatable, but with different type arguments.
            // We detect this case by checking whether 'newValue' is an instance of 'oldValue' (so that
            // oldValue.interpolate(newValue, t) succeeds), and skipping the transition when the test fails.
            set(newValue);
        } else {
            // If this.origin == null, we're setting the value for the first time.
            // No transition should be started in this case.
            TransitionDefinition transition = this.origin != null && getBean() instanceof Node node ?
                NodeHelper.findTransitionDefinition(node, getCssMetaData()) : null;

            if (transition == null) {
                set(newValue);
            } else if (mediator == null || !Objects.equals(mediator.newValue, newValue)) {
                // We only start a new transition if the new target value is different from the target
                // value of the existing transition. This scenario can sometimes happen when a CSS value
                // is redundantly applied, which would cause unexpected animations if we allowed the new
                // transition to interrupt the existing transition.
                mediator = new TransitionMediatorImpl(oldValue, newValue);
                mediator.run(transition);
            }
        }

        this.origin = origin;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(ObservableValue<? extends T> observable) {
        super.bind(observable);
        origin = StyleOrigin.USER;

        // Calling the 'bind' method always cancels a transition timer.
        if (mediator != null) {
            mediator.cancel(true);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void set(T v) {
        super.set(v);

        // Calling the 'set' method cancels the transition timer, but not if the 'set' method was
        // directly called by the timer itself (i.e. a timer will not accidentally cancel itself).
        // Note that indirect cancellation is still possible: a timer may fire a transition event,
        // which could cause user code to be executed that invokes this 'set' method. In that case,
        // the call will cancel the timer.
        if (mediator == null || mediator.cancel(false)) {
            origin = StyleOrigin.USER;
        }
    }

    /** {@inheritDoc} */
    @Override
    public StyleOrigin getStyleOrigin() { return origin; }

    private StyleOrigin origin = null;
    private TransitionMediatorImpl mediator = null;

    private final class TransitionMediatorImpl extends TransitionMediator {
        private final T oldValue;
        private final T newValue;

        TransitionMediatorImpl(T oldValue, T newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onUpdate(double progress) {
            set(progress < 1 ? ((Interpolatable<T>)oldValue).interpolate(newValue, progress) : newValue);
        }

        @Override
        public void onStop() {
            // When the transition is cancelled or completed, we clear the reference to this mediator.
            // However, when this mediator was cancelled by a reversing transition, the 'mediator' field
            // refers to the reversing mediator, and not to this mediator. We need to be careful to only
            // clear references to this mediator.
            if (mediator == this) {
                mediator = null;
            }
        }

        @Override
        public StyleableProperty<?> getStyleableProperty() {
            return StyleableObjectProperty.this;
        }
    }
}
