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

#import "JavaUtils.h"

NSString *NSStringFromJavaString(JNIEnv *env, jstring js)
{
    NSString *outString = nil;
    
    if (NULL != env && 0 != js) {
        jboolean isCopy = JNI_FALSE;
        const jchar *jsChars = (*env)->GetStringChars(env, js, &isCopy);
        
        outString = [NSString stringWithCharacters:(const unichar *)jsChars
                                            length:(*env)->GetStringLength(env, js)];
        (*env)->ReleaseStringChars(env, js, jsChars);
    }
    
    return outString;
}

jstring JavaStringFromNSString(JNIEnv *env, NSString *ns)
{
    jstring outString = 0;
    
    if (NULL != env && nil != ns) {
        NSInteger length = [ns length];
        unichar *strBuf = malloc(length * sizeof(unichar));
        if (!strBuf) {
            return 0;
        }
        
        @try {
            [ns getCharacters:strBuf range:NSMakeRange(0, length)];
        }
        @catch (NSException *exception) {
            free(strBuf);
            return 0;
        }
        
        outString = (*env)->NewString(env, strBuf, length);
        free(strBuf);
    }
    
    return outString;
}

JNIEnv *GetJavaEnvironment(JavaVM *jvm, BOOL *attached)
{
    JNIEnv *env = NULL;
    if ((*jvm)->GetEnv(jvm, (void*)&env, JNI_VERSION_1_6) == JNI_OK) {
        *attached = NO;
    } else {
        if ((*jvm)->AttachCurrentThreadAsDaemon(jvm, (void*)&env, NULL) != JNI_OK) {
            return NULL;
        }
        *attached = YES;
    }
    return env;
}
