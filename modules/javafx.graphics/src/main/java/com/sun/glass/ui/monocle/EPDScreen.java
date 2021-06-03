/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.monocle;

import com.sun.glass.ui.Pixels;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.util.Logging;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;

/**
 * A native screen for an electrophoretic display, also called an e-paper
 * display. This class uploads pixels directly into the Linux frame buffer if it
 * is configured with a color depth of 32 bits per pixel. Otherwise, this class
 * uploads pixels into a 32-bit off-screen composition buffer and converts the
 * pixels to the correct format when writing them to the Linux frame buffer.
 */
class EPDScreen implements NativeScreen {

    /**
     * The system property for setting the frame buffer device path.
     */
    private static final String FB_PATH_KEY = "monocle.screen.fb";

    /**
     * The default value for the frame buffer device path.
     */
    private static final String FB_PATH_DEFAULT = "/dev/fb0";

    /**
     * The density of this screen in pixels per inch. For now, the value is
     * hard-coded to the density of a 6-inch display panel with 800 x 600 px at
     * 167 ppi.
     */
    private static final int DPI = 167;

    /**
     * The ratio of physical pixels to logical pixels on this screen. For now,
     * the value is hard-coded to a ratio of 1.0.
     */
    private static final float SCALE = 1.0f;

    private final PlatformLogger logger = Logging.getJavaFXLogger();

    private final String fbPath;
    private final EPDFrameBuffer fbDevice;
    private final ByteBuffer fbMapping;
    private final FileChannel fbChannel;
    private final Framebuffer pixels;
    private final int width;
    private final int height;
    private final int bitDepth;

    private boolean isShutdown;

    /**
     * Creates a native screen for the electrophoretic display.
     *
     * @throws IllegalStateException if an error occurs opening the frame buffer
     */
    EPDScreen() {
        @SuppressWarnings("removal")
        String tmp = AccessController.doPrivileged((PrivilegedAction<String>) ()
                -> System.getProperty(FB_PATH_KEY, FB_PATH_DEFAULT));
        fbPath = tmp;
        try {
            fbDevice = new EPDFrameBuffer(fbPath);
            fbDevice.init();

            width = fbDevice.getWidth();
            height = fbDevice.getHeight();
            bitDepth = fbDevice.getBitDepth();
            logger.fine("Native screen geometry: {0} px x {1} px x {2} bpp",
                    width, height, bitDepth);

            /*
             * If the Linux frame buffer is configured for 32-bit color, compose
             * the pixels directly into it. Otherwise, compose the pixels into
             * an off-screen buffer and write them to the frame buffer when
             * swapping buffers.
             *
             * With an LCD display, there must be space for two full screens to
             * be able to write directly into the frame buffer, displaying one
             * while updating the other. The Snapshot update mode of an e-paper
             * display, though, allows us to reuse the same frame buffer region
             * immediately after sending an update.
             */
            ByteBuffer mapping = null;
            if (bitDepth == Integer.SIZE) {
                mapping = fbDevice.getMappedBuffer();
            }
            if (mapping != null) {
                fbMapping = mapping;
                fbChannel = null;
            } else {
                Path path = FileSystems.getDefault().getPath(fbPath);
                fbChannel = FileChannel.open(path, StandardOpenOption.WRITE);
                fbMapping = null;
            }
        } catch (IOException e) {
            String msg = MessageFormat.format("Failed opening frame buffer: {0}", fbPath);
            logger.severe(msg, e);
            throw new IllegalStateException(msg, e);
        }

        /*
         * Note that pixels.clearBufferContents() throws a NullPointerException
         * if the last parameter of its constructor ("clear") is false.
         */
        ByteBuffer buffer = fbMapping != null ? fbMapping : fbDevice.getOffscreenBuffer();
        buffer.order(ByteOrder.nativeOrder());
        pixels = new FramebufferY8(buffer, width, height, bitDepth, true);
        clearScreen();
    }

    /**
     * Closes the Linux frame buffer device and related resources. Called only
     * from the {@link #shutdown} method, which is called only once.
     */
    private void close() {
        try {
            if (fbChannel != null) {
                fbChannel.close();
            }
        } catch (IOException e) {
            logger.severe("Failed closing frame buffer channel", e);
        } finally {
            if (fbMapping != null) {
                fbDevice.releaseMappedBuffer(fbMapping);
            }
            fbDevice.close();
        }
    }

    /**
     * Writes the content of the off-screen buffer to the Linux frame buffer, if
     * necessary. If the frame buffer is mapped, the content to display is
     * already there, and this method does nothing.
     */
    private void writeBuffer() {
        if (fbChannel != null) {
            try {
                fbChannel.position(fbDevice.getByteOffset());
                pixels.write(fbChannel);
            } catch (IOException e) {
                logger.severe("Failed writing to frame buffer channel", e);
            }
        }
    }

    /**
     * Clears the screen.
     */
    private void clearScreen() {
        pixels.clearBufferContents();
        writeBuffer();
        fbDevice.clear();
    }

    @Override
    public int getDepth() {
        return bitDepth;
    }

    @Override
    public int getNativeFormat() {
        /*
         * The native pixel format must be one of either
         * Pixels.Format.BYTE_BGRA_PRE when the system byte order is
         * ByteOrder.LITTLE_ENDIAN, or Pixels.Format.BYTE_ARGB when the system
         * byte order is ByteOrder.BIG_ENDIAN. The ARMv7-A architecture is
         * little endian by default.
         */
        return Pixels.Format.BYTE_BGRA_PRE;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getDPI() {
        return DPI;
    }

    @Override
    public long getNativeHandle() {
        return fbDevice.getNativeHandle();
    }

    @Override
    public synchronized void shutdown() {
        close();
        isShutdown = true;
    }

    @Override
    public synchronized void uploadPixels(Buffer b, int x, int y, int width, int height, float alpha) {
        pixels.composePixels(b, x, y, width, height, alpha);
    }

    @Override
    public synchronized void swapBuffers() {
        if (!isShutdown && pixels.hasReceivedData()) {
            writeBuffer();
            fbDevice.sync();
            pixels.reset();
        }
    }

    @Override
    public synchronized ByteBuffer getScreenCapture() {
        return pixels.getBuffer().asReadOnlyBuffer();
    }

    @Override
    public float getScale() {
        return SCALE;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}[width={1} height={2} depth={3} DPI={4} scale={5,number,0.0#}]",
                getClass().getName(), getWidth(), getHeight(), getDepth(), getDPI(), getScale());
    }
}
