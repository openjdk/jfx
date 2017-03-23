/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font;

import com.sun.javafx.scene.text.FontHelper;
import javafx.scene.text.*;
import com.sun.javafx.tk.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class PrismFontLoader extends FontLoader {
    private static PrismFontLoader theInstance = new PrismFontLoader();
    public static PrismFontLoader getInstance() { return theInstance; }

    /**
     * Flag to keep track whether the fontCache map has been initialized with
     * the embedded fonts.
     */
    private boolean embeddedFontsLoaded = false;

    Properties loadEmbeddedFontDefinitions() {
        Properties map = new Properties();
        // locate the META-INF directory and search for a fonts.mf
        // located there
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) return map;
        URL u = loader.getResource("META-INF/fonts.mf");
        if (u == null) return map;

        // read in the contents of the file
        try (InputStream in = u.openStream()) {
            map.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private void loadEmbeddedFonts() {
        if (!embeddedFontsLoaded) {
            FontFactory fontFactory = getFontFactoryFromPipeline();
            if (!fontFactory.hasPermission()) {
                embeddedFontsLoaded = true;
                return;
            }
            Properties map = loadEmbeddedFontDefinitions();
            Enumeration<?> names = map.keys();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            while (names.hasMoreElements()) {
                String n = (String)names.nextElement();
                String p = map.getProperty(n);
                if (p.startsWith("/")) {
                    p = p.substring(1);
                    try (InputStream in = loader.getResourceAsStream(p)) {
                        fontFactory.loadEmbeddedFont(n, in, 0, true, false);
                    } catch (Exception e) {
                    }
                }
            }
            embeddedFontsLoaded = true;
        }
    }

    private Font[] createFonts(PGFont[] fonts) {
        if (fonts == null || fonts.length == 0) {
            return null;
        }
        Font[] fxFonts = new Font[fonts.length];
        for (int i=0; i<fonts.length; i++) {
            fxFonts[i] = createFont(fonts[i]);
        }
        return fxFonts;
    }

    @Override public Font[] loadFont(InputStream in,
                                     double size,
                                     boolean loadAll) {

        FontFactory factory = getFontFactoryFromPipeline();
        PGFont[] fonts =
            factory.loadEmbeddedFont(null, in, (float)size, true, loadAll);
        return createFonts(fonts);
     }

    @Override public Font[] loadFont(String path,
                                     double size,
                                     boolean loadAll) {

        FontFactory factory = getFontFactoryFromPipeline();
        PGFont[] fonts =
            factory.loadEmbeddedFont(null, path, (float)size, true, loadAll);
        return createFonts(fonts);
    }

    @SuppressWarnings("deprecation")
    private Font createFont(PGFont font) {
        return FontHelper.nativeFont(font,
                                     font.getName(),
                                     font.getFamilyName(),
                                     font.getStyleName(),
                                     font.getSize());
    }

    /**
     * Gets all the font families installed on the user's system, including any
     * embedded fonts or SDK fonts.
     *
     * @profile common
     */
    @Override public List<String> getFamilies() {
        loadEmbeddedFonts();
        return Arrays.asList(getFontFactoryFromPipeline().
                             getFontFamilyNames());
    }

    /**
     * Gets the names of all fonts that are installed on the users system,
     * including any embedded fonts and SDK fonts.
     *
     * @profile common
     */
    @Override public List<String> getFontNames() {
        loadEmbeddedFonts();
        return Arrays.asList(getFontFactoryFromPipeline().getFontFullNames());
    }

    /**
     * Gets the names of all fonts in the specified font family that are
     * installed  on the users system, including any embedded fonts and
     * SDK fonts.
     *
     * @profile common
     */
    @Override public List<String> getFontNames(String family) {
        loadEmbeddedFonts();
        return Arrays.asList(getFontFactoryFromPipeline().
                             getFontFullNames(family));
    }

    /**
     * Searches for an appropriate font based on the font family name and
     * weight and posture style. This method is not guaranteed to return
     * a specific font, but does its best to find one that fits the
     * specified requirements.
     *
     * For SDK/runtime fonts, we will attempt to match properties to a
     * SDK/runtime fonts.  If a specific SDK font is not found in the runtime
     * JAR, the font loading will revert to FontFactory default font, rather
     * then finding closest matching available SDK font. This is how SDK font
     * loading was handled in the past.
     *
     * @param family The family of the font
     * @param weight The weight of the font
     * @param posture The posture or posture of the font
     * @param size The point size of the font. This can be a fractional value
     *
     * @profile desktop
     */
    @Override public Font font(String family, FontWeight weight,
                               FontPosture posture, float size) {

        FontFactory fontFactory = getFontFactoryFromPipeline();
        if (!embeddedFontsLoaded && !fontFactory.isPlatformFont(family)) {
            loadEmbeddedFonts();
        }

        // REMIND. Some day need to have better granularity.

        boolean bold = weight != null &&
                       weight.ordinal() >= FontWeight.BOLD.ordinal();
        boolean italic = posture == FontPosture.ITALIC;
        PGFont prismFont = fontFactory.createFont(family, bold, italic, size);

        // Create Font and set implementation
        Font fxFont = FontHelper.nativeFont(prismFont, prismFont.getName(),
                                            prismFont.getFamilyName(),
                                            prismFont.getStyleName(), size);
        return fxFont;
    }

    /**
     * @param font
     */
    @Override public void loadFont(Font font) {
        FontFactory fontFactory = getFontFactoryFromPipeline();
        String fullName = font.getName();
        if (!embeddedFontsLoaded && !fontFactory.isPlatformFont(fullName)) {
            loadEmbeddedFonts();
        }

        // find the native Prism Font object based on this JavaFX font. At the
        // conclusion of this method, be sure to set the name, family, and
        // style on the Font object via the setNativeFont method.

        // the Prism font we're trying to find
        PGFont prismFont = fontFactory.createFont(fullName, (float)font.getSize());

        // update the name variable to match what was actually loaded
        String name = prismFont.getName();
        String family = prismFont.getFamilyName();
        String style = prismFont.getStyleName();
        FontHelper.setNativeFont(font, prismFont, name, family, style);
    }

    @Override public FontMetrics getFontMetrics(Font font) {
        if (font != null) {
            PGFont prismFont = (PGFont) FontHelper.getNativeFont(font);
            Metrics metrics = PrismFontUtils.getFontMetrics(prismFont);
            // TODO: what's the difference between ascent and maxAscent?
            float maxAscent = -metrics.getAscent();//metrics.getMaxAscent();
            float ascent = -metrics.getAscent();
            float xheight = metrics.getXHeight();
            float descent = metrics.getDescent();
            // TODO: what's the difference between descent and maxDescent?
            float maxDescent = metrics.getDescent();//metrics.getMaxDescent();
            float leading = metrics.getLineGap();
            return FontMetrics.createFontMetrics(maxAscent, ascent, xheight, descent, maxDescent, leading, font);
        } else {
            return null; // this should never happen
        }
    }

    @Override public float getCharWidth(char ch, Font font) {
        PGFont prismFont = (PGFont) FontHelper.getNativeFont(font);
        return (float)PrismFontUtils.getCharWidth(prismFont, ch);
    }

    @Override public float getSystemFontSize() {
        // PrismFontFactory is what loads the DLL, so we may as
        // well place the required native method there.
        return PrismFontFactory.getSystemFontSize();
    }

    FontFactory installedFontFactory = null;
    private FontFactory getFontFactoryFromPipeline() {
        if (installedFontFactory != null) {
            return installedFontFactory;
        }
        try {
            Class plc = Class.forName("com.sun.prism.GraphicsPipeline");
            Method gpm = plc.getMethod("getPipeline", (Class[])null);
            Object plo = gpm.invoke(null);
            Method gfm = plc.getMethod("getFontFactory", (Class[])null);
            Object ffo = gfm.invoke(plo);
            installedFontFactory = (FontFactory)ffo;
        } catch (Exception e) {
        }
        return installedFontFactory;
    }
}
