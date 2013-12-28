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
import java.net.URL;
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
    public static final String DOCUMENTATION_URL = "http://docs.oracle.com/javafx/index.html"; //NOI18N

    /**
     * Themes supported by Scene Builder Kit.
     */
    public enum Theme {

        MODENA,
        MODENA_TOUCH,
        CASPIAN,
        CASPIAN_HIGH_CONTRAST,
        CASPIAN_EMBEDDED,
        CASPIAN_EMBEDDED_HIGH_CONTRAST,
        CASPIAN_EMBEDDED_QVGA,
        CASPIAN_EMBEDDED_QVGA_HIGH_CONTRAST
    }

    private final static URL caspianThemeUrl = Deprecation.getCaspianStylesheetURL();
    private final static URL caspianHighContrastThemeUrl = Deprecation.getCaspianHighContrastStylesheetURL();
    private final static URL caspianEmbeddedThemeUrl = Deprecation.getCaspianEmbeddedStylesheetURL();
    private final static URL caspianEmbeddedQVGAThemeUrl = Deprecation.getCaspianEmbeddedQVGAStylesheetURL();
    private final static URL modenaThemeUrl = Deprecation.getModenaStylesheetURL();
    private final static URL modenaTouchThemeUrl = Deprecation.getModenaTouchStylesheetURL();

    /**
     * Returns the list of url for locating the specified set of stylesheet in jfxrt.jar.
     *
     * @param theme theme for which list of url should be computed
     * @return list of url for locating the specified stylesheet.
     */
    public static List<URL> getThemeStylesheetURLs(Theme theme) {
        final List<URL> result = new ArrayList<>();

        switch (theme) {
            default:
                break;
            case MODENA:
                result.add(modenaThemeUrl);
                break;
            case MODENA_TOUCH:
                result.add(modenaThemeUrl);
                result.add(modenaTouchThemeUrl);
                break;
            case CASPIAN:
                result.add(caspianThemeUrl);
                break;
            case CASPIAN_HIGH_CONTRAST:
                result.add(caspianThemeUrl);
                result.add(caspianHighContrastThemeUrl);
                break;
            case CASPIAN_EMBEDDED:
                result.add(caspianThemeUrl);
                result.add(caspianEmbeddedThemeUrl);
                break;
            case CASPIAN_EMBEDDED_HIGH_CONTRAST:
                result.add(caspianThemeUrl);
                result.add(caspianEmbeddedThemeUrl);
                result.add(caspianHighContrastThemeUrl);
                break;
            case CASPIAN_EMBEDDED_QVGA:
                result.add(caspianThemeUrl);
                result.add(caspianEmbeddedThemeUrl);
                result.add(caspianEmbeddedQVGAThemeUrl);
                break;
            case CASPIAN_EMBEDDED_QVGA_HIGH_CONTRAST:
                result.add(caspianThemeUrl);
                result.add(caspianEmbeddedThemeUrl);
                result.add(caspianEmbeddedQVGAThemeUrl);
                result.add(caspianHighContrastThemeUrl);
                break;
        }
        assert !result.isEmpty() : "Missing logic for " + theme;

        return result;
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

            if (path.indexOf(" ") != -1) { //NOI18N
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
        String path = filePath.getCanonicalPath();
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
        return IS_WINDOWS ? e.isControlDown() : e.isMetaDown();
    }

    /**
     * Returns true if the jvm is running with assertions enabled.
     *
     * @return true if the jvm is running with assertions enabled.
     */
    public static boolean isAssertionEnabled() {
        boolean result = false;
        assert result = true;
        return result;
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
