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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.AbstractDecoration;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

/**
 *
 */
public class TabOutline {
    
    //
    //           2              3
    //           +--------------+
    //     0     |              |                 5
    //     +-----+              +-----------------+
    //     |     1              4                 |
    //     |                                      |
    //     |                                      |
    //     |                                      |
    //     +--------------------------------------+
    //     7                                      6
    //
        

    private final Path ringPath = new Path();
    private final MoveTo moveTo0 = new MoveTo();
    private final LineTo lineTo1 = new LineTo();
    private final LineTo lineTo2 = new LineTo();
    private final LineTo lineTo3 = new LineTo();
    private final LineTo lineTo4 = new LineTo();
    private final LineTo lineTo5 = new LineTo();
    private final LineTo lineTo6 = new LineTo();
    private final LineTo lineTo7 = new LineTo();

    private final TabPaneDesignInfoX tabPaneDesignInfo
            = new TabPaneDesignInfoX();
    
    private final Tab tab;
    
    public TabOutline(Tab tab) {
        assert tab != null;
        
        this.tab = tab;
        
        final List<PathElement> ringElements = ringPath.getElements();
        ringElements.add(moveTo0);
        ringElements.add(lineTo1);
        ringElements.add(lineTo2);
        ringElements.add(lineTo3);
        ringElements.add(lineTo3);
        ringElements.add(lineTo4);
        ringElements.add(lineTo5);
        ringElements.add(lineTo6);
        ringElements.add(lineTo7);
        ringElements.add(new ClosePath());
    }
    
    public Path getRingPath() {
        return ringPath;
    }
    
    public void layout(AbstractDecoration<?> hostDecoration) {
        final TabPane tabPane = tab.getTabPane();
        final Bounds headerBounds = tabPaneDesignInfo.computeTabBounds(tabPane, tab);
        final Bounds contentBounds = tabPaneDesignInfo.computeContentAreaBounds(tabPane);

        switch(tabPane.getSide()) {
            default:
            case TOP:
                layoutForTopSide(headerBounds, contentBounds, hostDecoration);
                break;
            case BOTTOM:
                layoutForBottomSide(headerBounds, contentBounds, hostDecoration);
                break;
            case LEFT:
                layoutForLeftSide(headerBounds, contentBounds, hostDecoration);
                break;
            case RIGHT:
                layoutForRightSide(headerBounds, contentBounds, hostDecoration);
                break;
        }
    }

    
    /*
     * Private
     */
    
