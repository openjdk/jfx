/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.glass.ui;

import java.lang.annotation.Native;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

import com.sun.javafx.image.PixelUtils;

public abstract class GlassRobot {

    @Native public static final int MOUSE_LEFT_BTN    = 1 << 0;
    @Native public static final int MOUSE_RIGHT_BTN   = 1 << 1;
    @Native public static final int MOUSE_MIDDLE_BTN  = 1 << 2;
    @Native public static final int MOUSE_BACK_BTN    = 1 << 3;
    @Native public static final int MOUSE_FORWARD_BTN = 1 << 4;

    /**
     * Initializes any state necessary for this {@code Robot}. Called by
     * the {@code Robot} constructor.
     */
    public abstract void create();

    /**
     * Frees any resources allocated by this {@code Robot}.
     */
    public abstract void destroy();

    /**
     * Presses the specified {@link KeyCode} key.
     *
     * @param keyCode the key to press
     */
    public abstract void keyPress(KeyCode keyCode);

    /**
     * Releases the specified {@link KeyCode} key.
     *
     * @param keyCode the key to release
     */
    public abstract void keyRelease(KeyCode keyCode);

    /**
     * Returns the current mouse x-position.
     *
     * @return the current mouse x-position
     */
    public abstract double getMouseX();

    /**
     * Returns the current mouse y-position.
     *
     * @return the current mouse y-position
     */
    public abstract double getMouseY();

    /**
     * Moves the mouse to the specified (x,y) screen coordinates relative to
     * the primary screen.
     *
     * @param x screen coordinate x to move the mouse to
     * @param y screen coordinate y to move the mouse to
     */
    public abstract void mouseMove(double x, double y);

    /**
     * Presses the specified {@link MouseButton}s.
     *
     * @param buttons the mouse buttons to press
     */
    public abstract void mousePress(MouseButton... buttons);

    /**
     * Releases the specified {@link MouseButton}s.
     *
     * @param buttons the mouse buttons to release
     */
    public abstract void mouseRelease(MouseButton... buttons);

    /**
     * Scrolls the mouse wheel by the specified amount of wheel clicks. A positive
     * {@code wheelAmt} scrolls the wheel towards the user (down) whereas negative
     * amounts scrolls the wheel away from the user (up).
     *
     * @param wheelAmt the (signed) amount of clicks to scroll the wheel
     */
    public abstract void mouseWheel(int wheelAmt);

    /**
     * Returns the {@link Color} of the pixel at the screen coordinates relative to the
     * primary screen specified by {@code location}. Regardless of the scale of the screen
     * ({@link Screen#getOutputScaleX()}, {@link Screen#getOutputScaleY()}), this method only
     * samples a single pixel. For example, on a HiDPI screen with output scale 2, the screen
     * unit at the point (x,y) may have 4 pixels. In this case the color returned is the color
     * of the top, left pixel. Color values are <em>not</em> averaged when a screen unit is
     * made up of more than one pixel.
     *
     * @param x the x coordinate to get the pixel color from
     * @param y the y coordinate to get the pixel color from
     * @return the pixel color at the specified screen coordinates
     */
    public abstract Color getPixelColor(double x, double y);

    /**
     * Captures the specified rectangular area of the screen and uses it to fill the given
     * {@code data} array with the raw pixel data. The data is in RGBA format where each
     * pixel in the image is encoded as 4 bytes - one for each color component of each
     * pixel. If this method is not overridden by subclasses then
     * {@link #getScreenCapture(WritableImage, double, double, double, double, boolean)}
     * must be overridden to not call this method.
     *
     * @param x the starting x-position of the rectangular area to capture
     * @param y the starting y-position of the rectangular area to capture
     * @param width the width of the rectangular area to capture
     * @param height the height of the rectangular area to capture
     * @param data the array to fill with the raw pixel data corresponding to
     * the captured region
     * @param scaleToFit If {@literal true} the returned {@code Image} will be
     * scaled to fit the request dimensions, if necessary. Otherwise the size
     * of the returned image will depend on the output scale (DPI) of the primary
     * screen.
     */
    public void getScreenCapture(int x, int y, int width, int height, int[] data, boolean scaleToFit) {
        throw new InternalError("not implemented");
    }

