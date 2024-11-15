package test.robot.javafx.embed.swing;

import java.awt.Robot;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import test.util.Util;

@TestMethodOrder(OrderAnnotation.class)
public class LinuxScreencastHangCrashTest {

    private static Robot robot;
    private static javafx.scene.robot.Robot jfxRobot;

    private static final int DELAY_BEFORE_SESSION_CLOSE = 2000;
    private static final int DELAY_WAIT_FOR_SESSION_TO_CLOSE = DELAY_BEFORE_SESSION_CLOSE + 250;
    private static final int DELAY_KEEP_SESSION = DELAY_BEFORE_SESSION_CLOSE - 1000;

    private static volatile boolean isFxStarted = false;
    private static volatile boolean isFirstRun = true;

    @BeforeAll
    public static void init() throws Exception {
        Assumptions.assumeTrue(!Util.isOnWayland()); // JDK-8335470
        Assumptions.assumeTrue(Util.isOnWayland());
        robot = new Robot();
    }


    static void awtPixel() {
        System.out.println("awtPixel on " + Thread.currentThread().getName());
        java.awt.Color pixelColor = robot.getPixelColor(100, 100);
        System.out.println("\tAWT pixelColor: " + pixelColor);
    }

    private static void awtPixelOnFxThread() throws InterruptedException {
        System.out.println("awtPixelOnFxThread");
        initFX();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            awtPixel();
            latch.countDown();
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timed out waiting for awt pixel on FX thread");
        }
    }

    private static void fxPixel() throws InterruptedException {
        System.out.println("fxPixel");
        initFX();

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Color pixelColor = jfxRobot.getPixelColor(100, 100);
            System.out.println("\tFX pixelColor: " + pixelColor);
            latch.countDown();
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timed out waiting for FX pixelColor");
        }
    }

    private static void initFX() {
        if (!isFxStarted) {
            System.out.println("Platform.startup");
            Platform.startup(() -> {
                jfxRobot = new javafx.scene.robot.Robot();
            });
            isFxStarted = true;
        }
    }

    private static void checkFirstRun() {
        if (isFirstRun) {
            isFirstRun = false;
        } else {
            robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);
        }
    }

    @Test
    @Order(1)
    @Timeout(value=30)
    public void testHang() throws Exception {
        awtPixel();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);

        initFX();
        robot.delay(500);
        awtPixel();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);

        awtPixelOnFxThread();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);

        fxPixel();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);

        awtPixelOnFxThread();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);

        awtPixel();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);
    }

    @Test
    @Order(2)
    @Timeout(value=60)
    public void testCrash() {
        List.of(
                DELAY_KEEP_SESSION,
                DELAY_BEFORE_SESSION_CLOSE, // 3 following are just in case
                DELAY_BEFORE_SESSION_CLOSE - 25,
                DELAY_BEFORE_SESSION_CLOSE + 25
        ).forEach(delay -> {
            System.out.println("Testing with delay: " + delay);
            try {
                awtPixel();
                robot.delay(delay);
                fxPixel();
                robot.delay(delay);
                awtPixelOnFxThread();
                robot.delay(delay);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
