/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.text;

import com.sun.javafx.text.PrismCaretInfo;

/**
 * Provides the information associated with the caret.
 *
 * @since 24
 */
public sealed abstract class CaretInfo permits PrismCaretInfo {
    /**
     * Constructor for subclasses to call.
     */
    protected CaretInfo() {
    }

    /**
     * Returns the text line spacing at the caret position.
     *
     * @return the line spacing
     */
    public abstract double lineSpacing();

    /**
     * Returns the number of lines representing the caret.
     *
     * @return the number of parts representing the caret
     */
    public abstract int getLineCount();

    /**
     * Returns the geometry of the line at the specified index.
     * <p>
     * The geometry is encoded in an array of [x, ymin, ymax] values which
     * represent a line drawn from (x, ymin) to (x, ymax).
     *
     * @param index the line index
     * @return the array of [x, ymin, ymax] values
     */
    public abstract double[] getLineAt(int index);
}
