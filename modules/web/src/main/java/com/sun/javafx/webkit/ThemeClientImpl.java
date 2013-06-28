/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit;

import com.sun.webkit.ThemeClient;
import com.sun.webkit.graphics.RenderTheme;
import com.sun.webkit.graphics.ScrollBarTheme;
import com.sun.javafx.webkit.theme.RenderThemeImpl;
import com.sun.javafx.webkit.theme.ScrollBarThemeImpl;

public final class ThemeClientImpl extends ThemeClient {
    private final Accessor accessor;

    public ThemeClientImpl(Accessor accessor) {
        this.accessor = accessor;
    }

    @Override protected RenderTheme createRenderTheme() {
        return new RenderThemeImpl(accessor);
    }

    @Override protected ScrollBarTheme createScrollBarTheme() {
        return new ScrollBarThemeImpl(accessor);
    }
}
