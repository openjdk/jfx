/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>

#include "Transformer.h"

#define this (*((Transformer *) pTransformer))

#define TX_Declare(TX_TYPE)                     \
static MoveToFunc       TX_TYPE ## _moveTo;     \
static LineToFunc       TX_TYPE ## _lineTo;     \
static QuadToFunc       TX_TYPE ## _quadTo;     \
static CurveToFunc      TX_TYPE ## _curveTo;

TX_Declare(Translate)
TX_Declare(DeltaScale)
TX_Declare(ScaleTranslate)
TX_Declare(DeltaTransform)
TX_Declare(Transform)

static ClosePathFunc    Transformer_closePath;
static PathDoneFunc     Transformer_pathDone;

#define TX_Init(TX_TYPE)                          \
    PathConsumer_init(&pTransformer->consumer,    \
                      TX_TYPE ## _moveTo,         \
                      TX_TYPE ## _lineTo,         \
                      TX_TYPE ## _quadTo,         \
                      TX_TYPE ## _curveTo,        \
                      Transformer_closePath,      \
                      Transformer_pathDone)

PathConsumer *Transformer_init(Transformer *pTransformer,
                               PathConsumer *out,
                               jdouble mxx, jdouble mxy, jdouble mxt,
                               jdouble myx, jdouble myy, jdouble myt)
{
    if (mxy == 0.0 && myx == 0.0) {
        if (mxx == 1.0 && myy == 1.0) {
            if (mxt == 0.0 && myt == 0.0) {
                return out;
            } else {
                TX_Init(Translate);
            }
        } else {
            if (mxt == 0.0 && myt == 0.0) {
                TX_Init(DeltaScale);
            } else {
                TX_Init(ScaleTranslate);
            }
        }
    } else if (mxt == 0.0 && myt == 0.0) {
        TX_Init(DeltaTransform);
    } else {
        TX_Init(Transform);
    }

    this.out = out;
    this.mxx = mxx;
    this.mxy = mxy;
    this.mxt = mxt;
    this.myx = myx;
    this.myy = myy;
    this.myt = myt;
    return &pTransformer->consumer;
}

static void Translate_moveTo(PathConsumer *pTransformer,
                             jfloat x0, jfloat y0)
{
    this.out->moveTo(this.out,
                     (jfloat) (x0 + this.mxt),
                     (jfloat) (y0 + this.myt));
}

static void Translate_lineTo(PathConsumer *pTransformer,
                             jfloat x1, jfloat y1)
{
    this.out->lineTo(this.out,
                     (jfloat) (x1 + this.mxt),
                     (jfloat) (y1 + this.myt));
}

static void Translate_quadTo(PathConsumer *pTransformer,
                             jfloat xc, jfloat yc,
                             jfloat x1, jfloat y1)
{
    this.out->quadTo(this.out,
                     (jfloat) (xc + this.mxt),
                     (jfloat) (yc + this.myt),
                     (jfloat) (x1 + this.mxt),
                     (jfloat) (y1 + this.myt));
}

static void Translate_curveTo(PathConsumer *pTransformer,
                              jfloat xc0, jfloat yc0,
                              jfloat xc1, jfloat yc1,
                              jfloat x1, jfloat y1)
{
    this.out->curveTo(this.out,
                      (jfloat) (xc0 + this.mxt),
                      (jfloat) (yc0 + this.myt),
                      (jfloat) (xc1 + this.mxt),
                      (jfloat) (yc1 + this.myt),
                      (jfloat) (x1  + this.mxt),
                      (jfloat) (y1  + this.myt));
}

static void DeltaScale_moveTo(PathConsumer *pTransformer,
                              jfloat x0, jfloat y0)
{
    this.out->moveTo(this.out,
                     (jfloat) (x0 * this.mxx),
                     (jfloat) (y0 * this.myy));
}

static void DeltaScale_lineTo(PathConsumer *pTransformer,
                              jfloat x1, jfloat y1)
{
    this.out->lineTo(this.out,
                     (jfloat) (x1 * this.mxx),
                     (jfloat) (y1 * this.myy));
}

static void DeltaScale_quadTo(PathConsumer *pTransformer,
                              jfloat xc, jfloat yc,
                              jfloat x1, jfloat y1)
{
    this.out->quadTo(this.out,
                     (jfloat) (xc * this.mxx),
                     (jfloat) (yc * this.myy),
                     (jfloat) (x1 * this.mxx),
                     (jfloat) (y1 * this.myy));
}

static void DeltaScale_curveTo(PathConsumer *pTransformer,
                               jfloat xc0, jfloat yc0,
                               jfloat xc1, jfloat yc1,
                               jfloat x1, jfloat y1)
{
    this.out->curveTo(this.out,
                      (jfloat) (xc0 * this.mxx),
                      (jfloat) (yc0 * this.myy),
                      (jfloat) (xc1 * this.mxx),
                      (jfloat) (yc1 * this.myy),
                      (jfloat) (x1  * this.mxx),
                      (jfloat) (y1  * this.myy));
}

static void DeltaTransform_moveTo(PathConsumer *pTransformer,
                                  jfloat x0, jfloat y0)
{
    this.out->moveTo(this.out,
                     (jfloat) (x0 * this.mxx + y0 * this.mxy),
                     (jfloat) (x0 * this.myx + y0 * this.myy));
}

