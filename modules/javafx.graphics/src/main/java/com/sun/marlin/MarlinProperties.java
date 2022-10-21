/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.security.PrivilegedAction;

public final class MarlinProperties {

    private MarlinProperties() {
        // no-op
    }

    // marlin system properties

    public static boolean isUseThreadLocal() {
        return getBoolean("prism.marlin.useThreadLocal", "true");
    }

    /**
     * Return the initial edge capacity used to define initial arrays
     * (edges, polystack, crossings)
     *
     * @return 256 < initial edges < 65536 (4096 by default)
     */
    public static int getInitialEdges() {
        return align(
            getInteger("prism.marlin.edges", 4096, 64, 64 * 1024),
            64);
    }

    /**
     * Return the initial pixel width used to define initial arrays
     * (tile AA chunk, alpha line)
     *
     * @return 64 < initial pixel size < 32768 (4096 by default)
     */
    public static int getInitialPixelWidth() {
        return align(
            getInteger("prism.marlin.pixelWidth", 4096, 64, 32 * 1024),
            64);
    }

    /**
     * Return the initial pixel height used to define initial arrays
     * (buckets)
     *
     * @return 64 < initial pixel size < 32768 (2176 by default)
     */
    public static int getInitialPixelHeight() {
        return align(
            getInteger("prism.marlin.pixelHeight", 2176, 64, 32 * 1024),
            64);
    }

    /**
     * Return true if the profile is 'quality' (default) over 'speed'
     *
     * @return true if the profile is 'quality' (default), false otherwise
     */
    public static boolean isProfileQuality() {
        final String key = "prism.marlin.profile";
        final String profile = getString(key, "quality");
        if ("quality".equals(profile)) {
            return true;
        }
        if ("speed".equals(profile)) {
            return false;
        }
        logInfo("Invalid value for " + key + " = " + profile
                    + "; expect value in [quality, speed] !");
        return true;
    }

    /**
     * Return the log(2) corresponding to subpixel on x-axis
     *
     * @return 0 (1 subpixels) < initial pixel size < 8 (256 subpixels)
     * (8 by default ie 256 subpixels)
     */
    public static int getSubPixel_Log2_X() {
        return getInteger("prism.marlin.subPixel_log2_X", 8, 0, 8);
    }

    /**
     * Return the log(2) corresponding to subpixel on y-axis
     *
     * @return 0 (1 subpixels) < initial pixel size < 8 (256 subpixels)
     * (3 by default ie 8 subpixels for the quality profile)
     * (2 by default ie 4 subpixels for the speed profile)
     */
    public static int getSubPixel_Log2_Y() {
        final int def = isProfileQuality() ? 3 : 2;
        return getInteger("prism.marlin.subPixel_log2_Y", def, 0, 8);
    }

    /**
     * Return the log(2) corresponding to the block size in pixels
     *
     * @return 3 (8 pixels) < block size < 8 (256 pixels)
     * (5 by default ie 32 pixels)
     */
    public static int getBlockSize_Log2() {
        return getInteger("prism.marlin.blockSize_log2", 5, 3, 8);
    }

    // RLE / blockFlags settings

    public static boolean isForceRLE() {
        return getBoolean("prism.marlin.forceRLE", "false");
    }

    public static boolean isForceNoRLE() {
        return getBoolean("prism.marlin.forceNoRLE", "false");
    }

    public static boolean isUseTileFlags() {
        return getBoolean("prism.marlin.useTileFlags", "true");
    }

    public static boolean isUseTileFlagsWithHeuristics() {
        return isUseTileFlags()
        && getBoolean("prism.marlin.useTileFlags.useHeuristics", "true");
    }

    public static int getRLEMinWidth() {
        return getInteger("prism.marlin.rleMinWidth", 64, 0, Integer.MAX_VALUE);
    }

    // optimisation parameters

    public static boolean isUseSimplifier() {
        return getBoolean("prism.marlin.useSimplifier", "false");
    }

    public static boolean isUsePathSimplifier() {
        return getBoolean("prism.marlin.usePathSimplifier", "false");
    }

    public static float getPathSimplifierPixelTolerance() {
        // default: MIN_PEN_SIZE or less ?
        return getFloat("prism.marlin.pathSimplifier.pixTol",
                (1.0f / MarlinConst.MIN_SUBPIXELS),
                1e-3f,
                10.0f);
    }

    public static float getStrokerJoinError() {
        final float def = (1.0f / MarlinConst.MIN_SUBPIXELS);
        float err = getFloat("prism.marlin.stroker.joinError",
                def,
                -1.0f,
                10.0f);
        return (err < 0.0f) ? def : err;
    }

