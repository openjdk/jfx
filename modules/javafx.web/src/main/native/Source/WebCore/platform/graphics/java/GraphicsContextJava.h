/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#include <jni.h>
#include "PlatformJavaClasses.h"

extern jmethodID WCGM_getWCFont_mID;
extern jmethodID WCGM_createBufferedContext_mID;
extern jmethodID WCGM_createWCPath_mID;
extern jmethodID WCGM_createWCPath_L_mID;
extern jmethodID WCGM_createWCImage_mID;

extern jmethodID WCF_getXHeight_mID;
extern jmethodID WCF_getFontMetrics_mID;
extern jmethodID WCF_getGlyphCodes_mID;
extern jmethodID WCF_drawString_mID;
extern jmethodID WCF_getStringLength_mID;
extern jmethodID WCF_getStringBounds_mID;
extern jmethodID WCF_getGlyphWidth_mID;
extern jmethodID WCF_getOffsetForPosition_mID;
extern jmethodID WCF_hash_mID;
extern jmethodID WCF_compare_mID;
extern jmethodID WCF_getXHeight_mID;
extern jmethodID WCF_getAscent_mID;
extern jmethodID WCF_getDescent_mID;
extern jmethodID WCF_getHeight_mID;
extern jmethodID WCF_hasUniformLineMetrics_mID;
extern jmethodID WCGC_beginPaint_mID;
extern jmethodID WCGC_endPaint_mID;
extern jmethodID WCGC_getImage_mID;
extern jmethodID WCGC_drawImage_mID;
extern jmethodID WCGC_drawIcon_mID;
extern jmethodID WCGC_drawPattern_mID;

extern jmethodID WCP_contains_mID;
extern jmethodID WCP_clear_mID;
extern jmethodID WCP_moveTo_mID;
extern jmethodID WCP_addLineTo_mID;
extern jmethodID WCP_addBezierCurveTo_mID;
extern jmethodID WCP_addArcTo_mID;
extern jmethodID WCP_closeSubpath_mID;
extern jmethodID WCP_addArc_mID;
extern jmethodID WCP_addRect_mID;
extern jmethodID WCP_addEllipse_mID;
