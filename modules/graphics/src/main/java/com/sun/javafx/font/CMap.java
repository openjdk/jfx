/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.font.FontFileReader.Buffer;

/*
 * A tt font has a CMAP table which is in turn made up of sub-tables which
 * describe the char to glyph mapping in (possibly) multiple ways.
 * CMAP subtables are described by 3 values.
 * 1. Platform ID (eg 3=Microsoft, which is the id we look for in JDK)
 * 2. Encoding (eg 0=symbol, 1=unicode)
 * 3. TrueType subtable format (how the char->glyph mapping for the encoding
 * is stored in the subtable). See the TrueType spec. Format 4 is required
 * by MS in fonts for windows. Its uses segmented mapping to delta values.
 * Most typically we see are (3,1,4) :
 * CMAP Platform ID=3 is what we prefer. After that any 0,*.
 */
abstract class CMap {

    static final char noSuchChar = (char)0xfffd;
    static final int SHORTMASK = 0x0000ffff;
    static final int INTMASK   = 0xffffffff;

    private static final int MAX_CODE_POINTS = 0x10ffff;

    static CMap initialize(PrismFontFile font) {

        CMap cmap = null;

        int offset, platformID, encodingID=-1;

        int three0=0, three1=0, three10=0, zeroStarOffset=0;
        boolean zeroStar = false, threeStar = false;

        Buffer cmapBuffer = font.readTable(FontConstants.cmapTag);
        short numberSubTables = cmapBuffer.getShort(2);

        /* Locate the offsets of supported 3,* Microsoft platform encodings,
         * and any 0,* Unicode platform encoding. The latter is used by
         * all current OS X fonts that don't have a Microsoft cmap.
         * We will always prefer the Microsoft cmap, for the fonts that
         * provide both. They ought to perform the same mappings. Although
         * I can imagine that a vendor might provide a different looking
         * glyph for some special characters for OS X vs Windows, I'm not
         * actually aware of any such case.
         */
        for (int i=0; i<numberSubTables; i++) {
            cmapBuffer.position(i * 8 + 4);
            platformID = cmapBuffer.getShort();

            if (platformID == 0) {
                zeroStar = true;
                encodingID = cmapBuffer.getShort();
                zeroStarOffset = cmapBuffer.getInt();
            }
            else if (platformID == 3) {
                threeStar = true;
                encodingID = cmapBuffer.getShort();
                offset     = cmapBuffer.getInt();
                switch (encodingID) {
                case 0:  three0  = offset; break; // MS Symbol encoding
                case 1:  three1  = offset; break; // MS Unicode cmap
                case 10: three10 = offset; break; // MS Unicode surrogates
                }
            }
        }

        /* This defines the preference order for cmap subtables */
        if (threeStar) {
            if (three10 != 0) {
                cmap = createCMap(cmapBuffer, three10);
            }
            else if  (three0 != 0) {
                cmap = createCMap(cmapBuffer, three0);
            }
            else if (three1 != 0) {
                cmap = createCMap(cmapBuffer, three1);
            }
        } else if (zeroStar && zeroStarOffset != 0) {
            cmap = createCMap(cmapBuffer, zeroStarOffset);
        } else {
            /* No 0,* or supported 3,* subtable was found.
             * Use whatever is the first table listed.
             * Since these are supposed to be sorted, there's a good chance
             * it will be Mac Roman (1,0). If its not that then its
             * likely a really old font but not one that's found on either
             * Windows or OS X
             * In fact I didn't even find any OS X font that supported
             * only (1,*).
             * So this seems likely to be an untravelled path which is
             * just as well given that its not likely to work properly.
             */
            cmap = createCMap(cmapBuffer, cmapBuffer.getInt(8));
        }
        return cmap;
    }

