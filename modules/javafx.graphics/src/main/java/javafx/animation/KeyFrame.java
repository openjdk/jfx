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

package javafx.animation;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javafx.beans.NamedArg;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

/**
 * Defines target values at a specified point in time for a set of variables
 * that are interpolated along a {@link Timeline}.
 * <p>
 * The developer controls the interpolation of a set of variables for the
 * interval between successive key frames by providing a target value and an
 * {@link Interpolator} associated with each variable. The variables are
 * interpolated such that they will reach their target value at the specified
 * time. An {@code onFinished} function is invoked on each {@code KeyFrame} if one
 * is provided. A {@code KeyFrame} can optionally have a {@code name}, which
 * will result in a cuepoint that is automatically added to the {@code Timeline}.
 *
 * @see Timeline
 * @see KeyValue
 * @see Interpolator
 *
 * @since JavaFX 2.0
 */
public final class KeyFrame {

    private static final EventHandler<ActionEvent> DEFAULT_ON_FINISHED = null;
    private static final String DEFAULT_NAME = null;

    /**
     * Returns the time offset of this {@code KeyFrame}.
     *
     * The returned {@link javafx.util.Duration} defines the time offset within
     * a single cycle of a {@link Timeline} at which the {@link KeyValue
     * KeyValues} will be set and at which the {@code onFinished} function
     * variable will be called.
     * <p>
     * The {@code time} of a {@code KeyFrame} has to be greater than or equal to
     * {@link javafx.util.Duration#ZERO} and it cannot be
     * {@link javafx.util.Duration#UNKNOWN}.
     *
     * Note: While the unit of {@code time} is a millisecond, the granularity
     * depends on the underlying operating system and will in general be larger.
     * For example animations on desktop systems usually run with a maximum of
     * 60fps which gives a granularity of ~17 ms.
     * @return the time offset
     */
    public Duration getTime() {
        return time;
    }
    private final Duration time;

    /**
     * Returns an immutable {@code Set} of {@link KeyValue} instances.
     *
     * A {@code KeyValue} defines a target and the desired value that should be
     * interpolated at the specified time of this {@code KeyFrame}.
     * @return an immutable {@code Set} of {@link KeyValue} instances
     */
    public Set<KeyValue> getValues() {
        return values;
    }
    private final Set<KeyValue> values;

    /**
     * Returns the {@code onFinished} event handler of this {@code KeyFrame}.
     *
     * The {@code onFinished} event handler is a function that is called when
     * the elapsed time on a cycle passes the specified time of this
     * {@code KeyFrame}. The {@code onFinished} function variable will be called
     * if the elapsed time passes the indicated value, even if it never equaled
     * the time value exactly.
     * @return the {@code onFinished} event handler
     */
    public EventHandler<ActionEvent> getOnFinished() {
        return onFinished;
    }
    private final EventHandler<ActionEvent> onFinished;

    /**
     * Returns the {@code name} of this {@code KeyFrame}.
     *
     * If a named {@code KeyFrame} is added to a {@link Timeline}, a cuepoint
     * with the {@code name} and the {@code time} of the {@code KeyFrame} will
     * be added automatically. If the {@code KeyFrame} is removed, the cuepoint
     * will also be removed.
     * @return the {@code name}
     */
    public String getName() {
        return name;
    }
    private final String name;

    /**
     * Constructor of {@code KeyFrame}
     * <p>
     * If a passed in {@code KeyValue} is {@code null} or a duplicate, it will
     * be ignored.
     *
     * @param time
     *            the {@code time}
     * @param name
     *            the {@code name}
     * @param onFinished
     *            the {@code onFinished}
     * @param values
     *            a {@link javafx.collections.ObservableList} of
     *            {@link KeyValue} instances
     * @throws NullPointerException
     *             if {@code time} is null
     * @throws IllegalArgumentException
     *             if {@code time} is invalid (see {@link #getTime time})
     */
    public KeyFrame(@NamedArg("time") Duration time, @NamedArg("name") String name,
            @NamedArg("onFinished") EventHandler<ActionEvent> onFinished, @NamedArg("values") Collection<KeyValue> values) {
        if (time == null) {
            throw new NullPointerException("The time has to be specified");
        }
        if (time.lessThan(Duration.ZERO) || time.equals(Duration.UNKNOWN)) {
            throw new IllegalArgumentException("The time is invalid.");
        }
        this.time = time;
        this.name = name;
        if (values != null) {
            final Set<KeyValue> set = new CopyOnWriteArraySet<>(values);
            set.remove(null);
            this.values = (set.size() == 0) ? Collections.<KeyValue> emptySet()
                    : (set.size() == 1) ? Collections.<KeyValue> singleton(set
                            .iterator().next()) : Collections
                            .unmodifiableSet(set);
        } else {
            this.values = Collections.<KeyValue> emptySet();
        }
        this.onFinished = onFinished;
    }

