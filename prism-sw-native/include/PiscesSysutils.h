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

#ifndef PISCES_SYSUTILS_H
#define PISCES_SYSUTILS_H

#include <PiscesDefs.h>

// for malloc, etc.
#include <stdlib.h>

#define PISCESmalloc(x) malloc(x)
#define PISCESfree(x) free(x)
#define PISCEScalloc(x, y) calloc((x), (y))
#define PISCESrealloc(x,y) realloc((x), (y))

// for memcpy
#include <string.h>

#define PISCESclear_mem(buffer,count) memset(buffer,0,count)

// for sqrt, sin, cos
#include <math.h>

jboolean readAndClearMemErrorFlag();
jboolean readMemErrorFlag();
void setMemErrorFlag();

#define PISCESsqrt(x) sqrt((x))
#define PISCESsin(x) sin((x))
#define PISCEScos(x) cos((x))


#ifdef _MSC_VER
typedef unsigned __int64    ulong64;
#else
typedef unsigned long long  ulong64;
#endif


#ifdef DEBUG
// for assert(int )
#include <assert.h>
#else

// a definition for assert(). Does nothing!
#define assert(z)

#endif

#define ASSERT_ALLOC(memptr) \
    if (NULL == (memptr)) {  \
        setMemErrorFlag();   \
        return;              \
    }

#define ASSERT_ALLOC_BOOLEAN(memptr) \
    if (NULL == (memptr)) {          \
        setMemErrorFlag();           \
        return XNI_FALSE;            \
    }

#define ASSERT_ALLOC_POINTER(memptr) \
    if (NULL == (memptr)) {          \
        setMemErrorFlag();           \
        return NULL;                 \
    }

#define ASSERT_ALLOC_NO_RETURN(memptr) \
    if (NULL == (memptr)) {            \
        setMemErrorFlag();             \
    }

#endif //PISCES_SYSUTILS_H
