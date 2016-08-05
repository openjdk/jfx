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

#ifndef _LOGGER_H_
#define _LOGGER_H_

#include <Common/ProductFlags.h>

#if ENABLE_LOGGING
#include <com_sun_media_jfxmedia_logging_Logger.h>

#include <Utils/Singleton.h>

#include <string>

using namespace std;

// Level difinitions
#define LOGGER_OFF     com_sun_media_jfxmedia_logging_Logger_OFF
#define LOGGER_ERROR   com_sun_media_jfxmedia_logging_Logger_ERROR
#define LOGGER_WARNING com_sun_media_jfxmedia_logging_Logger_WARNING
#define LOGGER_INFO    com_sun_media_jfxmedia_logging_Logger_INFO
#define LOGGER_DEBUG   com_sun_media_jfxmedia_logging_Logger_DEBUG

// Macros for logging
// These macros should be used instead of calling CLogger directly
#define LOGGER_LOGMSG(l, m) { CLogger *pLogger = CLogger::getLogger(); if (pLogger) pLogger->logMsg(l, m); }
#define LOGGER_LOGMSG_CM(l, sc, sm, m) { CLogger *pLogger = CLogger::getLogger(); if (pLogger) pLogger->logMsg(l, sc, sm, m); }

#define LOGGER_ERRORMSG(m) {CLogger *pLogger = CLogger::getLogger(); if (pLogger) pLogger->logErrorMsg(m);}
#define LOGGER_WARNMSG(m) {CLogger *pLogger = CLogger::getLogger(); if (pLogger) pLogger->logWarningMsg(m);}
#define LOGGER_INFOMSG(m) {CLogger *pLogger = CLogger::getLogger(); if (pLogger) pLogger->logInfoMsg(m);}
#define LOGGER_DEBUGMSG(m) {CLogger *pLogger = CLogger::getLogger(); if (pLogger) pLogger->logDebugMsg(m);}

#define LOGGER_ERRORMSG_CM(sc,sm,m) {CLogger *pLogger = CLogger::getLogger(); if (pLogger) pLogger->logErrorMsg(sc,sm,m);}
#define LOGGER_WARNMSG_CM(sc,sm,m) {CLogger *pLogger = CLogger::getLogger(); if (pLogger) pLogger->logWarningMsg(sc,sm,m);}
#define LOGGER_INFOMSG_CM(sc,sm,m) {CLogger *pLogger = CLogger::getLogger(); if (pLogger) pLogger->logInfoMsg(sc,sm,m);}
#define LOGGER_DEBUGMSG_CM(sc,sm,m) {CLogger *pLogger = CLogger::getLogger(); if (pLogger) pLogger->logDebugMsg(sc,sm,m);}

class CLogger
{
public:
    bool canLog(int level);

    void logMsg(int level, const char *msg);
    void logMsg(int level, const char *sourceClass, const char *sourceMethod, const char *msg);

    inline void logErrorMsg(const char *msg) {
        logMsg(LOGGER_ERROR, msg);
    }

    inline void logInfoMsg(const char *msg) {
        logMsg(LOGGER_INFO, msg);
    }

    inline void logWarningMsg(const char *msg) {
        logMsg(LOGGER_DEBUG, msg);
    }

    inline void logDebugMsg(const char *msg) {
        logMsg(LOGGER_DEBUG, msg);
    }

    inline void logErrorMsg(const char *srcClass, const char *srcMethod, const char *msg) {
        logMsg(LOGGER_ERROR, srcClass, srcMethod, msg);
    }

    inline void logInfoMsg(const char *srcClass, const char *srcMethod, const char *msg) {
        logMsg(LOGGER_INFO, srcClass, srcMethod, msg);
    }

    inline void logWarningMsg(const char *srcClass, const char *srcMethod, const char *msg) {
        logMsg(LOGGER_DEBUG, srcClass, srcMethod, msg);
    }

    inline void logDebugMsg(const char *srcClass, const char *srcMethod, const char *msg) {
        logMsg(LOGGER_DEBUG, srcClass, srcMethod, msg);
    }

public:
    // Do NOT use this function. Instead use init() from Java layer.
    bool init(JNIEnv *env, jclass cls);
    // Do NOT use this function. Instead use setLevel() from Java layer.
    void setLevel(int level);

    static inline CLogger *getLogger() {
        CLogger *logger = NULL;
        s_Singleton.GetInstance(&logger);
        return logger;
    }

public:
    typedef Singleton<CLogger> LSingleton;
    friend class Singleton<CLogger>;
    static LSingleton s_Singleton;
    static uint32_t CreateInstance(CLogger **ppLogger);

private:
    bool m_areJMethodIDsInitialized;
    int m_currentLevel;
    JavaVM *m_jvm;
    jclass m_cls;
    jmethodID m_logMsg1Method;
    jmethodID m_logMsg2Method;
};
#else // ENABLE_LOGGING
#define LOGGER_LOGMSG(l, m) NULL
#define LOGGER_LOGMSG_CM(l, sc, sm, m) NULL
#endif // ENABLE_LOGGING

#endif // _LOGGER_H_
