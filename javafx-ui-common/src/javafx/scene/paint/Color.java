/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.paint;

import java.util.HashMap;
import java.util.Map;

import javafx.animation.Interpolatable;

import com.sun.javafx.Utils;
import com.sun.javafx.beans.annotations.Default;
import com.sun.javafx.tk.Toolkit;

// NOTE: this definition, while correct, contains a lot of information which
// is irrelevant to most developers. We should get to the basic definition and
// usage patterns sooner.

/**
 * The Color class is used to encapsulate colors in the default sRGB color space.
 * Every color has an implicit alpha value of 1.0 or an explicit one provided
 * in the constructor. The alpha value defines the transparency of a color
 * and can be  represented by a double value in the range 0.0-1.0 or 0-255.
 * An alpha value of 1.0 or 255 means that the color is completely opaque
 * and an alpha value of 0 or 0.0 means that the color is completely transparent.
 * When constructing a {@code Color} with an explicit alpha or getting
 * the color/alpha components of a Color,
 * the color components are never premultiplied by the alpha component.
 * </p>
 *
 * <p>{@code Color}s can be created with the constructor or with one of several
 * utility methods.  The following lines of code all create the same
 * blue color:</p>
 *
 * <pre><code>
 * Color c = Color.BLUE;   //use the blue constant
 * Color c = new Color(0,0,1,1.0); // standard constructor, use 0->1.0 values, explicit alpha of 1.0
 *
 * Color c = Color.color(0,0,1.0); //use 0->1.0 values. implicit alpha of 1.0
 * Color c = Color.color(0,0,1.0,1.0); //use 0->1.0 values, explicit alpha of 1.0
 *
 * Color c = Color.rgb(0,0,255); //use 0->255 integers, implict alpha of 1.0
 * Color c = Color.rgb(0,0,255,1.0); //use 0->255 integers, explict alpha of 1.0
 *
 * Color c = Color.hsb(270,1.0,1.0); //hue = 270, saturation & value = 1.0. inplict alpha of 1.0
 * Color c = Color.hsb(270,1.0,1.0,1.0); //hue = 270, saturation & value = 1.0, explict alpha of 1.0
 *
 * Color c = Color.web("0x0000FF",1.0);// blue as a hex web value, explict alpha
 * Color c = Color.web("0x0000FF");// blue as a hex web value, implict alpha
 * Color c = Color.web("#0000FF",1.0);// blue as a hex web value, explict alpha
 * Color c = Color.web("#0000FF");// blue as a hex web value, implict alpha
 * Color c = Color.web("0000FF",1.0);// blue as a hex web value, explict alpha
 * Color c = Color.web("0000FF");// blue as a hex web value, implict alpha
 * </code></pre>
 *
 * <p>
 * The creation of a {@code Color} will throw {@code IllegalArgumentException} if any
 * of the values are out of range.
 * </p>
 *
 * <p>
 * For example:
 * <pre><code>
 * Rectangle rec1 = new Rectangle(5, 5, 50, 40);
 * rec1.setFill(Color.RED);
 * rec1.setStroke(Color.GREEN);
 * rec1.setStrokeWidth(3);
 *
 * Rectangle rec2 = new Rectangle(65, 5, 50, 40);
 * rec2.setFill(Color.rgb(91, 127, 255));
 * rec2.setStroke(Color.hsb(40, 0.7, 0.8));
 * rec2.setStrokeWidth(3);
 * </code></pre>
 * </p>
 */
public class Color extends Paint implements Interpolatable<Color> { // final

    /**
     * Brightness change factor for darker() and brighter() methods.
     */
    private static final double DARKER_BRIGHTER_FACTOR = 0.7;

    /**
     * Saturation change factor for saturate() and desaturate() methods.
     */
    private static final double SATURATE_DESATURATE_FACTOR = 0.7;

    /**
     * Creates an sRGB color with the specified red, green and blue values
     * in the range {@code 0.0-1.0}, and a given opacity.
     *
     * @param red the red component, in the range {@code 0.0-1.0}
     * @param green the green component, in the range {@code 0.0-1.0}
     * @param blue the blue component, in the range {@code 0.0-1.0}
     * @param opacity the opacity component, in the range {@code 0.0-1.0}
     * @return the {@code Color}
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Color color(double red, double green, double blue, @Default("1") double opacity) {
        return new Color(red, green, blue, opacity);
    }

    /**
     * Creates an opaque sRGB color with the specified red, green and blue values
     * in the range {@code 0.0-1.0}.
     *
     * @param red the red component, in the range {@code 0.0-1.0}
     * @param green the green component, in the range {@code 0.0-1.0}
     * @param blue the blue component, in the range {@code 0.0-1.0}
     * @return the {@code Color}
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Color color(double red, double green, double blue) {
        return new Color(red, green, blue, 1);
    }

    /**
     * Creates an sRGB color with the specified RGB values in the range {@code 0-255},
     * and a given opacity.
     *
     * @param red the red component, in the range {@code 0-255}
     * @param green the green component, in the range {@code 0-255}
     * @param blue the blue component, in the range {@code 0-255}
     * @param opacity the opacity component, in the range {@code 0.0-1.0}
     * @return the {@code Color}
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Color rgb(int red, int green, int blue, double opacity) {
        checkRGB(red, green, blue);
        return new Color(
            red / 255.0,
            green / 255.0,
            blue / 255.0,
            opacity);
    }

    /**
     * Creates an opaque sRGB color with the specified RGB values in the range {@code 0-255}.
     *
     * @param red the red component, in the range {@code 0-255}
     * @param green the green component, in the range {@code 0-255}
     * @param blue the blue component, in the range {@code 0-255}
     * @return the {@code Color}
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Color rgb(int red, int green, int blue) {
        checkRGB(red, green, blue);
        return new Color(
            red / 255.0,
            green / 255.0,
            blue / 255.0,
            1.0);
    }


    /**
     * This is a shortcut for {@code rgb(gray, gray, gray)}.
     */
    public static Color grayRgb(int gray) {
        return rgb(gray, gray, gray);
    }

    /**
     * This is a shortcut for {@code rgb(gray, gray, gray, opacity)}.
     */
    public static Color grayRgb(int gray, double opacity) {
        return rgb(gray, gray, gray, opacity);
    }

