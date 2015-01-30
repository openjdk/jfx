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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

public class Pipe {

    Path pipe;
    Path pipeFill;
    Path pipeAnimation;
    Group group;

    BooleanProperty fillActive;
    BooleanProperty flowActive;

    private ObjectProperty<Paint> fillPipeWall;
    private ObjectProperty<Paint> fillPipeEmpty;
    private ObjectProperty<Paint> fillPipeFull;
    private ObjectProperty<Paint> fillPipeFull2;

    private Timeline timeline;

    public Pipe(Path pipePath) {
        pipe = new Path(pipePath.getElements());
        pipe.setStrokeWidth(30);
        pipe.setStroke(fillPipeWallProperty().get());

        pipeFill = new Path(pipePath.getElements());
        pipeFill.setStrokeWidth(20);

        double pulseLength = 30;

        pipeAnimation = new Path(pipePath.getElements());
        pipeAnimation.setStrokeWidth(pipeFill.getStrokeWidth());
        pipeAnimation.getStrokeDashArray().setAll(
                pulseLength, pulseLength
        );
        pipeAnimation.strokeLineCapProperty().setValue(StrokeLineCap.BUTT);
        pipeAnimation.setStroke(fillPipeFull2Property().get());

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        final KeyValue kv1 = new KeyValue(pipeAnimation.strokeDashOffsetProperty(),0.0);
        final KeyValue kv2 = new KeyValue(pipeAnimation.strokeDashOffsetProperty(),pulseLength*2);
        final KeyFrame kf = new KeyFrame(Duration.millis(3000), kv1, kv2);
        timeline.getKeyFrames().add(kf);
        timeline.setRate(-1);

        group = new Group();
        group.getChildren().addAll(pipe, pipeFill, pipeAnimation);

        getFilledProperty().set(true);
        getFlowProperty().set(true);
    }

    private void resetPipeFill() {
        if (getFilled()) {
            pipeFill.setStroke(fillPipeFullProperty().get());
            pipeAnimation.setVisible(true);
            if(getFlowProperty().get()) {
                timeline.play();
            } else {
                timeline.pause();
            }
        } else {
            pipeFill.setStroke(fillPipeEmptyProperty().get());
            timeline.pause();
            pipeAnimation.setVisible(false);
        }
    }

    public final BooleanProperty getFilledProperty() {
        if (fillActive == null) {
            fillActive = new BooleanPropertyBase() {

                @Override
                public void invalidated() {
                    resetPipeFill();
                }

                @Override
                public Object getBean() {
                    return Pipe.this;
                }

                @Override
                public String getName() {
                    return "fillActive";
                }
            };
        }
        return fillActive;

    }

    public void setFilled(boolean on) {
        getFilledProperty().set(on);
    }

    public boolean getFilled() {
        return getFilledProperty().get();
    }

    public final BooleanProperty getFlowProperty() {
        if (flowActive == null) {
            flowActive = new BooleanPropertyBase() {

                @Override
                public void invalidated() {
                    resetPipeFill();
                }

                @Override
                public Object getBean() {
                    return Pipe.this;
                }

                @Override
                public String getName() {
                    return "flowActive";
                }
            };
        }
        return flowActive;

    }

    public void setFlow(boolean on) {
        getFlowProperty().set(on);
    }

    public boolean getFlow() {
        return getFlowProperty().get();
    }


    public Group getGroup() {
        return group;
    }

    public final ObjectProperty<Paint> fillPipeWallProperty() {
        if (fillPipeWall == null) {
            fillPipeWall = new ObjectPropertyBase<Paint>(Color.BLACK) {

                boolean needsListener = false;

                @Override
                public void invalidated() {
                    pipe.setFill(get());

                }

                @Override
                public Object getBean() {
                    return Pipe.this;
                }

                @Override
                public String getName() {
                    return "fillPipeWall";
                }

            };
        }
        return fillPipeWall;
    }

    public final ObjectProperty<Paint> fillPipeFullProperty() {
        if (fillPipeFull == null) {
            fillPipeFull = new ObjectPropertyBase<Paint>(Color.web("#0000FE")) {

                boolean needsListener = false;

                @Override
                public void invalidated() {
                    resetPipeFill();
                }

                @Override
                public Object getBean() {
                    return Pipe.this;
                }

                @Override
                public String getName() {
                    return "fillPipeFull";
                }

            };
        }
        return fillPipeFull;
    }

    public final ObjectProperty<Paint> fillPipeFull2Property() {
        if (fillPipeFull2 == null) {
            fillPipeFull2 = new ObjectPropertyBase<Paint>(Color.web("#0000E0")) {

                boolean needsListener = false;

                @Override
                public void invalidated() {
                    resetPipeFill();
                }

                @Override
                public Object getBean() {
                    return Pipe.this;
                }

                @Override
                public String getName() {
                    return "fillPipeFull2";
                }

            };
        }
        return fillPipeFull2;
    }

    public final ObjectProperty<Paint> fillPipeEmptyProperty() {
        if (fillPipeEmpty == null) {
            fillPipeEmpty = new ObjectPropertyBase<Paint>(Color.LIGHTGREY) {

                boolean needsListener = false;

                @Override
                public void invalidated() {
                    resetPipeFill();
                }

                @Override
                public Object getBean() {
                    return Pipe.this;
                }

                @Override
                public String getName() {
                    return "fillPipeEmpty";
                }

            };
        }
        return fillPipeEmpty;
    }

}
