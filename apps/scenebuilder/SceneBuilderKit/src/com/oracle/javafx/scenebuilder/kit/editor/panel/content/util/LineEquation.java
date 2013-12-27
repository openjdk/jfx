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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.util;

import javafx.geometry.Point2D;

/**
 *
 * 
 */
public class LineEquation {
    
    private final double x0;
    private final double y0;
    private final double x1;
    private final double y1;
    private final double distance;
    
    public LineEquation(double x0, double y0, double x1, double y1) {
        assert ! ((x0 == x1) && (y0 == y1));
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        final double dx = x1 - x0;
        final double dy = y1 - y0;
        this.distance = Math.sqrt(dx * dx + dy * dy);
    }
    
    public LineEquation(Point2D p0, Point2D p1) {
        this(p0.getX(), p0.getY(), p1.getX(), p1.getY());
    }
    
    public double getDistance() {
        return distance;
    }
    
    public boolean isValid() {
        return distance > 0.0;
    }
    
    /**
     * Returns the (x,y) of a target point given its offset along this line.
     *  
     * @param offset offset of the target point
     * @return the coordinates of the target point
     */
    public Point2D pointAtOffset(double offset) {
        /*
         *  - offset=0           matches p0
         *  - offset=+D          matches p1
         *  - offset < 0         matches line points before p0
         *  - 0 < offset < D     matches line points between p0 and p1
         *  - D < offset         matches line points after p1
         * 
         * where D is the distance between p0 and p1
         */
        return pointAtP(offsetToP(offset));
    }
    
    /**
     * Projects p on this line and returns the offset of the projected point.
     * 
     * @param x target point x
     * @param y target point y
     * @return the offset of p projection on this line.
     */
    public double offsetAtPoint(double x, double y) {
        /*
         * offset is the dot product of:
         *      - v1 : vector ((x0, y0), (x, y))
         *      - v2 : normalized vector ((x0, y0), (x1, y1))
         */
        
        final double dx1 = x - x0;
        final double dy1 = y - y0;
        final double dx2 = (x1 - x0) / distance;
        final double dy2 = (y1 - y0) / distance;
        final double result = dx1 * dx2 + dy1 * dy2;
        
        return result;
    }
    
    /**
     * Returns the (x,y) of a target point given its parametric position.
     * 
     * @param p parametric position of the target point
     * @return the coordinates of the target point
     */
    public Point2D pointAtP(double p) {
        final double x, y;
        
        /*
         *  - p=0           matches p0
         *  - p=1           matches p1
         *  - p < 0         matches line points before p0
         *  - 0 < p < 1     matches line points between p0 and p1
         *  - 1 < p         matches line points after p1
         */
        
        assert isValid();
        
        x = x0 + p * (x1 - x0);
        y = y0 + p * (y1 - y0);
       
        return new Point2D(x, y);
    }
    
    /**
     * Convert an offset to a parametric position
     * 
     * @param offset an offset along this line
     * @return the matching parametric position
     */
    public double offsetToP(double offset) {
        assert isValid();
        return offset / distance;
    }
    
    /**
     * Convert a parametric position to an offset
     * 
     * @param p a parametric position
     * @return the matching offset
     */
    public double pToOffset(double p) {
        assert isValid();
        return p * distance;
    }
}
