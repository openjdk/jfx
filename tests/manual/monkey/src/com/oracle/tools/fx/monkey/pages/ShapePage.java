/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.pages;

import java.util.List;
import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleSpinner;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.options.PaintOption;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.NamedValue;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Shape Page.
 */
public class ShapePage extends TestPaneBase {
    private final SimpleObjectProperty<Supplier<Shape>> gen1 = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Supplier<Shape>> gen2 = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Operation> operation = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty scaleToFit = new SimpleBooleanProperty();
    private final Props props1 = new Props(true);
    private final Props props2 = new Props(false);
    private final ObjectBinding<Node[]> nodes;
    private final StackPane stack;
    private final DoubleBinding stackScale;
    private Shape shape1;
    private Shape shape2;
    private Shape result;
    private double originX;
    private double originY;
    private final Positioner pos1;
    private final Positioner pos2;
    private final BorderPane pane;

    public ShapePage() {
        super("ShapePage");

        pos1 = new Positioner();
        pos2 = new Positioner();

        stack = new StackPane();
        stack.setBackground(Background.fill(Color.WHITE));
        stack.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        stack.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        FX.setPopupMenu(stack, this::createPopupMenu);

        nodes = Bindings.createObjectBinding(
            this::updateShapes,
            gen1,
            gen2,
            props1.fill,
            props1.smooth,
            props1.stroke,
            props1.strokeDashOffset,
            props1.strokeLineCap,
            props1.strokeLineJoin,
            props1.strokeMiterLimit,
            props1.strokeType,
            props1.strokeWidth,
            props2.fill,
            props2.smooth,
            props2.stroke,
            props2.strokeDashOffset,
            props2.strokeLineCap,
            props2.strokeLineJoin,
            props2.strokeMiterLimit,
            props2.strokeType,
            props2.strokeWidth,
            operation
        );
        nodes.addListener((s,p,v) -> {
            stack.getChildren().setAll(v);
        });

        OptionPane op = new OptionPane();
        op.section("Shape 1");
        op.option("Shape 1", createShapeSelector("shape1", gen1));
        props("s1_", op, props1);
        op.section("Shape 2");
        op.option("Shape 2", createShapeSelector("shape2", gen2));
        props("s2_", op, props2);
        op.section("Common");
        op.option("Operation:", createOpSelector("op", operation));
        op.option(new BooleanOption("scaleToFit", "scale to fit", scaleToFit));
        op.label("use mouse / shift-mouse to move the shapes");

        pane = new BorderPane(stack);
        setContent(pane);
        setOptions(op);

        stackScale = Bindings.createDoubleBinding(
            this::computeScale,
            pane.widthProperty(),
            pane.heightProperty(),
            stack.getChildrenUnmodifiable(),
            scaleToFit
        );
        stack.scaleXProperty().bind(stackScale);
        stack.scaleYProperty().bind(stackScale);
    }

