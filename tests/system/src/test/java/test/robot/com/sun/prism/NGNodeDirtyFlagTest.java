/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.com.sun.prism;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import org.junit.Test;
import test.robot.testharness.VisualTestBase;
import test.util.Util;

public class NGNodeDirtyFlagTest extends VisualTestBase {

    private static final double TOLERANCE = 0.07;

    @Test
    public void testNGNodesNotDirty() throws InterruptedException {
        StackPane root = new StackPane();

        Util.runAndWait(() -> {
            Stage stage = getStage();
            stage.setScene(new Scene(root, 500, 400));
            stage.show();
        });

        ObjectProperty<Color> lineColor = new SimpleObjectProperty<>(Color.DARKGREEN);
        ObjectProperty<Color> circleColor = new SimpleObjectProperty<>(Color.DARKGREEN);

        Util.runAndWait(() -> {
            var contents = new HBox();
            contents.setSpacing(10);
            contents.setPadding(new Insets(10));
            contents.getChildren().add(contentElement("L", lineColor, circleColor));
            contents.getChildren().add(contentElement("R", lineColor, circleColor));
            root.getChildren().add(contents);

            Pane sideArea = createSideArea();
            StackPane.setAlignment(sideArea, Pos.CENTER_RIGHT);
            root.getChildren().add(sideArea);
        });

        Util.waitForIdle(root.getScene());

        for (int i = 0; i < 5; i++) {
            Util.runAndWait(() -> lineColor.set(Color.LIGHTGREEN));
            Util.waitForIdle(root.getScene());
            Util.runAndWait(() -> circleColor.set(Color.LIGHTGREEN));
            Util.waitForIdle(root.getScene());

            checkLineColor(root, lineColor.get());

            Util.runAndWait(() -> lineColor.set(Color.DARKGREEN));
            Util.waitForIdle(root.getScene());
            Util.runAndWait(() -> circleColor.set(Color.DARKGREEN));
            Util.waitForIdle(root.getScene());

            checkLineColor(root, lineColor.get());
        }
    }

    private void checkLineColor(StackPane root, Color expected) {
        Util.runAndWait(() -> {
            checkColor(root.lookup("#Line-L"), expected);
            checkColor(root.lookup("#Line-R"), expected);
        });
    }

    private void checkColor(Node node, Color expected) {
        Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
        assertColorEquals(expected, getColor((int) (screenBounds.getMinX() + 1), (int) (screenBounds.getMinY() + 1)), TOLERANCE);
    }

    private Pane contentElement(String id, ObjectProperty<Color> lineColor, ObjectProperty<Color> circleColor) {
        var group = new Group();
        group.setId(id);
        group.setManaged(false);

        double lineWidth = 220;

        var line = new Line(20, 50, lineWidth, 50);
        line.setId("Line-" + id);
        line.setStrokeWidth(4);
        lineColor.addListener((ov, o, n) -> line.setStroke(n));
        group.getChildren().add(line);

        var circle = new Circle(5);
        circle.setCenterX(lineWidth + 20);
        circle.setCenterY(50);
        circle.setId("Circle-" + id);
        circleColor.addListener((ov, o, n) -> circle.setFill(n));
        circle.setFill(Color.LIGHTGREEN);
        group.getChildren().add(circle);

        var result = new StackPane(group);
        result.setId(id);
        result.setStyle("-fx-background-color: lightgrey; -fx-border-color: black; -fx-border-width: 1;");
        result.setMinSize(lineWidth + 40, 200);

        return result;
    }

    private Pane createSideArea() {
        VBox result = new VBox();
        result.setPrefSize(150, 9900);
        result.setMaxWidth(200);
        result.setStyle("-fx-background-color: lightblue;");
        result.getChildren().add(new Label("SideArea"));
        return result;
    }
}
