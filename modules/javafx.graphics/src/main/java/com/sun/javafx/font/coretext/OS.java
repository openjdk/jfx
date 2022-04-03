/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.geom.Path2D;

class OS {
    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            NativeLibLoader.loadLibrary("javafx_font");
            return null;
        });
    }

    static final int kCFURLPOSIXPathStyle = 0;
    static final int kCTFontOrientationDefault = 0;
    static final int kCTFontManagerScopeProcess = 1;
    static final int kCGBitmapByteOrder32Big = 4 << 12;
    static final int kCGBitmapByteOrder32Little = 2 << 12;
    static final int kCGBitmapByteOrder32Host = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? kCGBitmapByteOrder32Little : kCGBitmapByteOrder32Big;
    static final int kCGImageAlphaPremultipliedFirst = 2;
    static final int kCGImageAlphaNone = 0;
    static final int kCTWritingDirectionRightToLeft = 1;

    static final long CFStringCreate(String string) {
        char[] buffer = string.toCharArray();
        long alloc = kCFAllocatorDefault();
        return CFStringCreateWithCharacters(alloc, buffer, buffer.length);
    }

    /* Custom */
    static final native byte[] CGBitmapContextGetData(long c, int width, int height, int bpp);
    static final native void CGRectApplyAffineTransform(CGRect rect, CGAffineTransform t);
    static final native Path2D CGPathApply(long path);
    static final native CGRect CGPathGetPathBoundingBox(long path);
    static final native long CFStringCreateWithCharacters(long alloc, char[] chars, long start, long numChars);
    static final native String CTFontCopyAttributeDisplayName(long font);
    static final native void CTFontDrawGlyphs(long font, short glyphs, double x, double y, long context);
    static final native double CTFontGetAdvancesForGlyphs(long font, int orientation, short glyphs, CGSize advances);
    static final native boolean CTFontGetBoundingRectForGlyphUsingTables(long font, short glyphs, short format, int[] retArr);
    static final native int CTRunGetGlyphs(long run, int slotMask, int start, int[] buffer);
    static final native int CTRunGetStringIndices(long run, int start, int[] buffer);
    static final native int CTRunGetPositions(long run, int start, float[] buffer);

    /* one to one */
    static final native long kCFAllocatorDefault();
    static final native long kCFTypeDictionaryKeyCallBacks();
    static final native long kCFTypeDictionaryValueCallBacks();
    static final native long kCTFontAttributeName();
    static final native long kCTParagraphStyleAttributeName();
    static final native long CFArrayGetCount(long theArray);
    static final native long CFArrayGetValueAtIndex(long theArray, long idx);
    static final native long CFAttributedStringCreate(long alloc, long str, long attributes);
    static final native void CFDictionaryAddValue(long theDict, long key, long value);
    static final native long CFDictionaryCreateMutable(long allocator, long capacity, long keyCallBacks, long valueCallBacks);
    static final native long CFDictionaryGetValue(long theDict, long key);
    static final native void CFRelease(long cf);
    static final native long CFStringCreateWithCharacters(long alloc, char[] chars, long numChars);
    static final native long CFURLCreateWithFileSystemPath(long allocator, long filePath, long pathStyle, boolean isDirectory);
    static final native long CGBitmapContextCreate(long data, long width, long height, long bitsPerComponent, long bytesPerRow, long colorspace, int bitmapInfo);
    static final native void CGContextFillRect(long context, CGRect rect);
    static final native void CGContextRelease(long context);
    static final native void CGContextSetAllowsFontSmoothing(long context, boolean allowsFontSmoothing);
    static final native void CGContextSetAllowsAntialiasing(long context, boolean allowsAntialiasing);
    static final native void CGContextSetAllowsFontSubpixelPositioning(long context, boolean allowsFontSubpixelPositioning);
    static final native void CGContextSetAllowsFontSubpixelQuantization(long context, boolean allowsFontSubpixelQuantization);
    static final native void CGContextSetRGBFillColor(long context, double red, double green, double blue, double alpha);
    static final native void CGContextTranslateCTM(long context, double tx, double ty);
    static final native long CGColorSpaceCreateDeviceGray();
    static final native long CGColorSpaceCreateDeviceRGB();
    static final native void CGColorSpaceRelease(long space);
    static final native long CGDataProviderCreateWithURL(long cfURL);
    static final native long CGFontCreateWithDataProvider(long dataProvider);
    static final native void CGPathRelease(long path);
    static final native long CTFontCreatePathForGlyph(long font, short glyph, CGAffineTransform matrix);
    static final native long CTFontCreateWithGraphicsFont(long cgFont, double size, CGAffineTransform matrix, long attributes);
    static final native long CTFontCreateWithName(long name, double size, CGAffineTransform matrix);
    static final native boolean CTFontManagerRegisterFontsForURL(long fontURL, int scope, long error);
    static final native long CTLineCreateWithAttributedString(long string);
    static final native long CTLineGetGlyphRuns(long line);
    static final native long CTLineGetGlyphCount(long line);
    static final native double CTLineGetTypographicBounds(long line);
    static final native long CTRunGetGlyphCount(long run);
    static final native long CTRunGetAttributes(long run);
    static final native long CTParagraphStyleCreate(int dir);

}
