/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.util.Utils;

/**
 * Contains the runtime arguments used by Prism.
 */
public final class PrismSettings {

    public static final boolean verbose;
    public static final boolean debug;
    public static final boolean trace;
    public static final boolean printAllocs;
    public static final boolean isVsyncEnabled;
    public static final boolean dirtyOptsEnabled;
    public static final boolean occlusionCullingEnabled;
    public static final boolean scrollCacheOpt;
    public static final boolean threadCheck;
    public static final boolean cacheSimpleShapes;
    public static final boolean cacheComplexShapes;
    public static final boolean useNewImageLoader;
    public static final List<String> tryOrder;
    public static final int prismStatFrequency;
    public static final RasterizerType rasterizerSpec;
    public static final String refType;
    public static final boolean forceRepaint;
    public static final boolean noFallback;
    public static final boolean showDirtyRegions;
    public static final boolean showOverdraw;
    public static final boolean printRenderGraph;
    public static final int minRTTSize;
    public static final int dirtyRegionCount;
    public static final boolean disableBadDriverWarning;
    public static final boolean forceGPU;
    public static final int maxTextureSize;
    public static final int primTextureSize;
    public static final boolean disableRegionCaching;
    public static final boolean forcePow2;
    public static final boolean noClampToZero;
    public static final boolean disableD3D9Ex;
    public static final boolean allowHiDPIScaling;
    public static final long maxVram;
    public static final long targetVram;
    public static final boolean poolStats;
    public static final boolean poolDebug;
    public static final boolean disableEffects;
    public static final int glyphCacheWidth;
    public static final int glyphCacheHeight;
    public static final String perfLog;
    public static final boolean perfLogExitFlush;
    public static final boolean perfLogFirstPaintFlush;
    public static final boolean perfLogFirstPaintExit;
    public static final boolean superShader;
    public static final boolean forceUploadingPainter;
    public static final boolean forceAlphaTestShader;
    public static final boolean forceNonAntialiasedShape;

    public static enum RasterizerType {
        DoubleMarlin("Double Precision Marlin Rasterizer");

        private String publicName;
        private RasterizerType(String publicname) {
            this.publicName = publicname;
        }
        @Override
        public String toString() {
            return publicName;
        }
    }

    private PrismSettings() {
    }

    private static void printBooleanOption(boolean opt, String trueStr) {
        if (opt) {
            System.out.println(trueStr);
        } else {
            System.out.print("Not ");
            System.out.print(Character.toLowerCase(trueStr.charAt(0)));
            System.out.println(trueStr.substring(1));
        }
    }

