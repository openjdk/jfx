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

#include "RQRef.h"

namespace WebCore {

RQRef::~RQRef()
{
    if (-1 != m_refID) {
        JNIEnv* env = WTF::GetJavaEnv();

        if (env) {
            //do it if JVM is here.
            static jmethodID mid = env->GetMethodID(PG_GetRefClass(env), "deref", "()V");
            ASSERT(mid);
            env->CallVoidMethod(m_ref, mid);

            WTF::CheckAndClearException(env);
        }
    }
}

RQRef::operator jint() {
    if (-1 == m_refID) {
        JNIEnv* env = WTF::GetJavaEnv();

        static jmethodID midGetId = env->GetMethodID(PG_GetRefClass(env), "getID", "()I");
        ASSERT(midGetId);
        m_refID = env->CallIntMethod(m_ref, midGetId);

        static jmethodID midRef = env->GetMethodID(PG_GetRefClass(env), "ref", "()V");
        ASSERT(midRef);
        env->CallVoidMethod(m_ref, midRef);

        WTF::CheckAndClearException(env);
    }
    return m_refID;
}

} // namespace WebCore