    /**
     * Returns an {@code Image} containing the specified rectangular area of the screen.
     * <p>
     * If the {@code scaleToFit} argument is {@literal false}, the returned
     * {@code Image} object dimensions may differ from the requested {@code width}
     * and {@code height} depending on how many physical pixels the area occupies
     * on the screen. E.g. in HiDPI mode on the Mac (aka Retina display) the pixels
     * are doubled, and thus a screen capture of an area of size (10x10) pixels
     * will result in an {@code Image} with dimensions (20x20). Calling code should
     * use the returned images's {@link Image#getWidth() and {@link Image#getHeight()
     * methods to determine the actual image size.
     * <p>
     * If {@code scaleToFit} is {@literal true}, the returned {@code Image} is of
     * the requested size. Note that in this case the image will be scaled in
     * order to fit to the requested dimensions if necessary such as when running
     * on a HiDPI display.
     *
     * @param x the starting x-position of the rectangular area to capture
     * @param y the starting y-position of the rectangular area to capture
     * @param width the width of the rectangular area to capture
     * @param height the height of the rectangular area to capture
     * @param scaleToFit If {@literal true} the returned {@code Image} will be
     * scaled to fit the request dimensions, if necessary. Otherwise the size
     * of the returned image will depend on the output scale (DPI) of the primary
     * screen.
     */
    public WritableImage getScreenCapture(WritableImage image, double x, double y, double width,
                                          double height, boolean scaleToFit) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }
        Screen primaryScreen = Screen.getPrimary();
        Objects.requireNonNull(primaryScreen);
        double outputScaleX = primaryScreen.getOutputScaleX();
        double outputScaleY = primaryScreen.getOutputScaleY();
        int data[];
        int dw, dh;
        if (outputScaleX == 1.0f && outputScaleY == 1.0f) {
            // No scaling will be necessary regardless of if "scaleToFit" is set or not.
            data = new int[(int) (width * height)];
            getScreenCapture((int) x, (int) y, (int) width, (int) height, data, scaleToFit);
            dw = (int) width;
            dh = (int) height;
        } else {
            // Compute the absolute pixel bounds that the requested size will fill given
            // the display's scale.
            int pminx = (int) Math.floor(x * outputScaleX);
            int pminy = (int) Math.floor(y * outputScaleY);
            int pmaxx = (int) Math.ceil((x + width) * outputScaleX);
            int pmaxy = (int) Math.ceil((y + height) * outputScaleY);
            int pwidth = pmaxx - pminx;
            int pheight = pmaxy - pminy;
            int tmpdata[] = new int[pwidth * pheight];
            getScreenCapture(pminx, pminy, pwidth, pheight, tmpdata, scaleToFit);
            dw = pwidth;
            dh = pheight;
            if (!scaleToFit) {
                data = tmpdata;
            } else {
                // We must resize the image to fit the requested bounds. This means
                // resizing the pixel data array which we accomplish using bilinear (?)
                // interpolation.
                data = new int[(int) (width * height)];
                int index = 0;
                for (int iy = 0; iy < height; iy++) {
                    double rely = ((y + iy + 0.5f) * outputScaleY) - (pminy + 0.5f);
                    int irely = (int) Math.floor(rely);
                    int fracty = (int) ((rely - irely) * 256);
                    for (int ix = 0; ix < width; ix++) {
                        double relx = ((x + ix + 0.5f) * outputScaleX) - (pminx + 0.5f);
                        int irelx = (int) Math.floor(relx);
                        int fractx = (int) ((relx - irelx) * 256);
                        data[index++] = interp(tmpdata, irelx, irely, pwidth, pheight, fractx, fracty);
                    }
                }
                dw = (int) width;
                dh = (int) height;
            }
        }

        return convertFromPixels(image, Application.GetApplication().createPixels(dw, dh, IntBuffer.wrap(data)));
    }

    public static int convertToRobotMouseButton(MouseButton[] buttons) {
        int ret = 0;
        for (MouseButton button : buttons) {
            switch (button) {
                case PRIMARY: ret |= MOUSE_LEFT_BTN; break;
                case SECONDARY: ret |= MOUSE_RIGHT_BTN; break;
                case MIDDLE: ret |= MOUSE_MIDDLE_BTN; break;
                case BACK: ret |= MOUSE_BACK_BTN; break;
                case FORWARD: ret |= MOUSE_FORWARD_BTN; break;
                default: throw new IllegalArgumentException("MouseButton: " + button + " not supported by Robot");
            }
        }
        return ret;
    }

    public static Color convertFromIntArgb(int color) {
        int alpha = (color >> 24) & 0xFF;
        int red   = (color >> 16) & 0xFF;
        int green = (color >>  8) & 0xFF;
        int blue  =  color        & 0xFF;
        return new Color(red / 255d, green / 255d, blue / 255d, alpha / 255d);
    }

    protected static WritableImage convertFromPixels(WritableImage image, Pixels pixels) {
        Objects.requireNonNull(pixels);
        int width = pixels.getWidth();
        int height = pixels.getHeight();
        if (image == null || image.getWidth() != width || image.getHeight() != height) {
            image = new WritableImage(width, height);
        }

        int bytesPerComponent = pixels.getBytesPerComponent();
        if (bytesPerComponent == 4) {
            IntBuffer intBuffer = (IntBuffer) pixels.getPixels();
            writeIntBufferToImage(intBuffer, image);
        } else if (bytesPerComponent == 1) {
            ByteBuffer byteBuffer = (ByteBuffer) pixels.getPixels();
            writeByteBufferToImage(byteBuffer, image);
        } else {
            throw new IllegalArgumentException("bytesPerComponent must be either 4 or 1 but was: " +
                    bytesPerComponent);
        }

        return image;
    }

    private static void writeIntBufferToImage(IntBuffer intBuffer, WritableImage image) {
        Objects.requireNonNull(image);
        PixelWriter pixelWriter = image.getPixelWriter();
        double width = image.getWidth();
        double height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = intBuffer.get();
                pixelWriter.setArgb(x, y, argb);
            }
        }
    }

    private static void writeByteBufferToImage(ByteBuffer byteBuffer, WritableImage image) {
        Objects.requireNonNull(image);
        PixelWriter pixelWriter = image.getPixelWriter();
        double width = image.getWidth();
        double height = image.getHeight();

        int format = Pixels.getNativeFormat();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (format == Pixels.Format.BYTE_BGRA_PRE) {
                    pixelWriter.setArgb(x, y, PixelUtils.PretoNonPre(bgraPreToRgbaPre(byteBuffer.getInt())));
                } else if (format == Pixels.Format.BYTE_ARGB) {
                    pixelWriter.setArgb(x, y, byteBuffer.getInt());
                } else {
                    throw new IllegalArgumentException("format must be either BYTE_BGRA_PRE or BYTE_ARGB");
                }
            }
        }
    }

    private static int bgraPreToRgbaPre(int bgraPre) {
        return Integer.reverseBytes(bgraPre);
    }

    private static int interp(int pixels[], int x, int y, int w, int h, int fractx1, int fracty1) {
        int fractx0 = 256 - fractx1;
        int fracty0 = 256 - fracty1;
        int i = y * w + x;
        int rgb00 = (x < 0 || y < 0 || x >= w || y >= h) ? 0 : pixels[i];
        if (fracty1 == 0) {
            // No interpolation with pixels[y+1]
            if (fractx1 == 0) {
                // No interpolation with any neighbors
                return rgb00;
            }
            int rgb10 = (y < 0 || x+1 >= w || y >= h) ? 0 : pixels[i+1];
            return interp(rgb00, rgb10, fractx0, fractx1);
        } else if (fractx1 == 0) {
            // No interpolation with pixels[x+1]
            int rgb01 = (x < 0 || x >= w || y+1 >= h) ? 0 : pixels[i+w];
            return interp(rgb00, rgb01, fracty0, fracty1);
        } else {
            // All 4 neighbors must be interpolated
            int rgb10 = (y < 0 || x+1 >= w || y >= h) ? 0 : pixels[i+1];
            int rgb01 = (x < 0 || x >= w || y+1 >= h) ? 0 : pixels[i+w];
            int rgb11 = (x+1 >= w || y+1 >= h) ? 0 : pixels[i+w+1];
            return interp(interp(rgb00, rgb10, fractx0, fractx1),
                    interp(rgb01, rgb11, fractx0, fractx1),
                    fracty0, fracty1);
        }
    }

    private static int interp(int rgb0, int rgb1, int fract0, int fract1) {
        int a0 = (rgb0 >> 24) & 0xff;
        int r0 = (rgb0 >> 16) & 0xff;
        int g0 = (rgb0 >>  8) & 0xff;
        int b0 = (rgb0      ) & 0xff;
        int a1 = (rgb1 >> 24) & 0xff;
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >>  8) & 0xff;
        int b1 = (rgb1      ) & 0xff;
        int a = (a0 * fract0 + a1 * fract1) >> 8;
        int r = (r0 * fract0 + r1 * fract1) >> 8;
        int g = (g0 * fract0 + g1 * fract1) >> 8;
        int b = (b0 * fract0 + b1 * fract1) >> 8;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

}
