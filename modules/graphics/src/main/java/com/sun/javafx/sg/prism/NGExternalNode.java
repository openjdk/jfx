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

import java.nio.Buffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import com.sun.javafx.geom.Rectangle;
import com.sun.prism.Graphics;
import com.sun.prism.PixelFormat;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;

public class NGExternalNode extends NGNode {
    
    private Texture dsttexture;

    private BufferData bufferData;
    private final AtomicReference<RenderData> renderData = new AtomicReference<RenderData>(null);    
    private RenderData rd; // last rendered data

    private volatile ReentrantLock bufferLock;

    @Override
    protected void renderContent(Graphics g) {
        
        RenderData curRenderData = renderData.getAndSet(null);
        
        if (curRenderData != null) {
            rd = curRenderData;
        }
        if (rd == null) return;
        
        int x = rd.bdata.srcbounds.x;
        int y = rd.bdata.srcbounds.y;
        int w = rd.bdata.srcbounds.width;
        int h = rd.bdata.srcbounds.height;

        if (dsttexture != null) {
            
            dsttexture.lock();
            
            if (dsttexture.isSurfaceLost() ||
               (dsttexture.getContentWidth() != rd.bdata.dstwidth) ||
               (dsttexture.getContentHeight() != rd.bdata.dstheight))
            {
                dsttexture.unlock();
                dsttexture.dispose();
                dsttexture = createTexture(g, rd);
            }
        } else {
            dsttexture = createTexture(g, rd);
        }
        if (dsttexture == null) {
            return;
        }        
        try {
            if (curRenderData != null) {
                bufferLock.lock();
                try {
                    dsttexture.update(rd.bdata.srcbuffer,
                                      PixelFormat.INT_ARGB_PRE,
                                      x + rd.dirtyRect.x, y + rd.dirtyRect.y, // dst
                                      x + rd.dirtyRect.x, y + rd.dirtyRect.y,  // src
                                      rd.dirtyRect.width, rd.dirtyRect.height, // src
                                      rd.bdata.linestride * 4,
                                      false);
                } finally {
                    bufferLock.unlock();
                }
                if (rd.clearTarget) {
                    g.clear();
                }
            }
            g.drawTexture(dsttexture,
                          x, y, x + w, y + h, // dst
                          x, y, x + w, y + h); // src
        } finally {        
            dsttexture.unlock();
        }
    }
    
    private Texture createTexture(Graphics g, RenderData rd) {
        ResourceFactory factory = g.getResourceFactory();
        if (!factory.isDeviceReady()) {
            System.err.println("NGExternalNode: graphics device is not ready");
            return null;
        }                
        Texture txt = factory.createTexture(PixelFormat.INT_ARGB_PRE,
                                            Texture.Usage.DYNAMIC,
                                            Texture.WrapMode.CLAMP_NOT_NEEDED,
                                            rd.bdata.dstwidth, rd.bdata.dstheight);            
        if (txt != null) {
            txt.contentsUseful();
        } else {
            System.err.println("NGExternalNode: failed to create a texture");            
        }
        return txt;
    }

    public void setLock(ReentrantLock lock) {
        this.bufferLock = lock;
    }

    private static class BufferData {
        // the source pixel buffer
        final Buffer srcbuffer;

        // line stride of the source buffer
        final int linestride;

        // source image bounds
        final Rectangle srcbounds;
        
        // source image buffer size
        final int dstwidth;
        final int dstheight;
        
        BufferData(Buffer srcbuffer, int linestride,
                   int x, int y, int width, int height)
        {
            this.srcbuffer = srcbuffer;            
            this.linestride = linestride;
            this.srcbounds = new Rectangle(x, y, width, height);
            this.dstwidth = linestride;
            this.dstheight = srcbuffer.capacity() / linestride;            
        }
        
        BufferData copyWithBounds(int x, int y, int width, int height) {            
            return new BufferData(this.srcbuffer, this.linestride, x, y, width, height);
        }
    }
    
    private static class RenderData {
        final BufferData bdata;
        final Rectangle dirtyRect;
        final boolean clearTarget;
        
        RenderData(BufferData bdata,
                   int dirtyX, int dirtyY, int dirtyWidth, int dirtyHeight,
                   boolean clearTarget)
        {
            this.bdata = bdata;
            this.dirtyRect = new Rectangle(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
            this.clearTarget = clearTarget;
        }

        RenderData copyAddDirtyRect(int dirtyX, int dirtyY, int dirtyWidth, int dirtyHeight) {
            
            Rectangle r = new Rectangle(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
            r.add(this.dirtyRect);
            return new RenderData(this.bdata,
                                  r.x, r.y, r.width, r.height,
                                  this.clearTarget);
        }
    }
    
    public void setImageBuffer(Buffer buffer,
                               int x, int y, int width, int height,
                               int linestride)
    {
        bufferData = new BufferData(buffer, linestride, x, y, width, height);
        renderData.set(new RenderData(bufferData, x, y, width, height, true));
    }

    public void setImageBounds(final int x, final int y, final int width, final int height) {
        
        final boolean shrinked = width < bufferData.srcbounds.width ||
                                 height < bufferData.srcbounds.height;
        
        bufferData = bufferData.copyWithBounds(x, y, width, height);
        renderData.updateAndGet(prev -> {
            boolean clearTarget = (prev != null ? prev.clearTarget : false);
            return new RenderData(bufferData, x, y, width, height, clearTarget | shrinked);
        });
    }

    public void repaintDirtyRegion(final int dirtyX, final int dirtyY,
                                   final int dirtyWidth, final int dirtyHeight)
    {
        renderData.updateAndGet(prev -> {
            if (prev != null) {
                return prev.copyAddDirtyRect(dirtyX, dirtyY, dirtyWidth, dirtyHeight);
            } else {
                return new RenderData(bufferData, dirtyX, dirtyY, dirtyWidth, dirtyHeight, false);
            }
        });
    }
    
    public void markContentDirty() {
        visualsChanged();
    }
    
    @Override
    protected boolean hasOverlappingContents() {  return false; }
}
