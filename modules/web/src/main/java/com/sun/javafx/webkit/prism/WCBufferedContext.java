/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
