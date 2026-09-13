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

package com.sun.glass.ui.win;

import com.sun.glass.ui.HeaderButtonOverlay;
import com.sun.javafx.binding.StringConstant;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.layout.HeaderButtonType;
import javafx.stage.Window;

/**
 * Windows-specific version of {@link HeaderButtonOverlay} that tweaks the scaling of header button glyphs.
 */
public class WinHeaderButtonOverlay extends HeaderButtonOverlay {

    private static final String HEADER_BUTTONS_STYLESHEET = "WindowDecoration.css";

    /**
     * These are additional scale factors for the header button glyphs at various DPI scales to account
     * for differences in the way the glyphs are rendered by JavaFX and Windows. Slightly adjusting
     * the scaling makes the JavaFX glyphs look more similar to the native glyphs drawn by Windows.
     * The values must be listed in 25% increments. DPI scales outside of the listed range default
     * to an additional scaling factor of 1.
     */
    private static final double[][] SCALE_FACTORS = new double[][] {
        { 1.0, 1.15 },
        { 1.25, 1.1 },
        { 1.5, 1.15 },
        { 1.75, 1.0 },
        { 2.0, 1.15 },
        { 2.25, 1.05 },
        { 2.5, 0.95 },
    };

    public WinHeaderButtonOverlay(boolean modalOrOwned, boolean utility, boolean rightToLeft) {
        super(getStylesheet(), modalOrOwned, utility, rightToLeft);

        var windowProperty = sceneProperty().flatMap(Scene::windowProperty);

        windowProperty
            .flatMap(Window::renderScaleXProperty)
            .orElse(1.0)
            .map(v -> getGlyphScaleFactor(v.doubleValue()))
            .subscribe(this::updateGlyphScaleX);

        windowProperty
            .flatMap(Window::renderScaleYProperty)
            .orElse(1.0)
            .map(v -> getGlyphScaleFactor(v.doubleValue()))
            .subscribe(this::updateGlyphScaleY);
    }

    private double getGlyphScaleFactor(double scale) {
        for (double[] mapping : SCALE_FACTORS) {
            if (scale >= (mapping[0] - 0.125) && scale <= (mapping[0] + 0.125)) {
                return mapping[1];
            }
        }

        return 1.0;
    }

    private void updateGlyphScaleX(double scale) {
        for (var buttonType : HeaderButtonType.values()) {
            getButtonGlyph(buttonType).setScaleX(scale);
        }
    }

    private void updateGlyphScaleY(double scale) {
        for (var buttonType : HeaderButtonType.values()) {
            getButtonGlyph(buttonType).setScaleY(scale);
        }
    }

    private static ObservableValue<String> getStylesheet() {
        var url = WinHeaderButtonOverlay.class.getResource(HEADER_BUTTONS_STYLESHEET);
        if (url == null) {
            throw new RuntimeException("Resource not found: " + HEADER_BUTTONS_STYLESHEET);
        }

        return StringConstant.valueOf(url.toExternalForm());
    }
}