    static {
        @SuppressWarnings("removal")
        final Properties systemProperties =
                (Properties) AccessController.doPrivileged(
                        (PrivilegedAction) () -> System.getProperties());

        /* Vsync */
        isVsyncEnabled  = getBoolean(systemProperties, "prism.vsync", true)
                              && !getBoolean(systemProperties,
                                             "javafx.animation.fullspeed",
                                             false);

        /* Dirty region optimizations */
        dirtyOptsEnabled = getBoolean(systemProperties, "prism.dirtyopts",
                                      true);
        occlusionCullingEnabled =
                dirtyOptsEnabled && getBoolean(systemProperties,
                                               "prism.occlusion.culling",
                                               true);

        // The maximum number of dirty regions to use. The absolute max that we can
        // support at present is 15.
        dirtyRegionCount = Utils.clamp(0, getInt(systemProperties, "prism.dirtyregioncount", 6, null), 15);

        // Scrolling cache optimization
        // Disabled as a workaround for RT-39755.
        scrollCacheOpt = getBoolean(systemProperties, "prism.scrollcacheopt", false);

        /* Dirty region optimizations */
        threadCheck = getBoolean(systemProperties, "prism.threadcheck", false);

        /* Draws overlay rectangles showing where the dirty regions were */
        showDirtyRegions = getBoolean(systemProperties, "prism.showdirty", false);

        /*
         * Draws overlay rectangles showing not only the dirty regions, but how many times
         * each area within that dirty region was drawn (covered by bounds of a drawn object).
         */
        showOverdraw = getBoolean(systemProperties, "prism.showoverdraw", false);

        /* Prints out the render graph, annotated with dirty opts information */
        printRenderGraph = getBoolean(systemProperties, "prism.printrendergraph", false);

        /* Force scene repaint on every frame */
        forceRepaint = getBoolean(systemProperties, "prism.forcerepaint", false);

        /* disable fallback to another toolkit if prism couldn't be init-ed */
        noFallback = getBoolean(systemProperties, "prism.noFallback", false);

        /* Shape caching optimizations */
        String cache = systemProperties.getProperty("prism.cacheshapes",
                                                    "complex");
        if ("all".equals(cache) || "true".equals(cache)) {
            cacheSimpleShapes = true;
            cacheComplexShapes = true;
        } else if ("complex".equals(cache)) {
            cacheSimpleShapes = false;
            cacheComplexShapes = true;
        } else {
            cacheSimpleShapes = false;
            cacheComplexShapes = false;
        }

        /* New javafx-iio image loader */
        useNewImageLoader = getBoolean(systemProperties, "prism.newiio", true);

        /* Verbose output*/
        verbose = getBoolean(systemProperties, "prism.verbose", false);

        /* Prism statistics print frequency, <=0 means "do not print" */
        prismStatFrequency =
                getInt(systemProperties, "prism.printStats",
                       0, 1, "Try -Dprism.printStats=<true or number>");

        /* Debug output*/
        debug = getBoolean(systemProperties, "prism.debug", false);

        /* Trace output*/
        trace = getBoolean(systemProperties, "prism.trace", false);

        /* Print texture allocation data */
        printAllocs = getBoolean(systemProperties, "prism.printallocs", false);

        /* Disable bad driver check warning */
        disableBadDriverWarning = getBoolean(systemProperties,
                                             "prism.disableBadDriverWarning",
                                             false);

        /* Force GPU, if GPU is PS 3 capable, disable GPU qualification check. */
        forceGPU = getBoolean(systemProperties, "prism.forceGPU", false);

        String order = systemProperties.getProperty("prism.order");
        String[] tryOrderArr;
        if (order != null) {
            tryOrderArr = split(order, ",");
        } else {
            if (PlatformUtil.isWindows()) {
                tryOrderArr = new String[] { "d3d", "sw" };
            } else if (PlatformUtil.isMac()) {
                tryOrderArr = new String[] { "es2", "sw" };
            } else if (PlatformUtil.isIOS()) {
                tryOrderArr = new String[] { "es2" };
            } else if (PlatformUtil.isAndroid()) {
                    tryOrderArr = new String[] { "es2" };
            } else if (PlatformUtil.isLinux()) {
                tryOrderArr = new String[] { "es2", "sw" };
            } else {
                tryOrderArr = new String[] { "sw" };
            }
        }

        tryOrder = List.of(tryOrderArr);

        RasterizerType rSpec = null;
        String rOrder = systemProperties.getProperty("prism.rasterizerorder");
        if (rOrder != null) {
            for (String s : split(rOrder.toLowerCase(), ",")) {
                switch (s) {
                    case "marlin":
                    case "doublemarlin":
                        rSpec = RasterizerType.DoubleMarlin;
                        break;
                    default:
                        continue;
                }
                break;
            }
        }
        if (rSpec == null) {
            rSpec = RasterizerType.DoubleMarlin;
        }
        rasterizerSpec = rSpec;

        String primtex = systemProperties.getProperty("prism.primtextures");
        if (primtex == null) {
            primTextureSize = PlatformUtil.isEmbedded() ? -1 : 0;
        } else if (primtex.equals("true")) {
            primTextureSize = -1;
        } else if (primtex.equals("false")) {
            primTextureSize = 0;
        } else {
            primTextureSize =
                    parseInt(primtex, 0,
                             "Try -Dprism.primtextures=[true|false|<number>]");
        }

        /* Setting for reference type used by Disposer */
        refType = systemProperties.getProperty("prism.reftype");

        forcePow2 = getBoolean(systemProperties, "prism.forcepowerof2", false);
        noClampToZero = getBoolean(systemProperties, "prism.noclamptozero", false);

        allowHiDPIScaling = getBoolean(systemProperties, "prism.allowhidpi", true);

        maxVram = getLong(systemProperties, "prism.maxvram", 512 * 1024 * 1024,
                          "Try -Dprism.maxvram=<long>[kKmMgG]");
        targetVram = getLong(systemProperties, "prism.targetvram", maxVram / 8, maxVram,
                             "Try -Dprism.targetvram=<long>[kKmMgG]|<double(0,100)>%");
        poolStats = getBoolean(systemProperties, "prism.poolstats", false);
        poolDebug = getBoolean(systemProperties, "prism.pooldebug", false);

        if (verbose) {
            System.out.print("Prism pipeline init order: ");
            for (String s : tryOrder) {
                System.out.print(s+" ");
            }
            System.out.println("");
            if (rOrder != null) {
                System.out.println("Requested rasterizer preference order: "+rOrder);
            }
            System.out.println("Using "+rSpec);
            printBooleanOption(dirtyOptsEnabled, "Using dirty region optimizations");
            if (primTextureSize == 0) {
                System.out.println("Not using texture mask for primitives");
            } else if (primTextureSize < 0) {
                System.out.println("Using system sized mask for primitives");
            } else {
                System.out.println("Using "+primTextureSize+" sized mask for primitives");
            }
            printBooleanOption(forcePow2, "Forcing power of 2 sizes for textures");
            printBooleanOption(!noClampToZero, "Using hardware CLAMP_TO_ZERO mode");
            printBooleanOption(allowHiDPIScaling, "Opting in for HiDPI pixel scaling");
        }

        /*
         * Setting for maximum texture size. Default is 4096.
         * This will clamp the limit reported by the card to the specified
         * value. A value of <= 0 will disable this clamping, causing the
         * limit reported by the card to be used without modification.
         *
         * See RT-21998. This is a workaround for the fact that we don't
         * yet handle the case where a texture allocation fails during
         * rendering of a very large tiled image.
         */
        int size = getInt(systemProperties, "prism.maxTextureSize",
                          4096, "Try -Dprism.maxTextureSize=<number>");

        if (size <= 0) {
            size = Integer.MAX_VALUE;
        }
        maxTextureSize = size;

        /*
         * Check minimum RTT size
         * This is needed for some embedded platforms to avoid rendering artifacts
         * when rendering into small RTT.
         */
       minRTTSize = getInt(systemProperties, "prism.minrttsize",
               PlatformUtil.isEmbedded() ? 16 : 0, "Try -Dprism.minrttsize=<number>");

        disableRegionCaching = getBoolean(systemProperties,
                                          "prism.disableRegionCaching",
                                          false);

        disableD3D9Ex = getBoolean(systemProperties, "prism.disableD3D9Ex", false);

        disableEffects = getBoolean(systemProperties, "prism.disableEffects", false);

        glyphCacheWidth = getInt(systemProperties, "prism.glyphCacheWidth", 1024,
                "Try -Dprism.glyphCacheWidth=<number>");
        glyphCacheHeight = getInt(systemProperties, "prism.glyphCacheHeight", 1024,
                "Try -Dprism.glyphCacheHeight=<number>");

        /*
         * Performance Logger flags
         * Enable the performance logger, print on exit, print on first paint etc.
         */
        perfLog = systemProperties.getProperty("sun.perflog");
        perfLogExitFlush = getBoolean(systemProperties, "sun.perflog.fx.exitflush", false, true);
        perfLogFirstPaintFlush = getBoolean(systemProperties, "sun.perflog.fx.firstpaintflush", false, true);
        perfLogFirstPaintExit = getBoolean(systemProperties, "sun.perflog.fx.firstpaintexit", false, true);

        superShader = getBoolean(systemProperties, "prism.supershader", true);

        // Force uploading painter (e.g., to avoid Linux live-resize jittering)
        forceUploadingPainter = getBoolean(systemProperties, "prism.forceUploadingPainter", false);

        // Force the use of fragment shader that does alpha testing (i.e. discard if alpha == 0.0)
        forceAlphaTestShader = getBoolean(systemProperties, "prism.forceAlphaTestShader", false);

        // Force non anti-aliasing (not smooth) shape rendering
        forceNonAntialiasedShape = getBoolean(systemProperties, "prism.forceNonAntialiasedShape", false);

    }

