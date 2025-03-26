/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.javafx.pgstub;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;
import javafx.scene.text.Font;
import com.sun.javafx.font.CharToGlyphMapper;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.FontStrikeDesc;
import com.sun.javafx.geom.transform.BaseTransform;

/**
 *
 */
public class StubFontResource implements FontResource {
    private final Font font;
    private Boolean bold;
    private static final CharToGlyphMapper glyphMapper = initCharToGlyphMapper();

    public StubFontResource(Font font) {
        this.font = font;
    }

    @Override
    public String getFullName() {
        return font.getName();
    }

    @Override
    public String getPSName() {
        return font.getName();
    }

    @Override
    public String getFamilyName() {
        return font.getFamily();
    }

    @Override
    public String getFileName() {
        return font.getName();
    }

    @Override
    public String getStyleName() {
        return font.getName();
    }

    @Override
    public String getLocaleFullName() {
        return getFullName();
    }

    @Override
    public String getLocaleFamilyName() {
        return getFamilyName();
    }

    @Override
    public String getLocaleStyleName() {
        return getStyleName();
    }

    // see com.sun.javafx.scene.text.TextLayout flags
    @Override
    public int getFeatures() {
        return 0;
    }

    @Override
    public boolean isBold() {
        if (bold == null) {
            String name = font.getStyle();
            bold = name.toLowerCase(Locale.ROOT).contains("bold");
        }
        return bold.booleanValue();
    }

    @Override
    public boolean isItalic() {
        return false;
    }

    // returns glyph width
    @Override
    public float getAdvance(int gc, float size) {
        // +1 for bold fonts
        return isBold() ? size + StubFontMetrics.BOLD_FONT_EXTRA_WIDTH : size;
    }

    // returns [xmin, ymin, xmax, ymax]
    @Override
    public float[] getGlyphBoundingBox(int gc, float size, float[] b) {
        if (b == null || b.length < 4) {
            b = new float[4];
        }

        float xmin = 0.0f;
        float ymin = 0.0f;
        float xmax = getAdvance(gc, size);
        float ymax = StubFontMetrics.BASELINE * size;

        // PrismTextLayoutBase:
        //private static final int X_MIN_INDEX = 0;
        //private static final int Y_MIN_INDEX = 1;
        //private static final int X_MAX_INDEX = 2;
        //private static final int Y_MAX_INDEX = 3;
        b[0] = xmin;
        b[1] = ymin;
        b[2] = xmax;
        b[3] = ymax;
        return b;
    }

    @Override
    public int getDefaultAAMode() {
        return 0;
    }

    @Override
    public CharToGlyphMapper getGlyphMapper() {
        return glyphMapper;
    }

    @Override
    public Map<FontStrikeDesc, WeakReference<FontStrike>> getStrikeMap() {
        return null;
    }

    @Override
    public FontStrike getStrike(float size, BaseTransform t) {
        return new StubFontStrike(this, size, t);
    }

    @Override
    public FontStrike getStrike(float size, BaseTransform t, int aaMode) {
        return new StubFontStrike(this, size, t);
    }

    @Override
    public Object getPeer() {
        return null;
    }

    @Override
    public void setPeer(Object peer) {
    }

    @Override
    public boolean isEmbeddedFont() {
        return false;
    }

    @Override
    public boolean isColorGlyph(int gc) {
        return false;
    }

    private static CharToGlyphMapper initCharToGlyphMapper() {
        return new CharToGlyphMapper() {
            @Override
            public int getGlyphCode(int charCode) {
                return charCode;
            }
        };
    }
}