    /**
     * Creates a grey color.
     * @param gray color on gray scale in the range
     *             {@code 0.0} (black) - {@code 1.0} (white).
     * @param opacity the opacity component, in the range {@code 0.0-1.0}
     * @return the {@code Color}
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Color gray(double gray, double opacity) {
        return new Color(gray, gray, gray, opacity);
    }

    /**
     * Creates an opaque grey color.
     * @param gray color on gray scale in the range
     *             {@code 0.0} (black) - {@code 1.0} (white).
     * @return the {@code Color}
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Color gray(double gray) {
        return gray(gray, 1.0);
    }

    private static void checkRGB(int red, int green, int blue) {
        if (red < 0 || red > 255) {
            throw new IllegalArgumentException("Color.rgb's red parameter (" + red + ") expects color values 0-255");
        }
        if (green < 0 || green > 255) {
            throw new IllegalArgumentException("Color.rgb's green parameter (" + green + ") expects color values 0-255");
        }
        if (blue < 0 || blue > 255) {
            throw new IllegalArgumentException("Color.rgb's blue parameter (" + blue + ") expects color values 0-255");
        }
    }

    /**
     * Creates a {@code Color} based on the specified values in the HSB color model,
     * and a given opacity.
     *
     * @param hue the hue, in degrees
     * @param saturation the saturation, {@code 0.0 to 1.0}
     * @param brightness the brightness, {@code 0.0 to 1.0}
     * @param opacity the opacity, {@code 0.0 to 1.0}
     * @return the {@code Color}
     * @throws IllegalArgumentException if {@code saturation}, {@code brightness} or
     *         {@code opacity} are out of range
     */
    public static Color hsb(double hue, double saturation, double brightness, double opacity) {
        checkSB(saturation, brightness);
        double[] rgb = Utils.HSBtoRGB(hue, saturation, brightness);
        Color result = new Color(rgb[0], rgb[1], rgb[2], opacity);
        return result;
    }

    /**
     * Creates an opaque {@code Color} based on the specified values in the HSB color model.
     *
     * @param hue the hue, in degrees
     * @param saturation the saturation, {@code 0.0 to 1.0}
     * @param brightness the brightness, {@code 0.0 to 1.0}
     * @return the {@code Color}
     * @throws IllegalArgumentException if {@code saturation} or {@code brightness} are
     *         out of range
     */
    public static Color hsb(double hue, double saturation, double brightness) {
        return hsb(hue, saturation, brightness, 1.0);
    }

    private static void checkSB(double saturation, double brightness) {
        if (saturation < 0.0 || saturation > 1.0) {
            throw new IllegalArgumentException("Color.hsb's saturation parameter (" + saturation + ") expects values 0.0-1.0");
        }
        if (brightness < 0.0 || brightness > 1.0) {
            throw new IllegalArgumentException("Color.hsb's brightness parameter (" + brightness + ") expects values 0.0-1.0");
        }
    }

    /**
     * Creates an RGB color specified with hexadecimal notation or
     * color name.
     *
     * <p>Hexadecimal string contains values of red, green and blue channel.
     * Optionally, value of alfa channel can be added. The whole string can
     * be optionally prefixed by "0x" or "#".</p>
     *
     * <p>For colors without alpha channel and for named colors, opacity
     * is set according to the {@code opacity} argument. For colors with
     * alpha channel, the resulting opacity is a combination of alpha channel
     * and the {@code opacity} argument, so transparent color becomes more
     * transparent by specifying opacity.</p>
     *
     * <p>This method supports also short notation, in which each channel
     * is represented by only one hexadecimal character.</p>
     *
     * Examples:
     * <pre><code>
     * Color c = Color.web("0xff668840", 0.5);
     * Color c = Color.web("0xff6688", 0.5);
     * Color c = Color.web("#ff6688", 0.5);
     * Color c = Color.web("ff6688", 0.5);
     * Color c = Color.web("f68", 0.5);
     * Color c = Color.web("orange", 0.5);
     * </code></pre>
     *
     * @param colorRawName the hexadecimal string or color name
     * @param opacity the opacity component in range from 0.0 (transparent)
     *                to 1.0 (opaque)
     * @throws NullPointerException if {@code colorRawName} is {@code null}
     * @throws IllegalArgumentException if {@code colorRawName} specifies
     *      an unsupported color name or illegal hexadecimal value
     */
    public static Color web(String colorRawName, double opacity) {
        if (colorRawName == null) {
            throw new NullPointerException(
                    "The color components or name must be specified");
        }
        if (colorRawName.isEmpty()) {
            throw new IllegalArgumentException("Invalid color specification");
        }
        
        String color = colorRawName.toLowerCase();

        if (color.startsWith("#")) {
            color = color.substring(1);
        } else if (color.startsWith("0x")) {
            color = color.substring(2);
        } else {
            Color col = NamedColors.get(color);
            if (col != null) {
                if (opacity == 1.0) {
                    return col;
                } else {
                    return Color.color(col.red, col.green, col.blue, opacity);
                }
            }
        }

        int len = color.length();

        try {
            int r;
            int g;
            int b;
            int a;

            if (len == 3) {
                r = Integer.parseInt(color.substring(0, 1), 16);
                g = Integer.parseInt(color.substring(1, 2), 16);
                b = Integer.parseInt(color.substring(2, 3), 16);
                return Color.color(r / 15.0, g / 15.0, b / 15.0, opacity);
            } else if (len == 4) {
                r = Integer.parseInt(color.substring(0, 1), 16);
                g = Integer.parseInt(color.substring(1, 2), 16);
                b = Integer.parseInt(color.substring(2, 3), 16);
                a = Integer.parseInt(color.substring(3, 4), 16);
                return Color.color(r / 15.0, g / 15.0, b / 15.0,
                        opacity * a / 15.0);
            } else if (len == 6) {
                r = Integer.parseInt(color.substring(0, 2), 16);
                g = Integer.parseInt(color.substring(2, 4), 16);
                b = Integer.parseInt(color.substring(4, 6), 16);
                return Color.rgb(r, g, b, opacity);
            } else if (len == 8) {
                r = Integer.parseInt(color.substring(0, 2), 16);
                g = Integer.parseInt(color.substring(2, 4), 16);
                b = Integer.parseInt(color.substring(4, 6), 16);
                a = Integer.parseInt(color.substring(6, 8), 16);
                return Color.rgb(r, g, b, opacity * a / 255.0);
            }
        } catch (NumberFormatException nfe) {}

        throw new IllegalArgumentException("Invalid color specification");
    }

    /**
     * Creates an RGB color specified with hexadecimal notation or
     * color name.
     *
     * <p>Hexadecimal string contains values of red, green and blue channel.
     * Optionally, value of alfa channel can be added. The whole string can
     * be optionally prefixed by "0x" or "#".</p>
     *
     * <p>For colors without alpha channel and for named colors, opacity
     * is set to {@code 1.0} (opaque).</p>
     *
     * <p>This method supports also short notation, in which each channel
     * is represented by only one hexadecimal character.</p>
     *
     * Examples:
     * <pre><code>
     * Color c = Color.web("0xff668840");
     * Color c = Color.web("0xff6688");
     * Color c = Color.web("#ff6688");
     * Color c = Color.web("ff6688");
     * Color c = Color.web("f68");
     * Color c = Color.web("orange");
     * </code></pre>
     *
     * @param color the hexadecimal string or color name
     */
    public static Color web(String color) {
        return web(color, 1.0);
    }

