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

/**
 * @file PiscesUtil.h 
 * PISCES memory management and other macro definitions.
 */ 

#ifndef PISCES_UTIL_H
#define PISCES_UTIL_H

#include <PiscesDefs.h>

#include <PiscesSysutils.h>

#ifndef ABS
/**
 * @def ABS(x)
 * Absolute value of x.
 */  
#define ABS(x) ((x) > 0 ? (x) : -(x))
#endif

#ifndef MIN
/**
 * @def MIN(a,b)
 * This macro gives minimum of a,b.
 */  
#define MIN(a,b) ((a)<(b)?(a):(b))
#endif

#ifndef MAX
/**
 * @def MAX(a,b)
 * This macro gives maximum of a,b.
 */
#define MAX(a,b) ((a)>(b)?(a):(b))
#endif

/**
 * @def my_malloc(type, len)
 * Allocates and cleares memory to zeros. Returns pointer to this memory 
 * type-casted to (type *). Size of allocated buffer is len*sizeof(type) bytes
 * long. 
 */
#define my_malloc(type, len) (type *)PISCEScalloc(len, sizeof(type))

/**
 * @def my_free(x)
 * Deallocates memory pointed to by pointer x. If x is NULL, does nothing.
 */  
#define my_free(x) do { if (x) PISCESfree(x); } while(0)

/* Clears count of bytes of memory pointed to by buffer to zero */
#define my_clear_mem(buffer,count) PISCESclear_mem(buffer,count)
 
/*
 * If 'array' is null or smaller than 'thresh', allocate with
 * length MAX(thresh, len).  Discard old contents.
 */
#define ALLOC(array, type, thresh, len) do { \
  if (array == NULL || array##_length < (thresh)) { \
    jint nlen = MAX(thresh, len); \
    PISCESfree(array); \
    array = my_malloc(type, nlen); \
    array##_length = nlen; \
  } \
} while (0)

/**
 * @def ALLOC3(array, type, len)
 * If 'array' is null or smaller than 'len', allocate with
 * length len.  Discard old contents.
 */
#define ALLOC3(array, type, len) ALLOC(array, type, len, len)

/**
 * @def REALLOC(array, type, thresh, len)
 * If 'array' is null or smaller than 'thresh', allocate with
 * length max(thresh, len).  Copy old contents into new storage.
 */
#define REALLOC(array, type, thresh, len) do { \
  if (array == NULL || array##_length < (thresh)) { \
    jint nlen; \
    nlen = MAX(thresh, len); \
    array = (type *)PISCESrealloc((array), nlen*sizeof(type)); \
    array##_length = nlen; \
  } \
} while (0)

/**
 * @def SHRINK(array, type, maxLen)
 * If 'array' is null or larger than 'maxLen', allocate with
 * length maxLen.  Discard old contents.
 */
#define SHRINK(array, type, maxLen) do { \
  if (array == NULL || array##_length > (maxLen)) { \
    if(array != NULL && array##_length > (maxLen)) { \
        PISCESfree(array); \
    } \
    array = my_malloc(type, (maxLen)); \
    array##_length = (maxLen); \
  } \
} while (0)

/** Convert 24-bit RGB color to 16bit (565) color */
#define CONVERT_888_TO_565_VALS(r, g, b) \
                ( (((r) & 0xF8) << 8) | (((g) & 0xFC) << 3) | ((b) >> 3))

/** Convert 24-bit RGB color to 16bit (565) color */
#define CONVERT_888_TO_565(x) ((( (x) & 0x00F80000) >> 8) | \
                (( (x) & 0x0000FC00) >> 5) | \
                (( (x) & 0x000000F8) >> 3) )

/** Convert 16-bit (565) color to 24-bit RGB color */
#define CONVERT_565_TO_888(x) ( ((x & 0x001F) << 3) | ((x & 0x001C) >> 2) |\
                              ((x & 0x07E0) << 5) | ((x & 0x0600) >> 1) |\
                              ((x & 0xF800) << 8) | ((x & 0xE000) << 3) )

extern jint PISCES_STROKE_X_BIAS;
extern jint PISCES_STROKE_Y_BIAS;

jboolean piscesutil_moduleInitialize();
void piscesutil_moduleFinalize();
void piscesutil_setStrokeBias(jint xbias, jint ybias);

#define PointerToJLong ptr_to_jlong
#define JLongToPointer jlong_to_ptr

#endif
