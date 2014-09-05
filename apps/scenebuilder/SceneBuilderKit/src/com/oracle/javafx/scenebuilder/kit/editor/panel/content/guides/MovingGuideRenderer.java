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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

/**
 *
 */
public class MovingGuideRenderer {
    
    private static final String NID_MOVING_GUIDE = "movingGuide"; //NOI18N

    private final Group guideGroup = new Group();
    private final Map<AbstractSegment, Line> chromeMap = new HashMap<>();
    private final Set<Line> reusableChromes = new HashSet<>();
    private final Paint chromeColor;
    private final Bounds scopeInScene;
    
    public MovingGuideRenderer(Paint chromeColor, Bounds scopeInScene) {
        this.chromeColor = chromeColor;
        this.scopeInScene = scopeInScene;
        guideGroup.setMouseTransparent(true);
    }
    
    public void setLines(List<? extends AbstractSegment> lines1, List<? extends AbstractSegment> lines2) {
        assert lines1 != null;
        assert lines2 != null;
        assert guideGroup.getScene() != null;
        
        final Set<AbstractSegment> currentLines = chromeMap.keySet();
        
        final Set<AbstractSegment> newLines = new HashSet<>();
        newLines.addAll(lines1);
        newLines.addAll(lines2);
        newLines.removeAll(currentLines);
        
        final Set<AbstractSegment> obsoleteLines = new HashSet<>();
        obsoleteLines.addAll(currentLines);
        obsoleteLines.removeAll(lines1);
        obsoleteLines.removeAll(lines2);
        
        for (AbstractSegment s : obsoleteLines) {
            final Line chrome = chromeMap.get(s);
            assert chrome != null;
            reusableChromes.add(chrome);
            chromeMap.remove(s);
            chrome.setVisible(false);
        }
        
        final Bounds scope = guideGroup.sceneToLocal(scopeInScene, true /* rootScene */);
        for (AbstractSegment s : newLines) {
            final Line chrome;
            if (reusableChromes.isEmpty()) {
                chrome = new Line();
                chrome.setId(NID_MOVING_GUIDE);
                chrome.setStroke(chromeColor);
                guideGroup.getChildren().add(chrome);
            } else {
                chrome = reusableChromes.iterator().next();
                reusableChromes.remove(chrome);
                chrome.setVisible(true);
            }
            final Point2D p1 = guideGroup.sceneToLocal(s.getX1(), s.getY1(), true /* rootScene */);
            final Point2D p2 = guideGroup.sceneToLocal(s.getX2(), s.getY2(), true /* rootScene */);
            final double startX, startY, endX, endY;
            if (s instanceof HorizontalSegment) {
                assert MathUtils.equals(p1.getY(), p2.getY());
                startX = scope.getMinX();
                startY = p1.getY();
                endX = scope.getMaxX();
                endY = p1.getY();
            } else {
                assert s instanceof VerticalSegment;
                assert MathUtils.equals(p1.getX(), p2.getX());
                startX = p1.getX();
                startY = scope.getMinY();
                endX = p1.getX();
                endY = scope.getMaxY();
            }
            chrome.setStartX(startX);
            chrome.setStartY(startY);
            chrome.setEndX(endX);
            chrome.setEndY(endY);
            assert chromeMap.containsKey(s) == false;
            chromeMap.put(s, chrome);
        }
        
//        assert chromeMap.keySet().size() == lines1.size() + lines2.size()
//                : "chromeMap.keySet().size()=" + chromeMap.keySet().size()
//                + ", lines1.size()=" + lines1.size()
//                + ", lines2.size()=" + lines2.size()
//                + ", currentLines.size()=" + currentLines.size()
//                + ", newLines.size()=" + newLines.size()
//                + ", obsoleteLines.size()=" + obsoleteLines.size();
//        assert chromeMap.size() + reusableChromes.size() == guideGroup.getChildren().size();
    }
    
    
    public Group getGuideGroup() {
        return guideGroup;
    }
}
