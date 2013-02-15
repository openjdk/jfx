/*
 * Copyright (c) 2009, 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.prism.ps;

import com.sun.glass.ui.Screen;
import com.sun.prism.RTTexture;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.ps.ShaderGraphics;
import com.sun.scenario.effect.impl.prism.PrDrawable;

public class PPSDrawable extends PrDrawable {

    private RTTexture rtt;

    private PPSDrawable(RTTexture rtt) {
        super(rtt);
        this.rtt = rtt;
    }

    static PPSDrawable create(RTTexture rtt) {
        return new PPSDrawable(rtt);
    }

    static PPSDrawable create(Screen screen, int width, int height) {
        ResourceFactory factory =
            GraphicsPipeline.getPipeline().getResourceFactory(screen);
        // force the wrap mode to CLAMP_TO_ZERO, as that is the mode
        // required by most Decora effects (blurs, etc)
        RTTexture rtt =
            factory.createRTTexture(width, height, WrapMode.CLAMP_TO_ZERO);
        return new PPSDrawable(rtt);
    }

    public boolean isLost() {
        return rtt == null || rtt.isSurfaceLost();
    }

    public void flush() {
        if (rtt != null) {
            rtt.dispose();
            rtt = null;
        }
    }

    public Object getData() {
        return this;
    }

    public int getContentWidth() {
        return rtt.getContentWidth();
    }

    public int getContentHeight() {
        return rtt.getContentHeight();
    }

    public int getPhysicalWidth() {
        return rtt.getPhysicalWidth();
    }

    public int getPhysicalHeight() {
        return rtt.getPhysicalHeight();
    }

    public ShaderGraphics createGraphics() {
        return (ShaderGraphics)rtt.createGraphics();
    }
}
