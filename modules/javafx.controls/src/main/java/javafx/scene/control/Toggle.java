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

package javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableMap;

/**
 * Represents a control that can be toggled between selected and non-selected
 * states. In addition, a Toggle can be assigned a
 * <code>{@link ToggleGroup}</code>, which manages all assigned Toggles such
 * that only a single Toggle within the <code>{@link ToggleGroup}</code> may be
 * selected at any one time.
 * @since JavaFX 2.0
 */
public interface Toggle {

    /**
     * Returns The {@link ToggleGroup} to which this {@code Toggle} belongs.
     * @return The {@link ToggleGroup} to which this {@code Toggle} belongs.
     */
    ToggleGroup getToggleGroup();

    /**
     * Sets the {@link ToggleGroup} to which this {@code Toggle} belongs.
     * @param toggleGroup The new {@link ToggleGroup}.
     */
    void setToggleGroup(ToggleGroup toggleGroup);

    /**
     * The {@link ToggleGroup} to which this {@code Toggle} belongs.
     * @return the toggle group property
     */
    ObjectProperty<ToggleGroup> toggleGroupProperty();

    /**
     * Indicates whether this {@code Toggle} is selected.
     * @return {@code true} if this {@code Toggle} is selected.
     */
    boolean isSelected();

    /**
     * Sets this {@code Toggle} as selected or unselected.
     *
     * @param selected {@code true} to make this {@code Toggle} selected.
     */
    void setSelected(boolean selected);

    /**
     * The selected state for this {@code Toggle}.
     * @return the selected property
     */
    BooleanProperty selectedProperty();

    /**
     * Returns a previously set Object property, or null if no such property
     * has been set using the {@code Node.setUserData(java.lang.Object)} method.
     *
     * @return The Object that was previously set, or null if no property
     *          has been set or if null was set.
     */
    Object getUserData();

    /**
     * Convenience method for setting a single Object property that can be
     * retrieved at a later date. This is functionally equivalent to calling
     * the getProperties().put(Object key, Object value) method. This can later
     * be retrieved by calling {@code Node.getUserData()}.
     *
     * @param value The value to be stored - this can later be retrieved by calling
     *          {@code Node.getUserData()}.
     */
    void setUserData(Object value);

    /**
     * Returns an observable map of properties on this toggle for use primarily
     * by application developers.
     *
     * @return An observable map of properties on this toggle for use primarily
     * by application developers
     */
    ObservableMap<Object, Object> getProperties();
}
