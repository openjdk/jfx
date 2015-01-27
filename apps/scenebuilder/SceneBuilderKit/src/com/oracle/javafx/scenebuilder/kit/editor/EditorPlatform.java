/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor;

import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.scene.input.MouseEvent;

/**
 * This class contains static methods that depends on the platform.
 *
 * @treatAsPrivate
 */
public class EditorPlatform {

    private static final String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT); //NOI18N

    /**
     * True if current platform is running Linux.
     */
    public static final boolean IS_LINUX = osName.contains("linux"); //NOI18N

    /**
     * True if current platform is running Mac OS X.
     */
    public static final boolean IS_MAC = osName.contains("mac"); //NOI18N

    /**
     * True if current platform is running Windows.
     */
    public static final boolean IS_WINDOWS = osName.contains("windows"); //NOI18N
    
    /**
     * This URL is where you go when the user takes Scene Builder Help action (shortcut F1)
     */
    public static final String DOCUMENTATION_URL = "https://docs.oracle.com/javafx/index.html"; //NOI18N

    /**
     * Javadoc home (for Inspector and CSS Analyzer properties)
     */
    public final static String JAVADOC_HOME = "https://docs.oracle.com/javase/8/javafx/api/"; //NOI18N
    

    /**
     * Themes supported by Scene Builder Kit.
     */
    public enum Theme {

        MODENA,
        MODENA_TOUCH,
        MODENA_HIGH_CONTRAST_BLACK_ON_WHITE,
        MODENA_HIGH_CONTRAST_WHITE_ON_BLACK,
        MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK,
        MODENA_TOUCH_HIGH_CONTRAST_BLACK_ON_WHITE,
        MODENA_TOUCH_HIGH_CONTRAST_WHITE_ON_BLACK,
        MODENA_TOUCH_HIGH_CONTRAST_YELLOW_ON_BLACK,
        CASPIAN,
        CASPIAN_HIGH_CONTRAST,
        CASPIAN_EMBEDDED,
        CASPIAN_EMBEDDED_HIGH_CONTRAST,
        CASPIAN_EMBEDDED_QVGA,
        CASPIAN_EMBEDDED_QVGA_HIGH_CONTRAST
    }

    /**
     * Returns the url string for locating the specified stylesheet.
     * SB uses a set of CSS files aggregating several @import statements (see DTL-6799).
     *
     * @param theme theme for which string should be computed
     * @return string for locating the specified stylesheet.
     */
    public static String getThemeStylesheetURL(Theme theme) {
        final String result;

        switch (theme) {
            default:
                result = null;
                break;
            case MODENA:
                result = Deprecation.MODENA_STYLESHEET;
                break;
            case MODENA_TOUCH:
                result = Deprecation.MODENA_TOUCH_STYLESHEET;
                break;
            case MODENA_HIGH_CONTRAST_BLACK_ON_WHITE:
                result = Deprecation.MODENA_HIGHCONTRAST_BLACKONWHITE_STYLESHEET;
                break;
            case MODENA_HIGH_CONTRAST_WHITE_ON_BLACK:
                result = Deprecation.MODENA_HIGHCONTRAST_WHITEONBLACK_STYLESHEET;
                break;
            case MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK:
                result = Deprecation.MODENA_HIGHCONTRAST_YELLOWONBLACK_STYLESHEET;
                break;
            case MODENA_TOUCH_HIGH_CONTRAST_BLACK_ON_WHITE:
                result = Deprecation.MODENA_TOUCH_HIGHCONTRAST_BLACKONWHITE_STYLESHEET;
                break;
            case MODENA_TOUCH_HIGH_CONTRAST_WHITE_ON_BLACK:
                result = Deprecation.MODENA_TOUCH_HIGHCONTRAST_WHITEONBLACK_STYLESHEET;
                break;
            case MODENA_TOUCH_HIGH_CONTRAST_YELLOW_ON_BLACK:
                result = Deprecation.MODENA_TOUCH_HIGHCONTRAST_YELLOWONBLACK_STYLESHEET;
                break;
            case CASPIAN:
                result = Deprecation.CASPIAN_STYLESHEET;
                break;
            case CASPIAN_HIGH_CONTRAST:
                result = Deprecation.CASPIAN_HIGHCONTRAST_STYLESHEET;
                break;
            case CASPIAN_EMBEDDED:
                result = Deprecation.CASPIAN_EMBEDDED_STYLESHEET;
                break;
            case CASPIAN_EMBEDDED_HIGH_CONTRAST:
                result = Deprecation.CASPIAN_EMBEDDED_HIGHCONTRAST_STYLESHEET;
                break;
            case CASPIAN_EMBEDDED_QVGA:
                result = Deprecation.CASPIAN_EMBEDDED_QVGA_STYLESHEET;
                break;
            case CASPIAN_EMBEDDED_QVGA_HIGH_CONTRAST:
                result = Deprecation.CASPIAN_EMBEDDED_QVGA_HIGHCONTRAST_STYLESHEET;
                break;
        }
        
        if (!theme.equals(Theme.MODENA)) {
            assert result != null : "Missing logic for " + theme;
        }

        return result;
    }
    
    public static String getPlatformThemeStylesheetURL() {
        // Return USER_AGENT css, which is Modena for fx 8.0
        return Deprecation.MODENA_STYLESHEET;
    }
    
    public static boolean isModena(Theme theme) {
        return theme.toString().startsWith("MODENA");
    }
    
    public static boolean isModenaBlackonwhite(Theme theme) {
        return isModena(theme)
                && theme.toString().contains("BLACK_ON_WHITE");
    }
    
    public static boolean isModenaWhiteonblack(Theme theme) {
        return isModena(theme)
                && theme.toString().contains("WHITE_ON_BLACK");
    }
    
    public static boolean isModenaYellowonblack(Theme theme) {
        return isModena(theme)
                && theme.toString().contains("YELLOW_ON_BLACK");
    }
    
    public static boolean isModenaHighContrast(Theme theme) {
        return isModena(theme)
                && theme.toString().contains("HIGH_CONTRAST");
    }
    
    public static boolean isModenaTouch(Theme theme) {
        return isModena(theme)
                && theme.toString().contains("TOUCH");
    }
    
    public static boolean isModenaTouchHighContrast(Theme theme) {
        return isModena(theme)
                && theme.toString().contains("HIGH_CONTRAST")
                && theme.toString().contains("TOUCH");
    }
    
    public static boolean isCaspian(Theme theme) {
        return theme.toString().startsWith("CASPIAN");
    }

    /**
     * Requests the underlying platform to open a given file. On Linux, it runs
     * 'xdg-open'. On Mac, it runs 'open'. On Windows, it runs 'cmd /c start'.
     *
     * @param path path for the file to be opened
     * @throws IOException if an error occurs
     */
    public static void open(String path) throws IOException {
        List<String> args = new ArrayList<>();
        if (EditorPlatform.IS_MAC) {
            args.add("open"); //NOI18N
            args.add(path);
        } else if (EditorPlatform.IS_WINDOWS) {
            args.add("cmd"); //NOI18N
            args.add("/c"); //NOI18N
            args.add("start"); //NOI18N

            if (path.contains(" ")) { //NOI18N
                args.add("\"html\""); //NOI18N
            }

            args.add(path);
        } else if (EditorPlatform.IS_LINUX) {
            // xdg-open does fine on Ubuntu, which is a Debian.
            // I've no idea how it does with other Linux flavors.
            args.add("xdg-open"); //NOI18N
            args.add(path);
        }

        if (!args.isEmpty()) {
            executeDaemon(args, null);
        }
    }

    /**
     * Requests the underlying platform to "reveal" the specified folder. On
     * Linux, it runs 'nautilus'. On Mac, it runs 'open'. On Windows, it runs
     * 'explorer /select'.
     *
     * @param filePath path for the folder to be revealed
     * @throws IOException if an error occurs
     */
    public static void revealInFileBrowser(File filePath) throws IOException {
        List<String> args = new ArrayList<>();
        String path = filePath.toURI().toURL().toExternalForm();
        if (EditorPlatform.IS_MAC) {
            args.add("open"); //NOI18N
            args.add("-R"); //NOI18N
            args.add(path);
        } else if (EditorPlatform.IS_WINDOWS) {
            args.add("explorer"); //NOI18N
            args.add("/select," + path); //NOI18N
        } else if (EditorPlatform.IS_LINUX) {
            // nautilus does fine on Ubuntu, which is a Debian.
            // I've no idea how it does with other Linux flavors.
            args.add("nautilus"); //NOI18N
            // The nautilus that comes with Ubuntu up to 11.04 included doesn't
            // take a file path as parameter (you get an error popup), you must
            // provide a dir path.
            // Starting with Ubuntu 11.10 (the first based on kernel 3.x) a
            // file path is well managed.
            int osVersionNumerical = Integer.parseInt(System.getProperty("os.version").substring(0, 1)); //NOI18N
            if (osVersionNumerical < 3) {
                // Case Ubuntu 10.04 to 11.04: What you provide to nautilus is
                // the name of the directory containing the file you want to see
                // listed. See DTL-5384.
                path = filePath.getAbsoluteFile().getParent();
                if (path == null) {
                    path = "."; //NOI18N
                }
            }
            args.add(path);
        } else {
            // Not Supported
        }

        if (!args.isEmpty()) {
            executeDaemon(args, null);
        }
    }

    /**
     * Returns true if the modifier key for continuous selection is down.
     *
     * @param e mouse event to check (never null)
     * @return true if the modifier key for continuous selection is down.
     */
    public static boolean isContinuousSelectKeyDown(MouseEvent e) {
        return e.isShiftDown();
    }

    /**
     * Returns true if the modifier key for non-continuous selection is down.
     *
     * @param e mouse event to check (never null).
     * @return true if the modifier key for non-continuous selection is down.
     */
    public static boolean isNonContinousSelectKeyDown(MouseEvent e) {
        return IS_MAC ? e.isMetaDown(): e.isControlDown();
    }

    /**
     * Returns true if the jvm is running with assertions enabled.
     *
     * @return true if the jvm is running with assertions enabled.
     */
    public static boolean isAssertionEnabled() {
        return EditorPlatform.class.desiredAssertionStatus();
    }

    /*
     * Private
     */
    private static void executeDaemon(List<String> cmd, File wDir) throws IOException {
        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder = builder.directory(wDir);
            builder.start();
        } catch (RuntimeException ex) {
            throw new IOException(ex);
        }
    }

}
