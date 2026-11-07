/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d12;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import com.sun.glass.ui.Screen;
import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.tk.quantum.QuantumToolkit;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.ResourceFactory;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.d3d12.ni.D3D12AdapterInformation;
import com.sun.prism.d3d12.ni.D3D12DeviceInformation;
import com.sun.prism.d3d12.ni.D3D12NativeDevice;
import com.sun.prism.d3d12.ni.D3D12NativeInstance;
import com.sun.prism.d3d12.ni.D3D12NativeShader;


public final class D3D12Pipeline extends GraphicsPipeline {
    private static final Thread creator;
    private static boolean isEnabled;
    private static D3D12Pipeline theInstance;
    private static D3D12ResourceFactory mFactories[];

    private static native boolean nInit(Class psClass);

    private D3D12NativeInstance mInstance;
    private D3D12ResourceFactory mDefaultFactory;

    static {
        if (PrismSettings.verbose) {
            System.out.println("Loading D3D12 native library ...");
        }
        NativeLibLoader.loadLibrary("prism_d3d12");
        if (PrismSettings.verbose) {
            System.out.println("\tsucceeded.");
        }
        isEnabled = nInit(PrismSettings.class);

        creator = Thread.currentThread();

        if (isEnabled) {
            theInstance = new D3D12Pipeline();
        }
    }

    public static D3D12Pipeline getInstance() {
        return theInstance;
    }

    private D3D12Pipeline() {
    }

    private void printDeviceErrorInformation(D3D12DeviceInformation dinfo) {
        if (dinfo != null && dinfo.deviceError != 0) {
            System.out.println(String.format("D3D12 Device " + dinfo.description + " error: %08X - " + dinfo.deviceErrorReason, dinfo.deviceError));
        }
    }

    private void printAdapterInformation(int adapter) {
        D3D12AdapterInformation ainfo = mInstance.getAdapterInformation(adapter);
        if (ainfo != null) {
            System.out.println("OS Information:");
            System.out.println("\t" + ainfo.getOsVersion() + " build " + ainfo.osBuildNumber);
            System.out.println("DXGI Adapter Information:");
            System.out.println("\t" + ainfo.description);
            System.out.println("\tDevice ID: " + ainfo.getDeviceID());
            System.out.println("\tDevice Memory: " + ainfo.getDeviceMemory());
        }
    }

    private void printDeviceInformation(int adapter) {
        D3D12DeviceInformation dinfo = mInstance.getDeviceInformation(adapter);
        if (dinfo != null) {
            System.out.println("D3D12 Device Information:");
            System.out.println("\t" + dinfo.description);
            System.out.println("\tFeature Level: " + dinfo.featureLevel);
            System.out.println("\tShader Model: " + dinfo.shaderModel);
        }
    }

    private D3D12ResourceFactory createResourceFactory(int adapter, Screen screen) {
        D3D12ResourceFactory factory = new D3D12ResourceFactory(mInstance, adapter, screen);
        if (factory != null) {
            D3D12ShutdownHook.addNativeDevice(factory.getContext().getDevice());
        }
        return factory;
    }

    private D3D12ResourceFactory getD3D12ResourceFactory(int adapter, Screen screen) {
        D3D12ResourceFactory factory = mFactories[adapter];
        if (factory == null && screen != null) {
            factory = createResourceFactory(adapter, screen);
            mFactories[adapter] = factory;
        }

        return factory;
    }

    private Screen getScreenForAdapter(List<Screen> screens, int adapterOrdinal) {
        for (Screen screen : screens) {
            if (screen.getAdapterOrdinal() == adapterOrdinal) {
                return screen;
            }
        }
        return Screen.getMainScreen();
    }

    private D3D12ResourceFactory findDefaultResourceFactory(List<Screen> screens) {
        for (int adapter = 0; adapter < mFactories.length; ++adapter) {
            D3D12DeviceInformation deviceInfo = new D3D12DeviceInformation();
            if (mInstance.canCreateDevice(adapter, deviceInfo)) {
                D3D12ResourceFactory factory = getD3D12ResourceFactory(adapter, getScreenForAdapter(screens, adapter));
                if (factory != null) {
                    if (PrismSettings.verbose) {
                        printAdapterInformation(adapter);
                        printDeviceInformation(adapter);
                    }
                    return factory;
                }
            } else {
                if (!PrismSettings.disableBadDriverWarning) {
                    printDeviceErrorInformation(deviceInfo);
                }
            }
        }

        return null;
    }

    private void loadInternalShader(String name, D3D12NativeShader.PipelineMode mode, D3D12NativeShader.Visibility visibility) throws RuntimeException {
        ByteBuffer code = D3D12Utils.getShaderCodeBuffer("hlsl6/Native/" + name + ".cso");
        if (!mInstance.loadInternalSahder(name, mode, visibility, code)) {
            throw new RuntimeException("Failed to load " + name + " internal shader");
        }
    }

