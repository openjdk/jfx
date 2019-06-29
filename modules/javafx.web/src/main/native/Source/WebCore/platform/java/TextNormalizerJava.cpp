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
#include "TextNormalizerJava.h"

namespace WebCore {

namespace TextNormalizer {

    static JGClass textNormalizerClass;
    static jmethodID normalizeMID;

    static JNIEnv* setUpNormalizer(void)
    {
        JNIEnv* env = WTF::GetJavaEnv();

        if (! textNormalizerClass) {
            textNormalizerClass = JLClass(env->FindClass(
                         "com/sun/webkit/text/TextNormalizer"));
            ASSERT(textNormalizerClass);
            normalizeMID = env->GetStaticMethodID(textNormalizerClass,
                    "normalize", "(Ljava/lang/String;I)Ljava/lang/String;");
            ASSERT(normalizeMID);
        }

        return env;
    }

    String normalize(const UChar* data, int length, Form form)
    {
        JNIEnv *env = setUpNormalizer();

        JLString jData(env->NewString(reinterpret_cast<const jchar*>(data), length));
        ASSERT(jData);
        WTF::CheckAndClearException(env); // OOME

        JLString s(static_cast<jstring>(env->CallStaticObjectMethod(
                textNormalizerClass, normalizeMID, (jstring)jData, form)));
        ASSERT(s);
        WTF::CheckAndClearException(env);

        return String(env, s);
    }

} // namespace TextNormalizer

} // namespace WebCore
