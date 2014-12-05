/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "Logger.h"
#include "JniUtils.h"

#include <Common/VSMemory.h>

#if ENABLE_LOGGING

CLogger::LSingleton CLogger::s_Singleton;

bool CLogger::canLog(int level)
{
    if (level < m_currentLevel)
    {
        return false;
    }
    else
    {
        return true;
    }
}

void CLogger::logMsg(int level, const char *msg)
{
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *env = javaEnv.getEnvironment(); // env could be NULL

    if (!env || level < m_currentLevel || !m_areJMethodIDsInitialized) {
        return;
    }

    jstring jmsg = env->NewStringUTF(msg);
    env->CallStaticVoidMethod(m_cls, m_logMsg1Method, (jint)level, jmsg);
}

void CLogger::logMsg(int level, const char *sourceClass, const char *sourceMethod, const char *msg)
{
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *env = javaEnv.getEnvironment();

    if (!env || level < m_currentLevel || !m_areJMethodIDsInitialized) {
        return;
    }

    jstring jsourceClass = env->NewStringUTF(sourceClass);
    jstring jsourceMethod = env->NewStringUTF(sourceMethod);
    jstring jmsg = env->NewStringUTF(msg);

    env->CallStaticVoidMethod(m_cls, m_logMsg2Method, (jint)level, jsourceClass, jsourceMethod, jmsg);
}

// Do NOT use this function. Instead use init() from Java layer.
bool CLogger::init(JNIEnv *pEnv, jclass cls)
{
    if (!pEnv || !cls) {
        return false;
    }
    pEnv->GetJavaVM(&m_jvm);
    if (!m_areJMethodIDsInitialized) {
        jclass local_cls = pEnv->FindClass("com/sun/media/jfxmedia/logging/Logger");
        if (NULL != local_cls) {
            // Get global reference
            m_cls = (jclass)pEnv->NewWeakGlobalRef(local_cls);
            pEnv->DeleteLocalRef(local_cls);

            if (NULL != m_cls) {
                m_logMsg1Method = pEnv->GetStaticMethodID(m_cls, "logMsg", "(ILjava/lang/String;)V");
                m_logMsg2Method = pEnv->GetStaticMethodID(m_cls, "logMsg", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

                if (NULL != m_logMsg1Method && NULL != m_logMsg2Method) {
                    m_areJMethodIDsInitialized = true;
                }
            }
        }
    }

    return m_areJMethodIDsInitialized;
}

// Do NOT use this function. Instead use setLevel() from Java layer.
void CLogger::setLevel(int level)
{
    m_currentLevel = level;
}

uint32_t CLogger::CreateInstance(CLogger **ppLogger)
{
    if (ppLogger == NULL) {
        return ERROR_FUNCTION_PARAM_NULL;
    }

    *ppLogger = new CLogger();
    if (*ppLogger == NULL) {
        return ERROR_MEMORY_ALLOCATION;
    }

    return ERROR_NONE;
}

#endif // ENABLE_LOGGING
