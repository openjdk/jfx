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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.javafx.scene.control.skin;

import java.security.AccessController;
import java.security.PrivilegedAction;

import static javafx.scene.control.OverrunStyle.*;
import javafx.application.Platform;
import javafx.application.ConditionalFeature;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.Mnemonic;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import com.sun.javafx.scene.control.behavior.TextBinding;

public class Utils {

    static Text helper = new Text();

    static double computeTextWidth(Font font, String text, double wrappingWidth) {
        helper.setText(text);
        helper.setFont(font);
        // Note that the wrapping width needs to be set to zero before
        // getting the text's real preferred width.
        helper.setWrappingWidth(0);
        helper.setLineSpacing(0);
        double w = Math.min(helper.prefWidth(-1), wrappingWidth);
        helper.setWrappingWidth((int)Math.ceil(w));
        return Math.ceil(helper.getLayoutBounds().getWidth());
    }

    static double computeTextHeight(Font font, String text, double wrappingWidth) {
        return computeTextHeight(font, text, wrappingWidth, 0);
    }

    static double computeTextHeight(Font font, String text, double wrappingWidth, double lineSpacing) {
        helper.setText(text);
        helper.setFont(font);
        helper.setWrappingWidth((int)wrappingWidth);
        helper.setLineSpacing((int)lineSpacing);
        return helper.getLayoutBounds().getHeight();
    }

    static int computeTruncationIndex(Font font, String text, double width) {
        helper.setText(text);
        helper.setFont(font);
        helper.setWrappingWidth(0);
        helper.setLineSpacing(0);
        // The -2 is a fudge to make sure the result more often matches
        // what we get from using computeTextWidth instead. It's not yet
        // clear what causes the small discrepancies.
        Bounds bounds = helper.getLayoutBounds();
        Point2D endPoint = new Point2D(width - 2, bounds.getMinY() + bounds.getHeight() / 2);
        return helper.impl_hitTestChar(endPoint).getCharIndex();
    }

