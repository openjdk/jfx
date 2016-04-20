/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

class FontConfigManager {

    static boolean debugFonts = false;
    static boolean useFontConfig = true;
    static boolean fontConfigFailed = false;
    static boolean useEmbeddedFontSupport = false;

    static {
        AccessController.doPrivileged(
                (PrivilegedAction<Void>) () -> {
                    String dbg = System.getProperty("prism.debugfonts", "");
                    debugFonts = "true".equals(dbg);
                    String ufc = System.getProperty("prism.useFontConfig", "true");
                    useFontConfig = "true".equals(ufc);
                    String emb = System.getProperty("prism.embeddedfonts", "");
                    useEmbeddedFontSupport = "true".equals(emb);
                    return null;
                }
        );
    }

    /* These next three classes are just data structures.
     */
    public static class FontConfigFont {
        public String familyName;        // eg Bitstream Vera Sans
        public String styleStr;          // eg Bold
        public String fullName;          // eg Bitstream Vera Sans Bold
        public String fontFile;          // eg /usr/X11/lib/fonts/foo.ttf
    }

    public static class FcCompFont {
        public String fcName;            // eg sans
        public String fcFamily;          // eg sans
        //public String fxName;           // eg sans serif
        public int style;                // eg 0=PLAIN
        public FontConfigFont firstFont;
        public FontConfigFont[] allFonts;
        //public CompositeFont compFont;   // null if not yet created/known.
    }

    /* fontconfig recognises slants roman, italic, as well as oblique,
     * and a slew of weights, where the ones that matter here are
     * regular and bold.
     * To fully qualify what we want, we can for example ask for (eg)
     * Font.PLAIN             : "serif:regular:roman"
     * Font.BOLD              : "serif:bold:roman"
     * Font.ITALIC            : "serif:regular:italic"
     * Font.BOLD|Font.ITALIC  : "serif:bold:italic"
     */
    private static final String[] fontConfigNames = {
        "sans:regular:roman",
        "sans:bold:roman",
        "sans:regular:italic",
        "sans:bold:italic",

        "serif:regular:roman",
        "serif:bold:roman",
        "serif:regular:italic",
        "serif:bold:italic",

        "monospace:regular:roman",
        "monospace:bold:roman",
        "monospace:regular:italic",
        "monospace:bold:italic",
    };

    /* This array has the array elements created in Java code and is
     * passed down to native to be filled in.
     */
    private static FcCompFont[] fontConfigFonts;

    private FontConfigManager() {
    }

    private static String[] getFontConfigNames() {
        return fontConfigNames;
    }

    private static String getFCLocaleStr() {
        Locale l = Locale.getDefault();
        String localeStr = l.getLanguage();
        String country = l.getCountry();
        if (!country.equals("")) {
            localeStr = localeStr + "-" + country;
        }
        return localeStr;
    }

    /* Return an array of FcCompFont structs describing the primary
     * font located for each of fontconfig/GTK/Pango's logical font names.
     */
    private static native boolean getFontConfig(String locale,
                                                FcCompFont[] fonts,
                                                boolean includeFallbacks);

    private static synchronized void initFontConfigLogFonts() {

        if (fontConfigFonts != null || fontConfigFailed) {
            return;
        }

        long t0 = 0;
        if (debugFonts) {
            t0 = System.nanoTime();
        }

        String[] fontConfigNames = FontConfigManager.getFontConfigNames();
        FcCompFont[] fontArr = new FcCompFont[fontConfigNames.length];

        for (int i = 0; i< fontArr.length; i++) {
            fontArr[i] = new FcCompFont();
            fontArr[i].fcName = fontConfigNames[i];
            int colonPos = fontArr[i].fcName.indexOf(':');
            fontArr[i].fcFamily = fontArr[i].fcName.substring(0, colonPos);
            fontArr[i].style = i % 4; // depends on array order.
        }

        boolean foundFontConfig = false;
        if (useFontConfig) {
            foundFontConfig = getFontConfig(getFCLocaleStr(), fontArr, true);
        } else {
            if (debugFonts) {
                System.err.println("Not using FontConfig");
            }
        }

        if (useEmbeddedFontSupport ||
            !foundFontConfig)
        {
            EmbeddedFontSupport.initLogicalFonts(fontArr);
        }
        FontConfigFont anyFont = null;
        /* If don't find anything (eg no libfontconfig), then just return */
        for (int i = 0; i< fontArr.length; i++) {
            FcCompFont fci = fontArr[i];
            if (fci.firstFont == null) {
                if (debugFonts) {
                    System.err.println("Fontconfig returned no font for " +
                                fontArr[i].fcName);
                }
                fontConfigFailed = true;
            } else if (anyFont == null) {
                anyFont = fci.firstFont;
                defaultFontFile = anyFont.fontFile;
            }
        }

        if (anyFont == null) {
            fontConfigFailed = true;
            System.err.println("Error: JavaFX detected no fonts! " +
                "Please refer to release notes for proper font configuration");
            return;
        } else if (fontConfigFailed) {
            for (int i = 0; i< fontArr.length; i++) {
                if (fontArr[i].firstFont == null) {
                    fontArr[i].firstFont = anyFont;
                }
            }
        }

        fontConfigFonts = fontArr;

        if (debugFonts) {

            long t1 = System.nanoTime();
            System.err.println("Time spent accessing fontconfig="
                               + ((t1 - t0) / 1000000) + "ms.");

            for (int i = 0; i<fontConfigFonts.length; i++) {
                FcCompFont fci = fontConfigFonts[i];
                System.err.println("FC font " + fci.fcName+" maps to " +
                                   fci.firstFont.fullName +
                                   " in file " + fci.firstFont.fontFile);
                if (fci.allFonts != null) {
                    for (int f=0;f<fci.allFonts.length;f++) {
                        FontConfigFont fcf = fci.allFonts[f];
                        System.err.println(" "+f+ ") Family=" +
                                           fcf.familyName +
                                           ", Style="+ fcf.styleStr +
                                           ", Fullname="+fcf.fullName +
                                           ", File="+fcf.fontFile);
                    }
                }
            }
        }
    }

