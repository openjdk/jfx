/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.prism;

import com.sun.javafx.font.CharToGlyphMapper;
import com.sun.javafx.font.FontFactory;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.text.TextRun;
import static com.sun.javafx.webkit.prism.TextUtilities.getLayoutWidth;
import static com.sun.javafx.webkit.prism.TextUtilities.getLayoutBounds;
import com.sun.prism.GraphicsPipeline;
import com.sun.webkit.graphics.WCFont;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

final class WCFontImpl extends WCFont {
    private final static Logger log =
        Logger.getLogger(WCFontImpl.class.getName());

    private static final HashMap<String, String> FONT_MAP = new HashMap<String, String>();

    static WCFont getFont(String name, boolean bold, boolean italic, float size) {
        FontFactory factory = GraphicsPipeline.getPipeline().getFontFactory();
        synchronized (FONT_MAP) {
            if (FONT_MAP.isEmpty()) {
                FONT_MAP.put("serif", "Serif");
                FONT_MAP.put("dialog", "SansSerif");
                FONT_MAP.put("helvetica", "SansSerif");
                FONT_MAP.put("sansserif", "SansSerif");
                FONT_MAP.put("sans-serif", "SansSerif");
                FONT_MAP.put("monospace", "Monospaced");
                FONT_MAP.put("monospaced", "Monospaced");
                for (String family : factory.getFontFamilyNames()) {
                    FONT_MAP.put(family.toLowerCase(), family);
                }
            }
        }
        String family = FONT_MAP.get(name.toLowerCase());
        if (log.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder("WCFontImpl.get(");
            sb.append(name).append(", ").append(size);
            if (bold) {
                sb.append(", bold");
            }
            if (italic) {
                sb.append(", italic");
            }
            log.fine(sb.append(") = ").append(family).toString());
        }
        return (family != null)
                ? new WCFontImpl(factory.createFont(family, bold, italic, size))
                : null;
    }

    private final PGFont font;

    WCFontImpl(PGFont font) {
        this.font = font;
    }

    @Override public int getOffsetForPosition(String str, float x) {
        TextLayout layout = TextUtilities.createLayout(str, font);
        GlyphList[] runs = layout.getRuns();
        TextRun run = (TextRun) runs[0];
        int offset = run.getOffsetAtX(x, null);
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("str='%s' (length=%d), x=%.2f => %d",
                    str, str.length(), x, offset));
        }
        return offset;
    }

    private FontStrike strike;
    private FontStrike getFontStrike()
    {
        if (strike == null) {
            strike = font.getStrike(BaseTransform.IDENTITY_TRANSFORM, FontResource.AA_LCD);
        }
        return strike;
    }

    @Override public double getGlyphWidth(int glyph) {
        return getFontStrike().getFontResource().getAdvance(glyph, font.getSize());
    }

    @Override public float getXHeight() {
        return getFontStrike().getMetrics().getXHeight();
    }

    @Override public int[] getGlyphCodes(char[] chars) {
        int[] glyphs = new int[chars.length];
        CharToGlyphMapper mapper = getFontStrike().getFontResource().getGlyphMapper();
        mapper.charsToGlyphs(chars.length, chars, glyphs);
        return glyphs;
    }

    @Override public double getStringWidth(String str) {
        double result = getLayoutWidth(str, font);
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("str='%s' (length=%d) => %.1f",
                                    str, str.length(), result));
        }
        return result;
    }

    @Override public double[] getStringBounds(String str, int from, int to, boolean rtl) {
        float beforeWidth = getLayoutWidth(str.substring(0, from), font);
        BaseBounds bounds = getLayoutBounds(str.substring(0, to), font);
        double[] result = new double[] {
            beforeWidth,                    // see RTL case below
            0,                              // not really used
            bounds.getWidth() - beforeWidth,
            bounds.getHeight(),             // not really used
        };
        if (rtl) {
            float totalWidth = getLayoutWidth(str, font);
            result[0] = totalWidth - bounds.getWidth();
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format(
                    "str='%s' (length=%d) [%d, %d], rtl=%b => [%.1f, %.1f + %.1f x %.1f]",
                    str, str.length(), from, to, rtl,
                    result[0], result[1], result[2], result[3]));
        }
        return result;
    }

    public float getAscent() {
        // REMIND: This method needs to require a render context.
        float res = - getFontStrike().getMetrics().getAscent();
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "getAscent({0}, {1}) = {2}",
                    new Object[] {font.getName(), font.getSize(),
                    res});
        }
        return res;
    }

    public float getDescent() {
        // REMIND: This method needs to require a render context.
        float res = getFontStrike().getMetrics().getDescent();
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "getDescent({0}, {1}) = {2}",
                    new Object[] {font.getName(), font.getSize(),
                    res});
        }
        return res;
    }

    public float getLineSpacing() {
        // REMIND: This method needs to require a render context.
        float res = getFontStrike().getMetrics().getLineHeight();
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "getLineSpacing({0}, {1}) = {2}",
                    new Object[] {font.getName(), font.getSize(),
                    res});
        }
        return res;
    }

    public float getLineGap() {
        // REMIND: This method needs to require a render context.
        float res = getFontStrike().getMetrics().getLineGap();
        if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "getLineGap({0}, {1}) = {2}",
                    new Object[] {font.getName(), font.getSize(),
                    res });
        }
        return res;
    }

    public boolean hasUniformLineMetrics() {
        return false;
    }

    public Object getPlatformFont() {
        return font;
    }
}
