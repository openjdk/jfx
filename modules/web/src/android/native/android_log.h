/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