    public static int getStrokerJoinStyle() {
        return getInteger("prism.marlin.stroker.joinStyle", -1, -1, 2);
    }

    public static boolean isDoClip() {
        return getBoolean("prism.marlin.clip", "true");
    }

    public static boolean isDoClipRuntimeFlag() {
        return getBoolean("prism.marlin.clip.runtime.enable", "false");
    }

    public static boolean isDoClipAtRuntime() {
        return getBoolean("prism.marlin.clip.runtime", "true");
    }

    public static boolean isDoClipSubdivider() {
        return getBoolean("prism.marlin.clip.subdivider", "true");
    }

    public static float getSubdividerMinLength() {
        return getFloat("prism.marlin.clip.subdivider.minLength", 100.0f, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    public static boolean isUseDPQS() {
        return getBoolean("prism.marlin.useDPQS", "true");
    }

    // debugging parameters

    public static boolean isDoStats() {
        return getBoolean("prism.marlin.doStats", "false");
    }

    public static boolean isDoMonitors() {
        return getBoolean("prism.marlin.doMonitors", "false");
    }

    public static boolean isDoChecks() {
        return getBoolean("prism.marlin.doChecks", "false");
    }

    public static boolean isSkipRenderer() {
        return getBoolean("prism.marlin.skip_rdr", "false");
    }

    public static boolean isSkipRenderTiles() {
        return getBoolean("prism.marlin.skip_pipe", "false");
    }

    // logging parameters

    public static boolean isLoggingEnabled() {
        return getBoolean("prism.marlin.log", "false");
    }

    public static boolean isUseLogger() {
        return getBoolean("prism.marlin.useLogger", "false");
    }

    public static boolean isLogCreateContext() {
        return getBoolean("prism.marlin.logCreateContext", "false");
    }

    public static boolean isLogUnsafeMalloc() {
        return getBoolean("prism.marlin.logUnsafeMalloc", "false");
    }

    // quality settings

    public static float getCurveLengthError() {
        return getFloat("prism.marlin.curve_len_err", 0.01f, 1e-6f, 1.0f);
    }

    public static float getCubicDecD2() {
        final float def = isProfileQuality() ? 1.0f : 2.5f;
        return getFloat("prism.marlin.cubic_dec_d2", def, 1e-5f, 4.0f);
    }

    public static float getCubicIncD1() {
        final float def = isProfileQuality() ? 0.2f : 0.5f;
        return getFloat("prism.marlin.cubic_inc_d1", def, 1e-6f, 1.0f);
    }

    public static float getQuadDecD2() {
        final float def = isProfileQuality() ? 0.5f : 1.0f;
        return getFloat("prism.marlin.quad_dec_d2", def, 1e-5f, 4.0f);
    }

    // system property utilities
    @SuppressWarnings("removal")
    static String getString(final String key, final String def) {
        return AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> {
                String value = System.getProperty(key);
                return (value == null) ? def : value;
            });
    }

    @SuppressWarnings("removal")
    static boolean getBoolean(final String key, final String def) {
        return Boolean.valueOf(AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> {
                String value = System.getProperty(key);
                return (value == null) ? def : value;
            }));
    }

    static int getInteger(final String key, final int def,
                                 final int min, final int max)
    {
        @SuppressWarnings("removal")
        final String property = AccessController.doPrivileged(
                    (PrivilegedAction<String>) () -> System.getProperty(key));

        int value = def;
        if (property != null) {
            try {
                value = Integer.decode(property);
            } catch (NumberFormatException e) {
                logInfo("Invalid integer value for " + key + " = " + property);
            }
        }

        // check for invalid values
        if ((value < min) || (value > max)) {
            logInfo("Invalid value for " + key + " = " + value
                    + "; expected value in range[" + min + ", " + max + "] !");
            value = def;
        }
        return value;
    }

    static int align(final int val, final int norm) {
        final int ceil = FloatMath.ceil_int( ((float) val) / norm);
        return ceil * norm;
    }

    public static double getDouble(final String key, final double def,
                                   final double min, final double max)
    {
        double value = def;
        @SuppressWarnings("removal")
        final String property = AccessController.doPrivileged(
                    (PrivilegedAction<String>) () -> System.getProperty(key));

        if (property != null) {
            try {
                value = Double.parseDouble(property);
            } catch (NumberFormatException nfe) {
                logInfo("Invalid value for " + key + " = " + property + " !");
            }
        }
        // check for invalid values
        if (value < min || value > max) {
            logInfo("Invalid value for " + key + " = " + value
                    + "; expect value in range[" + min + ", " + max + "] !");
            value = def;
        }
        return value;
    }

    public static float getFloat(final String key, final float def,
                                 final float min, final float max)
    {
        return (float)getDouble(key, def, min, max);
    }
}
