/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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


#include "PiscesSurface.h"

#include <PiscesUtil.h>

#include <PiscesSysutils.h>

static INLINE void surface_dispose(Surface* surface);

static INLINE void surface_setRGB(Surface* dstSurface, jint x, jint y,
                                  jint width, jint height, jint* data,
                                  jint scanLength);


static void setRGB(jint* src, jint srcScanLength, jint* dst, jint dstScanLength,
                   jint width, jint height);


static INLINE void
surface_dispose(Surface* surface) {
    my_free(surface);
}

static INLINE void
surface_setRGB(Surface* dstSurface, jint x, jint y,
               jint width, jint height, jint* data, jint scanLength) {
    setRGB((jint*)dstSurface->data + y * dstSurface->width + x, 
           dstSurface->width, data, scanLength, width, height);
}


static void
setRGB(jint* dst, jint dstScanLength, jint* src, jint srcScanLength,
       jint width, jint height) {
    jint srcScanRest = srcScanLength - width;
    jint dstScanRest = dstScanLength - width;

    for (; height > 0; --height) {
        jint w2 = width;
        for (; w2 > 0; --w2) {
            *dst++ = *src++;
        }
        src += srcScanRest;
        dst += dstScanRest;
    }
}

