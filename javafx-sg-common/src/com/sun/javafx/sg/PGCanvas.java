/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.sg;

public interface PGCanvas extends PGNode {
    public static final byte                 ATTR_BASE = 0;
    public static final byte GLOBAL_ALPHA  = ATTR_BASE + 0;
    public static final byte COMP_MODE     = ATTR_BASE + 1;
    public static final byte FILL_PAINT    = ATTR_BASE + 2;
    public static final byte STROKE_PAINT  = ATTR_BASE + 3;
    public static final byte LINE_WIDTH    = ATTR_BASE + 4;
    public static final byte LINE_CAP      = ATTR_BASE + 5;
    public static final byte LINE_JOIN     = ATTR_BASE + 6;
    public static final byte MITER_LIMIT   = ATTR_BASE + 7;
    public static final byte FONT          = ATTR_BASE + 8;
    public static final byte TEXT_ALIGN    = ATTR_BASE + 9;
    public static final byte TEXT_BASELINE = ATTR_BASE + 10;
    public static final byte TRANSFORM     = ATTR_BASE + 11;
    public static final byte EFFECT        = ATTR_BASE + 12;
    public static final byte PUSH_CLIP     = ATTR_BASE + 13;
    public static final byte POP_CLIP      = ATTR_BASE + 14;
    public static final byte ARC_TYPE      = ATTR_BASE + 15;
    public static final byte FILL_RULE     = ATTR_BASE + 16;

    public static final byte                     OP_BASE = 20;
    public static final byte FILL_RECT         = OP_BASE + 0;
    public static final byte STROKE_RECT       = OP_BASE + 1;
    public static final byte CLEAR_RECT        = OP_BASE + 2;
    public static final byte STROKE_LINE       = OP_BASE + 3;
    public static final byte FILL_OVAL         = OP_BASE + 4;
    public static final byte STROKE_OVAL       = OP_BASE + 5;
    public static final byte FILL_ROUND_RECT   = OP_BASE + 6;
    public static final byte STROKE_ROUND_RECT = OP_BASE + 7;
    public static final byte FILL_ARC          = OP_BASE + 8;
    public static final byte STROKE_ARC        = OP_BASE + 9;
    public static final byte FILL_TEXT         = OP_BASE + 10;
    public static final byte STROKE_TEXT       = OP_BASE + 11;

    public static final byte                PATH_BASE = 40;
    public static final byte PATHSTART    = PATH_BASE + 0;
    public static final byte MOVETO       = PATH_BASE + 1;
    public static final byte LINETO       = PATH_BASE + 2;
    public static final byte QUADTO       = PATH_BASE + 3;
    public static final byte CUBICTO      = PATH_BASE + 4;
    public static final byte CLOSEPATH    = PATH_BASE + 5;
    public static final byte PATHEND      = PATH_BASE + 6;
    public static final byte FILL_PATH    = PATH_BASE + 7;
    public static final byte STROKE_PATH  = PATH_BASE + 8;

    public static final byte                   IMG_BASE = 50;
    public static final byte DRAW_IMAGE      = IMG_BASE + 0;
    public static final byte DRAW_SUBIMAGE   = IMG_BASE + 1;
    public static final byte PUT_ARGB        = IMG_BASE + 2;
    public static final byte PUT_ARGBPRE_BUF = IMG_BASE + 3;

    public static final byte                   FX_BASE = 60;
    public static final byte FX_APPLY_EFFECT = FX_BASE + 0;

    public static final byte CAP_BUTT   = 0;
    public static final byte CAP_ROUND  = 1;
    public static final byte CAP_SQUARE = 2;

    public static final byte JOIN_MITER = 0;
    public static final byte JOIN_ROUND = 1;
    public static final byte JOIN_BEVEL = 2;

    public static final byte ARC_OPEN   = 0;
    public static final byte ARC_CHORD  = 1;
    public static final byte ARC_PIE    = 2;
   
    public static final byte ALIGN_LEFT       = 0;
    public static final byte ALIGN_CENTER     = 1;
    public static final byte ALIGN_RIGHT      = 2;
    public static final byte ALIGN_JUSTIFY    = 3;

    public static final byte BASE_TOP        = 0;
    public static final byte BASE_MIDDLE     = 1;
    public static final byte BASE_ALPHABETIC = 2;
    public static final byte BASE_BOTTOM     = 3;
    
    public static final byte FILL_RULE_NON_ZERO = 0;
    public static final byte FILL_RULE_EVEN_ODD = 1;

    public void updateBounds(float w, float h);
    public void updateRendering(GrowableDataBuffer buf);
}
