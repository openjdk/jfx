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

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class Pump {

    Group pumpGroup;

    Paint pumpColor;
    Paint fan1, fan2;

    Timeline timeline;

    BooleanProperty pumpOn;
    DoubleProperty pumpRate; // a % of max
    IntegerProperty pumpSpinDuration; // millis for a single spin

    Model model;

    final KeyValue kv1;
    final KeyValue kv2;
    KeyFrame kf;

    Pump(Model model) {
        this.model = model;
        pumpColor = Color.RED;
        fan1 = Color.LIGHTBLUE;
        fan2 = Color.BLUE;

        Group fan = new Group();

        Rectangle a1 = new Rectangle(80 - 40, 105 - 40, 80, 80);
        a1.setFill(fan1);

        Rectangle a2 = new Rectangle(80, 105, 40, 40);
        a2.setFill(fan2);

        Rectangle a3 = new Rectangle(80 - 40, 105 - 40, 40, 40);
        a3.setFill(fan2);

        fan.getChildren().addAll(a1, a2, a3);
        Rotate spin = new Rotate(0, 80, 105);
        fan.getTransforms().add(spin);

        Ellipse hole = new Ellipse(80, 105, 30, 30);
        hole.setFill(Color.BLACK);

        Path base = new Path();
        base.getElements().addAll(
                new MoveTo(0, 0),
                new LineTo(40, 60),
                new LineTo(130, 60),
                new LineTo(160, 0),
                new ClosePath());
        base.setFill(Color.RED);
        base.setStroke(Color.BLACK);
        base.setStrokeWidth(1);

        Path outline = new Path();
        outline.getElements().addAll(
                new MoveTo(150, 180),
                new LineTo(150, 200),
                new LineTo(106, 200),
                new LineTo(106, 180),
                new ArcTo(80, 80, 0, 150, 140, true, true),
                new ClosePath());
        outline.setFill(Color.BLACK);

        Shape s = Path.subtract(outline, hole);
        s.setFill(Color.RED);
        s.setStroke(Color.BLACK);
        s.setStrokeWidth(1);

        pumpGroup = new Group();
        pumpGroup.getChildren().addAll(fan, base, s);
        Rotate flip = new Rotate(180, 80, 105);
        pumpGroup.getTransforms().add(flip);

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(false);
        kv1 = new KeyValue(spin.angleProperty(), 0);
        kv2 = new KeyValue(spin.angleProperty(), 360);
        kf = new KeyFrame(Duration.millis(3000), kv1, kv2);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }


    Group getGroup() {
        return pumpGroup;
    }

    public final BooleanProperty getPumpActiveProperty() {
        if (pumpOn == null) {
            pumpOn = new BooleanPropertyBase() {

                @Override
                public Object getBean() {
                    return Pump.this;
                }

                @Override
                public String getName() {
                    return "pumpOn";
                }

                @Override
                public void invalidated() {
                    if (get()) {
                        if (timeline != null) {
                            timeline.play();
                        }
                    } else {
                        if (timeline != null) {
                            timeline.pause();
                        }
                    }
                }
            };
        }
        return pumpOn;

    }

    public void setPumpActive(boolean on) {
        getPumpActiveProperty().set(on);
    }

    public boolean getPumpActive() {
        return getPumpActiveProperty().get();
    }


    public final DoubleProperty getPumpRatePercentProperty() {
        if (pumpRate == null) {
            pumpRate = new DoublePropertyBase(1.) { 
                @Override
                public Object getBean() {
                    return Pump.this;
                }

                @Override
                public String getName() {
                    return "pumpRate";
                }

                @Override
                public void invalidated() {
                    double value = get();
                    if (value < 0.0) {
                        set(0.0);
                    } 
                    if (value > 1.0) {
                        set(1.0);
                    } 
                    timeline.setRate(value);
                }
            };
        }
        return pumpRate;

    }

    public void setPumpRatePercent(double percent) {
        getPumpRatePercentProperty().set(percent);
    }

    public double getPumpRatePercent() {
        return getPumpRatePercentProperty().get();
    }

}
