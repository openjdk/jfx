/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.monocle;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.javafx.util.Logging;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Provides the values of the EPD system properties.
 */
class EPDSettings {

    /**
     * Sets the frame buffer color depth and pixel format: 8 for 8-bit grayscale
     * in the Y8 pixel format, 16 for 16-bit color in the RGB565 pixel format,
     * or 32 for 32-bit color in the ARGB32 pixel format. The default is 32.
     * <p>
     * Using the 32-bit format allows JavaFX to render directly into the Linux
     * frame buffer and avoid the step of copying and converting each pixel from
     * an off-screen composition buffer.</p>
     *
     * @implNote Corresponds to the {@code bits_per_pixel} field of
     * {@code fb_var_screeninfo} in <i>linux/fb.h</i>.
     */
    private static final String BITS_PER_PIXEL = "monocle.epd.bitsPerPixel";

    /**
     * Sets the frame buffer rotation: 0 for unrotated (UR), 1 for 90 degrees
     * clockwise (CW), 2 for 180 degrees upside-down (UD), and 3 for 90 degrees
     * counter-clockwise (CCW). The default is 0.
     * <p>
     * The unrotated and upside-down settings are in landscape mode, while the
     * clockwise and counter-clockwise settings are in portrait.</p>
     *
     * @implNote Corresponds to the {@code rotate} field of
     * {@code fb_var_screeninfo} in <i>linux/fb.h</i>.
     */
    private static final String ROTATE = "monocle.epd.rotate";

    /**
     * Sets an indicator for the frame buffer grayscale value: {@code true} to
     * invert the pixels of all updates when using 8-bit grayscale in the Y8
     * pixel format; otherwise {@code false}. The default is {@code false}.
     * <p>
     * The value is ignored when the frame buffer is not set to 8-bit grayscale
     * in the Y8 pixel format.</p>
     *
     * @implNote Corresponds to the {@code GRAYSCALE_8BIT_INVERTED} constant in
     * <i>linux/mxcfb.h</i>.
     */
    private static final String Y8_INVERTED = "monocle.epd.Y8Inverted";

    /**
     * Indicates whether to wait for the previous update to complete before
     * sending the next update: {@code true} to avoid waiting and send updates
     * as quickly as possible; otherwise {@code false}. The default is
     * {@code false}.
     * <p>
     * The number of outstanding updates is limited by the device controller to
     * either 16 or 64 concurrent non-colliding updates, depending on the model.
     * A value of {@code true} may result in errors if the maximum number of
     * concurrent non-colliding updates is exceeded.</p>
     *
     * @implNote Corresponds to the IOCTL call constant
     * {@code MXCFB_WAIT_FOR_UPDATE_COMPLETE} in <i>linux/mxcfb.h</i>.
     */
    private static final String NO_WAIT = "monocle.epd.noWait";

    /**
     * Sets the waveform mode used for updates: 1 for black-and-white direct
     * update (DU), 2 for 16 levels of gray (GC16), 3 for 4 levels of gray
     * (GC4), 4 for pure black-and-white animation (A2), and 257 for the
     * automatic selection of waveform mode based on the number of gray levels
     * in the update (AUTO). The default is 257.
     * <p>
     * Automatic selection chooses one of 1 (DU), 2 (GC16), or 3 (GC4). If the
     * waveform mode is set to 2 (GC16), it may be upgraded to a compatible but
     * optimized mode internal to the driver, if available.</p>
     *
     * @implNote Corresponds to the {@code waveform_mode} field of
     * {@code mxcfb_update_data} in <i>linux/mxcfb.h</i>.
     */
    private static final String WAVEFORM_MODE = "monocle.epd.waveformMode";

    /**
     * Sets the update flag for pixel inversion: {@code true} to invert the
     * pixels of each update; otherwise {@code false}. The default is
     * {@code false}.
     *
     * @implNote Corresponds to the {@code EPDC_FLAG_ENABLE_INVERSION} constant
     * in <i>linux/mxcfb.h</i>.
     */
    private static final String FLAG_ENABLE_INVERSION = "monocle.epd.enableInversion";

