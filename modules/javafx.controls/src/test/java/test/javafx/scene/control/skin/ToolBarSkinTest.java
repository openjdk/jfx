/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.skin.ToolBarSkin;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.binding.DoubleConstant;

/**
 * Tests the ToolBarSkin.
 */
public class ToolBarSkinTest {
    private ToolBar toolbar;
    private ToolBarSkinMock skin;

    @BeforeEach
    public void setup() {
        toolbar = new ToolBar();
        toolbar.getItems().addAll(new Button("Cut"), new Button("Copy"));
        skin = new ToolBarSkinMock(toolbar);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        toolbar.setPadding(new Insets(10, 10, 10, 10));
        toolbar.setSkin(skin);
    }

    @Test
    public void horizontalMaxHeightTracksPreferred() {
        toolbar.setOrientation(Orientation.HORIZONTAL);
        toolbar.setPrefHeight(100);
        assertEquals(100, toolbar.maxHeight(-1), 0);
    }

    @Test
    public void verticalMaxWidthTracksPreferred() {
        toolbar.setOrientation(Orientation.VERTICAL);
        toolbar.setPrefWidth(100);
        assertEquals(100, toolbar.maxWidth(-1), 0);
    }

    // Tests that floating-point precision errors do not trigger the overflow menu. For example, a calculation may yield
    // 117.60000000000001, which should not be treated as greater than 117.6 when determining whether there is
    // sufficient space.
    @ParameterizedTest
    @MethodSource("renderScalesAndOrientations")
    public void overflowMenuNotShowingDueToFloatPrecisionErrors(double scale, Orientation orientation) {
        // These pref height/width values can cause getOverflowNodeIndex() to compute a combined
        // child length of 117.60000000000001, while the toolbar length is 117.6 (for both horizontal
        // and vertical orientations). This would cause the overflow node to display unnecessarily, unless snapping
        // (e.g., snapPositionX/Y()) is applied correctly; which is what this test verifies.
        double buttonPrefDimension = 99.2;
        double separatorPrefDimension = 10.4;

        Button btn = new Button("123456 123456");
        btn.setPrefWidth(buttonPrefDimension);
        btn.setPrefHeight(buttonPrefDimension);

        // Set up a separator in opposite orientation
        Separator sep = new Separator(orientation == Orientation.VERTICAL ? Orientation.HORIZONTAL
                : Orientation.VERTICAL);
        sep.setPrefWidth(separatorPrefDimension);
        sep.setPrefHeight(separatorPrefDimension);

        ToolBar toolBar = new ToolBar(sep, btn);
        toolBar.setOrientation(orientation);

        BorderPane bp = new BorderPane();
        bp.setTop(new HBox(toolBar));

        assertOverflowNotShown(bp, toolBar, scale);
    }

    @ParameterizedTest
    @MethodSource("renderScalesAndOrientations")
    public void overflowMenuNotShowingWithDifferentRenderScales(double scale, Orientation orientation) {
        Rectangle rect = new Rectangle(100, 100);
        ToolBar toolBar = new ToolBar(rect);
        toolBar.setSkin(new ToolBarSkin(toolBar));
        toolBar.setOrientation(orientation);

        assertOverflowNotShown(new HBox(toolBar), toolBar, scale);
    }

    private static Stream<Arguments> renderScalesAndOrientations() {
        return Stream.of(1.0, 1.25, 1.5, 1.75, 2.0, 2.25)
            .flatMap(scale -> Stream.of(Orientation.values())
                .map(orientation -> Arguments.of(scale, orientation)));
    }

    private static void assertOverflowNotShown(Parent rootNode, ToolBar tb, double scale) {
        Stage stage = new Stage();
        stage.renderScaleXProperty().bind(DoubleConstant.valueOf(scale));
        stage.renderScaleYProperty().bind(DoubleConstant.valueOf(scale));
        stage.setScene(new Scene(rootNode, 600, 600));
        stage.show();

        try {
            Pane p = (Pane) tb.queryAccessibleAttribute(AccessibleAttribute.OVERFLOW_BUTTON);
            assertNotNull(p, "failed to obtain the overflow button");
            assertFalse(p.isVisible(), "the overflow button is expected to be hidden");
        } finally {
            stage.hide();
        }
    }

    public static final class ToolBarSkinMock extends ToolBarSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ToolBarSkinMock(ToolBar toolbar) {
            super(toolbar);
        }

        public void addWatchedProperty(ObservableValue<?> p) {
            p.addListener(o -> {
                propertyChanged = true;
                propertyChangeCount++;
            });
        }
    }
}
