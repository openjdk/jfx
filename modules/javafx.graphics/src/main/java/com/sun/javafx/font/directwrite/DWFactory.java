/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.directwrite;

import com.sun.javafx.font.FontFallbackInfo;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontFile;
import com.sun.javafx.text.GlyphLayout;
import com.sun.prism.GraphicsPipeline;

public class DWFactory extends PrismFontFactory {

    /* Factories (Singletons) */
    private static IDWriteFactory DWRITE_FACTORY = null;
    private static IDWriteFontCollection FONT_COLLECTION = null;
    private static IWICImagingFactory WIC_FACTORY = null;
    private static ID2D1Factory D2D_FACTORY = null;
    private static Thread d2dThread;

    public static PrismFontFactory getFactory() {
        /* DirectWrite is not available on Windows Vista SP2 (JFX minimal
         * requirement on Windows) without 'Platform Update'.
         * To workaround this limitation this method first checks if a
         * IDWriteFactory can be created. The DWriteCreateFactory is a dynamic
         * method which fails gracefully (returns NULL) when the system does
         * not support DirectWrite.
         */
        if (getDWriteFactory() == null) {
            /* Returning null here indicates to the PrismFontFactory
             * to throw an Error . */
            return null;
        }
        return new DWFactory();
    }

    private DWFactory() {
    }

    @Override
    protected PrismFontFile createFontFile(String name, String filename,
                                           int fIndex, boolean register,
                                           boolean embedded, boolean copy,
                                           boolean tracked) throws Exception {
        return new DWFontFile(name, filename, fIndex, register,
                              embedded, copy, tracked);
    }

    @Override public GlyphLayout createGlyphLayout() {
        return new DWGlyphLayout();
    }

    @Override
    protected boolean registerEmbeddedFont(String path) {
        IDWriteFactory factory = DWFactory.getDWriteFactory();
        IDWriteFontFile fontFile = factory.CreateFontFileReference(path);
        if (fontFile == null) return false;
        boolean[] isSupportedFontType = new boolean[1];
        int[] fontFileType = new int[1];
        int[] fontFaceType = new int[1];
        int[] numberOfFaces = new int[1];
        int hr = fontFile.Analyze(isSupportedFontType, fontFileType, fontFaceType, numberOfFaces);
        fontFile.Release();
        if (hr != OS.S_OK) return false;
        return isSupportedFontType[0];
    }

    static IDWriteFactory getDWriteFactory() {
        /* Using multi threaded DWrite factory as the JFX thread requires access
         * to DWrite resources for measuring and the Prism thread for rendering */
        if (DWRITE_FACTORY == null) {
            DWRITE_FACTORY = OS.DWriteCreateFactory(OS.DWRITE_FACTORY_TYPE_SHARED);
        }
        return DWRITE_FACTORY;
    }

    static IDWriteFontCollection getFontCollection() {
        if (FONT_COLLECTION == null) {
            FONT_COLLECTION = getDWriteFactory().GetSystemFontCollection(false);
        }
        return FONT_COLLECTION;
    }

    private static void checkThread() {
        /* Note: It is possible for the correct thread to acquire the factory and
         * hand it over to some other thread. This would be a programming error
         * and it is not check by this implementation. */
        Thread current = Thread.currentThread();
        if (d2dThread == null) {
            d2dThread = current;
        }
        if (d2dThread != current) {
            throw new IllegalStateException(
                    "This operation is not permitted on the current thread ["
                    + current.getName() + "]");
        }
    }

    static synchronized IWICImagingFactory getWICFactory() {
        checkThread();
        /* Using single threaded WIC Factory as it should only be used by the rendering thread */
        if (WIC_FACTORY == null) {
            /* Initialize COM in order to create a WICImagingFactory.
             * It runs on the prism thread and expects no other code in this thread
             * to interface with COM. */
            if (!OS.CoInitializeEx(OS.COINIT_APARTMENTTHREADED | OS.COINIT_DISABLE_OLE1DDE)) {
                return null;
            }

            WIC_FACTORY = OS.WICCreateImagingFactory();
            if (WIC_FACTORY == null) {
                return null;
            }

            GraphicsPipeline.getPipeline().addDisposeHook(() -> {
                checkThread();
                WIC_FACTORY.Release();
                OS.CoUninitialize();
                WIC_FACTORY = null;
            });
        }
        return WIC_FACTORY;
    }

    static synchronized ID2D1Factory getD2DFactory() {
        checkThread();
        /* Using single threaded D2D Factory as it should only be used by the rendering thread */
        if (D2D_FACTORY == null) {
            D2D_FACTORY = OS.D2D1CreateFactory(OS.D2D1_FACTORY_TYPE_SINGLE_THREADED);
        }
        return D2D_FACTORY;
    }

    private static native String regReadFontLink(String searchfont);
    private static native String getEUDCFontFile();

    /*
     * Ignoring the primary on Windows - this should change some day.
     */
    public FontFallbackInfo getFallbacks(FontResource primaryResource) {
        FontFallbackInfo info = new FontFallbackInfo();

        String fontRegBuf = regReadFontLink("Tahoma");
        if (fontRegBuf != null && fontRegBuf.length() > 0) {
            // split registry data into null terminated strings
            String[] fontRegList = fontRegBuf.split("\u0000");
            int linkListLen = fontRegList.length;
            for (int i=0; i < linkListLen; i++) {
                String[] splitFontData = fontRegList[i].split(",");
                int len = splitFontData.length;
                String file = getPathNameWindows(splitFontData[0]);
                String name = (len > 1) ? splitFontData[1] : null;
                if (name != null && info.containsName(name)) {
                    continue;
                } else if (name == null && info.containsFile(file)) {
                    continue;
                }
                info.add(name, file, null);
            }
        }

        String eudcFontFile = getEUDCFontFile();
        if (eudcFontFile != null) {
            info.add(null, eudcFontFile, null);
        }

        // CJK Ext B Supplementary character fallbacks.
        info.add("MingLiU-ExtB", getPathNameWindows("mingliub.ttc"), null);
        info.add("Segoe UI Symbol", getPathNameWindows("seguisym.ttf"), null);
        // TBD add way more here.

        return info;
    }
}
