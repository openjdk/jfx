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

import javafx.beans.binding.ObjectExpression;

/**
 * Super class for all readonly properties wrapping an arbitrary {@code Object}.
 * 
 * @see javafx.beans.value.ObservableObjectValue
 * @see javafx.beans.binding.ObjectExpression
 * @see ReadOnlyProperty
 * 
 * 
 * @param <T>
 *            the type of the wrapped {@code Object}
 */
public abstract class ReadOnlyObjectProperty<T> extends ObjectExpression<T>
        implements ReadOnlyProperty<T> {

    /**
     * The constructor of {@code ReadOnlyObjectProperty}.
     */
    public ReadOnlyObjectProperty() {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        final Object bean1 = getBean();
        final String name1 = getName();
        if ((bean1 == null) || (name1 == null) || name1.equals("")) {
            return false;
        }
        if (obj instanceof ReadOnlyObjectProperty) {
            final ReadOnlyObjectProperty<?> other = (ReadOnlyObjectProperty<?>) obj;
            final Object bean2 = other.getBean();
            final String name2 = other.getName();
            return (bean1 == bean2) && name1.equals(name2);
        }
        return false;
    }

    /**
     * Returns a hash code for this {@code ReadOnlyObjectProperty} object.
     * @return a hash code for this {@code ReadOnlyObjectProperty} object.
     */ 
    @Override
    public int hashCode() {
        final Object bean = getBean();
        final String name = getName();
        if ((bean == null) && ((name == null) || name.equals(""))) {
            return super.hashCode();
        } else {
            int result = 17;
            result = 31 * result + ((bean == null)? 0 : bean.hashCode());
            result = 31 * result + ((name == null)? 0 : name.hashCode());
            return result;
        }
    }

    /**
     * Returns a string representation of this {@code ReadOnlyObjectProperty} object.
     * @return a string representation of this {@code ReadOnlyObjectProperty} object.
     */ 
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder(
                "ReadOnlyObjectProperty [");
        if (bean != null) {
            result.append("bean: ").append(bean).append(", ");
        }
        if ((name != null) && !name.equals("")) {
            result.append("name: ").append(name).append(", ");
        }
        result.append("value: ").append(get()).append("]");
        return result.toString();
    }

}
