/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.sg.PGExternalNode;

import com.sun.prism.Graphics;
import com.sun.prism.PixelFormat;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;

import java.nio.Buffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NGExternalNode extends NGNode implements PGExternalNode {

    // pixel buffer of the source image
    volatile private Buffer srcbuffer;

    // line stride of the source buffer
    volatile private int linestride;

    // source image bounds
    volatile private int srcx;
    volatile private int srcy;
    volatile private int srcwidth;
    volatile private int srcheight;

    // of the same size as the source image buffer
    private Texture dsttexture;

    // size of the source image buffer
    volatile private int dstwidth;
    volatile private int dstheight;

    // when the content shrinks, we need to clear the target
    volatile private boolean clearTarget = false;

    // relative to the [srcx, srcy]
    volatile private Rectangle dirtyRect;
    volatile private boolean isDirty;

    volatile private Lock paintLock;

    @Override
    protected void renderContent(Graphics g) {
        paintLock.lock();
        try {
            if (srcbuffer == null) {
                // the buffer may be initialized with some delay, asynchronously
                return;
            }
            if ((dsttexture == null) ||
                (dsttexture.getContentWidth() != dstwidth) ||
                (dsttexture.getContentHeight() != dstheight))
            {
                ResourceFactory factory = g.getResourceFactory();
                if (!factory.isDeviceReady()) {
                    System.err.println("NGExternalNode: graphics device is not ready");
                    return;
                }
                if (dsttexture != null) {
                    dsttexture.dispose();
                }
                dsttexture =
                    factory.createTexture(PixelFormat.INT_ARGB_PRE,
                                          Texture.Usage.DYNAMIC,
                                          Texture.WrapMode.CLAMP_NOT_NEEDED,
                                          dstwidth, dstheight);

                if (dsttexture == null) {
                    System.err.println("NGExternalNode: failed to create a texture");
                    return;
                }
            }
            if (dirtyRect == null) return;

            dsttexture.update(srcbuffer,
                              PixelFormat.INT_ARGB_PRE,
                              srcx + dirtyRect.x, srcy + dirtyRect.y, // dst
                              srcx + dirtyRect.x, srcy + dirtyRect.y, dirtyRect.width, dirtyRect.height, // src
                              linestride * 4,
                              false);

            if (clearTarget) {
                clearTarget = false;
                g.clear();
            }
            g.drawTexture(dsttexture,
                          srcx, srcy, srcx + srcwidth, srcy + srcheight, // dst
                          srcx, srcy, srcx + srcwidth, srcy + srcheight); // src

            isDirty = false;

        } finally {
            paintLock.unlock();
        }
    }

    @Override
    public void setLock(ReentrantLock lock) {
        this.paintLock = lock;
    }

    @Override
    public void setImageBuffer(Buffer buffer, int x, int y, int width, int height, int linestride) {
        paintLock.lock();
        try {
            this.srcbuffer = buffer;

            this.srcx = x;
            this.srcy = y;
            this.srcwidth = width;
            this.srcheight = height;

            this.linestride = linestride;

            this.dstwidth = linestride;
            this.dstheight = buffer.capacity() / linestride;

            dirtyRect = new Rectangle(x, y, width, height);

        } finally {
            paintLock.unlock();
        }
    }

    @Override
    public void setImageBounds(int x, int y, int width, int height) {
        paintLock.lock();
        try {
            // content shrinked
            if (width < srcwidth || height < srcheight) {
                clearTarget = true;
            }
            this.srcx = x;
            this.srcy = y;
            this.srcwidth = width;
            this.srcheight = height;

            dirtyRect.setBounds(x, y, width, height);

        } finally {
            paintLock.unlock();
        }
    }

    @Override
    public void repaintDirtyRegion(int dirtyX, int dirtyY, int dirtyWidth, int dirtyHeight) {
        paintLock.lock();
        try {
            if (isDirty) {
                dirtyRect.add(new Rectangle(dirtyX, dirtyY, dirtyWidth, dirtyHeight));
            } else {
                dirtyRect.setBounds(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
            }
            // System.out.println("NGExternalNode.repaintDirtyRegion: " + dirtyRect);

            isDirty = true;
            visualsChanged();

        } finally {
            paintLock.unlock();
        }
    }

    @Override
    protected boolean hasOverlappingContents() {  return false; }
}
