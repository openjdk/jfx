/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property.adapter;

import javafx.beans.property.ReadOnlyProperty;

/**
 * {@code JavaBeanProperty} is the super interface of all adapters between
 * readonly Java Bean properties and JavaFX properties.
 *
 * @param <T> The type of the wrapped property
 * @since JavaFX 2.1
 */
public interface ReadOnlyJavaBeanProperty<T> extends ReadOnlyProperty<T> {
    /**
     * This method can be called to notify the adapter of a change of the Java
     * Bean value, if the Java Bean property is not bound (i.e. it does not
     * support PropertyChangeListeners).
     */
    void fireValueChangedEvent();

    /**
     * Signals to the JavaFX property that it will not be used anymore and any
     * references can be removed. A call of this method usually results in the
     * property stopping to observe the Java Bean property by unregistering its
     * listener(s).
     */
    void dispose();
}
