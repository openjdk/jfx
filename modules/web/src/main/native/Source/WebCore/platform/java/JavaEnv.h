/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef JavaEnv_h
#define JavaEnv_h

#include "JavaRef.h"

#include <jni.h>

extern JavaVM* jvm;

ALWAYS_INLINE JNIEnv* JNICALL WebCore_GetJavaEnv()
{
    void* env;
    jvm->GetEnv(&env, JNI_VERSION_1_2);
    return (JNIEnv*)env;
}

bool CheckAndClearException(JNIEnv* env);

namespace WebCore {

jclass PG_GetFontClass(JNIEnv* env);
jclass PG_GetFontCustomPlatformDataClass(JNIEnv* env);
jclass PG_GetGraphicsImageDecoderClass(JNIEnv* env);
jclass PG_GetGraphicsContextClass(JNIEnv* env);
jclass PG_GetGraphicsManagerClass(JNIEnv* env);
jclass PG_GetImageClass(JNIEnv* env);
jclass PG_GetMediaPlayerClass(JNIEnv* env);
jclass PG_GetPathClass(JNIEnv* env);
jclass PG_GetPathIteratorClass(JNIEnv* env);
jclass PG_GetRectangleClass(JNIEnv* env);
jclass PG_GetRefClass(JNIEnv* env);
jclass PG_GetRenderQueueClass(JNIEnv* env);
jclass PG_GetTransformClass(JNIEnv* env);
jclass PG_GetWebPageClass(JNIEnv* env);

jclass getTimerClass(JNIEnv* env);

JLObject PL_GetLogger(JNIEnv* env, const char* name);
void PL_ResumeCount(JNIEnv* env, jobject perfLogger, const char* probe);
void PL_SuspendCount(JNIEnv* env, jobject perfLogger, const char* probe);
bool PL_IsEnabled(JNIEnv* env, jobject perfLogger);

JLObject PL_GetGraphicsManager(JNIEnv* env);


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

} // namespace WebCore

//example: LOG_PERF_RECORD(env, "XXXX", "setUpIterator")
//the line
//  com.sun.webkit.perf.XXXX.level = ALL
//have to be added into the file <wk_root>/WebKitBuild/<Debug|Release>/dist/logging.properties
#define LOG_PERF_RECORD(env, LOG_NAME, LOG_RECORD) \
    static JGObject __logger__(WebCore::PL_GetLogger(env, LOG_NAME)); \
    WebCore::EntryJavaLogger __el__(env, __logger__, LOG_RECORD);

#define jlong_to_ptr(a) ((void*)(uintptr_t)(a))
#define ptr_to_jlong(a) ((jlong)(uintptr_t)(a))

#define bool_to_jbool(a) ((a) ? JNI_TRUE : JNI_FALSE)
#define jbool_to_bool(a) (((a) == JNI_TRUE) ? true : false)

#define JINT_SZ sizeof(jint)
#define JFLOAT_SZ sizeof(jfloat)

#endif // JavaEnv_h
