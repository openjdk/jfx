/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
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

package com.oracle.demo.richtext.headings;

import java.util.Optional;

public record ParagraphRange(int start, int end) implements Comparable<ParagraphRange> {

    public ParagraphRange {
        if (start < 0) {
            throw new IllegalArgumentException("start must be non-negative: " + start);
        }
        if (end < start) {
            throw new IllegalArgumentException("end must not precede start: " + start + ", " + end);
        }
    }

    public int length() {
        return end - start;
    }

    public boolean isEmpty() {
        return length() == 0;
    }

    public boolean contains(int modelIndex) {
        return this.start <= modelIndex && modelIndex < this.end;
    }

    @Override
    public int compareTo(ParagraphRange other) {
        int result = Integer.compare(this.start, other.start);
        if (result == 0) {
            result = Integer.compare(this.end, other.end);
        }
        return result;
    }

    public static Optional<ParagraphRange> ofNextParagraph(ParagraphRange range, int max) {
        if (range.start + 1 >= Math.min(range.end, max)) {
            return Optional.empty();
        }
        return Optional.of(new ParagraphRange(range.start + 1, Math.min(range.end, max)));
    }
}