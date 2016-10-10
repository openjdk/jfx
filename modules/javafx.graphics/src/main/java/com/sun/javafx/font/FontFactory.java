/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;

public interface FontFactory {
    public static final String DEFAULT_FULLNAME = "System Regular";

    public PGFont createFont(String name, float size);
    public PGFont createFont(String family,
                             boolean bold, boolean italic, float size);

    /**
     * Creates a new Font object by replicating the current Font object
     * and applying a new bold style, italic style, and size to it.
     * <p>
     * NOTE: bold and italic are hints.
     *
     * @param font the original font.
     * @param bold the bold style for the new font.
     * @param italic the italic style fort the new font.
     * @param size the size for the new font.
     * @return the new font.
     */
    public PGFont deriveFont(PGFont font,
                             boolean bold, boolean italic, float size);
    public String[] getFontFamilyNames();
    public String[] getFontFullNames();
    public String[] getFontFullNames(String family);

    /*
     * Indicates permission to load an embedded font
     */
    public boolean hasPermission();

    /**
     * Loads a font from the specified input stream.
     * If the load is successful such that the stream can be
     * fully read, and it represents a supported font format then a
     * <code>PGFont</code> object will be returned.
     * <p>
     * Any failure such as abbreviated input, or an unsupported font format
     * will result in a <code>null</code> return. It is the application's
     * responsibility to check this before use.
     * <p>
     * If the <code>register</code> flag is true, and the loading operation
     * completes successfully, then the returned font is registered
     * with the FX graphics system for creation by available constructors
     * and factory methods, and the application should use it in this
     * manner rather than calling this method again, which would
     * repeat the overhead of re-reading and installing the font.
     * <p>
     * When the font is registered, an alternative <code>name</code> can be
     * supplied. This name can be used for creation by available constructors
     * and factory methods.
     * <p>
     * The font <code>size</code> parameter is a convenience so that in
     * typical usage the application can directly use the returned (non-null)
     * font rather than needing to create one via a constructor. Invalid sizes
     * are those <=0 and will result in a default size.
     * <p>
     * This method does not close the input stream.
     *
     * @param name the name for font, it can be <code>null</code>.
     * @param stream the stream from which to load the font.
     * @param size the size for the font.
     * @param register whether the font should be register.
     * @param all whether to load all fonts from a TTC
     * @return the Font, or null if the font cannot be created.
     */
    public PGFont[] loadEmbeddedFont(String name, InputStream stream,
                                   float size, boolean register, boolean all);

    /**
     * Loads a font from the specified path. If the load is successful
     * such that the location is readable, and it represents a supported
     * font format then a <code>PGFont</code> object will be returned.
     * <p>
     * Any failure such as a file being unable to locate or read
     * from the resource, or if it doesn't represent a font, will result in
     * a <code>null</code> return. It is the application's responsibility
     * to check this before use.
     * <p>
     * If the <code>register</code> flag is true, and the loading operation
     * completes successfully, then the returned font is registered
     * with the FX graphics system for creation by available constructors
     * and factory methods, and the application should use it in this
     * manner rather than calling this method again, which would
     * repeat the overhead of re-reading and installing the font.
     * <p>
     * When the font is registered, an alternative <code>name</code> can be
     * supplied. This name can be used for creation by available constructors
     * and factory methods.
     * <p>
     * The font <code>size</code> parameter is a convenience so that in
     * typical usage the application can directly use the returned (non-null)
     * font rather than needing to create one via a constructor. Invalid sizes
     * are those <=0 and will result in a default size.
     *
     * @param name the name for font, it can be <code>null</code>.
     * @param path the path from which to load the font.
     * @param size the size for the font.
     * @param register whether the font should be register.
     * @param all whether to load all fonts from a TTC
     * @return the Font, or null if the font cannot be created.
     */
    public PGFont[] loadEmbeddedFont(String name, String path,
                                     float size, boolean register, boolean all);

    public boolean isPlatformFont(String name);
}
