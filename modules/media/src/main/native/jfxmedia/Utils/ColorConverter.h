/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __COLOR_CONVERTER_H__
#define __COLOR_CONVERTER_H__

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

    int ColorConvert_YCbCr420p_to_ARGB32(uint8_t *argb,
                                         int32_t argb_stride,
                                         int32_t width,
                                         int32_t height,
                                         const uint8_t *y,
                                         const uint8_t *v,
                                         const uint8_t *u,
                                         const uint8_t *a,
                                         int32_t y_stride,
                                         int32_t v_stride,
                                         int32_t u_stride,
                                         int32_t a_stride);

    int ColorConvert_YCbCr420p_to_ARGB32_no_alpha(uint8_t *argb,
                                                  int32_t argb_stride,
                                                  int32_t width,
                                                  int32_t height,
                                                  const uint8_t *y,
                                                  const uint8_t *v,
                                                  const uint8_t *u,
                                                  int32_t y_stride,
                                                  int32_t v_stride,
                                                  int32_t u_stride);

    int ColorConvert_YCbCr420p_to_BGRA32(uint8_t *argb,
                                         int32_t argb_stride,
                                         int32_t width,
                                         int32_t height,
                                         const uint8_t *y,
                                         const uint8_t *v,
                                         const uint8_t *u,
                                         const uint8_t *a,
                                         int32_t y_stride,
                                         int32_t v_stride,
                                         int32_t u_stride,
                                         int32_t a_stride);

    int ColorConvert_YCbCr420p_to_BGRA32_no_alpha(uint8_t *bgra,
                                                  int32_t bgra_stride,
                                                  int32_t width,
                                                  int32_t height,
                                                  const uint8_t *y,
                                                  const uint8_t *v,
                                                  const uint8_t *u,
                                                  int32_t y_stride,
                                                  int32_t v_stride,
                                                  int32_t u_stride);

    int ColorConvert_YCbCr422p_to_ARGB32_no_alpha(uint8_t *argb,
                                                  int32_t argb_stride,
                                                  int32_t width,
                                                  int32_t height,
                                                  const uint8_t *y,
                                                  const uint8_t *v,
                                                  const uint8_t *u,
                                                  int32_t y_stride,
                                                  int32_t uv_stride);


    int ColorConvert_YCbCr422p_to_BGRA32_no_alpha(uint8_t *bgra,
                                                  int32_t bgra_stride,
                                                  int32_t width,
                                                  int32_t height,
                                                  const uint8_t *y,
                                                  const uint8_t *v,
                                                  const uint8_t *u,
                                                  int32_t y_stride,
                                                  int32_t uv_stride);

#ifdef __cplusplus
};
#endif

#endif // __COLOR_CONVERTER_H__
