package test.javafx.scene;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class InitializeJavaFXTest {

    public static final CountDownLatch appLatch = new CountDownLatch(1);

    public static class InitializeApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            appLatch.countDown();
        }
    }

    public static void initializeApplication() throws Exception {
        new Thread(() -> {
            Application.launch(InitializeApp.class);
        }).start();
        appLatch.await();
    }

    public static void initializeStartup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            latch.countDown();
        });
        latch.await();
    }

    @BeforeClass
    public static void initialize() throws Exception {
        System.out.println("Calling Startup!");
        initializeStartup();
        System.out.println("Called Startup!");
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            System.out.println("start called!");
        }
    }

    @Test
    public void testStartupThenLaunchInFX() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                System.out.println("Calling launch!");
                Application.launch(TestApp.class);
                System.out.println("Finished launch!");
            } catch (IllegalStateException e) {
                latch.countDown();
            } catch (Exception e) {
                System.out.println("got exception:  " + e);
                e.printStackTrace();
            }
        });
        Assert.assertTrue("Timeout", latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testStartupThenLaunch() throws Exception {
        try {
            System.out.println("Calling launch!");
            Application.launch(TestApp.class);
            System.out.println("Finished launch!");
            throw new Exception("We excpect an error!");
        } catch (IllegalStateException e) {
            System.out.println("Works!");
        } catch (Exception e) {
            throw e;
        }
    }
}