    private static native boolean populateMapsNative
        (HashMap<String,String> fontToFileMap,
         HashMap<String,String> fontToFamilyNameMap,
         HashMap<String,ArrayList<String>> familyToFontListMap,
         Locale locale);

    public static void populateMaps
        (HashMap<String,String> fontToFileMap,
         HashMap<String,String> fontToFamilyNameMap,
         HashMap<String,ArrayList<String>> familyToFontListMap,
         Locale locale) {

        boolean pnm = false;
        if (useFontConfig && !fontConfigFailed) {
            pnm = populateMapsNative(fontToFileMap, fontToFamilyNameMap,
                                familyToFontListMap, locale);

        }

        if (fontConfigFailed ||
            useEmbeddedFontSupport ||
            !pnm) {
            EmbeddedFontSupport.populateMaps(fontToFileMap,
                                             fontToFamilyNameMap,
                                             familyToFontListMap, locale);
        }
    }

    private static String mapFxToFcLogicalFamilyName(String fxName) {
        if (fxName.equals("serif")) {
            return "serif";
        } else if (fxName.equals("monospaced")) {
            return "monospace";
        } else {
             return "sans";
        }
    }

    public static FcCompFont getFontConfigFont(String fxFamilyName,
                                               boolean bold, boolean italic) {

        initFontConfigLogFonts();

        if (fontConfigFonts == null) {
            return null;
        }

        String name = mapFxToFcLogicalFamilyName(fxFamilyName.toLowerCase());
        int style = (bold) ? 1 : 0;
        if (italic) {
            style +=2;
        }

        FcCompFont fcInfo = null;
        for (int i=0; i<fontConfigFonts.length; i++) {
            if (name.equals(fontConfigFonts[i].fcFamily) &&
                style == fontConfigFonts[i].style) {
                fcInfo = fontConfigFonts[i];
                break;
            }
        }
        if (fcInfo == null) {
            fcInfo = fontConfigFonts[0];
        }

        if (debugFonts) {
            System.err.println("FC name=" + name + " style=" + style +
                               " uses " + fcInfo.firstFont.fullName +
                               " in file: " + fcInfo.firstFont.fontFile);
        }
        return fcInfo;
    }

    private static String defaultFontFile;
    public static String getDefaultFontPath() {
        if (fontConfigFonts == null && !fontConfigFailed) {
            // trigger font config initialisation
            getFontConfigFont("System", false, false);
        }
        return defaultFontFile;
    }

    public static ArrayList<String>
        getFileNames(FcCompFont font, boolean fallBacksOnly) {

        ArrayList fileList = new ArrayList<String>();

        if (font.allFonts != null) {
            int start = (fallBacksOnly) ? 1 : 0;
            for (int i=start; i<font.allFonts.length; i++) {
                fileList.add(font.allFonts[i].fontFile);
            }
        }
        return fileList;
    }

    public static ArrayList<String>
        getFontNames(FcCompFont font, boolean fallBacksOnly) {

        ArrayList fontList = new ArrayList<String>();

        if (font.allFonts != null) {
            int start = (fallBacksOnly) ? 1 : 0;
            for (int i=start; i<font.allFonts.length; i++) {
                fontList.add(font.allFonts[i].fullName);
            }
        }
        return fontList;
    }