    /**
     * Creates a color value from a string representation. The format
     * of the string representation is the same as in {@link #web(String)}.
     *
     * @param value the string to convert
     * @throws NullPointerException if the {@code value} is {@code null}
     * @throws IllegalArgumentException if the {@code value} specifies
     *      an unsupported color name or illegal hexadecimal value
     * @return a {@code Color} object holding the value represented 
     * by the string argument
     * @see #web(String)
     */
    public static Color valueOf(String value) {
        if (value == null) {
            throw new NullPointerException("color must be specified");
        }

        return web(value);
    }
    
    private static int to32BitInteger(int red, int green, int blue, int alpha) {
        int i = red;
        i = i << 8;
        i = i | green;
        i = i << 8;
        i = i | blue;
        i = i << 8;
        i = i | alpha;
        return i;
    }

    /**
     * Gets the hue component of this {@code Color}.
     * @return Hue value in the range in the range {@code 0.0-360.0}.
     */
    public double getHue() {
        return Utils.RGBtoHSB(red, green, blue)[0];
    }

    /**
     * Gets the saturation component of this {@code Color}.
     * @return Saturation value in the range in the range {@code 0.0-1.0}.
     */
    public double getSaturation() {
        return Utils.RGBtoHSB(red, green, blue)[1];
    }

    /**
     * Gets the brightness component of this {@code Color}.
     * @return Brightness value in the range in the range {@code 0.0-1.0}.
     */
    public double getBrightness() {
        return Utils.RGBtoHSB(red, green, blue)[2];
    }

    /**
     * Creates a new {@code Color} based on this {@code Color} with hue,
     * saturation, brightness and opacity values altered. Hue is shifted
     * about the given value and normalized into its natural range, the
     * other components' values are multiplied by the given factors and
     * clipped into their ranges.
     *
     * Increasing brightness of black color is allowed by using an arbitrary,
     * very small source brightness instead of zero.
     */
    public Color deriveColor(double hueShift, double saturationFactor,
            double brightnessFactor, double opacityFactor) {

        double[] hsb = Utils.RGBtoHSB(red, green, blue);

        /* Allow brightness increase of black color */
        double b = hsb[2];
        if (b == 0 && brightnessFactor > 1.0) {
            b = 0.05;
        }

        /* the tail "+ 360) % 360" solves shifts into negative numbers */
        double h = (((hsb[0] + hueShift) % 360) + 360) % 360;
        double s = Math.max(Math.min(hsb[1] * saturationFactor, 1.0), 0.0);
        b = Math.max(Math.min(b * brightnessFactor, 1.0), 0.0);
        double a = Math.max(Math.min(opacity * opacityFactor, 1.0), 0.0);
        return hsb(h, s, b, a);
    }

    /**
     * Creates a new Color that is a brighter version of this Color.
     */
    public Color brighter() {
        return deriveColor(0, 1.0, 1.0 / DARKER_BRIGHTER_FACTOR, 1.0);
    }

    /**
     * Creates a new Color that is a darker version of this Color.
     */
    public Color darker() {
        return deriveColor(0, 1.0, DARKER_BRIGHTER_FACTOR, 1.0);
    }

    /**
     * Creates a new Color that is a more saturated version of this Color.
     */
    public Color saturate() {
        return deriveColor(0, 1.0 / SATURATE_DESATURATE_FACTOR, 1.0, 1.0);
    }

    /**
     * Creates a new Color that is a less saturated version of this Color.
     */
    public Color desaturate() {
        return deriveColor(0, SATURATE_DESATURATE_FACTOR, 1.0, 1.0);
    }

    /**
     * Creates a new Color that is grayscale equivalent of this Color.
     * Opacity is preserved.
     */
    public Color grayscale() {
        double gray = 0.21 * red + 0.71 * green + 0.07 * blue;
        return Color.color(gray, gray, gray, opacity);
    }

    /**
     * Creates a new Color that is inversion of this Color.
     * Opacity is preserved.
     */
    public Color invert() {
        return Color.color(1.0 - red, 1.0 - green, 1.0 - blue, opacity);
    }

    /**
     * A fully transparent color with an ARGB value of #00000000.
     */
    public static final Color TRANSPARENT          = color(0.0, 0.0, 0.0, 0.0);

    /**
     * The color alice blue with an RGB value of #F0F8FF.
     */
    public static final Color ALICEBLUE            = rgb(0xF0, 0xF8, 0xFF);

    /**
     * The color antique white with an RGB value of #FAEBD7.
     */
    public static final Color ANTIQUEWHITE         = rgb(0xFA, 0xEB, 0xD7);

    /**
     * The color aqua with an RGB value of #00FFFF.
     */
    public static final Color AQUA                 = rgb(0x00, 0xFF, 0xFF);

    /**
     * The color aquamarine with an RGB value of #7FFFD4.
     */
    public static final Color AQUAMARINE           = rgb(0x7F, 0xFF, 0xD4);

    /**
     * The color azure with an RGB value of #F0FFFF.
     */
    public static final Color AZURE                = rgb(0xF0, 0xFF, 0xFF);

    /**
     * The color beige with an RGB value of #F5F5DC.
     */
    public static final Color BEIGE                = rgb(0xF5, 0xF5, 0xDC);

    /**
     * The color bisque with an RGB value of #FFE4C4.
     */
    public static final Color BISQUE               = rgb(0xFF, 0xE4, 0xC4);

    /**
     * The color black with an RGB value of #000000.
     */
    public static final Color BLACK                = rgb(0x00, 0x00, 0x00);

    /**
     * The color blanched almond with an RGB value of #FFEBCD.
     */
    public static final Color BLANCHEDALMOND       = rgb(0xFF, 0xEB, 0xCD);

    /**
     * The color blue with an RGB value of #0000FF.
     */
    public static final Color BLUE                 = rgb(0x00, 0x00, 0xFF);

    /**
     * The color blue violet with an RGB value of #8A2BE2.
     */
    public static final Color BLUEVIOLET           = rgb(0x8A, 0x2B, 0xE2);

    /**
     * The color brown with an RGB value of #A52A2A.
     */
    public static final Color BROWN                = rgb(0xA5, 0x2A, 0x2A);

    /**
     * The color burly wood with an RGB value of #DEB887.
     */
    public static final Color BURLYWOOD            = rgb(0xDE, 0xB8, 0x87);

    /**
     * The color cadet blue with an RGB value of #5F9EA0.
     */
    public static final Color CADETBLUE            = rgb(0x5F, 0x9E, 0xA0);

    /**
     * The color chartreuse with an RGB value of #7FFF00.
     */
    public static final Color CHARTREUSE           = rgb(0x7F, 0xFF, 0x00);

