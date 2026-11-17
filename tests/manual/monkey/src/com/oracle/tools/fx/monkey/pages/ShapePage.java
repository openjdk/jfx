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

import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
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
            return union(
                // curves1
                new CubicCurve(2191.0, 7621.0, 2191.0, 7619.0, 2191.0, 7618.0, 2191.0, 7617.0),
                new CubicCurve(2191.0, 7617.0, 2191.0, 7617.0, 2191.0, 7616.0, 2191.0, 7615.0),
                new CubicCurve(2198.0, 7602.0, 2200.0, 7599.0, 2203.0, 7595.0, 2205.0, 7590.0),
                new CubicCurve(2205.0, 7590.0, 2212.0, 7580.0, 2220.0, 7571.0, 2228.0, 7563.0),
                new CubicCurve(2228.0, 7563.0, 2233.0, 7557.0, 2239.0, 7551.0, 2245.0, 7546.0),
                new CubicCurve(2245.0, 7546.0, 2252.0, 7540.0, 2260.0, 7534.0, 2267.0, 7528.0),
                new CubicCurve(2267.0, 7528.0, 2271.0, 7526.0, 2275.0, 7524.0, 2279.0, 7521.0),
                new CubicCurve(2279.0, 7521.0, 2279.0, 7520.0, 2280.0, 7520.0, 2281.0, 7519.0),
                // curves2
                new CubicCurve(2281.0, 7519.0, 2282.0, 7518.0, 2282.0, 7517.0, 2283.0, 7516.0),
                new CubicCurve(2283.0, 7516.0, 2284.0, 7515.0, 2284.0, 7515.0, 2285.0, 7514.0),
                new CubicCurve(2291.0, 7496.0, 2292.0, 7495.0, 2292.0, 7494.0, 2291.0, 7493.0),
                new CubicCurve(2291.0, 7493.0, 2290.0, 7492.0, 2290.0, 7492.0, 2289.0, 7492.0),
                new CubicCurve(2289.0, 7492.0, 2288.0, 7491.0, 2286.0, 7492.0, 2285.0, 7492.0),
                new CubicCurve(2262.0, 7496.0, 2260.0, 7497.0, 2259.0, 7497.0, 2257.0, 7498.0),
                new CubicCurve(2257.0, 7498.0, 2254.0, 7498.0, 2251.0, 7499.0, 2248.0, 7501.0),
                new CubicCurve(2248.0, 7501.0, 2247.0, 7501.0, 2245.0, 7502.0, 2244.0, 7503.0),
                new CubicCurve(2207.0, 7523.0, 2203.0, 7525.0, 2199.0, 7528.0, 2195.0, 7530.0),
                new CubicCurve(2195.0, 7530.0, 2191.0, 7534.0, 2186.0, 7538.0, 2182.0, 7541.0),
                // curves3
                new CubicCurve(2182.0, 7541.0, 2178.0, 7544.0, 2174.0, 7547.0, 2170.0, 7551.0),
                new CubicCurve(2170.0, 7551.0, 2164.0, 7556.0, 2158.0, 7563.0, 2152.0, 7569.0),
                new CubicCurve(2152.0, 7569.0, 2148.0, 7573.0, 2145.0, 7577.0, 2141.0, 7582.0),
                new CubicCurve(2141.0, 7582.0, 2138.0, 7588.0, 2134.0, 7595.0, 2132.0, 7602.0),
                new CubicCurve(2132.0, 7602.0, 2132.0, 7605.0, 2131.0, 7608.0, 2131.0, 7617.0),
                new CubicCurve(2131.0, 7617.0, 2131.0, 7620.0, 2131.0, 7622.0, 2131.0, 7624.0),
                new CubicCurve(2131.0, 7624.0, 2131.0, 7630.0, 2132.0, 7636.0, 2135.0, 7641.0),
                new CubicCurve(2135.0, 7641.0, 2136.0, 7644.0, 2137.0, 7647.0, 2139.0, 7650.0),
                new CubicCurve(2139.0, 7650.0, 2143.0, 7658.0, 2149.0, 7664.0, 2155.0, 7670.0),
                new CubicCurve(2155.0, 7670.0, 2160.0, 7676.0, 2165.0, 7681.0, 2171.0, 7686.0),
                // curves4
                new CubicCurve(2171.0, 7686.0, 2174.0, 7689.0, 2177.0, 7692.0, 2180.0, 7694.0),
                new CubicCurve(2180.0, 7694.0, 2185.0, 7698.0, 2191.0, 7702.0, 2196.0, 7706.0),
                new CubicCurve(2196.0, 7706.0, 2199.0, 7708.0, 2203.0, 7711.0, 2207.0, 7713.0),
                new CubicCurve(2244.0, 7734.0, 2245.0, 7734.0, 2247.0, 7735.0, 2248.0, 7736.0),
                new CubicCurve(2248.0, 7736.0, 2251.0, 7738.0, 2254.0, 7739.0, 2257.0, 7739.0),
                new CubicCurve(2257.0, 7739.0, 2259.0, 7739.0, 2260.0, 7739.0, 2262.0, 7740.0),
                new CubicCurve(2285.0, 7745.0, 2286.0, 7745.0, 2288.0, 7745.0, 2289.0, 7745.0),
                new CubicCurve(2289.0, 7745.0, 2290.0, 7745.0, 2290.0, 7744.0, 2291.0, 7743.0),
                new CubicCurve(2291.0, 7743.0, 2292.0, 7742.0, 2292.0, 7741.0, 2291.0, 7740.0),
                new CubicCurve(2285.0, 7722.0, 2284.0, 7721.0, 2284.0, 7721.0, 2283.0, 7720.0),
                new CubicCurve(2283.0, 7720.0, 2282.0, 7719.0, 2282.0, 7719.0, 2281.0, 7718.0),
                new CubicCurve(2281.0, 7718.0, 2280.0, 7717.0, 2279.0, 7716.0, 2279.0, 7716.0),
                new CubicCurve(2279.0, 7716.0, 2275.0, 7712.0, 2271.0, 7710.0, 2267.0, 7708.0),
                new CubicCurve(2267.0, 7708.0, 2260.0, 7702.0, 2252.0, 7697.0, 2245.0, 7691.0),
                new CubicCurve(2245.0, 7691.0, 2239.0, 7685.0, 2233.0, 7679.0, 2228.0, 7673.0),
                new CubicCurve(2228.0, 7673.0, 2220.0, 7665.0, 2212.0, 7656.0, 2205.0, 7646.0),
                new CubicCurve(2205.0, 7646.0, 2203.0, 7641.0, 2200.0, 7637.0, 2198.0, 7634.0)
            );
        });
        op.addChoice("Cubic Curves (*)", () -> {
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

    public static void props(String prefix, OptionPane op, Props p) {
        op.option("Fill:", new PaintOption(prefix + "fill", p.fill));
        op.option(new BooleanOption(prefix + "smooth", "smooth", p.smooth));
        op.option("Stroke:", new PaintOption("stroke", p.stroke));
        op.option("Stroke Dash Offset:", new DoubleSpinner(prefix + "strokeDashOffset", 0, 100, 0.1, p.strokeDashOffset));
        op.option("Stroke Line Cap:", new EnumOption<>(prefix + "strokeLineCap", StrokeLineCap.class, p.strokeLineCap));
        op.option("Stroke Line Join:", new EnumOption<>(prefix + "strokeLineJoin", StrokeLineJoin.class, p.strokeLineJoin));
        op.option("Stroke Miter Limit:", new DoubleSpinner(prefix + "strokeMeterLimit", 0, 100, 0.1, p.strokeMiterLimit));
        op.option("Stroke Type:", new EnumOption<>(prefix + "strokeType", StrokeType.class, p.strokeType));
        op.option("Stroke Width:", new DoubleSpinner(prefix + "strokeWidth", 0, 100, 0.1, p.strokeWidth));
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
    }

    private static class Props {
        public final SimpleObjectProperty<Paint> fill = new SimpleObjectProperty<>();
        public final SimpleBooleanProperty smooth = new SimpleBooleanProperty(true);
        public final SimpleObjectProperty<Paint> stroke = new SimpleObjectProperty<>();
        public final SimpleDoubleProperty strokeDashOffset = new SimpleDoubleProperty();
        public final SimpleObjectProperty<StrokeLineCap> strokeLineCap = new SimpleObjectProperty<>(StrokeLineCap.ROUND);
        public final SimpleObjectProperty<StrokeLineJoin> strokeLineJoin = new SimpleObjectProperty<>();
        public final SimpleDoubleProperty strokeMiterLimit = new SimpleDoubleProperty();
        public final SimpleObjectProperty<StrokeType> strokeType = new SimpleObjectProperty<>();
        public final SimpleDoubleProperty strokeWidth = new SimpleDoubleProperty(5);

        public Props(boolean first) {
            if (first) {
                stroke.set(Color.RED);
                fill.set(FX.alpha(Color.RED, 0.5));
            } else {
                stroke.set(Color.GREEN);
                fill.set(FX.alpha(Color.GREEN, 0.5));
            }
        }
    }
}
