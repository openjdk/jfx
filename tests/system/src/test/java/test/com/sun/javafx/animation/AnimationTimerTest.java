package test.com.sun.javafx.animation;

import org.junit.jupiter.api.Test;

import javafx.animation.AnimationTimer;

// Based on https://bugs.openjdk.org/browse/JDK-8159048
public class AnimationTimerTest extends SynchronisityTest {

    @Test
    public void testAnimationTimer() throws InterruptedException {
        runTest(this::startAnimationTimer);
    }

    private void startAnimationTimer() {
        registerExceptionHandler();

        var timer = new AnimationTimer() {

            @Override
            public void handle(long now) {
                sleep(10); // Some intensive processing
            }
        };

        while (true) {
            // Initialize the concurrent starts and stops
            timer.start();
            sleep(10);
            timer.stop();
            sleep(10);
        }
    }
}