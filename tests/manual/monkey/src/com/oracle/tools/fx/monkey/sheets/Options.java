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
package com.oracle.tools.fx.monkey.sheets;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;
import javafx.stage.FileChooser;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.IntOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.options.TextChoiceOption;
import com.oracle.tools.fx.monkey.util.CustomPane;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.ImageTools;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.StageInfoPane;
import com.oracle.tools.fx.monkey.util.TextTemplates;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Convenience Methods.
 */
public class Options {
    /**
     * Returns the text choice option initialized with common single- or multi-line strings.
     * @param name
     * @param multiLine
     * @param allowEditButton
     * @param prop
     * @return
     */
    public static Node textOption(String name, boolean multiLine, boolean allowEditButton, StringProperty prop) {
        TextChoiceOption op = new TextChoiceOption(name, true, prop);
        Object[] pairs = multiLine ? TextTemplates.multiLineTextPairs() : TextTemplates.singleLineTextPairs();
        Utils.fromPairs(pairs, (k,v) -> op.addChoice(k, v));
        op.selectFirst();
        return op;
    }

    public static Node textOption(String name, boolean allowEditButton, StringProperty prop, Object ... nameValuePairs) {
        TextChoiceOption op = new TextChoiceOption(name, true, prop);
        Utils.fromPairs(nameValuePairs, (k,v) -> op.addChoice(k, v));
        op.selectFirst();
        return op;
    }

    public static Node fixedSizeOption(String name, DoubleProperty p) {
        return DoubleOption.of(name, p, 0, 20, 33.4, 50, 100);
    }

    public static Node spacing(String name, DoubleProperty p) {
        return DoubleOption.of(name, p, 0, 0.333, 0.5, 1, 2, 10, 20, 33.4, 50, 100);
    }

    public static Node gaps(String name, DoubleProperty p) {
        return DoubleOption.of(name, p, 0, 1, 1.5, 4, 10, 20, 33.33, 100);
    }

    public static ObjectOption<Image> createImageOption(String name, ObjectProperty<Image> p) {
        ObjectOption<Image> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", null);
        op.addChoice("1x1", ImageTools.createImage(1, 1));
        op.addChoice("16 x 16", ImageTools.createImage(16, 16));
        op.addChoice("32 x 32", ImageTools.createImage(32, 32));
        op.addChoice("64 x 64", ImageTools.createImage(64, 64));
        op.addChoiceSupplier("128 x 16", () -> ImageTools.createImage(128, 16));
        op.addChoiceSupplier("16 x 128", () -> ImageTools.createImage(16, 128));
        op.addChoiceSupplier("256 x 256", () -> ImageTools.createImage(256, 256));
        op.addChoiceSupplier("4096 x 4096", () -> ImageTools.createImage(4096, 4096));
        op.selectFirst();
        return op;
    }

