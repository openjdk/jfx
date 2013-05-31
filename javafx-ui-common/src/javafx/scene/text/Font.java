/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.text;

import java.io.FilePermission;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import com.sun.javafx.beans.annotations.Default;
import com.sun.javafx.tk.Toolkit;

/**
 * <p>The {@code Font} class represents fonts, which are used to render text on
 * screen.
 * <p>
 * The size of a {@code Font} is described as being specified in points
 * which are a real world measurement of approximately 1/72 inch.
 * <p>
 * Given that fonts scale with the rendering transform as determined
 * by the transform attributes of a {@code Node} using the {@code Font}
 * and its ancestors, the size will actually be relative to the local
 * coordinate space of the node, which should provide coordinates
 * similar to the size of a point if no scaling transforms are present
 * in the environment of the node.
 * Note that the real world distances specified by the default coordinate
 * system only approximate point sizes as a rule of thumb and are typically
 * defaulted to screen pixels for most displays.
 * <p>
 * For more information see {@link javafx.scene.Node} for more information
 * on the default coordinate system 
 * @since JavaFX 2.0
 */
public final class Font {

    private static final String DEFAULT_FAMILY = "System";
    private static final String DEFAULT_FULLNAME = "System Regular";

    /**
     * The default font for this platform. This is used whenever a font is not
     * specifically set on a Text node or overridden by CSS.
     */
    private static float defaultSystemFontSize = -1;
    private static float getDefaultSystemFontSize() {
        if (defaultSystemFontSize == -1) {
            defaultSystemFontSize =
                Toolkit.getToolkit().getFontLoader().getSystemFontSize();
        }
        return defaultSystemFontSize;
    }

    private static Font DEFAULT;
    /**
     * Gets the default font which will be from the family "System",
     * and typically the style "Regular", and be of a size consistent
     * with the user's desktop environment, to the extent that can
     * be determined.
     * @return the default font.
     */
    public static synchronized Font getDefault() {
        if (DEFAULT == null) {
            DEFAULT = new Font(DEFAULT_FULLNAME, getDefaultSystemFontSize());
        }
        return DEFAULT;
    }

    /**
     * Gets all the font families installed on the user's system, including any
     * application fonts or SDK fonts. This call has performance considerations
     * as looking up all of the fonts may be an expensive operation the
     * first time.
     */
    public static List<String> getFamilies() {
        return Toolkit.getToolkit().getFontLoader().getFamilies();
    }

    /**
     * Gets the names of all fonts that are installed on the users system,
     * including any application fonts and SDK fonts.
     * This call has performance considerations as
     * looking up all of the fonts may be an expensive operation the first time.
     */
    public static List<String> getFontNames() {
        return Toolkit.getToolkit().getFontLoader().getFontNames();
    }

    /**
     * Gets the names of all fonts in the specified font family that are 
     * installed on the users system, including any application fonts
     * and SDK fonts.
     * This call has performance considerations as looking up all of the
     * fonts may be an expensive operation the first time.
     */
    public static List<String> getFontNames(String family) {
        return Toolkit.getToolkit().getFontLoader().getFontNames(family);
    }

    /**
     * Searches for an appropriate font based on the font family name and
     * weight and posture style. This method is not guaranteed to return
     * a specific font, but does its best to find one that fits the
     * specified requirements.
     * <p>
     * A null or empty value for family allows the implementation to
     * select any suitable font.
     *
     * @param family The family of the font
     * @param weight The weight of the font
     * @param posture The posture or posture of the font
     * @param size The point size of the font. This can be a fractional value,
     * but must not be negative. If the size is < 0 the default size will be
     * used.
     */
    public static Font font(String family, FontWeight weight,
                            FontPosture posture, double size) {

        String fam =
            (family == null || "".equals(family)) ? DEFAULT_FAMILY : family;
        double sz = size < 0 ? getDefaultSystemFontSize() : size;
        return Toolkit.getToolkit().getFontLoader().font(fam, weight, posture, (float)sz);
    }

    /**
     * Searches for an appropriate font based on the font family name and weight
     * style. This method is not guaranteed to return a specific font, but does
     * its best to find one that fits the specified requirements.
     * A null or empty  value for family allows the implementation
     * to select any suitable font.
     *
     * @param family The family of the font
     * @param weight The weight of the font
     * @param size The point size of the font. This can be a fractional value,
     * but must not be negative. If the size is < 0 the default size will be
     * used.
     */
    public static Font font(String family, FontWeight weight, double size) {
        return font(family, weight, null, size);
    }

