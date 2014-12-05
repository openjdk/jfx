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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Paint;

/**
 *
 */
public class ResizingGuideRenderer {
    
    private static final String NID_RESIZING_GUIDE = "resizingGuide"; //NOI18N

    private final Group guideGroup = new Group();
    private final Map<AbstractSegment, ResizingGuideChrome> chromeMap = new HashMap<>();
    private final Set<ResizingGuideChrome> reusableChromes = new HashSet<>();
    private final Paint chromeColor;
    private final double chromeSideLength;
    
    public ResizingGuideRenderer(Paint chromeColor, double chromeSideLength) {
        this.chromeColor = chromeColor;
        this.chromeSideLength = chromeSideLength;
        guideGroup.setMouseTransparent(true);
    }
    
    public void setSegments(List<AbstractSegment> segments) {
        assert segments != null;
        assert guideGroup.getScene() != null;
        
        final Set<AbstractSegment> currentSegments = chromeMap.keySet();
        
        final Set<AbstractSegment> newSegments = new HashSet<>();
        newSegments.addAll(segments);
        newSegments.removeAll(currentSegments);
        
        final Set<AbstractSegment> obsoleteSegments = new HashSet<>();
        obsoleteSegments.addAll(currentSegments);
        obsoleteSegments.removeAll(segments);
        
        for (AbstractSegment s : obsoleteSegments) {
            final ResizingGuideChrome chrome = chromeMap.get(s);
            assert chrome != null;
            reusableChromes.add(chrome);
            chromeMap.remove(s);
            chrome.setVisible(false);
        }
        
        for (AbstractSegment s : newSegments) {
            final ResizingGuideChrome chrome;
            if (reusableChromes.isEmpty()) {
                chrome = new ResizingGuideChrome(chromeSideLength);
                chrome.setId(NID_RESIZING_GUIDE);
                chrome.setStroke(chromeColor);
                guideGroup.getChildren().add(chrome);
            } else {
                chrome = reusableChromes.iterator().next();
                reusableChromes.remove(chrome);
                chrome.setVisible(true);
            }
            final Point2D p1 = guideGroup.sceneToLocal(s.getX1(), s.getY1(), true /* rootScene */);
            final Point2D p2 = guideGroup.sceneToLocal(s.getX2(), s.getY2(), true /* rootScene */);
            chrome.setup(p1.getX(), p1.getY(), p2.getX(), p2.getY());
            chromeMap.put(s, chrome);
        }
        
//        assert chromeMap.keySet().size() == segments.size()
//                : "chromeMap.keySet().size()=" + chromeMap.keySet().size()
//                + ", segments.size()=" + segments.size();
//        assert chromeMap.size() + reusableChromes.size() == guideGroup.getChildren().size();
    }
    
    
    public Group getGuideGroup() {
        return guideGroup;
    }
}
