/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @file PiscesTransform.c
 * Basic matrix algebra.
 */

#include <PiscesTransform.h>
#include <PiscesSysutils.h>

/**
 * This function simply copies data from source transformation matrix pointed to
 * by transformS to destination pointed to by transformD.
 */
void
pisces_transform_assign(Transform6* transformD, const Transform6* transformS) {
    memcpy(transformD, transformS, sizeof(Transform6));
}

/**
 * This function computes inverse transformation matrix of *transform. Result is
 * stored in structure pointed to by transform.
 */
void
pisces_transform_invert(Transform6* transform) {
    jfloat fm00 = transform->m00/65536.0f;
    jfloat fm01 = transform->m01/65536.0f;
    jfloat fm02 = transform->m02/65536.0f;
    jfloat fm10 = transform->m10/65536.0f;
    jfloat fm11 = transform->m11/65536.0f;
    jfloat fm12 = transform->m12/65536.0f;
    jfloat fdet = fm00*fm11 - fm01*fm10;

    jfloat fa00 =  fm11/fdet;
    jfloat fa01 = -fm01/fdet;
    jfloat fa10 = -fm10/fdet;
    jfloat fa11 =  fm00/fdet;
    jfloat fa02 = (fm01*fm12 - fm02*fm11)/fdet;
    jfloat fa12 = (fm02*fm10 - fm00*fm12)/fdet;

    transform->m00 = (jint)(fa00*65536.0f);
    transform->m01 = (jint)(fa01*65536.0f);
    transform->m10 = (jint)(fa10*65536.0f);
    transform->m11 = (jint)(fa11*65536.0f);
    transform->m02 = (jint)(fa02*65536.0f);
    transform->m12 = (jint)(fa12*65536.0f);
}
