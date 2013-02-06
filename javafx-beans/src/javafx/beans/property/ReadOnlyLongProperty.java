/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.binding.LongExpression;

/**
 * Super class for all readonly properties wrapping a {@code long}.
 * 
 * @see javafx.beans.value.ObservableLongValue
 * @see javafx.beans.binding.LongExpression
 * @see ReadOnlyProperty
 * 
 */
public abstract class ReadOnlyLongProperty extends LongExpression implements
        ReadOnlyProperty<Number> {

    /**
     * The constructor of {@code ReadOnlyLongProperty}.
     */
    public ReadOnlyLongProperty() {
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
        if (obj instanceof ReadOnlyLongProperty) {
            final ReadOnlyLongProperty other = (ReadOnlyLongProperty) obj;
            final Object bean2 = other.getBean();
            final String name2 = other.getName();
            return (bean1 == bean2) && ((name1 == null)? name2 == null : name1.equals(name2));
        }
        return false;
    }

    /**
     * Returns a hash code for this {@code ReadOnlyLongProperty} object.
     * @return a hash code for this {@code ReadOnlyLongProperty} object.
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
     * Returns a string representation of this {@code ReadOnlyLongProperty} object.
     * @return a string representation of this {@code ReadOnlyLongProperty} object.
     */ 
    @Override
    public String toString() {
        final Object bean = getBean();
        final String name = getName();
        final StringBuilder result = new StringBuilder("ReadOnlyLongProperty [");
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
