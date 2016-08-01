/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * Convert a Size to Duration
 *
 * @since 9
 */
public final class DurationConverter extends StyleConverter<ParsedValue<?, Size>, Duration> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static final DurationConverter INSTANCE = new DurationConverter();
    }

    public static StyleConverter<ParsedValue<?, Size>, Duration> getInstance() {
        return Holder.INSTANCE;
    }

    private DurationConverter() {
        super();
    }

    @Override
    public Duration convert(ParsedValue<ParsedValue<?, Size>, Duration> value, Font font) {
        ParsedValue<?, Size> parsedValue = value.getValue();
        Size size = parsedValue.convert(font);
        double time = size.getValue();
        Duration duration = null;
        if (time < Double.POSITIVE_INFINITY) {
            switch (size.getUnits()) {
                case S:  duration = Duration.seconds(time); break;
                case MS: duration = Duration.millis(time);  break;
                default: duration = Duration.UNKNOWN;
            }
        } else {
            duration = Duration.INDEFINITE;
        }
        return duration;
    }

    @Override
    public String toString() {
        return "DurationConverter";
    }

}
