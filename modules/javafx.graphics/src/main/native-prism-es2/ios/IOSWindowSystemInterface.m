/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#import <UIKit/UIKit.h>
#import <dlfcn.h>
#import "ios-window-system.h"

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 80000 // iOS 8.0 supported
#include <OpenGLES/ES2/gl.h>
#include <OpenGLES/ES2/glext.h>
#endif

void *createPixelFormat(jint* ivalues) {
    return NULL;
}

void deletePixelFormat(void* pixelFormat) {
}

void *createContext(void *shareContext, void *view,
                    void *pixelFormat, int *viewNotReady) {
    fprintf(stderr, "IOSWindowSystemInterface : share %x view %x pf % notready %\n",
            shareContext, view, pixelFormat, viewNotReady);

    EAGLContext *ctx = NULL;
    if (shareContext == NULL) {
        ctx = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];

    } else {
        ctx = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2 sharegroup:[(EAGLContext*)shareContext sharegroup]];
    }

    return ctx;
}


void *getCurrentContext() {
    EAGLContext *ctx = [EAGLContext currentContext];
    // fprintf(stderr, "IOSWindowSystemInterface : getCurrentContext %x\n", ctx);

    return ptr_to_jlong(ctx);
}

jboolean makeCurrentContext(void *context) {
    // fprintf(stderr, "IOSWindowSystemInterface : makeCurrentContext %x\n", context);
    if ([EAGLContext setCurrentContext:jlong_to_ptr(context)] == YES) {
        return JNI_TRUE;
    }

    return JNI_FALSE;
}

jboolean clearCurrentContext(void *context) {
    // fprintf(stderr, "IOSWindowSystemInterface : clearCurrentContext %x\n", context);
    if ([EAGLContext setCurrentContext:nil] == YES) {
        return JNI_TRUE;
    }

    return JNI_FALSE;
}

jboolean deleteContext(void *context) {
    fprintf(stderr, "IOSWindowSystemInterface : deleteContext unimp\n");
    return JNI_FALSE;
}

jboolean flushBuffer(void *context) {
    [[EAGLContext currentContext] presentRenderbuffer:GL_RENDERBUFFER];
    return JNI_FALSE;
}

void setSwapInterval(void *context, int interval) {
    if (pulseLoggingRequested) {
        fprintf(stderr, "IOSWindowSystemInterface : setSwapInterval unimp\n");
    }
}


#import <mach-o/dyld.h>
#import <stdlib.h>
#import <string.h>

static void *glesLibrary = NULL;

void *getProcAddress(const char *name) {

    if (glesLibrary == NULL) {
        glesLibrary = dlopen("/System/Library/Frameworks/OpenGLES.framework/OpenGLES", RTLD_LAZY | RTLD_GLOBAL);
    }
    void *address = dlsym(glesLibrary, name);

    // fprintf(stderr, "getProcAddress(%s) = %x\n", name, address);

    return address;
}

