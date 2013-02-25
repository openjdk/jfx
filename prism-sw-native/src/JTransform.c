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

#include <JTransform.h>

#include <JNIUtil.h>

#define TRANSFORM_M00 0
#define TRANSFORM_M01 1
#define TRANSFORM_M10 2
#define TRANSFORM_M11 3
#define TRANSFORM_M02 4
#define TRANSFORM_M12 5
#define TRANSFORM_LAST TRANSFORM_M12

static jfieldID fieldIds[TRANSFORM_LAST + 1];
static jboolean fieldIdsInitialized = JNI_FALSE;

static jboolean initializeTransformFieldIds(JNIEnv* env, jobject objectHandle);

void
transform_get6(Transform6* transform, JNIEnv* env, jobject object) {
    transform->m00 = (*env)->GetIntField(env, object, fieldIds[TRANSFORM_M00]);
    transform->m01 = (*env)->GetIntField(env, object, fieldIds[TRANSFORM_M01]);
    transform->m10 = (*env)->GetIntField(env, object, fieldIds[TRANSFORM_M10]);
    transform->m11 = (*env)->GetIntField(env, object, fieldIds[TRANSFORM_M11]);
    transform->m02 = (*env)->GetIntField(env, object, fieldIds[TRANSFORM_M02]);
    transform->m12 = (*env)->GetIntField(env, object, fieldIds[TRANSFORM_M12]);
}

void
transform_set6(JNIEnv* env, jobject object, const Transform6* transform) {
    (*env)->SetIntField(env, object, fieldIds[TRANSFORM_M00], transform->m00);
    (*env)->SetIntField(env, object, fieldIds[TRANSFORM_M01], transform->m01);
    (*env)->SetIntField(env, object, fieldIds[TRANSFORM_M10], transform->m10);
    (*env)->SetIntField(env, object, fieldIds[TRANSFORM_M11], transform->m11);
    (*env)->SetIntField(env, object, fieldIds[TRANSFORM_M02], transform->m02);
    (*env)->SetIntField(env, object, fieldIds[TRANSFORM_M12], transform->m12);
}

JNIEXPORT void JNICALL
Java_com_sun_pisces_Transform6_initialize(JNIEnv* env, jobject objectHandle) {
    if (!initializeTransformFieldIds(env, objectHandle)) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "");
    }
}

static jboolean
initializeTransformFieldIds(JNIEnv* env, jobject objectHandle) {
    static const FieldDesc transformFieldDesc[] = {
                { "m00", "I" },
                { "m01", "I" },
                { "m10", "I" },
                { "m11", "I" },
                { "m02", "I" },
                { "m12", "I" },
                { NULL, NULL }
            };

    jboolean retVal;
    jclass classHandle;

    if (fieldIdsInitialized) {
        return JNI_TRUE;
    }

    retVal = JNI_FALSE;

    classHandle = (*env)->GetObjectClass(env, objectHandle);

    if (initializeFieldIds(fieldIds, env, classHandle, transformFieldDesc)) {
        retVal = JNI_TRUE;
        fieldIdsInitialized = JNI_TRUE;
    }

    return retVal;
}
