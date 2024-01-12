package test.robot.javafx.scene;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.stage.Stage;

import org.junit.Test;
import test.robot.testharness.VisualTestBase;
import test.util.Util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static test.util.Util.TIMEOUT;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SnapshotLightsTest extends VisualTestBase {

    static final int BOX_DIM = 50;

    private Scene buildScene(boolean inSubScene) {
        Box boxNode = new Box(BOX_DIM, BOX_DIM, BOX_DIM - 10);
        boxNode.setMaterial(new PhongMaterial(Color.WHITE));

        StackPane pane = new StackPane(boxNode);
        pane.setAlignment(Pos.CENTER);

        PointLight light = new PointLight(Color.BLUE);
        light.setTranslateZ(-150);
        pane.getChildren().add(light);

        if (inSubScene) {
            SubScene ss = new SubScene(pane, BOX_DIM, BOX_DIM);
            StackPane subSceneRoot = new StackPane(ss);
            subSceneRoot.setAlignment(Pos.CENTER);
            return new Scene(subSceneRoot, BOX_DIM, BOX_DIM);
        } else {
            return new Scene(pane, BOX_DIM, BOX_DIM);
        }
    }

    private void compareSnapshots(WritableImage base, WritableImage node) {
        assertEquals(base.getWidth(), node.getWidth(), 0.1);
        assertEquals(base.getHeight(), node.getHeight(), 0.1);

        PixelReader baseReader = base.getPixelReader();
        PixelReader nodeReader = node.getPixelReader();


        assertEquals(baseReader.getArgb(BOX_DIM / 2, BOX_DIM / 2), nodeReader.getArgb(BOX_DIM / 2, BOX_DIM / 2));
    }

    public SnapshotLightsTest() {
    }

    private Scene scene;

    @Test
    public void testSceneNodeSnapshotLighting() throws Exception {
        final CountDownLatch stageShownLatch = new CountDownLatch(1);

        runAndWait(() -> {
            Stage stage = getStage(false);

            scene = buildScene(false);
            stage.setScene(scene);
            stage.setOnShown(e -> Platform.runLater(stageShownLatch::countDown));
            stage.show();
        });

        assertTrue("Timeout waiting for stage to be shown",
            stageShownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

        Util.runAndWait(() -> {
            WritableImage baseSnapshot = scene.snapshot(null);

            Node boxNode = scene.getRoot().getChildrenUnmodifiable().get(0);
            WritableImage nodeSnapshot = boxNode.snapshot(null, null);

            compareSnapshots(baseSnapshot, nodeSnapshot);
        });
    }

    @Test
    public void testSubSceneNodeSnapshotLighting() throws Exception {
        final CountDownLatch stageShownLatch = new CountDownLatch(1);

        runAndWait(() -> {
            Stage stage = getStage(false);

            scene = buildScene(true);
            stage.setScene(scene);
            stage.setOnShown(e -> Platform.runLater(stageShownLatch::countDown));
            stage.show();
        });

        assertTrue("Timeout waiting for stage to be shown",
            stageShownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));

        Util.runAndWait(() -> {
            WritableImage baseSnapshot = scene.snapshot(null);

            SubScene ss = (SubScene)scene.getRoot().getChildrenUnmodifiable().get(0);
            Node boxNode = ss.getRoot().getChildrenUnmodifiable().get(0);
            WritableImage nodeSnapshot = boxNode.snapshot(null, null);

            compareSnapshots(baseSnapshot, nodeSnapshot);
        });

    }
}