    static String computeClippedText(Font font, String text, double width,
                                     OverrunStyle type, String ellipsisString) {
        if (font == null) {
            throw new IllegalArgumentException("Must specify a font");
        }
        OverrunStyle style = (type == null || type == CLIP) ? ELLIPSIS : type;
        final String ellipsis = (type == CLIP) ? "" : ellipsisString;
        // if the text is empty or null or no ellipsis, then it always fits
        if (text == null || "".equals(text)) {
            return text;
        }
        // if the string width is < the available width, then it fits and
        // doesn't need to be clipped.  We use a double point comparison
        // of 0.001 (1/1000th of a pixel) to account for any numerical
        // discrepancies introduced when the available width was calculated.
        // MenuItemSkinBase.doLayout, for example, does a number of double
        // point operations when computing the available width.
        final double stringWidth = computeTextWidth(font, text, 0);
        if (stringWidth - width < 0.0010F) {
            return text;
        }
        // the width used by the ellipsis string
        final double ellipsisWidth = computeTextWidth(font, ellipsis, 0);
        // the available maximum width to fit chars into. This is essentially
        // the width minus the space required for the ellipsis string
        final double availableWidth = width - ellipsisWidth;

        if (width < ellipsisWidth) {
            // The ellipsis doesn't fit.
            return "";
        }

        // if we got here, then we must clip the text with an ellipsis.
        // this can be pretty expensive depending on whether "complex" text
        // layout needs to be taken into account. So each ellipsis option has
        // to take into account two code paths: the easy way and the correct
        // way. This is flagged by the "complexLayout" boolean
        // TODO make sure this function call takes into account ligatures, kerning,
        // and such as that will change the layout characteristics of the text
        // and will require a full complex layout
        // TODO since we don't have all the stuff available in FX to determine
        // complex text, I'm going to for now assume complex text is always false.
        final boolean complexLayout = false;
        //requiresComplexLayout(font, text);

        // generally all we want to do is count characters and add their widths.
        // For ellipsis that breaks on words, we do NOT want to include any
        // hanging whitespace.
        if (style == ELLIPSIS ||
            style == WORD_ELLIPSIS ||
            style == LEADING_ELLIPSIS ||
            style == LEADING_WORD_ELLIPSIS) {

            final boolean wordTrim =
                (style == WORD_ELLIPSIS || style == LEADING_WORD_ELLIPSIS);
            String substring;
            if (complexLayout) {
            //            AttributedString a = new AttributedString(text);
            //            LineBreakMeasurer m = new LineBreakMeasurer(a.getIterator(), frc);
            //            substring = text.substring(0, m.nextOffset((double)availableWidth));
            } else {
                // RT-23458: Use a faster algorithm for the most common case
                // where truncation happens at the end, i.e. for ELLIPSIS and
                // CLIP, but not for other cases such as WORD_ELLIPSIS, etc.
                if (style == ELLIPSIS) {
                    int hit = computeTruncationIndex(font, text, width - ellipsisWidth);
                    if (hit < 0 || hit >= text.length()) {
                        return text;
                    } else {
                        return text.substring(0, hit) + ellipsis;
                    }
                }

                // simply total up the widths of all chars to determine how many
                // will fit in the available space. Remember the last whitespace
                // encountered so that if we're breaking on words we can trim
                // and omit it.
                double total = 0.0F;
                int whitespaceIndex = -1;
                // at the termination of the loop, index will be one past the
                // end of the substring
                int index = 0;
                int start = (style == LEADING_ELLIPSIS || style == LEADING_WORD_ELLIPSIS) ? (text.length() - 1) : (0);
                int end = (start == 0) ? (text.length() - 1) : 0;
                int stepValue = (start == 0) ? 1 : -1;
                boolean done = (start == 0) ? start > end : start < end;
                for (int i = start; !done ; i += stepValue) {
                    index = i;
                    char c = text.charAt(index);
                    total = computeTextWidth(font,
                                             (start == 0) ? text.substring(0, i + 1)
                                                          : text.substring(i, start + 1),
                                             0);
                    if (Character.isWhitespace(c)) {
                        whitespaceIndex = index;
                    }
                    if (total > availableWidth) {
                        break;
                    }
                    done = start == 0? i >= end : i <= end;
                }
                final boolean fullTrim = !wordTrim || whitespaceIndex == -1;
                substring = (start == 0) ?
                    (text.substring(0, fullTrim ? index : whitespaceIndex)) :
                        (text.substring((fullTrim ? index : whitespaceIndex) + 1));
                assert(!text.equals(substring));
            }
            if (style == ELLIPSIS || style == WORD_ELLIPSIS) {
                 return substring + ellipsis;
            } else {
                //style is LEADING_ELLIPSIS or LEADING_WORD_ELLIPSIS
                return ellipsis + substring;
            }
        } else {
            // these two indexes are INCLUSIVE not exclusive
            int leadingIndex = 0;
            int trailingIndex = 0;
            int leadingWhitespace = -1;
            int trailingWhitespace = -1;
            // The complex case is going to be killer. What I have to do is
            // read all the chars from the left up to the leadingIndex,
            // and all the chars from the right up to the trailingIndex,
            // and sum those together to get my total. That is, I cannot have
            // a running total but must retotal the cummulative chars each time
            if (complexLayout) {
            } else /*            double leadingTotal = 0;
               double trailingTotal = 0;
               for (int i=0; i<text.length(); i++) {
               double total = computeStringWidth(metrics, text.substring(0, i));
               if (total + trailingTotal > availableWidth) break;
               leadingIndex = i;
               leadingTotal = total;
               if (Character.isWhitespace(text.charAt(i))) leadingWhitespace = leadingIndex;

               int index = text.length() - (i + 1);
               total = computeStringWidth(metrics, text.substring(index - 1));
               if (total + leadingTotal > availableWidth) break;
               trailingIndex = index;
               trailingTotal = total;
               if (Character.isWhitespace(text.charAt(index))) trailingWhitespace = trailingIndex;
               }*/
            {
                // either CENTER_ELLIPSIS or CENTER_WORD_ELLIPSIS
                // for this case I read one char on the left, then one on the end
                // then second on the left, then second from the end, etc until
                // I have used up all the availableWidth. At that point, I trim
                // the string twice: once from the start to firstIndex, and
                // once from secondIndex to the end. I then insert the ellipsis
                // between the two.
                leadingIndex = -1;
                trailingIndex = -1;
                double total = 0.0F;
                for (int i = 0; i <= text.length() - 1; i++) {
                    char c = text.charAt(i);
                    //total += metrics.charWidth(c);
                    total += computeTextWidth(font, "" + c, 0);
                    if (total > availableWidth) {
                        break;
                    }
                    leadingIndex = i;
                    if (Character.isWhitespace(c)) {
                        leadingWhitespace = leadingIndex;
                    }
                    int index = text.length() - 1 - i;
                    c = text.charAt(index);
                    //total += metrics.charWidth(c);
                    total += computeTextWidth(font, "" + c, 0);
                    if (total > availableWidth) {
                        break;
                    }
                    trailingIndex = index;
                    if (Character.isWhitespace(c)) {
                        trailingWhitespace = trailingIndex;
                    }
                }
            }
            if (leadingIndex < 0) {
                return ellipsis;
            }
            if (style == CENTER_ELLIPSIS) {
                if (trailingIndex < 0) {
                    return text.substring(0, leadingIndex + 1) + ellipsis;
                }
                return text.substring(0, leadingIndex + 1) + ellipsis + text.substring(trailingIndex);
            } else {
                boolean leadingIndexIsLastLetterInWord =
                    Character.isWhitespace(text.charAt(leadingIndex + 1));
                int index = (leadingWhitespace == -1 || leadingIndexIsLastLetterInWord) ? (leadingIndex + 1) : (leadingWhitespace);
                String leading = text.substring(0, index);
                if (trailingIndex < 0) {
                    return leading + ellipsis;
                }
                boolean trailingIndexIsFirstLetterInWord =
                    Character.isWhitespace(text.charAt(trailingIndex - 1));
                index = (trailingWhitespace == -1 || trailingIndexIsFirstLetterInWord) ? (trailingIndex) : (trailingWhitespace + 1);
                String trailing = text.substring(index);
                return leading + ellipsis + trailing;
            }
        }
    }