    /**
     * The color chocolate with an RGB value of #D2691E.
     */
    public static final Color CHOCOLATE            = rgb(0xD2, 0x69, 0x1E);

    /**
     * The color coral with an RGB value of #FF7F50.
     */
    public static final Color CORAL                = rgb(0xFF, 0x7F, 0x50);

    /**
     * The color cornflower blue with an RGB value of #6495ED.
     */
    public static final Color CORNFLOWERBLUE       = rgb(0x64, 0x95, 0xED);

    /**
     * The color cornsilk with an RGB value of #FFF8DC.
     */
    public static final Color CORNSILK             = rgb(0xFF, 0xF8, 0xDC);

    /**
     * The color crimson with an RGB value of #DC143C.
     */
    public static final Color CRIMSON              = rgb(0xDC, 0x14, 0x3C);

    /**
     * The color cyan with an RGB value of #00FFFF.
     */
    public static final Color CYAN                 = rgb(0x00, 0xFF, 0xFF);

    /**
     * The color dark blue with an RGB value of #00008B.
     */
    public static final Color DARKBLUE             = rgb(0x00, 0x00, 0x8B);

    /**
     * The color dark cyan with an RGB value of #008B8B.
     */
    public static final Color DARKCYAN             = rgb(0x00, 0x8B, 0x8B);

    /**
     * The color dark goldenrod with an RGB value of #B8860B.
     */
    public static final Color DARKGOLDENROD        = rgb(0xB8, 0x86, 0x0B);

    /**
     * The color dark gray with an RGB value of #A9A9A9.
     */
    public static final Color DARKGRAY             = rgb(0xA9, 0xA9, 0xA9);

    /**
     * The color dark green with an RGB value of #006400.
     */
    public static final Color DARKGREEN            = rgb(0x00, 0x64, 0x00);

    /**
     * The color dark grey with an RGB value of #A9A9A9.
     */
    public static final Color DARKGREY             = DARKGRAY;

    /**
     * The color dark khaki with an RGB value of #BDB76B.
     */
    public static final Color DARKKHAKI            = rgb(0xBD, 0xB7, 0x6B);

    /**
     * The color dark magenta with an RGB value of #8B008B.
     */
    public static final Color DARKMAGENTA          = rgb(0x8B, 0x00, 0x8B);

    /**
     * The color dark olive green with an RGB value of #556B2F.
     */
    public static final Color DARKOLIVEGREEN       = rgb(0x55, 0x6B, 0x2F);

    /**
     * The color dark orange with an RGB value of #FF8C00.
     */
    public static final Color DARKORANGE           = rgb(0xFF, 0x8C, 0x00);

    /**
     * The color dark orchid with an RGB value of #9932CC.
     */
    public static final Color DARKORCHID           = rgb(0x99, 0x32, 0xCC);

    /**
     * The color dark red with an RGB value of #8B0000.
     */
    public static final Color DARKRED              = rgb(0x8B, 0x00, 0x00);

    /**
     * The color dark salmon with an RGB value of #E9967A.
     */
    public static final Color DARKSALMON           = rgb(0xE9, 0x96, 0x7A);

    /**
     * The color dark sea green with an RGB value of #8FBC8F.
     */
    public static final Color DARKSEAGREEN         = rgb(0x8F, 0xBC, 0x8F);

    /**
     * The color dark slate blue with an RGB value of #483D8B.
     */
    public static final Color DARKSLATEBLUE        = rgb(0x48, 0x3D, 0x8B);

    /**
     * The color dark slate gray with an RGB value of #2F4F4F.
     */
    public static final Color DARKSLATEGRAY        = rgb(0x2F, 0x4F, 0x4F);

    /**
     * The color dark slate grey with an RGB value of #2F4F4F.
     */
    public static final Color DARKSLATEGREY        = DARKSLATEGRAY;

    /**
     * The color dark turquoise with an RGB value of #00CED1.
     */
    public static final Color DARKTURQUOISE        = rgb(0x00, 0xCE, 0xD1);

    /**
     * The color dark violet with an RGB value of #9400D3.
     */
    public static final Color DARKVIOLET           = rgb(0x94, 0x00, 0xD3);

    /**
     * The color deep pink with an RGB value of #FF1493.
     */
    public static final Color DEEPPINK             = rgb(0xFF, 0x14, 0x93);

    /**
     * The color deep sky blue with an RGB value of #00BFFF.
     */
    public static final Color DEEPSKYBLUE          = rgb(0x00, 0xBF, 0xFF);

    /**
     * The color dim gray with an RGB value of #696969.
     */
    public static final Color DIMGRAY              = rgb(0x69, 0x69, 0x69);

    /**
     * The color dim grey with an RGB value of #696969.
     */
    public static final Color DIMGREY              = DIMGRAY;

    /**
     * The color dodger blue with an RGB value of #1E90FF.
     */
    public static final Color DODGERBLUE           = rgb(0x1E, 0x90, 0xFF);

    /**
     * The color firebrick with an RGB value of #B22222.
     */
    public static final Color FIREBRICK            = rgb(0xB2, 0x22, 0x22);

    /**
     * The color floral white with an RGB value of #FFFAF0.
     */
    public static final Color FLORALWHITE          = rgb(0xFF, 0xFA, 0xF0);

    /**
     * The color forest green with an RGB value of #228B22.
     */
    public static final Color FORESTGREEN          = rgb(0x22, 0x8B, 0x22);

    /**
     * The color fuchsia with an RGB value of #FF00FF.
     */
    public static final Color FUCHSIA              = rgb(0xFF, 0x00, 0xFF);

    /**
     * The color gainsboro with an RGB value of #DCDCDC.
     */
    public static final Color GAINSBORO            = rgb(0xDC, 0xDC, 0xDC);

    /**
     * The color ghost white with an RGB value of #F8F8FF.
     */
    public static final Color GHOSTWHITE           = rgb(0xF8, 0xF8, 0xFF);

    /**
     * The color gold with an RGB value of #FFD700.
     */
    public static final Color GOLD                 = rgb(0xFF, 0xD7, 0x00);

    /**
     * The color goldenrod with an RGB value of #DAA520.
     */
    public static final Color GOLDENROD            = rgb(0xDA, 0xA5, 0x20);

    /**
     * The color gray with an RGB value of #808080.
     */
    public static final Color GRAY                 = rgb(0x80, 0x80, 0x80);

    /**
     * The color green with an RGB value of #008000.
     */
    public static final Color GREEN                = rgb(0x00, 0x80, 0x00);

    /**
     * The color green yellow with an RGB value of #ADFF2F.
     */
    public static final Color GREENYELLOW          = rgb(0xAD, 0xFF, 0x2F);

    /**
     * The color grey with an RGB value of #808080.
     */
    public static final Color GREY                 = GRAY;

