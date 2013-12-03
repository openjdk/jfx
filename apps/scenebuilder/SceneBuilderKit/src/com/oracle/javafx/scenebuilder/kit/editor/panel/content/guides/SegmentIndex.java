/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
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

package com.oracle.javafx.scenebuilder.kit.editor.panel.content.guides;

import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class SegmentIndex {
    
    private final List<AbstractSegment> segments = new ArrayList<>();
    private boolean sorted;
    

    public void addSegment(AbstractSegment s) {
        segments.add(s);
        sorted = false;
    }
    
    public void clear() {
        segments.clear();
    }
    
    public List<AbstractSegment> match(double targetLength, double threshold) {
        assert targetLength >= 0;
        assert threshold >= 0;
        
        if (sorted == false) {
            Collections.sort(segments);
        }
        double bestDelta = Double.MAX_VALUE;
        final List<AbstractSegment> result = new ArrayList<>();
        for (AbstractSegment s : segments) {
            final double delta = Math.abs(s.getLength() - targetLength);
            if (delta < threshold) {
                if (MathUtils.equals(delta, bestDelta)) {
                    result.add(s);
                } else if (delta < bestDelta) {
                    bestDelta = delta;
                    result.clear();
                    result.add(s);
                }
            }
        }
        
        return result;
    }
}
