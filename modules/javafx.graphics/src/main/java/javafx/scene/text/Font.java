/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.text.FontHelper;
import java.io.FilePermission;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import com.sun.javafx.tk.Toolkit;
import javafx.beans.NamedArg;

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

    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        FontHelper.setFontAccessor(new FontHelper.FontAccessor() {

            @Override
            public Object getNativeFont(Font font) {
                return font.getNativeFont();
            }

            @Override
            public void setNativeFont(Font font, Object f, String nam, String fam, String styl) {
                font.setNativeFont(f, nam, fam, styl);
            }

            @Override
            public Font nativeFont(Object f, String name, String family, String style, double size) {
                return Font.nativeFont(f, name, family, style, size);
            }

        });
    }

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
     * @return The default font.
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
     * @return The list containing all available font families.
     */
    public static List<String> getFamilies() {
        return Toolkit.getToolkit().getFontLoader().getFamilies();
    }

    /**
     * Gets the names of all fonts that are installed on the users system,
     * including any application fonts and SDK fonts.
     * This call has performance considerations as
     * looking up all of the fonts may be an expensive operation the first time.
     * @return The list containing all available fonts.
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
     * @param family the font family
     * @return The list containing the fonts for the given family.
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
     * @param size {@literal The point size of the font. This can be a fractional value,
     * but must not be negative. If the size is < 0 the default size will be
     * used.}
     * @return The font that best fits the specified requirements.
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
     * @param size {@literal The point size of the font. This can be a fractional value,
     * but must not be negative. If the size is < 0 the default size will be
     * used.}
     * @return The font that best fits the specified requirements.
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
     * @param size {@literal The point size of the font. This can be a fractional value,
     * but must not be negative. If the size is < 0 the default size will be
     * used.}
     * @return The font that best fits the specified requirements.
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
     * @param size {@literal The point size of the font. This can be a fractional value,
     * but must not be negative. If the size is < 0 the default size will be
     * used.}
     * @return The font that best fits the specified requirements.
     */
    public static Font font(String family, double size) {
        return font(family, null, null, size);
    }

    /**
     * Searches for an appropriate font based on the given font family name and
     * default font size.
     * This method is not guaranteed to return a specific font, but does
     * its best to find one that fits the specified requirements. A null or empty
     * value for family allows the implementation to select any suitable font.
     *
     * @param family The family of the font
     * @return The font that best fits the specified requirements.
     */
    public static Font font(String family) {
        return font(family, null, null, -1);
    }

    /**
     * Searches for an appropriate font based on the default font family name and
     * given font size.
     * This method is not guaranteed to return a specific font, but does
     * its best to find one that fits the specified requirements.
     *
     * @param size {@literal The point size of the font. This can be a fractional value,
     * but must not be negative. If the size is < 0 the default size will be
     * used.}
     * @return The font that best fits the specified requirements.
     */
    public static Font font(double size) {
        return font(null, null, null, size);
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
     * @return the full font name
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
     * {@code 11.5}. {@literal If the specified value is < 0 the default size will be
     * used.}
     *
     * @return the point size for this font
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
    public Font(@NamedArg("size") double size) {
        this(null, size);
    }


    /**
     * Constructs a font using the specified full face name and size
     * @param name full name of the font.
     * @param size the font size to use
     */
    public Font(@NamedArg("name") String name, @NamedArg("size") double size) {
        this.name = name;
        this.size = size;

        if (name == null || "".equals(name)) this.name = DEFAULT_FULLNAME;
        if (size < 0f) this.size = getDefaultSystemFontSize();
        // if a search was done based on family + style information, then the
        // native font has already been located and specified. Likewise if the
        // Font was created based on an existing native font. If however a Font
        // was created directly in FX, then we need to find the native font
        // to use. This call will also set the family and style by invoking
        // the setNativeFont callback method.
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
     * If a security manager is present, the application
     * must have both permission to read from the specified URL location
     * and the {@link javafx.util.FXPermission} "loadFont".
     * If the application does not have permission to read from the specified
     * URL location, then null is returned.
     * If the application does not have the "loadFont" permission then this method
     * will return the default system font with the specified font size.
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
     * font rather than needing to create one via a constructor.
     * {@literal Invalid sizes are those <=0 and will result in a default size.}
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
        Font[] fonts = loadFontInternal(urlStr, size, false);
        return (fonts == null) ? null : fonts[0];
    }

    /**
     * Loads font resources from the specified URL. If the load is successful
     * such that the location is readable, and it represents a supported
     * font format then an array of <code>Font</code> will be returned.
     * <p>
     * The use case for this method is for loading all fonts
     * from a TrueType Collection (TTC).
     * <p>
     * If a security manager is present, the application
     * must have both permission to read from the specified URL location
     * and the {@link javafx.util.FXPermission} "loadFont".
     * If the application does not have permission to read from the specified
     * URL location, then null is returned.
     * If the application does not have the "loadFont" permission then this method
     * will return an array of one element which is the default
     *  system font with the specified font size.
     * <p>
     * Any failure such as a malformed URL being unable to locate or read
     * from the resource, or if it doesn't represent a font, will result in
     * a <code>null</code> return. It is the application's responsibility
     * to check this before use.
     * <p>
     * On a successful (non-null) return the fonts will be registered
     * with the FX graphics system for creation by available constructors
     * and factory methods, and the application should use it in this
     * manner rather than calling this method again, which would
     * repeat the overhead of downloading and installing the fonts.
     * <p>
     * The font <code>size</code> parameter is a convenience so that in
     * typical usage the application can directly use the returned (non-null)
     * font rather than needing to create one via a constructor.
     * {@literal Invalid sizes are those <=0 and will result in a default size.}
     * <p>
     * If the URL represents a local disk file, then no copying is performed
     * and the font file is required to persist for the lifetime of the
     * application. Updating the file in any manner will result
     * in unspecified and likely undesired behaviours.
     *
     * @param urlStr from which to load the fonts, specified as a String.
     * @param size of the returned fonts.
     * @return array of Font, or null if the fonts cannot be created.
     * @since 9
     */
    public static Font[] loadFonts(String urlStr, double size) {
        return loadFontInternal(urlStr, size, true);
    }

    private static Font[] loadFontInternal(String urlStr, double size,
                                           boolean loadAll) {
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
            return
            Toolkit.getToolkit().getFontLoader().loadFont(path, size, loadAll);
        }
        Font[] fonts = null;
        URLConnection connection = null;
        InputStream in = null;
        try {
            connection = url.openConnection();
            in = connection.getInputStream();
            fonts =
               Toolkit.getToolkit().getFontLoader().loadFont(in, size, loadAll);
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
        return fonts;
    }

    /**
     * Loads a font resource from the specified input stream.
     * If the load is successful such that the stream can be
     * fully read, and it represents a supported font format then a
     * <code>Font</code> object will be returned.
     * <p>
     * If a security manager is present, the application
     * must have the {@link javafx.util.FXPermission} "loadFont".
     * If the application does not have permission then this method
     * will return the default system font with the specified font size.
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
     * font rather than needing to create one via a constructor.
     * {@literal Invalid sizes are those <=0 and will result in a default size.}
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
        Font[] fonts =
           Toolkit.getToolkit().getFontLoader().loadFont(in, size, false);
        return (fonts == null) ? null : fonts[0];
    }

    /**
     * Loads font resources from the specified input stream.
     * If the load is successful such that the stream can be
     * fully read, and it represents a supported font format then
     * an array of <code>Font</code> will be returned.
     * <p>
     * The use case for this method is for loading all fonts
     * from a TrueType Collection (TTC).
     * <p>
     * If a security manager is present, the application
     * must have the {@link javafx.util.FXPermission} "loadFont".
     * If the application does not have permission then this method
     * will return the default system font with the specified font size.
     * <p>
     * Any failure such as abbreviated input, or an unsupported font format
     * will result in a <code>null</code> return. It is the application's
     * responsibility to check this before use.
     * <p>
     * On a successful (non-null) return the fonts will be registered
     * with the FX graphics system for creation by available constructors
     * and factory methods, and the application should use it in this
     * manner rather than calling this method again, which would
     * repeat the overhead of re-reading and installing the fonts.
     * <p>
     * The font <code>size</code> parameter is a convenience so that in
     * typical usage the application can directly use the returned (non-null)
     * fonts rather than needing to re-create via a constructor.
     * {@literal Invalid sizes are those <=0 and will result in a default size.}
     * <p>
     * This method does not close the input stream.
     * @param in stream from which to load the fonts.
     * @param size of the returned fonts.
     * @return array of Font, or null if the fonts cannot be created.
     * @since 9
     */
    public static Font[] loadFonts(InputStream in, double size) {
        if (size <= 0) {
            size = getDefaultSystemFontSize();
        }
        Font[] fonts =
            Toolkit.getToolkit().getFontLoader().loadFont(in, size, true);
        return (fonts == null) ? null : fonts;
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

    Object getNativeFont() { return nativeFont; }

    void setNativeFont(Object f, String nam, String fam, String styl) {
        nativeFont = f;
        name = nam;
        family = fam;
        style = styl;
    }

    static Font nativeFont(Object f, String name, String family,
                                       String style, double size) {
        Font retFont = new Font( f, family, name, style, size);
        return retFont;
    }
}
