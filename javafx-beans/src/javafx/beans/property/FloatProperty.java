/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableFloatValue;
import com.sun.javafx.binding.Logging;

/**
 * This class defines a {@link Property} wrapping a {@code float} value.
 * 
 * The value of a {@code FloatProperty} can be get and set with {@link #get()},
 * {@link #getValue()}, {@link #set(float)}, and {@link #setValue(Number)}.
 * 
 * A property can be bound and unbound unidirectional with
 * {@link #bind(ObservableValue)} and {@link #unbind()}. Bidirectional bindings
 * can be created and removed with {@link #bindBidirectional(Property)} and
 * {@link #unbindBidirectional(Property)}.
 * 
 * The context of a {@code FloatProperty} can be read with {@link #getBean()}
 * and {@link #getName()}.
 * 
 * @see javafx.beans.value.ObservableFloatValue
 * @see javafx.beans.value.WritableFloatValue
 * @see ReadOnlyFloatProperty
 * @see Property
 * 
 */
public abstract class FloatProperty extends ReadOnlyFloatProperty implements
        Property<Number>, WritableFloatValue {

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Number v) {
        if (v == null) {
            Logging.getLogger().info("Attempt to set float property to null, using default value instead.", new NullPointerException());
            set(0.0f);
        } else {
            set(v.floatValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bindBidirectional(Property<Number> other) {
        Bindings.bindBidirectional(this, other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unbindBidirectional(Property<Number> other) {
        Bindings.unbindBidirectional(this, other);
    }

    /**
     * Returns a string representation of this {@code FloatProperty} object.
     * @return a string representation of this {@code FloatProperty} object.
     */ 
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder(
                "FloatProperty [");
        if (bean != null) {
            result.append("bean: ").append(bean).append(", ");
        }
        if ((name != null) && (!name.equals(""))) {
            result.append("name: ").append(name).append(", ");
        }
        result.append("value: ").append(get()).append("]");
        return result.toString();
    }
}
