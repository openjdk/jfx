/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.stage;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.util.Util.PARAMETERIZED_TEST_DISPLAY;

class SizingTest extends StageTestBase {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;
    private static final int MAX_WIDTH = 350;
    private static final int MAX_HEIGHT = 350;
    private static final int MIN_WIDTH = 500;
    private static final int MIN_HEIGHT = 500;
    private static final int NEW_WIDTH = 450;
    private static final int NEW_HEIGHT = 450;

    protected Label createLabel(String prefix, ReadOnlyDoubleProperty property) {
        Label label = new Label();
        label.textProperty().bind(Bindings.concat(prefix, Bindings.convert(property)));
        return label;
    }

    @Override
    protected Region createRoot() {
        VBox vBox = new VBox(createLabel("Width: ", getStage().widthProperty()),
                createLabel("Height: ", getStage().heightProperty()),
                createLabel("Max Width: ", getStage().maxWidthProperty()),
                createLabel("Max Height: ", getStage().maxHeightProperty()),
                createLabel("Min Width: ", getStage().minWidthProperty()),
                createLabel("Min Height: ", getStage().minHeightProperty()));
        vBox.setBackground(Background.EMPTY);

        return vBox;
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT", "UTILITY"})
    void maxSize(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.setMaxWidth(MAX_WIDTH);
            s.setMaxHeight(MAX_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        assertEquals(MAX_WIDTH, getStage().getWidth(), SIZING_DELTA,
                "Stage width should have been limited to max width");
        assertEquals(MAX_HEIGHT, getStage().getHeight(), SIZING_DELTA,
                "Stage height should have been limited to max height");

        // Reset it
        Util.runAndWait(() -> {
            getStage().setMaxWidth(Double.MAX_VALUE);
            getStage().setMaxHeight(Double.MAX_VALUE);
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        assertEquals(NEW_WIDTH, getStage().getWidth(), SIZING_DELTA,
                "Stage width should have been accepted after removing min width");
        assertEquals(NEW_HEIGHT, getStage().getHeight(), SIZING_DELTA,
                "Stage height should have been accepted after removing min height");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT", "UTILITY"})
    void maxWidth(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setMaxWidth(MAX_WIDTH);
        });

        Util.sleep(MEDIUM_WAIT);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        assertEquals(MAX_WIDTH, getStage().getWidth(), SIZING_DELTA,
                "Stage width should have been limited to max width");
        assertEquals(NEW_HEIGHT, getStage().getHeight(), SIZING_DELTA, "Only max width should be limited");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT", "UTILITY"})
    void maxHeight(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> s.setMaxHeight(MAX_HEIGHT));

        Util.sleep(MEDIUM_WAIT);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        assertEquals(NEW_WIDTH, getStage().getWidth(), SIZING_DELTA, "Only max height should be limited");
        assertEquals(MAX_HEIGHT, getStage().getHeight(), SIZING_DELTA,
                "Stage height should have been limited to max height");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT", "UTILITY"})
    void minSize(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.setMinWidth(MIN_WIDTH);
            s.setMinHeight(MIN_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        assertEquals(MIN_WIDTH, getStage().getWidth(), SIZING_DELTA,
                "Stage width should have been limited to min width");
        assertEquals(MIN_HEIGHT, getStage().getHeight(), SIZING_DELTA,
                "Stage height should have been limited to min height");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT", "UTILITY"})
    void minWidth(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> s.setMinWidth(MIN_WIDTH));

        Util.sleep(MEDIUM_WAIT);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        assertEquals(MIN_WIDTH, getStage().getWidth(), SIZING_DELTA,
                "Stage width should have been limited to min width");
        assertEquals(NEW_HEIGHT, getStage().getHeight(), SIZING_DELTA, "Only min width should be limited");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT", "UTILITY"})
    void minHeight(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> s.setMinHeight(MIN_HEIGHT));

        Util.sleep(MEDIUM_WAIT);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        assertEquals(NEW_WIDTH, getStage().getWidth(), SIZING_DELTA, "Only min height should be limited");
        assertEquals(MIN_HEIGHT, getStage().getHeight(), SIZING_DELTA,
                "Stage height should have been limited to min height");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT", "UTILITY"})
    void noSize(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, null);

        Util.sleep(MEDIUM_WAIT);

        assertTrue(getStage().getWidth() > 1, "Stage width should be greater than 1");
        assertTrue(getStage().getHeight() > 1, "Stage height should be greater than 1");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT", "UTILITY"})
    void noHeight(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> s.setWidth(WIDTH));

        Util.sleep(MEDIUM_WAIT);

        assertEquals(WIDTH, getStage().getWidth(), SIZING_DELTA, "Stage do not match the set width");
        assertTrue(getStage().getHeight() > 1, "Stage height should be greater than 1");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(names = {"DECORATED", "UNDECORATED", "EXTENDED", "TRANSPARENT", "UTILITY"})
    void noWidth(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> s.setHeight(HEIGHT));

        Util.sleep(MEDIUM_WAIT);

        assertTrue(getStage().getWidth() > 1, "Stage width should be greater than 1");
        assertEquals(HEIGHT, getStage().getHeight(), SIZING_DELTA, "Stage do not match the set height");
    }

    @Test
    void sceneSizeOnly() {
        setupStageWithStyle(StageStyle.DECORATED, s -> s.setScene(new Scene(new StackPane(), WIDTH, HEIGHT)));

        Util.sleep(MEDIUM_WAIT);

        assertEquals(WIDTH, getScene().getWidth(), SIZING_DELTA,
                "Scene width should not be affected by decoration if stage width not set");
        assertEquals(HEIGHT, getScene().getHeight(), SIZING_DELTA,
                "Scene height should not be affected by decoration if stage height not set");
    }

    @Test
    void sceneWidthWithWindowHeight() {
        setupStageWithStyle(StageStyle.DECORATED, s -> {
            s.setScene(new Scene(new StackPane(), WIDTH, HEIGHT));
            s.setHeight(NEW_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        assertEquals(WIDTH, getScene().getWidth(),
                "Scene width should not be affected by decoration if stage width not set");
        assertEquals(NEW_HEIGHT, getStage().getHeight(), SIZING_DELTA, "Stage height should match the new height");
    }

    @Test
    void sceneHeightWithWindowWidth() {
        setupStageWithStyle(StageStyle.DECORATED, s -> {
            s.setScene(new Scene(new StackPane(), WIDTH, HEIGHT));
            s.setWidth(NEW_WIDTH);
        });

        Util.sleep(MEDIUM_WAIT);

        assertEquals(NEW_WIDTH, getStage().getWidth(), SIZING_DELTA, "Stage with should match the set new width");
        assertEquals(HEIGHT, getScene().getHeight(),
                "Scene height should not be affected by decoration if stage height not set");
    }

    @Test
    void sceneSizeThenStageSize() {
        setupStageWithStyle(StageStyle.DECORATED, s -> s.setScene(new Scene(new StackPane(), WIDTH, HEIGHT)));

        Util.sleep(MEDIUM_WAIT);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(MEDIUM_WAIT);

        assertEquals(NEW_WIDTH, getStage().getWidth(), SIZING_DELTA,
                "Scene width should match the new stage width");
        assertEquals(NEW_HEIGHT, getStage().getHeight(), SIZING_DELTA,
                "Scene height should match the new stage height");

        Util.runAndWait(() -> getStage().setScene(new Scene(new StackPane(), WIDTH, HEIGHT)));

        Util.sleep(MEDIUM_WAIT);

        assertEquals(NEW_WIDTH, getStage().getWidth(), SIZING_DELTA,
                "Scene width should remain unchanged after setting a new scene");
        assertEquals(NEW_HEIGHT, getStage().getHeight(), SIZING_DELTA,
                "Scene height should remain unchanged after setting a new scene");
    }
}