    static String computeClippedWrappedText(Font font, String text, double width,
                                            double height, OverrunStyle truncationStyle,
                                            String ellipsisString) {
        if (font == null) {
            throw new IllegalArgumentException("Must specify a font");
        }

        String ellipsis = (truncationStyle == CLIP) ? "" : ellipsisString;
        int eLen = ellipsis.length();
        // Do this before using helper, as it's not reentrant.
        double eWidth = computeTextWidth(font, ellipsis, 0);
        double eHeight = computeTextHeight(font, ellipsis, 0);

        if (width < eWidth || height < eHeight) {
            // The ellipsis doesn't fit.
            return "";
        }

        helper.setText(text);
        helper.setFont(font);
        helper.setWrappingWidth((int)Math.ceil(width));
        helper.setLineSpacing(0);

        boolean leading =  (truncationStyle == LEADING_ELLIPSIS ||
                            truncationStyle == LEADING_WORD_ELLIPSIS);
        boolean center =   (truncationStyle == CENTER_ELLIPSIS ||
                            truncationStyle == CENTER_WORD_ELLIPSIS);
        boolean trailing = !(leading || center);
        boolean wordTrim = (truncationStyle == WORD_ELLIPSIS ||
                            truncationStyle == LEADING_WORD_ELLIPSIS ||
                            truncationStyle == CENTER_WORD_ELLIPSIS);

        String result = text;
        int len = (result != null) ? result.length() : 0;
        int centerLen = -1;

        Point2D centerPoint = null;
        if (center) {
            // Find index of character in the middle of the visual text area
            centerPoint = new Point2D((width - eWidth) / 2, height / 2 - helper.getBaselineOffset());
        }

        // Find index of character at the bottom left of the text area.
        // This should be the first character of a line that would be clipped.
        Point2D endPoint = new Point2D(0, height - helper.getBaselineOffset());

        int hit = helper.impl_hitTestChar(endPoint).getCharIndex();
        if (hit >= len) {
            return text;
        }
        if (center) {
            hit = helper.impl_hitTestChar(centerPoint).getCharIndex();
        }

        if (hit > 0 && hit < len) {
            // Step one, make a truncation estimate.

            if (center || trailing) {
                int ind = hit;
                if (center) {
                    // This is for the first part, i.e. beginning of text up to ellipsis.
                    if (wordTrim) {
                        int brInd = lastBreakCharIndex(text, ind + 1);
                        if (brInd >= 0) {
                            ind = brInd + 1;
                        } else {
                            brInd = firstBreakCharIndex(text, ind);
                            if (brInd >= 0) {
                                ind = brInd + 1;
                            }
                        }
                    }
                    centerLen = ind + eLen;
                } // else: text node wraps at words, so wordTrim is not needed here.
                result = result.substring(0, ind) + ellipsis;
            }

            if (leading || center) {
                // The hit is an index counted from the beginning, but we need
                // the opposite, i.e. an index counted from the end.  However,
                // the Text node does not support wrapped line layout in the
                // reverse direction, starting at the bottom right corner.

                // We'll simulate by assuming the index will be a similar
                // number, then back up 10 characters just to add some slop.
                // For example, the ending lines might pack tighter than the
                // beginning lines, and therefore fit a higher number of
                // characters.
                int ind = Math.max(0, len - hit - 10);
                if (ind > 0 && wordTrim) {
                    int brInd = lastBreakCharIndex(text, ind + 1);
                    if (brInd >= 0) {
                        ind = brInd + 1;
                    } else {
                        brInd = firstBreakCharIndex(text, ind);
                        if (brInd >= 0) {
                            ind = brInd + 1;
                        }
                    }
                }
                if (center) {
                    // This is for the second part, i.e. from ellipsis to end of text.
                    result = result + text.substring(ind);
                } else {
                    result = ellipsis + text.substring(ind);
                }
            }

            // Step two, check if text still overflows after we added the ellipsis.
            // If so, remove one char or word at a time.
            while (true) {
                helper.setText(result);
                int hit2 = helper.impl_hitTestChar(endPoint).getCharIndex();
                if (center && hit2 < centerLen) {
                    // No room for text after ellipsis. Maybe there is a newline
                    // here, and the next line falls outside the view.
                    if (hit2 > 0 && result.charAt(hit2-1) == '\n') {
                        hit2--;
                    }
                    result = text.substring(0, hit2) + ellipsis;
                    break;
                } else if (hit2 > 0 && hit2 < result.length()) {
                    if (leading) {
                        int ind = eLen + 1; // Past ellipsis and first char.
                        if (wordTrim) {
                            int brInd = firstBreakCharIndex(result, ind);
                            if (brInd >= 0) {
                                ind = brInd + 1;
                            }
                        }
                        result = ellipsis + result.substring(ind);
                    } else if (center) {
                        int ind = centerLen + 1; // Past ellipsis and first char.
                        if (wordTrim) {
                            int brInd = firstBreakCharIndex(result, ind);
                            if (brInd >= 0) {
                                ind = brInd + 1;
                            }
                        }
                        result = result.substring(0, centerLen) + result.substring(ind);
                    } else {
                        int ind = result.length() - eLen - 1; // Before last char and ellipsis.
                        if (wordTrim) {
                            int brInd = lastBreakCharIndex(result, ind);
                            if (brInd >= 0) {
                                ind = brInd;
                            }
                        }
                        result = result.substring(0, ind) + ellipsis;
                    }
                } else {
                    break;
                }
            }
        }
        return result;
    }


