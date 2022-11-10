/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.scene.layout;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the snapping of all container inside {@link javafx.scene.layout}.
 * Containers must always snap their width/height as well as their insets, otherwise the children may look blurry or
 * have other side effects. This also ensures that the containers and their children have the correct position and size
 * on all different render scales.
 *
 * @see Stage#setRenderScaleX(double)
 * @see Stage#setRenderScaleY(double)
 * @see Region#snapPositionX(double)
 * @see Region#snapPositionY(double)
 */
class SnappingTest {

    private static final double EPSILON = 0.00001;

    private Stage stage;

    @AfterEach
    void tearDown() {
        if (stage != null) {
            stage.hide();
            stage = null;
        }
    }

    @ParameterizedTest
    @MethodSource(value = { "getContainerCreators" })
    void testContainerSnappingScale100(ContainerCreator<Region> containerCreator) {
        testContainerSnappingImpl(containerCreator, 1);
    }

    @ParameterizedTest
    @MethodSource(value = { "getContainerCreators" })
    void testContainerSnappingScale125(ContainerCreator<Region> containerCreator) {
        testContainerSnappingImpl(containerCreator, 1.25);
    }

    @ParameterizedTest
    @MethodSource(value = { "getContainerCreators" })
    void testContainerSnappingScale150(ContainerCreator<Region> containerCreator) {
        testContainerSnappingImpl(containerCreator, 1.5);
    }

    @ParameterizedTest
    @MethodSource(value = { "getContainerCreators" })
    void testContainerSnappingScale175(ContainerCreator<Region> containerCreator) {
        testContainerSnappingImpl(containerCreator, 1.75);
    }

    @ParameterizedTest
    @MethodSource(value = { "getContainerCreators" })
    void testContainerSnappingScale200(ContainerCreator<Region> containerCreator) {
        testContainerSnappingImpl(containerCreator, 2);
    }

    private void testContainerSnappingImpl(ContainerCreator<Region> containerCreator, double scale) {
        double widthHeight = 100;
        double padding = 9.6;

        Region child = new Region();
        child.setMinWidth(widthHeight);
        child.setMinHeight(widthHeight);
        child.setPrefWidth(widthHeight);
        child.setPrefHeight(widthHeight);
        child.setMaxWidth(widthHeight);
        child.setMaxHeight(widthHeight);

        Region container = containerCreator.apply(child);
        container.setStyle("-fx-padding: " + padding + "px;");

        DoubleProperty renderScaleProperty = new SimpleDoubleProperty(scale);

        stage = new Stage();
        stage.renderScaleXProperty().bind(renderScaleProperty);
        stage.renderScaleYProperty().bind(renderScaleProperty);

        Scene scene = new Scene(container, widthHeight, widthHeight);
        stage.setScene(scene);
        stage.show();

        double snappedPaddingX = container.snapPositionX(padding) * 2;
        double snappedPaddingY = container.snapPositionY(padding) * 2;

        // Special case: The min width/height of the Pane is only the padding
        String className = container.getClass().getSimpleName();
        if (container.getClass() == Pane.class) {
            assertEquals(snappedPaddingX, container.minWidth(-1), EPSILON, className);
            assertEquals(snappedPaddingY, container.minHeight(-1), EPSILON, className);
        } else {
            assertEquals(widthHeight + snappedPaddingX, container.minWidth(-1), EPSILON, className);
            assertEquals(widthHeight + snappedPaddingY, container.minHeight(-1), EPSILON, className);
        }

        assertEquals(widthHeight + snappedPaddingX, container.prefWidth(-1), EPSILON, className);
        assertEquals(widthHeight + snappedPaddingY, container.prefHeight(-1), EPSILON, className);
    }

    static Stream<ContainerCreator<?>> getContainerCreators() {
        // TODO: Create issues and fix snapping for all commented out layout containers below.
        // The issues should be linked to JDK-8296609
        // Note that the working layout containers do not necessarily use the optimized snappedXXXInsets() methods,
        // but instead snap the insets (again). This can be optimized as well.
        return Stream.of(
                new ContainerCreator<>(HBox::new),
                new ContainerCreator<>(VBox::new),
                new ContainerCreator<>(node -> {
                    GridPane gridPane = new GridPane();
                    gridPane.getChildren().add(node);
                    return gridPane;
                })
//                new ContainerCreator<>(Pane::new),
//                new ContainerCreator<>(StackPane::new),
//                new ContainerCreator<>(BorderPane::new),
//                new ContainerCreator<>(node -> {
//                    TilePane tilePane = new TilePane(node);
//                    tilePane.setPrefColumns(1);
//                    return tilePane;
//                }),
//                new ContainerCreator<>(AnchorPane::new), // fixed by JDK-8295078
//                new ContainerCreator<>(node -> {
//                    FlowPane flowPane = new FlowPane(node);
//                    flowPane.setPrefWrapLength(0);
//                    return flowPane;
//                })
        );
    }

    private record ContainerCreator<S extends Region>(Function<Node, S> containerCreatorFunction) {
        public S apply(Node child) {
            return containerCreatorFunction().apply(child);
        }
    }

}
