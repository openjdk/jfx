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

import javafx.beans.value.ObservableValue;

/**
 * Generic interface that defines the methods common to all readable properties
 * independent of their type.
 *
 *
 * @param <T>
 *            the type of the wrapped value
 * @since JavaFX 2.0
 */
public interface ReadOnlyProperty<T> extends ObservableValue<T> {

    /**
     * Returns the {@code Object} that contains this property. If this property
     * is not contained in an {@code Object}, {@code null} is returned.
     *
     * @return the containing {@code Object} or {@code null}
     */
    Object getBean();

    /**
     * Returns the name of this property. If the property does not have a name,
     * this method returns an empty {@code String}.
     *
     * @return the name or an empty {@code String}
     */
    String getName();

}
