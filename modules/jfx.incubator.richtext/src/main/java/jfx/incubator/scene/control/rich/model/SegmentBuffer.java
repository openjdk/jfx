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

package jfx.incubator.scene.control.rich.model;

import java.io.IOException;
import java.util.ArrayList;

/**
 * In-memory buffer which stored {@code StyledSegment}s with associated output and input streams,
 * for the use in export/import or transfer operations.
 * This class and its streams are not thread safe.
 */
public class SegmentBuffer {
    private ArrayList<StyledSegment> segments;
    private Output output;

    /**
     * Creates the buffer with the specified initial capacity.
     * @param initialCapacity the initial capacity
     */
    public SegmentBuffer(int initialCapacity) {
        segments = new ArrayList<>(initialCapacity);
    }

    /**
     * Creates the buffer.
     */
    public SegmentBuffer() {
        this(256);
    }

    /**
     * Returns the singleton {@code StyledOutput} instance associated with this buffer.
     * @return the StyledOutput instance
     */
    public StyledOutput getStyledOutput() {
        if(output == null) {
            output = new Output();
        }
        return output;
    }

    /**
     * Returns an array of {@code StyledSegment}s accumulated so far.
     * @return the array of {@code StyledSegment}s
     */
    public StyledSegment[] getSegments() {
        return segments.toArray(new StyledSegment[segments.size()]);
    }

    /**
     * Returns a new instance of {@code StyledInput} which contains the segments accumulated so far.
     * @return the instance of {@code StyledInput}
     */
    public StyledInput getStyledInput() {
        return new Input(getSegments());
    }

    private class Output implements StyledOutput {
        Output() {
        }

        @Override
        public void append(StyledSegment s) throws IOException {
            segments.add(s);
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
            // possibly create a boolean flag to force an IOException in append when closed
        }
    }

    private static class Input implements StyledInput {
        private final StyledSegment[] segments;
        private int index;

        Input(StyledSegment[] segments) {
            this.segments = segments;
        }

        @Override
        public StyledSegment nextSegment() {
            if (index < segments.length) {
                return segments[index++];
            }
            return null;
        }

        @Override
        public void close() throws IOException {
        }
    }
}
