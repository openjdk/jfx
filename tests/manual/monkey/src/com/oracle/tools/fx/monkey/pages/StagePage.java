/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.TextChoiceOption;
import com.oracle.tools.fx.monkey.tools.CustomStage;
import com.oracle.tools.fx.monkey.util.BooleanConsumer;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.TextTemplates;

/**
 * Stage Page.
 */
public class StagePage extends TestPaneBase {
    private final ToggleButton button;
    private Stage stage;
    private final SimpleBooleanProperty alwaysOnTop = new SimpleBooleanProperty();
    private final SimpleBooleanProperty focused = new SimpleBooleanProperty();
    private final SimpleBooleanProperty fullScreen = new SimpleBooleanProperty();
    private final SimpleStringProperty fullScreenExitHint = new SimpleStringProperty();
    private final SimpleBooleanProperty iconified = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty maximized = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty maxHeight = new SimpleDoubleProperty(Double.MAX_VALUE);
    private final SimpleDoubleProperty maxWidth = new SimpleDoubleProperty(Double.MAX_VALUE);
    private final SimpleDoubleProperty minHeight = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty minWidth = new SimpleDoubleProperty(0);
    private final SimpleObjectProperty<Modality> modality = new SimpleObjectProperty<>(Modality.NONE);
    private final SimpleDoubleProperty opacity = new SimpleDoubleProperty(1.0);
    private final SimpleBooleanProperty owner = new SimpleBooleanProperty();
    private final SimpleDoubleProperty renderScaleX = new SimpleDoubleProperty(1.0);
    private final SimpleDoubleProperty renderScaleY = new SimpleDoubleProperty(1.0);
    private final SimpleBooleanProperty resizable = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty showing = new SimpleBooleanProperty();
    private final SimpleObjectProperty<StageStyle> stageStyle = new SimpleObjectProperty<>(StageStyle.DECORATED);
    private final SimpleStringProperty title = new SimpleStringProperty();

    public StagePage() {
        super("WindowPage");

        button = new ToggleButton("Show Stage");
        button.setOnAction((ev) -> {
            toggleStage();
        });

        OptionPane op = createOptionPane();

        HBox p = new HBox(4, button);
        p.setPadding(new Insets(4));

        setContent(p);
        setOptions(op);
    }

    private OptionPane createOptionPane() {
        OptionPane op = new OptionPane();

        // stage
        op.section("Stage");
        op.option(new BooleanOption("fullScreen", "full screen", fullScreen));
        op.option("Full Screen Hint:", textChoices("fullScreenHint", fullScreenExitHint));
        op.option(new BooleanOption("iconified", "iconified", iconified));
        op.option(new BooleanOption("maximized", "maximized", maximized));
        op.option("Max Height:", maxHeight("maxHeight", maxHeight));
        op.option("Max Width:", maxHeight("maxWidth", maxWidth));
        op.option("Min Height:", maxHeight("minHeight", minHeight));
        op.option("Min Width:", maxHeight("minWidth", minWidth));
        op.option(new BooleanOption("resizable", "resizable", resizable));
        op.option("Title:", textChoices("title", title));

        // init
        op.section("Stage Initialization");
        op.option(new BooleanOption("alwaysOnTop", "always on top", alwaysOnTop));
        op.option("Modality:", new EnumOption("modality", Modality.class, modality));
        op.option(new BooleanOption("owner", "set owner", owner));
        op.option("Stage Style:", new EnumOption("stageStyle", StageStyle.class, stageStyle));

        // window
        op.section("Window");
        op.option(new BooleanOption("focused", "focused", focused));
        op.option("Opacity:", opacity("opacity", opacity));
        op.option("Render Scale X:", scale("renderScaleX", renderScaleX));
        op.option("Render Scale Y:", scale("renderScaleY", renderScaleY));
        return op;
    }

