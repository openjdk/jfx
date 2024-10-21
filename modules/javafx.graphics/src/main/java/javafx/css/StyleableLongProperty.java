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
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.Utils;
import javafx.beans.property.LongPropertyBase;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * This class extends {@code LongPropertyBase} and provides a partial
 * implementation of a {@code StyleableProperty}. The method
 * {@link StyleableProperty#getCssMetaData()} is not implemented.
 *
 * This class is used to make a {@link javafx.beans.property.LongProperty},
 * that would otherwise be implemented as a {@link LongPropertyBase},
 * styleable by CSS.
 *
 * @see javafx.beans.property.LongPropertyBase
 * @see CssMetaData
 * @see StyleableProperty
 * @since JavaFX 8.0
 */
public abstract class StyleableLongProperty
    extends LongPropertyBase implements StyleableProperty<Number> {

    /**
     * The constructor of the {@code StyleableLongProperty}.
     */
    public StyleableLongProperty() {
        super();
    }

    /**
     * The constructor of the {@code StyleableLongProperty}.
     *
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public StyleableLongProperty(long initialValue) {
        super(initialValue);
    }

    /** {@inheritDoc} */
    @Override
    public void applyStyle(StyleOrigin origin, Number v) {
        // If the value is applied for the first time, we don't start a transition.
        TransitionDefinition transition = getBean() instanceof Node node && !NodeHelper.isInitialCssState(node) ?
            NodeHelper.findTransitionDefinition(node, getCssMetaData()) : null;

        long newValue = v != null ? v.longValue() : 0;

        if (transition == null) {
            set(newValue);
        } else if (mediator == null || mediator.endValue != newValue) {
            // We only start a new transition if the new target value is different from the target
            // value of the existing transition. This scenario can sometimes happen when a CSS value
            // is redundantly applied, which would cause unexpected animations if we allowed the new
            // transition to interrupt the existing transition.
            mediator = new TransitionMediatorImpl(get(), newValue);
            mediator.run(transition, getCssMetaData().getProperty(), Toolkit.getToolkit().getPrimaryTimer().nanos());
        }

        this.origin = origin;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(ObservableValue<? extends Number> observable) {
        super.bind(observable);
        onUserChange();
    }

    /** {@inheritDoc} */
    @Override
    public void set(long v) {
        super.set(v);
        onUserChange();
    }

    /** {@inheritDoc} */
    @Override
    public StyleOrigin getStyleOrigin() { return origin; }

    private void onUserChange() {
        origin = StyleOrigin.USER;

        if (mediator != null) {
            mediator.cancel();
        }
    }

    private StyleOrigin origin;
    private TransitionMediatorImpl mediator;

    private final class TransitionMediatorImpl extends TransitionMediator {
        private final long startValue;
        private final long endValue;
        private long reversingAdjustedStartValue;

        public TransitionMediatorImpl(long startValue, long endValue) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.reversingAdjustedStartValue = startValue;
        }

        @Override
        public void onUpdate(double progress) {
            // Longs are interpolated in real number space and rounded to the nearest long.
            long diff = endValue - startValue;
            long result = startValue + Math.round(progress * diff);
            StyleableLongProperty.super.set(progress < 1 ? Utils.clamp(startValue, result, endValue) : endValue);
        }

        @Override
        public void onStop() {
            mediator = null;
        }

        @Override
        public StyleableProperty<?> getStyleableProperty() {
            return StyleableLongProperty.this;
        }

        @Override
        public boolean updateReversingAdjustedStartValue(TransitionMediator existingMediator) {
            var mediator = (TransitionMediatorImpl)existingMediator;

            if (mediator.reversingAdjustedStartValue == endValue) {
                reversingAdjustedStartValue = mediator.endValue;
                return true;
            }

            return false;
        }
    }
}
