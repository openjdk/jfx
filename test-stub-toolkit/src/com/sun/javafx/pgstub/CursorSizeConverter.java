/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.pgstub;

import javafx.geometry.Dimension2D;

public abstract class CursorSizeConverter {
    public static final CursorSizeConverter NO_CURSOR_SUPPORT =
            createConstantConverter(0, 0);

    public static final CursorSizeConverter IDENTITY_CONVERTER =
            new IdentityConverter();

    protected CursorSizeConverter() {
    }

    public abstract Dimension2D getBestCursorSize(
            int preferredWidth,
            int preferredHeight);

    public static CursorSizeConverter createConstantConverter(
            final int width, final int height) {
        return new ConstantConverter(width, height);
    }

    private static final class ConstantConverter extends CursorSizeConverter {
        final Dimension2D constantSize;

        public ConstantConverter(final int width, final int height) {
            constantSize = new Dimension2D(width, height);
        }

        @Override
        public Dimension2D getBestCursorSize(
                final int preferredWidth, 
                final int preferredHeight) {
            return constantSize;
        }
    }

    private static final class IdentityConverter extends CursorSizeConverter {
        @Override
        public Dimension2D getBestCursorSize(
                final int preferredWidth,
                final int preferredHeight) {
            return new Dimension2D(preferredWidth, preferredHeight);
        }
    }
}
