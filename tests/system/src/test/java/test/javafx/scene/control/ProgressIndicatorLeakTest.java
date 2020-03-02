package test.javafx.scene.control;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.skin.ProgressIndicatorSkin;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProgressIndicatorLeakTest {

    static CountDownLatch startupLatch;
    static WeakReference<Node> detIndicator = null;
    static Stage stage = null;

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            ProgressIndicator indicator = new ProgressIndicator(-1);
            indicator.setSkin(new ProgressIndicatorSkin(indicator));
            Scene scene = new Scene(indicator);
            primaryStage.setScene(scene);
            stage = primaryStage;
            indicator.setProgress(1.0);
            Assert.assertTrue("size was: " + indicator.getChildrenUnmodifiable().size(), indicator.getChildrenUnmodifiable().size() == 1);
            detIndicator = new WeakReference<Node>(indicator.getChildrenUnmodifiable().get(0));
            indicator.setProgress(-1.0);
            indicator.setProgress(1.0);

            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                Platform.runLater(() -> {
                    startupLatch.countDown();
                });
            });
            primaryStage.show();
        }
    }

    @BeforeClass
    public static void initFX() {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(TestApp.class, (String[])null)).start();
        try {
            if (!startupLatch.await(15, TimeUnit.SECONDS)) {
                Assert.fail("Timeout waiting for FX runtime to start");
            }
        } catch (InterruptedException ex) {
            Assert.fail("Unexpected exception: " + ex);
        }
    }


    @Test
    public void memoryTest() throws NoSuchFieldException,IllegalAccessException {
        System.out.println("detIndicator: " + detIndicator.get());
        assertCollectable(detIndicator);
    }

    public static void assertCollectable(WeakReference weakReference) {
        int counter = 0;

        createGarbage();
        System.gc();

        while(counter < 10 && weakReference.get() != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
            counter = counter + 1;
            createGarbage();
            System.gc();
        }

        if(weakReference.get() != null) {
            throw new AssertionError("Content of WeakReference was not collected. content: " + weakReference.get());
        }
    }
    public static void createGarbage() {
        LinkedList list = new LinkedList<Integer>();
        int counter = 0;
        while(counter < 999999) {
            counter += 1;
            list.add(1);
        }
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.runLater(() -> {
            stage.hide();
            Platform.exit();
        });
    }

}