    /**
     * Searches for an appropriate font based on the font family name and posture
     * style. This method is not guaranteed to return a specific font, but does
     * its best to find one that fits the specified requirements. A null or empty
     * value for family allows the implementation to select any suitable font.
     *
     * @param family The family of the font
     * @param posture The posture or posture of the font
     * @param size The point size of the font. This can be a fractional value,
     * but must not be negative. If the size is < 0 the default size will be
     * used.
     */
    public static Font font(String family, FontPosture posture, double size) {
        return font(family, null, posture, size);
    }

    /**
     * Searches for an appropriate font based on the font family name and size.
     * This method is not guaranteed to return a specific font, but does
     * its best to find one that fits the specified requirements. A null or empty
     * value for family allows the implementation to select any suitable font.
     *
     * @param family The family of the font
     * @param size The point size of the font. This can be a fractional value,
     * but must not be negative. If the size is < 0 the default size will be
     * used.
     */
    public static Font font(String family, double size) {
        return font(family, null, null, size);
    }

    /**
     * The full font name. This name includes both the family name
     * and the style variant within that family. For example, for a plain
     * Arial font this would be "Arial Regular" and for a bolded
     * Arial font this would be "Arial Bold". The precise name to use when
     * loading a font is defined within each font file as the full font name.
     * For example, "Proxima Nova ExtraCondensed Bold Italic" would refer to a
     * specific Proxima Nova font.
     * A null or empty name allows the implementation to select any suitable
     * font.
     * <p>
     * There is a single unified way to load all of application supplied
     * (via <code>Font.loadFont()</code>, JavaFX runtime delivered fonts,
     * and system installed fonts. Simply create the font by specifying
     * the full name of the font you want to load. 
     * If the specific font cannot be located, then a fallback or default
     * font will be used. The "name" will be updated to reflect the actual name
     * of the font being used. A load failure condition can be discovered by
     * checking the name of the Font with the name you tried to load.
     * <p>
     * Note that if you wish to locate a font by font family and style
     * then you can use one of the {@link #font} factory methods defined in
     * this class.
     *
     * @defaultValue empty string
     */
    public final String getName() { return name; }
    private String name;

    /**
     * Returns the family of this font.
     * @return The family of this font.
     */
    public final String getFamily() { return family; }
    private String family;

    /**
     * The font specified string describing the style within the font family.
     * @return The style name of this font.
     */
    public final String getStyle() { return style; }
    private String style;

    /**
     * The point size for this font. This may be a fractional value such as
     * {@code 11.5}. If the specified value is < 0 the default size will be
     * used.
     *
     * @defaultValue 12
     */
    public final double getSize() { return size; }
    private double size;

    /**
     * The cached hash code, used to improve performance in situations where
     * we cache fonts, such as in the CSS routines.
     */
    private int hash = 0;

    /**
     * Constructs a font using the default face "System".
     * The underlying font used is determined by the implementation
     * based on the typical UI font for the current UI environment.
     *
     * @param size the font size to use
     */
    public Font(double size) {
        this(null, size);
    }

    
    /**
     * Constructs a font using the specified full face name and size
     * @param name full name of the font.
     * @param size the font size to use
     */
    public Font(String name, double size) {
        this.name = name;
        this.size = size;

        if (name == null || "".equals(name)) this.name = DEFAULT_FULLNAME;
        if (size < 0f) this.size = getDefaultSystemFontSize();
        // if a search was done based on family + style information, then the
        // native font has already been located and specified. Likewise if the
        // Font was created based on an existing native font. If however a Font
        // was created directly in FX, then we need to find the native font
        // to use. This call will also set the family and style by invoking
        // the impl_setNativeFont callback method.
        Toolkit.getToolkit().getFontLoader().loadFont(this);
    }

    /**
     * Private constructor for internal implementation
     * 
     * @param f native font
     * @param family font family name
     * @param name font full name
     * @param style style string
     * @param size font size
     */
    private Font(Object f, String family, String name, String style, double size) {
        this.nativeFont = f;
        this.family = family;
        this.name = name;
        this.style = style;
        this.size = size;
    }

