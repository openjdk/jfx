/*
 * Copyright (c) 2009, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism;

import com.sun.glass.ui.Screen;
import com.sun.javafx.font.FontFactory;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.prism.impl.PrismSettings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class GraphicsPipeline {

    public static enum ShaderType {
        /**
         * The pipeline supports shaders built with the D3D HLSL shader language.
         */
        HLSL,
        /**
         * The pipeline supports shaders built with the OpenGL GLSL shader language
         */
        GLSL
    }

    public static enum ShaderModel {
        /**
         * The pipeline supports Shader Model 3 features, including Pixel Shader
         * 3.0 and Vertex Shader 3.0 programs.
         */
        SM3
    }
    private FontFactory fontFactory;
    private final Set<Runnable> disposeHooks = new HashSet<>();

    public abstract boolean init();
    public void dispose() {
        notifyDisposeHooks();
        installedPipeline = null;
    }

    /**
     * Add a dispose hook to be called when the pipeline is disposed.
     *
     * @param runnable the {@link Runnable} to be called when the pipeline is disposed
     */
    public void addDisposeHook(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        synchronized (disposeHooks) {
            disposeHooks.add(runnable);
        }
    }

    private void notifyDisposeHooks() {
        List<Runnable> hooks;
        synchronized (disposeHooks) {
            hooks = new ArrayList<>(disposeHooks);
            disposeHooks.clear();
        }

        for (Runnable hook : hooks) {
            hook.run();
        }
    }

    public abstract int getAdapterOrdinal(Screen screen);

    /*
     * The following method allows to access several graphics adapters individually.
     * Graphics resources are not sharable between different adapters
     */
    public abstract ResourceFactory getResourceFactory(Screen screen);

    /*
     * getDefaultResourceFactory returns system-default graphics device
     */
    public abstract ResourceFactory getDefaultResourceFactory(List<Screen> screens);

    public abstract boolean is3DSupported();

    public boolean isMSAASupported() { return false; }

    public abstract boolean isVsyncSupported();

    /**
     * Returns true iff the graphics objects from this pipeline support
     * the indicated {@link ShaderType}.
     *
     * @param type the desired {@link ShaderType} to be used
     * @return true if the indicated {@code ShaderType} is supported
     */
    public abstract boolean supportsShaderType(ShaderType type);

    /**
     * Returns true iff the graphics objects from this pipeline support
     * the indicated {@link ShaderModel}.  Generally, the pipeline will
     * also support all older or lower-numbered {@code ShaderModel}s as well.
     *
     * @param model the desired {@link ShaderModel} to be used
     * @return true if the indicated {@code ShaderModel} is supported
     */
    public abstract boolean supportsShaderModel(ShaderModel model);

    /**
     * Returns true iff the graphics objects from this pipeline support
     * the indicated {@link ShaderType} and {@link ShaderModel}.  Generally,
     * the pipeline will also support all older or lower-numbered
     * {@code ShaderModel}s as well.
     *
     * @param type the desired {@link ShaderType} to be used
     * @param model the desired {@link ShaderModel} to be used
     * @return true if the indicated {@code ShaderType} and {@code ShaderModel}
     *              are supported
     */
    public boolean supportsShader(ShaderType type, ShaderModel model) {
        return (supportsShaderType(type) && supportsShaderModel(model));
    }

    public static ResourceFactory getDefaultResourceFactory() {
        List<Screen> screens = Screen.getScreens();
        return getPipeline().getDefaultResourceFactory(screens);
    }

    public FontFactory getFontFactory() {
        if (fontFactory == null) {
            fontFactory = PrismFontFactory.getFontFactory();
        }
        return fontFactory;
    }

    protected Map deviceDetails = null;

    /*
     * returns optional device dependant details, may be null.
     */
    public Map getDeviceDetails() {
        return deviceDetails;
    }

    /*
     * sets optional device dependant details, may be null.
     * This should be done very early (like at init time) and then not changed.
     */
    protected void setDeviceDetails(Map details) {
        deviceDetails = details;
    }

    private static GraphicsPipeline installedPipeline;

    public static GraphicsPipeline createPipeline() {
        if (PrismSettings.tryOrder.isEmpty()) {
            // if no pipelines specified just return null
            if (PrismSettings.verbose) {
                System.out.println("No Prism pipelines specified");
            }
            return null;
        }

        if (installedPipeline != null) {
            throw new IllegalStateException("pipeline already created:"+
                                            installedPipeline);
        }
        for (String prefix : PrismSettings.tryOrder) {
            // Warn if j2d pipeline is specified
            if ("j2d".equals(prefix)) {
                System.err.println(
                    "WARNING: The prism-j2d pipeline should not be used as the software");
                System.err.println(
                    "fallback pipeline. It is no longer tested nor intended to be used for");
                System.err.println(
                    "on-screen rendering. Please use the prism-sw pipeline instead by setting");
                System.err.println(
                    "the \"prism.order\" system property to \"sw\" rather than \"j2d\".");
            }

            if (PrismSettings.verbose) {
                if ("j2d".equals(prefix) || "sw".equals(prefix)) {
                    System.err.println("*** Fallback to Prism SW pipeline");
                }
            }

            String className =
                "com.sun.prism."+prefix+"."+prefix.toUpperCase()+"Pipeline";
            try {
                if (PrismSettings.verbose) {
                    System.out.println("Prism pipeline name = " + className);
                }
                Class klass = Class.forName(className);
                if (PrismSettings.verbose) {
                    System.out.println("(X) Got class = " + klass);
                }
                Method m = klass.getMethod("getInstance", (Class[])null);
                GraphicsPipeline newPipeline = (GraphicsPipeline)
                    m.invoke(null, (Object[])null);
                if (newPipeline != null && newPipeline.init()) {
                    if (PrismSettings.verbose) {
                        System.out.println("Initialized prism pipeline: " +
                                           klass.getName());
                    }
                    installedPipeline = newPipeline;
                    return installedPipeline;
                }
                if (newPipeline != null) {
                    newPipeline.dispose();
                    newPipeline = null;
                }
                if (PrismSettings.verbose) {
                    System.err.println("GraphicsPipeline.createPipeline: error" +
                                       " initializing pipeline " + className);
                    if (newPipeline == null) {
                        System.err.println("Reason: could not create an instance");
                    } else {
                        System.err.println("Reason: could not initialize the instance");
                    }
                }
            } catch (Throwable t) {
                if (PrismSettings.verbose) {
                    System.err.println("GraphicsPipeline.createPipeline " +
                                       "failed for " + className);
                    t.printStackTrace();
                }
            }
        }
        StringBuffer sBuf = new StringBuffer("Graphics Device initialization failed for :  ");
        final Iterator<String> orderIterator =
                PrismSettings.tryOrder.iterator();
        if (orderIterator.hasNext()) {
            sBuf.append(orderIterator.next());
            while (orderIterator.hasNext()) {
                sBuf.append(", ");
                sBuf.append(orderIterator.next());
            }
        }
        System.err.println(sBuf);
        return null;
    }

    public static GraphicsPipeline getPipeline() {
        return installedPipeline;
    }

    public boolean isEffectSupported() {
        return true;
    }

    /**
     * Checks if the GraphicsPipeline uses uploading or presenting painter
     * @return true if the pipeline uses an uploading painter
     */
    public boolean isUploading() {
        return PrismSettings.forceUploadingPainter;
    }
}
