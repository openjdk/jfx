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

import com.sun.javafx.css.TransitionTimer;
import com.sun.javafx.scene.NodeHelper;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * This class extends {@code DoublePropertyBase} and provides a partial
 * implementation of a {@code StyleableProperty}. The method
 * {@link StyleableProperty#getCssMetaData()} is not implemented.
 *
 * This class is used to make a {@link javafx.beans.property.DoubleProperty},
 * that would otherwise be implemented as a {@link DoublePropertyBase},
 * styleable by CSS.
 *
 * @see javafx.beans.property.DoublePropertyBase
 * @see CssMetaData
 * @see StyleableProperty
 * @since JavaFX 8.0
 */
public abstract class StyleableDoubleProperty
    extends DoublePropertyBase implements StyleableProperty<Number> {

    /**
     * The constructor of the {@code StyleableDoubleProperty}.
     */
    public StyleableDoubleProperty() {
        super();
    }

    /**
     * The constructor of the {@code StyleableDoubleProperty}.
     *
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     */
    public StyleableDoubleProperty(double initialValue) {
        super(initialValue);
    }

    /** {@inheritDoc} */
    @Override
    public void applyStyle(StyleOrigin origin, Number v) {
        if (timer != null) {
            timer.stop();
        }

        // If this.origin == null, we're setting the initial value; no transition should be started in this case.
        TransitionDefinition transition = this.origin != null && getBean() instanceof Node node ?
            NodeHelper.findTransition(node, getCssMetaData()) : null;

        if (transition != null) {
            timer = new TransitionTimer(transition) {
                final double oldValue = get();
                final double newValue = v != null ? v.doubleValue() : 0;

                @Override
                protected void onUpdate(double progress) {
                    set(progress < 1 ? oldValue + (newValue - oldValue) * progress : newValue);
                }

                @Override
                public void stop() {
                    super.stop();
                    timer = null;
                }
            };

            timer.start();
        } else {
            setValue(v);
        }

        this.origin = origin;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(ObservableValue<? extends Number> observable) {
        if (TransitionTimer.tryStop(timer)) {
            super.bind(observable);
            origin = StyleOrigin.USER;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void set(double v) {
        super.set(v);

        if (TransitionTimer.tryStop(timer)) {
            origin = StyleOrigin.USER;
        }
    }

    /** {@inheritDoc} */
    @Override
    public StyleOrigin getStyleOrigin() { return origin; }

    private StyleOrigin origin = null;
    private TransitionTimer timer = null;

}
