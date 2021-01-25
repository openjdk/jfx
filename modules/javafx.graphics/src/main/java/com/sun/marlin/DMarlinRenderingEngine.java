/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.marlin;

import java.security.AccessController;
import static com.sun.marlin.MarlinUtils.logInfo;
import com.sun.util.reentrant.ReentrantContextProvider;
import com.sun.util.reentrant.ReentrantContextProviderCLQ;
import com.sun.util.reentrant.ReentrantContextProviderTL;
import com.sun.javafx.geom.PathIterator;
import com.sun.prism.BasicStroke;
import java.security.PrivilegedAction;

/**
 * Marlin RendererEngine implementation (derived from Pisces)
 */
public final class DMarlinRenderingEngine implements MarlinConst
{
    /**
     * Private constructor to prevent instantiation.
     */
    private DMarlinRenderingEngine() {
    }

    static {
        if (PathIterator.WIND_NON_ZERO != WIND_NON_ZERO ||
            PathIterator.WIND_EVEN_ODD != WIND_EVEN_ODD ||
            BasicStroke.JOIN_MITER != JOIN_MITER ||
            BasicStroke.JOIN_ROUND != JOIN_ROUND ||
            BasicStroke.JOIN_BEVEL != JOIN_BEVEL ||
            BasicStroke.CAP_BUTT != CAP_BUTT ||
            BasicStroke.CAP_ROUND != CAP_ROUND ||
            BasicStroke.CAP_SQUARE != CAP_SQUARE)
        {
            throw new InternalError("mismatched renderer constants");
        }
    }

    // --- RendererContext handling ---
    // use ThreadLocal or ConcurrentLinkedQueue to get one RendererContext
    private static final boolean USE_THREAD_LOCAL;

    // reference type stored in either TL or CLQ
    static final int REF_TYPE;

    // Per-thread RendererContext
    private static final ReentrantContextProvider<RendererContext> RDR_CTX_PROVIDER;

    // Static initializer to use TL or CLQ mode
    static {
        USE_THREAD_LOCAL = MarlinProperties.isUseThreadLocal();

        // Soft reference by default:
        final String refType = AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> {
                String value = System.getProperty("prism.marlin.useRef");
                return (value == null) ? "soft" : value;
            });
        switch (refType) {
            default:
            case "soft":
                REF_TYPE = ReentrantContextProvider.REF_SOFT;
                break;
            case "weak":
                REF_TYPE = ReentrantContextProvider.REF_WEAK;
                break;
            case "hard":
                REF_TYPE = ReentrantContextProvider.REF_HARD;
                break;
        }

        if (USE_THREAD_LOCAL) {
            RDR_CTX_PROVIDER = new ReentrantContextProviderTL<RendererContext>(REF_TYPE)
                {
                    @Override
                    protected RendererContext newContext() {
                        return RendererContext.createContext();
                    }
                };
        } else {
            RDR_CTX_PROVIDER = new ReentrantContextProviderCLQ<RendererContext>(REF_TYPE)
                {
                    @Override
                    protected RendererContext newContext() {
                        return RendererContext.createContext();
                    }
                };
        }