    private Stage createStage() {
        Stage s = new CustomStage(stageStyle.get());

        // init
        s.setAlwaysOnTop(alwaysOnTop.get());
        s.initModality(modality.get());
        s.initOwner(owner.get() ? FX.getParentWindow(this) : null);

        // properties
        link(fullScreen, s.fullScreenProperty(), s::setFullScreen);
        // TODO fullScreenExitCombination
        s.fullScreenExitHintProperty().bindBidirectional(fullScreenExitHint);
        link(iconified, s.iconifiedProperty(), s::setIconified);
        link(maximized, s.maximizedProperty(), s::setMaximized);
        s.maxHeightProperty().bindBidirectional(maxHeight);
        s.maxWidthProperty().bindBidirectional(maxWidth);
        s.minHeightProperty().bindBidirectional(minHeight);
        s.minWidthProperty().bindBidirectional(minWidth);
        s.resizableProperty().bindBidirectional(resizable);
        // TODO scene
        s.titleProperty().bindBidirectional(title);

        // window
        link(focused, s.focusedProperty(), null);
        // TODO forceIntegerRenderScale
        // TODO height, ro
        // TODO setOnXXX
        s.opacityProperty().bindBidirectional(opacity);
        s.renderScaleXProperty().bindBidirectional(renderScaleX);
        s.renderScaleYProperty().bindBidirectional(renderScaleY);
        // TODO width, ro
        // TODO x,y
        link(showing, s.showingProperty(), null);
        return s;
    }

    private void setSceneFocused(Stage s, boolean on) {
        if (on) {
            s.requestFocus();
        }
    }

    private void setSceneShowing(Stage s, boolean on) {
        if (on) {
            s.show();
        } else {
            s.hide();
        }
    }

    private static Node maxHeight(String name, DoubleProperty p) {
        DoubleOption op = new DoubleOption(name, p);
        op.addChoice(0);
        op.addChoice(10.0);
        op.addChoice(33.3);
        op.addChoice(100.0);
        op.addChoice("Double.MAX_VALUE", Double.MAX_VALUE);
        op.addChoice("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY);
        op.addChoice("NaN", Double.NaN);
        op.selectInitialValue();
        return op;
    }

    private static Node opacity(String name, DoubleProperty p) {
        DoubleOption op = new DoubleOption(name, p);
        op.addChoice(0);
        op.addChoice(0.5);
        op.addChoice(1.0);
        op.addChoice("NaN", Double.NaN);
        op.selectInitialValue();
        return op;
    }

    private static Node scale(String name, DoubleProperty p) {
        DoubleOption op = new DoubleOption(name, p);
        op.addChoice(0);
        op.addChoice(0.5);
        op.addChoice(1.0);
        op.addChoice(2.0);
        op.addChoice("NaN", Double.NaN);
        op.selectInitialValue();
        return op;
    }

    private Node textChoices(String name, SimpleStringProperty p) {
        TextChoiceOption op = new TextChoiceOption(name, true, p);
        op.addChoice("<null>", null);
        op.addChoice("Short", "We are now full screen");
        op.addChoice("Multi-line", "One.\nTwo.\nThree.");
        op.addChoice("Lorem Impsum", TextTemplates.loremIpsum());
        return op;
    }

    private void toggleStage() {
        if (stage == null) {
            stage = createStage();
            stage.show();
            stage.showingProperty().addListener((s, p, on) -> {
                if (!on) {
                    button.setSelected(false);
                    stage = null;
                }
            });
        } else {
            stage.hide();
            stage = null;
            button.setSelected(false);
        }
    }

    private void close() {
        if (stage != null) {
            stage.hide();
            stage = null;
            button.setSelected(false);
        }
    }

    private static void link(BooleanProperty ui, ReadOnlyBooleanProperty main, BooleanConsumer c) {
        main.addListener((s, p, v) -> {
            ui.set(v);
        });
        if (c != null) {
            ui.addListener((s, p, v) -> {
                if (main.get() != v) {
                    c.consume(v);
                }
            });
            boolean val = ui.get();
            c.consume(val);
        }
    }
}
