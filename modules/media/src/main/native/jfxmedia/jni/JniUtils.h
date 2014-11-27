/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _JNI_UTILS_H_
#define _JNI_UTILS_H_

#include <jni.h>
#include <string>

#if defined (_LP64) || defined(_WIN64)
#define jlong_to_ptr(a) ((void*)(a))
#define ptr_to_jlong(a) ((jlong)(a))
#else
#define jlong_to_ptr(a) ((void*)(int)(a))
#define ptr_to_jlong(a) ((jlong)(int)(a))
#endif

// Throws an exception of the given type (class name)
// if type is NULL, then will throw a generic Exception
void ThrowJavaException(JNIEnv *env, const char* type, const char* message);

// Gets a valid, usable JNIEnv for the current thread
// if didAttach is true on return then you should call
// jvm->DetachCurrentThread() when done
JNIEnv *GetJavaEnvironment(JavaVM *jvm, jboolean &didAttach);


/*
 * Example usage of this class:
 * {
 *     JavaVM *jvm = ...;
 *     CJavaEnvironment jenv(jvm);
 *     if ((env = jenv->getEnvironment()) != NULL) {
 *         env->...;
 *     }
 * }
 */
class CJavaEnvironment {
public:
    CJavaEnvironment(JavaVM *);
    CJavaEnvironment(JNIEnv *); // create with an existing JNIEnv
    ~CJavaEnvironment();

    JNIEnv *getEnvironment();
    bool hasException();    // return true if an exception is raised (but do nothing with it)
    bool clearException();  // if an exception is raised, clear it and return true
    bool reportException(); // as above but log the exception to Logger
    void throwException(std::string message); // Throw an exception with the given message

private:
    JNIEnv *environment;
    jboolean attached;
};

#endif  //_JNI_UTILS_H_
