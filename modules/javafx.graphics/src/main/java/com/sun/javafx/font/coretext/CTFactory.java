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

package com.sun.javafx.font.coretext;

import java.util.ArrayList;

import com.sun.javafx.font.FallbackResource;
import com.sun.javafx.font.FontFallbackInfo;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontFile;
import com.sun.javafx.text.GlyphLayout;

public class CTFactory extends PrismFontFactory {

    public static PrismFontFactory getFactory() {
        return new CTFactory();
    }

    private CTFactory() {
    }

    /*
     * This is needed for fonts where the name starts with "."
     * It isn't just the ".SF" "System Font", but related fallbacks which
     * layout returns and we need to add as a native fallback to a composite font.
     */
    CTFontFile createFontFile(String name, long fontRef) throws Exception {
        String filename = OS.CTFontCopyURLAttribute(fontRef);
        int fIndex = findFontIndex(name, filename);

        if (debugFonts) {
            System.err.println("createFontFile by ref name="+name + " filename="+filename+" index="+fIndex);
        }
        if (fIndex == -1) {
            return null;
        }
        CTFontFile font = new CTFontFile(name, filename, fIndex, fontRef);
        // Store the lookup name in the map.
        // This should prevent us needing to create new instances.
        storeInMap(name, font);
        return font;
    }

    @Override
    protected PrismFontFile createFontFile(String name, String filename,
            int fIndex, boolean register, boolean embedded, boolean copy,
            boolean tracked) throws Exception {
        return new CTFontFile(name, filename, fIndex, register,
                              embedded, copy, tracked);
    }

    @Override
    public GlyphLayout createGlyphLayout() {
        return new CTGlyphLayout();
    }

    @Override
    protected boolean registerEmbeddedFont(String path) {
        boolean result = CTFontFile.registerFont(path);
        if (debugFonts) {
            if (result) {
                System.err.println("[CoreText] Font registration succeeded:" + path);
            } else {
                System.err.println("[CoreText] Font registration failed:" + path);
            }
        }
        return result;
    }

    /*
     * This should prevent enumeration of special fonts, but also should
     * be used to prevent public lookup of these even if not enumerated
     */
    @Override
    public boolean isExcluded(String name) {
         return name.startsWith(".") || name.startsWith("System Font");
    }

    public FontFallbackInfo getFallbacks(FontResource primaryResource) {
        FontFallbackInfo info = new FontFallbackInfo();
        ArrayList<String> names;
        ArrayList<String> files;
        ArrayList<String> fonts;

        // First add cascading font list.
        if (primaryResource instanceof CTFontFile ctff) {
           ctff.getCascadingInfo(info);
        }
        // ELSE: REMIND we can get the default cascading list for this case.
        // Do this only if we find we end up here, which I think unlikely today.

        String name = "System Regular"; // a default
        boolean bold = false;
        if (primaryResource != null) {
            name = primaryResource.getFullName();
            bold = primaryResource.isBold();
        }

        // The "." fonts for Japanese, Korean, Simplified Chinese, HK Chinese, TW Chinese
        // do not report a file, so out of caution, let's add some known fallbacks used
        // by Arial and Arial Bold for these scripts.
        // Note: even if "." fonts do report a file, macOS does not like apps looking
        // them up that way.
        if (name.startsWith("System ")) {
           if (!bold) {
               info.add("Hiragino Sans W3", "/System/Library/Fonts/ヒラギノ角ゴシック W3.ttc", null);
               info.add("Hiragino Sans GB W3", "/System/Library/Fonts/Hiragino Sans GB.ttc", null);
               info.add("Apple SD Gothic Neo Regular", "/System/Library/Fonts/AppleSDGothicNeo.ttc", null);
               info.add("PingFang SC Regular", "/System/Library/Fonts/PingFang.ttc", null);
               info.add("PingFang TC Regular", "/System/Library/Fonts/PingFang.ttc", null);
               info.add("PingFang HK Regular", "/System/Library/Fonts/PingFang.ttc", null);
           } else {
               info.add("Hiragino Sans W6", "/System/Library/Fonts/ヒラギノ角ゴシック W6.ttc", null);
               info.add("Hiragino Sans GB W6", "System/Library/Fonts/Hiragino Sans GB.ttc", null);
               info.add("Apple SD Gothic Neo Bold", "/System/Library/Fonts/AppleSDGothicNeo.ttc", null);
               info.add("PingFang SC Semibold", "/System/Library/Fonts/PingFang.ttc", null);
               info.add("PingFang TC Semibold", "/System/Library/Fonts/PingFang.ttc", null);
               info.add("PingFang HK Semibold", "/System/Library/Fonts/PingFang.ttc", null);
           }
        }

        // Now add hardwired fall backs in case of problems with the above.
        info.add("Apple Symbols", "/System/Library/Fonts/Apple Symbols.ttf", null);
        info.add("Apple Color Emoji", "/System/Library/Fonts/Apple Color Emoji.ttc", null);
        info.add("Arial Unicode MS", "/Library/Fonts/Arial Unicode.ttf", null);
        // Add CJK Ext B supplementary characters.
        info.add("Heiti SC Light", "/System/Library/Fonts/STHeiti Light.ttc", null);

        return info;
    }
}
