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
 * @file PiscesTransform.h
 * Struct declarations of PISCES transformation matrixes.
 */ 

#ifndef PISCES_TRANSFORM_H
#define PISCES_TRANSFORM_H

#include <PiscesDefs.h>

/**
 * @struct _Transform6
 * Declaration of PISCES transformation matrix structure. Matrix is in 
 * normalized homogenous coordinates. This is convenient because we can
 * make compound transformations by matrix multiplication of basic 
 * transformation matrices (translation, scale and symmetry, rotation).   
 * Because third row is always (0,0,1), we keep members of first two rows only 
 * (2-rows x 3-columns).
 * \typedef Transform6
 * Typedef to struct _Transform6. 
 */
typedef struct _Transform6 {         
    jint m00;
    jint m01;
    jint m10;     
    jint m11;              
    jint m02;              
    jint m12;
}

Transform6;
/* See implementation or documentation on comments. */ 
void pisces_transform_assign(Transform6* transformD, 
                             const Transform6* transformS);

/* See implementation or documentation on comments. */                             
void pisces_transform_invert(Transform6* transform);

/* See implementation or documentation on comments. */
void pisces_transform_multiply(Transform6* transformD, 
                               const Transform6* transformS);
                              
/** Returns XNI_TRUE if tr1 and tr2 define same transformation matrix */ 
jboolean pisces_transform_equals(const Transform6* tr1, const Transform6* tr2);

#endif