    private void loadInternalShaders() {
        loadInternalShader("PassThroughVS", D3D12NativeShader.PipelineMode.UI_2D, D3D12NativeShader.Visibility.VERTEX);
        loadInternalShader("MipmapGenCS", D3D12NativeShader.PipelineMode.COMPUTE, D3D12NativeShader.Visibility.ALL);
        loadInternalShader("BlitPS", D3D12NativeShader.PipelineMode.UI_2D, D3D12NativeShader.Visibility.PIXEL);

        loadInternalShader("Mtl1VS", D3D12NativeShader.PipelineMode.PHONG_3D, D3D12NativeShader.Visibility.VERTEX); // 3D Vertex Shader
        loadInternalShader("Mtl1PS", D3D12NativeShader.PipelineMode.PHONG_3D, D3D12NativeShader.Visibility.PIXEL); // 3D no lights shader
        loadInternalShader("Mtl1PS_i", D3D12NativeShader.PipelineMode.PHONG_3D, D3D12NativeShader.Visibility.PIXEL); // 3D no lights + self illuminated shader

        // rest of 3D Pixel Shaders is iterated from below variants
        String[] selfIllumVariants = { "", "i" }; // no self illumination, with self illumination
        char[] mappingVariants = { 's', 'b' }; // "simple", "bump" (basically without/with bump mapping)
        char[] lightVariants = { '1', '2', '3' }; // number of lights; no lights has only one shader
        char[] specularVariants = { 'n', 't', 'c', 'm' }; // "none", "texture", "color", "mix"

        for (int selfIllum = 0; selfIllum < selfIllumVariants.length; ++selfIllum) {
            for (int mapping = 0; mapping < mappingVariants.length; ++mapping) {
                for (int specular = 0; specular < specularVariants.length; ++specular) {
                    for (int light = 0; light < lightVariants.length; ++light) {
                        String shaderName = "Mtl1PS_" + mappingVariants[mapping] + lightVariants[light] + specularVariants[specular] + selfIllumVariants[selfIllum];
                        loadInternalShader(shaderName, D3D12NativeShader.PipelineMode.PHONG_3D, D3D12NativeShader.Visibility.PIXEL);
                    }
                }
            }
        }
    }

    @Override
    public boolean init() {
        if (!isEnabled)
            return false;

        mInstance = new D3D12NativeInstance();
        if (!mInstance.Init())
            return false;

        try {
            loadInternalShaders();
        } catch (Exception e) {
            System.err.println("Failed to load internal D3D12 shaders: " + e.getMessage());
            return false;
        }

        mFactories = new D3D12ResourceFactory[mInstance.getAdapterCount()];
        return true;
    }

    @Override
    public void dispose() {
        if (creator != Thread.currentThread()) {
            throw new IllegalStateException(
                    "This operation is not permitted on the current thread ["
                    + Thread.currentThread().getName() + "]");
        }

        if (mFactories != null) {
            for (int i = 0; i < mFactories.length; ++i) {
                if (mFactories[i] != null) {
                    mFactories[i].dispose();
                }
                mFactories[i] = null;
            }

            mFactories = null;
        }

        if (mInstance != null) {
            mInstance.dispose();
            mInstance = null;
            theInstance = null;
        }

        super.dispose();
    }

    @Override
    public int getAdapterOrdinal(Screen screen) {
        return mInstance.getAdapterOrdinal(screen.getNativeScreen());
    }

    @Override
    public ResourceFactory getResourceFactory(Screen screen) {
        return getD3D12ResourceFactory(screen.getAdapterOrdinal(), screen);
    }

    @Override
    public ResourceFactory getDefaultResourceFactory(List<Screen> screens) {
        if (mDefaultFactory == null) {
            mDefaultFactory = findDefaultResourceFactory(screens);
        }
        return mDefaultFactory;
    }

    @Override
    public boolean is3DSupported() {
        return true;
    }

    @Override
    public boolean isMSAASupported() {
        // D3D12 in general supports MSAA, but in reality the level highly depends on
        // used format. Here we can safely return yes, and we will fetch maximum MSAA
        // level when creating the Texture/RTT.
        return true;
    }

    @Override
    public boolean isVsyncSupported() {
        return true;
    }

    @Override
    public boolean supportsShaderType(ShaderType type) {
        switch (type) {
            case HLSL:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean supportsShaderModel(ShaderModel model) {
        switch (model) {
            case SM6:
                return true;
            default:
                return false;
        }
    }

}

class D3D12ShutdownHook implements Runnable {

    private static List<WeakReference<D3D12NativeDevice>> deviceRefs = new ArrayList<>();
    private static boolean isShutdown = false;

    static {
        Toolkit.getToolkit().addShutdownHook(new D3D12ShutdownHook());
    }

    public static void addNativeDevice(D3D12NativeDevice device) {
        synchronized (deviceRefs) {
            if (isShutdown) {
                if (device != null) {
                    QuantumToolkit.runInRenderThreadAndWait(() -> {
                        device.waitUntilIdle();
                    });
                }
            } else {
                for (ListIterator<WeakReference<D3D12NativeDevice>> it = deviceRefs.listIterator(); it.hasNext();) {
                    D3D12NativeDevice dev = it.next().get();
                    if (dev == null) {
                        it.remove();
                    }
                }

                deviceRefs.add(new WeakReference<>(device));
            }
        }
    }

    @Override
    public void run() {
        synchronized (deviceRefs) {
            for (ListIterator<WeakReference<D3D12NativeDevice>> it = deviceRefs.listIterator(); it.hasNext();) {
                D3D12NativeDevice device = it.next().get();
                if (device != null) {
                    QuantumToolkit.runInRenderThreadAndWait(() -> {
                        device.waitUntilIdle();
                    });
                } else {
                    it.remove();
                }
            }

            isShutdown = true;
        }
    }
}
