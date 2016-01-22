/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package sensorstest;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.stage.Screen;

import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.ClosePath;

import javafx.scene.shape.Rectangle;

import javafx.scene.paint.Color;


public class AttitudeIndicator extends Group {

    public AttitudeIndicator() {
        super();

        final Rectangle2D dims = Screen.getPrimary().getVisualBounds();
        final int width = (int) dims.getWidth();
        final int height = (int) dims.getHeight();

        final int cx = width/2;
        final int cy = height/2;

        final Path mask = new Path();
        mask.getElements().addAll(
            new MoveTo(-1,0),
            new LineTo(-1, height),
            new LineTo(width,height),
            new LineTo(width,0),
            new LineTo(-1,0),
            new LineTo(width/4,   height/4),
            new LineTo(width*3/4, height/4),
            new LineTo(width*3/4, height*3/4),
            new LineTo(width/4,   height*3/4),
            new LineTo(width/4,   height/4),
            new ClosePath()
        );
        mask.setStrokeWidth(0);
        mask.setStroke(null);
        mask.setFill(Color.BLACK);

        final Rectangle rSky = new Rectangle(cx - 500, cy - 1000, 1000, 1000);
        rSky.setFill(Color.NAVY);

        final Rectangle rGround = new Rectangle(cx - 500, cy, 1000, 1000);
        rGround.setFill(Color.BROWN);

        final Rectangle bar = new Rectangle(width/4, cy-4, width/2, 8);
        bar.setFill(Color.GRAY);

        final Group g = new Group(rSky, rGround);

        getChildren().addAll(g, mask, bar);

        com.sun.javafx.ext.device.ios.sensors.IOSMotionManager.addAccelerationListener(
                new com.sun.javafx.ext.device.ios.sensors.IOSMotionManager.Listener() {
            public void handleMotion(float x, float y, float z) {
                final double roll = Math.atan(-y/x);
                final double pitch = Math.atan(-y/z);

                g.setRotate(Math.toDegrees(Math.atan(-y/x)));
                g.setTranslateY(z * cy);
            }
        });
    }
}
