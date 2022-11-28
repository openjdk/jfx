/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.control.behavior.MnemonicInfo;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.tk.Toolkit;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import com.sun.javafx.scene.control.ContextMenuContent;
import com.sun.javafx.scene.text.FontHelper;
import java.net.URL;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.Mnemonic;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

import java.text.Bidi;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static javafx.scene.control.OverrunStyle.CENTER_ELLIPSIS;
import static javafx.scene.control.OverrunStyle.CENTER_WORD_ELLIPSIS;
import static javafx.scene.control.OverrunStyle.CLIP;
import static javafx.scene.control.OverrunStyle.ELLIPSIS;
import static javafx.scene.control.OverrunStyle.LEADING_ELLIPSIS;
import static javafx.scene.control.OverrunStyle.LEADING_WORD_ELLIPSIS;
import static javafx.scene.control.OverrunStyle.WORD_ELLIPSIS;

/**
 * BE REALLY CAREFUL WITH RESTORING OR RESETTING STATE OF helper NODE AS LEFTOVER
 * STATE CAUSES REALLY ODD NASTY BUGS!
 *
 * We expect all methods to set the Font property of helper but other than that
 * any properties set should be restored to defaults.
 */
public class Utils {

    static final Text helper = new Text();
    static final double DEFAULT_WRAPPING_WIDTH = helper.getWrappingWidth();
    static final double DEFAULT_LINE_SPACING = helper.getLineSpacing();
    static final String DEFAULT_TEXT = helper.getText();
    static final TextBoundsType DEFAULT_BOUNDS_TYPE = helper.getBoundsType();

    /* Using TextLayout directly for simple text measurement.
     * Instead of restoring the TextLayout attributes to default values
     * (each renders the TextLayout unable to efficiently cache layout data).
     * It always sets all the attributes pertinent to calculation being performed.
     * Note that lineSpacing and boundsType are important when computing the height
     * but irrelevant when computing the width.
     *
     * Note: This code assumes that TextBoundsType#VISUAL is never used by controls.
     * */
    static final TextLayout layout = Toolkit.getToolkit().getTextLayoutFactory().createLayout();

    public static double getAscent(Font font, TextBoundsType boundsType) {
        layout.setContent("", FontHelper.getNativeFont(font));
        layout.setWrapWidth(0);
        layout.setLineSpacing(0);
        if (boundsType == TextBoundsType.LOGICAL_VERTICAL_CENTER) {
            layout.setBoundsType(TextLayout.BOUNDS_CENTER);
        } else {
            layout.setBoundsType(0);
        }
        return -layout.getBounds().getMinY();
    }

    public static double getLineHeight(Font font, TextBoundsType boundsType) {
        layout.setContent("", FontHelper.getNativeFont(font));
        layout.setWrapWidth(0);
        layout.setLineSpacing(0);
        if (boundsType == TextBoundsType.LOGICAL_VERTICAL_CENTER) {
            layout.setBoundsType(TextLayout.BOUNDS_CENTER);
        } else {
            layout.setBoundsType(0);
        }

        // RT-37092: Use the line bounds specifically, to include font leading.
        return layout.getLines()[0].getBounds().getHeight();
    }

    public static double computeTextWidth(Font font, String text, double wrappingWidth) {
        layout.setContent(text != null ? text : "", FontHelper.getNativeFont(font));
        layout.setWrapWidth((float)wrappingWidth);
        return layout.getBounds().getWidth();
    }

    public static double computeTextHeight(Font font, String text, double wrappingWidth, TextBoundsType boundsType) {
        return computeTextHeight(font, text, wrappingWidth, 0, boundsType);
    }

    @SuppressWarnings("deprecation")
    public static double computeTextHeight(Font font, String text, double wrappingWidth, double lineSpacing, TextBoundsType boundsType) {
        layout.setContent(text != null ? text : "", FontHelper.getNativeFont(font));
        layout.setWrapWidth((float)wrappingWidth);
        layout.setLineSpacing((float)lineSpacing);
        if (boundsType == TextBoundsType.LOGICAL_VERTICAL_CENTER) {
            layout.setBoundsType(TextLayout.BOUNDS_CENTER);
        } else {
            layout.setBoundsType(0);
        }
        return layout.getBounds().getHeight();
    }

