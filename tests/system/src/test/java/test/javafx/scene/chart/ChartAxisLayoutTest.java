package test.javafx.scene.chart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class ChartAxisLayoutTest {

    private static CountDownLatch startupLatch;
    private static Stage stage;
    private static VBox rootPane;
    private static CategoryAxis xAxis;

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            stage = primaryStage;
            rootPane = new VBox();
            stage.setScene(new Scene(rootPane, 600, 800));

            var categories = List.of(
                    "1st very long category name..............",
                    "2nd very long category name..............",
                    "3rd very long category name..............",
                    "4th very long category name..............",
                    "5th very long category name.............."
            );

            xAxis = new CategoryAxis();
            xAxis.setCategories(FXCollections.observableList(categories));

            BarChart<String, Number> chart = new BarChart<>(xAxis, new NumberAxis());
            chart.prefWidthProperty().bind(rootPane.widthProperty());
            chart.prefHeightProperty().bind(rootPane.heightProperty());
            chart.setAnimated(false);
            rootPane.getChildren().add(chart);

            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                Platform.runLater(() -> startupLatch.countDown());
            });
            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() throws Exception {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(ChartAxisLayoutTest.TestApp.class, (String[]) null)).start();

        assertTrue("Timeout waiting for FX runtime to start", startupLatch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void ensureThatLabelsAreAutoRotated() {
        Util.runAndWait(() -> assertEquals(90.0, xAxis.getTickLabelRotation(), 0.0001));
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.runLater(() -> {
            stage.hide();
            Platform.exit();
        });
    }
}
