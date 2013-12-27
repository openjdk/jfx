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

import java.util.List;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

/**
 *
 */
public class ResizingGuideChrome extends Path {
    
    /*
     * 
     *      moveTo0                                  moveTo1
     *         |                                        |
     *         |                                        |
     * moveTo  +----------------------------------------+ lineTo
     *         |                                        |
     *         |                                        |
     *      lineTo0                                  lineTo1
     * 
     */
    
    private final MoveTo moveTo = new MoveTo();
    private final LineTo lineTo = new LineTo();
    private final MoveTo moveTo0 = new MoveTo();
    private final LineTo lineTo0 = new LineTo();
    private final MoveTo moveTo1 = new MoveTo();
    private final LineTo lineTo1 = new LineTo();
    private final double sideLength;
    
    public ResizingGuideChrome(double sideLength) {
        this.sideLength = sideLength;
        
        final List<PathElement> elements = getElements();
        elements.add(moveTo);
        elements.add(lineTo);
        elements.add(moveTo0);
        elements.add(lineTo0);
        elements.add(moveTo1);
        elements.add(lineTo1);
    }
    
    public void setup(double x1, double y1, double x2, double y2) {
        moveTo.setX(x1);
        moveTo.setY(y1);
        lineTo.setX(x2);
        lineTo.setY(y2);
        
        if (x1 == x2) {  // Chrome is vertical
            moveTo0.setX(x1 - sideLength);
            moveTo0.setY(y1);
            lineTo0.setX(x1 + sideLength);
            lineTo0.setY(y1);
            moveTo1.setX(x2 - sideLength);
            moveTo1.setY(y2);
            lineTo1.setX(x2 + sideLength);
            lineTo1.setY(y2);
        } else if (y1 == y2) { // Chrome is horizontal
            moveTo0.setX(x1);
            moveTo0.setY(y1 - sideLength);
            lineTo0.setX(x1);
            lineTo0.setY(y1 + sideLength);
            moveTo1.setX(x2);
            moveTo1.setY(y2 - sideLength);
            lineTo1.setX(x2);
            lineTo1.setY(y2 + sideLength);
        } else {
            final double dx = x2 - x1;
            final double dy = y2 - y1;
            final double distance = Math.sqrt(dx * dx + dy * dy);
            final double leftX = -dy / distance;
            final double leftY = +dx / distance;
            
            moveTo0.setX(x1 + leftX * sideLength);
            moveTo0.setY(y1 + leftY * sideLength);
            lineTo0.setX(x1 - leftX * sideLength);
            lineTo0.setY(y1 - leftY + sideLength);
            moveTo1.setX(x2 + leftX * sideLength);
            moveTo1.setY(y2 + leftY * sideLength);
            lineTo1.setX(x2 - leftX * sideLength);
            lineTo1.setY(y2 - leftY + sideLength);
        }
    }
}
