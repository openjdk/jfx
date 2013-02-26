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

#ifndef PATHCONSUMER_H
#define PATHCONSUMER_H

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

typedef struct _PathConsumer PathConsumer;

typedef void MoveToFunc(PathConsumer *pConsumer,
                        jfloat x0, jfloat y0);
typedef void LineToFunc(PathConsumer *pConsumer,
                        jfloat x1, jfloat y1);
typedef void QuadToFunc(PathConsumer *pConsumer,
                        jfloat xc, jfloat yc,
                        jfloat x1, jfloat y1);
typedef void CurveToFunc(PathConsumer *pConsumer,
                         jfloat xc0, jfloat yc0,
                         jfloat xc1, jfloat yc1,
                         jfloat x1, jfloat y1);
typedef void ClosePathFunc(PathConsumer *pConsumer);
typedef void PathDoneFunc(PathConsumer *pConsumer);

struct _PathConsumer {
    MoveToFunc       *moveTo;
    LineToFunc       *lineTo;
    QuadToFunc       *quadTo;
    CurveToFunc      *curveTo;
    ClosePathFunc    *closePath;
    PathDoneFunc     *pathDone;
};

extern void PathConsumer_init(PathConsumer *pConsumer,
                              MoveToFunc       *moveTo,
                              LineToFunc       *lineTo,
                              QuadToFunc       *quadTo,
                              CurveToFunc      *curveTo,
                              ClosePathFunc    *closePath,
                              PathDoneFunc     *pathDone);

#ifdef __cplusplus
}
#endif

#endif /* PATHCONSUMER_H */

