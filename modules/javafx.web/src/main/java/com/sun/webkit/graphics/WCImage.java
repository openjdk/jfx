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

package com.sun.webkit.graphics;

import java.nio.ByteBuffer;

public abstract class WCImage extends Ref {
    private WCRenderQueue rq;
    private String fileExtension;

    public abstract int getWidth();

    public abstract int getHeight();

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public Object getPlatformImage() {return null;}

    protected abstract byte[] toData(String mimeType);

    protected abstract String toDataURL(String mimeType);

    public ByteBuffer getPixelBuffer() {return null;}

    protected void drawPixelBuffer() {}

    public synchronized void setRQ(WCRenderQueue rq) {
        this.rq = rq;
    }

    // should be called on render thread
    protected synchronized void flushRQ() {
        if (rq != null) {
            rq.decode();
        }
    }

    protected synchronized boolean isDirty() {
        return (rq == null)
           ? false
           : !rq.isEmpty();
    }

    public static WCImage getImage(Object imgFrame) {
        WCImage img = null;
        if (imgFrame instanceof WCImage) {
            //from BufferImage.drawPattern (canvas/fill layer):
            //NativeImagePtr is a wrapper over the WCImage
            img = (WCImage)imgFrame;
        } else if (imgFrame instanceof WCImageFrame) {
            //from BitmapImage.drawPattern (decoder/GIF animator):
            //NativeImagePtr is a wrapper over the WCImageFrame
            img = ((WCImageFrame)imgFrame).getFrame();
        }
        return img;
    }

    public abstract float getPixelScale();
}
