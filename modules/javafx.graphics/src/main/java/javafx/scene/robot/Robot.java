/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.robot;

import static com.sun.javafx.FXPermissions.CREATE_ROBOT_PERMISSION;

import java.util.Objects;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.GlassRobot;
import com.sun.javafx.tk.Toolkit;

/**
 * A {@code Robot} is used for simulating user interaction such as
 * typing keys on the keyboard and using the mouse as well as capturing
 * graphical information without requiring a {@link javafx.scene.Scene}
 * instance. Robot objects must be constructed and used on the JavaFX
 * Application Thread.
 *
 * @since 11
 */
public final class Robot {

    private final GlassRobot peer;

    /**
     * Constructs a new {@code Robot} that can be used for simulating user
     * interactions. If a security manager is present, the application must
     * have the {@link javafx.util.FXPermission} {@code "createRobot"} permission
     * in order to construct a {@code Robot} object.
     *
     * @throws IllegalStateException if this object is constructed on a thread
     * other than the JavaFX Application Thread.
     * @throws SecurityException if a security manager exists and the application
     * does not have the {@link javafx.util.FXPermission} {@code "createRobot"}
     * permission.
     */
    public Robot() {
        Application.checkEventThread();

        // Ensure we have proper permission for creating a robot.
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(CREATE_ROBOT_PERMISSION);
        }

