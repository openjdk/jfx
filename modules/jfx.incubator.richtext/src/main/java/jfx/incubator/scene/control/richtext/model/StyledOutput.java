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
import com.sun.jfx.incubator.scene.control.richtext.StringBuilderStyledOutput;

/**
 * Class represents a consumer of styled text segments for the purposes of
 * exporting, copying, or saving to an output stream.
 *
 * @since 24
 */
public interface StyledOutput extends Closeable {
    /**
     * Consumes the next styled segment.
     *
     * @param segment the segment to output
     * @throws IOException when an I/O error occurs
     */
    public void consume(StyledSegment segment) throws IOException;

    /**
     * Flushes this output stream.
     * @throws IOException when an I/O error occurs
     */
    public void flush() throws IOException;

    /**
     * Creates an instance of a plain text StyledOutput.
     * @return the instance of a plain text StyledOutput
     */
    public static StyledOutput forPlainText() {
        return new StringBuilderStyledOutput();
    }
}
