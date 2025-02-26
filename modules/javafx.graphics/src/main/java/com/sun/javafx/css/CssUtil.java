/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.css;

import java.util.List;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.Node;

/**
 * Utility methods for dealing with CSS.
 */
public final class CssUtil {

    private CssUtil() {
    }

    /**
     * Utility method which combines {@code CssMetaData} items in one immutable list.
     * <p>
     * The intended usage is to combine the parent and the child {@code CssMetaData} for
     * the purposes of {@code getClassCssMetaData()} method, see for example {@link Node#getClassCssMetaData()}.
     * <p>
     * Example:
     * <pre>{@code
     * private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES = CssMetaData.combine(
     *      <Parent>.getClassCssMetaData(),
     *      STYLEABLE1,
     *      STYLEABLE2
     *  );
     * }</pre>
     * This method returns an instance of a {@code List} that implements
     * {@link java.util.RandomAccess} interface.
     *
     * @param inheritedFromParent the {@code CssMetaData} items inherited from parent, must not be null
     * @param items the additional items
     * @return the immutable list containing all of the items
     */
    // NOTE: this should be a public utility, see https://bugs.openjdk.org/browse/JDK-8320796
    public static List<CssMetaData<? extends Styleable, ?>> combine(
        List<CssMetaData<? extends Styleable, ?>> inheritedFromParent,
        CssMetaData<? extends Styleable, ?>... items)
    {
        CssMetaData[] combined = new CssMetaData[inheritedFromParent.size() + items.length];
        inheritedFromParent.toArray(combined);
        System.arraycopy(items, 0, combined, inheritedFromParent.size(), items.length);
        return List.of(combined);
    }
}