    public static Point2D computeMnemonicPosition(Font font, String text, int mnemonicIndex, double wrappingWidth,
                                                  double lineSpacing, boolean isRTL) {
        // Input validation
        if ((font == null) || (text == null) ||
            (mnemonicIndex < 0) || (mnemonicIndex > text.length())) {
            return null;
        }

        // Layout the text with given font, wrapping width and line spacing
        layout.setContent(text, FontHelper.getNativeFont(font));
        layout.setWrapWidth((float)wrappingWidth);
        layout.setLineSpacing((float)lineSpacing);

        // The text could be spread over multiple lines
        // We need to find out on which line the mnemonic character lies
        int start = 0;
        int i = 0;
        int totalLines = layout.getLines().length;
        int lineLength = 0;
        while (i < totalLines) {
            lineLength = layout.getLines()[i].getLength();

            if ((mnemonicIndex >= start) &&
                (mnemonicIndex < (start + lineLength))) {
                // mnemonic lies on line 'i'
                break;
            }

            start += lineLength;
            i++;
        }

        // Find x and y offsets of mnemonic character position
        // in line numbered 'i'
        double lineHeight = layout.getBounds().getHeight() / totalLines;
        double x = Utils.computeTextWidth(font, text.substring(start, mnemonicIndex), 0);
        if (isRTL) {
            double lineWidth = Utils.computeTextWidth(font, text.substring(start, (start + lineLength - 1)), 0);
            x = lineWidth - x;
        }

        double y = (lineHeight * (i+1));
        // Adjust y offset for linespacing except for the last line.
        if ((i+1) != totalLines) {
            y -= (lineSpacing / 2);
        }

        return new Point2D(x, y);
    }

    public static int computeTruncationIndex(Font font, String text, double width) {
        helper.setText(text);
        helper.setFont(font);
        helper.setWrappingWidth(0);
        helper.setLineSpacing(0);
        // The -2 is a fudge to make sure the result more often matches
        // what we get from using computeTextWidth instead. It's not yet
        // clear what causes the small discrepancies.
        Bounds bounds = helper.getLayoutBounds();
        Point2D endPoint = new Point2D(width - 2, bounds.getMinY() + bounds.getHeight() / 2);
        final int index = helper.hitTest(endPoint).getCharIndex();
        // RESTORE STATE
        helper.setWrappingWidth(DEFAULT_WRAPPING_WIDTH);
        helper.setLineSpacing(DEFAULT_LINE_SPACING);
        helper.setText(DEFAULT_TEXT);
        return index;
    }