static void DeltaTransform_lineTo(PathConsumer *pTransformer,
                                  jfloat x1, jfloat y1)
{
    this.out->lineTo(this.out,
                     (jfloat) (x1 * this.mxx + y1 * this.mxy),
                     (jfloat) (x1 * this.myx + y1 * this.myy));
}

static void DeltaTransform_quadTo(PathConsumer *pTransformer,
                                  jfloat xc, jfloat yc,
                                  jfloat x1, jfloat y1)
{
    this.out->quadTo(this.out,
                     (jfloat) (xc * this.mxx + yc * this.mxy),
                     (jfloat) (xc * this.myx + yc * this.myy),
                     (jfloat) (x1 * this.mxx + y1 * this.mxy),
                     (jfloat) (x1 * this.myx + y1 * this.myy));
}

static void DeltaTransform_curveTo(PathConsumer *pTransformer,
                                   jfloat xc0, jfloat yc0,
                                   jfloat xc1, jfloat yc1,
                                   jfloat x1, jfloat y1)
{
    this.out->curveTo(this.out,
                      (jfloat) (xc0 * this.mxx + yc0 * this.mxy),
                      (jfloat) (xc0 * this.myx + yc0 * this.myy),
                      (jfloat) (xc1 * this.mxx + yc1 * this.mxy),
                      (jfloat) (xc1 * this.myx + yc1 * this.myy),
                      (jfloat) (x1  * this.mxx + y1  * this.mxy),
                      (jfloat) (x1  * this.myx + y1  * this.myy));
}

static void ScaleTranslate_moveTo(PathConsumer *pTransformer,
                                  jfloat x0, jfloat y0)
{
    this.out->moveTo(this.out,
                     (jfloat) (x0 * this.mxx + this.mxt),
                     (jfloat) (y0 * this.myy + this.myt));
}

static void ScaleTranslate_lineTo(PathConsumer *pTransformer,
                                  jfloat x1, jfloat y1)
{
    this.out->lineTo(this.out,
                     (jfloat) (x1 * this.mxx + this.mxt),
                     (jfloat) (y1 * this.myy + this.myt));
}

static void ScaleTranslate_quadTo(PathConsumer *pTransformer,
                                  jfloat xc, jfloat yc,
                                  jfloat x1, jfloat y1)
{
    this.out->quadTo(this.out,
                     (jfloat) (xc * this.mxx + this.mxt),
                     (jfloat) (yc * this.myy + this.myt),
                     (jfloat) (x1 * this.mxx + this.mxt),
                     (jfloat) (y1 * this.myy + this.myt));
}

static void ScaleTranslate_curveTo(PathConsumer *pTransformer,
                                   jfloat xc0, jfloat yc0,
                                   jfloat xc1, jfloat yc1,
                                   jfloat x1, jfloat y1)
{
    this.out->curveTo(this.out,
                      (jfloat) (xc0 * this.mxx + this.mxt),
                      (jfloat) (yc0 * this.myy + this.myt),
                      (jfloat) (xc1 * this.mxx + this.mxt),
                      (jfloat) (yc1 * this.myy + this.myt),
                      (jfloat) (x1  * this.mxx + this.mxt),
                      (jfloat) (y1  * this.myy + this.myt));
}

static void Transform_moveTo(PathConsumer *pTransformer,
                             jfloat x0, jfloat y0)
{
    this.out->moveTo(this.out,
                     (jfloat) (x0 * this.mxx + y0 * this.mxy + this.mxt),
                     (jfloat) (x0 * this.myx + y0 * this.myy + this.myt));
}

static void Transform_lineTo(PathConsumer *pTransformer,
                             jfloat x1, jfloat y1)
{
    this.out->lineTo(this.out,
                     (jfloat) (x1 * this.mxx + y1 * this.mxy + this.mxt),
                     (jfloat) (x1 * this.myx + y1 * this.myy + this.myt));
}

static void Transform_quadTo(PathConsumer *pTransformer,
                             jfloat xc, jfloat yc,
                             jfloat x1, jfloat y1)
{
    this.out->quadTo(this.out,
                     (jfloat) (xc * this.mxx + yc * this.mxy + this.mxt),
                     (jfloat) (xc * this.myx + yc * this.myy + this.myt),
                     (jfloat) (x1 * this.mxx + y1 * this.mxy + this.mxt),
                     (jfloat) (x1 * this.myx + y1 * this.myy + this.myt));
}

static void Transform_curveTo(PathConsumer *pTransformer,
                              jfloat xc0, jfloat yc0,
                              jfloat xc1, jfloat yc1,
                              jfloat x1, jfloat y1)
{
    this.out->curveTo(this.out,
                      (jfloat) (xc0 * this.mxx + yc0 * this.mxy + this.mxt),
                      (jfloat) (xc0 * this.myx + yc0 * this.myy + this.myt),
                      (jfloat) (xc1 * this.mxx + yc1 * this.mxy + this.mxt),
                      (jfloat) (xc1 * this.myx + yc1 * this.myy + this.myt),
                      (jfloat) (x1  * this.mxx + y1  * this.mxy + this.mxt),
                      (jfloat) (x1  * this.myx + y1  * this.myy + this.myt));
}

static void Transformer_closePath(PathConsumer *pTransformer) {
    this.out->closePath(this.out);
}

static void Transformer_pathDone(PathConsumer *pTransformer) {
    this.out->pathDone(this.out);
}
