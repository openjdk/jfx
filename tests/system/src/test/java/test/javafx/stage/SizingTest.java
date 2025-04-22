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
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
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
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT"})
    void testMaximizeUnresizable(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setWidth(WIDTH);
            s.setHeight(HEIGHT);
            s.setResizable(false);
        });
        Util.runAndWait(() -> getStage().setMaximized(true));
        Util.sleep(500);

        assertTrue(getStage().isMaximized(), "Unresizable stage should be maximized");
        assertTrue(getStage().getWidth() > WIDTH, "Stage width should be maximized");
        assertTrue(getStage().getHeight() > HEIGHT, "Stage height should be maximized");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT"})
    void testFullscreenUnresizable(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setWidth(WIDTH);
            s.setHeight(HEIGHT);
            s.setResizable(false);
        });

        Util.runAndWait(() -> getStage().setFullScreen(true));
        Util.sleep(500);
        assertTrue(getStage().isFullScreen(), "Unresizable stage should be fullscreen");
        assertTrue(getStage().getWidth() > WIDTH, "Stage width should be fullscreen");
        assertTrue(getStage().getHeight() > HEIGHT, "Stage height should be fullscreen");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT", "UTILITY"})
    void testResizeUnresizable(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setWidth(WIDTH);
            s.setHeight(HEIGHT);
            s.setResizable(false);
        });

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });
        Util.sleep(500);

        assertEquals(NEW_WIDTH, getStage().getWidth(), "Stage should have resized");
        assertEquals(NEW_HEIGHT, getStage().getHeight(), "Stage should have resized");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT"})
    void testMaximizeMaxSize(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setWidth(WIDTH);
            s.setHeight(HEIGHT);
            s.setMaxWidth(MAX_WIDTH);
            s.setMaxHeight(MAX_HEIGHT);
        });

        Util.doTimeLine(500,
                () -> getStage().setMaximized(true),
                () -> getStage().isMaximized(),
                () -> {
                    assertTrue(getStage().getWidth() > MAX_WIDTH, "Stage width should be maximized");
                    assertTrue(getStage().getHeight() > MAX_HEIGHT, "Stage width should be maximized");
                },
                () -> getStage().setMaximized(false));

        Util.sleep(500);

        assertEquals(WIDTH, getStage().getWidth(), "Stage width should have been restored");
        assertEquals(HEIGHT, getStage().getHeight(), "Stage height should have been restored");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT"})
    void testFullScreenMaxSize(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setWidth(WIDTH);
            s.setHeight(HEIGHT);
            s.setMaxWidth(MAX_WIDTH);
            s.setMaxHeight(MAX_HEIGHT);
        });

        Util.doTimeLine(500,
                () -> getStage().setFullScreen(true),
                () -> getStage().isFullScreen(),
                () -> {
                    assertTrue(getStage().getWidth() > MAX_WIDTH, "Stage width should be maximized");
                    assertTrue(getStage().getHeight() > MAX_HEIGHT, "Stage width should be maximized");
                },
                () -> getStage().setFullScreen(false));

        Util.sleep(500);

        assertEquals(WIDTH, getStage().getWidth(), "Stage width should have been restored");
        assertEquals(HEIGHT, getStage().getHeight(), "Stage height should have been restored");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT", "UTILITY"})
    void testMaxSize(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setMaxWidth(MAX_WIDTH);
            s.setMaxHeight(MAX_HEIGHT);
        });

        Util.sleep(500);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(500);

        assertEquals(MAX_WIDTH, getStage().getWidth(), "Stage width should have been limited to max width");
        assertEquals(MAX_HEIGHT, getStage().getHeight(), "Stage height should have been limited to max height");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT", "UTILITY"})
    void testMaxWidth(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setMaxWidth(MAX_WIDTH);
        });

        Util.sleep(500);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(500);

        assertEquals(MAX_WIDTH, getStage().getWidth(), "Stage width should have been limited to max width");
        assertEquals(NEW_HEIGHT, getStage().getHeight(), "Only max width should be limited");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT", "UTILITY"})
    void testMaxHeight(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setMaxHeight(MAX_HEIGHT);
        });

        Util.sleep(500);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(500);

        assertEquals(NEW_WIDTH, getStage().getWidth(), "Only max height should be limited");
        assertEquals(MAX_HEIGHT, getStage().getHeight(), "Stage height should have been limited to max height");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT", "UTILITY"})
    void testMinSize(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setMinWidth(MIN_WIDTH);
            s.setMinHeight(MIN_HEIGHT);
        });

        Util.sleep(500);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(500);

        assertEquals(MIN_WIDTH, getStage().getWidth(), "Stage width should have been limited to min width");
        assertEquals(MIN_HEIGHT, getStage().getHeight(),  "Stage height should have been limited to min height");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT", "UTILITY"})
    void testMinWidth(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setMinWidth(MIN_WIDTH);
        });

        Util.sleep(500);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(500);

        assertEquals(MIN_WIDTH, getStage().getWidth(), "Stage width should have been limited to min width");
        assertEquals(NEW_HEIGHT, getStage().getHeight(),  "Only min width should be limited");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT", "UTILITY"})
    void testMinHeight(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setMinHeight(MIN_HEIGHT);
        });

        Util.sleep(500);

        Util.runAndWait(() -> {
            getStage().setWidth(NEW_WIDTH);
            getStage().setHeight(NEW_HEIGHT);
        });

        Util.sleep(500);

        assertEquals(NEW_WIDTH, getStage().getWidth(),  "Only min height should be limited");
        assertEquals(MIN_HEIGHT, getStage().getHeight(), "Stage height should have been limited to min height");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT", "UTILITY"})
    void testNoSize(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> s.initStyle(stageStyle));

        Util.sleep(500);

        assertTrue(getStage().getWidth() > 1, "Stage width should be greater than 1");
        assertTrue(getStage().getHeight() > 1, "Stage height should be greater than 1");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT", "UTILITY"})
    void testNoHeight(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setWidth(WIDTH);
        });

        Util.sleep(500);

        assertEquals(WIDTH, getStage().getWidth(), "Stage do not match the set width");
        assertTrue(getStage().getHeight() > 1, "Stage height should be greater than 1");
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DISPLAY)
    @EnumSource(value = StageStyle.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"DECORATED", "UNDECORATED", "TRANSPARENT", "UTILITY"})
    void testNoWidth(StageStyle stageStyle) {
        setupStageWithStyle(stageStyle, s -> {
            s.initStyle(stageStyle);
            s.setHeight(HEIGHT);
        });

        Util.sleep(500);

        assertTrue(getStage().getWidth() > 1, "Stage width should be greater than 1");
        assertEquals(HEIGHT, getStage().getHeight(), "Stage do not match the set height");
    }
}
