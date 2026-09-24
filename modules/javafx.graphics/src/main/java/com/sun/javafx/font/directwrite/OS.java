/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font.directwrite;

/**
 * The DirectWrite, Direct2D and WIC constants shared by the peers in this package; the Java side of
 * the DirectWrite API itself is {@link DWNative}. The {@code native} declarations that used to sit
 * beside these constants, and {@code directwrite.cpp} - the only source of the Windows
 * {@code javafx_font} library - are gone.
 */
class OS {
    static final int S_OK = 0x0;
    static final int E_NOT_SUFFICIENT_BUFFER = 0x8007007A;

    /* COM init constants */
    static final int COINIT_APARTMENTTHREADED = 0x2;
    static final int COINIT_DISABLE_OLE1DDE = 0x4;

    /* Direct2D constants */
    static final int D2D1_FACTORY_TYPE_SINGLE_THREADED = 0;
    static final int D2D1_RENDER_TARGET_TYPE_DEFAULT    = 0;
    static final int D2D1_RENDER_TARGET_TYPE_SOFTWARE   = 1;
    static final int D2D1_RENDER_TARGET_TYPE_HARDWARE   = 2;
    static final int D2D1_RENDER_TARGET_USAGE_NONE                   = 0x00000000;
    static final int D2D1_RENDER_TARGET_USAGE_FORCE_BITMAP_REMOTING  = 0x00000001;
    static final int D2D1_RENDER_TARGET_USAGE_GDI_COMPATIBLE         = 0x00000002;
    static final int D2D1_FEATURE_LEVEL_DEFAULT  = 0;
    static final int D2D1_ALPHA_MODE_UNKNOWN        = 0;
    static final int D2D1_ALPHA_MODE_PREMULTIPLIED  = 1;
    static final int D2D1_ALPHA_MODE_STRAIGHT       = 2;
    static final int D2D1_ALPHA_MODE_IGNORE         = 3;
    static final int DXGI_FORMAT_UNKNOWN = 0;
    static final int DXGI_FORMAT_A8_UNORM  = 65;
    static final int DXGI_FORMAT_B8G8R8A8_UNORM  = 87;
    static final int D2D1_TEXT_ANTIALIAS_MODE_DEFAULT    = 0;
    static final int D2D1_TEXT_ANTIALIAS_MODE_CLEARTYPE  = 1;
    static final int D2D1_TEXT_ANTIALIAS_MODE_GRAYSCALE  = 2;
    static final int D2D1_TEXT_ANTIALIAS_MODE_ALIASED    = 3;

    /* WICImagining constants */
    static final int GUID_WICPixelFormat8bppGray = 1;
    static final int GUID_WICPixelFormat8bppAlpha = 2;
    static final int GUID_WICPixelFormat16bppGray = 3;
    static final int GUID_WICPixelFormat24bppRGB = 4;
    static final int GUID_WICPixelFormat24bppBGR = 5;
    static final int GUID_WICPixelFormat32bppBGR = 6;
    static final int GUID_WICPixelFormat32bppBGRA = 7;
    static final int GUID_WICPixelFormat32bppPBGRA = 8;
    static final int GUID_WICPixelFormat32bppGrayFloat = 9;
    static final int GUID_WICPixelFormat32bppRGBA = 10;
    static final int GUID_WICPixelFormat32bppPRGBA = 11;
    static final int WICBitmapNoCache       = 0;
    static final int WICBitmapCacheOnDemand = 0x1;
    static final int WICBitmapCacheOnLoad   = 0x2;
    static final int WICBitmapLockRead   = 0x00000001;
    static final int WICBitmapLockWrite  = 0x00000002;

