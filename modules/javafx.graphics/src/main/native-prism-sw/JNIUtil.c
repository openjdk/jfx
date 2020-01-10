/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <JNIUtil.h>
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL
JNI_OnLoad_prism_sw(JavaVM *vm, void * reserved) {
#ifdef JNI_VERSION_1_8
    //min. returned JNI_VERSION required by JDK8 for builtin libraries
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_4;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_4;
#endif
}
#endif // STATIC_BUILD


jboolean
initializeFieldIds(jfieldID* dest, JNIEnv* env, jclass classHandle,
                   const FieldDesc* fields) {
    jboolean retVal = JNI_TRUE;

    while (fields->name != NULL) {
        *dest = (*env)->GetFieldID(env, classHandle, fields->name,
                                   fields->signature);
        prismsw_checkAndClearException(env);
        if (*dest == NULL) {
            retVal = JNI_FALSE;
            break;
        }
        ++fields;
        ++dest;
    }

    return retVal;
}

jboolean
initializeStaticFieldIds(jfieldID* dest, JNIEnv* env, jclass classHandle,
                         const FieldDesc* fields) {
    jboolean retVal = JNI_TRUE;

    while (fields->name != NULL) {
        *dest = (*env)->GetStaticFieldID(env, classHandle, fields->name,
                                         fields->signature);
        prismsw_checkAndClearException(env);
        if (*dest == NULL) {
            retVal = JNI_FALSE;
            break;
        }
        ++fields;
        ++dest;
    }

    return retVal;
}

void
JNI_ThrowNew(JNIEnv* env, const char* throwable, const char* message) {
    jclass throwableClass;
    jint status;

    throwableClass = (*env)->FindClass(env, throwable);
    if ((*env)->ExceptionCheck(env) || throwableClass == NULL) {
        (*env)->FatalError(env, "Failed to load an exception class!");
        return;
    }

    status = (*env)->ThrowNew(env, throwableClass, message);
    if ((*env)->ExceptionCheck(env) || status != 0) {
        (*env)->FatalError(env, "Failed to throw an exception!");
    }
}

jboolean
prismsw_checkAndClearException(JNIEnv *env) {
    if (!(*env)->ExceptionCheck(env)) {
        return JNI_FALSE;
    }
    (*env)->ExceptionClear(env);
    return JNI_TRUE;
}

