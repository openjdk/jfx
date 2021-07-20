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

#pragma once

#include <wtf/java/JavaRef.h>

#include <jni.h>

extern JavaVM* jvm;

#define WC_GETJAVAENV_CHKRET(_env_var, ... /* ret val */)   \
    JNIEnv* _env_var = WTF::GetJavaEnv(); \
    if (!_env_var) return __VA_ARGS__;

namespace WTF {

ALWAYS_INLINE JNIEnv* JNICALL GetJavaEnv()
{
    void* env;
    jvm->GetEnv(&env, JNI_VERSION_1_2);
    return (JNIEnv*)env;
}

bool CheckAndClearException(JNIEnv* env);

JLObject PL_GetLogger(JNIEnv* env, const char* name);
void PL_ResumeCount(JNIEnv* env, jobject perfLogger, const char* probe);
void PL_SuspendCount(JNIEnv* env, jobject perfLogger, const char* probe);
bool PL_IsEnabled(JNIEnv* env, jobject perfLogger);

//Log wrapper
struct EntryJavaLogger
{
    JNIEnv     *m_env;
    jobject     m_perfLogger;
    const char *m_probe;

    EntryJavaLogger(
         JNIEnv *env,
         jobject global_perfLogger,
         const char* probe
    ) : m_env(env)
      , m_perfLogger(global_perfLogger)
      , m_probe(probe)
    {
        PL_ResumeCount(m_env, m_perfLogger, m_probe);
    }

    ~EntryJavaLogger()
    {
        PL_SuspendCount(m_env, m_perfLogger, m_probe);
    }
};


} // namespace WTF

namespace WTF {
template<bool daemon> class AttachThreadToJavaEnv {
public:
    AttachThreadToJavaEnv()
    {
        m_status = jvm->GetEnv((void **)&m_env, JNI_VERSION_1_2);
        if (m_status == JNI_EDETACHED) {
            if (daemon) {
                jvm->AttachCurrentThreadAsDaemon((void **)&m_env, nullptr);
            } else {
                jvm->AttachCurrentThread((void **)&m_env, nullptr);
            }
        }
    }

    ~AttachThreadToJavaEnv()
    {
        if (m_status == JNI_EDETACHED) {
            jvm->DetachCurrentThread();
        }
    }

    JNIEnv* env() { return m_env; }
private:
    JNIEnv* m_env;
    int m_status;

};

using AttachThreadAsDaemonToJavaEnv = AttachThreadToJavaEnv<true>;
using AttachThreadAsNonDaemonToJavaEnv = AttachThreadToJavaEnv<false>;
} // namespace

//example: LOG_PERF_RECORD(env, "XXXX", "setUpIterator")
//the line
//  com.sun.webkit.perf.XXXX.level = ALL
//have to be added into the file <wk_root>/WebKitBuild/<Debug|Release>/dist/logging.properties
#define LOG_PERF_RECORD(env, LOG_NAME, LOG_RECORD) \
    static JGObject __logger__(WTF::PL_GetLogger(env, LOG_NAME)); \
    WTF::EntryJavaLogger __el__(env, __logger__, LOG_RECORD);

#define jlong_to_ptr(a) ((void*)(uintptr_t)(a))
#define ptr_to_jlong(a) ((jlong)(uintptr_t)(a))

#define bool_to_jbool(a) ((a) ? JNI_TRUE : JNI_FALSE)
#define jbool_to_bool(a) (((a) == JNI_TRUE) ? true : false)

#define JINT_SZ sizeof(jint)
#define JFLOAT_SZ sizeof(jfloat)

namespace WTF {
extern JGClass comSunWebkitFileSystem;
}
