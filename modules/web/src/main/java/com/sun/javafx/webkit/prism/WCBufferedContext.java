/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.prism;

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.Graphics;
import com.sun.webkit.graphics.WCImage;

final class WCBufferedContext extends WCGraphicsPrismContext {

    private final PrismImage img;
    private boolean isInitialized;

    WCBufferedContext(PrismImage img) {
        this.img = img;
    }

    @Override
    public WCImage getImage() {
        return img;
    }

    @Override Graphics getGraphics(boolean checkClip) {
        init();
        return super.getGraphics(checkClip);
    }
    
    @Override public void saveState() {
        init();
        super.saveState();
    }
    
    private void init() {
        if (! isInitialized) {
            Graphics g = img.getGraphics();
            init(g, false);
            
            BaseTransform t = g.getTransformNoClone();
            int w = (int) Math.ceil(img.getWidth() * t.getMxx());
            int h = (int) Math.ceil(img.getHeight() * t.getMyy());
            setClip(0, 0, w, h);

            isInitialized = true;
        }
    }
}
