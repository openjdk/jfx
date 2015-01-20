/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.text;

import com.sun.javafx.font.CharToGlyphMapper;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextSpan;

public class TextRun implements GlyphList {
    int glyphCount;
    int[] gids;
    float[] positions;
    int[] charIndices;
    int start, length;
    float width = -1;
    byte level;
    int script;
    TextSpan span;
    TextLine line;
    Point2D location;
    private float ascent, descent, leading;
    int flags = 0;
    int slot = 0;

    final static int FLAGS_TAB              = 1 << 0;
    final static int FLAGS_LINEBREAK        = 1 << 1;
    final static int FLAGS_SOFTBREAK        = 1 << 2;
    final static int FLAGS_NO_LINK_BEFORE   = 1 << 3;
    final static int FLAGS_NO_LINK_AFTER    = 1 << 4;
    final static int FLAGS_COMPLEX          = 1 << 5;
    final static int FLAGS_EMBEDDED         = 1 << 6;
    final static int FLAGS_SPLIT            = 1 << 7;
    final static int FLAGS_SPLIT_LAST       = 1 << 8;
    final static int FLAGS_LEFT_BEARING     = 1 << 9;
    final static int FLAGS_RIGHT_BEARING    = 1 << 10;
    final static int FLAGS_CANONICAL        = 1 << 11;
    final static int FLAGS_COMPACT          = 1 << 12;
    /* Compact is performance optimization used for simple text, it implies:
     * The glyphs and positions arrays are shared by all the runs and owned
     * by the TextHelper. The positions arrays only has x advance.
     */

    public TextRun(int start, int length, byte level, boolean complex,
                   int script, TextSpan span, int slot, boolean canonical) {

        this.start = start;
        this.length = length;
        this.level = level;
        this.script = script;
        this.span = span;
        this.slot = slot;
        if (complex) flags |= FLAGS_COMPLEX;
        if (canonical) flags |= FLAGS_CANONICAL;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return start + length;
    }

    public int getLength() {
        return length;
    }

    public byte getLevel() {
        return level;
    }

    @Override public RectBounds getLineBounds() {
        return line.getBounds();
    }

    public void setLine(TextLine line) {
        this.line = line;
    }

    public int getScript() {
        return script;
    }

    @Override public TextSpan getTextSpan() {
        return span;
    }

    public int getSlot() {
        return slot;
    }

    public boolean isLinebreak() {
        return (flags & FLAGS_LINEBREAK) != 0;
    }

    public boolean isCanonical() {
        return (flags & FLAGS_CANONICAL) != 0;
    }

    public boolean isSoftbreak() {
        return (flags & FLAGS_SOFTBREAK) != 0;
    }

    public boolean isBreak() {
        return (flags & (FLAGS_LINEBREAK | FLAGS_SOFTBREAK)) != 0;
    }

    public boolean isTab() {
        return (flags & FLAGS_TAB) != 0;
    }

    public boolean isEmbedded() {
        return (flags & FLAGS_EMBEDDED) != 0;
    }

    public boolean isNoLinkBefore() {
        return (flags & FLAGS_NO_LINK_BEFORE) != 0;
    }

    public boolean isNoLinkAfter() {
        return (flags & FLAGS_NO_LINK_AFTER) != 0;
    }

    public boolean isSplit() {
        return (flags & FLAGS_SPLIT) != 0;
    }

    public boolean isSplitLast() {
        return (flags & FLAGS_SPLIT_LAST) != 0;
    }

    @Override public boolean isComplex() {
        return (flags & FLAGS_COMPLEX) != 0;
    }

    public boolean isLeftBearing() {
        return (flags & FLAGS_LEFT_BEARING) != 0;
    }

    public boolean isRightBearing() {
        return (flags & FLAGS_RIGHT_BEARING) != 0;
    }

    public boolean isLeftToRight() {
        return (level & 1) == 0;
    }