    public static Node tabPaneConstraints(String name, DoubleProperty p) {
        DoubleOption d = new DoubleOption(name, p);
        d.addChoice(0);
        d.addChoice(10.0);
        d.addChoice(33.3);
        d.addChoice(100.0);
        d.addChoice("Double.MAX_VALUE", Double.MAX_VALUE);
        d.addChoice("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY);
        d.addChoice("NaN", Double.NaN);
        d.selectInitialValue();
        return d;
    }

    public static Node forRegion(String name, Property<Number> p) {
        DoubleOption d = new DoubleOption(name, p);
        d.addChoice("USE_COMPUTED_SIZE (-1)", Region.USE_COMPUTED_SIZE);
        d.addChoice("USE_PREF_SIZE (-∞)", Region.USE_PREF_SIZE);
        d.addChoice("0", Double.valueOf(0));
        d.addChoice("10", 10.0);
        d.addChoice("33.3", 33.3);
        d.addChoice("100", 100.0);
        d.addChoice("Double.MAX_VALUE", Double.MAX_VALUE);
        d.addChoice("Double.MIN_VALUE", Double.MIN_VALUE);
        d.addChoice("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY);
        d.addChoice("NaN", Double.NaN);
        d.selectInitialValue();
        return d;
    }

    public static Node doubleOption(String name, Property<Number> p) {
        DoubleOption d = new DoubleOption(name, p);
        d.addChoice("0", Double.valueOf(0));
        d.addChoice("10", 10.0);
        d.addChoice("33.3", 33.3);
        d.addChoice("100", 100.0);
        d.addChoice("Double.MAX_VALUE", Double.MAX_VALUE);
        d.addChoice("Double.MIN_VALUE", Double.MIN_VALUE);
        d.addChoice("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY);
        d.addChoice("NaN", Double.NaN);
        d.select(p.getValue(), true);
        return d;
    }

    public static Node lineSpacing(String name, Property<Number> p) {
        return DoubleOption.of(name, p, 0, 0.5, 1, 2, 3.14, 10, 33.33, 100);
    }

    public static Node background(Node owner, String name, Property<Background> p) {
        ObjectOption<Background> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", null);
        op.addChoiceSupplier("Black", () -> {
            return Background.fill(Color.BLACK);
        });
        op.addChoiceSupplier("Dark Gray", () -> {
            return Background.fill(Color.DARKGRAY);
        });
        op.addChoiceSupplier("Light Gray", () -> {
            return Background.fill(Color.LIGHTGRAY);
        });
        op.addChoiceSupplier("White", () -> {
            return Background.fill(Color.WHITE);
        });
        op.addChoiceSupplier("Red", () -> {
            return Background.fill(Color.RED);
        });
        op.addChoiceSupplier("Linear Gradient", () -> {
            LinearGradient g = new LinearGradient(
                0, 0, 30, 30, false,
                CycleMethod.REFLECT,
                new Stop(0, Color.RED), new Stop(30, Color.GREEN)
            );
            return Background.fill(g);
        });
        op.addChoiceSupplier("Radial Gradient", () -> {
            RadialGradient g = new RadialGradient(
                45, 0, 50, 10, 10, false,
                CycleMethod.REFLECT,
                new Stop(0, Color.RED), new Stop(10, Color.GREEN)
            );
            return Background.fill(g);
        });
        op.addChoiceSupplier("Image Pattern", () -> {
            ImagePattern g = new ImagePattern(
                ImageTools.createImage(50, 50),
                0, 0, 50, 50, false
            );
            return Background.fill(g);
        });
        op.addChoiceSupplier("Negative Insets", () -> {
            BackgroundFill f = new BackgroundFill(Color.rgb(0, 0, 255, 0.5), new CornerRadii(10), new Insets(-10, -10, -10, -10));
            return new Background(f);
        });
        op.selectInitialValue();
        return op;
    }

    public static Node tabSize(String name, IntegerProperty p) {
        return new IntOption(name, 0, Integer.MAX_VALUE, p);
    }

    public static Node placeholderNode(String name, ObjectProperty<Node> p) {
        ObjectOption<Node> op = new ObjectOption<>(name, p);
        op.addChoiceSupplier("Button", () -> new Button("Placeholder"));
        op.addChoiceSupplier("Label", () -> new Label("Placeholder"));
        op.addChoice("<null>", null);
        op.selectInitialValue();
        return op;
    }

    public static TextChoiceOption promptText(String name, boolean allowEditButton, StringProperty p) {
        TextChoiceOption op = new TextChoiceOption(name, allowEditButton, p);
        Utils.fromPairs(TextTemplates.singleLineTextPairs(), (k, v) -> op.addChoice(k, v));
        return op;
    }

    public static ObjectSelector<Runnable> selector(String name) {
        return new ObjectSelector<Runnable>(name, (r) -> {
            try {
                r.run();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public static Node forColumnWidth(String name, double defaultValue, DoubleProperty p) {
        DoubleOption d = new DoubleOption(name, p);
        d.addChoice("0", Double.valueOf(0));
        d.addChoice("10", 10.0);
        d.addChoice("33.3", 33.3);
        d.addChoice("100", 100.0);
        d.addChoice("Double.MAX_VALUE", Double.MAX_VALUE);
        d.addChoice("Double.MIN_VALUE", Double.MIN_VALUE);
        d.addChoice("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY);
        d.addChoice("NaN", Double.NaN);
        d.addChoice("<default: " + defaultValue + ">", defaultValue);
        d.selectInitialValue();
        return d;
    }

    public static Node boundsOption(String name, ObjectProperty<Bounds> p) {
        Bounds[] bounds = {
            b(0, 0, 0, 0),
            b(0, 0, 10, 10),
            b(0, 0, 1000, 1000),
            b(-500, -500, 1000, 1000)
        };

        ObjectOption<Bounds> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", null);
        for(Bounds b: bounds) {
            String s =
                "@" + b.getMinX() + "," + b.getMinY() +
                "  [" + b.getWidth() + "x" + b.getHeight() + "]";
            op.addChoice(s, b);
        }
        op.selectInitialValue();
        return op;
    }

    private static Bounds b(double x, double y, double w, double h) {
        return new BoundingBox(x, y, 0.0, w, h, 0.0);
    }

    public static Node shape(String name, Node n, ObjectProperty<Shape> prop) {
        ObjectOption<Shape> op = new ObjectOption<>(name, prop);
        op.addChoice("<null>", null);
        op.addChoiceSupplier("Leaf", () -> new LeafShape(n));
        op.selectInitialValue();
        return op;
    }

    public static Node clip(String name, Node n, ObjectProperty<Node> prop) {
        ObjectOption<Node> op = new ObjectOption<>(name, prop);
        op.addChoice("<null>", null);
        op.addChoiceSupplier("Ellipse", () -> {
            return new EllipseClip(n);
        });
        op.selectInitialValue();
        return op;
    }

    private static class EllipseClip extends Ellipse {
        private final Node owner;
        private final ObjectBinding<Bounds> binding;

        public EllipseClip(Node n) {
            this.owner = n;
            binding = Bindings.createObjectBinding(n::getLayoutBounds, n.layoutBoundsProperty());
            binding.addListener((p) -> {
                update();
            });
            update();
        }

        private void update() {
            Bounds b = binding.get();
            double rx = b.getWidth() / 2.0;
            double ry = b.getHeight() / 2.0;
            setCenterX(rx);
            setCenterY(ry);
            setRadiusX(rx);
            setRadiusY(ry);
        }
    }

    private static class LeafShape extends Path {
        private final Node owner;
        private final ObjectBinding<Bounds> binding;

        public LeafShape(Node n) {
            this.owner = n;
            binding = Bindings.createObjectBinding(n::getLayoutBounds, n.layoutBoundsProperty());
            binding.addListener((p) -> {
                update();
            });
            update();
        }

        private void update() {
            ArrayList<PathElement> a = new ArrayList<>();
            Bounds b = binding.get();
            double w = b.getWidth();
            double h = b.getHeight();
            if ((w > 0.0) && (h > 0.0)) {
                a.add(new MoveTo(0.0, 0.0));
                a.add(new CubicCurveTo(0.0, 0.0, w, 0.0, w, h));
                a.add(new CubicCurveTo(w, h, 0.0, h, 0.0, 0.0));
                a.add(new ClosePath());
            }
            getElements().setAll(a);
        }
    }

    public static Node createSourceUriOption(Node parent, String name, SimpleStringProperty sourceURI) {
        TextField uriField = new TextField();
        uriField.setPromptText("URI");
        Button button = new Button("Browse...");
        button.setOnAction((ev) -> {
            FileChooser fc = new FileChooser();
            String uri = sourceURI.get();
            if (uri != null) {
                File f = parseFileURI(uri);
                if (f != null) {
                    fc.setInitialDirectory(f.getParentFile());
                    fc.setInitialFileName(f.getName());
                }
            }
            File file = fc.showOpenDialog(FX.getParentWindow(parent));
            if (file != null) {
                String s = file.toURI().toString();
                uriField.setText(s);
                sourceURI.set(s);
            }
        });
        HBox hb = new HBox(5, uriField, button);
        HBox.setHgrow(uriField, Priority.ALWAYS);
        return hb;
    }

    private static File parseFileURI(String text) {
        // TODO
        return null;
    }

    public static Node tooltipOption(String name, ObjectProperty<Tooltip> p) {
        ObjectOption<Tooltip> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", null);
        op.addChoiceSupplier("simple text", () -> createSimpleTextTooltip());
        op.addChoiceSupplier("with image", () -> createTooltipWithImage());
        op.selectInitialValue();
        return op;
    }

    private static Tooltip createSimpleTextTooltip() {
        Tooltip t = new Tooltip("simple text tooltip");
        return t;
    }

    private static Tooltip createTooltipWithImage() {
        Tooltip t = new Tooltip("tooltip with image");
        t.setGraphic(ImageTools.createImageView(128, 96));
        return t;
    }

    public static <T extends Enum> ObjectSelector<T> ofEnum(String name, boolean allowNull, Class<T> type, T defaultValue, Consumer<T> client) {
        ObjectSelector<T> op = new ObjectSelector<>(name, client);
        T[] values = type.getEnumConstants();
        Arrays.sort(values, new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return a.toString().compareTo(b.toString());
            }
        });

        if (allowNull) {
            op.addChoice("<null>", null);
        }
        for (T v : values) {
            op.addChoice(v.toString(), v);
        }
        return op;
    }

    public static ObjectOption<Node> nodeOption(String name, ObjectProperty<Node> p) {
        ObjectOption<Node> op = new ObjectOption<Node>(name, p);
        op.addChoice("<null>", null);
        op.addChoice("1 x 1", ImageTools.createImageView(1, 1));
        op.addChoice("16 x 16", ImageTools.createImageView(16, 16));
        op.addChoice("128 x 16", ImageTools.createImageView(128, 16));
        op.addChoice("16 x 128", ImageTools.createImageView(16, 128));
        op.addChoice("512 x 512", ImageTools.createImageView(512, 512));
        op.addChoiceSupplier("Complex Pane", CustomPane::create);
        op.addChoiceSupplier("Stage Information", StageInfoPane::new);
        op.selectInitialValue();
        return op;
    }

    public static Node opacity(String name, DoubleProperty p) {
        DoubleOption op = new DoubleOption(name, p);
        op.addChoice(0);
        op.addChoice(0.5);
        op.addChoice(1.0);
        op.addChoice("NaN", Double.NaN);
        op.selectInitialValue();
        return op;
    }

    public static Node scale(String name, DoubleProperty p) {
        DoubleOption op = new DoubleOption(name, p);
        op.addChoice(0);
        op.addChoice(0.5);
        op.addChoice(1.0);
        op.addChoice(2.0);
        op.addChoice("NaN", Double.NaN);
        op.selectInitialValue();
        return op;
    }

    public static CheckBox booleanIndicator(String text, ReadOnlyBooleanProperty p) {
        CheckBox c = new CheckBox(text);
        c.setPadding(OptionPane.INDENT);
        c.setDisable(true);
        c.selectedProperty().bind(p);
        return c;
    }
}
