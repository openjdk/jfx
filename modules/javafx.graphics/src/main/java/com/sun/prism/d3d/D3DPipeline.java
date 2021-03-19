/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d;

import com.sun.glass.ui.Screen;
import com.sun.glass.utils.NativeLibLoader;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.ResourceFactory;
import com.sun.prism.impl.PrismSettings;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

public final class D3DPipeline extends GraphicsPipeline {

    private static final boolean d3dEnabled;

    private static final Thread creator;
    private static D3DPipeline theInstance;
    private static D3DResourceFactory factories[];
    private static boolean d3dInitialized;

    static {

        d3dEnabled = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            if (PrismSettings.verbose) {
                System.out.println("Loading D3D native library ...");
            }
            NativeLibLoader.loadLibrary("prism_d3d");
            if (PrismSettings.verbose) {
                System.out.println("\tsucceeded.");
            }
            return Boolean.valueOf(nInit(PrismSettings.class, true));
        });

        if (PrismSettings.verbose) {
            System.out.println("Direct3D initialization " + (d3dEnabled ? "succeeded" : "failed"));
        }

        boolean printD3DError = PrismSettings.verbose || !PrismSettings.disableBadDriverWarning;
        if (!d3dEnabled && printD3DError) {
            if (PrismSettings.verbose) {
                System.out.println(nGetErrorMessage());
            }
            printDriverWarnings();
        }

        creator = Thread.currentThread();

        if (d3dEnabled) {
            d3dInitialized = true;
            theInstance = new D3DPipeline();
            factories = new D3DResourceFactory[nGetAdapterCount()];
        }
    }

    public static D3DPipeline getInstance() {
        return theInstance;
    }

    private static boolean isDriverWarning(String warningMessage) {
        return warningMessage.contains("driver version");
    }

    private static void printDriverWarning(D3DDriverInformation di) {
        if ((di != null) && (di.warningMessage != null)
                && (PrismSettings.verbose || isDriverWarning(di.warningMessage))) {
            System.out.println("Device \"" + di.deviceDescription
                    + "\" (" + di.deviceName + ") initialization failed : ");
            System.out.println(di.warningMessage);
        }
    }

    private static void printDriverWarning(int adapter) {
        printDriverWarning(nGetDriverInformation(adapter, new D3DDriverInformation()));
    }

    private static void printDriverInformation(int adapter) {
        D3DDriverInformation di = nGetDriverInformation(adapter, new D3DDriverInformation());
        if (di != null) {
            System.out.println("OS Information:");
            System.out.println("\t" + di.getOsVersion() + " build " + di.osBuildNumber);
            System.out.println("D3D Driver Information:");
            System.out.println("\t" + di.deviceDescription);
            System.out.println("\t" + di.deviceName);
            System.out.println("\tDriver " + di.driverName + ", version " + di.getDriverVersion());
            System.out.println("\tPixel Shader version " + di.psVersionMajor + "." + di.psVersionMinor);
            System.out.println("\tDevice : " + di.getDeviceID());
            System.out.println("\tMax Multisamples supported: " + di.maxSamples);
            if (di.warningMessage != null) {
                System.out.println("\t *** " + di.warningMessage);
            }
        }
    }

    // Note that ordinarily a library should not print out
    // informational messages by default, but in the case of
    // bad driver it might be warranted.

    private static void printDriverWarnings() {
        // enumerate all adapters and print driver warnings
        for (int adapter = 0;; ++adapter) {
            D3DDriverInformation di = nGetDriverInformation(adapter, new D3DDriverInformation());
            if (di != null) {
                printDriverWarning(di);
            } else {
                break;
            }
        }
    }

    private D3DPipeline() {
    }

    @Override
    public boolean init() {
        return d3dEnabled;
    }

    private static native boolean nInit(Class psClass, boolean load);
    private static native String nGetErrorMessage();
    private static native void nDispose(boolean unload);

    private static native int nGetAdapterOrdinal(long hMonitor);
    private static native int nGetAdapterCount();

    /*
     * This method fill object with data and return an argument
     */
    private static native D3DDriverInformation nGetDriverInformation(
            int adapterOrdinal, D3DDriverInformation object);

    private static native int nGetMaxSampleSupport(int adapterOrdinal);

    // Called by dispose and reinitialize methods to reset the pipeline
    // and free all resources
    private void reset(boolean unload) {
        if (!d3dInitialized) {
            return;
        }

        if (creator != Thread.currentThread()) {
            throw new IllegalStateException(
                    "This operation is not permitted on the current thread ["
                    + Thread.currentThread().getName() + "]");
        }
        for (int i = 0; i != factories.length; ++i) {
            if (factories[i] != null) {
                factories[i].dispose();
            }
            factories[i] = null;
        }
        factories = null;
        _default = null;
        d3dInitialized = false;
        nDispose(unload);
    }

    // Reinitialize pipeline
    void reinitialize() {
        if (PrismSettings.verbose) {
            System.err.println("D3DPipeline: reinitialize after device was removed");
        }

        // Device was removed, reset and reinitialize
        reset(false);

        boolean success = nInit(PrismSettings.class, false);
        if (!success) {
            nDispose(false);
            return;
        }

        d3dInitialized = true;
        factories = new D3DResourceFactory[nGetAdapterCount()];
    }

    @Override
    public void dispose() {
        reset(true);
        theInstance = null;
        super.dispose();
    }

    private static D3DResourceFactory createResourceFactory(int adapterOrdinal, Screen screen) {
        long pContext = D3DResourceFactory.nGetContext(adapterOrdinal);
        return pContext != 0 ? new D3DResourceFactory(pContext, screen) : null;
    }

    private static D3DResourceFactory getD3DResourceFactory(int adapterOrdinal, Screen screen) {
        D3DResourceFactory factory = factories[adapterOrdinal];
        if (factory == null && screen != null) {
            factory = createResourceFactory(adapterOrdinal, screen);
            factories[adapterOrdinal] = factory;
        }
        return factory;
    }

    /*
     * we need screen only because BaseShaderContext requres Screen in the constructor
     */
    private static Screen getScreenForAdapter(List<Screen> screens, int adapterOrdinal) {
        for (Screen screen : screens) {
            if (screen.getAdapterOrdinal() == adapterOrdinal) {
                return screen;
            }
        }
        return Screen.getMainScreen();
    }

    @Override
    public int getAdapterOrdinal(Screen screen) {
        return nGetAdapterOrdinal(screen.getNativeScreen());
    }

    private static D3DResourceFactory findDefaultResourceFactory(List<Screen> screens) {
        if (!d3dInitialized) {
            // If initialization failed, try again
            D3DPipeline.getInstance().reinitialize();

            // If reinitialization failed, return a null resource factory; we will
            // try again the next time this method is called.
            if (!d3dInitialized) {
                return null;
            }
        }

        for (int adapter = 0, n = nGetAdapterCount(); adapter != n; ++adapter) {
            D3DResourceFactory rf =
                    getD3DResourceFactory(adapter, getScreenForAdapter(screens, adapter));

            if (rf != null) {
                if (PrismSettings.verbose) {
                    printDriverInformation(adapter);
                }
                return rf;
            } else {
                if (!PrismSettings.disableBadDriverWarning) {
                    printDriverWarning(adapter);
                }
            }
        }
        return null;
    }

    D3DResourceFactory _default;

    @Override
    public ResourceFactory getDefaultResourceFactory(List<Screen> screens) {
        if (_default == null) {
            _default = findDefaultResourceFactory(screens);
        }
        return _default;
    }

    @Override
    public ResourceFactory getResourceFactory(Screen screen) {
        return getD3DResourceFactory(screen.getAdapterOrdinal(), screen);
    }

    @Override
    public boolean is3DSupported() {
        return true;
    }

    private int maxSamples = -1;

    int getMaxSamples() {
        if (maxSamples < 0) {
            isMSAASupported();
        }
        return maxSamples;
    }

    @Override
    public boolean isMSAASupported() {
        if (maxSamples < 0) {
            //TODO: 3D - consider different adapters
            maxSamples = nGetMaxSampleSupport(0);
        }
        return maxSamples > 0;
    }

    @Override
    public boolean isVsyncSupported() {
        return true;
    }

    @Override
    public boolean supportsShaderType(ShaderType type) {
        switch (type) {
            case HLSL: return true;
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
