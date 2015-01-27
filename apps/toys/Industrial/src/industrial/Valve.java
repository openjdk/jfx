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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.transform.Rotate;

public class Valve {

    Group group;

    BooleanProperty fillActive;

    private ObjectProperty<Paint> fillValveBody;
    private ObjectProperty<Paint> fillValveOn;
    private ObjectProperty<Paint> fillValveOff;

    private final Path valveBodyLeft;
    private final Path valveBodyRight;
    private final Path valveStick;
    private final Path valveActiveFlag;

    public Valve() {
        valveBodyLeft = new Path(
                new MoveTo(0, 0),
                new LineTo(0, 80),
                new LineTo(80, 40),
                new ClosePath()
        );
        valveBodyRight = new Path(
                new MoveTo(80, 40),
                new LineTo(160, 80),
                new LineTo(160, 0),
                new ClosePath()
        );
        valveStick = new Path(
                new MoveTo(80, 40),
                new LineTo(80, 90)
        );
        valveActiveFlag = new Path(
                new MoveTo(40, 90),
                new LineTo(40, 120),
                new LineTo(120, 120),
                new LineTo(120, 90),
                new ClosePath()
        );

        group = new Group(valveBodyLeft, valveBodyRight, valveStick, valveActiveFlag);

        Rotate flip = new Rotate(180, 80, 60);
        group.getTransforms().add(flip);

        getActiveProperty().set(true);
        resetActiveFill();
        resetBodyFill();
    }

    private void resetActiveFill() {
        if (getActive()) {
            valveActiveFlag.setFill(fillValveOnProperty().get());
        } else {
            valveActiveFlag.setFill(fillValveOffProperty().get());
        }
    }

    private void resetBodyFill() {
        Paint p = fillValveBodyProperty().get();
        valveBodyLeft.setFill(p);
        valveBodyRight.setFill(p);
        valveStick.setStroke(p);

    }

    public final BooleanProperty getActiveProperty() {
        if (fillActive == null) {
            fillActive = new BooleanPropertyBase() {

                @Override
                public void invalidated() {
                    resetActiveFill();
                }

                @Override
                public Object getBean() {
                    return Valve.this;
                }

                @Override
                public String getName() {
                    return "fillActive";
                }
            };
        }
        return fillActive;

    }

    public void setActive(boolean on) {
        getActiveProperty().set(on);
    }

    public boolean getActive() {
        return getActiveProperty().get();
    }

    public Group getGroup() {
        return group;
    }

    public final ObjectProperty<Paint> fillValveBodyProperty() {
        if (fillValveBody == null) {
            fillValveBody = new ObjectPropertyBase<Paint>(Color.RED) {

                boolean needsListener = false;

                @Override
                public void invalidated() {
                    resetBodyFill();
                }

                @Override
                public Object getBean() {
                    return Valve.this;
                }

                @Override
                public String getName() {
                    return "fillValveBody";
                }

            };
        }
        return fillValveBody;
    }

    public final ObjectProperty<Paint> fillValveOnProperty() {
        if (fillValveOn == null) {
            fillValveOn = new ObjectPropertyBase<Paint>(Color.GREEN) {

                boolean needsListener = false;

                @Override
                public void invalidated() {
                    resetActiveFill();
                }

                @Override
                public Object getBean() {
                    return Valve.this;
                }

                @Override
                public String getName() {
                    return "fillValveOn";
                }

            };
        }
        return fillValveOn;
    }

    public final ObjectProperty<Paint> fillValveOffProperty() {
        if (fillValveOff == null) {
            fillValveOff = new ObjectPropertyBase<Paint>(Color.ORANGE) {

                boolean needsListener = false;

                @Override
                public void invalidated() {
                    resetActiveFill();
                }

                @Override
                public Object getBean() {
                    return Valve.this;
                }

                @Override
                public String getName() {
                    return "fillValveOff";
                }

            };
        }
        return fillValveOff;
    }

}