    static CMap createCMap(Buffer buffer, int offset) {
        /* First do a sanity check that this cmap subtable is contained
         * within the cmap table.
         */
        int subtableFormat = buffer.getChar(offset);

        switch (subtableFormat) {
        case 0:  return new CMapFormat0(buffer, offset);
        case 2:  return new CMapFormat2(buffer, offset);
        case 4:  return new CMapFormat4(buffer, offset);
        case 6:  return new CMapFormat6(buffer, offset);
        case 8:  return new CMapFormat8(buffer, offset);
        case 10: return new CMapFormat10(buffer, offset);
        case 12: return new CMapFormat12(buffer, offset);
        default: throw new RuntimeException("Cmap format unimplemented: " +
                                            (int)buffer.getChar(offset));
        }
    }

    abstract char getGlyph(int charCode);

    /* Format 4 Header is
     * ushort format (off=0)
     * ushort length (off=2)
     * ushort language (off=4)
     * ushort segCountX2 (off=6)
     * ushort searchRange (off=8)
     * ushort entrySelector (off=10)
     * ushort rangeShift (off=12)
     * ushort endCount[segCount] (off=14)
     * ushort reservedPad
     * ushort startCount[segCount]
     * short idDelta[segCount]
     * idRangeOFfset[segCount]
     * ushort glyphIdArray[]
     */
    static class CMapFormat4 extends CMap {
        int segCount;
        int entrySelector;
        int rangeShift;
        char[] endCount;
        char[] startCount;
        short[] idDelta;
        char[] idRangeOffset;
        char[] glyphIds;

        CMapFormat4(Buffer buffer, int offset) {

            buffer.position(offset);
            buffer.getChar(); // skip, we already know format=4
            int subtableLength = buffer.getChar();
            /* Try to recover from some bad fonts which specify a subtable
             * length that would overflow the byte buffer holding the whole
             * cmap table. If this isn't a recoverable situation an exception
             * may be thrown which is caught higher up the call stack.
             * Whilst this may seem lenient, in practice, unless the "bad"
             * subtable we are using is the last one in the cmap table we
             * would have no way of knowing about this problem anyway.
             */
            if (offset+subtableLength > buffer.capacity()) {
                subtableLength = buffer.capacity() - offset;
            }
            buffer.getChar(); // skip language
            segCount = buffer.getChar()/2;
            buffer.getChar(); // skip searchRange
            entrySelector = buffer.getChar();
            rangeShift    = buffer.getChar()/2;
            startCount = new char[segCount];
            endCount = new char[segCount];
            idDelta = new short[segCount];
            idRangeOffset = new char[segCount];

            for (int i=0; i<segCount; i++) {
                endCount[i] = buffer.getChar();
            }
            buffer.getChar(); // 2 bytes for reserved pad
            for (int i=0; i<segCount; i++) {
                startCount[i] = buffer.getChar();
            }

            for (int i=0; i<segCount; i++) {
                idDelta[i] = (short)buffer.getChar();
            }

            for (int i=0; i<segCount; i++) {
                char ctmp = buffer.getChar();
                idRangeOffset[i] = (char)((ctmp>>1)&0xffff);
            }
            /* Can calculate the number of glyph IDs by subtracting
             * "pos" from the length of the cmap
             */
            int pos = (segCount*8+16)/2;
            buffer.position(pos*2+offset); // * 2 for chars
            int numGlyphIds = (subtableLength/2 - pos);
            glyphIds = new char[numGlyphIds];
            for (int i=0;i<numGlyphIds;i++) {
                glyphIds[i] = buffer.getChar();
            }
        }

