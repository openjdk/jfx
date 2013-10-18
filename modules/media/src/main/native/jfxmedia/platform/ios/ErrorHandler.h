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

#ifndef _ERRORHANDLER_H_
#define _ERRORHANDLER_H_


#import "jni.h"

#include "com_sun_media_jfxmedia_logging_Logger.h"


#define LOGGER_OFF     com_sun_media_jfxmedia_logging_Logger_OFF
#define LOGGER_ERROR   com_sun_media_jfxmedia_logging_Logger_ERROR
#define LOGGER_WARNING com_sun_media_jfxmedia_logging_Logger_WARNING
#define LOGGER_INFO    com_sun_media_jfxmedia_logging_Logger_INFO
#define LOGGER_DEBUG   com_sun_media_jfxmedia_logging_Logger_DEBUG


@interface ErrorHandler : NSObject


+ (void) initHandler;

+ (void) logError: (NSError *) error;

+ (jint) mapAVErrorToFXError: (NSError *) error;

+ (void) logMsg: (int) level message: (const char *) msg;

+ (void) logMsg: (int) level
    sourceClass: (const char *)
sourceClass method: (const char *) sourceMethod
        message: (const char *) msg;

+ (int) getLevel;

+ (void) setLevel: (int) newLevel;


@end


#endif