    /**
     * Sets the update flag for monochrome conversion: {@code true} to convert
     * the pixels of each update to pure black and white using a 50-percent
     * threshold; otherwise {@code false}. The default is {@code false}.
     *
     * @implNote Corresponds to the {@code EPDC_FLAG_FORCE_MONOCHROME} constant
     * in <i>linux/mxcfb.h</i>.
     */
    private static final String FLAG_FORCE_MONOCHROME = "monocle.epd.forceMonochrome";

    /**
     * Sets the update flag for 1-bit dithering: {@code true} to dither each
     * update in an 8-bit Y8 frame buffer to 1-bit black and white, if
     * available; otherwise {@code false}. The default is {@code false}.
     *
     * @implNote Corresponds to the {@code EPDC_FLAG_USE_DITHERING_Y1} constant
     * in <i>linux/mxcfb.h</i>.
     */
    private static final String FLAG_USE_DITHERING_Y1 = "monocle.epd.useDitheringY1";

    /**
     * Sets the update flag for 4-bit dithering: {@code true} to dither each
     * update in an 8-bit Y8 frame buffer to 4-bit grayscale, if available;
     * otherwise {@code false}. The default is {@code false}.
     *
     * @implNote Corresponds to the {@code EPDC_FLAG_USE_DITHERING_Y4} constant
     * in <i>linux/mxcfb.h</i>.
     */
    private static final String FLAG_USE_DITHERING_Y4 = "monocle.epd.useDitheringY4";

    /**
     * Indicates whether to work around the bug found on devices, such as the
     * Kobo Clara HD Model N249, which require a screen width equal to the
     * visible x-resolution, instead of the normal virtual x-resolution, when
     * using an 8-bit, unrotated, and uninverted frame buffer in the Y8 pixel
     * format: {@code true} to work around the bug; otherwise {@code false}. The
     * default is {@code false}.
     */
    private static final String FIX_WIDTH_Y8UR = "monocle.epd.fixWidthY8UR";

    private static final String[] EPD_PROPERTIES = {
        BITS_PER_PIXEL,
        ROTATE,
        Y8_INVERTED,
        NO_WAIT,
        WAVEFORM_MODE,
        FLAG_ENABLE_INVERSION,
        FLAG_FORCE_MONOCHROME,
        FLAG_USE_DITHERING_Y1,
        FLAG_USE_DITHERING_Y4,
        FIX_WIDTH_Y8UR
    };

    private static final int BITS_PER_PIXEL_DEFAULT = Integer.SIZE;
    private static final int ROTATE_DEFAULT = EPDSystem.FB_ROTATE_UR;
    private static final int WAVEFORM_MODE_DEFAULT = EPDSystem.WAVEFORM_MODE_AUTO;

    private static final int[] BITS_PER_PIXEL_PERMITTED = {
        Byte.SIZE,
        Short.SIZE,
        Integer.SIZE
    };

    private static final int[] ROTATIONS_PERMITTED = {
        EPDSystem.FB_ROTATE_UR,
        EPDSystem.FB_ROTATE_CW,
        EPDSystem.FB_ROTATE_UD,
        EPDSystem.FB_ROTATE_CCW
    };

    private static final int[] WAVEFORM_MODES_PERMITTED = {
        EPDSystem.WAVEFORM_MODE_DU,
        EPDSystem.WAVEFORM_MODE_GC16,
        EPDSystem.WAVEFORM_MODE_GC4,
        EPDSystem.WAVEFORM_MODE_A2,
        EPDSystem.WAVEFORM_MODE_AUTO
    };

    /**
     * Obtains a new instance of this class with the current values of the EPD
     * system properties.
     *
     * @return a new {@code EPDSettings} instance
     */
    @SuppressWarnings("removal")
    static EPDSettings newInstance() {
        return AccessController.doPrivileged(
                (PrivilegedAction<EPDSettings>) () -> new EPDSettings());
    }

