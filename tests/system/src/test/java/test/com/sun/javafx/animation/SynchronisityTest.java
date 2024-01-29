package test.com.sun.javafx.animation;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import test.util.Util;

// Based on https://bugs.openjdk.org/browse/JDK-8159048
public class SynchronisityTest extends Application {

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
        Util.launch(startupLatch, SynchronisityTest.class);
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

    private AtomicBoolean failed = new AtomicBoolean(false);
    private CountDownLatch waiter = new CountDownLatch(1);
    private ExecutorService executor = Executors.newCachedThreadPool();

    private Thread thread;
    private Throwable throwable;

    protected void runTest(Runnable runnable) throws InterruptedException {
        Platform.runLater(() -> registerExceptionHandler());

        for (int i = 0; i < 10; i++) {
            executor.submit(runnable);
        }

        waiter.await(GRACE_PERIOD, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertFalse(failed.get(), "\"" + throwable + "\" was thrown on " + thread);
    }

    protected void registerExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            thread = t;
            throwable = e;
            failed.set(true);
            waiter.countDown();
        });
    }

    /**
     * Utility method for making a thread wait.
     *
     * @param millis time in milliseconds to wait
     */
    protected static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }
}