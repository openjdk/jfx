/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.StyleableTimer;
import com.sun.javafx.css.TransitionTimer;
import com.sun.javafx.scene.NodeHelper;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import java.lang.ref.WeakReference;

/**
 * This class extends {@code IntegerPropertyBase} and provides a partial
 * implementation of a {@code StyleableProperty}. The method
 * {@link StyleableProperty#getCssMetaData()} is not implemented.
 *
 * This class is used to make a {@link javafx.beans.property.IntegerProperty},
 * that would otherwise be implemented as a {@link IntegerPropertyBase},
 * styleable by CSS.
 *
 * @see javafx.beans.property.IntegerPropertyBase
 * @see CssMetaData
 * @see StyleableProperty
 * @since JavaFX 8.0
 */
public abstract class StyleableIntegerProperty
    extends IntegerPropertyBase implements StyleableProperty<Number> {

    /**
     * The constructor of the {@code StyleableIntegerProperty}.
     */
    public StyleableIntegerProperty() {
        super();
    }

    /**
     * The constructor of the {@code StyleableIntegerProperty}.
     *
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public StyleableIntegerProperty(int initialValue) {
        super(initialValue);
    }

    /** {@inheritDoc} */
    @Override
    public void applyStyle(StyleOrigin origin, Number v) {
        if (timer != null) {
            timer.stop();
        }

        // If this.origin == null, we're setting the value for the first time.
        // No transition should be started in this case.
        TransitionDefinition transition = this.origin != null && getBean() instanceof Node node ?
            NodeHelper.findTransition(node, getCssMetaData()) : null;

        if (transition != null) {
            timer = TransitionTimer.run(this, new TransitionTimerImpl(this, v, transition));
        } else {
            setValue(v);
        }

        this.origin = origin;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(ObservableValue<? extends Number> observable) {
        if (StyleableTimer.tryStop(timer)) {
            super.bind(observable);
            origin = StyleOrigin.USER;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void set(int v) {
        super.set(v);

        if (StyleableTimer.tryStop(timer)) {
            origin = StyleOrigin.USER;
        }
    }

    /** {@inheritDoc} */
    @Override
    public StyleOrigin getStyleOrigin() { return origin; }

    private StyleOrigin origin = null;
    private StyleableTimer timer = null;

    private static class TransitionTimerImpl extends TransitionTimer {
        final WeakReference<StyleableIntegerProperty> wref;
        final int oldValue;
        final int newValue;

        @Override
        protected Property<?> getProperty() {
            return wref.get();
        }

        TransitionTimerImpl(StyleableIntegerProperty property, Number value, TransitionDefinition transition) {
            super(transition);
            this.wref = new WeakReference<>(property);
            this.oldValue = property.get();
            this.newValue = value != null ? value.intValue() : 0;
        }

        @Override
        protected void onUpdate(double progress) {
            StyleableIntegerProperty property = wref.get();
            if (property != null) {
                property.set(progress < 1 ? oldValue + (int)((newValue - oldValue) * progress) : newValue);
            } else {
                super.stop();
            }
        }

        @Override
        public void stop() {
            super.stop();

            StyleableIntegerProperty property = wref.get();
            if (property != null) {
                property.timer = null;
            }
        }
    }

}
