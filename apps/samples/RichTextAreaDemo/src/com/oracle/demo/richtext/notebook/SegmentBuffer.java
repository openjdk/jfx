/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.notebook;

import java.io.IOException;
import java.util.ArrayList;
import jfx.incubator.scene.control.richtext.model.StyledInput;
import jfx.incubator.scene.control.richtext.model.StyledOutput;
import jfx.incubator.scene.control.richtext.model.StyledSegment;

/**
 * In-memory buffer which stored {@code StyledSegment}s with associated output and input streams,
 * for the use in export/import or transfer operations.
 * This class and its streams are not thread safe.
 *
 * @author Andy Goryachev
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
        public void consume(StyledSegment s) throws IOException {
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
