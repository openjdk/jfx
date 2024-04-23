/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.text;

import com.sun.javafx.geom.RectBounds;

/**
 * Represents a sequence of characters all using the same font, or
 * an embedded object if no font is supplied.
 * <p>
 * A text span can contain line breaks if the text should span multiple
 * lines.
 */
public interface TextSpan {
    /**
     * The text for the span, can be empty but not null.
     */
    public String getText();

    /**
     * The font for the span, if null the span is handled as embedded object.
     */
    public Object getFont();

    /**
     * The bounds for embedded object, only used the font returns null.
     * The text for a embedded object should be a single char ("\uFFFC" is
     * recommended).
     */
    public RectBounds getBounds();
}
