/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

class IDWriteFactory extends IUnknown {
    IDWriteFactory(long ptr) {
        super(ptr);
    }

    IDWriteFontCollection GetSystemFontCollection(boolean checkForUpdates) {
        long result = OS.GetSystemFontCollection(ptr, checkForUpdates);
        return result != 0 ? new IDWriteFontCollection(result) : null;
    }

    IDWriteTextAnalyzer CreateTextAnalyzer() {
        long result = OS.CreateTextAnalyzer(ptr);
        return result != 0 ? new IDWriteTextAnalyzer(result) : null;
    }

    IDWriteTextFormat CreateTextFormat(String fontFamily,
                                       IDWriteFontCollection fontCollection,
                                       int fontWeight,
                                       int fontStyle,
                                       int fontStretch,
                                       float fontSize,
                                       String localeName) {
        long result = OS.CreateTextFormat(ptr,
                                          (fontFamily+'\0').toCharArray(),
                                          fontCollection.ptr,
                                          fontWeight,
                                          fontStyle,
                                          fontStretch,
                                          fontSize,
                                          (localeName+'\0').toCharArray());
        return result != 0 ? new IDWriteTextFormat(result) : null;
    }

    IDWriteTextLayout CreateTextLayout(char[] text,
                                       int stringStart,
                                       int stringLength,
                                       IDWriteTextFormat textFormat,
                                       float maxWidth,
                                       float maxHeight) {
        long result = OS.CreateTextLayout(ptr,
                                          text,
                                          stringStart,
                                          stringLength,
                                          textFormat.ptr,
                                          maxWidth,
                                          maxHeight);
        return result != 0 ? new IDWriteTextLayout(result) : null;
    }

    IDWriteGlyphRunAnalysis CreateGlyphRunAnalysis(DWRITE_GLYPH_RUN glyphRun,
                                                   float pixelsPerDip,
                                                   DWRITE_MATRIX transform,
                                                   int renderingMode,
                                                   int measuringMode,
                                                   float baselineOriginX,
                                                   float baselineOriginY) {
        long result = OS.CreateGlyphRunAnalysis(ptr,
                                                glyphRun,
                                                pixelsPerDip,
                                                transform,
                                                renderingMode,
                                                measuringMode,
                                                baselineOriginX,
                                                baselineOriginY);
        return result != 0 ? new IDWriteGlyphRunAnalysis(result) : null;
    }

    IDWriteFontFile CreateFontFileReference(String filePath) {
        long result = OS.CreateFontFileReference(ptr, (filePath+'\0').toCharArray());
        return result != 0 ? new IDWriteFontFile(result) : null;
    }

    IDWriteFontFace CreateFontFace(int fontFaceType,
                                   IDWriteFontFile fontFiles,
                                   int faceIndex,
                                   int fontFaceSimulationFlags) {

        long result = OS.CreateFontFace(ptr, fontFaceType, fontFiles.ptr, faceIndex, fontFaceSimulationFlags);
        return result != 0 ? new IDWriteFontFace(result) : null;
    }
}