    private final PlatformLogger logger = Logging.getJavaFXLogger();

    private final boolean y8inverted;
    private final boolean flagEnableInversion;
    private final boolean flagForceMonochrome;
    private final boolean flagUseDitheringY1;
    private final boolean flagUseDitheringY4;
    private final boolean fixWidthY8UR;

    final int bitsPerPixel;
    final int rotate;
    final boolean noWait;
    final int waveformMode;
    final int grayscale;
    final int flags;
    final boolean getWidthVisible;

    /**
     * Creates a new EPDSettings, capturing the current values of the EPD system
     * properties.
     */
    private EPDSettings() {
        if (logger.isLoggable(Level.FINE)) {
            HashMap<String, String> map = new HashMap<>();
            for (String key : EPD_PROPERTIES) {
                String value = System.getProperty(key);
                if (value != null) {
                    map.put(key, value);
                }
            }
            logger.fine("EPD system properties: {0}", map);
        }

        bitsPerPixel = getInteger(BITS_PER_PIXEL, BITS_PER_PIXEL_DEFAULT, BITS_PER_PIXEL_PERMITTED);
        rotate = getInteger(ROTATE, ROTATE_DEFAULT, ROTATIONS_PERMITTED);
        noWait = Boolean.getBoolean(NO_WAIT);
        waveformMode = getInteger(WAVEFORM_MODE, WAVEFORM_MODE_DEFAULT, WAVEFORM_MODES_PERMITTED);

        y8inverted = Boolean.getBoolean(Y8_INVERTED);
        if (bitsPerPixel == Byte.SIZE) {
            if (y8inverted) {
                grayscale = EPDSystem.GRAYSCALE_8BIT_INVERTED;
            } else {
                grayscale = EPDSystem.GRAYSCALE_8BIT;
            }
        } else {
            grayscale = 0;
        }

        flagEnableInversion = Boolean.getBoolean(FLAG_ENABLE_INVERSION);
        flagForceMonochrome = Boolean.getBoolean(FLAG_FORCE_MONOCHROME);
        flagUseDitheringY1 = Boolean.getBoolean(FLAG_USE_DITHERING_Y1);
        flagUseDitheringY4 = Boolean.getBoolean(FLAG_USE_DITHERING_Y4);
        flags = (flagEnableInversion ? EPDSystem.EPDC_FLAG_ENABLE_INVERSION : 0)
                | (flagForceMonochrome ? EPDSystem.EPDC_FLAG_FORCE_MONOCHROME : 0)
                | (flagUseDitheringY1 ? EPDSystem.EPDC_FLAG_USE_DITHERING_Y1 : 0)
                | (flagUseDitheringY4 ? EPDSystem.EPDC_FLAG_USE_DITHERING_Y4 : 0);

        fixWidthY8UR = Boolean.getBoolean(FIX_WIDTH_Y8UR);
        getWidthVisible = fixWidthY8UR && grayscale == EPDSystem.GRAYSCALE_8BIT
                && rotate == EPDSystem.FB_ROTATE_UR;
    }

    /**
     * Gets an integer system property.
     *
     * @param key the property name
     * @param def the default value
     * @param list a list of the permitted values for the property
     * @return the value provided for the property if it is equal to one of the
     * permitted values; otherwise, the default value
     */
    private int getInteger(String key, int def, int... list) {
        int value = Integer.getInteger(key, def);
        boolean found = false;
        for (int i = 0; i < list.length && !found; i++) {
            found = value == list[i];
        }
        if (!found) {
            logger.severe("Value of {0}={1} not in {2}; using default ({3})",
                    key, value, Arrays.toString(list), def);
            value = def;
        }
        return value;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}[bitsPerPixel={1} rotate={2} "
                + "noWait={3} waveformMode={4} grayscale={5} flags=0x{6} "
                + "getWidthVisible={7}]",
                getClass().getName(), bitsPerPixel, rotate,
                noWait, waveformMode, grayscale, Integer.toHexString(flags),
                getWidthVisible);
    }
}
