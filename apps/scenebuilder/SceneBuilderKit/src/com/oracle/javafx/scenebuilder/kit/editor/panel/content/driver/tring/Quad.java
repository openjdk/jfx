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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring;

import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

/**
 *
 * 
 */
public class Quad {
    
    private final boolean clockwise;
    private final MoveTo moveTo0 = new MoveTo();
    private final LineTo lineTo1 = new LineTo();
    private final LineTo lineTo2 = new LineTo();
    private final LineTo lineTo3 = new LineTo();
    
    public Quad(boolean clockwise) {
        this.clockwise = clockwise;
    }
    
    public Quad() {
        this(true /* clockwise */);
    }

    public void addToPath(Path path) {
        assert path != null;
        
        final List<PathElement> ringElements = path.getElements();
        if (clockwise) {
            ringElements.add(moveTo0);
            ringElements.add(lineTo1);
            ringElements.add(lineTo2);
            ringElements.add(lineTo3);
        } else {
            ringElements.add(moveTo0);
            ringElements.add(lineTo3);
            ringElements.add(lineTo2);
            ringElements.add(lineTo1);
        }
        ringElements.add(new ClosePath());
    }
    
    public void removeFromPath(Path path) {
        assert path != null;
        assert path.getElements().contains(moveTo0);
        
        final List<PathElement> ringElements = path.getElements();
        ringElements.remove(moveTo0);
        ringElements.remove(lineTo1);
        ringElements.remove(lineTo2);
        ringElements.remove(lineTo3);
    }

    public double getX0() {
        return moveTo0.getX();
    }
    
    public void setX0(double x) {
        moveTo0.setX(x);
    }

    public double getY0() {
        return moveTo0.getY();
    }

    public void setY0(double y) {
        moveTo0.setY(y);
    }

    public double getX1() {
        return lineTo1.getX();
    }

    public void setX1(double x) {
        lineTo1.setX(x);
    }

    public double getY1() {
        return lineTo1.getY();
    }

    public void setY1(double y) {
        lineTo1.setY(y);
    }

    public double getX2() {
        return lineTo2.getX();
    }

    public void setX2(double x) {
        lineTo2.setX(x);
    }

    public double getY2() {
        return lineTo2.getY();
    }

    public void setY2(double y) {
        lineTo2.setY(y);
    }

    public double getX3() {
        return lineTo3.getX();
    }

    public void setX3(double x) {
        lineTo3.setX(x);
    }

    public double getY3() {
        return lineTo3.getY();
    }

    public void setY3(double y) {
        lineTo3.setY(y);
    }

    public void setBounds(Bounds bounds) {
        moveTo0.setX(bounds.getMinX());
        moveTo0.setY(bounds.getMinY());
        lineTo1.setX(bounds.getMaxX());
        lineTo1.setY(bounds.getMinY());
        lineTo2.setX(bounds.getMaxX());
        lineTo2.setY(bounds.getMaxY());
        lineTo3.setX(bounds.getMinX());
        lineTo3.setY(bounds.getMaxY());
    }
    
    public void insets(Insets insets) {
        moveTo0.setX(moveTo0.getX() + insets.getLeft());
        moveTo0.setY(moveTo0.getY() + insets.getTop());
        lineTo1.setX(lineTo1.getX() - insets.getRight());
        lineTo1.setY(lineTo1.getY() + insets.getTop());
        lineTo2.setX(lineTo2.getX() - insets.getRight());
        lineTo2.setY(lineTo2.getY() - insets.getBottom());
        lineTo3.setX(lineTo3.getX() + insets.getLeft());
        lineTo3.setY(lineTo3.getY() - insets.getBottom());
    }
    
    public void insets(Insets insets, double minInset) {
        assert minInset >= 0.0;
        moveTo0.setX(moveTo0.getX() + Math.max(minInset, insets.getLeft()));
        moveTo0.setY(moveTo0.getY() + Math.max(minInset, insets.getTop()));
        lineTo1.setX(lineTo1.getX() - Math.max(minInset, insets.getRight()));
        lineTo1.setY(lineTo1.getY() + Math.max(minInset, insets.getTop()));
        lineTo2.setX(lineTo2.getX() - Math.max(minInset, insets.getRight()));
        lineTo2.setY(lineTo2.getY() - Math.max(minInset, insets.getBottom()));
        lineTo3.setX(lineTo3.getX() + Math.max(minInset, insets.getLeft()));
        lineTo3.setY(lineTo3.getY() - Math.max(minInset, insets.getBottom()));
    }
}
