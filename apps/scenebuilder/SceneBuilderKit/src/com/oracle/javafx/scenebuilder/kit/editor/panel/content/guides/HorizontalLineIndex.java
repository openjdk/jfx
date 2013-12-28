/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
import javafx.geometry.Bounds;

/**
 *
 */
class HorizontalLineIndex {
    
    private static final HorizontalLineComparator comparator = new HorizontalLineComparator();
    
    private final List<HorizontalSegment> lines = new ArrayList<>();
    private boolean sorted;
    

    public void addLine(HorizontalSegment s) {
        lines.add(s);
        sorted = false;
    }
    
    public void clear() {
        lines.clear();
    }
    
    public List<HorizontalSegment> matchNorth(Bounds boundsInScene, double threshold) {
        assert boundsInScene.isEmpty() == false;
        return matchY(boundsInScene.getMinY(), threshold);
    }
    
    public List<HorizontalSegment> matchSouth(Bounds boundsInScene, double threshold) {
        assert boundsInScene.isEmpty() == false;
        return matchY(boundsInScene.getMaxY(), threshold);
    }
    
    public List<HorizontalSegment> matchCenter(Bounds boundsInScene, double threshold) {
        assert boundsInScene.isEmpty() == false;
        final double minY = boundsInScene.getMinY();
        final double maxY = boundsInScene.getMaxY();
        return matchY((minY + maxY) / 2.0, threshold);
    }
    
    
    /*
     * Private
     */
    
    private List<HorizontalSegment> matchY(double targetY, double threshold) {
        assert threshold >= 0;
        
        if (sorted == false) {
            Collections.sort(lines, comparator);
        }
        double bestDelta = Double.MAX_VALUE;
        final List<HorizontalSegment> result = new ArrayList<>();
        for (HorizontalSegment l : lines) {
            final double delta = Math.abs(l.getY1() - targetY);
            if (delta < threshold) {
                if (MathUtils.equals(delta, bestDelta)) {
                    result.add(l);
                } else if (delta < bestDelta) {
                    bestDelta = delta;
                    result.clear();
                    result.add(l);
                }
            }
        }
        
        return result;
    }
}
