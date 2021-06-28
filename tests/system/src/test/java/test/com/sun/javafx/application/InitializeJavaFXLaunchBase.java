package test.com.sun.javafx.application;

import javafx.application.Application;
import javafx.stage.Stage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertTrue;

public class InitializeJavaFXLaunchBase extends InitializeJavaFXBase {
    public static final CountDownLatch appLatch = new CountDownLatch(1);

    public static class InitializeApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            appLatch.countDown();
        }
    }

    public static void initializeApplicationLaunch() throws Exception {
        new Thread(() -> {
            Application.launch(InitializeApp.class);
        }).start();
        assertTrue(appLatch.await(5, TimeUnit.SECONDS));
    }
}
