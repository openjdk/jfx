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
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.skin.ToolBarSkin;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    public void overflowMenuNotShowingWithDifferentRenderScales() {
        double[] renderScales = {
            1.0,
            1.25,
            1.5,
            1.75,
            2.0,
            2.25
        };

        Rectangle rect = new Rectangle(100, 100);
        ToolBar toolBar = new ToolBar(rect);
        toolBar.setSkin(new ToolBarSkin(toolBar));

        for (var orientation : Orientation.values()) {
            toolBar.setOrientation(orientation);

            for (double scale : renderScales) {
                Stage stage = new Stage();
                stage.renderScaleXProperty().bind(DoubleConstant.valueOf(scale));
                stage.renderScaleYProperty().bind(DoubleConstant.valueOf(scale));
                stage.setScene(new Scene(new HBox(toolBar), 600, 600));
                stage.show();

                try {
                    assertOverflowNotShown(toolBar);
                } finally {
                    stage.hide();
                }
            }
        }
    }

    private static void assertOverflowNotShown(ToolBar tb) {
        Pane p = (Pane)tb.queryAccessibleAttribute(AccessibleAttribute.OVERFLOW_BUTTON);
        assertNotNull(p, "failed to obtain the overflow button");
        assertFalse(p.isVisible(), "the overflow button is expected to be hidden");
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
