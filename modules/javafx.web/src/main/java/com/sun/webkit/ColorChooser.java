/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import com.sun.javafx.scene.control.CustomColorDialog;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;

public final class ColorChooser {
    /**
     * Color conversion from double (0.0 to 1.0) to uchar (0 to 255)
     */
    private static final double COLOR_DOUBLE_TO_UCHAR_FACTOR = 255.0;

    /**
     * Color chooser dialog
     */
    private final CustomColorDialog colorChooserDialog;

    /**
     * Handle / pointer to native object
     */
    private final long pdata;

    private ColorChooser(WebPage webPage, Color color, long data) {
        this.pdata = data;

        WebPageClient<WebView> client = webPage.getPageClient();
        assert (client != null);
        colorChooserDialog = new CustomColorDialog(client.getContainer().getScene().getWindow());
        colorChooserDialog.setSaveBtnToOk();
        colorChooserDialog.setShowUseBtn(false);
        colorChooserDialog.setShowOpacitySlider(false);

        colorChooserDialog.setOnSave(() -> {
            twkSetSelectedColor(pdata,
                    (int) Math.round(colorChooserDialog.getCustomColor().getRed() * COLOR_DOUBLE_TO_UCHAR_FACTOR),
                    (int) Math.round(colorChooserDialog.getCustomColor().getGreen() * COLOR_DOUBLE_TO_UCHAR_FACTOR),
                    (int) Math.round(colorChooserDialog.getCustomColor().getBlue() * COLOR_DOUBLE_TO_UCHAR_FACTOR));
        });

        colorChooserDialog.setCurrentColor(color);
        colorChooserDialog.show();
    }

    private static ColorChooser fwkCreateAndShowColorChooser(WebPage webPage, int r, int g, int b, long pdata) {
        return new ColorChooser(webPage, Color.rgb(r,g,b), pdata);
    }

    private void fwkShowColorChooser(int r, int g, int b) {
        colorChooserDialog.setCurrentColor(Color.rgb(r,g,b));
        colorChooserDialog.show();
    }

    private void fwkHideColorChooser() {
        colorChooserDialog.hide();
    }

    private native void twkSetSelectedColor(long data, int r, int g, int b);
}
