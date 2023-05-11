/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;

public abstract class PrismFontStrike<T extends PrismFontFile> implements FontStrike {
    private DisposerRecord disposer;
    private T fontResource;
    private Map<Integer,Glyph> glyphMap = new HashMap<>();
    private PrismMetrics metrics;
    protected boolean drawShapes = false;
    private float size;
    private BaseTransform transform;
    private int aaMode;
    private FontStrikeDesc desc;

    protected PrismFontStrike(T fontResource,
                              float size, BaseTransform tx, int aaMode,
                              FontStrikeDesc desc) {

        this.fontResource = fontResource;
        this.size = size;
        this.desc = desc;
        PrismFontFactory factory = PrismFontFactory.getFontFactory();
        boolean lcdEnabled = factory.isLCDTextSupported();
        this.aaMode = lcdEnabled ? aaMode : FontResource.AA_GREYSCALE;
        if (tx.isTranslateOrIdentity()) {
            transform = BaseTransform.IDENTITY_TRANSFORM;
        } else {
            transform = new Affine2D(tx.getMxx(), tx.getMyx(),
                                     tx.getMxy(), tx.getMyy(),
                                     0f, 0f);
        }
    }

    DisposerRecord getDisposer() {
        if (disposer == null) {
            // Caller will arrange for the disposer to be enqueued.
            // Strikes are partialy managed by a GlyphCache such that when it
            // wants to free space there it calls back to remove the
            // strike from a font's map.
            // So we could instead arrange for synchronously freeing the resources
            // at that time, in which case a disposer reference queue isn't needed.
            // But the disposer is more certain (safer).
            disposer = createDisposer(desc);
        }
        return disposer;
    }

    protected abstract DisposerRecord createDisposer(FontStrikeDesc desc);

    @Override
    public synchronized void clearDesc() {
        fontResource.getStrikeMap().remove(desc);
        // Native resources are freed via a disposer once we are sure
        // all references are cleared. It also ensures we don't leak.
    }

    /**
     * Returns the notional size of this strike with
     * the graphics transform factored out. This is presently
     * needed for the J2D pipeline but arguably the strike should
     * not even need to keep this around except for needing to
     * return  metrics and outlines in user space. The consequence is
     * we can't share a strike between 12 pt at scale of 2.0 and 24 pt
     * at scale of 1.0
     */
    @Override
    public float getSize() {
        return size;
    }

    @Override
    public Metrics getMetrics() {
        // I don't need native code to do this .. it can be done
        // by just reading the hhea table once for the font. This should
        // save a JNI call per strike.
        // T2K uses the hhea table. Maybe we should use OS/2 metrics
        // but www.microsoft.com/typography/otspec/recom.htm#tad has
        // a section on GDI baseline to baseline distance which shows it
        // to be a wash if the usWin ascent and descent match, and in any
        // case, clearly the hhea values are part of the calculation for
        // leading.
        if (metrics == null) {
            metrics = fontResource.getFontMetrics(size);
        }
        return metrics;
    }

    @Override
    public T getFontResource() {
        return fontResource;
    }

    @Override
    public boolean drawAsShapes() {
        return drawShapes;
    }

    @Override
    public int getAAMode() {
        return aaMode;
    }

    @Override
    public BaseTransform getTransform() {
        return transform;
    }

    @Override
    public int getQuantizedPosition(Point2D point) {
        if (aaMode == FontResource.AA_GREYSCALE) {
            /* No subpixel position */
            point.x = Math.round(point.x);
        } else {
            /* Prism can produce 3 subpixel positions in the shader */
            point.x = Math.round(3.0 * point.x) / 3.0f;
        }
        point.y = Math.round(point.y);
        return 0;
    }

    /**
     * Access to individual character advances are frequently needed for layout
     * understand that advance may vary for single glyph if ligatures or kerning
     * are enabled
     * @param ch char
     * @return advance of single char
     */
    @Override
    public float getCharAdvance(char ch) {
        int glyphCode = fontResource.getGlyphMapper().charToGlyph((int)ch);
        return fontResource.getAdvance(glyphCode, size);
    }

    /* REMIND A map is not the solution ultimately required here */
    @Override
    public Glyph getGlyph(char ch) {
        int glyphCode = fontResource.getGlyphMapper().charToGlyph((int)ch);
        return getGlyph(glyphCode);
    }

    protected abstract Glyph createGlyph(int glyphCode);

    @Override
    public Glyph getGlyph(int glyphCode) {
        Glyph glyph = glyphMap.get(glyphCode);
        if (glyph == null) {
            glyph = createGlyph(glyphCode);
            glyphMap.put(glyphCode, glyph);
        }
        return glyph;
    }

    protected abstract Path2D createGlyphOutline(int glyphCode);

    @Override
    public Shape getOutline(GlyphList gl, BaseTransform transform) {
        Path2D result = new Path2D();
        getOutline(gl, transform, result);
        return result;
    }

    void getOutline(GlyphList gl, BaseTransform transform, Path2D p) {
        p.reset();
        if (gl == null) {
            return;
        }
        if (transform == null) {
            transform = BaseTransform.IDENTITY_TRANSFORM;
        }
        Affine2D t = new Affine2D();
        for (int i = 0; i < gl.getGlyphCount(); i++) {
            int glyphCode = gl.getGlyphCode(i);
            if (glyphCode != CharToGlyphMapper.INVISIBLE_GLYPH_ID) {
                Shape gp = createGlyphOutline(glyphCode);
                if (gp != null) {
                    t.setTransform(transform);
                    t.translate(gl.getPosX(i), gl.getPosY(i));
                    p.append(gp.getPathIterator(t), false);
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PrismFontStrike)) {
            return false;
        }
        final PrismFontStrike other = (PrismFontStrike) obj;

        // REMIND: When fonts can be rendered other than as greyscale
        // and generally differ in ways other than the size
        // we need to update this method.
        return this.size == other.size &&
               this.transform.getMxx() == other.transform.getMxx() &&
               this.transform.getMxy() == other.transform.getMxy() &&
               this.transform.getMyx() == other.transform.getMyx() &&
               this.transform.getMyy() == other.transform.getMyy() &&
               this.fontResource.equals(other.fontResource);
    }

    private int hash;
    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }
        hash = Float.floatToIntBits(size) +
               Float.floatToIntBits((float)transform.getMxx()) +
               Float.floatToIntBits((float)transform.getMyx()) +
               Float.floatToIntBits((float)transform.getMxy()) +
               Float.floatToIntBits((float)transform.getMyy());
        hash = 71 * hash + fontResource.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "FontStrike: " + super.toString() +
               " font resource = " + fontResource +
               " size = " + size +
               " matrix = " + transform;
    }
}
