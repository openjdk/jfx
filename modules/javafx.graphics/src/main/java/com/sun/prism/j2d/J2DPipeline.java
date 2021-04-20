/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.j2d;

import com.sun.glass.ui.Screen;
import com.sun.javafx.font.FontFactory;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.ResourceFactory;

import java.util.List;
import java.util.HashMap;

public class J2DPipeline extends GraphicsPipeline {
    @Override
    public boolean init() {
        return true;
    }

    private static J2DPipeline theInstance;

    private J2DPipeline() {
    }

    public static J2DPipeline getInstance() {
        if (theInstance == null) {
            theInstance = new J2DPipeline();
        }
        return theInstance;
    }

    private final HashMap<Integer, J2DResourceFactory> factories =
            new HashMap<Integer, J2DResourceFactory>(1);

    @Override
    public int getAdapterOrdinal(Screen screen) {
        return Screen.getScreens().indexOf(screen);
    }

    @Override
    public ResourceFactory getResourceFactory(Screen screen) {
        Integer index = Integer.valueOf(screen.getAdapterOrdinal());
        J2DResourceFactory factory = factories.get(index);
        if (factory == null) {
            factory = new J2DResourceFactory(screen);
            factories.put(index, factory);
        }
        return factory;
    }

    @Override
    public ResourceFactory getDefaultResourceFactory(List<Screen> screens) {
        return getResourceFactory(Screen.getMainScreen());
    }

    @Override
    public boolean is3DSupported() {
        return false;
    }

    @Override
    public boolean isVsyncSupported() {
        return false;
    }

    @Override
    public boolean supportsShaderType(ShaderType type) {
        return false;
    }

    @Override
    public boolean supportsShaderModel(ShaderModel model) {
        return false;
    }

    @Override
    public void dispose() {
        // TODO: implement (RT-27375)
        super.dispose();
    }

    /*
     * The J2DFontFactory is mostly just a delegate, but as a
     * consequence it gets to listen in on all the requests and
     * intercept them as it needs.
     */
    private FontFactory j2DFontFactory;

    public FontFactory getFontFactory() {
        if (j2DFontFactory == null) {
            FontFactory fontFactory = super.getFontFactory();
            j2DFontFactory = new J2DFontFactory(fontFactory);
        }
        return j2DFontFactory;
    }

    @Override
    public boolean isUploading() {
        return true;
    }
}