    /* Embedded Linux may not have fontconfig, in which case we look for
     * 1) A single directory which contains all font files.
     * 2) A java properties style file which describes which of these files
     * to use for the logical fonts.
     * Optionally a system property can be used to set the directory
     * else it will default to "{FXHOME}/lib/fonts".
     */
    private static class EmbeddedFontSupport {

        private static String fontDirProp = null;
        private static String fontDir;
        private static boolean fontDirFromJRE = false;

        static {
            AccessController.doPrivileged(
                    (PrivilegedAction<Void>) () -> {
                        initEmbeddedFonts();
                    return null;
                    }
            );
        }

        private static void initEmbeddedFonts() {
            fontDirProp = System.getProperty("prism.fontdir");
            if (fontDirProp != null) {
                fontDir = fontDirProp;
            } else {
                try {
                    // no fontConfig or -Dprism.fontdir, lets fallback
                    // to {java.home}/lib/fonts if it exists
                    final String javaHome = System.getProperty("java.home");
                    if (javaHome == null) {
                        return;
                    }
                    File fontDirectory = new File(javaHome, "lib/fonts");
                    if (fontDirectory.exists()) {
                        fontDirFromJRE = true;
                        fontDir = fontDirectory.getPath();
                    }
                    if (debugFonts) {
                        System.err.println("Fallback fontDir is " + fontDirectory +
                                           " exists = " +
                                           fontDirectory.exists());
                    }
                } catch (Exception e) {
                    if (debugFonts) {
                        e.printStackTrace();
                    }
                    fontDir = "/";
                }
            }
        }

        private static String getStyleStr(int style) {
            switch (style) {
                case 0 : return "regular";
                case 1 : return "bold";
                case 2 : return "italic";
                case 3 : return "bolditalic";
                default : return "regular";
            }
        }


