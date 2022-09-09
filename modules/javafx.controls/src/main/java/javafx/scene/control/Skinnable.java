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
     * The Skin is responsible for rendering this {@code Skinnable}. From the
     * perspective of the {@code Skinnable}, the {@code Skin} is a black box.
     * It listens and responds to changes in state in a {@code Skinnable}.
     * <p>
     * There is typically a one-to-one relationship between a {@code Skinnable} and its
     * {@code Skin}. Every {@code Skin} maintains a back reference to the
     * {@code Skinnable}.
     * <p>
     * To ensure a one-to-one relationship between a {@code Skinnable} and its
     * {@code Skin}, some implementations of {@link Skinnable#setSkin(Skin)} method will check
     * the return value of {@link Skin#getSkinnable()} against this Skinnable,
     * and throw an {@code IllegalArgumentException} if it is not the same.
     * <p>
     * A skin may be null.
     *
     * @return the skin property for this Skinnable
     */
    public ObjectProperty<Skin<?>> skinProperty();

    public void setSkin(Skin<?> value);

    public Skin<?> getSkin();
}