        peer = Toolkit.getToolkit().createRobot();
        peer.create();
    }

    /**
     * Presses the specified {@link KeyCode} key.
     *
     * @param keyCode the key to press
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if keyCode is {@literal null}.
     */
    public void keyPress(KeyCode keyCode) {
        Objects.requireNonNull(keyCode, "keyCode must not be null");
        peer.keyPress(keyCode);
    }

    /**
     * Releases the specified {@link KeyCode} key.
     *
     * @param keyCode the key to release
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if keyCode is {@literal null}.
     */
    public void keyRelease(KeyCode keyCode) {
        Objects.requireNonNull(keyCode, "keyCode must not be null");
        peer.keyRelease(keyCode);
    }

    /**
     * Types the specified {@link KeyCode} key.
     *
     * @implSpec This is a convenience method that is equivalent to calling
     * {@link #keyPress(KeyCode)} followed by {@link #keyRelease(KeyCode)}.
     * @param keyCode the key to type
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if keyCode is {@literal null}.
     */
    public void keyType(KeyCode keyCode) {
        Objects.requireNonNull(keyCode, "keyCode must not be null");
        keyPress(keyCode);
        keyRelease(keyCode);
    }

    /**
     * Returns the current mouse {@code x} position in screen coordinates.
     *
     * @return the current mouse {@code x} position
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    public double getMouseX() {
        return peer.getMouseX();
    }

    /**
     * Returns the current mouse {@code y} position in screen coordinates.
     *
     * @return the current mouse {@code y} position
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    public double getMouseY() {
        return peer.getMouseY();
    }

    /**
     * Returns the current mouse (x,y) screen coordinates as a {@link Point2D}.
     *
     * @return the current mouse (x,y) screen coordinates
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    public Point2D getMousePosition() {
        return new Point2D(getMouseX(), getMouseY());
    }

    /**
     * Moves the mouse to the specified (x,y) screen coordinates relative to
     * the primary screen.
     *
     * @param x screen coordinate x to move the mouse to
     * @param y screen coordinate y to move the mouse to
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    public void mouseMove(double x, double y) {
        peer.mouseMove(x, y);
    }

    /**
     * Moves the mouse to the (x,y) screen coordinates, relative to the primary
     * screen, specified by the given {@code location}.
     *
     * @param location the (x,y) coordinates to move the mouse to
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if location is {@literal null}.
     */
    public final void mouseMove(Point2D location) {
        Objects.requireNonNull(location);
        mouseMove(location.getX(), location.getY());
    }

    /**
     * Presses the specified {@link MouseButton}s.
     *
     * @param buttons the mouse buttons to press
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if buttons is {@literal null}.
     */
    public void mousePress(MouseButton... buttons) {
        Objects.requireNonNull(buttons, "buttons must not be null");
        peer.mousePress(buttons);
    }

    /**
     * Releases the specified {@link MouseButton}s.
     *
     * @param buttons the mouse buttons to release
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if buttons is {@literal null}.
     */
    public void mouseRelease(MouseButton... buttons) {
        Objects.requireNonNull(buttons, "buttons must not be null");
        peer.mouseRelease(buttons);
    }

    /**
     * Clicks the specified {@link MouseButton}s.
     *
     * @implSpec This is a convenience method that is equivalent to calling
     * {@link #mousePress(MouseButton...)} followed by {@link #mouseRelease(MouseButton...)}.
     * @param buttons the mouse buttons to click
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if buttons is {@literal null}.
     */
    public void mouseClick(MouseButton... buttons) {
        Objects.requireNonNull(buttons, "buttons must not be null");
        mousePress(buttons);
        mouseRelease(buttons);
    }

    /**
     * Scrolls the mouse wheel by the specified amount of wheel clicks. A positive
     * {@code wheelAmt} scrolls the wheel towards the user (down) whereas negative
     * amounts scrolls the wheel away from the user (up).
     *
     * @param wheelAmt the (signed) amount of clicks to scroll the wheel
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    public void mouseWheel(int wheelAmt) {
        peer.mouseWheel(wheelAmt);
    }

    /**
     * Returns the {@link Color} of the pixel at the screen coordinates relative to the
     * primary screen specified by {@code location}. Regardless of the scale of the screen
     * ({@link javafx.stage.Screen#getOutputScaleX()}, {@link javafx.stage.Screen#getOutputScaleY()}),
     * this method only samples a single pixel. For example, on a HiDPI screen with output
     * scale 2, the screen unit at the point (x,y) may have 4 pixels. In this case the color
     * returned is the color of the top, left pixel. Color values are <em>not</em>
     * averaged when a screen unit is made up of more than one pixel.
     *
     * @param x the x coordinate to get the pixel color from
     * @param y the y coordinate to get the pixel color from
     * @return the pixel color at the specified screen coordinates
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    public Color getPixelColor(double x, double y) {
        return peer.getPixelColor(x, y);
    }

    /**
     * Returns the {@link Color} of the pixel at the screen coordinates relative to the
     * primary screen specified by {@code location}. Regardless of the scale of the screen
     * ({@link javafx.stage.Screen#getOutputScaleX()}, {@link javafx.stage.Screen#getOutputScaleY()}),
     * this method only samples a single pixel. For example, on a HiDPI screen with output
     * scale 2, the screen unit at the point (x,y) may have 4 pixels. In this case the color
     * returned is the color of the top, left pixel. Color values are <em>not</em>
     * averaged when a screen unit is made up of more than one pixel.
     *
     * @param location the (x,y) coordinates to get the pixel color from
     * @return the pixel color at the specified screen coordinates
     * @throws NullPointerException if the given {@code location} is {@literal null}
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    public Color getPixelColor(Point2D location) {
        return getPixelColor(location.getX(), location.getY());
    }

    /**
     * Returns a {@link WritableImage} containing the specified rectangular area relative
     * to the primary screen. If the given {@code image} is {@literal null}, or if the given
     * {@code image} is not the required size, a new {@code WritableImage} will be created
     * and returned. Otherwise, the given {@code image} is re-used.
     * <p>
     * If the {@code scaleToFit} argument is {@literal false}, the returned
     * {@code Image} object dimensions may differ from the requested {@code width}
     * and {@code height} depending on how many physical pixels the area occupies
     * on the screen. For example, in HiDPI mode on the Mac (aka Retina display) the
     * pixels are doubled, and thus a screen capture of an area of size (10x10) pixels
     * will result in an {@code Image} with dimensions (20x20). Calling code should
     * use the returned images's {@link Image#getWidth()} and {@link Image#getHeight()}
     * methods to determine the actual image size.
     * <p>
     * If {@code scaleToFit} is {@literal true}, the returned {@code Image} is of
     * the requested size. Note that in this case the image will be scaled in
     * order to fit to the requested dimensions if necessary, such as when running
     * on a HiDPI display.
     *
     * @param image either {@literal null} or a {@code WritableImage} that will
     * be used to place the screen capture in
     * @param x the starting x-position of the rectangular area to capture
     * @param y the starting y-position of the rectangular area to capture
     * @param width the width of the rectangular area to capture
     * @param height the height of the rectangular area to capture
     * @param scaleToFit If {@literal true}, the returned {@code Image} will be
     * scaled to fit the request dimensions (if necessary). Otherwise, the size
     * of the returned image will depend on the output scale (DPI) of the primary
     * screen.
     * @return the screen capture of the specified {@code region} as a {@link WritableImage}
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    public WritableImage getScreenCapture(WritableImage image, double x, double y,
                                          double width, double height, boolean scaleToFit) {
        return peer.getScreenCapture(image, x, y, width, height, scaleToFit);
    }

    /**
     * Returns a {@link WritableImage} containing the specified rectangular area relative
     * to the primary screen. If the given {@code image} is {@literal null}, or if the given
     * {@code image} is not the required size, a new {@code WritableImage} will be created
     * and returned. Otherwise, the given {@code image} is re-used.
     *
     * @implSpec This method is equivalent to calling {@code getScreenCapture(x, y, width, height, true)},
     * that is, this method scales the image to fit the requested size.
     * @param image either {@literal null} or a {@code WritableImage} that will
     * be used to place the screen capture in
     * @param x the starting x-position of the rectangular area to capture
     * @param y the starting y-position of the rectangular area to capture
     * @param width the width of the rectangular area to capture
     * @param height the height of the rectangular area to capture
     * @return the screen capture of the specified {@code region} as a {@link WritableImage}
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     */
    public WritableImage getScreenCapture(WritableImage image, double x, double y,
                                          double width, double height) {
        return getScreenCapture(image, x, y, width, height, true);
    }

    /**
     * Returns a {@link WritableImage} containing the specified rectangular area relative
     * to the primary screen. If the given {@code image} is {@literal null}, or if the given
     * {@code image} is not the required size, a new {@code WritableImage} will be created
     * and returned. Otherwise, the given {@code image} is re-used.
     *
     * @implSpec This method is equivalent to calling {@code getScreenCapture(image, region, true)},
     * that is, this method scales the image to fit the requested size.
     * @param image either {@literal null} or a {@code WritableImage} that will
     * be used to place the screen capture in
     * @param region the rectangular area of the screen to capture
     * @return the screen capture of the specified {@code region} as a {@link WritableImage}
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if region is {@literal null}.
     */
    public WritableImage getScreenCapture(WritableImage image, Rectangle2D region) {
        Objects.requireNonNull(region);
        return getScreenCapture(image, region.getMinX(), region.getMinY(),
                region.getWidth(), region.getHeight(), true);
    }

    /**
     * Returns a {@link WritableImage} containing the specified rectangular area relative to
     * the primary screen. If the given {@code image} is {@literal null}, or if the given
     * {@code image} is not the required size, a new {@code WritableImage} will be created
     * and returned. Otherwise, the given {@code image} is re-used.
     * <p>
     * If the {@code scaleToFit} argument is {@literal false}, the returned
     * {@code Image} object dimensions may differ from the requested {@code width}
     * and {@code height} depending on how many physical pixels the area occupies
     * on the screen. For example, in HiDPI mode on the Mac (aka Retina display) the
     * pixels are doubled, and thus a screen capture of an area of size (10x10) pixels
     * will result in an {@code Image} with dimensions (20x20). Calling code should
     * use the returned images's {@link Image#getWidth()} and {@link Image#getHeight()}
     * methods to determine the actual image size.
     * <p>
     * If {@code scaleToFit} is {@literal true}, the returned {@code Image} is of
     * the requested size. Note that in this case the image will be scaled in
     * order to fit to the requested dimensions if necessary, such as when running
     * on a HiDPI display.
     *
     * @param image either {@literal null} or a {@code WritableImage} that will
     * be used to place the screen capture in
     * @param region the rectangular area of the screen to capture
     * @param scaleToFit if {@literal true}, the returned {@code Image} will be
     * scaled to fit the request dimensions (if necessary). Otherwise, the size
     * of the returned image will depend on the output scale (DPI) of the primary
     * screen.
     * @return the screen capture of the specified {@code region} as a {@link WritableImage}
     * @throws IllegalStateException if this method is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if region is {@literal null}.
     */
    public WritableImage getScreenCapture(WritableImage image, Rectangle2D region, boolean scaleToFit) {
        Objects.requireNonNull(region);
        return getScreenCapture(image, region.getMinX(), region.getMinY(),
                region.getWidth(), region.getHeight(), scaleToFit);
    }
}
