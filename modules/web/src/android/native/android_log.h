/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ANDROID_LOG_H
#define	ANDROID_LOG_H

#include <stdio.h>
#include <android/log.h>

#ifdef	__cplusplus
extern "C" {
#endif

#ifdef DEBUG
#define TAG "NATIVE_WEBVIEW"
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__))
#define LOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__))
#else
#define LOGI(...)
#define LOGE(...)
#define LOGV(...)
#endif

#define CHECK_EXCEPTION(ENV) \
    if ((*ENV)->ExceptionCheck(ENV) == JNI_TRUE) {                \
        LOGE("Detected outstanding Java exception at %s:%s:%d\n", \
                __FUNCTION__, __FILE__, __LINE__);                \
        (*ENV)->ExceptionDescribe(ENV);                           \
        (*ENV)->ExceptionClear(ENV);                              \
        return;                                                   \
    };
#define CLEAR_EXCEPTION(ENV) (*ENV)->ExceptionClear(ENV);

#define THROW_RUNTIME_EXCEPTION(ENV, MESSAGE, ...)                          \
    char error_msg[256];                                                    \
    sprintf(error_msg, MESSAGE, __VA_ARGS__);                               \
    (*ENV)->ThrowNew(ENV,                                                   \
        (*ENV)->FindClass(ENV, "java/lang/RuntimeException"), error_msg);

#ifdef	__cplusplus
}
#endif

#endif	/* ANDROID_LOG_H */

