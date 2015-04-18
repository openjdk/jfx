/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package industrial;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class Tank {

    private BooleanProperty showWaterMarks;
    private DoubleProperty fillPercentProperty;
    private DoubleProperty lowPercentProperty;
    private DoubleProperty highPercentProperty;

    private final Line highWaterLine;
    private final Line lowWaterLine;

    int width;
    int height;
    Rectangle fill;
    Group group;

    final int wallStroke = 6;

    public Tank(int width, int height) {

        Rectangle liquidClip = new Rectangle();
        liquidClip.setWidth(width);
        liquidClip.setHeight(height);
        liquidClip.setArcWidth(16);
        liquidClip.setArcHeight(16);
        liquidClip.setStrokeWidth(wallStroke);

        this.width = width;
        this.height = height;

        Rectangle tank = new Rectangle();
        tank.setWidth(width);
        tank.setHeight(height);
        tank.setStrokeWidth(wallStroke);
        tank.setStroke(Color.BLACK);
        tank.setFill(Color.web("#606060"));
        tank.setArcWidth(16);
        tank.setArcHeight(16);

        fill = new Rectangle();
        fill.setWidth(width);
        fill.setHeight(height);
        fill.setX(0);
        fill.setFill(Color.BLUE);
        fill.setClip(liquidClip);

        highWaterLine = new Line(10, 0, width - 5, 0);
        highWaterLine.setStrokeWidth(4);
        highWaterLine.setStroke(Color.GREEN);
        highWaterLine.getStrokeDashArray().setAll(25.0, 20.0, 5.0, 20.0);

        lowWaterLine = new Line(10, 0, width - 5, 0);
        lowWaterLine.setStrokeWidth(4);
        lowWaterLine.setStroke(Color.RED);
        lowWaterLine.getStrokeDashArray().setAll(25.0, 20.0, 5.0, 20.0);

        group = new Group();
        group.getChildren().addAll(tank, fill, highWaterLine, lowWaterLine);

    }

    public Group getGroup() {
        return group;
    }

    public final BooleanProperty getShowWatermarkProperty() {
        if (showWaterMarks == null) {
            showWaterMarks = new BooleanPropertyBase(true) {

                @Override
                public void invalidated() {
                    highWaterLine.visibleProperty().setValue(get());
                    lowWaterLine.visibleProperty().setValue(get());
                }

                @Override
                public Object getBean() {
                    return Tank.this;
                }

                @Override
                public String getName() {
                    return "showWaterMarkPercent";
                }

            };
        }
        return showWaterMarks;

    }

    public Line getHighWaterLine() {
        return highWaterLine;
    }

    public Line getLowWaterLine() {
        return highWaterLine;
    }

    public DoubleProperty getFillPercentageProperty() {
        if (fillPercentProperty == null) {

            fillPercentProperty = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    double v = get();
                    if (v < 0.0) {
                        v = 0.0;
                    }
                    if (v > 100.0) {
                        v = 100.0;
                    }
                    fill.setY((100.0 - v) / 100.0 * height);
                }

                @Override
                public Object getBean() {
                    return Tank.this;
                }

                @Override
                public String getName() {
                    return "fillPercentage";
                }
            };
        }
        return fillPercentProperty;
    }

    public DoubleProperty getHighWaterPercentageProperty() {
        if (highPercentProperty == null) {

            highPercentProperty = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    double v = get();
                    if (v < 0.0) {
                        v = 0.0;
                    }
                    if (v > 100.0) {
                        v = 100.0;
                    }
                    double newY = (100.0 - v) / 100.0 * height;
                    highWaterLine.startYProperty().set(newY);
                    highWaterLine.endYProperty().set(newY);
                }

                @Override
                public Object getBean() {
                    return Tank.this;
                }

                @Override
                public String getName() {
                    return "highWaterPercentage";
                }
            };
        }
        return highPercentProperty;
    }

    public DoubleProperty getLowWaterPercentageProperty() {
        if (lowPercentProperty == null) {

            lowPercentProperty = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    double v = get();
                    if (v < 0.0) {
                        v = 0.0;
                    }
                    if (v > 100.0) {
                        v = 100.0;
                    }
                    double newY = (100.0 - v) / 100.0 * height;
                    lowWaterLine.startYProperty().set(newY);
                    lowWaterLine.endYProperty().set(newY);
                }

                @Override
                public Object getBean() {
                    return Tank.this;
                }

                @Override
                public String getName() {
                    return "lowWaterPercentage";
                }
            };
        }
        return lowPercentProperty;
    }

}
