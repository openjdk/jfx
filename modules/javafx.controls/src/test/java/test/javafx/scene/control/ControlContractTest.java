/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.SkinBase;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlContractTest {

    private ControlStub control;

    @BeforeEach
    void setup() {
        control = new ControlStub();
    }

    @AfterEach
    void cleanup() {
        control = null;
    }

    /**
     * Tests the contract of {@link SkinBase#layoutChildren(double, double, double, double)} method that is called
     * when a {@link javafx.scene.control.Control} should layout itself and his children.
     * The x, y, width and height should be snapped according to the render scale.
     * The width and height should subtract the snapped padding insets.
     *
     * @param scale the render scale
     */
    @ParameterizedTest
    @ValueSource(doubles = { 1.0, 1.25, 1.5, 1.75, 2.0 })
    void testLayoutChildrenContract(double scale) {
        DoubleProperty renderScaleProperty = new SimpleDoubleProperty(scale);

        Stage stage = new Stage();
        stage.renderScaleXProperty().bind(renderScaleProperty);
        stage.renderScaleYProperty().bind(renderScaleProperty);

        control.setPadding(new Insets(4, 5, 6, 7));

        AtomicBoolean layoutChildrenCalled = new AtomicBoolean(false);

        control.setSkin(new SkinBase<>(control) {

            @Override
            protected void layoutChildren(double x, double y, double w, double h) {
                layoutChildrenCalled.set(true);

                Insets padding = control.getPadding();

                double expectedX = control.snappedLeftInset();
                assertEquals(expectedX, x);

                expectedX = control.snapPositionX(padding.getLeft());
                assertEquals(expectedX, x);

                double expectedY = control.snappedTopInset();
                assertEquals(expectedY, y);

                expectedY = control.snapPositionY(padding.getTop());
                assertEquals(expectedY, y);

                double expectedWidth = control.snapSizeX(control.getWidth()) - control.snappedLeftInset() - control.snappedRightInset();
                assertEquals(expectedWidth, w);

                expectedWidth = control.snapSizeX(control.getWidth()) - control.snapPositionX(padding.getLeft())
                        - control.snapPositionX(padding.getRight());
                assertEquals(expectedWidth, w);

                double expectedHeight = control.snapSizeY(control.getHeight()) - control.snappedTopInset() - control.snappedBottomInset();
                assertEquals(expectedHeight, h);

                expectedHeight = control.snapSizeY(control.getHeight()) - control.snapPositionY(padding.getTop())
                        - control.snapPositionY(padding.getBottom());
                assertEquals(expectedHeight, h);
            }
        });

        Scene scene = new Scene(control);
        stage.setScene(scene);

        stage.show();

        assertTrue(layoutChildrenCalled.get());
    }

}
