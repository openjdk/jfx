/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import javafx.animation.Interpolator;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.TransitionDefinition;
import javafx.css.converter.DurationConverter;
import javafx.css.converter.StringConverter;
import javafx.util.Duration;
import java.util.List;

/**
 * A partial implementation of {@link CssMetaData} for the {@code transition} property that includes the
 * four sub-properties {@code transition-property}, {@code transition-duration}, {@code transition-delay}
 * and {@code transition-timing-function}.
 *
 * @param <S> the {@link Styleable} type
 */
public abstract class TransitionDefinitionCssMetaData<S extends Styleable>
        extends CssMetaData<S, TransitionDefinition[]> {

    public TransitionDefinitionCssMetaData() {
        super("transition", TransitionDefinitionConverter.SequenceConverter.getInstance(),
              new TransitionDefinition[0], false, createSubProperties());
    }

    private static final String[] PROPERTY_ALL = new String[] { "all" };

    private static final Duration[] DURATION_ZERO = new Duration[] { Duration.ZERO };

    private static final Interpolator[] INTERPOLATOR_LINEAR = new Interpolator[] { Interpolator.LINEAR };

    private static <S extends Styleable> List<CssMetaData<? extends Styleable, ?>> createSubProperties() {
        return List.of(
            new CssMetaData<S, String[]>("transition-property",
                    StringConverter.SequenceConverter.getInstance(), PROPERTY_ALL, false) {
                @Override
                public boolean isSettable(S styleable) {
                    return false;
                }

                @Override
                public StyleableProperty<String[]> getStyleableProperty(S styleable) {
                    return null;
                }
            },
            new CssMetaData<S, Duration[]>( "transition-duration",
                    DurationConverter.SequenceConverter.getInstance(), DURATION_ZERO, false) {
                @Override
                public boolean isSettable(S styleable) {
                    return false;
                }

                @Override
                public StyleableProperty<Duration[]> getStyleableProperty(S styleable) {
                    return null;
                }
            },
            new CssMetaData<S, Duration[]>( "transition-delay",
                    DurationConverter.SequenceConverter.getInstance(), DURATION_ZERO, false) {
                @Override
                public boolean isSettable(S styleable) {
                    return false;
                }

                @Override
                public StyleableProperty<Duration[]> getStyleableProperty(S styleable) {
                    return null;
                }
            },
            new CssMetaData<S, Interpolator[]>( "transition-timing-function",
                    InterpolatorConverter.SequenceConverter.getInstance(), INTERPOLATOR_LINEAR, false) {
                @Override
                public boolean isSettable(S styleable) {
                    return false;
                }

                @Override
                public StyleableProperty<Interpolator[]> getStyleableProperty(S styleable) {
                    return null;
                }
            }
        );
    }

}