    public static String computeClippedText(Font font, String text, double width,
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
                if (style == ELLIPSIS && !new Bidi(text, Bidi.DIRECTION_LEFT_TO_RIGHT).isMixed()) {
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

    public static String computeClippedWrappedText(Font font, String text, double width,
                                            double height, double lineSpacing, OverrunStyle truncationStyle,
                                            String ellipsisString, TextBoundsType boundsType) {
        if (font == null) {
            throw new IllegalArgumentException("Must specify a font");
        }

        // The height given does not need to include the line spacing after
        // the last line to be able to render that last line correctly.
        //
        // However the calculations include the line spacing as part of a
        // line's height.  In order to not cut off the last line because its
        // line spacing wouldn't fit, the height used for the calculation
        // is increased here with the line spacing amount.

        height += lineSpacing;

        String ellipsis = (truncationStyle == CLIP) ? "" : ellipsisString;
        int eLen = ellipsis.length();
        // Do this before using helper, as it's not reentrant.
        double eWidth = computeTextWidth(font, ellipsis, 0);
        double eHeight = computeTextHeight(font, ellipsis, 0, lineSpacing, boundsType);

        if (width < eWidth || height < eHeight) {
            // The ellipsis doesn't fit.
            return text; // RT-30868 - return text, not empty string.
        }

        helper.setText(text);
        helper.setFont(font);
        helper.setWrappingWidth((int)Math.ceil(width));
        helper.setBoundsType(boundsType);
        helper.setLineSpacing(lineSpacing);

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

        int hit = helper.hitTest(endPoint).getCharIndex();
        if (hit >= len) {
            helper.setBoundsType(TextBoundsType.LOGICAL); // restore
            return text;
        }
        if (center) {
            hit = helper.hitTest(centerPoint).getCharIndex();
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
                int hit2 = helper.hitTest(endPoint).getCharIndex();
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
        // RESTORE STATE
        helper.setWrappingWidth(DEFAULT_WRAPPING_WIDTH);
        helper.setLineSpacing(DEFAULT_LINE_SPACING);
        helper.setText(DEFAULT_TEXT);
        helper.setBoundsType(DEFAULT_BOUNDS_TYPE);
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

    public static void addMnemonics(ContextMenu popup, Scene scene) {
        addMnemonics(popup, scene, false);
    }

    public static void addMnemonics(ContextMenu popup, Scene scene, boolean initialState) {
        addMnemonics(popup, scene, initialState, null);
    }

    public static void addMnemonics(ContextMenu popup, Scene scene, boolean initialState, List<Mnemonic> into) {

        if (!com.sun.javafx.PlatformUtil.isMac()) {

            ContextMenuContent cmContent = (ContextMenuContent)popup.getSkin().getNode();
            MenuItem menuitem;

            for (int i = 0 ; i < popup.getItems().size() ; i++) {
                menuitem = popup.getItems().get(i);
                /*
                ** check is there are any mnemonics in this menu
                */
                if (menuitem.isMnemonicParsing()) {

                    MnemonicInfo mnemonicInfo = new MnemonicInfo(menuitem.getText());
                    int mnemonicIndex = mnemonicInfo.getMnemonicIndex() ;
                    if (mnemonicIndex >= 0) {
                        KeyCombination mnemonicKeyCombo = mnemonicInfo.getMnemonicKeyCombination();
                        Mnemonic myMnemonic = new Mnemonic(cmContent.getLabelAt(i), mnemonicKeyCombo);
                        scene.addMnemonic(myMnemonic);
                        NodeHelper.setShowMnemonics(cmContent.getLabelAt(i), initialState);
                        if (into != null) {
                            into.add(myMnemonic);
                        }
                    }
                }
            }
        }
    }



    public static void removeMnemonics(ContextMenu popup, Scene scene) {

        if (!com.sun.javafx.PlatformUtil.isMac()) {

            ContextMenuContent cmContent = (ContextMenuContent)popup.getSkin().getNode();
            MenuItem menuitem;

            for (int i = 0 ; i < popup.getItems().size() ; i++) {
                menuitem = popup.getItems().get(i);
                /*
                ** check is there are any mnemonics in this menu
                */
                if (menuitem.isMnemonicParsing()) {

                    MnemonicInfo mnemonicInfo = new MnemonicInfo(menuitem.getText());
                    int mnemonicIndex = mnemonicInfo.getMnemonicIndex() ;
                    if (mnemonicIndex >= 0) {
                        KeyCombination mnemonicKeyCombo = mnemonicInfo.getMnemonicKeyCombination();

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

    public static double computeXOffset(double width, double contentWidth, HPos hpos) {
        if (hpos == null) {
            return 0;
        }

        switch(hpos) {
            case LEFT:
               return 0;
            case CENTER:
               return (width - contentWidth) / 2;
            case RIGHT:
               return width - contentWidth;
            default:
                return 0;
        }
    }

    public static double computeYOffset(double height, double contentHeight, VPos vpos) {
        if (vpos == null) {
            return 0;
        }

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


    // useful method for linking things together when before a property is
    // necessarily set
    public static <T> void executeOnceWhenPropertyIsNonNull(ObservableValue<T> p, Consumer<T> consumer) {
        if (p == null) return;

        T value = p.getValue();
        if (value != null) {
            consumer.accept(value);
        } else {
            final InvalidationListener listener = new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    T value = p.getValue();

                    if (value != null) {
                        p.removeListener(this);
                        consumer.accept(value);
                    }
                }
            };
            p.addListener(listener);
        }
    }

    public static String formatHexString(Color c) {
        if (c != null) {
            return String.format((Locale) null, "#%02x%02x%02x",
                    Math.round(c.getRed() * 255),
                    Math.round(c.getGreen() * 255),
                    Math.round(c.getBlue() * 255));
        } else {
            return null;
        }
    }

    public static URL getResource(String str) {
        return Utils.class.getResource(str);
    }

}
