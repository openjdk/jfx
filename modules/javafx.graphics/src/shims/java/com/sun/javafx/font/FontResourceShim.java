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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Uncached font objects for the Linux goldens.
 * <p>
 * {@link PrismFontFactory} caches physical, composite and logical fonts, and their fallback slots grow in the
 * order the process first needs them, so a golden that went through those caches would depend on which test
 * class looked a font up first. The instances made here are never entered in any cache: a font file made by
 * {@link #newUncachedFontFile} is neither in the factory's file map nor under any name in its resource map, a
 * {@link PrismCompositeFontResource} with a {@code null} lookup name is not put in the factory's composite map
 * and gets a fresh {@link FallbackResource}, and a {@link LogicalFont} built directly bypasses the static
 * logical font table. No {@code java.lang.foreign} and no native call of its own.
 */
public final class FontResourceShim {

    private FontResourceShim() {
    }

    /**
     * The font at index 0 of {@code file}, made by the factory's own {@code createFontFile} exactly as its
     * {@code createFontResource} makes it, but unregistered and entered in no map; {@code null} when the factory
     * cannot open the file, as for a lookup.
     */
    public static FontResource newUncachedFontFile(String file) {
        try {
            return PrismFontFactory.getFontFactory().createFontFile(null, file, 0, false, false, false);
        } catch (Exception e) {
            return null;
        }
    }

    /** The package-private {@link PrismFont} constructor. */
    public static PGFont newPrismFont(FontResource resource, String name, float size) {
        return new PrismFont(resource, name, size);
    }

    /** A composite over {@code primary} with its own, empty fallback list. */
    public static CompositeFontResource newComposite(FontResource primary) {
        return new PrismCompositeFontResource(primary, null);
    }

    /** The glyph count the Java side parsed from the font's {@code maxp} table; no native call. */
    public static int numGlyphs(FontResource resource) {
        return ((PrismFontFile) resource).getNumGlyphs();
    }

    /** A logical font ({@code System}, {@code SansSerif}, {@code Serif}, {@code Monospaced}) outside the cache. */
    public static CompositeFontResource newLogicalFont(String family, boolean bold, boolean italic) {
        try {
            Constructor<LogicalFont> constructor =
                    LogicalFont.class.getDeclaredConstructor(String.class, boolean.class, boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(family, bold, italic);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("LogicalFont(String, boolean, boolean) no longer exists; re-point"
                    + " FontResourceShim at its replacement", e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        }
    }
}