        char getGlyph(int charCode) {

            int index = 0;
            char glyphCode = 0;

            int controlGlyph = getControlCodeGlyph(charCode, true);
            if (controlGlyph >= 0) {
                return (char)controlGlyph;
            }

            /*
             * Citation from the TrueType (and OpenType) spec:
             *   The segments are sorted in order of increasing endCode
             *   values, and the segment values are specified in four parallel
             *   arrays. You search for the first endCode that is greater than
             *   or equal to the character code you want to map. If the
             *   corresponding startCode is less than or equal to the
             *   character code, then you use the corresponding idDelta and
             *   idRangeOffset to map the character code to a glyph index
             *   (otherwise, the missingGlyph is returned).
             */

            /*
             * CMAP format4 defines several fields for optimized search of
             * the segment list (entrySelector, searchRange, rangeShift).
             * However, benefits are neglible and some fonts have incorrect
             * data - so we use straightforward binary search (see bug 6247425)
             */
            int left = 0, right = startCount.length;
            index = startCount.length >> 1;
            while (left < right) {
                if (endCount[index] < charCode) {
                    left = index + 1;
                } else {
                    right = index;
                }
                index = (left + right) >> 1;
            }

            if (charCode >= startCount[index] && charCode <= endCount[index]) {
                int rangeOffset = idRangeOffset[index];

                if (rangeOffset == 0) {
                    glyphCode = (char)(charCode + idDelta[index]);
                } else {
                    /* Calculate an index into the glyphIds array */
                    int glyphIDIndex = rangeOffset - segCount + index
                                         + (charCode - startCount[index]);
                    glyphCode = glyphIds[glyphIDIndex];
                    if (glyphCode != 0) {
                        glyphCode = (char)(glyphCode + idDelta[index]);
                    }
                }
            }
            return glyphCode;
        }
    }

    // Format 0: Byte Encoding table
    static class CMapFormat0 extends CMap {
        byte [] cmap;

        CMapFormat0(Buffer buffer, int offset) {

            /* skip 6 bytes of format, length, and version */
            int len = buffer.getChar(offset+2);
            cmap = new byte[len-6];
            buffer.get(offset+6, cmap, 0, len-6);
        }

        char getGlyph(int charCode) {
            if (charCode < 256) {
                if (charCode < 0x0010) {
                    switch (charCode) {
                    case 0x0009:
                    case 0x000a:
                    case 0x000d: return CharToGlyphMapper.INVISIBLE_GLYPH_ID;
                    }
                }
                return (char)(0xff & cmap[charCode]);
            } else {
                return 0;
            }
        }
    }

    // Format 2: High-byte mapping through table
    static class CMapFormat2 extends CMap {

        char[] subHeaderKey = new char[256];
         /* Store subheaders in individual arrays
          * A SubHeader entry theoretically looks like {
          *   char firstCode;
          *   char entryCount;
          *   short idDelta;
          *   char idRangeOffset;
          * }
          */
        char[] firstCodeArray;
        char[] entryCountArray;
        short[] idDeltaArray;
        char[] idRangeOffSetArray;

        char[] glyphIndexArray;

        CMapFormat2(Buffer buffer, int offset) {

            int tableLen = buffer.getChar(offset+2);
            buffer.position(offset+6);
            char maxSubHeader = 0;
            for (int i=0;i<256;i++) {
                subHeaderKey[i] = buffer.getChar();
                if (subHeaderKey[i] > maxSubHeader) {
                    maxSubHeader = subHeaderKey[i];
                }
            }
            /* The value of the subHeaderKey is 8 * the subHeader index,
             * so the number of subHeaders can be obtained by dividing
             * this value bv 8 and adding 1.
             */
            int numSubHeaders = (maxSubHeader >> 3) +1;
            firstCodeArray = new char[numSubHeaders];
            entryCountArray = new char[numSubHeaders];
            idDeltaArray  = new short[numSubHeaders];
            idRangeOffSetArray  = new char[numSubHeaders];
            for (int i=0; i<numSubHeaders; i++) {
                firstCodeArray[i] = buffer.getChar();
                entryCountArray[i] = buffer.getChar();
                idDeltaArray[i] = (short)buffer.getChar();
                idRangeOffSetArray[i] = buffer.getChar();
            }

            int glyphIndexArrSize = (tableLen-518-numSubHeaders*8)/2;
            glyphIndexArray = new char[glyphIndexArrSize];
            for (int i=0; i<glyphIndexArrSize;i++) {
                glyphIndexArray[i] = buffer.getChar();
            }
        }