    /**
     * The color honeydew with an RGB value of #F0FFF0.
     */
    public static final Color HONEYDEW             = rgb(0xF0, 0xFF, 0xF0);

    /**
     * The color hot pink with an RGB value of #FF69B4.
     */
    public static final Color HOTPINK              = rgb(0xFF, 0x69, 0xB4);

    /**
     * The color indian red with an RGB value of #CD5C5C.
     */
    public static final Color INDIANRED            = rgb(0xCD, 0x5C, 0x5C);

    /**
     * The color indigo with an RGB value of #4B0082.
     */
    public static final Color INDIGO               = rgb(0x4B, 0x00, 0x82);

    /**
     * The color ivory with an RGB value of #FFFFF0.
     */
    public static final Color IVORY                = rgb(0xFF, 0xFF, 0xF0);

    /**
     * The color khaki with an RGB value of #F0E68C.
     */
    public static final Color KHAKI                = rgb(0xF0, 0xE6, 0x8C);

    /**
     * The color lavender with an RGB value of #E6E6FA.
     */
    public static final Color LAVENDER             = rgb(0xE6, 0xE6, 0xFA);

    /**
     * The color lavender blush with an RGB value of #FFF0F5.
     */
    public static final Color LAVENDERBLUSH        = rgb(0xFF, 0xF0, 0xF5);

    /**
     * The color lawn green with an RGB value of #7CFC00.
     */
    public static final Color LAWNGREEN            = rgb(0x7C, 0xFC, 0x00);

    /**
     * The color lemon chiffon with an RGB value of #FFFACD.
     */
    public static final Color LEMONCHIFFON         = rgb(0xFF, 0xFA, 0xCD);

    /**
     * The color light blue with an RGB value of #ADD8E6.
     */
    public static final Color LIGHTBLUE            = rgb(0xAD, 0xD8, 0xE6);

    /**
     * The color light coral with an RGB value of #F08080.
     */
    public static final Color LIGHTCORAL           = rgb(0xF0, 0x80, 0x80);

    /**
     * The color light cyan with an RGB value of #E0FFFF.
     */
    public static final Color LIGHTCYAN            = rgb(0xE0, 0xFF, 0xFF);

    /**
     * The color light goldenrod yellow with an RGB value of #FAFAD2.
     */
    public static final Color LIGHTGOLDENRODYELLOW = rgb(0xFA, 0xFA, 0xD2);

    /**
     * The color light gray with an RGB value of #D3D3D3.
     */
    public static final Color LIGHTGRAY            = rgb(0xD3, 0xD3, 0xD3);

    /**
     * The color light green with an RGB value of #90EE90.
     */
    public static final Color LIGHTGREEN           = rgb(0x90, 0xEE, 0x90);

    /**
     * The color light grey with an RGB value of #D3D3D3.
     */
    public static final Color LIGHTGREY            = LIGHTGRAY;

    /**
     * The color light pink with an RGB value of #FFB6C1.
     */
    public static final Color LIGHTPINK            = rgb(0xFF, 0xB6, 0xC1);

    /**
     * The color light salmon with an RGB value of #FFA07A.
     */
    public static final Color LIGHTSALMON          = rgb(0xFF, 0xA0, 0x7A);

    /**
     * The color light sea green with an RGB value of #20B2AA.
     */
    public static final Color LIGHTSEAGREEN        = rgb(0x20, 0xB2, 0xAA);

    /**
     * The color light sky blue with an RGB value of #87CEFA.
     */
    public static final Color LIGHTSKYBLUE         = rgb(0x87, 0xCE, 0xFA);

    /**
     * The color light slate gray with an RGB value of #778899.
     */
    public static final Color LIGHTSLATEGRAY       = rgb(0x77, 0x88, 0x99);

    /**
     * The color light slate grey with an RGB value of #778899.
     */
    public static final Color LIGHTSLATEGREY       = LIGHTSLATEGRAY;

    /**
     * The color light steel blue with an RGB value of #B0C4DE.
     */
    public static final Color LIGHTSTEELBLUE       = rgb(0xB0, 0xC4, 0xDE);

    /**
     * The color light yellow with an RGB value of #FFFFE0.
     */
    public static final Color LIGHTYELLOW          = rgb(0xFF, 0xFF, 0xE0);

    /**
     * The color lime with an RGB value of #00FF00.
     */
    public static final Color LIME                 = rgb(0x00, 0xFF, 0x00);

    /**
     * The color lime green with an RGB value of #32CD32.
     */
    public static final Color LIMEGREEN            = rgb(0x32, 0xCD, 0x32);

    /**
     * The color linen with an RGB value of #FAF0E6.
     */
    public static final Color LINEN                = rgb(0xFA, 0xF0, 0xE6);

    /**
     * The color magenta with an RGB value of #FF00FF.
     */
    public static final Color MAGENTA              = rgb(0xFF, 0x00, 0xFF);

    /**
     * The color maroon with an RGB value of #800000.
     */
    public static final Color MAROON               = rgb(0x80, 0x00, 0x00);

    /**
     * The color medium aquamarine with an RGB value of #66CDAA.
     */
    public static final Color MEDIUMAQUAMARINE     = rgb(0x66, 0xCD, 0xAA);

    /**
     * The color medium blue with an RGB value of #0000CD.
     */
    public static final Color MEDIUMBLUE           = rgb(0x00, 0x00, 0xCD);

    /**
     * The color medium orchid with an RGB value of #BA55D3.
     */
    public static final Color MEDIUMORCHID         = rgb(0xBA, 0x55, 0xD3);

    /**
     * The color medium purple with an RGB value of #9370DB.
     */
    public static final Color MEDIUMPURPLE         = rgb(0x93, 0x70, 0xDB);

    /**
     * The color medium sea green with an RGB value of #3CB371.
     */
    public static final Color MEDIUMSEAGREEN       = rgb(0x3C, 0xB3, 0x71);

    /**
     * The color medium slate blue with an RGB value of #7B68EE.
     */
    public static final Color MEDIUMSLATEBLUE      = rgb(0x7B, 0x68, 0xEE);

    /**
     * The color medium spring green with an RGB value of #00FA9A.
     */
    public static final Color MEDIUMSPRINGGREEN    = rgb(0x00, 0xFA, 0x9A);

    /**
     * The color medium turquoise with an RGB value of #48D1CC.
     */
    public static final Color MEDIUMTURQUOISE      = rgb(0x48, 0xD1, 0xCC);

    /**
     * The color medium violet red with an RGB value of #C71585.
     */
    public static final Color MEDIUMVIOLETRED      = rgb(0xC7, 0x15, 0x85);

    /**
     * The color midnight blue with an RGB value of #191970.
     */
    public static final Color MIDNIGHTBLUE         = rgb(0x19, 0x19, 0x70);

