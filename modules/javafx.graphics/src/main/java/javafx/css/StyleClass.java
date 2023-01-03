/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javafx.css;

/**
 * A class that contains {@code StyleClass} information.
 * @since 9
 */
public final class StyleClass {

    /**
     * Constructs a {@code StyleClass} object.
     * @param styleClassName name of the style class
     * @param index style class index
     */
    public StyleClass(String styleClassName, int index) {
        this.styleClassName = styleClassName;
        this.index = index;
    }

    /**
     * Returns the name of {@code StyleClass}.
     * @return the name of {@code StyleClass}
     */
    public String getStyleClassName() {
        return styleClassName;
    }

    /**
     * Returns the name of {@code StyleClass}.
     * @return the name of {@code StyleClass}
     */
    @Override public String toString() {
        return styleClassName;
    }

    /**
     * Returns the index of this {@code StyleClass} in the styleClasses list.
     * @return index
     */
    public int getIndex() {
       return index;
    }

    private final String styleClassName;

    // index of this StyleClass in styleClasses list.
    private final int index;

}
