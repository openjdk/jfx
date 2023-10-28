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

import com.sun.javafx.font.Disposer;
import com.sun.javafx.font.DisposerRecord;
import com.sun.javafx.font.FontFallbackInfo;
import com.sun.javafx.font.FontStrikeDesc;
import com.sun.javafx.font.MacFontFinder;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontFile;
import com.sun.javafx.font.PrismFontStrike;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.transform.BaseTransform;

class CTFontFile extends PrismFontFile {

    private long cgFontRef = 0;
    /* Transform used for outline and bounds */
    private final static CGAffineTransform tx = new CGAffineTransform();
    static {
        tx.a = 1;   /* scale x */
        tx.d = -1;  /* scale y */
    }

    private static class SelfDisposerRecord implements DisposerRecord {
        private long fontRef;

        SelfDisposerRecord(long fontRef) {
            this.fontRef = fontRef;
        }

        @Override
        public synchronized void dispose() {
            if (fontRef != 0) {
                OS.CFRelease(fontRef);
                fontRef = 0;
            }
        }
    }

    private long ctFontRef = 0;
    CTFontFile(String name, String filename, int fIndex, long fontRef) throws Exception {
        super(name, filename, fIndex, false, false, false, false);

        if (fontRef == 0) {
           throw new InternalError("Zero fontref");
        }
        ctFontRef = fontRef;
        Disposer.addRecord(this, new SelfDisposerRecord(ctFontRef));
    }

    CTFontFile(String name, String filename, int fIndex, boolean register,
               boolean embedded, boolean copy, boolean tracked) throws Exception {
        super(name, filename, fIndex, register, embedded, copy, tracked);

        // The super-class code that opens and reads the font can't handle font variations,
        // as used by the macOS "System Font"
        // So when we see a font from this family, we need to use the original name
        // passed in here as the full name.
        // This isn't robust against Apple changing the name of the font
        // but I think it sufficient until we add support for font variations.

        if (name != null) {
            String family = getFamilyName();
            if (family.equals("System Font")) {
                fullName = name;
            }
        }
        if (embedded) {
            cgFontRef = createCGFontForEmbeddedFont();
            Disposer.addRecord(this, new SelfDisposerRecord(cgFontRef));
        } else {
            cgFontRef = 0;
        }
    }

    @Override
    public boolean isBold() {
        // Need to do this until we add font variation support into the super-class
        return fullName.equals("System Font Bold") || super.isBold();
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
                long dataProvider = OS.CGDataProviderCreateWithURL(url);
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

   @Override protected float getAdvanceFromPlatform(int glyphCode, float ptSize) {
      CTFontStrike strike =
          (CTFontStrike)getStrike(ptSize, BaseTransform.IDENTITY_TRANSFORM);
      long fontRef = strike.getFontRef();
      int orientation = OS.kCTFontOrientationDefault;
      CGSize size = new CGSize();
      return (float)OS.CTFontGetAdvancesForGlyphs(fontRef, orientation, (short)glyphCode, size);
   }

   @Override protected int[] createGlyphBoundingBox(int gc) {
        /*
         * This is being done at size 12 so that the font can cache
         * bounds and scale to the required point size. But if the
         * bounds do not scale linearly this will fail badly
         */
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
         * However for color (emoji) glyphs this returns the wrong bounds,
         * so use CTFontGetBoundingRectsForGlyphs anyway.
         * In case it fails, or the font doesn't have a glyph table
         * (CFF fonts), then the bounds of the glyph outline is used instead.
         */
        if (!isCFF()) {
            if (isColorGlyph(gc)) {
                CGRect rect = OS.CTFontGetBoundingRectForGlyphs(fontRef, (short)gc);
                float scale = getUnitsPerEm() / size;
                bb[0] = (int)(Math.round(rect.origin.x * scale));
                bb[1] = (int)(Math.round(rect.origin.y * scale));
                bb[2] = (int)(Math.round((rect.origin.x + rect.size.width) * scale));
                bb[3] = (int)(Math.round((rect.origin.y + rect.size.height) * scale));
                return bb;
            } else {
                short format = getIndexToLocFormat();
                if (OS.CTFontGetBoundingRectForGlyphUsingTables(fontRef, (short)gc, format, bb)) {
                    return bb;
                }
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

    long getFontRef(float size, CGAffineTransform matrix) {
      long retRef = 0;
      if (isEmbeddedFont()) {
          if (cgFontRef != 0) {
             retRef = OS.CTFontCreateWithGraphicsFont(cgFontRef, size, matrix, 0);
          }
      } else if (ctFontRef != 0) {
           retRef = OS.CTFontCreateCopyWithAttributes(ctFontRef, size, matrix, 0);
      } else {
          String psName = getPSName();
          if (psName.startsWith(".")) {
               boolean bold = getFullName().indexOf("Bold") > 0;
               retRef = OS.CTFontCreateUIFontForLanguage(size, matrix, bold);
          } else {
              final long psNameRef = OS.CFStringCreate(psName);
              if (psNameRef != 0) {
                  retRef = OS.CTFontCreateWithName(psNameRef, size, matrix);
                  OS.CFRelease(psNameRef);
              }
           }
       }
        return retRef;
    }

    void getCascadingInfo(FontFallbackInfo info) {
        CTFactory factory = (CTFactory)PrismFontFactory.getFontFactory();
        long ref = getFontRef(0f, null);
        String[] stringInfo = MacFontFinder.getCascadeList(ref);
        // stringInfo is displayname and file.
        // if there's a null file, skip
        // if there's a non-null file but name starts with ".",
        // we'd need a fontRef to use it since macOS won't let us create it.
        // So we need to skip that case too - unless/until I find an answer to that
        // in which case we could use Factory.createFontFile(name, ref); and
        // pass a font instead of null.
        if (PrismFontFactory.debugFonts) {
            System.err.println("Cascading list for " + getFullName());
        }
        for (int i=0; i<stringInfo.length; i+=2) {
            String name = stringInfo[i];
            String file = stringInfo[i+1];
            if (PrismFontFactory.debugFonts) {
                System.err.print("Entry : name=" + name + " file="+file);
            }
            if (file == null || name.startsWith(".")) {
                if (PrismFontFactory.debugFonts) {
                    System.err.println(" - *** not using this entry (.font and/or null file)");
                }
                continue;
            }
            if (PrismFontFactory.debugFonts) {
                System.err.println();
            }
            info.add(name, file, null);
        }
        if (PrismFontFactory.debugFonts) {
             System.err.println("End cascading list");
        }
    }
}
