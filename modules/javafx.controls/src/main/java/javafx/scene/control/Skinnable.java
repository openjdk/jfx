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

package javafx.scene.control;

import javafx.beans.property.ObjectProperty;

/**
 * <p>
 * The Skinnable interface is implemented by the {@link Control} class,
 * and therefore is implemented by all Control implementations.
 *
 * @since JavaFX 2.0
 */
public interface Skinnable {
    /**
     * Skin is responsible for rendering this {@code Control}. From the
     * perspective of the {@code Control}, the {@code Skin} is a black box.
     * It listens and responds to changes in state in a {@code Control}.
     * <p>
     * There is a one-to-one relationship between a {@code Control} and its
     * {@code Skin}. Every {@code Skin} maintains a back reference to the
     * {@code Control}.
     * <p>
     * A skin may be null.
     *
     * @return the skin property for this control
     */
    public ObjectProperty<Skin<?>> skinProperty();

    /**
     * Sets the skin that will render this {@link Control}.
     * <p>
     * To ensure a one-to-one relationship between a {@code Control} and its
     * {@code Skin}, this method must check (for any non-null value) that
     * {@link Skin#getSkinnable()}, throwing an IllegalArgumentException
     * in the case of mismatch.
     * returns the same value as this Skinnable.
     *
     * @param value the skin value for this control
     * 
     * @throws IllegalArgumentException if {@link Skin#getSkinnable()} returns
     * value other than this Skinnable.
     */
    public void setSkin(Skin<?> value);

    /**
     * Returns the skin that renders this {@link Control}
     * @return the skin for this control
     */
    public Skin<?> getSkin();
}