        char getGlyph(int charCode) {
            int controlGlyph = getControlCodeGlyph(charCode, true);
            if (controlGlyph >= 0) {
                return (char)controlGlyph;
            }

            char highByte = (char)(charCode >> 8);
            char lowByte = (char)(charCode & 0xff);
            int key = subHeaderKey[highByte]>>3; // index into subHeaders
            char mapMe;

            if (key != 0) {
                mapMe = lowByte;
            } else {
                mapMe = highByte;
                if (mapMe == 0) {
                    mapMe = lowByte;
                }
            }
            char firstCode = firstCodeArray[key];
            if (mapMe < firstCode) {
                return 0;
            } else {
                mapMe -= firstCode;
            }

            if (mapMe < entryCountArray[key]) {
                /* "address" arithmetic is needed to calculate the offset
                 * into glyphIndexArray. "idRangeOffSetArray[key]" specifies
                 * the number of bytes from that location in the table where
                 * the subarray of glyphIndexes starting at "firstCode" begins.
                 * Each entry in the subHeader table is 8 bytes, and the
                 * idRangeOffSetArray field is at offset 6 in the entry.
                 * The glyphIndexArray immediately follows the subHeaders.
                 * So if there are "N" entries then the number of bytes to the
                 * start of glyphIndexArray is (N-key)*8-6.
                 * Subtract this from the idRangeOffSetArray value to get
                 * the number of bytes into glyphIndexArray and divide by 2 to
                 * get the (char) array index.
                 */
                int glyphArrayOffset = ((idRangeOffSetArray.length-key)*8)-6;
                int glyphSubArrayStart =
                        (idRangeOffSetArray[key] - glyphArrayOffset)/2;
                char glyphCode = glyphIndexArray[glyphSubArrayStart+mapMe];
                if (glyphCode != 0) {
                    glyphCode += idDeltaArray[key]; //idDelta
                    return glyphCode;
                }
            }
            return 0;
        }
    }

    // Format 6: Trimmed table mapping
    static class CMapFormat6 extends CMap {

        char firstCode;
        char entryCount;
        char[] glyphIdArray;

        CMapFormat6(Buffer buffer, int offset) {

             buffer.position(offset+6);
             firstCode = buffer.getChar();
             entryCount = buffer.getChar();
             glyphIdArray = new char[entryCount];
             for (int i=0; i< entryCount; i++) {
                 glyphIdArray[i] = buffer.getChar();
             }
         }

         char getGlyph(int charCode) {
             int controlGlyph = getControlCodeGlyph(charCode, true);
             if (controlGlyph >= 0) {
                 return (char)controlGlyph;
             }

             charCode -= firstCode;
             if (charCode < 0 || charCode >= entryCount) {
                  return 0;
             } else {
                  return glyphIdArray[charCode];
             }
         }
    }

    // Format 8: mixed 16-bit and 32-bit coverage
    // Seems unlikely this code will ever get tested as we look for
    // MS platform Cmaps and MS states (in the Opentype spec on their website)
    // that MS doesn't support this format
    static class CMapFormat8 extends CMap {

         CMapFormat8(Buffer buffer, int offset) {
         }

        char getGlyph(int charCode) {
            return 0;
        }

    }


    // Format 4-byte 10: Trimmed table mapping
    // MS platform Cmaps and MS states (in the Opentype spec on their website)
    // that MS doesn't support this format
    static class CMapFormat10 extends CMap {

         long startCharCode;
         int numChars;
         char[] glyphIdArray;

