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

#ifndef PISCES_MATH_H
#define PISCES_MATH_H

#include <PiscesDefs.h>

#define PI_DOUBLE 3.141592653589793L

#define PISCES_PI ((jint)(PI_DOUBLE*65536.0))
#define PISCES_TWO_PI ((jint)(2.0*PI_DOUBLE*65536.0))
#define PISCES_PI_OVER_TWO ((jint)((PI_DOUBLE/2.0)*65536.0))
#define PISCES_SQRT_TWO ((jint)(1.414213562373095*65536.0))
#define PISCES_360_DEGREES (jint)(360 *65536.0)
#define PISCES_RADIANS_TO_DEGREES_MULTIPLIER (jint)(65536.0 * 360.0 /(2.0 * PI_DOUBLE ))
#define PISCES_DEGREES_TO_RADIANS_MULTIPLIER (jint)(((2.0*PI_DOUBLE/360.0))*65536.0)
#define PISCES_CV (jint)(0.5522847498307933 * 65536.0)

jboolean piscesmath_moduleInitialize();
void piscesmath_moduleFinalize();

jint piscesmath_sin(jint theta);
jint piscesmath_cos(jint theta);
jdouble piscesmath_dhypot(jdouble x, jdouble y);
jint piscesmath_toDegrees(jint thetaRadians);
jint piscesmath_toRadians(jint thetaDegrees);

jint piscesmath_ceil(jfloat x);
jint piscesmath_abs(jint x);

jfloat piscesmath_btan(jfloat x);
jfloat piscesmath_acos(jfloat value);
jfloat piscesmath_asin(jfloat value);
jfloat piscesmath_mod(jfloat x, int y) ;
#endif
