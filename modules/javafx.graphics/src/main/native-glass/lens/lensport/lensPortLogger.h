/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef PLATFORM_LOGGER_H
#define PLATFORM_LOGGER_H

/**
 * Log a message at the given logging level.
 * Not used directly. GLASS_LOG should be used instead.
 */
extern void (*platform_logf)(int level,
                const char *func,
                const char *file,
                int line,
                const char *format, ...);

/**
 * The logging level.
 * Not used directly. GLASS_LOG and GLASS_IF_LOG should be used instead.
 */
extern jint platform_log_level;

/**
 * Begins a conditional statement that is only run if the current logging level
 * is less than or equal to "level".
 * For example, GLASS_IF_LOG(LOG_WARNING) { f(); } will call f() if and only if
 * the current logging settings include printing warning messages.
 * @param level The logging level to be tested against.
 */
#define GLASS_IF_LOG(level) if ((platform_logf) && (level >= platform_log_level))

/**
 * Logs a message at the given logging level
 * @param level the logging level (e.g. LOG_WARNING)
 * @param ... a format string and parameters in printf format
 */
/** Logging levels, with same meanings as in java.util.logging.Level */
#define GLASS_LOG_LEVEL_SEVERE  1000
#define GLASS_LOG_LEVEL_WARNING 900
#define GLASS_LOG_LEVEL_INFO    800
#define GLASS_LOG_LEVEL_CONFIG  700
#define GLASS_LOG_LEVEL_FINE    500
#define GLASS_LOG_LEVEL_FINER   400
#define GLASS_LOG_LEVEL_FINEST  300

#ifdef ANDROID_NDK
// Can't use java logger in jvm8 on Android. Remove when this issue is fixed.
#include <android/log.h>
#define TAG "GLASS"
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, __VA_ARGS__))
#define LOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, __VA_ARGS__))
#define GLASS_LOG(level,...) \
        LOGI(TAG, __VA_ARGS__)
#else
#define GLASS_LOG(level,...) \
    GLASS_IF_LOG(level) \
    (*platform_logf)(level, __func__, __FILE__, __LINE__, __VA_ARGS__)

#define GLASS_IF_LOG_SEVERE  GLASS_IF_LOG(GLASS_LOG_LEVEL_SEVERE)
#define GLASS_IF_LOG_WARNING GLASS_IF_LOG(GLASS_LOG_LEVEL_WARNING)
#define GLASS_IF_LOG_INFO    GLASS_IF_LOG(GLASS_LOG_LEVEL_INFO)
#define GLASS_IF_LOG_CONFIG  GLASS_IF_LOG(GLASS_LOG_LEVEL_CONFIG)
#define GLASS_IF_LOG_FINE    GLASS_IF_LOG(GLASS_LOG_LEVEL_FINE)
#define GLASS_IF_LOG_FINER   GLASS_IF_LOG(GLASS_LOG_LEVEL_FINER)
#define GLASS_IF_LOG_FINEST  GLASS_IF_LOG(GLASS_LOG_LEVEL_FINEST)
#endif

#ifdef NO_LOGGING
#define GLASS_LOG_SEVERE(...)  (void)0, ##__VA_ARGS__
#define GLASS_LOG_WARNING(...) (void)0, ##__VA_ARGS__
#define GLASS_LOG_INFO(...) (void)0, ##__VA_ARGS__
#define GLASS_LOG_CONFIG(...) (void)0, ##__VA_ARGS__
#define GLASS_LOG_FINE(...) (void)0, ##__VA_ARGS__
#define GLASS_LOG_FINER(...) (void)0, ##__VA_ARGS__
#define GLASS_LOG_FINEST(...) (void)0, ##__VA_ARGS__
#else
#define GLASS_LOG_SEVERE(...) GLASS_LOG(GLASS_LOG_LEVEL_SEVERE, __VA_ARGS__)
#define GLASS_LOG_WARNING(...) GLASS_LOG(GLASS_LOG_LEVEL_WARNING, __VA_ARGS__)
#define GLASS_LOG_INFO(...) GLASS_LOG(GLASS_LOG_LEVEL_INFO, __VA_ARGS__)
#define GLASS_LOG_CONFIG(...) GLASS_LOG(GLASS_LOG_LEVEL_CONFIG, __VA_ARGS__)
#define GLASS_LOG_FINE(...) GLASS_LOG(GLASS_LOG_LEVEL_FINE, __VA_ARGS__)
#define GLASS_LOG_FINER(...) GLASS_LOG(GLASS_LOG_LEVEL_FINER, __VA_ARGS__)
#define GLASS_LOG_FINEST(...) GLASS_LOG(GLASS_LOG_LEVEL_FINEST, __VA_ARGS__)
#endif

#endif // PLATFORM_LOGGER_H
