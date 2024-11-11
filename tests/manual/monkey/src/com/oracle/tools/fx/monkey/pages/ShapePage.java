/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Shape Page.
 */
public class ShapePage extends TestPaneBase {
    private final SimpleObjectProperty<Supplier<Shape>> gen1;
    private final SimpleObjectProperty<Supplier<Shape>> gen2;
    private final CheckBox stroke1;
    private final CheckBox stroke2;
    private final CheckBox fill1;
    private final CheckBox fill2;
    private final SimpleObjectProperty<Operation> operation;
    private final ObjectBinding<Node[]> nodes;
    private final StackPane stack;
    private Shape shape1;
    private Shape shape2;
    private Shape result;
    private double originX;
    private double originY;
    private final Positioner pos1;
    private final Positioner pos2;

    public ShapePage() {
        super("ShapePage");

        pos1 = new Positioner();
        pos2 = new Positioner();

        stack = new StackPane();
        stack.setBackground(Background.fill(Color.WHITE));
        stack.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        stack.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        FX.setPopupMenu(stack, this::createPopupMenu);

        gen1 = new SimpleObjectProperty<>();

        gen2 = new SimpleObjectProperty<>();

        stroke1 = new CheckBox("S");
        FX.name(stroke1, "stroke1");
        stroke1.setSelected(true);
        FX.tooltip(stroke1, "Enables the stroke on the shape");

        stroke2 = new CheckBox("S");
        FX.name(stroke2, "stroke2");
        stroke2.setSelected(true);
        FX.tooltip(stroke2, "Enables the stroke on the shape");

        fill1 = new CheckBox("F");
        FX.name(stroke2, "fill1");
        FX.tooltip(fill1, "Enables the fill on the shape");

        fill2 = new CheckBox("F");
        FX.name(stroke2, "fill2");
        FX.tooltip(fill2, "Enables the fill on the shape");

        operation = new SimpleObjectProperty<>();

        nodes = Bindings.createObjectBinding(
            this::process,
            gen1,
            gen2,
            stroke1.selectedProperty(),
            stroke2.selectedProperty(),
            fill1.selectedProperty(),
            fill2.selectedProperty(),
            operation
        );
        nodes.addListener((s,p,v) -> {
            stack.getChildren().setAll(v);
        });

        OptionPane op = new OptionPane();
        op.section("Shape");
        op.option("Shape 1", createShapeSelector("shape1", gen1, stroke1, fill1));
        op.option("Shape 2", createShapeSelector("shape2", gen2, stroke2, fill2));
        op.separator();
        op.option("Operation:", createOpSelector("op", operation));

        setContent(stack);
        setOptions(op);
    }

    private Node createShapeSelector(String name, ObjectProperty<Supplier<Shape>> p, CheckBox stroke, CheckBox fill) {
        ObjectOption<Supplier<Shape>> op = new ObjectOption<>(name, p);
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

        return new HBox(op, stroke, fill);
    }

    private ObjectOption<Operation> createOpSelector(String name, ObjectProperty<Operation> p) {
        ObjectOption<Operation> op = new ObjectOption<>(name, p);
        op.addChoice("Intersect", Shape::intersect);
        op.addChoice("Subtract", Shape::subtract);
        op.addChoice("Union", Shape::union);
        op.selectFirst();
        return op;
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
                result = op.op(shape1, shape2);
                init(result);
                result.setFill(FX.alpha(Color.BLACK, 0.5));
                stack.getChildren().add(result);
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

    Node[] process() {
        Supplier<Shape> sup1 = gen1.get();
        if (sup1 == null) {
            return clear();
        }

        Supplier<Shape> sup2 = gen2.get();
        if (sup2 == null) {
            return clear();
        }

        Operation op = operation.get();
        if (op == null) {
            return clear();
        }

        // shape 1

        shape1 = sup1.get();
        pos1.translate(shape1);
        init(shape1);

        if (stroke1.isSelected()) {
            shape1.setStroke(Color.RED);
            shape1.setStrokeLineCap(StrokeLineCap.ROUND);
            shape1.setStrokeWidth(10);
        } else {
            shape1.setStroke(null);
            shape1.setStrokeWidth(0);
        }
        shape1.setFill(fill1.isSelected() ? FX.alpha(Color.RED, 0.5) : null);

        // shape 2

        shape2 = sup2.get();
        pos2.translate(shape2);
        init(shape2);

        if (stroke2.isSelected()) {
            shape2.setStroke(Color.GREEN);
            shape2.setStrokeLineCap(StrokeLineCap.ROUND);
            shape2.setStrokeWidth(10);
        } else {
            shape2.setStroke(null);
            shape2.setStrokeWidth(0);
        }
        shape2.setFill(fill2.isSelected() ? FX.alpha(Color.GREEN, 0.5) : null);

        // result

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

    void handleMousePressed(MouseEvent ev) {
        originX = ev.getX();
        originY = ev.getY();
        pos1.set();
        pos2.set();
    }

    void handleMouseDragged(MouseEvent ev) {
        if(ev.isShiftDown()) {
            pos2.handleEvent(ev, shape2);
        } else {
            pos1.handleEvent(ev, shape1);
        }
    }

    void reset() {
        pos1.reset();
        pos2.reset();
    }

    ContextMenu createPopupMenu() {
        ContextMenu m = new ContextMenu();
        FX.item(m, "Reset Transform", this::reset);
        return m;
    }
}
