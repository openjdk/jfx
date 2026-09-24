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

import com.sun.javafx.font.directwrite.DWFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Test seam onto the Windows font-enumeration entry points of {@code javafx_font}: the
 * {@code fontpath.c} natives that {@link PrismFontFactory} and {@link DWFactory} call.
 * <p>
 * The parity golden for the Windows enumeration ({@code test.com.sun.javafx.font.FontEnumerationGoldenTest})
 * records what these entry points return, so it has to reach them <em>raw</em> - before
 * {@code resolveWindowsFonts} post-processes the maps and before the factory caches anything. When the
 * natives are replaced by Java, only this class needs re-pointing at the replacements: a signature change
 * fails here at compile time, which is the intended signal.
 * <p>
 * The package-private members are called directly. The three {@code private static} natives are reached
 * reflectively; this class is compiled into {@code javafx.graphics} itself, so the reflection stays inside
 * the module and needs no {@code --add-opens}.
 */
public final class PrismFontFactoryShim {

    private PrismFontFactoryShim() {
    }

    /** {@code PrismFontFactory.populateFontFileNameMap} - the GDI/registry enumeration into fresh maps. */
    public static void populateFontFileNameMap(HashMap<String, String> fontToFileMap,
                                               HashMap<String, String> fontToFamilyNameMap,
                                               HashMap<String, ArrayList<String>> familyToFontListMap,
                                               Locale locale) {
        PrismFontFactory.populateFontFileNameMap(fontToFileMap, fontToFamilyNameMap,
                                                 familyToFontListMap, locale);
    }

    /** {@code PrismFontFactory.getFontPath} - the system font directory, or two joined by {@code ;}. */
    public static String getFontPath() {
        return invokeStatic(PrismFontFactory.class, "getFontPath", new Class<?>[0]);
    }

    /** {@code PrismFontFactory.getPathNameWindows} - a bare file name resolved against the font dirs. */
    public static String getPathNameWindows(String filename) {
        return PrismFontFactory.getPathNameWindows(filename);
    }

    /** {@code PrismFontFactory.getLCDContrastWin32} - SPI_GETFONTSMOOTHINGCONTRAST, in thousandths. */
    public static int getLCDContrastWin32() {
        return PrismFontFactory.getLCDContrastWin32();
    }

    /** {@code PrismFontFactory.getSystemLCID} - LOCALE_ILANGUAGE of the system default LCID. */
    public static short getSystemLCID() {
        return PrismFontFactory.getSystemLCID();
    }

    /**
     * {@code DWFactory.regReadFontLink} - the raw REG_MULTI_SZ under
     * {@code HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\FontLink\SystemLink}, NUL separators and
     * terminators included, or {@code null} when the value is absent.
     */
    public static String regReadFontLink(String fontName) {
        return invokeStatic(DWFactory.class, "regReadFontLink", new Class<?>[] {String.class}, fontName);
    }

    /** {@code DWFactory.getEUDCFontFile} - the configured EUDC font file, or {@code null}. */
    public static String getEUDCFontFile() {
        return invokeStatic(DWFactory.class, "getEUDCFontFile", new Class<?>[0]);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeStatic(Class<?> owner, String name, Class<?>[] parameterTypes,
                                      Object... arguments) {
        try {
            Method method = owner.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(null, arguments);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(owner.getName() + "." + name + " no longer exists; re-point"
                    + " PrismFontFactoryShim at its replacement", e);
        } catch (IllegalAccessException e) {
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