    /**
     * Loads a font resource from the specified URL. If the load is successful
     * such that the location is readable, and it represents a supported
     * font format then a <code>Font</code> object will be returned.
     * <p>
     * Any failure such as a malformed URL being unable to locate or read
     * from the resource, or if it doesn't represent a font, will result in
     * a <code>null</code> return. It is the application's responsibility
     * to check this before use.
     * <p>
     * On a successful (non-null) return the font will be registered
     * with the FX graphics system for creation by available constructors
     * and factory methods, and the application should use it in this
     * manner rather than calling this method again, which would
     * repeat the overhead of downloading and installing the font.
     * <p>
     * The font <code>size</code> parameter is a convenience so that in
     * typical usage the application can directly use the returned (non-null)
     * font rather than needing to create one via a constructor. Invalid sizes
     * are those <=0 and will result in a default size.
     * <p>
     * If the URL represents a local disk file, then no copying is performed
     * and the font file is required to persist for the lifetime of the
     * application. Updating the file in any manner will result
     * in unspecified and likely undesired behaviours.
     * 
     * @param urlStr from which to load the font, specified as a String.
     * @param size of the returned font.
     * @return the Font, or null if the font cannot be created.
     */
    public static Font loadFont(String urlStr, double size) {
        URL url = null;
        try {
            url = new URL(urlStr); // null string arg. is caught here.
        } catch (Exception e) {
            return null;
        }
        if (size <= 0) {
            size = getDefaultSystemFontSize();
        }
        // Now lets parse the URL and decide if its a file,
        // or a remote URL from which we need to read.
        if (url.getProtocol().equals("file")) {
            String path = url.getFile();
            // The URL path may have a leading "/", when obtained
            // via ClassLoader. This can cause problems on Windows.
            // Getting the path from a File fixes this.
            path = new java.io.File(path).getPath();
            try {
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    FilePermission filePermission =
                        new FilePermission(path, "read");
                    sm.checkPermission(filePermission);
                }
            } catch (Exception e) {
                return null;
            }
            return Toolkit.getToolkit().getFontLoader().loadFont(path, size);
        }
        Font font = null;
        URLConnection connection = null;
        InputStream in = null;
        try {
            connection = url.openConnection();
            in = connection.getInputStream();
            font = Toolkit.getToolkit().getFontLoader().loadFont(in, size);
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
        }
        return font;
    }

    /**
     * Loads a font resource from the specified input stream.
     * If the load is successful such that the stream can be
     * fully read, and it represents a supported font format then a
     * <code>Font</code> object will be returned.
     * <p>
     * Any failure such as abbreviated input, or an unsupported font format
     * will result in a <code>null</code> return. It is the application's
     * responsibility to check this before use.
     * <p>
     * On a successful (non-null) return the font will be registered
     * with the FX graphics system for creation by available constructors
     * and factory methods, and the application should use it in this
     * manner rather than calling this method again, which would
     * repeat the overhead of re-reading and installing the font.
     * <p>
     * The font <code>size</code> parameter is a convenience so that in
     * typical usage the application can directly use the returned (non-null)
     * font rather than needing to create one via a constructor. Invalid sizes
     * are those <=0 and will result in a default size.
     * <p>
     * This method does not close the input stream.
     * @param in stream from which to load the font.
     * @param size of the returned font.
     * @return the Font, or null if the font cannot be created.
     */
    public static Font loadFont(InputStream in, double size) {
        if (size <= 0) {
            size = getDefaultSystemFontSize();
        }
        return Toolkit.getToolkit().getFontLoader().loadFont(in, size);
    }
    
    /**
     * Converts this {@code Font} object to a {@code String} representation.
     * The String representation is for informational use only and will change.
     * Do not use this string representation for any programmatic purpose.
     */
    @Override public String toString() {
        StringBuilder builder = new StringBuilder("Font[name=");
        builder = builder.append(name);
        builder = builder.append(", family=").append(family);
        builder = builder.append(", style=").append(style);
        builder = builder.append(", size=").append(size);
        builder = builder.append("]");
        return builder.toString();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is equal to the {@code obj} argument; {@code false} otherwise.
     */
    @Override public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Font) {
            Font other = (Font)obj;
            return (name == null ? other.name == null : name.equals(other.name))
                && size == other.size;
        }
        return false;
    }

    /**
     * Returns a hash code for this {@code Font} object.
     * @return a hash code for this {@code Font} object.
     */ 
    @Override public int hashCode() {
        if (hash == 0) {
            long bits = 17L;
            bits = 37L * bits + name.hashCode();
            bits = 37L * bits + Double.doubleToLongBits(size);
            hash = (int) (bits ^ (bits >> 32));
        }
        return hash;
    }

    private Object nativeFont;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public Object impl_getNativeFont() { return nativeFont; }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_setNativeFont(Object f, String nam, String fam, String styl) {
        nativeFont = f;
        name = nam;
        family = fam;
        style = styl;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static Font impl_NativeFont(Object f, String name, String family,
                                       String style, double size) {
        Font retFont = new Font( f, family, name, style, size);
        return retFont;
    }
}
