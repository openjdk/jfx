package test.com.sun.javafx.animation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import test.util.Util;

// Based on https://bugs.openjdk.org/browse/JDK-8159048
public class AnimationSynchronisityTest extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        startupLatch.countDown();
    }

    @BeforeAll
    public static void setup() throws Exception {
        Util.launch(startupLatch, AnimationSynchronisityTest.class);
    }

    @AfterAll
    public static void shutdown() {
        Util.shutdown(primaryStage);
    }

    /**
     * Number of seconds to wait for a failure. If an exception is not thrown in this time, it's assumed it won't be
     * thrown later too.
     */
    private static final int GRACE_PERIOD = 15;

    @Test
    public void catcher() throws InterruptedException {
        var failed = new AtomicBoolean(false);
        var waiter = new CountDownLatch(1);
        registerFxExceptionHandler(failed, waiter);

        for (int i = 0; i < 10; i++) {
            new Thread(this::bugProc).start();
        }
        waiter.await(GRACE_PERIOD, TimeUnit.SECONDS);
        assertFalse(failed.get(), "An exception was thrown on the JavaFX Application Thread");
    }

    private void registerFxExceptionHandler(AtomicBoolean failed, CountDownLatch waiter) {
        Platform.runLater(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
                failed.set(true);
                waiter.countDown();
            });
        });
    }

    private void bugProc() {
        var tester = new AnimationTimer() {

            @Override
            public void handle(long now) {
                waitSomeTime(10); // Some intensive processing
            }
        };

        while (true) {
            // Initialize the concurrent starts and stops
            tester.start();
            waitSomeTime(10);
            tester.stop();
            waitSomeTime(10);
        }
    }

    /**
     * Utility method for waiting some time
     * 
     * @param millis time in milliseconds to wait
     */
    private static void waitSomeTime(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }
}