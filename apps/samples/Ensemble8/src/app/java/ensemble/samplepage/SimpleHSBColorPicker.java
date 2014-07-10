/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package ensemble.samplepage;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;


/**
 * A simple inline HSB color picker
 */
public class SimpleHSBColorPicker extends Region {

    private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
    private Rectangle hsbRect = new Rectangle(200, 30, buildHueBar());
    private Rectangle lightRect = new Rectangle(200, 30,
            new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                               new Stop(0, Color.WHITE),
                               new Stop(0.5, Color.rgb(255, 255, 255, 0)),
                               new Stop(0.501, Color.rgb(0, 0, 0, 0)),
                               new Stop(1, Color.BLACK)));

    public SimpleHSBColorPicker() {
        getChildren().addAll(hsbRect, lightRect);
        lightRect.setStroke(Color.GRAY);
        lightRect.setStrokeType(StrokeType.OUTSIDE);
        EventHandler<MouseEvent> ml = (MouseEvent e) -> {
            double w = getWidth();
            double h = getHeight();
            double x = Math.min(w, Math.max(0, e.getX()));
            double y = Math.min(h, Math.max(0, e.getY()));
            double hue = (360 / w) * x;
            double vert = (1 / h) * y;
            double sat;
            double bright;
            if (vert < 0.5) {
                bright = 1;
                sat = vert * 2;
            } else {
                bright = sat = 1 - 2 * (vert - 0.5);
            }
            // convert back to color
            Color c = Color.hsb((int) hue, sat, bright);
            color.set(c);
            e.consume();
        };
        lightRect.setOnMouseDragged(ml);
        lightRect.setOnMouseClicked(ml);
    }

    @Override
    protected double computeMinWidth(double height) {
        return 200;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 30;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 200;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 30;
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        hsbRect.setX(1);
        hsbRect.setY(1);
        hsbRect.setWidth(w - 2);
        hsbRect.setHeight(h - 2);
        lightRect.setX(1);
        lightRect.setY(1);
        lightRect.setWidth(w - 2);
        lightRect.setHeight(h - 2);
    }

    public ObjectProperty<Color> getColor() {
        return color;
    }

    private LinearGradient buildHueBar() {
        double offset;
        Stop[] stops = new Stop[255];
        for (int y = 0; y < 255; y++) {
            offset = (double) (1.0 / 255) * y;
            int h = (int) ((y / 255.0) * 360);
            stops[y] = new Stop(offset, Color.hsb(h, 1.0, 1.0));
        }
        return new LinearGradient(0f, 0f, 1f, 0f, true, CycleMethod.NO_CYCLE, stops);
    }
}
