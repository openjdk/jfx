/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font;

/**
 * Test seam onto the font-directory scan of {@link PrismFontFactory}: the pass that adds the fonts of
 * {@code ${java.home}/lib/fonts} to the factory's maps once the platform fonts are known. The scan offers every
 * file with a font suffix to {@code registerEmbeddedFont} before it parses the file, so a test that scans a
 * directory of its own reaches the rasterizer with files of its choosing.
 */
public final class PrismFontFactoryScanShim {

    private PrismFontFactoryScanShim() {
    }

    /**
     * {@code PrismFontFactory.populateFontFileNameMapGeneric}. The platform maps must already exist, as they do when
     * the factory scans the JDK font directory; any font lookup through {@code factory} builds them.
     */
    public static void populateFontFileNameMapGeneric(PrismFontFactory factory, String fontDirectory) {
        factory.populateFontFileNameMapGeneric(fontDirectory);
    }
}
