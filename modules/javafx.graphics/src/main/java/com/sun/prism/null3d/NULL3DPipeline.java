/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.null3d;

import com.sun.glass.ui.Screen;
import com.sun.javafx.PlatformUtil;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.ResourceFactory;
import com.sun.prism.impl.PrismSettings;

import java.util.List;
import java.util.HashMap;

public class NULL3DPipeline extends GraphicsPipeline {

    static {
        if (PrismSettings.verbose) {
            System.out.println("NULL3DPipeline enabled !");
        }
    }

    static NULL3DPipeline theInstance;

    public static NULL3DPipeline getInstance() {
        if (theInstance == null) {
            theInstance = new NULL3DPipeline();
        }
        return theInstance;
    }

    public boolean init() {
        return true;
    }


    public void dispose() {
    }

    private final HashMap<Integer, DummyResourceFactory> factories =
            new HashMap<Integer, DummyResourceFactory>(1);

    @Override
    public int getAdapterOrdinal(Screen screen) {
        return Screen.getScreens().indexOf(screen);
    }

    @Override
    public ResourceFactory getResourceFactory(Screen screen) {
        Integer index = Integer.valueOf(screen.getAdapterOrdinal());
        DummyResourceFactory factory = factories.get(index);
        if (factory == null) {
            factory = new DummyResourceFactory(screen);
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
        return true;
    }

    @Override
    public boolean isMSAASupported() {
        return true;
    }

    @Override
    public boolean isVsyncSupported() {
        return false;
    }

    @Override
    public boolean supportsShaderType(ShaderType type) {
        switch (type) {
            case HLSL: return PlatformUtil.isWindows();
            case GLSL: return !PlatformUtil.isWindows();
            default: return false;
        }
    }

    @Override
    public boolean supportsShaderModel(ShaderModel model) {
        switch (model) {
            case SM3: return true;
            default: return false;
        }
    }
}
