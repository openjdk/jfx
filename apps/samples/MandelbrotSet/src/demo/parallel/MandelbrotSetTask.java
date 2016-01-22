/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package demo.parallel;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javafx.concurrent.Task;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;


/**
 * Task to render Mandelbrot set using given parameters. See {@link
 * #MandelbrotRendererTask(boolean, javafx.scene.image.PixelWriter, int, int,
 * double, double, double, double, double, double, double, double, boolean)
 * constructor} for parameters list. The task returns time in milliseconds as
 * its calculated value.
 *
 * <p><i>
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.</i>
 *
 * @author Alexander Kouznetsov, Tristan Yan
 */
class MandelbrotSetTask extends Task<Long> {

    /**
     * Calculation times, deliberately choose it as 256 because we will use the
     * count to calculate Color
     */
    private static final int CAL_MAX_COUNT = 256;

    /**
     * This is the square of max radius, Mandelbrot set contained in the closed
     * disk of radius 2 around the origin plus some area around, so
     * LENGTH_BOUNDARY is 6.
     */
    private static final double LENGTH_BOUNDARY = 6d;

    /**
     * For antialiasing we break each pixel into 3x3 grid and interpolate
     * between values calculated on those grid positions
     */
    private static final int ANTIALIASING_BASE = 3;

    /**
     * Sequential vs. parallel calculation mode
     */
    private final boolean parallel;

    /**
     * Antialiased mode flag
     */
    private final boolean antialiased;

    /**
     * Dimension of the area
     */
    private final int width, height;

    /**
     * Rectangle range to exclude from calculations. Used to skip calculations
     * for parts of MandelbrotSet that are already calculated.
     */
    private final double minX, minY, maxX, maxY;

    /**
     * Real and imaginary part of min and max number in the set we need
     * calculate
     */
    private final double minR, minI, maxR, maxI;

    /**
     * Pixel writer to use for writing calculated pixels
     */
    private final PixelWriter pixelWriter;

    /**
     * Flag indicating that some new pixels were calculated
     */
    private volatile boolean hasUpdates;

    /**
     * Start time of the task in milliseconds
     */
    private volatile long startTime = -1;

    /**
     * Total time of the task in milliseconds
     */
    private volatile long taskTime = -1;

    /**
     * Progress of the task
     */
    private final AtomicInteger progress = new AtomicInteger(0);

    /**
     * Creates a task to render a MandelBrot set into an image using given
     * PixelWriter with given dimensions of the image, given real and imaginary
     * values range and given rectangular area to skip. Also there is a switch
     * that disables more computational-extensive antialiasing mode.
     * @param parallel parallel vs. sequential switch
     * @param pixelWriter target to write pixels to
     * @param width width of the image area
     * @param height height of the image area
     * @param minR min real value of the area
     * @param minI min imaginary value of the area
     * @param maxR max real value of the area
     * @param maxI max imaginary value of the area
     * @param minX min x value of the rectangular area to skip
     * @param minY min y value of the rectangular area to skip
     * @param maxX max x value of the rectangular area to skip
     * @param maxY max y value of the rectangular area to skip
     * @param fast fast mode disables antialiasing
     */
    public MandelbrotSetTask(boolean parallel, PixelWriter pixelWriter, int width, int height, double minR, double minI, double maxR, double maxI, double minX, double minY, double maxX, double maxY, boolean fast) {
        this.parallel = parallel;
        this.pixelWriter = pixelWriter;
        this.width = width;
        this.height = height;
        this.maxX = maxX;
        this.minX = minX;
        this.maxY = maxY;
        this.minY = minY;
        this.minR = minR;
        this.maxR = maxR;
        this.minI = minI;
        this.maxI = maxI;
        this.antialiased = !fast;
        updateProgress(0, 0);
    }

    /**
     *
     * @return whether new pixels were written to the image
     */
    public boolean hasUpdates() {
        return hasUpdates;
    }

    /**
     * @return true if task is parallel
     */
    public boolean isParallel() {
        return parallel;
    }

