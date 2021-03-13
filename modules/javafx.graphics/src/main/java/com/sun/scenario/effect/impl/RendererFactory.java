/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.javafx.PlatformUtil;
import com.sun.scenario.effect.FilterContext;

/**
 * A factory that produces a {@code Renderer} instance appropriate for
 * the desktop and tv stacks (either Swing or Prism based).  This class
 * dynamically locates a {@code Renderer} using the java.lang.reflect package,
 * which is not available on CLDC.  The CLDC-based mobile stack may
 * substitute their own version of this class that does not rely on reflection.
 */
class RendererFactory {

    private static String rootPkg = Renderer.rootPkg;
    private static boolean tryRSL = true;
    private static boolean trySIMD = false;
    // by default we only enable jogl hw acceleration on MacOS
    private static boolean tryJOGL = PlatformUtil.isMac();
    private static boolean tryPrism = true;

    static {
        try {
            if ("false".equals(System.getProperty("decora.rsl"))) {
                tryRSL = false;
            }
            if ("false".equals(System.getProperty("decora.simd"))) {
                trySIMD = false;
            }
            String tryJOGLProp = System.getProperty("decora.jogl");
            if (tryJOGLProp != null) {
                tryJOGL = Boolean.parseBoolean(tryJOGLProp);
            }
            if ("false".equals(System.getProperty("decora.prism"))) {
                tryPrism = false;
            }
        } catch (SecurityException ignore) {
        }
    }

    private static boolean isRSLFriendly(Class klass) {
        // can't use reflection here to check for sun.* class when running
        // in sandbox; however, we are allowed to walk up the tree and
        // check names of interfaces loaded by the system
        if (klass.getName().equals("sun.java2d.pipe.hw.AccelGraphicsConfig")) {
            return true;
        }
        boolean rsl = false;
        for (Class iface : klass.getInterfaces()) {
            if (isRSLFriendly(iface)) {
                rsl = true;
                break;
            }
        }
        return rsl;
    }

    private static boolean isRSLAvailable(FilterContext fctx) {
        return isRSLFriendly(fctx.getReferent().getClass());
    }

    private static Renderer createRSLRenderer(FilterContext fctx) {
        try {
            Class klass = Class.forName(rootPkg + ".impl.j2d.rsl.RSLRenderer");
            Method m = klass.getMethod("createRenderer",
                                       new Class[] { FilterContext.class });
            return (Renderer)m.invoke(null, new Object[] { fctx });
        } catch (Throwable e) {}

        return null;
    }

    private static Renderer createJOGLRenderer(FilterContext fctx) {
        if (tryJOGL) {
            try {
                Class klass = Class.forName(rootPkg + ".impl.j2d.jogl.JOGLRenderer");
                Method m = klass.getMethod("createRenderer",
                                           new Class[] { FilterContext.class });
                return (Renderer)m.invoke(null, new Object[] { fctx });
            } catch (Throwable e) {}
            // don't disable jogl if failed, it may be available for other config
        }
        return null;
    }

    private static Renderer createPrismRenderer(FilterContext fctx) {
        if (tryPrism) {
            try {
                Class klass = Class.forName(rootPkg + ".impl.prism.PrRenderer");
                Method m = klass.getMethod("createRenderer",
                                           new Class[] { FilterContext.class });
                return (Renderer)m.invoke(null, new Object[] { fctx });
            } catch (Throwable e) {
                e.printStackTrace();
            }
            // don't disable prism if failed, it may be available for other config
        }
        return null;
    }

    private static Renderer getSSERenderer() {
        if (trySIMD) {
            try {
                Class klass = Class.forName(rootPkg + ".impl.j2d.J2DSWRenderer");
                Method m = klass.getMethod("getSSEInstance", (Class[])null);
                Renderer sseRenderer = (Renderer)m.invoke(null, (Object[])null);
                if (sseRenderer != null) {
                    return sseRenderer;
                }
            } catch (Throwable e) {e.printStackTrace();}
            // don't bother trying to find SSE renderer again
            trySIMD = false;
        }
        return null;
    }

    private static Renderer getJavaRenderer() {
        try {
            Class klass = Class.forName(rootPkg + ".impl.prism.sw.PSWRenderer");
            Class screenClass = Class.forName("com.sun.glass.ui.Screen");
            Method m = klass.getMethod("createJSWInstance",
                                       new Class[] { screenClass });
            Renderer jswRenderer =
                (Renderer)m.invoke(null, new Object[] { null } );
            if (jswRenderer != null) {
                return jswRenderer;
            }
        } catch (Throwable e) {e.printStackTrace();}
        return null;
    }

    private static Renderer getJavaRenderer(FilterContext fctx) {
        try {
            Class klass = Class.forName(rootPkg + ".impl.prism.sw.PSWRenderer");
            Method m = klass.getMethod("createJSWInstance",
                                       new Class[] { FilterContext.class });
            Renderer jswRenderer =
               (Renderer)m.invoke(null, new Object[] { fctx } );
            if (jswRenderer != null) {
                return jswRenderer;
            }
        } catch (Throwable e) {}
        return null;
    }

    static Renderer getSoftwareRenderer() {
        Renderer r = getSSERenderer();
        if (r == null) {
            r = getJavaRenderer();
        }
        return r;
    }

    static Renderer createRenderer(final FilterContext fctx) {
        return AccessController.doPrivileged((PrivilegedAction<Renderer>) () -> {
            Renderer r = null;
            // Class.getSimpleName is not available on CDC
            String klassName = fctx.getClass().getName();
            String simpleName = klassName.substring(klassName.lastIndexOf(".") + 1);

            if (simpleName.equals("PrFilterContext") && tryPrism) {
                r = createPrismRenderer(fctx);
            }
            // check to see whether one of the hardware accelerated
            // Java 2D pipelines is in use and exposes the necessary
            // "resource sharing layer" APIs (only in Sun's JDK 6u10 and above)
            if (r == null && tryRSL && isRSLAvailable(fctx)) {
                // try locating an RSLRenderer (need to use reflection in case
                // certain RSL backend classes are not available;
                // this step will trigger lazy downloading of impl jars
                // via JNLP, if not already available)
                r = createRSLRenderer(fctx);
            }
            if (r == null && tryJOGL) {
                // next try the JOGL renderer
                r = createJOGLRenderer(fctx);
            }
            if (r == null && trySIMD) {
                // next try the SSE renderer
                r = getSSERenderer();
            }
            if (r == null) {
                // otherwise, fall back on the Java/CPU renderer
                r = getJavaRenderer(fctx);
            }
            return r;
        });
    }
}
