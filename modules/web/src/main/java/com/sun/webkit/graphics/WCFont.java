/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;

public abstract class WCFont extends Ref {

    public abstract Object getPlatformFont();

    public abstract WCFont deriveFont(float size);
    
    public abstract int getOffsetForPosition(String str, float x);

    public abstract int[] getGlyphCodes(char[] chars);

    public abstract float getXHeight();

    public abstract double getGlyphWidth(int glyph);

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
}