    /**
     * Clears the updates flag
     */
    public void clearHasUpdates() {
        hasUpdates = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void failed() {
        super.failed();
        getException().printStackTrace(System.err);
    }

    /**
     * Returns current task execution time while task is running and total
     * task time when task is finished
     * @return task time in milliseconds
     */
    public long getTime() {
        if (taskTime != -1) {
            return taskTime;
        }
        if (startTime == -1) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long call() throws Exception {
        synchronized(pixelWriter) {
            // Prepares an image
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    pixelWriter.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }
        startTime = System.currentTimeMillis();

        // We do horizontal lines in parallel when asked
        IntStream yStream = IntStream.range(0, height);
        if (parallel) {
            yStream = yStream.parallel();
        } else {
            yStream = yStream.sequential();
        }
        updateProgress(0, height);
        yStream.forEach((int y) -> {

            // We do pixels in horizontal lines always sequentially
            for (int x = 0; x < width; x++) {

                // Skip excluded rectangular area
                if (!(x >= maxX || x < minX || y >= maxY || y < minY)) {
                    continue;
                }
                Color c;
                if (antialiased) {
                    c = calcAntialiasedPixel(x, y);
                } else {
                    c = calcPixel(x, y);
                }
                if (isCancelled()) {
                    return;
                }
                synchronized(pixelWriter) {
                    pixelWriter.setColor(x, y, c);
                }
                hasUpdates = true;
            }
            updateProgress(progress.incrementAndGet(), height);
        });
        taskTime = getTime();
        return taskTime;
    }

    /**
     * Calculates number of iterations a complex quadratic polynomials
     * stays within a disk of some finite radius for a given complex number.
     *
     * This number is used to choose a color for this pixel for precalculated
     * color tables.
     *
     * @param comp a complex number used for calculation
     * @return number of iterations a value stayed within a given disk.
     */
    private int calc(Complex comp) {
        int count = 0;
        Complex c = new Complex(0, 0);
        do {
            c = c.times(c).plus(comp);
            count++;
        } while (count < CAL_MAX_COUNT && c.lengthSQ() < LENGTH_BOUNDARY);
        return count;
    }

    /**
     * Calculates a color of a given pixel on the image using
     * {@link #calc(demo.parallel.Complex) } method.
     * @param x x coordinate of the pixel in the image
     * @param y y coordinate of the pixel in the image
     * @return calculated color of the pixel
     */
    private Color calcPixel(double x, double y) {
        double re = (minR * (width - x) + x * maxR) / width;
        double im = (minI * (height - y) + y * maxI) / height;
        Complex calPixel = new Complex(re, im);
        return getColor(calc(calPixel));
    }

    /**
     * Calculates antialised color of a given pixel on the image by dividing
     * real and imaginary value ranges of a pixel by {@link #ANTIALIASING_BASE}
     * and doing interpolation between calculated values
     * @param x x coordinate of the pixel in the image
     * @param y y coordinate of the pixel in the image
     * @return calculated color of the pixel
     */
    private Color calcAntialiasedPixel(int x, int y) {
        double step = 1d / ANTIALIASING_BASE;
        double N = ANTIALIASING_BASE * ANTIALIASING_BASE;
        double r = 0, g = 0, b = 0;
        for (int i = 0; i < ANTIALIASING_BASE; i++) {
            for (int j = 0; j < ANTIALIASING_BASE; j++) {
                Color c = calcPixel(x + step * (i + 0.5) - 0.5, y + step * (j + 0.5) - 0.5);
                r += c.getRed() / N;
                g += c.getGreen() / N;
                b += c.getBlue() / N;
            }
        }
        return new Color(clamp(r), clamp(g), clamp(b), 1);
    }

    /**
     * Clamps the value in 0..1 interval
     * @param val value to clamp
     * @return value in 0..1 interval
     */
    private double clamp(double val) {
        return val > 1 ? 1 : val < 0 ? 0 : val;
    }

    /**
     * Returns a color for a given iteration count.
     * @param count number of iterations return by
     * {@link #calc(demo.parallel.Complex)} method
     * @return color from pre-calculated table
     */
    private Color getColor(int count) {
        if (count >= colors.length) {
            return Color.BLACK;
        }
        return colors[count];
    }

    /**
     * Pre-calculated colors table
     */
    static final Color[] colors = new Color[256];

    static {

        /**
         * Color stops for colors table: color values
         */
        Color[] cc = {
            Color.rgb(40, 0, 0),
            Color.RED,
            Color.WHITE,
            Color.RED,
            Color.rgb(100, 0, 0),
            Color.RED,
            Color.rgb(50, 0, 0)
        };

        /**
         * Color stops for colors table: relative position in the table
         */
        double[] cp = {
            0, 0.17, 0.25, 0.30, 0.5, 0.75, 1,};

        /**
         * Color table population
         */
        int j = 0;
        for (int i = 0; i < colors.length; i++) {
            double p = (double) i / (colors.length - 1);
            if (p > cp[j + 1]) {
                j++;
            }
            double val = (p - cp[j]) / (cp[j + 1] - cp[j]);
            colors[i] = cc[j].interpolate(cc[j + 1], val);
        }
    }
}
