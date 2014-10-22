package javafx.scene.control;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class BlockingDialogTest extends Application {

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        launch(args);
    }

    @Override public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Primary stage");
        Group root = new Group();
        Scene scene = new Scene(root);
        scene.setFill(Color.LIGHTYELLOW);
        primaryStage.setScene(scene);
        primaryStage.setX(0);
        primaryStage.setY(0);
        primaryStage.setWidth(210);
        primaryStage.setHeight(180);

        launchLatch.countDown();

        JUnitCore junit = new JUnitCore();
        AlertTest.blocking = true;
        Result result = junit.run(AlertTest.class);
        System.out.println("Blocking dialog test result: " +
                (result.wasSuccessful() ?
                        "SUCCESS (" + result.getRunCount() + " tests run)" :
                        "FAILED (" + result.getFailureCount() + " / " + result.getRunCount() + " failures)"));
        if (!result.wasSuccessful()) {
            System.out.println("Blocking dialog tests failed, for the following reasons: ");
            for (Failure failure : result.getFailures()) {
                failure.getException().printStackTrace();
            }
            fail();
        }
    }

    @BeforeClass
    public static void setupOnce() {
        // Start the Application
        new Thread(() -> Application.launch(BlockingDialogTest.class, (String[])null)).start();

        try {
            if (!launchLatch.await(5000, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to launch");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.exit();
    }
}