         CMapFormat10(Buffer buffer, int offset) {

             buffer.position(offset+12);
             startCharCode = buffer.getInt() & INTMASK;
             numChars = buffer.getInt() & INTMASK;
             if (numChars <= 0 || numChars > MAX_CODE_POINTS ||
                 offset > buffer.capacity() - numChars*2 - 12 - 8)
             {
                 throw new RuntimeException("Invalid cmap subtable");
             }
             glyphIdArray = new char[numChars];
             for (int i=0; i< numChars; i++) {
                 glyphIdArray[i] = buffer.getChar();
             }
         }

         char getGlyph(int charCode) {

             int code = (int)(charCode - startCharCode);
             if (code < 0 || code >= numChars) {
                 return 0;
             } else {
                 return glyphIdArray[code];
             }
         }
    }

    // Format 12: Segmented coverage for UCS-4 (fonts supporting
    // surrogate pairs)
    static class CMapFormat12 extends CMap {

        int numGroups;
        int highBit =0;
        int power;
        int extra;
        long[] startCharCode;
        long[] endCharCode;
        int[] startGlyphID;

        CMapFormat12(Buffer buffer, int offset) {

            numGroups = buffer.getInt(offset+12);
            if (numGroups <= 0 || numGroups > MAX_CODE_POINTS ||
                offset > buffer.capacity() - numGroups*12 - 12 - 4)
            {
                throw new RuntimeException("Invalid cmap subtable");
            }
            startCharCode = new long[numGroups];
            endCharCode = new long[numGroups];
            startGlyphID = new int[numGroups];
            buffer.position(offset+16);
            // REMIND: why slice ?
            //buffer = buffer.slice();
            for (int i=0; i<numGroups; i++) {
                startCharCode[i] = buffer.getInt() & INTMASK;
                endCharCode[i] = buffer.getInt() & INTMASK;
                startGlyphID[i] = buffer.getInt() & INTMASK;
            }

            /* Finds the high bit by binary searching through the bits */
            int value = numGroups;

            if (value >= 1 << 16) {
                value >>= 16;
                highBit += 16;
            }

            if (value >= 1 << 8) {
                value >>= 8;
                highBit += 8;
            }

            if (value >= 1 << 4) {
                value >>= 4;
                highBit += 4;
            }

            if (value >= 1 << 2) {
                value >>= 2;
                highBit += 2;
            }

            if (value >= 1 << 1) {
                value >>= 1;
                highBit += 1;
            }

            power = 1 << highBit;
            extra = numGroups - power;
        }

        char getGlyph(int charCode) {
            int controlGlyph = getControlCodeGlyph(charCode, false);
            if (controlGlyph >= 0) {
                return (char)controlGlyph;
            }
            int probe = power;
            int range = 0;

            if (startCharCode[extra] <= charCode) {
                range = extra;
            }

            while (probe > 1) {
                probe >>= 1;

                if (startCharCode[range+probe] <= charCode) {
                    range += probe;
                }
            }

            if (startCharCode[range] <= charCode &&
                  endCharCode[range] >= charCode) {
                return (char)
                    (startGlyphID[range] + (charCode - startCharCode[range]));
            }

            return 0;
        }

    }

    /* Used to substitute for bad Cmaps. */
    static class NullCMapClass extends CMap {

        char getGlyph(int charCode) {
            return 0;
        }
    }

    public static final NullCMapClass theNullCmap = new NullCMapClass();

    final int getControlCodeGlyph(int charCode, boolean noSurrogates) {
        if (charCode < 0x0010) {
            switch (charCode) {
            case 0x0009:
            case 0x000a:
            case 0x000d: return CharToGlyphMapper.INVISIBLE_GLYPH_ID;
            }
        } else if (charCode >= 0x200c) {
            if ((charCode <= 0x200f) ||
                (charCode >= 0x2028 && charCode <= 0x202e) ||
                (charCode >= 0x206a && charCode <= 0x206f)) {
                return CharToGlyphMapper.INVISIBLE_GLYPH_ID;
            } else if (noSurrogates && charCode >= 0xFFFF) {
                return 0;
            }
        }
        return -1;
    }
}
