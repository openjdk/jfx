/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.perf;

import java.util.logging.Logger;

import com.sun.webkit.graphics.WCFont;

public final class WCFontPerfLogger extends WCFont {
    private static final Logger log =
            Logger.getLogger(WCFontPerfLogger.class.getName());

    private static final PerfLogger logger = PerfLogger.getLogger(log);

    private final WCFont fnt;

    public WCFontPerfLogger(WCFont fnt) {
        this.fnt = fnt;
    }

    public synchronized static boolean isEnabled() {
        return logger.isEnabled();
    }

    public static void log() {
        logger.log();
    }

    public static void reset() {
        logger.reset();
    }

    public Object getPlatformFont() {
        return fnt.getPlatformFont();
    }

    public int getOffsetForPosition(String str, float x, float lSpacing,
                                    float wSpacing)
    {
        logger.resumeCount("GETOFFSETFORPOSITION");
        int res = fnt.getOffsetForPosition(str, x, lSpacing, wSpacing);
        logger.suspendCount("GETOFFSETFORPOSITION");
        return res;
    }


    public int[] getGlyphCodes(char[] chars) {
        logger.resumeCount("GETGLYPHCODES");
        int[] res = fnt.getGlyphCodes(chars);
        logger.suspendCount("GETGLYPHCODES");
        return res;
    }

    public float getXHeight() {
        logger.resumeCount("GETXHEIGHT");
        float res = fnt.getXHeight();
        logger.suspendCount("GETXHEIGHT");
        return res;
    }

    public double getGlyphWidth(int glyph) {
        logger.resumeCount("GETGLYPHWIDTH");
        double res = fnt.getGlyphWidth(glyph);
        logger.suspendCount("GETGLYPHWIDTH");
        return res;
    }

    public double getStringWidth(String str, float lSpacing, float wSpacing) {
        logger.resumeCount("GETSTRINGLENGTH");
        double res = fnt.getStringWidth(str, lSpacing, wSpacing);
        logger.suspendCount("GETSTRINGLENGTH");
        return res;
    }

    public double[] getStringBounds(String str, int from, int to, boolean rtl,
                                    float lSpacing, float wSpacing)
    {
        logger.resumeCount("GETSTRINGBOUNDS");
        double[] res = fnt.getStringBounds(str, from, to, rtl, lSpacing, wSpacing);
        logger.suspendCount("GETSTRINGBOUNDS");
        return res;
    }

    public int hashCode() {
        logger.resumeCount("HASH");
        int res = fnt.hashCode();
        logger.suspendCount("HASH");
        return res;
    }

    public boolean equals(Object object) {
        logger.resumeCount("COMPARE");
        boolean res = fnt.equals(object);
        logger.suspendCount("COMPARE");
        return res;
    }

    public float getAscent() {
        logger.resumeCount("GETASCENT");
        float res = fnt.getAscent();
        logger.suspendCount("GETASCENT");
        return res;
    }

    public float getDescent() {
        logger.resumeCount("GETDESCENT");
        float res = fnt.getDescent();
        logger.suspendCount("GETDESCENT");
        return res;
    }

    public float getLineSpacing() {
        logger.resumeCount("GETLINESPACING");
        float res = fnt.getLineSpacing();
        logger.suspendCount("GETLINESPACING");
        return res;
    }

    public float getLineGap() {
        logger.resumeCount("GETLINEGAP");
        float res = fnt.getLineGap();
        logger.suspendCount("GETLINEGAP");
        return res;
    }

    public boolean hasUniformLineMetrics() {
        logger.resumeCount("HASUNIFORMLINEMETRICS");
        boolean res = fnt.hasUniformLineMetrics();
        logger.suspendCount("HASUNIFORMLINEMETRICS");
        return res;
    }
}
