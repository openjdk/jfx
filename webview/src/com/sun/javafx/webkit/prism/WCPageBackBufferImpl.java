/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.prism;

import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.Image;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactoryListener;
import com.sun.prism.Texture;
import com.sun.prism.paint.Color;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.webkit.graphics.WCGraphicsManager;
import com.sun.webkit.graphics.WCPageBackBuffer;

final class WCPageBackBufferImpl extends WCPageBackBuffer implements ResourceFactoryListener {
    private RTTexture texture;
    private boolean listenerAdded = false;

    private static RTTexture createTexture(int w, int h) {
        return GraphicsPipeline.getDefaultResourceFactory()
                .createRTTexture(w, h, Texture.WrapMode.CLAMP_NOT_NEEDED);
    }

    public WCGraphicsContext createGraphics() {
        return WCGraphicsManager.getGraphicsManager().createGraphicsContext(texture.createGraphics());
    }

    public void disposeGraphics(WCGraphicsContext gc) {
        gc.dispose();
    }

    public void flush(final WCGraphicsContext gc, int x, int y, final int w, final int h) {
        PrismImage img = new PrismImage() {
            @Override public int getWidth() {
                return w;
            }
            @Override public int getHeight() {
                return h;
            }
            @Override public Graphics getGraphics() {
                return (Graphics)gc.getPlatformGraphics();
            }
            @Override public Image getImage() {
                throw new UnsupportedOperationException();
            }
            @Override public void draw(Graphics g,
                    int dstx1, int dsty1, int dstx2, int dsty2,
                    int srcx1, int srcy1, int srcx2, int srcy2)
            {
                g.drawTexture(texture,
                        dstx1, dsty1, dstx2, dsty2,
                        srcx1, srcy1, srcx2, srcy2);
            }
            @Override public void dispose() {
                throw new UnsupportedOperationException();
            }
        };
        gc.drawImage(img, x, y, w, h, x, y, w, h);
    }

    protected void copyArea(int x, int y, int w, int h, int dx, int dy) {
        RTTexture aux = createTexture(w, h);
        aux.createGraphics().drawTexture(texture, 0, 0, w, h, x, y, x + w, y + h);
        texture.createGraphics().drawTexture(aux, x + dx, y + dy, x + w + dx, y + h + dy,
                                             0, 0, w, h);
        aux.dispose();
    }

    public boolean validate(int width, int height) {
        if (texture == null) {
            texture = createTexture(width, height);
            texture.contentsUseful();
            texture.makePermanent();
            if (! listenerAdded) {
                // this is the very first time validate() is called. We assume
                // full repaint is already happening, so we don't return false
                GraphicsPipeline.getDefaultResourceFactory().addFactoryListener(this);
                listenerAdded = true;
            } else {
                // texture must have been nullified in factoryReset().
                // Backbuffer is lost, so we request full repaint.
                return false;
            }
        } else {
            int tw = texture.getContentWidth();
            int th = texture.getContentHeight();
            if (tw != width || th != height) {
                // Change the texture size
                RTTexture newTexture = createTexture(width, height);
                newTexture.contentsUseful();
                newTexture.makePermanent();
                newTexture.createGraphics().drawTexture(texture, 0, 0,
                        Math.min(width, tw), Math.min(height, th));
                texture.dispose();
                texture = newTexture;
            }
        }
        return true;
    }

    @Override public void factoryReset() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }

    @Override public void factoryReleased() {
    }
}
