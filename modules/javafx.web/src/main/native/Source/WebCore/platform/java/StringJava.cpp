/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "PlatformJavaClasses.h"
#include "StringJava.h"

namespace WebCore {

using WTF::String;

jobjectArray strVect2JArray(JNIEnv* env, const Vector<String>& strVect)
{
    if (!strVect.size()) {
        jobjectArray arr = (jobjectArray) env->NewObjectArray(0,
            JLClass(env->FindClass("java/lang/String")), 0);
        WTF::CheckAndClearException(env); // OOME
        return arr;
    }

    ASSERT(strVect[0]);
    JLString str(strVect[0].toJavaString(env));

    JLClass sclass(env->GetObjectClass(str));
    jobjectArray strArray =
        (jobjectArray) env->NewObjectArray(strVect.size(), sclass, 0);
    WTF::CheckAndClearException(env); // OOME

    env->SetObjectArrayElement(strArray, 0, (jstring)str);

    for (size_t i = 1; i < strVect.size(); i++) {
        ASSERT(strVect[i]);
        str = strVect[i].toJavaString(env);
        env->SetObjectArrayElement(strArray, i, (jstring)str);
    }

    return strArray;
}

} // namespace WebCore
