/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;

import com.sun.prism.impl.PrismSettings;
import com.sun.javafx.PlatformUtil;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;

abstract class GLFactory {

    private static native boolean
            nIsGLExtensionSupported(long nativeContextObject, String glExtStr);
    private static native String nGetGLVendor(long nativeCtxInfo);
    private static native String nGetGLRenderer(long nativeCtxInfo);
    private static native String nGetGLVersion(long nativeCtxInfo);

    private static final GLFactory platformFactory;

    /* Note: We are only storing the string information of a driver in this
     * object. We are assuming a system with a single or homogeneous GPUs.
     * For the case of heterogeneous GPUs system the string information
     * will need to move to GLContext class. */
    long nativeCtxInfo;
    boolean gl2 = false;
    private GLContext shareCtx = null;

    /**
     * Creates a new GLFactory instance. End users do not need
     * to call this method.
     */
    GLFactory() {
    }

    /**
     * Instantiate singleton factories if available, the OS native ones.
     */
    static {

        final String factoryClassName;
        if (PlatformUtil.isUnix()) {
            if ("monocle".equals(PlatformUtil.getEmbeddedType()))
                factoryClassName = "com.sun.prism.es2.MonocleGLFactory";
            else
                factoryClassName = "com.sun.prism.es2.X11GLFactory";
        } else if (PlatformUtil.isWindows()) {
            factoryClassName = "com.sun.prism.es2.WinGLFactory";
        } else if (PlatformUtil.isMac()) {
            factoryClassName = "com.sun.prism.es2.MacGLFactory";
        } else if (PlatformUtil.isIOS()) {
            factoryClassName = "com.sun.prism.es2.IOSGLFactory";
        } else if (PlatformUtil.isAndroid()) {
            factoryClassName = "com.sun.prism.es2.MonocleGLFactory";
        } else {
            factoryClassName = null;
            System.err.println("GLFactory.static - No Platform Factory for: " + System.getProperty("os.name"));
        }
        if (PrismSettings.verbose) {
            System.out.println("GLFactory using " + factoryClassName);
        }
        @SuppressWarnings("removal")
        GLFactory tmp = factoryClassName == null ? null :
            AccessController.doPrivileged(new FactoryLoader(factoryClassName));
        platformFactory = tmp;
    }

    private static class FactoryLoader implements PrivilegedAction<GLFactory> {
        private final String factoryClassName;
        FactoryLoader(String factoryClassName) {
            this.factoryClassName = factoryClassName;
        }

        @Override
        public GLFactory run() {
            GLFactory factory = null;
            try {
                factory = (GLFactory) Class.forName(factoryClassName).getDeclaredConstructor().newInstance();
            } catch (Throwable t) {
                System.err.println("GLFactory.static - Platform: "
                        + System.getProperty("os.name")
                        + " - not available: "
                        + factoryClassName);
                t.printStackTrace();
            }
            return factory;
        }
    }

    /**
     * Returns the sole GLFactory instance.
     */
    static GLFactory getFactory() throws RuntimeException {
        if (null != platformFactory) {
            return platformFactory;
        }
        throw new RuntimeException("No native platform GLFactory available.");
    }

    // Consists of a list of prequalifying GPUs that we may use for the es2 pipe.
    // A null preQualificationFilter implies we may consider any GPU
    abstract GLGPUInfo[] getPreQualificationFilter();

    // Consists of a list of GPUs that we will not use for the es2 pipe.
    abstract GLGPUInfo[] getRejectList();

    private static GLGPUInfo readGPUInfo(long nativeCtxInfo) {
        String glVendor = nGetGLVendor(nativeCtxInfo);
        String glRenderer = nGetGLRenderer(nativeCtxInfo);
        return new GLGPUInfo(glVendor.toLowerCase(),
                glRenderer.toLowerCase());
    }

    private static boolean matches(GLGPUInfo gpuInfo, GLGPUInfo[] gpuInfoArr) {
        if (gpuInfoArr != null) {
            for (int i = 0; i < gpuInfoArr.length; i++) {
                if (gpuInfo.matches(gpuInfoArr[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean inPreQualificationFilter(GLGPUInfo gpuInfo) {
        GLGPUInfo[] preQualificationFilter = getPreQualificationFilter();
        if (preQualificationFilter == null) {
            // We will consider any GPU if preQualificationFilter is null
            return true;
        }
        return matches(gpuInfo, preQualificationFilter);
    }

    private boolean inRejectList(GLGPUInfo gpuInfo) {
        return matches(gpuInfo, getRejectList());
    }

    boolean isQualified(long nativeCtxInfo) {
        // Read the GPU (graphics hardware) information and qualifying it by
        // checking against the preQualificationFilter and the rejectList.
        GLGPUInfo gpuInfo = readGPUInfo(nativeCtxInfo);

        if (gpuInfo.vendor == null || gpuInfo.model == null
                || gpuInfo.vendor.contains("unknown")
                || gpuInfo.model.contains("unknown")) {
            // Return false if we can't determine the vendor and model of the
            // gpu installed on the system
            return false;
        }

        return inPreQualificationFilter(gpuInfo) && !inRejectList(gpuInfo);
    }

    abstract GLContext createGLContext(long nativeCtxInfo);

    abstract GLContext createGLContext(GLDrawable drawable,
            GLPixelFormat pixelFormat, GLContext shareCtx, boolean vSyncRequest);

    abstract GLDrawable createGLDrawable(long nativeWindow, GLPixelFormat pixelFormat);

    abstract GLDrawable createDummyGLDrawable(GLPixelFormat pixelFormat);

    abstract GLPixelFormat createGLPixelFormat(long nativeScreen, GLPixelFormat.Attributes attrs);

    boolean isGLGPUQualify() {
        return isQualified(nativeCtxInfo);
    }

    abstract boolean initialize(Class psClass, GLPixelFormat.Attributes attrs);

    GLContext getShareContext() {
        if (shareCtx == null) {
            shareCtx = createGLContext(nativeCtxInfo);
        }
        return shareCtx;
    }

    // Returns true if this pipe supports GL2 profile, false for GLES2
    boolean isGL2() {
        return gl2;
    }

    boolean isGLExtensionSupported(String sglExtStr) {
        return nIsGLExtensionSupported(nativeCtxInfo, sglExtStr);
    }

    boolean isNPOTSupported() {
        return (isGLExtensionSupported("GL_ARB_texture_non_power_of_two")
                    || isGLExtensionSupported("GL_OES_texture_npot"));
    }

    abstract int getAdapterCount();

    abstract int getAdapterOrdinal(long nativeScreen);

    abstract void updateDeviceDetails(HashMap deviceDetails);

    void printDriverInformation(int adapter) {
        /* We are assuming a system with a single or homogeneous GPUs. */
        System.out.println("Graphics Vendor: " + nGetGLVendor(nativeCtxInfo));
        System.out.println("       Renderer: " + nGetGLRenderer(nativeCtxInfo));
        System.out.println("        Version: " + nGetGLVersion(nativeCtxInfo));
    }
}
