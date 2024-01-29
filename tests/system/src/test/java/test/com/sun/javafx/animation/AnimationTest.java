package test.com.sun.javafx.animation;

import org.junit.jupiter.api.Test;

import javafx.animation.Animation;
import javafx.animation.StrokeTransition;
import javafx.util.Duration;

public class AnimationTest extends SynchronisityTest {

    @Test
    public void testAnimation() throws InterruptedException {
        runTest(this::startAnimation);
    }

    private void startAnimation() {
        registerExceptionHandler();

        var anim = new StrokeTransition(Duration.millis(10));
        anim.setCycleCount(Animation.INDEFINITE);

        while (true) {
            // Initialize the concurrent starts and stops
            anim.play();
            sleep(10);
            anim.stop();
            sleep(10);
        }
    }
}