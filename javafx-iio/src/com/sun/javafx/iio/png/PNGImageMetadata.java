/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.png;

import java.util.ArrayList;

public class PNGImageMetadata {
    // IHDR chunk
    public boolean IHDR_present;
    public int IHDR_width;
    public int IHDR_height;
    public int IHDR_bitDepth;
    public int IHDR_colorType;
    public int IHDR_compressionMethod;
    public int IHDR_filterMethod;
    public int IHDR_interlaceMethod; // 0 == none, 1 == adam7

    // PLTE chunk
    public boolean PLTE_present;
    public byte[] PLTE_red;
    public byte[] PLTE_green;
    public byte[] PLTE_blue;

    // If non-null, used to reorder palette entries during encoding in
    // order to minimize the size of the tRNS chunk.  Thus an index of
    // 'i' in the source should be encoded as index 'PLTE_order[i]'.
    // PLTE_order will be null unless 'initialize' is called with an
    // IndexColorModel image type.
    public int[] PLTE_order = null;

    // bKGD chunk
    // If external (non-PNG sourced) data has red = green = blue,
    // always store it as gray and promote when writing
    public boolean bKGD_present;
    public int bKGD_colorType; // PNG_COLOR_GRAY, _RGB, or _PALETTE
    public int bKGD_index;
    public int bKGD_gray;
    public int bKGD_red;
    public int bKGD_green;
    public int bKGD_blue;

    // cHRM chunk
    public boolean cHRM_present;
    public int cHRM_whitePointX;
    public int cHRM_whitePointY;
    public int cHRM_redX;
    public int cHRM_redY;
    public int cHRM_greenX;
    public int cHRM_greenY;
    public int cHRM_blueX;
    public int cHRM_blueY;

    // gAMA chunk
    public boolean gAMA_present;
    public int gAMA_gamma;

    // hIST chunk
    public boolean hIST_present;
    public char[] hIST_histogram;

    // iCCP chunk
    public boolean iCCP_present;
    public String iCCP_profileName;
    public int iCCP_compressionMethod;
    public byte[] iCCP_compressedProfile;

    // iTXt chunk
    public ArrayList<String> iTXt_keyword = new ArrayList<String>();
    public ArrayList<Boolean> iTXt_compressionFlag = new ArrayList<Boolean>();
    public ArrayList<Integer> iTXt_compressionMethod = new ArrayList<Integer>();
    public ArrayList<String> iTXt_languageTag = new ArrayList<String>();
    public ArrayList<String> iTXt_translatedKeyword = new ArrayList<String>();
    public ArrayList<String> iTXt_text = new ArrayList<String>();

    // pHYs chunk
    public boolean pHYs_present;
    public int pHYs_pixelsPerUnitXAxis;
    public int pHYs_pixelsPerUnitYAxis;
    public int pHYs_unitSpecifier; // 0 == unknown, 1 == meter

    // sBIT chunk
    public boolean sBIT_present;
    public int sBIT_colorType; // PNG_COLOR_GRAY, _GRAY_ALPHA, _RGB, _RGB_ALPHA
    public int sBIT_grayBits;
    public int sBIT_redBits;
    public int sBIT_greenBits;
    public int sBIT_blueBits;
    public int sBIT_alphaBits;

    // sPLT chunk
    public boolean sPLT_present;
    public String sPLT_paletteName; // 1-79 characters
    public int sPLT_sampleDepth; // 8 or 16
    public int[] sPLT_red;
    public int[] sPLT_green;
    public int[] sPLT_blue;
    public int[] sPLT_alpha;
    public int[] sPLT_frequency;

    // sRGB chunk
    public boolean sRGB_present;
    public int sRGB_renderingIntent;

    // tEXt chunk
    public ArrayList<String> tEXt_keyword = new ArrayList<String>(); // 1-79 characters
    public ArrayList<String> tEXt_text = new ArrayList<String>();

    // tIME chunk
    public boolean tIME_present;
    public int tIME_year;
    public int tIME_month;
    public int tIME_day;
    public int tIME_hour;
    public int tIME_minute;
    public int tIME_second;

    // tRNS chunk
    // If external (non-PNG sourced) data has red = green = blue,
    // always store it as gray and promote when writing
    public boolean tRNS_present;
    public int tRNS_colorType; // PNG_COLOR_GRAY, _RGB, or _PALETTE
    public byte[] tRNS_alpha; // May have fewer entries than PLTE_red, etc.
    public int tRNS_gray;
    public int tRNS_red;
    public int tRNS_green;
    public int tRNS_blue;

    // zTXt chunk
    public ArrayList<String> zTXt_keyword = new ArrayList<String>();
    public ArrayList<Integer> zTXt_compressionMethod = new ArrayList<Integer>();
    public ArrayList<String> zTXt_text = new ArrayList<String>();

    // Unknown chunks
    public ArrayList<String> unknownChunkType = new ArrayList<String>();
    public ArrayList<byte[]> unknownChunkData = new ArrayList<byte[]>();

    PNGImageMetadata() {}
}
