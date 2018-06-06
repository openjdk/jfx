/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "com_sun_prism_j2d_print_J2DPrinterJob.h"

/*
 * Class com_sun_prism_j2d_print_J2DPrinterJob2D
 * Method: getAlwaysOnTop
 * Signature (Ljava/lang/Class;J)Ljavax.print.attribute.standard.DialogOwner;
 */
JNIEXPORT jobject
Java_com_sun_prism_j2d_print_J2DPrinterJob_getAlwaysOnTop(
 JNIEnv *env, jclass cls, jclass ownerClass, jlong id) {

    jmethodID cons;
    if (ownerClass == NULL) {
        return NULL;
    }
    cons = (*env)->GetMethodID(env, ownerClass, "<init>", "(J)V");
    if (cons == NULL || (*env)->ExceptionCheck(env)) {
        return NULL;
    }
    return (*env)->NewObject(env, ownerClass, cons, id);
}

