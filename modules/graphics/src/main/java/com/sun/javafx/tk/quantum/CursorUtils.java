/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javafx.geometry.Dimension2D;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Size;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.ImageCursorFrame;
import com.sun.javafx.iio.common.PushbroomScaler;
import com.sun.javafx.iio.common.ScalerFactory;

final class CursorUtils {
    private CursorUtils() {
    }

    public static Cursor getPlatformCursor(final CursorFrame cursorFrame) {
        final Cursor cachedPlatformCursor =
                cursorFrame.getPlatformCursor(Cursor.class);
        if (cachedPlatformCursor != null) {
            // platform cursor already cached
            return cachedPlatformCursor;
        }

        // platform cursor not cached yet
        final Cursor platformCursor = createPlatformCursor(cursorFrame);
        cursorFrame.setPlatforCursor(Cursor.class, platformCursor);

        return platformCursor;
    }

    public static Dimension2D getBestCursorSize(int preferredWidth,
                                                int preferredHeight) {
        Size size = Cursor.getBestSize(preferredWidth, preferredHeight);
        return new Dimension2D(size.width, size.height);
    }

    private static Cursor createPlatformCursor(final CursorFrame cursorFrame) {
        Application app = Application.GetApplication();
        switch (cursorFrame.getCursorType()) {
            case CROSSHAIR:
                return app.createCursor(Cursor.CURSOR_CROSSHAIR);
            case TEXT:
                return app.createCursor(Cursor.CURSOR_TEXT);
            case WAIT:
                return app.createCursor(Cursor.CURSOR_WAIT);
            case DEFAULT:
                return app.createCursor(Cursor.CURSOR_DEFAULT);
            case OPEN_HAND:
                return app.createCursor(Cursor.CURSOR_OPEN_HAND);
            case CLOSED_HAND:
                return app.createCursor(Cursor.CURSOR_CLOSED_HAND);
            case HAND:
                return app.createCursor(Cursor.CURSOR_POINTING_HAND);
            case H_RESIZE:
                return app.createCursor(Cursor.CURSOR_RESIZE_LEFTRIGHT);
            case V_RESIZE:
                return app.createCursor(Cursor.CURSOR_RESIZE_UPDOWN);
            case MOVE:
                return app.createCursor(Cursor.CURSOR_MOVE);
            case DISAPPEAR:
                return app.createCursor(Cursor.CURSOR_DISAPPEAR);
            case SW_RESIZE:
                return app.createCursor(Cursor.CURSOR_RESIZE_SOUTHWEST);
            case SE_RESIZE:
                return app.createCursor(Cursor.CURSOR_RESIZE_SOUTHEAST);
            case NW_RESIZE:
                return app.createCursor(Cursor.CURSOR_RESIZE_NORTHWEST);
            case NE_RESIZE:
                return app.createCursor(Cursor.CURSOR_RESIZE_NORTHEAST);
            case N_RESIZE:
            case S_RESIZE:
                return app.createCursor(Cursor.CURSOR_RESIZE_UPDOWN);
            case W_RESIZE:
            case E_RESIZE:
                return app.createCursor(Cursor.CURSOR_RESIZE_LEFTRIGHT);
            case NONE:
                return app.createCursor(Cursor.CURSOR_NONE);
            case IMAGE:
                return createPlatformImageCursor(
                               (ImageCursorFrame) cursorFrame);
            default:
                System.err.println("unhandled Cursor: "
                        + cursorFrame.getCursorType());
                return app.createCursor(Cursor.CURSOR_DEFAULT);
        }
    }

    private static Cursor createPlatformImageCursor(
            final ImageCursorFrame imageCursorFrame) {
        return createPlatformImageCursor(
                       imageCursorFrame.getPlatformImage(),
                       (float) imageCursorFrame.getHotspotX(),
                       (float) imageCursorFrame.getHotspotY());
    }

    private static Cursor createPlatformImageCursor(Object platformImage,
                                                    float hotspotX,
                                                    float hotspotY) {
        if (platformImage == null) {
            throw new IllegalArgumentException(
                    "QuantumToolkit.createImageCursor: no image");
        }

        assert platformImage instanceof com.sun.prism.Image;

        com.sun.prism.Image prismImage = (com.sun.prism.Image) platformImage;

        int iheight = prismImage.getHeight();
        int iwidth = prismImage.getWidth();
        Dimension2D d = getBestCursorSize(iwidth, iheight);
        float bestWidth = (float)d.getWidth();
        float bestHeight = (float)d.getHeight();

        if ((bestWidth <= 0) || (bestHeight <= 0)) {
            return Application.GetApplication()
                              .createCursor(Cursor.CURSOR_DEFAULT);
        }

        ByteBuffer buf;
        switch (prismImage.getPixelFormat()) {
            case INT_ARGB_PRE:
                return createPlatformImageCursor((int) hotspotX, (int) hotspotY,
                                                 iwidth, iheight,
                                                 prismImage.getPixelBuffer());
            case BYTE_RGB:
            case BYTE_BGRA_PRE:
            case BYTE_GRAY:
                buf = (ByteBuffer) prismImage.getPixelBuffer();
                break;
            default:
                throw new IllegalArgumentException(
                        "QuantumToolkit.createImageCursor: bad image format");
        }

        float xscale = bestWidth / (float)iwidth;
        float yscale = bestHeight / (float)iheight;

        int scaledHotSpotX = (int) (hotspotX * xscale);
        int scaledHotSpotY = (int) (hotspotY * yscale);

        PushbroomScaler scaler;
        scaler = ScalerFactory.createScaler(iwidth, iheight,
                prismImage.getBytesPerPixelUnit(),
                (int)bestWidth, (int)bestHeight, true);

        //shrink the image and convert the format to INT_ARGB_PRE
        byte bytes[] = new byte[buf.limit()];

        //Iterate through each scanline of the image
        //and pass it one at a time to the scaling object
        int scanlineStride = prismImage.getScanlineStride();
        for (int z = 0; z < iheight; z++) {
            buf.position(z * scanlineStride);
            buf.get(bytes, 0, scanlineStride);
            if (scaler != null) {
                scaler.putSourceScanline(bytes, 0);
            }
        }

        buf.rewind();

        final com.sun.prism.Image img =
                prismImage.iconify(scaler.getDestination(),
                                   (int)bestWidth,
                                   (int)bestHeight);

        return createPlatformImageCursor(scaledHotSpotX, scaledHotSpotY,
                                         img.getWidth(), img.getHeight(),
                                         img.getPixelBuffer());
    }

    private static Cursor createPlatformImageCursor(int x, int y,
                                                    int width,
                                                    int height,
                                                    Object buffer) {
        Application app = Application.GetApplication();
        return app.createCursor(x, y, app.createPixels(width, height,
                                                       (IntBuffer) buffer));
    }
}