    private static int parseInt(String s, int dflt, int trueDflt,
                                String errMsg) {
        return "true".equalsIgnoreCase(s)
                   ? trueDflt
                   : parseInt(s, dflt, errMsg);
    }

    private static int parseInt(String s, int dflt, String errMsg) {
        if (s != null) {
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                if (errMsg != null) {
                    System.err.println(errMsg);
                }
            }
        }

        return dflt;
    }

    private static long parseLong(String s, long dflt, long rel, String errMsg) {
        if (s != null && s.length() > 0) {
            long mult = 1;
            if (s.endsWith("%")) {
                if (rel > 0) {
                    try {
                        s = s.substring(0, s.length() - 1);
                        double percent = Double.parseDouble(s);
                        if (percent >= 0 && percent <= 100) {
                            return Math.round(rel * percent / 100.0);
                        }
                    } catch (Exception e) {
                    }
                }

                if (errMsg != null) {
                    System.err.println(errMsg);
                }
                return dflt;
            }
            if (s.endsWith("k") || s.endsWith("K")) {
                mult = 1024L;
            } else if (s.endsWith("m") || s.endsWith("M")) {
                mult = 1024L * 1024L;
            } else if (s.endsWith("g") || s.endsWith("G")) {
                mult = 1024L * 1024L * 1024L;
            }
            if (mult > 1) {
                s = s.substring(0, s.length() - 1);
            }
            try {
                return Long.parseLong(s) * mult;
            } catch (Exception e) {
                if (errMsg != null) {
                    System.err.println(errMsg);
                }
            }
        }

        return dflt;
    }

    /* A simple version of String.split(), since that isn't available on CDC */
    private static String[] split(String str, String delim) {
        StringTokenizer st = new StringTokenizer(str, delim);
        String[] ret = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            ret[i++] = st.nextToken();
        }
        return ret;
    }

    private static boolean getBoolean(Properties properties,
                                      String key,
                                      boolean dflt) {
        final String strval = properties.getProperty(key);
        return (strval != null) ? Boolean.parseBoolean(strval) : dflt;
    }

    private static boolean getBoolean(Properties properties,
                                      String key,
                                      boolean dflt,
                                      boolean dfltIfDefined) {
        final String strval = properties.getProperty(key);
        if (strval != null && strval.length() == 0) return dfltIfDefined;
        return (strval != null) ? Boolean.parseBoolean(strval) : dflt;
    }

    private static int getInt(Properties properties,
                              String key,
                              int dflt,
                              int trueDflt,
                              String errMsg) {
        return parseInt(properties.getProperty(key),
                        dflt,
                        trueDflt,
                        errMsg);
    }

    private static int getInt(Properties properties,
                              String key,
                              int dflt,
                              String errMsg) {
        return parseInt(properties.getProperty(key),
                        dflt,
                        errMsg);
    }

    private static long getLong(Properties properties,
                                String key,
                                long dflt,
                                String errMsg)
    {
        return parseLong(properties.getProperty(key),
                         dflt, 0,
                         errMsg);
    }

    private static long getLong(Properties properties,
                                String key,
                                long dflt, long rel,
                                String errMsg)
    {
        return parseLong(properties.getProperty(key),
                         dflt, rel,
                         errMsg);
    }
}
