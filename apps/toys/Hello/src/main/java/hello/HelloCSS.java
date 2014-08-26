/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.WritableValue;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class HelloCSS extends Application {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello CSS");
        Scene scene = new Scene(new Group(), 600, 450);
        scene.setFill(Color.LIGHTGREEN);
        scene.getStylesheets().add("hello/hello.css");
        Rectangle rect = new Rectangle();
        rect.getStyleClass().add("rect");
        rect.setX(25);
        rect.setY(40);
        rect.setWidth(100);
        rect.setHeight(50);
        rect.setFill(Color.GREEN);
        Rectangle rect2 = new Rectangle();
        rect2.getStyleClass().add("rect");
        rect2.setX(135);
        rect2.setY(40);
        rect2.setWidth(100);
        rect2.setHeight(50);
        rect2.setStyle(
                "-fx-stroke: yellow;"
              + "-fx-stroke-width: 3;"
              + "-fx-stroke-dash-array: 5 7;"
        );

        Node swapTest = createSwapTest();
        swapTest.setLayoutX(25);
        swapTest.setLayoutY(110);

        VBox subSceneRoot = new VBox(5, new Label("Caspian style button in a SubScene"), new Button("CASPIAN"));
        subSceneRoot.setStyle("-fx-border-color: white; -fx-alignment: center;");
        SubScene caspianSubScene = new SubScene(subSceneRoot, 300, 100);
        caspianSubScene.setUserAgentStylesheet("com/sun/javafx/scene/control/skin/caspian/caspian.css");

        caspianSubScene.setLayoutX(275);
        caspianSubScene.setLayoutY(40);

        Node durationTest = createDurationTest();
        durationTest.setLayoutX(25);
        durationTest.setLayoutY(210);

        ((Group)scene.getRoot()).getChildren().addAll(rect,rect2,swapTest,caspianSubScene, durationTest);
        stage.setScene(scene);
        stage.show();
    }

    private Node createSwapTest() {

        final StackPane r1 = new StackPane();
        r1.setPrefSize(100,50);
        r1.setStyle("-fx-base: red; -fx-border-color: red;");

        final StackPane r2 = new StackPane();
        r2.setPrefSize(100,50);
        r2.setStyle("-fx-base: yellow; -fx-border-color: yellow;");

        final Button swapButton = new Button("Move");
        swapButton.setOnAction(actionEvent -> {
            if (swapButton.getParent() == r1) {
                r1.getChildren().remove(swapButton);
                r2.getChildren().add(swapButton);
            } else if (swapButton.getParent() == r2) {
                r2.getChildren().remove(swapButton);
                r1.getChildren().add(swapButton);
            }
        });
        r1.getChildren().add(swapButton);

        FlowPane hBox = new FlowPane(Orientation.HORIZONTAL, 5, 5);
        hBox.getChildren().addAll(r1, r2, new Text("Click button to move.\nButton's base color should match surrounding border."));

        return hBox;
    }

    private static class TestNode extends Rectangle {

        public TestNode() {
            super(100, 100);
        }

        @Override public void impl_processCSS(WritableValue<Boolean> foo) {
            super.impl_processCSS(foo);
        }
        StyleablePropertyFactory<TestNode> factory = new StyleablePropertyFactory<>(Rectangle.getClassCssMetaData());
        StyleableProperty<Duration> myDuration = factory.createStyleableDurationProperty(this, "myDuration", "-my-duration", (s) -> s.myDuration, Duration.millis(1000));

        @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
            return factory.getCssMetaData();
        }
    }


    BooleanProperty fadeIn = new SimpleBooleanProperty(false);
    private Node createDurationTest() {

        final BorderPane pane = new BorderPane();
        pane.setStyle("-fx-border-color: blue;");
        pane.setPadding(new Insets(10,10,10,10));

        Slider slider = new Slider();
        slider.setPadding(new Insets(5,5,5,10));
        slider.setMin(500d);
        slider.setMax(1500d);
        slider.setBlockIncrement(50);
        slider.setValue(1000d);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);
        slider.setOrientation(Orientation.VERTICAL);

        pane.setRight(slider);

        final TestNode testNode = new TestNode();
        slider.valueProperty().addListener(o -> testNode.setStyle("-my-duration: " + ((Property<Number>)o).getValue().intValue() + "ms;"));

        final Button fadeButton = new Button();
        fadeButton.textProperty().bind(Bindings.when(fadeIn).then("Fade In").otherwise("Fade Out"));
        fadeButton.setOnAction(e -> {
            Duration duration = testNode.myDuration.getValue();
            FadeTransition transition = new FadeTransition(duration, testNode);
            transition.setFromValue(testNode.getOpacity());
            transition.statusProperty().addListener(o -> {
                if (((ReadOnlyObjectProperty<Animation.Status>) o).getValue() == Animation.Status.STOPPED) {
                    fadeButton.setDisable(false);
                } else {
                    fadeButton.setDisable(true);
                }
            });
            if (fadeIn.get()) {
                transition.setToValue(1.0);
                transition.setByValue(5);
                transition.setOnFinished(a -> fadeIn.set(false));
            } else {
                transition.setToValue(0.1);
                transition.setByValue(-5);
                transition.setOnFinished(a -> fadeIn.set(true));
            }
            transition.playFromStart();
        });

        VBox vbox = new VBox(5, testNode, fadeButton);
        vbox.setAlignment(Pos.CENTER);
        pane.setCenter(vbox);

        Label label = new Label("Use slider to adjust duration of the\nFadeTransition, then click the button.");
        pane.setTop(label);

        Label status = new Label();
        status.textProperty().bind(Bindings.createStringBinding(
                () -> testNode.myDuration.getValue().toString(),
                (ObjectProperty<Duration>)testNode.myDuration
        ));
        pane.setBottom(status);

        BorderPane.setAlignment(label, Pos.CENTER);
        BorderPane.setAlignment(slider, Pos.CENTER);
        BorderPane.setAlignment(vbox, Pos.CENTER);
        BorderPane.setAlignment(status, Pos.BOTTOM_RIGHT);

        return pane;
    }

}