    private ObjectOption<Supplier<Shape>> createShapeSelector(String name, ObjectProperty<Supplier<Shape>> p) {
        ObjectOption<Supplier<Shape>> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", () -> {
            return null;
        });
        op.addChoice("Arc", () -> {
            return new Arc(0, 0, 100, 200, 0, 270);
        });
        op.addChoice("Circle", () -> {
            return new Circle(20, 20, 40);
        });
        op.addChoice("Cubic Curve", () -> {
            return new CubicCurve(0, 0, 100, 60, 200, 0, 300, 80);
        });
        op.addChoice("Ellipse", () -> {
            return new Ellipse(0, 0, 200, 100);
        });
        op.addChoice("Line", () -> {
            return new Line(0, 0, 200, 100);
        });
        op.addChoice("Path (H)", () -> {
            Path t = new Path();
            t.getElements().addAll(
                new MoveTo(0.0, 0),
                new LineTo(100, 10),
                new LineTo(110, 45),
                new LineTo(10, 50),
                new ClosePath()
            );
            return t;
        });
        op.addChoice("Path (V)", () -> {
            Path t = new Path();
            t.getElements().addAll(
                new MoveTo(50, 0),
                new LineTo(75, 5),
                new LineTo(80, 120),
                new LineTo(45, 125),
                new ClosePath()
            );
            return t;
        });
        op.addChoice("Polygon", () -> {
            return new Polygon(0, 0, 200, 100, 250, 200, 200, 210, 100, -30, 20, 205);
        });
        op.addChoice("Polyline", () -> {
            return new Polyline(0, 0, 200, 100, 250, 200, 200, 210, 100, -30, 20, 205);
        });
        // TODO quad curve, svgpath?
        op.addChoice("Rectangle", () -> {
            return new Rectangle(0, 0, 400, 200);
        });
        op.addChoice("Text", () -> {
            Text t = new Text("Text");
            t.setFont(Font.font("System", FontWeight.BOLD, 48));
            return t;
        });
        op.selectFirst();
        return op;
    }

    private ObjectOption<Operation> createOpSelector(String name, ObjectProperty<Operation> p) {
        ObjectOption<Operation> op = new ObjectOption<>(name, p);
        op.addChoice("Intersect", Shape::intersect);
        op.addChoice("Subtract", Shape::subtract);
        op.addChoice("Union", Shape::union);
        op.selectFirst();
        return op;
    }

    private double computeScale() {
        if (scaleToFit.get()) {
            double s1 = getScale(stack.prefHeight(-1), pane.getHeight());
            double s2 = getScale(stack.prefWidth(-1), pane.getWidth());
            return Math.min(s1, s2);
        }
        return 1.0;
    }

    private static double getScale(double pref, double available) {
        if ((pref > 0) && (available > 0)) {
            return available / pref;
        }
        return 1.0;
    }

    @FunctionalInterface
    private interface Operation {
        public Shape op(Shape a, Shape b);
    }

    private class Positioner {
        double tx;
        double ty;
        double initx;
        double inity;

        public void set() {
            initx = tx;
            inity = ty;
        }

        public void reset() {
            tx = 0;
            ty = 0;
            translate(shape1);
            translate(shape2);
        }

        public void handleEvent(MouseEvent ev, Shape s) {
            if (s == null) {
                return;
            }

            tx = initx + ev.getX() - originX;
            ty = inity + ev.getY() - originY;
            translate(s);

            Operation op = operation.get();
            if (op != null) {
                if (result != null) {
                    stack.getChildren().remove(result);
                }
                if ((shape1 != null) && (shape2 != null)) {
                    result = op.op(shape1, shape2);
                    init(result);
                    result.setFill(FX.alpha(Color.BLACK, 0.5));
                    stack.getChildren().add(result);
                }
            }
        }

        public void translate(Shape s) {
            if (s != null) {
                s.setTranslateX(tx);
                s.setTranslateY(ty);
            }
        }
    }

    private Node[] clear() {
        shape1 = null;
        shape2 = null;
        result = null;
        return null;
    }

    private Node[] updateShapes() {
        result = null;
        Supplier<Shape> sup1 = gen1.get();
        Supplier<Shape> sup2 = gen2.get();
        if ((sup1 == null) || (sup2 == null)) {
            return clear();
        }

        // shape 1

        shape1 = sup1.get();
        if (shape1 != null) {
            pos1.translate(shape1);
            init(shape1);
            setProps(shape1, props1);
        }

        // shape 2

        shape2 = sup2.get();
        if (shape2 != null) {
            pos2.translate(shape2);
            init(shape2);
            setProps(shape2, props2);
        }

        if (shape1 == null) {
            if (shape2 == null) {
                return clear();
            }
            return new Node[] { shape2 };
        } else if (shape2 == null) {
            return new Node[] { shape1 };
        }

        // result

        Operation op = operation.get();
        if (op == null) {
            return new Node[] { shape1, shape2 };
        }

        result = op.op(shape1, shape2);
        init(result);
        result.setFill(FX.alpha(Color.BLACK, 0.5));
        return new Node[] { shape1, shape2, result };
    }

    private static void init(Node n) {
        //n.setManaged(false);
        n.setLayoutX(0);
        n.setLayoutY(0);
    }

    private void handleMousePressed(MouseEvent ev) {
        originX = ev.getX();
        originY = ev.getY();
        pos1.set();
        pos2.set();
    }

    private void handleMouseDragged(MouseEvent ev) {
        if(ev.isShiftDown()) {
            pos2.handleEvent(ev, shape2);
        } else {
            pos1.handleEvent(ev, shape1);
        }
    }

    private void reset() {
        pos1.reset();
        pos2.reset();
    }

    private ContextMenu createPopupMenu() {
        ContextMenu m = new ContextMenu();
        FX.item(m, "Reset Transform", this::reset);
        return m;
    }

    private static Shape union(Shape... shapes) {
        Shape rv = null;
        for (Shape s : shapes) {
            if (rv == null) {
                rv = s;
            } else {
                rv = Shape.union(rv, s);
            }
        }
        return rv;
    }

    private static Node createDashArrayOption(String name, ObservableList<Double> dashArray) {
        ComboBox<NamedValue<List<Double>>> op = new ComboBox<>();
        FX.name(op, name);
        op.getItems().setAll(
            new NamedValue("[]", List.of()),
            new NamedValue("[2]", List.of(2.0)),
            new NamedValue("[3, 5]", List.of(3.0, 5.0))
        );
        op.getSelectionModel().selectedItemProperty().addListener((s, pr, c) -> {
            List<Double> v = c.getValue();
            dashArray.setAll(v);
        });
        return op;
    }

    public static void props(String prefix, OptionPane op, Props p) {
        op.option("Fill:", new PaintOption(prefix + "fill", p.fill));
        op.option(new BooleanOption(prefix + "smooth", "smooth", p.smooth));
        op.option("Stroke:", new PaintOption("stroke", p.stroke));
        op.option("- Dash Array:", createDashArrayOption(prefix + "dashArray", p.dashArray));
        op.option("- Dash Offset:", new DoubleSpinner(prefix + "strokeDashOffset", 0, 100, 0.1, p.strokeDashOffset));
        op.option("- Line Cap:", new EnumOption<>(prefix + "strokeLineCap", StrokeLineCap.class, p.strokeLineCap));
        op.option("- Line Join:", new EnumOption<>(prefix + "strokeLineJoin", StrokeLineJoin.class, p.strokeLineJoin));
        op.option("- Miter Limit:", new DoubleSpinner(prefix + "strokeMeterLimit", 0, 100, 0.1, p.strokeMiterLimit));
        op.option("- Type:", new EnumOption<>(prefix + "strokeType", StrokeType.class, p.strokeType));
        op.option("- Width:", new DoubleSpinner(prefix + "strokeWidth", 0, 100, 0.1, p.strokeWidth));
    }

    private static void setProps(Shape s, Props p) {
        s.fillProperty().bind(p.fill);
        s.smoothProperty().bind(p.smooth);
        s.strokeProperty().bind(p.stroke);
        s.strokeDashOffsetProperty().bind(p.strokeDashOffset);
        s.strokeLineCapProperty().bind(p.strokeLineCap);
        s.strokeLineJoinProperty().bind(p.strokeLineJoin);
        s.strokeMiterLimitProperty().bind(p.strokeMiterLimit);
        s.strokeTypeProperty().bind(p.strokeType);
        s.strokeWidthProperty().bind(p.strokeWidth);
        p.setShape(s);
    }

    private static class Props {
        public final ObservableList<Double> dashArray = FXCollections.observableArrayList();
        public final SimpleObjectProperty<Paint> fill = new SimpleObjectProperty<>();
        public final SimpleBooleanProperty smooth = new SimpleBooleanProperty(true);
        public final SimpleObjectProperty<Paint> stroke = new SimpleObjectProperty<>();
        public final SimpleDoubleProperty strokeDashOffset = new SimpleDoubleProperty();
        public final SimpleObjectProperty<StrokeLineCap> strokeLineCap = new SimpleObjectProperty<>(StrokeLineCap.ROUND);
        public final SimpleObjectProperty<StrokeLineJoin> strokeLineJoin = new SimpleObjectProperty<>();
        public final SimpleDoubleProperty strokeMiterLimit = new SimpleDoubleProperty();
        public final SimpleObjectProperty<StrokeType> strokeType = new SimpleObjectProperty<>();
        public final SimpleDoubleProperty strokeWidth = new SimpleDoubleProperty(5);
        private Shape shape;

        public Props(boolean first) {
            if (first) {
                stroke.set(Color.RED);
                fill.set(FX.alpha(Color.RED, 0.5));
            } else {
                stroke.set(Color.GREEN);
                fill.set(FX.alpha(Color.GREEN, 0.5));
            }

            dashArray.addListener(new ListChangeListener<>() {
                @Override
                public void onChanged(Change<? extends Double> ch) {
                    if (shape != null) {
                        shape.getStrokeDashArray().setAll(ch.getList());
                    }
                }
            });
        }

        public void setShape(Shape s) {
            this.shape = s;
        }
    }
}
