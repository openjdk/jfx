/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.TransitionDefinition;
import com.sun.javafx.css.TransitionTimer;
import com.sun.javafx.scene.NodeHelper;
import javafx.animation.Interpolatable;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * This class extends {@code ObjectPropertyBase} and provides a partial
 * implementation of a {@code StyleableProperty}. The method
 * {@link StyleableProperty#getCssMetaData()} is not implemented.
 *
 * This class is used to make a {@link javafx.beans.property.ObjectProperty},
 * that would otherwise be implemented as a {@link ObjectPropertyBase},
 * styleable by CSS.
 *
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
    public void applyStyle(StyleOrigin origin, T v) {
        T oldValue;

        if (v == null) {
            set(null);
        } else if ((oldValue = get()) == null) {
            set(v);
        } else {
            // If this.origin == null, we're setting the value for the first time.
            // No transition should be started in this case.
            TransitionDefinition transition = this.origin != null
                && v instanceof Interpolatable<?>
                && getBean() instanceof Node node ? NodeHelper.findTransitionDefinition(node, getCssMetaData()) : null;

            if (transition != null) {
                timer = TransitionTimer.run(new TransitionTimerImpl<>(this, oldValue, v), transition);
            } else {
                set(v);
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
        TransitionTimer.cancel(timer, true);
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
        if (TransitionTimer.cancel(timer, false)) {
            origin = StyleOrigin.USER;
        }
    }

    /** {@inheritDoc} */
    @Override
    public StyleOrigin getStyleOrigin() { return origin; }

    private StyleOrigin origin = null;
    private TransitionTimer<?, ?> timer = null;

    // This class must not retain a strong reference to the enclosing property, because transitions
    // don't keep properties (and their scene graph nodes) alive.
    private static class TransitionTimerImpl<T> extends TransitionTimer<T, StyleableObjectProperty<T>> {
        final T oldValue;
        final T newValue;

        TransitionTimerImpl(StyleableObjectProperty<T> property, T oldValue, T newValue) {
            super(property);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void onUpdate(StyleableObjectProperty<T> property, double progress) {
            property.set(progress < 1 ? ((Interpolatable<T>)oldValue).interpolate(newValue, progress) : newValue);
        }

        @Override
        public void onStop(StyleableObjectProperty<T> property) {
            property.timer = null;
        }
    }

}
