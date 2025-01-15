/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext;

import jfx.incubator.scene.control.richtext.skin.CellContext;

/**
 * This functional interface defines a style attribute handler.
 * <p>
 * This interface is needed when extending the RichTextArea with support for other style attributes.
 * Applications should not normally use this interface.
 * <p>
 * The purpose of this handler is to apply changes to the {@code CellContext} based on the value
 * of the corresponding attribute.
 *
 * @param <C> the actual type of RichTextArea control
 * @param <T> the attribute value type
 *
 * @since 24
 */
@FunctionalInterface
public interface StyleAttributeHandler<C extends RichTextArea, T> {
    /**
     * Executes the attribute handler for the given control, cell context,
     * and the attribute value.
     *
     * @param control the control
     * @param cx the cell context
     * @param value the attribute value
     */
    public void apply(C control, CellContext cx, T value);
}