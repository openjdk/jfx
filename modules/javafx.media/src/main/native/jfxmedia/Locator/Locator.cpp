/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

#include <Common/ProductFlags.h>
#include "Locator.h"
#include <jni/Logger.h>
#include <Common/VSMemory.h>

CLocator::CLocator(LocatorType type, const char* contentType, const char* location)
{
    LOGGER_LOGMSG(LOGGER_DEBUG, "CLocator::CLocator()");
    m_type = type;
    m_contentType = contentType;
    m_location = string(location);
    m_llSizeHint = -1;
}

CLocator::CLocator(LocatorType type, const char* contentType, const char* location, int64_t llSizeHint)
{
    LOGGER_LOGMSG(LOGGER_DEBUG, "CLocator::CLocator()");
    m_type = type;
    m_contentType = contentType;
    m_location = string(location);
    m_llSizeHint = llSizeHint;
}

CLocator::LocatorType CLocator::GetType()
{
    return m_type;
}

jstring CLocator::LocatorGetStringLocation(JNIEnv *env, jobject locator)
{
    static jmethodID mid_toString = 0;
    jstring result = NULL;
    CJavaEnvironment javaEnv(env);

    if (mid_toString == 0)
    {
        jclass klass = env->GetObjectClass(locator);

        mid_toString = env->GetMethodID(klass, "getStringLocation", "()Ljava/lang/String;");
        env->DeleteLocalRef(klass);
        if (javaEnv.clearException())
        {
            return NULL;
        }
    }

    result = (jstring)env->CallObjectMethod(locator, mid_toString);
    if (javaEnv.clearException())
    {
        return NULL;
    }

    return result;
}

int64_t CLocator::GetSizeHint()
{
    return m_llSizeHint;
}
