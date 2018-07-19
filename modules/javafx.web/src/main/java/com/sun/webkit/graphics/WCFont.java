/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.graphics;

public abstract class WCFont extends Ref {

    public abstract Object getPlatformFont();

    public abstract WCFont deriveFont(float size);

    public abstract int getOffsetForPosition(String str, float x);

    public abstract WCGlyphBuffer getGlyphsAndAdvances(String str, int from,
                                                       int to, boolean rtl);

    public abstract int[] getGlyphCodes(char[] chars);

    public abstract float getXHeight();

    public abstract double getGlyphWidth(int glyph);

    public abstract float[] getGlyphBoundingBox(int glyph);

    public abstract double[] getStringBounds(String str, int from, int to,
                                             boolean rtl);

    public abstract double getStringWidth(String str);

    /**
     * Returns a hash code value for the object.
     * NB: This method is called from native code!
     *
     * @return a hash code value for this object.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @see     java.util.Hashtable
     */
    @Override
    public int hashCode() {
        Object font = getPlatformFont();
        return (font != null)
                ? font.hashCode()
                : 0;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * NB: This method is called from native code!
     *
     * @param object  the reference object with which to compare
     * @return        {@code true} if this object is the same as the object argument;
     *  {@code false} otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof WCFont) {
            Object font1 = getPlatformFont();
            Object font2 = ((WCFont) object).getPlatformFont();
            return font1 == null ? font2 == null : font1.equals(font2);
        }
        return false;
    }

    // Font metrics

    public abstract float getAscent();

    public abstract float getDescent();

    public abstract float getLineSpacing();

    public abstract float getLineGap();

    public abstract boolean hasUniformLineMetrics();

    public abstract float getCapHeight();
}
