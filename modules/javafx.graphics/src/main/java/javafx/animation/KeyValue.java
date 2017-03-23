/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

import com.sun.javafx.animation.KeyValueHelper;
import com.sun.javafx.animation.KeyValueType;
import javafx.beans.NamedArg;
import javafx.beans.value.WritableBooleanValue;
import javafx.beans.value.WritableDoubleValue;
import javafx.beans.value.WritableFloatValue;
import javafx.beans.value.WritableIntegerValue;
import javafx.beans.value.WritableLongValue;
import javafx.beans.value.WritableNumberValue;
import javafx.beans.value.WritableValue;

/**
 * Defines a key value to be interpolated for a particular interval along the
 * animation. A {@link KeyFrame}, which defines a specific point on a timeline,
 * can hold multiple {@code KeyValues}. {@code KeyValue} is an immutable class.
 * <p>
 * A {@code KeyValue} is defined by a target, which is an implementation of
 * {@link javafx.beans.value.WritableValue}, an end value and an
 * {@link Interpolator}.
 * <p>
 * Most interpolators define the interpolation between two {@code KeyFrames}.
 * (The only exception are tangent-interpolators.)
 * The {@code KeyValue} of the second {@code KeyFrame} (in forward
 * direction) specifies the interpolator to be used in the interval.
 * <p>
 * Tangent-interpolators define the interpolation to the left and to the right of
 * a {@code KeyFrame} (see {@link  Interpolator#TANGENT(javafx.util.Duration, double, javafx.util.Duration, double)
 * Interpolator.TANGENT}).
 * <p>
 * By default, {@link Interpolator#LINEAR} is used in the interval.
 *
 * @see Timeline
 * @see KeyFrame
 * @see Interpolator
 *
 * @since JavaFX 2.0
 */
public final class KeyValue {

    private static final Interpolator DEFAULT_INTERPOLATOR = Interpolator.LINEAR;

    static {
        KeyValueHelper.setKeyValueAccessor(new KeyValueHelper.KeyValueAccessor() {
            @Override public KeyValueType getType(KeyValue keyValue) {
                return keyValue.getType();
            }
        });
    }

    KeyValueType getType() {
        return type;
    }

    private final KeyValueType type;

    /**
     * Returns the target of this {@code KeyValue}
     *
     * @return the target
     */
    public WritableValue<?> getTarget() {
        return target;
    }

    private final WritableValue<?> target;

    /**
     * Returns the end value of this {@code KeyValue}
     *
     * @return the end value
     */
    public Object getEndValue() {
        return endValue;
    }

    private final Object endValue;

    /**
     * {@link Interpolator} to be used for calculating the key value along the
     * particular interval. By default, {@link Interpolator#LINEAR} is used.
     * @return the interpolator to be used for calculating the key value along
     * the particular interval
     */
    public Interpolator getInterpolator() {
        return interpolator;
    }

    private final Interpolator interpolator;

    /**
     * Creates a {@code KeyValue}.
     *
     * @param <T> the type of the {@code KeyValue}
     * @param target
     *            the target
     * @param endValue
     *            the end value
     * @param interpolator
     *            the {@link Interpolator}
     * @throws NullPointerException
     *             if {@code target} or {@code interpolator} are {@code null}
     */
    public <T> KeyValue(@NamedArg("target") WritableValue<T> target, @NamedArg("endValue") T endValue,
            @NamedArg("interpolator") Interpolator interpolator) {
        if (target == null) {
            throw new NullPointerException("Target needs to be specified");
        }
        if (interpolator == null) {
            throw new NullPointerException("Interpolator needs to be specified");
        }

        this.target = target;
        this.endValue = endValue;
        this.interpolator = interpolator;
        this.type = (target instanceof WritableNumberValue) ? (target instanceof WritableDoubleValue) ? KeyValueType.DOUBLE
                : (target instanceof WritableIntegerValue) ? KeyValueType.INTEGER
                        : (target instanceof WritableFloatValue) ? KeyValueType.FLOAT
                                : (target instanceof WritableLongValue) ? KeyValueType.LONG
                                        : KeyValueType.OBJECT
                : (target instanceof WritableBooleanValue) ? KeyValueType.BOOLEAN
                        : KeyValueType.OBJECT;
    }

    /**
     * Creates a {@code KeyValue} that uses {@link Interpolator#LINEAR}.
     *
     * @param <T> the type of the {@code KeyValue}
     * @param target
     *            the target
     * @param endValue
     *            the end value
     * @throws NullPointerException
     *             if {@code target} or {@code interpolator} are {@code null}
     */
    public <T> KeyValue(@NamedArg("target") WritableValue<T> target, @NamedArg("endValue") T endValue) {
        this(target, endValue, DEFAULT_INTERPOLATOR);
    }

    /**
     * Returns a string representation of this {@code KeyValue} object.
     * @return the string representation
     */
    @Override
    public String toString() {
        return "KeyValue [target=" + target + ", endValue=" + endValue
                + ", interpolator=" + interpolator + "]";
    }

    /**
     * Returns a hash code for this {@code KeyValue} object.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        assert (target != null) && (interpolator != null);
        final int prime = 31;
        int result = 1;
        result = prime * result + target.hashCode();
        result = prime * result
                + ((endValue == null) ? 0 : endValue.hashCode());
        result = prime * result + interpolator.hashCode();
        return result;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two {@code KeyValues} are considered equal, if their {@link #getTarget()
     * target}, {@link #getEndValue() endValue}, and {@link #getInterpolator()
     * interpolator} are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof KeyValue) {
            final KeyValue keyValue = (KeyValue) obj;
            assert (target != null) && (interpolator != null)
                    && (keyValue.target != null)
                    && (keyValue.interpolator != null);
            return target.equals(keyValue.target)
                    && ((endValue == null) ? (keyValue.endValue == null)
                            : endValue.equals(keyValue.endValue))
                    && interpolator.equals(keyValue.interpolator);
        }
        return false;
    }

}
