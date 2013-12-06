/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.j2d;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.FontFactory;
import com.sun.javafx.PlatformUtil;

/**
 * This is mostly just delegating, except for allowing for the
 * registration of embedded fonts with Java 2D.
 * Its possible it could also be check the names of all fonts that
 * are being used are known to 2D but I'm not sure I need that.
 */

final class J2DFontFactory implements FontFactory {

    FontFactory prismFontFactory;

    J2DFontFactory(FontFactory fontFactory) {
        prismFontFactory = fontFactory;
    }

    public PGFont createFont(String name, float size) {
        return prismFontFactory.createFont(name, size);
    }

    public PGFont createFont(String family,
                             boolean bold, boolean italic, float size) {
        return prismFontFactory.createFont(family, bold, italic, size);
    }

    public synchronized PGFont deriveFont(PGFont font, boolean bold, 
                                          boolean italic, float size) {
        return prismFontFactory.deriveFont(font, bold, italic, size);
    }

    public String[] getFontFamilyNames() {
        return prismFontFactory.getFontFamilyNames();
    }

    public String[] getFontFullNames() {
        return prismFontFactory.getFontFullNames();
    }

    public String[] getFontFullNames(String family) {
        return prismFontFactory.getFontFullNames(family);
    }

    public boolean isPlatformFont(String name) {
        return prismFontFactory.isPlatformFont(name);
    }

    /* This is an important but tricky one. We need to copy the
     * stream. I don't want to have to manage the temp file deletion here,
     * so although its non-optimal I will create a temp file, provide
     * input streams on it to both prism and 2D, then when they are done,
     * remove it.
     */
    public PGFont loadEmbeddedFont(String name, InputStream fontStream,
                                   float size, boolean register) {

        PGFont font = prismFontFactory.loadEmbeddedFont(name, fontStream, 
                                                        size, register);

        if (font == null) return null;
        final FontResource fr = font.getFontResource();
        registerFont(font.getFontResource());
        return font;
    }

    /**
     * Printing uses the 2D pipeline which isn't initialised until
     * printing begins, so grabs a copy of the file holding an 
     * embedded font to 2D on first use.
     */
    public static void registerFont(final FontResource fr) {

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                InputStream stream = null;
                try {
                    File file = new File(fr.getFileName());
                    stream = new FileInputStream(file);
                    Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
                    fr.setPeer(font);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Exception e2) {
                        }
                    }
                }
                return null;
            }
        });
    }

    public PGFont loadEmbeddedFont(String name, String path, 
                                   float size, boolean register) {

        PGFont font = prismFontFactory.loadEmbeddedFont(name, path, 
                                                        size, register);

        if (font == null) return null;
        final FontResource fr = font.getFontResource();
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    File file = new File(fr.getFileName());
                    Font font = Font.createFont(Font.TRUETYPE_FONT, file);
                    fr.setPeer(font);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        return font;
    }

    private static boolean compositeFontMethodsInitialized = false;
    private static Method getCompositeFontUIResource = null;

    /**
     * Returns a composite font from the font passed in the argument.
     * Note: This method relies on Java2D's sun.* APIs which are
     * subject to change.
     * ALso this may be just a stop gap measure.
     */
    static java.awt.Font getCompositeFont(final java.awt.Font srcFont) {
        if (PlatformUtil.isMac()) {
            return srcFont;
        }
        synchronized (J2DFontFactory.class) {
            if (!compositeFontMethodsInitialized) {
                AccessController.doPrivileged(
                    new PrivilegedAction<Void>() {
                        public Void run() {
                            compositeFontMethodsInitialized = true;
                            Class<?> fontMgrCls;
                            try {
                                // JDK7
                                fontMgrCls = Class.forName(
                                        "sun.font.FontUtilities", true, null);
                            } catch (ClassNotFoundException cnfe) {
                                try {
                                    // JDK5/6
                                    fontMgrCls = Class.forName(
                                       "sun.font.FontManager", true, null);
                                } catch (ClassNotFoundException cnfe2) {
                                    return null;
                                }
                            }

                            try {
                                getCompositeFontUIResource =
                                    fontMgrCls.getMethod(
                                    "getCompositeFontUIResource",
                                    java.awt.Font.class);
                            } catch (NoSuchMethodException nsme) {
                            }
                            return null;
                        }
                    });
            }
        }
    
        if (getCompositeFontUIResource != null) {
            try {
                return
                    (java.awt.Font)getCompositeFontUIResource.
                    invoke(null, srcFont);
            } catch (IllegalAccessException iae) {
            } catch (InvocationTargetException ite) {}
        }

        return srcFont;
    }
}
