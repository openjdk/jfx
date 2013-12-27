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

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

/**
 *
 * 
 */
public enum CardinalPoint {
    N, NE, E, SE, S, SW, W, NW;
    
    
    public CardinalPoint getOpposite() {
        final CardinalPoint result;
        
        switch(this) {
            case N:
                result = S;
                break;
            case NE:
                result = SW;
                break;
            case E:
                result = W;
                break;
            case SE:
                result = NW;
                break;
            case S:
                result = N;
                break;
            case SW:
                result = NE;
                break;
            case W:
                result = E;
                break;
            case NW:
                result = SE;
                break;
            default:
                assert false : "unexpected cardinal point:" + this;
                result = N;
                break;
        }
        
        return result;
    }
    
    
    public Point2D getPosition(Bounds bounds) {
        final double x, y;
        
        switch(this) {
            case N:
                x = (bounds.getMinX() + bounds.getMaxX()) / 2.0;
                y = bounds.getMinY();
                break;
            case NE:
                x = bounds.getMaxX();
                y = bounds.getMinY();
                break;
            case E:
                x = bounds.getMaxX();
                y = (bounds.getMinY() + bounds.getMaxY()) / 2.0;
                break;
            case SE:
                x = bounds.getMaxX();
                y = bounds.getMaxY();
                break;
            case S:
                x = (bounds.getMinX() + bounds.getMaxX()) / 2.0;
                y = bounds.getMaxY();
                break;
            case SW:
                x = bounds.getMinX();
                y = bounds.getMaxY();
                break;
            case W:
                x = bounds.getMinX();
                y = (bounds.getMinY() + bounds.getMaxY()) / 2.0;
                break;
            case NW:
                x = bounds.getMinX();
                y = bounds.getMinY();
                break;
            default:
                assert false : "unexpected cardinal point:" + this;
                x = bounds.getMinX();
                y = bounds.getMinY();
                break;
        }
        
        return new Point2D(x, y);
    }
    
    
    public Bounds getResizedBounds(Bounds currentBounds, double dx, double dy) {
        /*
         * 
         *        NW                N                NE
         *         *----------------*----------------*
         *         |                                 |
         *         |                                 |
         *         |                                 |
         *       W *                                 * E
         *         |                                 |
         *         |                                 |
         *         |                                 |
         *         *----------------*----------------*
         *        SW                S                SE
         * 
         * 
         */

        // x axis
        final double minX = currentBounds.getMinX();
        final double maxX = currentBounds.getMaxX();
        final double newMinX, newMaxX;
        switch(this) {
            case NW:
            case  W:
            case SW:
                newMinX = Math.min(minX + dx, maxX);
                newMaxX = maxX;
                break;
            case NE:
            case  E:
            case SE:
                newMinX = minX;
                newMaxX = Math.max(maxX + dx, minX);
                break;
            case N:
            case S:
                newMinX = minX;
                newMaxX = maxX;
                break;
            default:
                // Emergency code
                assert false : "unexpected value=" + this;
                newMinX = minX;
                newMaxX = maxX;
                break;
        }
        
        // y axis
        final double minY = currentBounds.getMinY();
        final double maxY = currentBounds.getMaxY();
        final double newMinY, newMaxY;
        switch(this) {
            case NW:
            case N:
            case NE:
                newMinY = Math.min(minY + dy, maxY);
                newMaxY = maxY;
                break;
            case SE:
            case S:
            case SW:
                newMinY = minY;
                newMaxY = Math.max(maxY + dy, minY);
                break;
            case  E:
            case  W:
                newMinY = minY;
                newMaxY = maxY;
                break;
            default:
                // Emergency code
                assert false : "unexpected value=" + this;
                newMinY = minY;
                newMaxY = maxY;
                break;
        }

        return new BoundingBox(newMinX, newMinY, newMaxX - newMinX, newMaxY - newMinY);
    }
    
    
    public Point2D clampVector(double dx, double dy) {
        final double resultDX, resultDY;
        
        switch(this) {
            case N:
            case S:
                resultDX = 0.0;
                resultDY = dy;
                break;
            case E:
            case W:
                resultDX = dx;
                resultDY = 0.0;
                break;
            default:
                resultDX = dx;
                resultDY = dy;
                break;
        }
        
        return new Point2D(resultDX, resultDY);
    }
    
    
    public Bounds snapBounds(Bounds bounds, double ratio) {
        /*
         * 
         *        NW                N                NE
         *         *----------------*----------------*
         *         |                                 |
         *         |                                 |
         *         |                                 |
         *       W *                                 * E
         *         |                                 |
         *         |                                 |
         *         |                                 |
         *         *----------------*----------------*
         *        SW                S                SE
         * 
         * 
         */
        
        final double minX = bounds.getMinX();
        final double minY = bounds.getMinY();
        final double maxX = bounds.getMaxX();
        final double maxY = bounds.getMaxY();
        final double snapWidth = bounds.getHeight() / ratio;
        final double snapDX = snapWidth - bounds.getWidth();
        final double snapHeight = bounds.getWidth() * ratio;
        final double snapDY = snapHeight - bounds.getHeight();
        final double newMinX, newMinY, newMaxX, newMaxY;

        // x axis
        switch(this) {
            case N:
            case S:
                newMinX = minX - snapDX / 2.0;
                newMaxX = maxX + snapDX / 2.0;
                break;
            case E:
            case W:
                newMinX = minX;
                newMaxX = maxX;
                break;
            case NW:
            case SW:
                if (Math.abs(snapDX) >= Math.abs(snapDY)) {
                    newMinX = minX - snapDX;
                } else {
                    newMinX = minX;
                }
                newMaxX = maxX;
                break;
            case NE:
            case SE:
                newMinX = minX;
                if (Math.abs(snapDX) >= Math.abs(snapDY)) {
                    newMaxX = maxX + snapDX;
                } else {
                    newMaxX = maxX;
                }
                break;
            default:
                // Emergency code
                assert false : "Unexpected " + this;
                newMinX = minX;
                newMaxX = maxX;
                break;
        }

        // y axis
        switch(this) {
            case N:
            case S:
                newMinY = minY;
                newMaxY = maxY;
                break;
            case E:
            case W:
                newMinY = minY - snapDY / 2.0;
                newMaxY = maxY + snapDY / 2.0;
                break;
            case NW:
            case NE:
                if (Math.abs(snapDY) > Math.abs(snapDX)) {
                    newMinY = minY - snapDY;
                } else {
                    newMinY = minY;
                }
                newMaxY = maxY;
                break;
            case SW:
            case SE:
                newMinY = minY;
                if (Math.abs(snapDY) > Math.abs(snapDX)) {
                    newMaxY = maxY + snapDY;
                } else {
                    newMaxY = maxY;
                }
                break;
            default:
                // Emergency code
                assert false : "Unexpected " + this;
                newMinY = minY;
                newMaxY = maxY;
                break;
        }

        return new BoundingBox(newMinX, newMinY, newMaxX - newMinX, newMaxY - newMinY);
    }
}
