/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css.converter;

import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.StyleConverter;
import javafx.css.ParsedValue;
import javafx.scene.text.Font;

/**
 * Converter to combine two {@code Size} values.  The primary purpose of
 * this type is to handle "convert(size1, size2)" expressions in CSS.
 *
 * @since 9
 */
public final class DeriveSizeConverter extends StyleConverter<ParsedValue<Size, Size>[], Size> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static final DeriveSizeConverter INSTANCE = new DeriveSizeConverter();
    }

    /**
     * Gets the {@code DeriveSizeConverter} instance.
     * @return the {@code DeriveSizeConverter} instance
     */
    public static DeriveSizeConverter getInstance() {
        return Holder.INSTANCE;
    }

    private DeriveSizeConverter() {
        super();
    }

    @Override
    public Size convert(ParsedValue<ParsedValue<Size, Size>[], Size> value, Font font) {
        final ParsedValue<Size, Size>[] sizes = value.getValue();
        final double px1 = sizes[0].convert(font).pixels(font);
        final double px2 = sizes[1].convert(font).pixels(font);
        return new Size(px1 + px2, SizeUnits.PX);
    }

    @Override
    public String toString() {
        return "DeriveSizeConverter";
    }
}
