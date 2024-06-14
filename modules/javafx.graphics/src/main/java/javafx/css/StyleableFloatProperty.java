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
import javafx.beans.property.FloatPropertyBase;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * This class extends {@code FloatPropertyBase} and provides a partial
 * implementation of a {@code StyleableProperty}. The method
 * {@link StyleableProperty#getCssMetaData()} is not implemented.
 *
 * This class is used to make a {@link javafx.beans.property.FloatProperty},
 * that would otherwise be implemented as a {@link FloatPropertyBase},
 * styleable by CSS.
 *
 * @see javafx.beans.property.FloatPropertyBase
 * @see CssMetaData
 * @see StyleableProperty
 * @since JavaFX 8.0
 */
public abstract class StyleableFloatProperty
    extends FloatPropertyBase implements StyleableProperty<Number> {

    /**
     * The constructor of the {@code StyleableFloatProperty}.
     */
    public StyleableFloatProperty() {
        super();
    }

    /**
     * The constructor of the {@code StyleableFloatProperty}.
     *
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public StyleableFloatProperty(float initialValue) {
        super(initialValue);
    }

    /** {@inheritDoc} */
    @Override
    public void applyStyle(StyleOrigin origin, Number v) {
        TransitionDefinition transition = this.origin != null && getBean() instanceof Node node ?
            NodeHelper.findTransitionDefinition(node, getCssMetaData()) : null;

        float newValue = v != null ? v.floatValue() : 0;

        if (transition == null) {
            set(newValue);
        } else if (mediator == null || mediator.newValue != newValue) {
            // We only start a new transition if the new target value is different from the target
            // value of the existing transition. This scenario can sometimes happen when a CSS value
            // is redundantly applied, which would cause unexpected animations if we allowed the new
            // transition to interrupt the existing transition.
            mediator = new TransitionMediatorImpl(get(), newValue);
            mediator.run(transition);
        }

        this.origin = origin;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(ObservableValue<? extends Number> observable) {
        super.bind(observable);
        origin = StyleOrigin.USER;

        // Calling the 'bind' method always cancels a transition timer.
        if (mediator != null) {
            mediator.cancel(true);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void set(float v) {
        super.set(v);

        if (mediator == null || mediator.cancel(false)) {
            origin = StyleOrigin.USER;
        }
    }

    /** {@inheritDoc} */
    @Override
    public StyleOrigin getStyleOrigin() { return origin; }

    private StyleOrigin origin = null;
    private TransitionMediatorImpl mediator = null;

    private class TransitionMediatorImpl extends TransitionMediator {
        private final float oldValue;
        private final float newValue;

        public TransitionMediatorImpl(float oldValue, float newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public void onUpdate(double progress) {
            set(progress < 1 ? oldValue + (newValue - oldValue) * (float)progress : newValue);
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
            return StyleableFloatProperty.this;
        }
    }
}
