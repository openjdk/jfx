/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.prism;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.text.PrismTextLayoutFactory;
import com.sun.javafx.text.TextRun;

final class TextUtilities {
    static TextLayout createLayout(String text, Object font) {
        TextLayout layout = PrismTextLayoutFactory.getFactory().createLayout();
        layout.setContent(text, font);
        return layout;
    }
    
    static BaseBounds getLayoutBounds(String str, Object font) {
        return createLayout(str, font).getBounds();
    }

    static float getLayoutWidth(String str, Object font) {
        return getLayoutBounds(str, font).getWidth();
    }

    static GlyphList createGlyphList(int[] glyphs, float[] advances, float x, float y) {
        TextRun run = new TextRun(0, glyphs.length, (byte) 0, true, 0, null, 0, false) {
            @Override public RectBounds getLineBounds() {
                return new RectBounds();
            }
        };
        run.shape(glyphs.length, glyphs, advances);
        run.setLocation(x, y);
        return run;
    }
}
