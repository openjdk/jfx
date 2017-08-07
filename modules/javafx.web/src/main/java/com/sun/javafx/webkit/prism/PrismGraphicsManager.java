/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.media.jfxmedia.MediaManager;
import com.sun.prism.Graphics;
import com.sun.webkit.perf.WCFontPerfLogger;
import com.sun.webkit.perf.WCGraphicsPerfLogger;
import com.sun.webkit.graphics.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class PrismGraphicsManager extends WCGraphicsManager {

    private final static float highestPixelScale;
    private final static BaseTransform pixelScaleTransform;

    static {
        float ps = 1f;
        for (Screen s : Screen.getScreens()) {
            ps = Math.max(s.getRecommendedOutputScaleX(), ps);
            ps = Math.max(s.getRecommendedOutputScaleY(), ps);
        }
        highestPixelScale = (float) Math.ceil(ps);
        pixelScaleTransform = BaseTransform.getScaleInstance(ps, ps);
    }

    static BaseTransform getPixelScaleTransform() {
        return pixelScaleTransform;
    }

    @Override public float getDevicePixelScale() {
        return highestPixelScale;
    }

    @Override protected WCImageDecoder getImageDecoder() {
        return new WCImageDecoderImpl();
    }

    @Override public WCRenderQueue createRenderQueue(WCRectangle clip,
                                                     boolean opaque)
    {
        return new WCRenderQueueImpl(clip, opaque);
    }

    @Override protected WCRenderQueue createBufferedContextRQ(WCImage image) {
        WCGraphicsContext g = new WCBufferedContext((PrismImage) image);
        WCRenderQueue rq = new WCRenderQueueImpl(
                WCGraphicsPerfLogger.isEnabled()
                        ? new WCGraphicsPerfLogger(g) : g);
        image.setRQ(rq);
        return rq;
    }

    @Override protected WCFont getWCFont(String name, boolean bold, boolean italic, float size)
    {
        WCFont f = WCFontImpl.getFont(name, bold, italic, size);
        return WCFontPerfLogger.isEnabled() && (f != null) ? new WCFontPerfLogger(f) : f;
    }

    @Override
    protected WCFontCustomPlatformData createFontCustomPlatformData(
            InputStream inputStream) throws IOException
    {
        return new WCFontCustomPlatformDataImpl(inputStream);
    }

    @Override
    public WCGraphicsContext createGraphicsContext(Object platG) {
        WCGraphicsContext g = new WCGraphicsPrismContext((Graphics)platG);
        return WCGraphicsPerfLogger.isEnabled() ? new WCGraphicsPerfLogger(g) : g;
    }

    @Override public WCPageBackBuffer createPageBackBuffer() {
        return new WCPageBackBufferImpl(highestPixelScale);
    }

    @Override
    protected WCPath createWCPath() {
        return new WCPathImpl();
    }

    @Override
    protected WCPath createWCPath(WCPath path) {
        return new WCPathImpl((WCPathImpl)path);
    }

    @Override
    protected WCImage createWCImage(int w, int h) {
        return new WCImageImpl(w, h);
    }

    @Override
    protected WCImage createRTImage(int w, int h) {
        return new RTImage(w, h, highestPixelScale);
    }

    @Override public WCImage getIconImage(String iconURL) {
        return null;
    }

    @Override public Object toPlatformImage(WCImage image) {
        return ((WCImageImpl) image).getImage();
    }

    @Override
    protected WCImageFrame createFrame(int w, int h, ByteBuffer bytes) {
        int[] data = new int[bytes.capacity() / 4];
        bytes.order(ByteOrder.nativeOrder());
        bytes.asIntBuffer().get(data);
        final WCImageImpl wimg = new WCImageImpl(data, w, h);

        return new WCImageFrame() {
            @Override public WCImage getFrame() { return wimg; }
            @Override public int[] getSize() { return new int[] {w, h}; }
        };
    }

    @Override
    protected WCTransform createTransform(double m00, double m10, double m01,
            double m11, double m02, double m12)
    {
        return new WCTransform(m00, m10, m01, m11, m02, m12);
    }

    @Override
    protected String[] getSupportedMediaTypes() {
        String[] types = MediaManager.getSupportedContentTypes();
        // RT-19949: disable FLV support (workaround for youtube):
        // if browser reports support for video/x-flv, youtube player sets
        // media source to FLV (H264+AAC) stream and does not switch to MP4 on error
        int len = types.length;
        for (int i=0; i<len; i++) {
            if ("video/x-flv".compareToIgnoreCase(types[i]) == 0) {
                System.arraycopy(types, i+1, types, i, len-(i+1));
                len--;
            }
        }
        if (len < types.length) {
            String[] trimmedArray = new String[len];
            System.arraycopy(types, 0, trimmedArray, 0, len);
            types = trimmedArray;
        }
        return types;
    }

    @Override
    protected WCMediaPlayer createMediaPlayer() {
        return new WCMediaPlayerImpl();
    }
}
