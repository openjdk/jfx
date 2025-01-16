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

package jfx.incubator.scene.control.richtext.model;

import java.io.Closeable;
import java.io.IOException;
import com.sun.jfx.incubator.scene.control.richtext.StringStyledInput;

/**
 * This interface represents a source of styled text segments for the purposes of
 * pasting, importing, or loading from an input stream.
 *
 * @since 24
 */
public interface StyledInput extends Closeable {
    /**
     * Returns the next segment, or null if no more segments.
     * @return the next segment, or null if no more segments
     */
    public abstract StyledSegment nextSegment();

    /** An empty StyledInput. */
    public static final StyledInput EMPTY = new StyledInput() {
        @Override
        public StyledSegment nextSegment() {
            return null;
        }

        @Override
        public void close() throws IOException {
        }
    };

    /**
     * Creates a plain text styled input with the specified style.
     *
     * @param text the source text
     * @param attrs the source style attributes
     * @return the StyledInput instance
     */
    public static StyledInput of(String text, StyleAttributeMap attrs) {
        return new StringStyledInput(text, attrs);
    }

    /**
     * Creates a plain text styled input with {@link StyleAttributeMap#EMPTY}.
     *
     * @param text the source text
     * @return the StyledInput instance
     */
    public static StyledInput of(String text) {
        return new StringStyledInput(text, StyleAttributeMap.EMPTY);
    }
}