    /**
     * Constructor of {@code KeyFrame}
     * <p>
     * If a passed in {@code KeyValue} is {@code null} or a duplicate, it will
     * be ignored.
     *
     * @param time
     *            the {@code time}
     * @param name
     *            the {@code name}
     * @param onFinished
     *            the {@code onFinished}
     * @param values
     *            the {@link KeyValue} instances
     * @throws NullPointerException
     *             if {@code time} is null
     * @throws IllegalArgumentException
     *             if {@code time} is invalid (see {@link #getTime time})
     */
    public KeyFrame(@NamedArg("time") Duration time, @NamedArg("name") String name,
            @NamedArg("onFinished") EventHandler<ActionEvent> onFinished, @NamedArg("values") KeyValue... values) {
        if (time == null) {
            throw new NullPointerException("The time has to be specified");
        }
        if (time.lessThan(Duration.ZERO) || time.equals(Duration.UNKNOWN)) {
            throw new IllegalArgumentException("The time is invalid.");
        }
        this.time = time;
        this.name = name;
        if (values != null) {
            final Set<KeyValue> set = new CopyOnWriteArraySet<>();
            for (final KeyValue keyValue : values) {
                if (keyValue != null) {
                    set.add(keyValue);
                }
            }
            this.values = (set.size() == 0) ? Collections.<KeyValue> emptySet()
                    : (set.size() == 1) ? Collections.<KeyValue> singleton(set
                            .iterator().next()) : Collections
                            .unmodifiableSet(set);
        } else {
            this.values = Collections.emptySet();
        }
        this.onFinished = onFinished;
    }

    /**
     * Constructor of {@code KeyFrame}
     *
     * @param time
     *            the {@code time}
     * @param onFinished
     *            the {@code onFinished}
     * @param values
     *            the {@link KeyValue} instances
     * @throws NullPointerException
     *             if {@code time} is null
     * @throws IllegalArgumentException
     *             if {@code time} is invalid (see {@link #getTime time})
     */
    public KeyFrame(@NamedArg("time") Duration time, @NamedArg("onFinished") EventHandler<ActionEvent> onFinished,
            @NamedArg("values") KeyValue... values) {
        this(time, DEFAULT_NAME, onFinished, values);
    }

    /**
     * Constructor of {@code KeyFrame}
     *
     * @param time
     *            the {@code time}
     * @param name
     *            the {@code name}
     * @param values
     *            the {@link KeyValue} instances
     * @throws NullPointerException
     *             if {@code time} is null
     * @throws IllegalArgumentException
     *             if {@code time} is invalid (see {@link #getTime time})
     */
    public KeyFrame(@NamedArg("time") Duration time, @NamedArg("name") String name, @NamedArg("values") KeyValue... values) {
        this(time, name, DEFAULT_ON_FINISHED, values);
    }

    /**
     * Constructor of {@code KeyFrame}
     *
     * @param time
     *            the {@code time}
     * @param values
     *            the {@link KeyValue} instances
     * @throws NullPointerException
     *             if {@code time} is null
     * @throws IllegalArgumentException
     *             if {@code time} is invalid (see {@link #getTime time})
     */
    public KeyFrame(@NamedArg("time") Duration time, @NamedArg("values") KeyValue... values) {
        this(time, DEFAULT_NAME, DEFAULT_ON_FINISHED, values);
    }

    /**
     * Returns a string representation of this {@code KeyFrame} object.
     * @return the string representation
     */
    @Override
    public String toString() {
        return "KeyFrame [time=" + time + ", values=" + values
                + ", onFinished=" + onFinished + ", name=" + name + "]";
    }

    /**
     * Returns a hash code for this {@code KeyFrame} object.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        assert (time != null) && (values != null);
        final int prime = 31;
        int result = 1;
        result = prime * result + time.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((onFinished == null) ? 0 : onFinished.hashCode());
        result = prime * result + values.hashCode();
        return result;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two {@code KeyFrames} are considered equal, if their {@link #getTime()
     * time}, {@link #getOnFinished onFinished}, and {@link #getValues() values}
     * are equal.
     * @return {@code true} if this is the same as obj, otherwise {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof KeyFrame) {
            final KeyFrame kf = (KeyFrame) obj;
            assert (time != null) && (values != null) && (kf.time != null)
                    && (kf.values != null);
            return time.equals(kf.time)
                    && ((name == null) ? kf.name == null : name.equals(kf.name))
                    && ((onFinished == null) ? kf.onFinished == null
                            : onFinished.equals(kf.onFinished))
                    && values.equals(kf.values);
        }
        return false;
    }

}
