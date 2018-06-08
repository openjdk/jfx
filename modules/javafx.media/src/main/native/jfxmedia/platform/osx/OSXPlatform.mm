/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

#import "com_sun_media_jfxmediaimpl_platform_osx_OSXPlatform.h"
#import "OSXMediaPlayer.h"

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXPlatform
 * Method:    osxPlatformInit
 * Signature: ()V
 */
JNIEXPORT jboolean JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXPlatform_osxPlatformInit
    (JNIEnv *env, jclass klass)
{
    // Workaround for JDK-8202393. All errors will be considered as warnings if code below fails.
    NSBundle *main = [NSBundle mainBundle];
    if (main != nil) {
        NSDictionary *dictionary = main.infoDictionary;
        if (dictionary != nil) {
            if ([dictionary isKindOfClass:[NSMutableDictionary class]]) {
                NSMutableDictionary *mDictionary = (NSMutableDictionary *)dictionary;
                NSDictionary *data = @{@"NSAllowsArbitraryLoads" : @YES, @"NSAllowsArbitraryLoadsForMedia" : @YES};
                mDictionary[@"NSAppTransportSecurity"] = data;
                LOGGER_INFOMSG("OSXPlatform: Info dictionary updated successfully.");
            } else {
                LOGGER_WARNMSG("OSXPlatform: Info dictionary is not mutable dictionary.");
            }
        } else {
            LOGGER_WARNMSG("OSXPlatform: Cannot get info dictionary.");
        }
    } else {
        LOGGER_WARNMSG("OSXPlatform: Cannot get main bundle.");
    }

    // Tell OSXMediaPlayer to initialize itself
    return (jboolean)[OSXMediaPlayer initPlayerPlatform];
}