    private static int firstBreakCharIndex(String str, int start) {
        char[] chars = str.toCharArray();
        for (int i = start; i < chars.length; i++) {
            if (isPreferredBreakCharacter(chars[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int lastBreakCharIndex(String str, int start) {
        char[] chars = str.toCharArray();
        for (int i = start; i >= 0; i--) {
            if (isPreferredBreakCharacter(chars[i])) {
                return i;
            }
        }
        return -1;
    }

    /* Recognizes white space and latin punctuation as preferred
     * line break positions. Could do a bit better with using more
     * of the properties from the Character class.
     */
    private static boolean isPreferredBreakCharacter(char ch) {
        if (Character.isWhitespace(ch)) {
            return true;
        } else {
            switch (ch) {
            case ';' :
            case ':' :
            case '.' :
                return true;
            default: return false;
            }
        }
    }


    /*    private static Object KERNING;
       private static Object KERNING_ON;
       private static Object LIGATURES;
       private static Object LIGATURES_ON;
       private static Object TRACKING;

       static {
       try {
       KERNING = TextAttribute.class.getField("KERNING").get(null);
       KERNING_ON = TextAttribute.class.getField("KERNING_ON").get(null);
       LIGATURES = TextAttribute.class.getField("LIGATURES").get(null);
       LIGATURES_ON = TextAttribute.class.getField("LIGATURES_ON").get(null);
       TRACKING = TextAttribute.class.getField("TRACKING").get(null);
       }
       catch (Exception e) {
       // running on 1.5
       KERNING = KERNING_ON = LIGATURES = LIGATURES_ON = TRACKING = "unsupported";
       }
       }

       private static FontRenderContext DEFAULT_FRC = new FontRenderContext(null, false, false);

       private Utils() {}

       static FontMetrics getFontMetrics(Font font) {
       return getFontMetrics(font, DEFAULT_FRC);
       }

       static double computeLineHeight(Font font) {
       if (font == null) return 0;
       FontMetrics metrics = getFontMetrics(font, DEFAULT_FRC);
       return metrics.getMaxAscent() + metrics.getMaxDescent();
       }

       static double computeXHeight(Font font) {
       if (font == null) return 0;
       return font.createGlyphVector(DEFAULT_FRC, "x").getGlyphMetrics(0).getBounds2D().getHeight();
       }

       // computes an offset from the baseline that would be the location of
       // the center of the font. basically, this is where strikthrough would
       // happen
       static double computeCenterline(Font font) {
       FontMetrics metrics = getFontMetrics(font, DEFAULT_FRC);
       return metrics.getAscent() / 2;
       }*/

    /**
     * Computes the width of the string if displayed with a default font
     * render context (typically useful for display but not for printing).
     * Nodes in the scene graph don't have a way to fetch the font render
     * context, and computation of string widths typically happens outside
     * of a paint cycle (since the scene graph is a retained mode API and not
     * an immediate mode API).
     *
     * @param font this is an AWT font because I cannot refer to the fx font
     *        from this java file.
     * @param string
     * @return
     */
    /*    static double computeStringWidth(Font font, String string) {
       if (string == null || string.equals("")) return 0;
       FontMetrics metrics = getFontMetrics(font, DEFAULT_FRC);
       return computeStringWidth(metrics, string);
       }

       static double computeStringWidth(FontMetrics metrics, String string) {
       if (string == null || string.equals("")) return 0;
       return metrics.stringWidth(string);
       }

       private static boolean contains(Map map, Object key, Object value) {
       Object o = map.get(key);
       return o != null && o.equals(value);
       }

       private static boolean fontDesignMetricsInitialized = false;
       private static Class<?> fontDesignMetricsClass;
       private static Method getFontDesignMetrics;
       private static Constructor newFontDesignMetrics;

       private static synchronized FontMetrics getFontMetrics(final Font font, final FontRenderContext frc) {
       FontMetrics rv = null;
       if (! fontDesignMetricsInitialized) {
       AccessController.doPrivileged(
       new PrivilegedAction<Void>() {
       public Void run() {
       fontDesignMetricsInitialized = true;
       try {
       fontDesignMetricsClass =
       Class.forName("sun.font.FontDesignMetrics", true, null);
       } catch (ClassNotFoundException e) {
       return null;
       }
       try {
       newFontDesignMetrics =
       fontDesignMetricsClass.getConstructor(Font.class,
       FontRenderContext.class);
       getFontDesignMetrics =
       fontDesignMetricsClass.getMethod("getMetrics",
       Font.class, FontRenderContext.class);
       } catch (NoSuchMethodException ignore) {
       }
       return null;
       }
       });
       }
       try {
       if (getFontDesignMetrics != null) {
       rv = (FontMetrics) getFontDesignMetrics.invoke(null, font, frc);
       } else if (newFontDesignMetrics != null) {
       rv = (FontMetrics) newFontDesignMetrics.newInstance(font, frc);
       }
       } catch(IllegalAccessException ignore) {
       } catch(InstantiationException ignore) {
       } catch(InvocationTargetException exception) {
       if (exception.getTargetException() instanceof RuntimeException) {
       throw (RuntimeException) exception.getTargetException();
       }
       }
       if (rv == null) {
       //can not create FontDesignMetrics. Fall back to toolkit's fontMetrics.
       rv = Toolkit.getDefaultToolkit().getFontMetrics(font);
       }
       return rv;
       }*/

    // ---- begin code included from SwingUtilities2 ----
    // This code is present in sun.swing.SwingUtilities2 in JRE 1.6 or higher, but
    // was copied into our codebase to handle running on Java 1.5.  Arguably we
    // shouldn't be depending on sun.* APIs if at all possible anyway.

    /**
     * Indic, Thai, and surrogate char values require complex
     * text layout and cursor support.
     *
     * @param ch character to be tested
     * @return <tt>true</tt> if TextLayout is required
     */
    /*    private static final boolean isComplexLayout(char ch) {
       return (ch >= '\u0900' && ch <= '\u0D7F') || // Indic
       (ch >= '\u0E00' && ch <= '\u0E7F') || // Thai
       (ch >= '\u1780' && ch <= '\u17ff') || // Khmer
       (ch >= '\uD800' && ch <= '\uDFFF');   // surrogate value range
       }*/

    /**
     * Fast method to check that TextLayout is not required for the
     * character.
     * if returns <tt>true</tt> TextLayout is not required
     * if returns <tt>false</tt> TextLayout might be required
     *
     * @param ch character to be tested
     * @return <tt>true</tt> if TextLayout is not required
     */
    /*    private static final boolean isSimpleLayout(char ch) {
       return ch < 0x590 || (0x2E00 <= ch && ch < 0xD800);
       }*/

    /**
     * checks whether TextLayout is required to handle characters.
     *
     * @param text characters to be tested
     * @param start start
     * @param limit limit
     * @return <tt>true</tt>  if TextLayout is required
     *         <tt>false</tt> if TextLayout is not required
     */
    /*    public static final boolean isComplexLayout(char[] text, int start, int limit) {
       boolean simpleLayout = true;
       char ch;
       for (int i = start; i < limit; ++i) {
       ch = text[i];
       if (isComplexLayout(ch)) {
       return true;
       }
       if (simpleLayout) {
       simpleLayout = isSimpleLayout(ch);
       }
       }
       if (simpleLayout) {
       return false;
       } else {
       return Bidi.requiresBidi(text, start, limit);
       }
       }*/
    // ---- end code included from SwingUtilities2 ----

    private static boolean requiresComplexLayout(Font font, String string) {
        /*        Map attrs = font.getAttributes();
           if (contains(attrs, KERNING, KERNING_ON) ||
           contains(attrs, LIGATURES, LIGATURES_ON) ||
           (attrs.containsKey(TRACKING) && attrs.get(TRACKING) != null)) {
           return true;
           }
           return isComplexLayout(string.toCharArray(), 0, string.length());
         */
        return false;
    }

    static int computeStartOfWord(Font font, String text, int index) {
        if ("".equals(text) || index < 0) return 0;
        if (text.length() <= index) return text.length();
        // if the given index is not in a word (but in whitespace), then
        // simply return the index
        if (Character.isWhitespace(text.charAt(index))) {
            return index;
        }
        boolean complexLayout = requiresComplexLayout(font, text);
        if (complexLayout) {
            // TODO needs implementation
            return 0;
        } else {
            // just start walking backwards from index until either i<0 or
            // the first whitespace is found.
            int i = index;
            while (--i >= 0) {
                if (Character.isWhitespace(text.charAt(i))) {
                    return i + 1;
                }
            }
            return 0;
        }
    }

    static int computeEndOfWord(Font font, String text, int index) {
        if (text.equals("") || index < 0) {
            return 0;
        }
        if (text.length() <= index) {
            return text.length();
        }
        // if the given index is not in a word (but in whitespace), then
        // simply return the index
        if (Character.isWhitespace(text.charAt(index))) {
            return index;
        }
        boolean complexLayout = requiresComplexLayout(font, text);
        if (complexLayout) {
            // TODO needs implementation
            return text.length();
        } else {
            // just start walking forward from index until either i > length or
            // the first whitespace is found.
            int i = index;
            while (++i < text.length()) {
                if (Character.isWhitespace(text.charAt(i))) {
                    return i;
                }
            }
            return text.length();
        }
    }

    // used for layout to adjust widths to honor the min/max policies consistently
    public static double boundedSize(double value, double min, double max) {
        // if max < value, return max
        // if min > value, return min
        // if min > max, return min
        return Math.min(Math.max(value, min), Math.max(min,max));
    }

    static void addMnemonics(ContextMenu popup, Scene scene) {

        if (!com.sun.javafx.PlatformUtil.isMac()) {

            ContextMenuContent cmContent = (ContextMenuContent)popup.getSkin().getNode();
            MenuItem menuitem;

            for (int i = 0 ; i < popup.getItems().size() ; i++) {
                menuitem = popup.getItems().get(i);
                /*
                ** check is there are any mnemonics in this menu
                */
                if (menuitem.isMnemonicParsing()) {

                    TextBinding bindings = new TextBinding(menuitem.getText());
                    int mnemonicIndex = bindings.getMnemonicIndex() ;
                    if (mnemonicIndex >= 0) {

                        KeyCode mnemonicCode = bindings.getMnemonic();
                    
                        KeyCodeCombination mnemonicKeyCombo =
                                new KeyCodeCombination(
                                        mnemonicCode,
                                        com.sun.javafx.PlatformUtil.isMac()
                                                ? KeyCombination.META_DOWN
                                                : KeyCombination.ALT_DOWN);

                        Mnemonic myMnemonic = new Mnemonic(cmContent.getLabelAt(i), mnemonicKeyCombo);
                        scene.addMnemonic(myMnemonic);
                    }
                }
            }
        }
    }



    static void removeMnemonics(ContextMenu popup, Scene scene) {

        if (!com.sun.javafx.PlatformUtil.isMac()) {

            ContextMenuContent cmContent = (ContextMenuContent)popup.getSkin().getNode();
            MenuItem menuitem;

            for (int i = 0 ; i < popup.getItems().size() ; i++) {
                menuitem = popup.getItems().get(i);
                /*
                ** check is there are any mnemonics in this menu
                */
                if (menuitem.isMnemonicParsing()) {
                
                    TextBinding bindings = new TextBinding(menuitem.getText());
                    int mnemonicIndex = bindings.getMnemonicIndex() ;
                    if (mnemonicIndex >= 0) {

                        KeyCode mnemonicCode = bindings.getMnemonic();
                    
                        KeyCodeCombination mnemonicKeyCombo =
                                new KeyCodeCombination(
                                        mnemonicCode,
                                        com.sun.javafx.PlatformUtil.isMac()
                                                ? KeyCombination.META_DOWN
                                                : KeyCombination.ALT_DOWN);

                        ObservableList<Mnemonic> mnemonicsList = scene.getMnemonics().get(mnemonicKeyCombo);
                        if (mnemonicsList != null) {
                            for (int j = 0 ; j < mnemonicsList.size() ; j++) {
                                if (mnemonicsList.get(j).getNode() == cmContent.getLabelAt(i)) {
                                    mnemonicsList.remove(j);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    static double computeXOffset(double width, double contentWidth, HPos hpos) {
        switch(hpos) {
            case LEFT:
               return 0;
            case CENTER:
               return (width - contentWidth) / 2;
            case RIGHT:
               return width - contentWidth;
        }
        return 0;
    }

    static double computeYOffset(double height, double contentHeight, VPos vpos) {
       switch(vpos) {
            case TOP:
               return 0;
            case CENTER:
               return (height - contentHeight) / 2;
            case BOTTOM:
               return height - contentHeight;
            default:
                return 0;
        }
    }

    /*
    ** Returns true if the platform is to use Two-Level-Focus.
    ** This is in the Util class to ease any changes in
    ** the criteria for enabling this feature.
    ** 
    ** TwoLevelFocus is needed on platforms that
    ** only support 5-button navigation (arrow keys and Select/OK).
    **
    */
    public static boolean isTwoLevelFocus() {
        return Platform.isSupported(ConditionalFeature.TWO_LEVEL_FOCUS);
    }
}
