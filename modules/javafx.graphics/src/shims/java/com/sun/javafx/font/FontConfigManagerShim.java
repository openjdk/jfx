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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Test seam onto the Linux fontconfig entry points production uses: {@link FontConfigNative#getLogicalFonts} and
 * {@link FontConfigNative#populateFontMaps}, the {@code java.lang.foreign} bindings that replaced the two
 * {@code private static native} methods of {@link FontConfigManager} implemented by {@code fontpath_linux.c} at
 * commit {@code 7b43255b30}. The shim's own method names are still those of the natives, so the goldens read as
 * they were captured.
 * <p>
 * The goldens ({@code test.com.sun.javafx.font.LinuxFontConfigGoldenTest} and the child JVMs of
 * {@code LinuxFontProcessGoldenTest}) reach the entry points <em>raw</em>, with fresh arrays and maps, before
 * {@code FontConfigManager} post-processes or caches anything. This class has no {@code java.lang.foreign} of
 * its own. It names the entry points as strings and invokes them by same-module reflection: that is what lets
 * {@code LinuxFontGoldens.requireSameBinding} prove, at every verify, that these are the methods
 * {@code FontConfigManager} itself calls; when they move again, only this class is re-pointed, and a missing
 * method fails here naming the class to change.
 */
public final class FontConfigManagerShim {

    private FontConfigManagerShim() {
    }

    /**
     * Makes the font layer's process state what production has when it first reaches fontconfig:
     * {@link PrismFontFactory}'s static initializer has run and the font factory exists. On Linux that loads no
     * library of this module any more; {@code FontConfigNative} opens {@code libfontconfig.so.1} per call, and
     * {@code FTNative} and {@code PangoNative} bind their system libraries on their first use.
     */
    public static void ensureLoaded() {
        PrismFontFactory.getFontFactory();
    }

    /** {@code FontConfigNative.getLogicalFonts}: fills {@code fonts} in place and returns whether it succeeded. */
    public static boolean getFontConfig(String locale, FontConfigManager.FcCompFont[] fonts,
                                        boolean includeFallbacks) {
        return invokeStatic(FontConfigNative.class, "getLogicalFonts",
                            new Class<?>[] {String.class, FontConfigManager.FcCompFont[].class, boolean.class},
                            locale, fonts, includeFallbacks);
    }

    /** {@code FontConfigNative.populateFontMaps}: fills the three maps in place. */
    public static boolean populateMapsNative(HashMap<String, String> fontToFileMap,
                                             HashMap<String, String> fontToFamilyNameMap,
                                             HashMap<String, ArrayList<String>> familyToFontListMap,
                                             Locale locale) {
        return invokeStatic(FontConfigNative.class, "populateFontMaps",
                            new Class<?>[] {HashMap.class, HashMap.class, HashMap.class, Locale.class},
                            fontToFileMap, fontToFamilyNameMap, familyToFontListMap, locale);
    }

    /** {@code FontConfigManager.getFCLocaleStr}: the locale string production passes to fontconfig. */
    public static String fcLocaleStr() {
        return invokeStatic(FontConfigManager.class, "getFCLocaleStr", new Class<?>[0]);
    }

    /** A copy of the twelve fontconfig logical names, in the order production uses. */
    public static String[] fontConfigNames() {
        try {
            Field field = FontConfigManager.class.getDeclaredField("fontConfigNames");
            field.setAccessible(true);
            return ((String[]) field.get(null)).clone();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("FontConfigManager.fontConfigNames no longer exists; re-point"
                    + " FontConfigManagerShim at its replacement", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Entries built exactly as {@code FontConfigManager.initFontConfigLogFonts} builds them: {@code fcName},
     * {@code fcFamily} the text before the first {@code ':'} (all of it when there is none), {@code style} the
     * index modulo 4.
     */
    public static FontConfigManager.FcCompFont[] newLogicalFontArray(String... fcNames) {
        FontConfigManager.FcCompFont[] fonts = new FontConfigManager.FcCompFont[fcNames.length];
        for (int i = 0; i < fcNames.length; i++) {
            FontConfigManager.FcCompFont font = new FontConfigManager.FcCompFont();
            font.fcName = fcNames[i];
            int colon = fcNames[i].indexOf(':');
            font.fcFamily = colon < 0 ? fcNames[i] : fcNames[i].substring(0, colon);
            font.style = i % 4;
            fonts[i] = font;
        }
        return fonts;
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeStatic(Class<?> owner, String name, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = owner.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(null, arguments);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(owner.getSimpleName() + "." + name + " no longer exists; re-point"
                    + " FontConfigManagerShim at its replacement", e);
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
