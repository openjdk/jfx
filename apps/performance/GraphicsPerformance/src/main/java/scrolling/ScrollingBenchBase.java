/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates.
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

package scrolling;

import javafx.scene.Node;
import nodecount.BenchBase;
import nodecount.BenchTest;

/**
 * A type of grid-based test which scrolls in one dimension. The scrolling parent
 * might be a Group with a translation, or a ScrollPane.
 */
public abstract class ScrollingBenchBase<T extends Node> extends BenchBase<T> {

    @Override protected BenchTest[] createTests() {
        int[][] sizes = new int[][] {
                {30, 6},
                {50, 6},
                {100, 6},
                {120, 6},
                {180, 6},
                {200, 6},
        };
        BenchTest[] tests = new BenchTest[3 * 6];
        int sizeIndex = 0;
        for (int i=0; i<tests.length; i+=3) {
            int rows = sizes[sizeIndex][0];
            int cols = sizes[sizeIndex][1];
            tests[i] = new TranslatingGridTest(this, rows, cols); // Causing all rects to be drawn? Grows with each iteration!
            tests[i+1] = new ScrollPaneGridTest(this, rows, cols); // Very few rects drawn
            tests[i+2] = new PixelAlignedTranslatingGridTest(this, rows, cols); // causes all rects to be drawn at first and then only very few thereafter
            sizeIndex++;
        }
        return tests;
    }
}