        logSettings(Renderer.class.getName());
    }

    private static boolean SETTINGS_LOGGED = !ENABLE_LOGS;

    public static void logSettings(final String reClass) {
        // log information at startup
        if (SETTINGS_LOGGED) {
            return;
        }
        SETTINGS_LOGGED = true;

        String refType;
        switch (REF_TYPE) {
            default:
            case ReentrantContextProvider.REF_HARD:
                refType = "hard";
                break;
            case ReentrantContextProvider.REF_SOFT:
                refType = "soft";
                break;
            case ReentrantContextProvider.REF_WEAK:
                refType = "weak";
                break;
        }

        logInfo("=========================================================="
                + "=====================");

        logInfo("Marlin software rasterizer    = ENABLED");
        logInfo("Version                       = ["
                + Version.getVersion() + "]");
        logInfo("prism.marlin                  = "
                + reClass);
        logInfo("prism.marlin.useThreadLocal   = "
                + USE_THREAD_LOCAL);
        logInfo("prism.marlin.useRef           = "
                + refType);

        logInfo("prism.marlin.edges            = "
                + MarlinConst.INITIAL_EDGES_COUNT);
        logInfo("prism.marlin.pixelWidth       = "
                + MarlinConst.INITIAL_PIXEL_WIDTH);
        logInfo("prism.marlin.pixelHeight      = "
                + MarlinConst.INITIAL_PIXEL_HEIGHT);

        logInfo("prism.marlin.profile          = "
                + (MarlinProperties.isProfileQuality() ?
                    "quality" : "speed"));

        logInfo("prism.marlin.subPixel_log2_X  = "
                + MarlinConst.SUBPIXEL_LG_POSITIONS_X);
        logInfo("prism.marlin.subPixel_log2_Y  = "
                + MarlinConst.SUBPIXEL_LG_POSITIONS_Y);

        logInfo("prism.marlin.blockSize_log2   = "
                + MarlinConst.BLOCK_SIZE_LG);

        // RLE / blockFlags settings

        logInfo("prism.marlin.forceRLE         = "
                + MarlinProperties.isForceRLE());
        logInfo("prism.marlin.forceNoRLE       = "
                + MarlinProperties.isForceNoRLE());
        logInfo("prism.marlin.useTileFlags     = "
                + MarlinProperties.isUseTileFlags());
        logInfo("prism.marlin.useTileFlags.useHeuristics = "
                + MarlinProperties.isUseTileFlagsWithHeuristics());
        logInfo("prism.marlin.rleMinWidth      = "
                + MarlinConst.RLE_MIN_WIDTH);

        // optimisation parameters
        logInfo("prism.marlin.useSimplifier    = "
                + MarlinConst.USE_SIMPLIFIER);
        logInfo("prism.marlin.usePathSimplifier= "
                + MarlinConst.USE_PATH_SIMPLIFIER);
        logInfo("prism.marlin.pathSimplifier.pixTol = "
                + MarlinProperties.getPathSimplifierPixelTolerance());

        logInfo("prism.marlin.clip             = "
                + MarlinProperties.isDoClip());
        logInfo("prism.marlin.clip.runtime.enable = "
                + MarlinProperties.isDoClipRuntimeFlag());

        logInfo("prism.marlin.clip.subdivider  = "
                + MarlinProperties.isDoClipSubdivider());
        logInfo("prism.marlin.clip.subdivider.minLength = "
                + MarlinProperties.getSubdividerMinLength());

        // debugging parameters
        logInfo("prism.marlin.doStats          = "
                + MarlinConst.DO_STATS);
        logInfo("prism.marlin.doMonitors       = "
                + MarlinConst.DO_MONITORS);
        logInfo("prism.marlin.doChecks         = "
                + MarlinConst.DO_CHECKS);

        // logging parameters
        logInfo("prism.marlin.log              = "
                + MarlinConst.ENABLE_LOGS);
        logInfo("prism.marlin.useLogger        = "
                + MarlinConst.USE_LOGGER);
        logInfo("prism.marlin.logCreateContext = "
                + MarlinConst.LOG_CREATE_CONTEXT);
        logInfo("prism.marlin.logUnsafeMalloc  = "
                + MarlinConst.LOG_UNSAFE_MALLOC);

        // quality settings
        logInfo("prism.marlin.curve_len_err    = "
                + MarlinProperties.getCurveLengthError());
        logInfo("prism.marlin.cubic_dec_d2     = "
                + MarlinProperties.getCubicDecD2());
        logInfo("prism.marlin.cubic_inc_d1     = "
                + MarlinProperties.getCubicIncD1());
        logInfo("prism.marlin.quad_dec_d2      = "
                + MarlinProperties.getQuadDecD2());

        logInfo("Renderer settings:");
        logInfo("CUB_DEC_BND  = " + Renderer.CUB_DEC_BND);
        logInfo("CUB_INC_BND  = " + Renderer.CUB_INC_BND);
        logInfo("QUAD_DEC_BND = " + Renderer.QUAD_DEC_BND);

        logInfo("INITIAL_EDGES_CAPACITY        = "
                + MarlinConst.INITIAL_EDGES_CAPACITY);
        logInfo("INITIAL_CROSSING_COUNT        = "
                + Renderer.INITIAL_CROSSING_COUNT);

        logInfo("=========================================================="
                + "=====================");
    }

    /**
     * Get the RendererContext instance dedicated to the current thread
     * @return RendererContext instance
     */
    @SuppressWarnings({"unchecked"})
    public static RendererContext getRendererContext() {
        final RendererContext rdrCtx = RDR_CTX_PROVIDER.acquire();
        if (DO_MONITORS) {
            rdrCtx.stats.mon_pre_getAATileGenerator.start();
        }
        return rdrCtx;
    }

    /**
     * Reset and return the given RendererContext instance for reuse
     * @param rdrCtx RendererContext instance
     */
    public static void returnRendererContext(final RendererContext rdrCtx) {
        rdrCtx.dispose();

        if (DO_MONITORS) {
            rdrCtx.stats.mon_pre_getAATileGenerator.stop();
        }
        RDR_CTX_PROVIDER.release(rdrCtx);
    }
}
