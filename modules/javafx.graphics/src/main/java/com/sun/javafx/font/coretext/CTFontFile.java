/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.font.Disposer;
import com.sun.javafx.font.DisposerRecord;
import com.sun.javafx.font.FontStrikeDesc;
import com.sun.javafx.font.PrismFontFile;
import com.sun.javafx.font.PrismFontStrike;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.transform.BaseTransform;

class CTFontFile extends PrismFontFile {

    private final long cgFontRef;
    /* Transform used for outline and bounds */
    private final static CGAffineTransform tx = new CGAffineTransform();
    static {
        tx.a = 1;   /* scale x */
        tx.d = -1;  /* scale y */
    }

    private static class SelfDisposerRecord implements DisposerRecord {
        private long cgFontRef;

        SelfDisposerRecord(long cgFontRef) {
            this.cgFontRef = cgFontRef;
        }

        @Override
        public synchronized void dispose() {
            if (cgFontRef != 0) {
                OS.CFRelease(cgFontRef);
                cgFontRef = 0;
            }
        }
    }

    CTFontFile(String name, String filename, int fIndex, boolean register,
               boolean embedded, boolean copy, boolean tracked) throws Exception {
        super(name, filename, fIndex, register, embedded, copy, tracked);

        if (embedded) {
            cgFontRef = createCGFontForEmbeddedFont();
            Disposer.addRecord(this, new SelfDisposerRecord(cgFontRef));
        } else {
            cgFontRef = 0;
        }
    }

    public static boolean registerFont(String fontfile) {
        if (fontfile == null) return false;
        long alloc = OS.kCFAllocatorDefault();
        boolean result = false;
        long fileRef = OS.CFStringCreate(fontfile);
        if (fileRef != 0) {
            int pathStyle = OS.kCFURLPOSIXPathStyle;
            long urlRef = OS.CFURLCreateWithFileSystemPath(alloc, fileRef, pathStyle, false);
            if (urlRef != 0) {
                int scope = OS.kCTFontManagerScopeProcess;
                result = OS.CTFontManagerRegisterFontsForURL(urlRef, scope, 0);
                OS.CFRelease(urlRef);
            }
            OS.CFRelease(fileRef);
        }
        return result;
    }

    private long createCGFontForEmbeddedFont() {
        long cgFontRef = 0;
        final long fileNameRef = OS.CFStringCreate(getFileName());
        if (fileNameRef != 0) {
            final long url = OS.CFURLCreateWithFileSystemPath(
                    OS.kCFAllocatorDefault(), fileNameRef,
                    OS.kCFURLPOSIXPathStyle, false);
            if (url != 0) {
                final long dataProvider = OS.CGDataProviderCreateWithURL(url);
                if (dataProvider != 0) {
                    cgFontRef = OS.CGFontCreateWithDataProvider(dataProvider);
                    OS.CFRelease(dataProvider);
                }
                OS.CFRelease(url);
            }
            OS.CFRelease(fileNameRef);
        }
        return cgFontRef;
    }

    long getCGFontRef() {
        return cgFontRef;
    }

    CGRect getBBox(int gc, float size) {
        CTFontStrike strike = (CTFontStrike)getStrike(size, BaseTransform.IDENTITY_TRANSFORM);
        long fontRef = strike.getFontRef();
        if (fontRef == 0) return null;
        long pathRef = OS.CTFontCreatePathForGlyph(fontRef, (short)gc, tx);
        if (pathRef == 0) return null;
        CGRect rect = OS.CGPathGetPathBoundingBox(pathRef);
        OS.CGPathRelease(pathRef);
        return rect;
    }

    Path2D getGlyphOutline(int gc, float size) {
        CTFontStrike strike = (CTFontStrike)getStrike(size, BaseTransform.IDENTITY_TRANSFORM);
        long fontRef = strike.getFontRef();
        if (fontRef == 0) return null;
        long pathRef = OS.CTFontCreatePathForGlyph(fontRef, (short)gc, tx);
        if (pathRef == 0) return null;
        Path2D path = OS.CGPathApply(pathRef);
        OS.CGPathRelease(pathRef);
        return path;
    }

    @Override protected int[] createGlyphBoundingBox(int gc) {
        float size = 12;
        CTFontStrike strike = (CTFontStrike)getStrike(size,
                                                      BaseTransform.IDENTITY_TRANSFORM);

        long fontRef = strike.getFontRef();
        if (fontRef == 0) return null;
        int[] bb = new int[4];

        /* For some reason CTFontGetBoundingRectsForGlyphs has poor performance.
         * The fix is to use the 'loca' and the 'glyf' tables to determine
         * the glyph bounding box (same as T2K). This implementation
         * uses native code to read these tables since they can be large.
         * In case it fails, or the font doesn't have a glyph table
         * (CFF fonts), then the bounds of the glyph outline is used instead.
         */
        if (!isCFF()) {
            short format = getIndexToLocFormat();
            if (OS.CTFontGetBoundingRectForGlyphUsingTables(fontRef, (short)gc, format, bb)) {
                return bb;
            }
        }
        /* Note: not using tx here as the bounds need to be y up */
        long pathRef = OS.CTFontCreatePathForGlyph(fontRef, (short)gc, null);
        if (pathRef == 0) return null;
        CGRect rect = OS.CGPathGetPathBoundingBox(pathRef);
        OS.CGPathRelease(pathRef);
        float scale = getUnitsPerEm() / size;
        bb[0] = (int)(Math.round(rect.origin.x * scale));
        bb[1] = (int)(Math.round(rect.origin.y * scale));
        bb[2] = (int)(Math.round((rect.origin.x + rect.size.width) * scale));
        bb[3] = (int)(Math.round((rect.origin.y + rect.size.height) * scale));
        return bb;
    }

    @Override
    protected PrismFontStrike<CTFontFile> createStrike(float size,
            BaseTransform transform, int aaMode, FontStrikeDesc desc) {
        return new CTFontStrike(this, size, transform, aaMode, desc);
    }

}
