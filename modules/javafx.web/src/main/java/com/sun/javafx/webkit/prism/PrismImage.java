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

import com.sun.javafx.tk.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

import com.sun.javafx.webkit.UIClientImpl;
import com.sun.prism.Image;
import com.sun.prism.Graphics;
import com.sun.webkit.graphics.WCImage;

/**
 * @author Alexey.Ushakov
 */
abstract class PrismImage extends WCImage {
    abstract Image getImage();
    abstract Graphics getGraphics();
    abstract void draw(Graphics g,
            int dstx1, int dsty1, int dstx2, int dsty2,
            int srcx1, int srcy1, int srcx2, int srcy2);
    abstract void dispose();

    @Override
    public Object getPlatformImage() {
       return getImage();
    }

    @Override
    public void deref() {
        super.deref();
        if (!hasRefs()) {
            dispose();
        }
    }

    @Override
    protected final byte[] toData(String mimeType) {
        Object image = UIClientImpl.toBufferedImage(Toolkit.getImageAccessor().fromPlatformImage(getImage()));
        if (image instanceof BufferedImage) {
            Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType(mimeType);
            while (it.hasNext()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageWriter writer = it.next();
                try {
                    writer.setOutput(ImageIO.createImageOutputStream(output));
                    writer.write((BufferedImage) image);
                }
                catch (IOException exception) {
                    continue; // try next image writer
                }
                finally {
                    writer.dispose();
                }
                return output.toByteArray();
            }
        }
        return null;
    }

    @Override
    protected final String toDataURL(String mimeType) {
        final byte[] data = toData(mimeType);
        if (data != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("data:").append(mimeType).append(";base64,");
            sb.append(Base64.getMimeEncoder().encodeToString(data));
            return sb.toString();
        }
        return null;
    }
}
