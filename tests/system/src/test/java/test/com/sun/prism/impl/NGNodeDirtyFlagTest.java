package test.com.sun.prism.impl;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.image.WritableImage;
import javafx.scene.robot.Robot;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import test.util.Util;

import static org.junit.Assert.assertEquals;

public class NGNodeDirtyFlagTest {

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Singleton Application instance
    static MyApp myApp;

    private CountDownLatch latch = new CountDownLatch(1);

    public static class MyApp extends Application {

        private StackPane root;

        public MyApp() {
            super();
        }

        @Override
        public void init() {
            myApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            root = new StackPane();
            primaryStage.setScene(new Scene(root, 500, 400));
            primaryStage.show();

            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void setupOnce() {
        Util.launch(launchLatch, MyApp.class);
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Test
    public void testNGNodesNotDirty() throws InterruptedException {
        ObjectProperty<Color> lineColor = new SimpleObjectProperty<>(Color.DARKGREEN);
        ObjectProperty<Color> circleColor = new SimpleObjectProperty<>(Color.DARKGREEN);

        StackPane root = myApp.root;

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
        Robot robot = new Robot();
        Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
        WritableImage image = robot.getScreenCapture(null, screenBounds.getMinX(), screenBounds.getMinY(), 100, 100);
        Assert.assertEquals("A node was not rendered properly. Wrong color found", expected, image.getPixelReader().getColor(1, 1));
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
