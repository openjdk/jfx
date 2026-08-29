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
package com.oracle.tools.fx.monkey.tools;

import java.util.List;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.CustomPane;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HeaderBars;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * Custom Stage Window.
 */
public class CustomStage extends Stage {

    public enum StageContent {
        EMPTY("Empty"),
        IRREGULAR_SHAPE("Irregular Shape"),
        NESTED_STAGES("Nested Stages"),
        TEXT_AREA("TextArea"),
        UI_PANEL("UI Panel");

        private final String text;

        StageContent(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public enum TargetLocation {
        SAME_SCREEN("Same Screen"),
        OTHER_SCREEN("Other Screen"),
        OUTSIDE("Outside");

        private final String text;

        TargetLocation(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private final int num;
    private final HeaderBars.Choice headerBarChoice;
    private final Config conf;
    private static int seq;

    public CustomStage(StageStyle style, StageContent content, HeaderBars.Choice h, Config conf) {
        super(style);
        this.num = seq++;
        this.headerBarChoice = h;
        this.conf = Config.copy(conf);

        setWidth(700);
        setHeight(500);

        setContent(content, h);
    }

    private void setContent(StageContent content, HeaderBars.Choice h) {
        Parent n = switch(content) {
        case EMPTY ->
            createEmpty();
        case IRREGULAR_SHAPE ->
            createIrregularShape();
        case NESTED_STAGES ->
            createNestedStages();
        case TEXT_AREA ->
            createTextArea();
        case UI_PANEL ->
            createUiPanel();
        };

        n = switch(h) {
        case NONE -> n;
        case SIMPLE -> HeaderBars.createSimple(n);
        case SPLIT -> HeaderBars.createSplit(n);
        };

        Scene sc = new Scene(n);
        sc.setFill(Color.TRANSPARENT);
        n.setOnContextMenuRequested(this::createPopupMenu);
        // scene config
        Scene.Preferences p = sc.getPreferences();
        p.setColorScheme(conf.colorScheme.get());
        p.setPersistentScrollBars(conf.persistentScrollBars.get());
        p.setReducedData(conf.reducedData.get());
        p.setReducedMotion(conf.reducedMotion.get());
        p.setReducedTransparency(conf.reducedTransparency.get());
        setScene(sc);
    }

    private void createPopupMenu(ContextMenuEvent ev) {
        ContextMenu m = new ContextMenu();
        for(StageContent c: StageContent.values()) {
            FX.item(m, c.toString(), () -> setContent(c, headerBarChoice));
        }
        FX.separator(m);
        FX.item(m, "Size to Scene", this::sizeToScene);
        FX.item(m, "To Back", this::toBack);
        FX.item(m, "To Front", this::toFront);
        FX.separator(m);
        FX.checkItem(m, "Full Screen", isFullScreen(), this::setFullScreen);
        FX.checkItem(m, "Iconified", isIconified(), this::setIconified);
        FX.checkItem(m, "Maximize", isMaximized(), this::setMaximized);
        FX.separator(m);
        FX.item(m, "Close", this::hide);
        m.show(this, ev.getScreenX(), ev.getScreenY());
    }

    private Parent createEmpty() {
        return new Group();
    }

    private Parent createIrregularShape() {
        Circle c = new Circle(100, Color.RED);
        StackPane g = new StackPane(c);
        g.setBorder(new Border(new BorderStroke(Color.rgb(0, 0, 0, 0.3), BorderStrokeStyle.SOLID, null, new BorderWidths(4))));
        g.setBackground(Background.fill(Color.TRANSPARENT));
        return g;
    }

    private Parent createUiPanel() {
        return CustomPane.create();
    }

    private Parent createTextArea() {
        return new TextArea();
    }

    record Position(double x, double y) { }

    public Position getPosition(TargetLocation t) {
        return switch(t) {
        case OTHER_SCREEN -> {
            List<Screen> ss = Screen.getScreensForRectangle(getX(), getY(), 1, 1);
            Screen current = (ss.isEmpty() ? null : ss.getFirst());
            for (Screen s: Screen.getScreens()) {
                if(s != current) {
                    Rectangle2D r = s.getVisualBounds();
                    yield new Position(r.getMinX() + r.getWidth() * 0.25, r.getMinY() + r.getHeight() * 0.25);
                }
            }
            yield null;
        }
        case OUTSIDE -> {
            double x = Double.MAX_VALUE;
            double y = Double.MAX_VALUE;
            for (Screen s: Screen.getScreens()) {
                var b = s.getVisualBounds();
                x = Math.min(x, b.getMinX());
                y = Math.min(y, b.getMinY());
            }
            yield new Position(x - 1000, y - 1000);
        }
        case SAME_SCREEN ->
            new Position(getX() + 20, getY() + 20);
        };
    }

    public static class Config {
        public final SimpleBooleanProperty alwaysOnTop = new SimpleBooleanProperty();
        public final SimpleObjectProperty<TargetLocation> location = new SimpleObjectProperty<>(TargetLocation.SAME_SCREEN);
        public final SimpleObjectProperty<Modality> modality = new SimpleObjectProperty<>(Modality.NONE);
        public final SimpleObjectProperty<NodeOrientation> nodeOrientation = new SimpleObjectProperty<>(NodeOrientation.INHERIT);
        public final SimpleBooleanProperty owner = new SimpleBooleanProperty();
        public final SimpleObjectProperty<StageStyle> stageStyle = new SimpleObjectProperty<>(StageStyle.DECORATED);
        //
        public final SimpleBooleanProperty fullScreen = new SimpleBooleanProperty();
        public final SimpleStringProperty fullScreenExitHint = new SimpleStringProperty();
        public final SimpleBooleanProperty iconified = new SimpleBooleanProperty(false);
        public final SimpleBooleanProperty maximized = new SimpleBooleanProperty(false);
        //
        public final ObjectProperty<ColorScheme> colorScheme = new SimpleObjectProperty<>();
        public final ObjectProperty<Boolean> persistentScrollBars = new SimpleObjectProperty<>();
        public final ObjectProperty<Boolean> reducedData = new SimpleObjectProperty<>();
        public final ObjectProperty<Boolean> reducedMotion = new SimpleObjectProperty<>();
        public final ObjectProperty<Boolean> reducedTransparency = new SimpleObjectProperty<>();

        public static Config getDefault() {
            Config c = new Config();
            Platform.Preferences pp = Platform.getPreferences();
            c.colorScheme.set(pp.getColorScheme());
            c.persistentScrollBars.set(pp.persistentScrollBarsProperty().get());
            c.reducedData.set(pp.reducedDataProperty().get());
            c.reducedMotion.set(pp.reducedMotionProperty().get());
            c.reducedTransparency.set(pp.reducedTransparencyProperty().get());
            return c;
        }

        public static Config copy(Config x) {
            Config c = new Config();
            c.alwaysOnTop.set(x.alwaysOnTop.get());
            c.location.set(x.location.get());
            c.modality.set(x.modality.get());
            c.nodeOrientation.set(x.nodeOrientation.get());
            c.owner.set(x.owner.get());
            c.stageStyle.set(x.stageStyle.get());
            c.fullScreen.set(x.fullScreen.get());
            c.fullScreenExitHint.set(x.fullScreenExitHint.get());
            c.iconified.set(x.iconified.get());
            c.maximized.set(x.maximized.get());
            c.colorScheme.set(x.colorScheme.get());
            c.persistentScrollBars.set(x.persistentScrollBars.get());
            c.reducedData.set(x.reducedData.get());
            c.reducedMotion.set(x.reducedMotion.get());
            c.reducedTransparency.set(x.reducedTransparency.get());
            return c;
        }
    }

    private Parent createNestedStages() {
        OptionPane op = new OptionPane();
        // init
        op.section("Initialization");
        op.option(new BooleanOption("alwaysOnTop", "always on top", conf.alwaysOnTop));
        // TODO HeaderBar
        op.option("Location:", new EnumOption("location", TargetLocation.class, conf.location));
        op.option("Modality:", new EnumOption("modality", Modality.class, conf.modality));
        op.option("Node Orientation:", new EnumOption("nodeOrientation", NodeOrientation.class, conf.nodeOrientation));
        op.option(new BooleanOption("owner", "set owner", conf.owner));
        op.option("Stage Style:", new EnumOption("stageStyle", StageStyle.class, conf.stageStyle));
        // stage
        op.section("Stage");
        op.option(new BooleanOption("fullScreen", "full screen", conf.fullScreen));
        op.option("Full Screen Hint:", Options.textOption("fullScreenHint", true, true, conf.fullScreenExitHint));
        op.option(new BooleanOption("iconified", "iconified", conf.iconified));
        op.option(new BooleanOption("maximized", "maximized", conf.maximized));

        Button onTopButton = new Button();
        onTopButton.setTooltip(new Tooltip("Toggles the alwaysOnTop property"));
        onTopButton.textProperty().bind(Bindings.createStringBinding(() -> {
            return "AlwaysOnTop" + (isAlwaysOnTop() ? " ✓" : "");
        }, alwaysOnTopProperty()));
        onTopButton.setOnAction((_) -> {
            setAlwaysOnTop(!onTopButton.getText().contains("✓"));
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction((_) -> {
            hide();
        });

        Button createButton = new Button("Create Stage");
        createButton.setOnAction((_) -> {
            // create stage
            Modality mod = conf.modality.get();
            CustomStage own = conf.owner.get() ? this : null;
            Position pos = getPosition(conf.location.get());

            StringBuilder sb = new StringBuilder();
            if ((mod != null) && (mod != Modality.NONE)) {
                sb.append(mod).append(" ");
            }
            sb.append("S.");
            sb.append(seq++);
            if (own != null) {
                sb.append(" owner=S.");
                sb.append(own.num);
            }

            Stage s = new CustomStage(conf.stageStyle.get(), StageContent.NESTED_STAGES, headerBarChoice, conf);
            // init
            s.setTitle(sb.toString());
            s.setAlwaysOnTop(conf.alwaysOnTop.get());
            s.initModality(mod);
            s.initOwner(own);
            s.setFullScreen(conf.fullScreen.get());
            s.setFullScreenExitHint(conf.fullScreenExitHint.get());
            s.setIconified(conf.iconified.get());
            s.setMaximized(conf.maximized.get());

            if (pos != null) {
                s.setX(pos.x());
                s.setY(pos.y());
            }

            s.show();
        });

        BorderPane bp = new BorderPane(op);
        bp.setPadding(new Insets(10));
        bp.setBottom(FX.buttonBar(onTopButton, closeButton, null, createButton));
        return bp;
    }
}