    private void layoutForTopSide(Bounds headerBounds, Bounds contentBounds,
            AbstractDecoration<?> hd) {
        
        //
        //     x0    x1             x2                x3
        //
        // y0        +--------------+
        //           |    header    |
        //           +--------------+
        // y1  +--------------------------------------+
        //     |                                      |
        //     |                                      |
        //     |               content                |
        //     |                                      |
        // y2  +--------------------------------------+
        //
        
        final double x0 = contentBounds.getMinX();
        final double x1 = headerBounds.getMinX();
        final double x2 = headerBounds.getMaxX();
        final double x3 = contentBounds.getMaxX();
        final double y0 = headerBounds.getMinY();
        final double y1 = contentBounds.getMinY();
        final double y2 = contentBounds.getMaxY();
        
        final boolean snapToPixel = true;
        final Point2D p0 = hd.sceneGraphObjectToDecoration(x0, y1, snapToPixel);
        final Point2D p1 = hd.sceneGraphObjectToDecoration(x1, y1, snapToPixel);
        final Point2D p2 = hd.sceneGraphObjectToDecoration(x1, y0, snapToPixel);
        final Point2D p3 = hd.sceneGraphObjectToDecoration(x2, y0, snapToPixel);
        final Point2D p4 = hd.sceneGraphObjectToDecoration(x2, y1, snapToPixel);
        final Point2D p5 = hd.sceneGraphObjectToDecoration(x3, y1, snapToPixel);
        final Point2D p6 = hd.sceneGraphObjectToDecoration(x3, y2, snapToPixel);
        final Point2D p7 = hd.sceneGraphObjectToDecoration(x0, y2, snapToPixel);
        
        moveTo0.setX(p0.getX());
        moveTo0.setY(p0.getY());
        lineTo1.setX(p1.getX());
        lineTo1.setY(p1.getY());
        lineTo2.setX(p2.getX());
        lineTo2.setY(p2.getY());
        lineTo3.setX(p3.getX());
        lineTo3.setY(p3.getY());
        lineTo4.setX(p4.getX());
        lineTo4.setY(p4.getY());
        lineTo5.setX(p5.getX());
        lineTo5.setY(p5.getY());
        lineTo6.setX(p6.getX());
        lineTo6.setY(p6.getY());
        lineTo7.setX(p7.getX());
        lineTo7.setY(p7.getY());
    }
    
    
    private void layoutForBottomSide(Bounds headerBounds, Bounds contentBounds,
            AbstractDecoration<?> hd) {
        
        //
        //     x0    x1             x2                x3
        //
        // y0  +--------------------------------------+
        //     |                                      |
        //     |                                      |
        //     |               content                |
        //     |                                      |
        // y1  +--------------------------------------+
        //           +--------------+
        //           |    header    |
        // y2        +--------------+
        //
        
        final double x0 = contentBounds.getMinX();
        final double x1 = headerBounds.getMinX();
        final double x2 = headerBounds.getMaxX();
        final double x3 = contentBounds.getMaxX();
        final double y0 = contentBounds.getMinY();
        final double y1 = contentBounds.getMaxY();
        final double y2 = headerBounds.getMaxY();
        
        final boolean snapToPixel = true;
        final Point2D p0 = hd.sceneGraphObjectToDecoration(x0, y0, snapToPixel);
        final Point2D p1 = hd.sceneGraphObjectToDecoration(x3, y0, snapToPixel);
        final Point2D p2 = hd.sceneGraphObjectToDecoration(x3, y1, snapToPixel);
        final Point2D p3 = hd.sceneGraphObjectToDecoration(x2, y1, snapToPixel);
        final Point2D p4 = hd.sceneGraphObjectToDecoration(x2, y2, snapToPixel);
        final Point2D p5 = hd.sceneGraphObjectToDecoration(x1, y2, snapToPixel);
        final Point2D p6 = hd.sceneGraphObjectToDecoration(x1, y1, snapToPixel);
        final Point2D p7 = hd.sceneGraphObjectToDecoration(x0, y1, snapToPixel);
        
        moveTo0.setX(p0.getX());
        moveTo0.setY(p0.getY());
        lineTo1.setX(p1.getX());
        lineTo1.setY(p1.getY());
        lineTo2.setX(p2.getX());
        lineTo2.setY(p2.getY());
        lineTo3.setX(p3.getX());
        lineTo3.setY(p3.getY());
        lineTo4.setX(p4.getX());
        lineTo4.setY(p4.getY());
        lineTo5.setX(p5.getX());
        lineTo5.setY(p5.getY());
        lineTo6.setX(p6.getX());
        lineTo6.setY(p6.getY());
        lineTo7.setX(p7.getX());
        lineTo7.setY(p7.getY());
    }
    
    
    private void layoutForLeftSide(Bounds headerBounds, Bounds contentBounds,
            AbstractDecoration<?> hd) {
        
        //
        //     x0   x1                          x2
        // 
        // y0       +---------------------------+
        //          |                           |
        // y1  +--+ |                           |
        //     |  | |                           |
        //     |h | |                           |
        //     |  | |          content          |
        //     |  | |                           |
        // y2  +--+ |                           |
        //          |                           |
        //          |                           |
        //          |                           |
        //          |                           |
        // y3       +---------------------------+
        //     
        //
        
        final double x0 = headerBounds.getMinX();
        final double x1 = contentBounds.getMinX();
        final double x2 = contentBounds.getMaxX();
        final double y0 = contentBounds.getMinY();
        final double y1 = headerBounds.getMinY();
        final double y2 = headerBounds.getMaxY();
        final double y3 = contentBounds.getMaxY();
        
        final boolean snapToPixel = true;
        final Point2D p0 = hd.sceneGraphObjectToDecoration(x0, y1, snapToPixel);
        final Point2D p1 = hd.sceneGraphObjectToDecoration(x1, y1, snapToPixel);
        final Point2D p2 = hd.sceneGraphObjectToDecoration(x1, y0, snapToPixel);
        final Point2D p3 = hd.sceneGraphObjectToDecoration(x2, y0, snapToPixel);
        final Point2D p4 = hd.sceneGraphObjectToDecoration(x2, y3, snapToPixel);
        final Point2D p5 = hd.sceneGraphObjectToDecoration(x1, y3, snapToPixel);
        final Point2D p6 = hd.sceneGraphObjectToDecoration(x1, y2, snapToPixel);
        final Point2D p7 = hd.sceneGraphObjectToDecoration(x0, y2, snapToPixel);
        
        moveTo0.setX(p0.getX());
        moveTo0.setY(p0.getY());
        lineTo1.setX(p1.getX());
        lineTo1.setY(p1.getY());
        lineTo2.setX(p2.getX());
        lineTo2.setY(p2.getY());
        lineTo3.setX(p3.getX());
        lineTo3.setY(p3.getY());
        lineTo4.setX(p4.getX());
        lineTo4.setY(p4.getY());
        lineTo5.setX(p5.getX());
        lineTo5.setY(p5.getY());
        lineTo6.setX(p6.getX());
        lineTo6.setY(p6.getY());
        lineTo7.setX(p7.getX());
        lineTo7.setY(p7.getY());
    }
    
    
    private void layoutForRightSide(Bounds headerBounds, Bounds contentBounds,
            AbstractDecoration<?> hd) {
        
        //
        //        x0                          x1   x2
        // 
        // y0     +---------------------------+
        //        |                           |
        // y1     |                           | +--+
        //        |                           | |  |
        //        |                           | |h |
        //        |          content          | |  |
        //        |                           | |  |
        // y2     |                           | +--+
        //        |                           |
        //        |                           |
        //        |                           |
        //        |                           |
        // y3     +---------------------------+
        //     
        //
        
        final double x0 = contentBounds.getMinX();
        final double x1 = contentBounds.getMaxX();
        final double x2 = headerBounds.getMaxX();
        final double y0 = contentBounds.getMinY();
        final double y1 = headerBounds.getMinY();
        final double y2 = headerBounds.getMaxY();
        final double y3 = contentBounds.getMaxY();
        
        final boolean snapToPixel = true;
        final Point2D p0 = hd.sceneGraphObjectToDecoration(x0, y0, snapToPixel);
        final Point2D p1 = hd.sceneGraphObjectToDecoration(x1, y0, snapToPixel);
        final Point2D p2 = hd.sceneGraphObjectToDecoration(x1, y1, snapToPixel);
        final Point2D p3 = hd.sceneGraphObjectToDecoration(x2, y1, snapToPixel);
        final Point2D p4 = hd.sceneGraphObjectToDecoration(x2, y2, snapToPixel);
        final Point2D p5 = hd.sceneGraphObjectToDecoration(x1, y2, snapToPixel);
        final Point2D p6 = hd.sceneGraphObjectToDecoration(x1, y3, snapToPixel);
        final Point2D p7 = hd.sceneGraphObjectToDecoration(x0, y3, snapToPixel);
        
        moveTo0.setX(p0.getX());
        moveTo0.setY(p0.getY());
        lineTo1.setX(p1.getX());
        lineTo1.setY(p1.getY());
        lineTo2.setX(p2.getX());
        lineTo2.setY(p2.getY());
        lineTo3.setX(p3.getX());
        lineTo3.setY(p3.getY());
        lineTo4.setX(p4.getX());
        lineTo4.setY(p4.getY());
        lineTo5.setX(p5.getX());
        lineTo5.setY(p5.getY());
        lineTo6.setX(p6.getX());
        lineTo6.setY(p6.getY());
        lineTo7.setX(p7.getX());
        lineTo7.setY(p7.getY());
    }
    
}