        private static boolean exists(final File f) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<Boolean>) () -> f.exists()
            );
        }

        // this mapping is used in the embedded world when
        // fontconfig is not used and we find the jre fonts
        static String[] jreFontsProperties = {
            "sans.regular.0.font", "Lucida Sans Regular",
            "sans.regular.0.file", "LucidaSansRegular.ttf",
            "sans.bold.0.font", "Lucida Sans Bold",
            "sans.bold.0.file", "LucidaSansDemiBold.ttf",
            "monospace.regular.0.font", "Lucida Typewriter Regular",
            "monospace.regular.0.file", "LucidaTypewriterRegular.ttf",
            "monospace.bold.0.font", "Lucida Typewriter Bold",
            "monospace.bold.0.file", "LucidaTypewriterBold.ttf",
            "serif.regular.0.font", "Lucida Bright",
            "serif.regular.0.file", "LucidaBrightRegular.ttf",
            "serif.bold.0.font", "Lucida Bright Demibold",
            "serif.bold.0.file", "LucidaBrightDemiBold.ttf",
            "serif.italic.0.font", "Lucida Bright Italic",
            "serif.italic.0.file", "LucidaBrightItalic.ttf",
            "serif.bolditalic.0.font", "Lucida Bright Demibold Italic",
            "serif.bolditalic.0.file", "LucidaBrightDemiItalic.ttf"
        };

        /**
         * Logical Font Support for FX on embedded platforms
         *
         * Reads a simple properties file which defines the 3
         * font families "sans", "serif", "monospace". This uses
         * fontconfig names because the interface to access this
         * goes via internal APIs which communicate with FontConfig.
         * The format using sans as an example, looks like
         * sans.regular.0.font=Arial
         * sans.regular.0.file=Arial.ttf
         * sans.bold.0.font=Arial Bold
         * sans.bold.0.file=Arial-Bold.ttf
         * sans.italic.0.font=Arial Italic
         * sans.italic.0.file=Arial-Italic.ttf
         * sans.bolditalic.0.font=Arial Bold Italic
         * sans.bolditalic.0.file=Arial-BoldItalic.ttf
         *
         * Additional fonts for each style would be indexed 1, 2, etc ..
         * The fonts must be made visible to the embedded FX - see
         * <code>populateMaps()</code>
         */
        static void initLogicalFonts(FcCompFont[] fonts) {

            Properties props = new Properties();
            try {
                File f = new File(fontDir,"logicalfonts.properties");
                if (f.exists()) {
                    FileInputStream fis = new FileInputStream(f);
                    props.load(fis);
                    fis.close();
                } else if (fontDirFromJRE) {
                    // Our fontDir is in the JRE (at least relative to our Jar)
                    // we have no logicalfonts.properties, but we can see
                    // if we can intuit one.... this is checked next.
                    for(int i=0; i < jreFontsProperties.length; i += 2) {
                        props.setProperty(jreFontsProperties[i],jreFontsProperties[i+1]);
                    }
                    if (debugFonts) {
                        System.err.println("Using fallback implied logicalfonts.properties");
                    }
                }
            } catch (IOException ioe) {
                if (debugFonts) {
                    System.err.println(ioe);
                    return;
                }
            }
            for (int f=0; f<fonts.length; f++) {
                String fcFamily = fonts[f].fcFamily;
                String styleStr = getStyleStr(fonts[f].style);
                String key = fcFamily+"."+styleStr+".";
                ArrayList<FontConfigFont> allFonts =
                      new ArrayList<FontConfigFont>();
                int i=0;
                while (true) {
                    String file = props.getProperty(key+i+".file");
                    String font = props.getProperty(key+i+".font");
                    i++;
                    if (file == null) {
                        break;
                    }
                    File ff = new File(fontDir, file);
                    if (!exists(ff)) {
                        if (debugFonts) {
                            System.out.println("Failed to find logical font file "+ff);
                        }
                        continue;
                    }
                    FontConfigFont fcFont = new FontConfigFont();
                    fcFont.fontFile = ff.getPath(); // required.
                    fcFont.fullName = font; // optional.
                    fcFont.familyName = null; // not used.
                    fcFont.styleStr = null; // not used.
                    if (fonts[f].firstFont == null) {
                        fonts[f].firstFont = fcFont;
                    }
                    allFonts.add(fcFont);
                }
                if (allFonts.size() > 0) {
                    fonts[f].allFonts = new FontConfigFont[allFonts.size()];
                    allFonts.toArray(fonts[f].allFonts);
                }
            }
        }

        /*
         * To speed lookup of fonts, check for a file called
         * allfonts.properties. Only fonts in that file will be enumerated.
         * If this file isn't present we'll open all the files.
         * The file name must be a simple basename, and is mandatory.
         * But you are encouraged to name the font and family too.
         * If this file doesn't say the name or family of a font,
         * we'll still open the file. The names must be the actual
         * font names. No promises to fix it if you spell it wrongly.
         * We look for "maxFont" and if that's a valid property
         * we'll look from 0 .. maxFont, else we stop at the first
         * index for which no file exists. Eg file :
         * maxFont=1
         * family.0=Arial
         * font.0=Arial Regular
         * file.0=arial.ttf
         * family.1=Times New Roman
         * font.1=Times New Roman Regular
         * file.1=times.ttf
         *
         * NOTE: if you are testing embedded on desktop, and are using
         * the 2D pipeline, you will want to use /home/<you>/.fonts as
         * the font directory, else Java 2D won't know where to find
         * the fonts. If this turns out to be a major impediment, we
         * can arrange to register them with Java 2D.
         */
        static void populateMaps
            (HashMap<String,String> fontToFileMap,
             HashMap<String,String> fontToFamilyNameMap,
             HashMap<String,ArrayList<String>> familyToFontListMap,
             Locale locale)
        {
            final Properties props = new Properties();
            AccessController.doPrivileged(
                    (PrivilegedAction<Void>) () -> {
                        try {
                            String lFile = fontDir+"/allfonts.properties";
                            FileInputStream fis = new FileInputStream(lFile);
                            props.load(fis);
                            fis.close();
                        } catch (IOException ioe) {
                            props.clear();
                            if (debugFonts) {
                                System.err.println(ioe);
                                System.err.println("Fall back to opening the files");
                            }
                        }
                        return null;
                    }
            );

            if (!props.isEmpty()) {
                int maxFont = Integer.MAX_VALUE;
                try {
                    maxFont = Integer.parseInt(props.getProperty("maxFont",""));
                } catch (NumberFormatException e) {
                }
                if (maxFont <= 0) {
                    maxFont = Integer.MAX_VALUE;
                }
                for (int f=0; f<maxFont; f++) {
                    String family = props.getProperty("family."+f);
                    String font = props.getProperty("font."+f);
                    String file = props.getProperty("file."+f);
                    if (file == null) {
                        break;
                    }
                    File ff = new File(fontDir, file);
                    if (!exists(ff)) {
                        continue;
                    }
                    if (family == null || font == null) {
                        continue; // TBD
                    }
                    String fontLC = font.toLowerCase(Locale.ENGLISH);
                    String familyLC = family.toLowerCase(Locale.ENGLISH);
                    fontToFileMap.put(fontLC, ff.getPath());
                    fontToFamilyNameMap.put(fontLC, family);
                    ArrayList<String> familyArr =
                        familyToFontListMap.get(familyLC);
                    if (familyArr == null) {
                        familyArr = new ArrayList<String>(4);
                        familyToFontListMap.put(familyLC, familyArr);
                    }
                    familyArr.add(font);
                }
            }
        }
    }

}