    /**
     * The color mint cream with an RGB value of #F5FFFA.
     */
    public static final Color MINTCREAM            = rgb(0xF5, 0xFF, 0xFA);

    /**
     * The color misty rose with an RGB value of #FFE4E1.
     */
    public static final Color MISTYROSE            = rgb(0xFF, 0xE4, 0xE1);

    /**
     * The color moccasin with an RGB value of #FFE4B5.
     */
    public static final Color MOCCASIN             = rgb(0xFF, 0xE4, 0xB5);

    /**
     * The color navajo white with an RGB value of #FFDEAD.
     */
    public static final Color NAVAJOWHITE          = rgb(0xFF, 0xDE, 0xAD);

    /**
     * The color navy with an RGB value of #000080.
     */
    public static final Color NAVY                 = rgb(0x00, 0x00, 0x80);

    /**
     * The color old lace with an RGB value of #FDF5E6.
     */
    public static final Color OLDLACE              = rgb(0xFD, 0xF5, 0xE6);

    /**
     * The color olive with an RGB value of #808000.
     */
    public static final Color OLIVE                = rgb(0x80, 0x80, 0x00);

    /**
     * The color olive drab with an RGB value of #6B8E23.
     */
    public static final Color OLIVEDRAB            = rgb(0x6B, 0x8E, 0x23);

    /**
     * The color orange with an RGB value of #FFA500.
     */
    public static final Color ORANGE               = rgb(0xFF, 0xA5, 0x00);

    /**
     * The color orange red with an RGB value of #FF4500.
     */
    public static final Color ORANGERED            = rgb(0xFF, 0x45, 0x00);

    /**
     * The color orchid with an RGB value of #DA70D6.
     */
    public static final Color ORCHID               = rgb(0xDA, 0x70, 0xD6);

    /**
     * The color pale goldenrod with an RGB value of #EEE8AA.
     */
    public static final Color PALEGOLDENROD        = rgb(0xEE, 0xE8, 0xAA);

    /**
     * The color pale green with an RGB value of #98FB98.
     */
    public static final Color PALEGREEN            = rgb(0x98, 0xFB, 0x98);

    /**
     * The color pale turquoise with an RGB value of #AFEEEE.
     */
    public static final Color PALETURQUOISE        = rgb(0xAF, 0xEE, 0xEE);

    /**
     * The color pale violet red with an RGB value of #DB7093.
     */
    public static final Color PALEVIOLETRED        = rgb(0xDB, 0x70, 0x93);

    /**
     * The color papaya whip with an RGB value of #FFEFD5.
     */
    public static final Color PAPAYAWHIP           = rgb(0xFF, 0xEF, 0xD5);

    /**
     * The color peach puff with an RGB value of #FFDAB9.
     */
    public static final Color PEACHPUFF            = rgb(0xFF, 0xDA, 0xB9);

    /**
     * The color peru with an RGB value of #CD853F.
     */
    public static final Color PERU                 = rgb(0xCD, 0x85, 0x3F);

    /**
     * The color pink with an RGB value of #FFC0CB.
     */
    public static final Color PINK                 = rgb(0xFF, 0xC0, 0xCB);

    /**
     * The color plum with an RGB value of #DDA0DD.
     */
    public static final Color PLUM                 = rgb(0xDD, 0xA0, 0xDD);

    /**
     * The color powder blue with an RGB value of #B0E0E6.
     */
    public static final Color POWDERBLUE           = rgb(0xB0, 0xE0, 0xE6);

    /**
     * The color purple with an RGB value of #800080.
     */
    public static final Color PURPLE               = rgb(0x80, 0x00, 0x80);

    /**
     * The color red with an RGB value of #FF0000.
     */
    public static final Color RED                  = rgb(0xFF, 0x00, 0x00);

    /**
     * The color rosy brown with an RGB value of #BC8F8F.
     */
    public static final Color ROSYBROWN            = rgb(0xBC, 0x8F, 0x8F);

    /**
     * The color royal blue with an RGB value of #4169E1.
     */
    public static final Color ROYALBLUE            = rgb(0x41, 0x69, 0xE1);

    /**
     * The color saddle brown with an RGB value of #8B4513.
     */
    public static final Color SADDLEBROWN          = rgb(0x8B, 0x45, 0x13);

    /**
     * The color salmon with an RGB value of #FA8072.
     */
    public static final Color SALMON               = rgb(0xFA, 0x80, 0x72);

    /**
     * The color sandy brown with an RGB value of #F4A460.
     */
    public static final Color SANDYBROWN           = rgb(0xF4, 0xA4, 0x60);

    /**
     * The color sea green with an RGB value of #2E8B57.
     */
    public static final Color SEAGREEN             = rgb(0x2E, 0x8B, 0x57);

    /**
     * The color sea shell with an RGB value of #FFF5EE.
     */
    public static final Color SEASHELL             = rgb(0xFF, 0xF5, 0xEE);

    /**
     * The color sienna with an RGB value of #A0522D.
     */
    public static final Color SIENNA               = rgb(0xA0, 0x52, 0x2D);

    /**
     * The color silver with an RGB value of #C0C0C0.
     */
    public static final Color SILVER               = rgb(0xC0, 0xC0, 0xC0);

    /**
     * The color sky blue with an RGB value of #87CEEB.
     */
    public static final Color SKYBLUE              = rgb(0x87, 0xCE, 0xEB);

    /**
     * The color slate blue with an RGB value of #6A5ACD.
     */
    public static final Color SLATEBLUE            = rgb(0x6A, 0x5A, 0xCD);

    /**
     * The color slate gray with an RGB value of #708090.
     */
    public static final Color SLATEGRAY            = rgb(0x70, 0x80, 0x90);

    /**
     * The color slate grey with an RGB value of #708090.
     */
    public static final Color SLATEGREY            = SLATEGRAY;

    /**
     * The color snow with an RGB value of #FFFAFA.
     */
    public static final Color SNOW                 = rgb(0xFF, 0xFA, 0xFA);

    /**
     * The color spring green with an RGB value of #00FF7F.
     */
    public static final Color SPRINGGREEN          = rgb(0x00, 0xFF, 0x7F);

    /**
     * The color steel blue with an RGB value of #4682B4.
     */
    public static final Color STEELBLUE            = rgb(0x46, 0x82, 0xB4);

    /**
     * The color tan with an RGB value of #D2B48C.
     */
    public static final Color TAN                  = rgb(0xD2, 0xB4, 0x8C);

    /**
     * The color teal with an RGB value of #008080.
     */
    public static final Color TEAL                 = rgb(0x00, 0x80, 0x80);

    /**
     * The color thistle with an RGB value of #D8BFD8.
     */
    public static final Color THISTLE              = rgb(0xD8, 0xBF, 0xD8);

