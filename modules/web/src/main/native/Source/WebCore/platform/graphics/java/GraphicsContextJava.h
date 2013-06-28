/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef GraphicsContextJava_h
#define GraphicsContextJava_h

#include <jni.h>
#include "JavaEnv.h"

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

#endif
