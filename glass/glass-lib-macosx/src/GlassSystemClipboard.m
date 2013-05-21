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

#import "common.h"
#import "GlassHelper.h"
#import "com_sun_glass_ui_mac_MacSystemClipboard_FormatEncoder.h"
#import "GlassMacros.h"

/*
 * Class:     com_sun_glass_ui_mac_MacSystemClipboard_FormatEncoder
 * Method:    _convertMIMEtoUTI
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_mac_MacSystemClipboard_00024FormatEncoder__1convertMIMEtoUTI
(JNIEnv *env, jclass clazz, jstring mime)
{
    jstring result = nil;
    
    GLASS_POOL_ENTER;
    {
        CFStringRef cfMIME = (CFStringRef)[GlassHelper nsStringWithJavaString:mime withEnv:env];
        NSString* nsUTI = (NSString *)UTTypeCreatePreferredIdentifierForTag(kUTTagClassMIMEType,
                                                                            cfMIME,
                                                                            (CFStringRef)@"public.mime-type");
        [nsUTI autorelease];    
        result = (*env) -> NewStringUTF(env, [nsUTI UTF8String]);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return result;
}

/*
 * Class:     com_sun_glass_ui_mac_MacSystemClipboard_FormatEncoder
 * Method:    _convertUTItoMIME
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_mac_MacSystemClipboard_00024FormatEncoder__1convertUTItoMIME
(JNIEnv *env, jclass clazz, jstring uti)
{
    jstring result = nil;

    GLASS_POOL_ENTER;
    {
        CFStringRef cfUTI = (CFStringRef)[GlassHelper nsStringWithJavaString:uti withEnv:env];
        NSString* nsMIME = [(NSString*)UTTypeCopyPreferredTagWithClass(cfUTI,
                                                                       kUTTagClassMIMEType) autorelease];
        result = (*env) -> NewStringUTF(env, [nsMIME UTF8String]);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return result;
}

/*
 * Class:     com_sun_glass_ui_mac_MacSystemClipboard
 * Method:    _convertFileReferencePath
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_mac_MacSystemClipboard__1convertFileReferencePath
(JNIEnv *env, jclass clazz, jstring jPath)
{
    jstring result = nil;

    GLASS_POOL_ENTER;
    {
        NSString* path = [GlassHelper nsStringWithJavaString:jPath withEnv:env];
        NSURL *url = [NSURL URLWithString:path];
        if (url != nil) {
            url = [url filePathURL];
            if (url != nil) {
                path = [url absoluteString];
            }
        }
        result = (*env) -> NewStringUTF(env, [path UTF8String]);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return result;
}