    /**
     * The color tomato with an RGB value of #FF6347.
     */
    public static final Color TOMATO               = rgb(0xFF, 0x63, 0x47);

    /**
     * The color turquoise with an RGB value of #40E0D0.
     */
    public static final Color TURQUOISE            = rgb(0x40, 0xE0, 0xD0);

    /**
     * The color violet with an RGB value of #EE82EE.
     */
    public static final Color VIOLET               = rgb(0xEE, 0x82, 0xEE);

    /**
     * The color wheat with an RGB value of #F5DEB3.
     */
    public static final Color WHEAT                = rgb(0xF5, 0xDE, 0xB3);

    /**
     * The color white with an RGB value of #FFFFFF.
     */
    public static final Color WHITE                = rgb(0xFF, 0xFF, 0xFF);

    /**
     * The color white smoke with an RGB value of #F5F5F5.
     */
    public static final Color WHITESMOKE           = rgb(0xF5, 0xF5, 0xF5);

    /**
     * The color yellow with an RGB value of #FFFF00.
     */
    public static final Color YELLOW               = rgb(0xFF, 0xFF, 0x00);

    /**
     * The color yellow green with an RGB value of #9ACD32.
     */
    public static final Color YELLOWGREEN          = rgb(0x9A, 0xCD, 0x32);

    /*
     * Named colors moved to nested class to initialize them only when they
     * are needed.
     */
    private static final class NamedColors {
        private static final Map<String, Color> namedColors =
                createNamedColors();

        private NamedColors() {
        }
        
        private static Color get(String name) {
            return namedColors.get(name);
        }

        private static Map<String, Color> createNamedColors() {
            Map<String, Color> colors = new HashMap<String,Color>(256);
            
            colors.put("aliceblue",            ALICEBLUE);
            colors.put("antiquewhite",         ANTIQUEWHITE);
            colors.put("aqua",                 AQUA);
            colors.put("aquamarine",           AQUAMARINE);
            colors.put("azure",                AZURE);
            colors.put("beige",                BEIGE);
            colors.put("bisque",               BISQUE);
            colors.put("black",                BLACK);
            colors.put("blanchedalmond",       BLANCHEDALMOND);
            colors.put("blue",                 BLUE);
            colors.put("blueviolet",           BLUEVIOLET);
            colors.put("brown",                BROWN);
            colors.put("burlywood",            BURLYWOOD);
            colors.put("cadetblue",            CADETBLUE);
            colors.put("chartreuse",           CHARTREUSE);
            colors.put("chocolate",            CHOCOLATE);
            colors.put("coral",                CORAL);
            colors.put("cornflowerblue",       CORNFLOWERBLUE);
            colors.put("cornsilk",             CORNSILK);
            colors.put("crimson",              CRIMSON);
            colors.put("cyan",                 CYAN);
            colors.put("darkblue",             DARKBLUE);
            colors.put("darkcyan",             DARKCYAN);
            colors.put("darkgoldenrod",        DARKGOLDENROD);
            colors.put("darkgray",             DARKGRAY);
            colors.put("darkgreen",            DARKGREEN);
            colors.put("darkgrey",             DARKGREY);
            colors.put("darkkhaki",            DARKKHAKI);
            colors.put("darkmagenta",          DARKMAGENTA);
            colors.put("darkolivegreen",       DARKOLIVEGREEN);
            colors.put("darkorange",           DARKORANGE);
            colors.put("darkorchid",           DARKORCHID);
            colors.put("darkred",              DARKRED);
            colors.put("darksalmon",           DARKSALMON);
            colors.put("darkseagreen",         DARKSEAGREEN);
            colors.put("darkslateblue",        DARKSLATEBLUE);
            colors.put("darkslategray",        DARKSLATEGRAY);
            colors.put("darkslategrey",        DARKSLATEGREY);
            colors.put("darkturquoise",        DARKTURQUOISE);
            colors.put("darkviolet",           DARKVIOLET);
            colors.put("deeppink",             DEEPPINK);
            colors.put("deepskyblue",          DEEPSKYBLUE);
            colors.put("dimgray",              DIMGRAY);
            colors.put("dimgrey",              DIMGREY);
            colors.put("dodgerblue",           DODGERBLUE);
            colors.put("firebrick",            FIREBRICK);
            colors.put("floralwhite",          FLORALWHITE);
            colors.put("forestgreen",          FORESTGREEN);
            colors.put("fuchsia",              FUCHSIA);
            colors.put("gainsboro",            GAINSBORO);
            colors.put("ghostwhite",           GHOSTWHITE);
            colors.put("gold",                 GOLD);
            colors.put("goldenrod",            GOLDENROD);
            colors.put("gray",                 GRAY);
            colors.put("green",                GREEN);
            colors.put("greenyellow",          GREENYELLOW);
            colors.put("grey",                 GREY);
            colors.put("honeydew",             HONEYDEW);
            colors.put("hotpink",              HOTPINK);
            colors.put("indianred",            INDIANRED);
            colors.put("indigo",               INDIGO);
            colors.put("ivory",                IVORY);
            colors.put("khaki",                KHAKI);
            colors.put("lavender",             LAVENDER);
            colors.put("lavenderblush",        LAVENDERBLUSH);
            colors.put("lawngreen",            LAWNGREEN);
            colors.put("lemonchiffon",         LEMONCHIFFON);
            colors.put("lightblue",            LIGHTBLUE);
            colors.put("lightcoral",           LIGHTCORAL);
            colors.put("lightcyan",            LIGHTCYAN);
            colors.put("lightgoldenrodyellow", LIGHTGOLDENRODYELLOW);
            colors.put("lightgray",            LIGHTGRAY);
            colors.put("lightgreen",           LIGHTGREEN);
            colors.put("lightgrey",            LIGHTGREY);
            colors.put("lightpink",            LIGHTPINK);
            colors.put("lightsalmon",          LIGHTSALMON);
            colors.put("lightseagreen",        LIGHTSEAGREEN);
            colors.put("lightskyblue",         LIGHTSKYBLUE);
            colors.put("lightslategray",       LIGHTSLATEGRAY);
            colors.put("lightslategrey",       LIGHTSLATEGREY);
            colors.put("lightsteelblue",       LIGHTSTEELBLUE);
            colors.put("lightyellow",          LIGHTYELLOW);
            colors.put("lime",                 LIME);
            colors.put("limegreen",            LIMEGREEN);
            colors.put("linen",                LINEN);
            colors.put("magenta",              MAGENTA);
            colors.put("maroon",               MAROON);
            colors.put("mediumaquamarine",     MEDIUMAQUAMARINE);
            colors.put("mediumblue",           MEDIUMBLUE);
            colors.put("mediumorchid",         MEDIUMORCHID);
            colors.put("mediumpurple",         MEDIUMPURPLE);
            colors.put("mediumseagreen",       MEDIUMSEAGREEN);
            colors.put("mediumslateblue",      MEDIUMSLATEBLUE);
            colors.put("mediumspringgreen",    MEDIUMSPRINGGREEN);
            colors.put("mediumturquoise",      MEDIUMTURQUOISE);
            colors.put("mediumvioletred",      MEDIUMVIOLETRED);
            colors.put("midnightblue",         MIDNIGHTBLUE);
            colors.put("mintcream",            MINTCREAM);
            colors.put("mistyrose",            MISTYROSE);
            colors.put("moccasin",             MOCCASIN);
            colors.put("navajowhite",          NAVAJOWHITE);
            colors.put("navy",                 NAVY);
            colors.put("oldlace",              OLDLACE);
            colors.put("olive",                OLIVE);
            colors.put("olivedrab",            OLIVEDRAB);
            colors.put("orange",               ORANGE);
            colors.put("orangered",            ORANGERED);
            colors.put("orchid",               ORCHID);
            colors.put("palegoldenrod",        PALEGOLDENROD);
            colors.put("palegreen",            PALEGREEN);
            colors.put("paleturquoise",        PALETURQUOISE);
            colors.put("palevioletred",        PALEVIOLETRED);
            colors.put("papayawhip",           PAPAYAWHIP);
            colors.put("peachpuff",            PEACHPUFF);
            colors.put("peru",                 PERU);
            colors.put("pink",                 PINK);
            colors.put("plum",                 PLUM);
            colors.put("powderblue",           POWDERBLUE);
            colors.put("purple",               PURPLE);
            colors.put("red",                  RED);
            colors.put("rosybrown",            ROSYBROWN);
            colors.put("royalblue",            ROYALBLUE);
            colors.put("saddlebrown",          SADDLEBROWN);
            colors.put("salmon",               SALMON);
            colors.put("sandybrown",           SANDYBROWN);
            colors.put("seagreen",             SEAGREEN);
            colors.put("seashell",             SEASHELL);
            colors.put("sienna",               SIENNA);
            colors.put("silver",               SILVER);
            colors.put("skyblue",              SKYBLUE);
            colors.put("slateblue",            SLATEBLUE);
            colors.put("slategray",            SLATEGRAY);
            colors.put("slategrey",            SLATEGREY);
            colors.put("snow",                 SNOW);
            colors.put("springgreen",          SPRINGGREEN);
            colors.put("steelblue",            STEELBLUE);
            colors.put("tan",                  TAN);
            colors.put("teal",                 TEAL);
            colors.put("thistle",              THISTLE);
            colors.put("tomato",               TOMATO);
            colors.put("transparent",          TRANSPARENT);
            colors.put("turquoise",            TURQUOISE);
            colors.put("violet",               VIOLET);
            colors.put("wheat",                WHEAT);
            colors.put("white",                WHITE);
            colors.put("whitesmoke",           WHITESMOKE);
            colors.put("yellow",               YELLOW);
            colors.put("yellowgreen",          YELLOWGREEN);

            return colors;
        }
    }
    
