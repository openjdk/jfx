/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.options;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import com.oracle.tools.fx.monkey.settings.HasSettings;
import com.oracle.tools.fx.monkey.settings.SStream;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.FontPickerPane;
import com.oracle.tools.fx.monkey.util.Formats;
import com.oracle.tools.fx.monkey.util.PopupButton;

/**
 * Font Option Bound to a Property.
 */
public class FontOption extends PopupButton implements HasSettings {

    private final SimpleObjectProperty<Font> property = new SimpleObjectProperty<>();

    public FontOption(String name, boolean allowNull, ObjectProperty<Font> p) {
        FX.name(this, name);
        setMaxWidth(Double.MAX_VALUE);
        setAlignment(Pos.CENTER_LEFT);

        if (p != null) {
            property.bindBidirectional(p);
        }

        setContentSupplier(() -> {
            FontPickerPane fp = new FontPickerPane(property, allowNull, this::hidePopup);
            onShown(fp::requestPatternFieldFocus);
            return fp;
        });

        textProperty().bind(Bindings.createStringBinding(this::getButtonText, property));

        setFontValue(property.get());

        setOnAction((ev) -> togglePopup());
    }

    private void setFontValue(Font f) {
        String name;
        String style;
        double size;
        if (f == null) {
            name = null;
            style = null;
            size = Font.getDefault().getSize();
        } else {
            name = f.getFamily();
            style = f.getStyle();
            size = f.getSize();
        }
    }

    @Override
    public SStream storeSettings() {
        SStream s = SStream.writer();
        Font f = property.get();
        if (f == null) {
            s.add("-");
        } else {
            s.add(f.getName());
            s.add(f.getSize());
        }
        return s;
    }

    @Override
    public void restoreSettings(SStream s) {
        Font f;
        String name = s.nextString("-");
        if ("-".equals(name)) {
            f = null;
        } else {
            double sz = s.nextDouble(Font.getDefault().getSize());
            f = new Font(name, sz);
        }
        property.set(f);
    }

    private String getButtonText() {
        Font f = property.get();
        return Formats.font(f);
    }

    public SimpleObjectProperty<Font> getProperty() {
        return property;
    }

    public void selectSystemFont() {
        setFont(Font.getDefault());
    }
}