    /* DirectWrite constants */
    static final int DWRITE_FONT_WEIGHT_THIN         = 100;
    static final int DWRITE_FONT_WEIGHT_EXTRA_LIGHT  = 200;
    static final int DWRITE_FONT_WEIGHT_ULTRA_LIGHT  = 200;
    static final int DWRITE_FONT_WEIGHT_LIGHT        = 300;
    static final int DWRITE_FONT_WEIGHT_SEMI_LIGHT   = 350;
    static final int DWRITE_FONT_WEIGHT_NORMAL       = 400;
    static final int DWRITE_FONT_WEIGHT_REGULAR      = 400;
    static final int DWRITE_FONT_WEIGHT_MEDIUM       = 500;
    static final int DWRITE_FONT_WEIGHT_DEMI_BOLD    = 600;
    static final int DWRITE_FONT_WEIGHT_SEMI_BOLD    = 600;
    static final int DWRITE_FONT_WEIGHT_BOLD         = 700;
    static final int DWRITE_FONT_WEIGHT_EXTRA_BOLD   = 800;
    static final int DWRITE_FONT_WEIGHT_ULTRA_BOLD   = 800;
    static final int DWRITE_FONT_WEIGHT_BLACK        = 900;
    static final int DWRITE_FONT_WEIGHT_HEAVY        = 900;
    static final int DWRITE_FONT_WEIGHT_EXTRA_BLACK  = 950;
    static final int DWRITE_FONT_WEIGHT_ULTRA_BLACK  = 950;
    static final int DWRITE_FONT_STRETCH_UNDEFINED        = 0;
    static final int DWRITE_FONT_STRETCH_ULTRA_CONDENSED  = 1;
    static final int DWRITE_FONT_STRETCH_EXTRA_CONDENSED  = 2;
    static final int DWRITE_FONT_STRETCH_CONDENSED        = 3;
    static final int DWRITE_FONT_STRETCH_SEMI_CONDENSED   = 4;
    static final int DWRITE_FONT_STRETCH_NORMAL           = 5;
    static final int DWRITE_FONT_STRETCH_MEDIUM           = 5;
    static final int DWRITE_FONT_STRETCH_SEMI_EXPANDED    = 6;
    static final int DWRITE_FONT_STRETCH_EXPANDED         = 7;
    static final int DWRITE_FONT_STRETCH_EXTRA_EXPANDED   = 8;
    static final int DWRITE_FONT_STRETCH_ULTRA_EXPANDED   = 9;
    static final int DWRITE_FONT_STYLE_NORMAL       = 0;
    static final int DWRITE_FONT_STYLE_OBLIQUE      = 1;
    static final int DWRITE_FONT_STYLE_ITALIC       = 2;
    static final int DWRITE_TEXTURE_ALIASED_1x1 = 0;
    static final int DWRITE_TEXTURE_CLEARTYPE_3x1 = 1;
    static final int DWRITE_RENDERING_MODE_DEFAULT                      = 0;
    static final int DWRITE_RENDERING_MODE_ALIASED                      = 1;
    static final int DWRITE_RENDERING_MODE_GDI_CLASSIC                  = 2;
    static final int DWRITE_RENDERING_MODE_GDI_NATURAL                  = 3;
    static final int DWRITE_RENDERING_MODE_NATURAL                      = 4;
    static final int DWRITE_RENDERING_MODE_NATURAL_SYMMETRIC            = 5;
    static final int DWRITE_RENDERING_MODE_OUTLINE                      = 6;
    static final int DWRITE_RENDERING_MODE_CLEARTYPE_GDI_CLASSIC        = DWRITE_RENDERING_MODE_GDI_CLASSIC;
    static final int DWRITE_RENDERING_MODE_CLEARTYPE_GDI_NATURAL        = DWRITE_RENDERING_MODE_GDI_NATURAL;
    static final int DWRITE_RENDERING_MODE_CLEARTYPE_NATURAL            = DWRITE_RENDERING_MODE_NATURAL;
    static final int DWRITE_RENDERING_MODE_CLEARTYPE_NATURAL_SYMMETRIC  = DWRITE_RENDERING_MODE_NATURAL_SYMMETRIC;
    static final int DWRITE_MEASURING_MODE_NATURAL = 0;
    static final int DWRITE_MEASURING_MODE_GDI_CLASSIC = 1;
    static final int DWRITE_MEASURING_MODE_GDI_NATURAL = 2;
    static final int DWRITE_FACTORY_TYPE_SHARED = 0;
    static final int DWRITE_READING_DIRECTION_LEFT_TO_RIGHT = 0;
    static final int DWRITE_READING_DIRECTION_RIGHT_TO_LEFT = 1;
    static final int DWRITE_FONT_SIMULATIONS_NONE     = 0x0000;
    static final int DWRITE_FONT_SIMULATIONS_BOLD     = 0x0001;
    static final int DWRITE_FONT_SIMULATIONS_OBLIQUE  = 0x0002;
    static final int DWRITE_INFORMATIONAL_STRING_NONE = 0;
    static final int DWRITE_INFORMATIONAL_STRING_COPYRIGHT_NOTICE = 1;
    static final int DWRITE_INFORMATIONAL_STRING_VERSION_STRINGS = 2;
    static final int DWRITE_INFORMATIONAL_STRING_TRADEMARK = 3;
    static final int DWRITE_INFORMATIONAL_STRING_MANUFACTURER = 4;
    static final int DWRITE_INFORMATIONAL_STRING_DESIGNER = 5;
    static final int DWRITE_INFORMATIONAL_STRING_DESIGNER_URL = 6;
    static final int DWRITE_INFORMATIONAL_STRING_DESCRIPTION = 7;
    static final int DWRITE_INFORMATIONAL_STRING_FONT_VENDOR_URL = 8;
    static final int DWRITE_INFORMATIONAL_STRING_LICENSE_DESCRIPTION = 9;
    static final int DWRITE_INFORMATIONAL_STRING_LICENSE_INFO_URL = 10;
    static final int DWRITE_INFORMATIONAL_STRING_WIN32_FAMILY_NAMES = 11;
    static final int DWRITE_INFORMATIONAL_STRING_WIN32_SUBFAMILY_NAMES = 12;
    static final int DWRITE_INFORMATIONAL_STRING_PREFERRED_FAMILY_NAMES = 13;
    static final int DWRITE_INFORMATIONAL_STRING_PREFERRED_SUBFAMILY_NAMES = 14;
    static final int DWRITE_INFORMATIONAL_STRING_SAMPLE_TEXT = 15;
    /* Only on newer versions of Dwrite */
    static final int DWRITE_INFORMATIONAL_STRING_FULL_NAME = 16;
    static final int DWRITE_INFORMATIONAL_STRING_POSTSCRIPT_NAME = 17;
    static final int DWRITE_INFORMATIONAL_STRING_POSTSCRIPT_CID_NAME = 18;
}