    /**
     * The red component of the {@code Color}, in the range {@code 0.0-1.0}.
     *
     * @defaultvalue 0.0
     */
    public final double getRed() { return red; }
    private float red;

    /**
     * The green component of the {@code Color}, in the range {@code 0.0-1.0}.
     *
     * @defaultvalue 0.0
     */
    public final double getGreen() { return green; }
    private float green;

    /**
     * The blue component of the {@code Color}, in the range {@code 0.0-1.0}.
     *
     * @defaultvalue 0.0
     */
    public final double getBlue() { return blue; }
    private float blue;

    /**
     * The opacity of the {@code Color}, in the range {@code 0.0-1.0}.
     *
     * @defaultvalue 1.0
     */
    public final double getOpacity() { return opacity; }
    private float opacity = 1;

    private Object platformPaint;

    /**
     * Creates a new isntance of color
     * @param red red component ranging from {@code 0} to {@code 1}
     * @param green green component ranging from {@code 0} to {@code 1}
     * @param blue blue component ranging from {@code 0} to {@code 1}
     * @param opacity opacity ranging from {@code 0} to {@code 1}
     */
    public Color(double red, double green, double blue, @Default("1") double opacity) {
        if (red < 0 || red > 1) {
            throw new IllegalArgumentException("Color's red value (" + red + ") must be in the range 0.0-1.0");
        }
        if (green < 0 || green > 1) {
            throw new IllegalArgumentException("Color's green value (" + green + ") must be in the range 0.0-1.0");
        }
        if (blue < 0 || blue > 1) {
            throw new IllegalArgumentException("Color's blue value (" + blue + ") must be in the range 0.0-1.0");
        }
        if (opacity < 0 || opacity > 1) {
            throw new IllegalArgumentException("Color's opacity value (" + opacity + ") must be in the range 0.0-1.0");
        }

        this.red = (float) red;
        this.green = (float) green;
        this.blue = (float) blue;
        this.opacity = (float) opacity;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public Object impl_getPlatformPaint() {
        if (platformPaint == null) {
            platformPaint = Toolkit.getToolkit().getPaint(this);
        }
        return platformPaint;
    }

    /**
     * @inheritDoc
     */
    @Override public Color interpolate(Color endValue, double t) {
        if (t <= 0.0) return this;
        if (t >= 1.0) return endValue;
        float ft = (float) t;
        return new Color(
            red     + (endValue.red     - red)     * ft,
            green   + (endValue.green   - green)   * ft,
            blue    + (endValue.blue    - blue)    * ft,
            opacity + (endValue.opacity - opacity) * ft
        );
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is equal to the {@code obj} argument; {@code false} otherwise.
     */
    @Override public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Color) {
            Color other = (Color) obj;
            return red == other.red
                && green == other.green
                && blue == other.blue
                && opacity == other.opacity;
        } else return false;
    }

    /**
     * Returns a hash code for this {@code Color} object.
     * @return a hash code for this {@code Color} object.
     */ 
    @Override public int hashCode() {
        // construct the 32bit integer representation of this color
        int r = (int)Math.round(red * 255.0);
        int g = (int)Math.round(green * 255.0);
        int b = (int)Math.round(blue * 255.0);
        int a = (int)Math.round(opacity * 255.0);
        return to32BitInteger(r, g, b, a);
    }

    /**
     * Returns a string representation of this {@code Color}.
     * This method is intended to be used only for informational purposes.
     * The content and format of the returned string might vary between implementations.
     * The returned string might be empty but cannot be {@code null}.
     *
     * @return the string representation
     */
    @Override public String toString() {
        int r = (int)Math.round(red * 255.0);
        int g = (int)Math.round(green * 255.0);
        int b = (int)Math.round(blue * 255.0);
        return "Color[red=" + r + ",green=" + g + ",blue=" + b + ",opacity=" + opacity + "]";
    }
}
