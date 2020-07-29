package attenuation;

import javafx.animation.AnimationTimer;

final class FPSCounter extends AnimationTimer {

    private int skipFrames = 100;
    private long lastTime = -1;
    private long elapsedTime;
    private int elapsedFrames;
    private long totalElapsedTime;
    private int totalElapsedFrames;

    @Override
    public void handle(long now) {
        if (skipFrames > 0) {
            --skipFrames;
            return;
        }

        if (lastTime < 0) {
            lastTime = System.nanoTime();
            elapsedTime = 0;
            elapsedFrames = 0;
            totalElapsedTime = 0;
            totalElapsedFrames = 0;
            return;
        }

        long currTime = System.nanoTime();
        elapsedTime += currTime - lastTime;
        elapsedFrames += 1;
        totalElapsedTime += currTime - lastTime;
        totalElapsedFrames += 1;

        double elapsedSeconds = (double) elapsedTime / 1e9;
        double totalElapsedSeconds = (double) totalElapsedTime / 1e9;
        if (elapsedSeconds >= 5.0) {
            double fps = elapsedFrames / elapsedSeconds;
            System.out.println();
            System.out.println("instant fps: " + fps);
            double avgFps = totalElapsedFrames / totalElapsedSeconds;
            System.out.println("average fps: " + avgFps);
            System.out.flush();
            elapsedTime = 0;
            elapsedFrames = 0;
        }

        lastTime = currTime;
    }

    void reset() {
        skipFrames = 100;
        lastTime = -1;
        elapsedTime = 0;
        elapsedFrames = 0;
        totalElapsedTime = 0;
        totalElapsedFrames = 0;
        System.out.println();
        System.out.println(" --------------------- ");
    }
}