    public void setComplex(boolean complex) {
        if (complex) {
            flags |= FLAGS_COMPLEX;
        } else {
            flags &= ~FLAGS_COMPLEX;
        }
    }

    @Override public float getWidth() {
        if (width != -1) return width;
        if (positions != null) {
            if ((flags & FLAGS_COMPACT) != 0) {
                width = 0;
                for (int i = 0; i < glyphCount; i++) {
                    width += positions[start + i];
                }
                return width;
            }
            return positions[glyphCount<<1];
        }
        return 0; //line break
    }

    @Override public float getHeight() {
        return -ascent + descent + leading;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setMetrics(float ascent, float descent, float leading) {
        this.ascent = ascent;
        this.descent = descent;
        this.leading = leading;
    }

    public float getAscent() {
        return ascent;
    }

    public float getDescent() {
        return descent;
    }

    public float getLeading() {
        return leading;
    }

    public void setLocation(float x, float y) {
        this.location = new Point2D(x, y);
    }

    @Override public Point2D getLocation() {
        return location;
    }

    public void setTab() {
        flags |= FLAGS_TAB;
    }

    public void setEmbedded(RectBounds bounds, int length) {
        width = bounds.getWidth() * length;
        ascent = bounds.getMinY();
        descent = bounds.getHeight() + ascent;
        this.length = length;
        flags |= FLAGS_EMBEDDED;
    }

    public void setLinebreak() {
        flags |= FLAGS_LINEBREAK;
    }

    public void setSoftbreak() {
        flags |= FLAGS_SOFTBREAK;
    }

    public void setLeftBearing() {
        flags |= FLAGS_LEFT_BEARING;
    }

    public void setRightBearing() {
        flags |= FLAGS_RIGHT_BEARING;
    }

    public int getWrapIndex(float width) {
        if (glyphCount == 0) return 0;
        if (isLeftToRight()) {
            int gi = 0;
            if ((flags & FLAGS_COMPACT) != 0) {
                float right = 0;
                while (gi < glyphCount) {
                    right += positions[start + gi];
                    if (right > width) {
                        return getCharOffset(gi);
                    }
                    gi++;
                }
            } else {
                while (gi < glyphCount) {
                    if (positions[(gi + 1) << 1] > width) {
                        return getCharOffset(gi);
                    }
                    gi++;
                }
            }
        } else {
            /* This code is not correct. The width of the run excluding a glyph
             * cannot be computed by subtracting the glyph's width. Removing a
             * glyph from the run can change the contextual shapes for the
             * remaining glyphs (i.e. Arabic). The correct code is to reshape
             * the run excluding the given glyph. Due to performance reshaping
             * should only be used when the run has contextual shaping.
             */
            /* Not need to check for compact as bidi disables the simple case */
            int gi = 0;
            float runWidth = positions[glyphCount<<1];
            while (runWidth > width) {
                float glyphWidth = positions[(gi+1)<<1] - positions[gi<<1];
                if (runWidth - glyphWidth <= width) {
                    return getCharOffset(gi);
                }
                runWidth -= glyphWidth;
                gi++;
            }
        }
        return 0;
    }

    @Override public int getGlyphCount() {
        return glyphCount;
    }

    @Override public int getGlyphCode(int glyphIndex) {
        if (0 <= glyphIndex && glyphIndex < glyphCount) {
            if ((flags & FLAGS_COMPACT) != 0) {
                return gids[start + glyphIndex];
            }
            return gids[glyphIndex];
        }
        //tab and line break
        return CharToGlyphMapper.INVISIBLE_GLYPH_ID;
    }

    float cacheWidth = 0;
    int cacheIndex = 0;
    @Override public float getPosX(int glyphIndex) {
        if (0 <= glyphIndex && glyphIndex <= glyphCount) {
            if ((flags & FLAGS_COMPACT) != 0) {
                if (cacheIndex == glyphIndex) return cacheWidth;
                float x = 0;
                // Makes this faster when accessing incrementally
                if (cacheIndex + 1 == glyphIndex) {
                    x = cacheWidth + positions[start + glyphIndex - 1];
                } else {
                    for (int i = 0; i < glyphIndex; i++) {
                        x += positions[start + i];
                    }
                }
                cacheIndex = glyphIndex;
                cacheWidth = x;
                return x;
            }
            return positions[glyphIndex<<1];
        }
        return glyphIndex == 0 ? 0 : getWidth();
    }

    @Override public float getPosY(int glyphIndex) {
        if ((flags & FLAGS_COMPACT) != 0) return 0;
        if (0 <= glyphIndex && glyphIndex <= glyphCount) {
            return positions[(glyphIndex<<1) + 1];
        }
        return 0;
    }

    public float getAdvance(int glyphIndex) {
        if ((flags & FLAGS_COMPACT) != 0) {
            return positions[start + glyphIndex];
        } else {
            return positions[(glyphIndex + 1) << 1] - positions[glyphIndex << 1];
        }
    }

    public void shape(int count, int[] glyphs, float[] pos, int[] indices) {
        this.glyphCount = count;
        this.gids = glyphs;
        this.positions = pos;
        this.charIndices = indices;
    }

    public void shape(int count, int[] glyphs, float[] pos) {
        this.glyphCount = count;
        this.gids = glyphs;
        this.positions = pos;
        this.charIndices = null;
        this.flags |= FLAGS_COMPACT;
    }

    public float getXAtOffset(int offset, boolean leading) {
        boolean ltr = isLeftToRight();
        if (offset == length) {
            return ltr ? getWidth() : 0;
        }
        if (glyphCount > 0) {
            int glyphIndex = getGlyphIndex(offset);
            if (ltr) {
                return getPosX(glyphIndex + (leading ? 0 : 1));
            } else {
                return getPosX(glyphIndex + (leading ? 1 : 0));
            }
        }
        if (isTab()) {
            if (ltr) {
                return leading ? 0 : getWidth();
            } else {
                return leading ? getWidth() : 0;
            }
        }
        return 0; //line break
    }

    public int getGlyphAtX(float x, int[] trailing) {
        boolean ltr = isLeftToRight();
        float runX = 0;
        for (int i = 0; i < glyphCount; i++) {
            float advance = getAdvance(i);
            if (runX + advance > x) {
                if (trailing != null) {
                    //TODO handle clusters
                    if (x - runX > advance / 2) {
                        trailing[0] = ltr ? 1 : 0;
                    } else {
                        trailing[0] = ltr ? 0 : 1;
                    }
                }
                return i;
            }
            runX += advance;
        }
        if (trailing != null) trailing[0] = ltr ? 1 : 0;
        return Math.max(0, glyphCount - 1);
    }

    public int getOffsetAtX(float x, int[] trailing) {
        if (glyphCount > 0) {
            int glyphIndex = getGlyphAtX(x, trailing);
            return getCharOffset(glyphIndex);
        }
        /* tab */
        if (width != -1 && length > 0) {
            if (trailing != null) {
                if (x > width / 2) {
                    trailing[0] = 1;
                }
            }
        }
        return 0;
    }

    private void reset() {
        positions = null;
        charIndices = null;
        gids = null;
        width = -1;
        ascent = descent = leading = 0;
        glyphCount = 0;
    }

    public TextRun split(int offset) {
        int newLength = length - offset;
        length = offset;
        boolean complex = isComplex();
        TextRun newRun = new TextRun(start + length, newLength, level, complex,
                                     script, span, slot, isCanonical());
        flags |= FLAGS_NO_LINK_AFTER;
        newRun.flags |= FLAGS_NO_LINK_BEFORE;
        flags |= FLAGS_SPLIT;
        flags &= ~FLAGS_SPLIT_LAST;
        newRun.flags |= FLAGS_SPLIT_LAST;
        newRun.setMetrics(ascent, descent, leading);
        if (!complex) {
            glyphCount = length;

            /* No need to shape the newly created run (performance) */
            if ((flags & FLAGS_COMPACT) != 0) {
                newRun.shape(newLength, gids, positions);
                if (width != -1) {
                    if (newLength > length) {
                        float oldWidth = width;
                        width = -1;
                        newRun.setWidth(oldWidth - getWidth());
                    } else {
                        width -= newRun.getWidth();
                    }
                }
            } else {
                int[] newGlyphs = new int[newLength];
                float[] newPos = new float[(newLength + 1) << 1];
                System.arraycopy(gids, offset, newGlyphs, 0, newLength);
                float width = getWidth();
                int delta = offset << 1;
                for (int i = 2; i < newPos.length; i += 2) {
                    newPos[i] = positions[i+delta] - width;
                }
                newRun.shape(newLength, newGlyphs, newPos, null);
            }
            /* ignore glyphData array as it is only used for complex text */
        } else {
            reset();
        }
        return newRun;
    }

    public void merge(TextRun run) {
        /* This method can only be used for already shaped runs in simple layout */
        if (run != null) {
            length += run.length;
            glyphCount += run.glyphCount;
            if (width != -1 && run.width != -1) {
                width += run.width;
            } else {
                width = -1;
            }
        }
        flags &= ~FLAGS_SPLIT;
        flags &= ~FLAGS_SPLIT_LAST;
    }

    public TextRun unwrap() {
        TextRun newRun = new TextRun(start, length, level, isComplex(),
                                     script, span, slot, isCanonical());
        newRun.shape(glyphCount, gids, positions);
        newRun.setWidth(width);
        newRun.setMetrics(ascent, descent, leading);
        /* do not clear SPLIT here as it is still needed for merging */
        int mask = FLAGS_SOFTBREAK | FLAGS_NO_LINK_AFTER | FLAGS_NO_LINK_BEFORE;
        newRun.flags = flags & ~mask;
        return newRun;
    }

    public void justify(int offset, float width) {
        /* Not need to check for compact as justify disables the simple case */
        if (positions != null) {
            int glyphIndex = getGlyphIndex(offset);
            if (glyphIndex != -1) {
                for (int i = glyphIndex + 1; i <= glyphCount; i++) {
                    positions[i << 1] += width;
                }
                this.width = -1;
            }

            /* Temp code: Setting complex to true to force rendering to use
             * advances from the GlyphList instead of GlyphData
             */
            setComplex(true);
        }
    }

    public int getGlyphIndex(int charOffset) {
        if (charIndices == null) return charOffset;
        for (int i = 0; i < charIndices.length && i < glyphCount; i++) {
            if (charIndices[i] == charOffset) {
                return i;
            }
        }
        /* The charOffset does not have a glyph that maps back to it. This
         * happens with cluster, specially on Windows where all glyphs in the
         * cluster map back to the base character. The fix is to search for
         * glyph index for previous character offset (which we expect is the
         * base character for the cluster). */
        if (isLeftToRight()) {
            if (charOffset > 0) return getGlyphIndex(charOffset - 1);
        } else {
            if (charOffset + 1 < length) return getGlyphIndex(charOffset + 1);
        }
        return 0;
    }

    @Override public int getCharOffset(int glyphIndex) {
        return charIndices == null ? glyphIndex : charIndices[glyphIndex];
    }

    @Override public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("TextRun start=");
        buffer.append(start);
        buffer.append(", length=");
        buffer.append(length);
        buffer.append(", script=");
        buffer.append(script);
        buffer.append(", linebreak=");
        buffer.append(isLinebreak());
        buffer.append(", softbreak=");
        buffer.append(isSoftbreak());
        buffer.append(", complex=");
        buffer.append(isComplex());
        buffer.append(", tab=");
        buffer.append(isTab());
        buffer.append(", compact=");
        buffer.append((flags & FLAGS_COMPACT) != 0);
        buffer.append(", ltr=");
        buffer.append(isLeftToRight());
        buffer.append(", split=");
        buffer.append(isSplit());
        return buffer.toString();
    }
}
