/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.prism;

import com.sun.prism.Graphics;
import com.sun.webkit.graphics.WCImage;

final class WCBufferedContext extends WCGraphicsPrismContext {

    private final PrismImage img;

    WCBufferedContext(PrismImage img) {
        this.img = img;
        setClip(0, 0, img.getWidth(), img.getHeight());
    }

    @Override
    public WCImage getImage() {
        return img;
    }

    @Override
    Graphics getGraphics(boolean checkClip) {
        init(img.getGraphics(), false);
        return super.getGraphics(checkClip);
    }
}
