/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

#import <Cocoa/Cocoa.h>
#import <OpenGL/gl.h>
#import <OpenGL/CGLTypes.h>

#import "../macosx-window-system.h"
#import "com_sun_prism_es2_GLPixelFormat_Attributes.h"

void *createPixelFormat(jint *ivalues) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSOpenGLPixelFormatAttribute attribs[20];
    int index = 0;

    if (ivalues == NULL) {
        return NULL;
    }
    attribs[index++] = NSOpenGLPFAAccelerated;

    if (ivalues[com_sun_prism_es2_GLPixelFormat_Attributes_DOUBLEBUFFER] != 0) {
        attribs[index++] = NSOpenGLPFADoubleBuffer;
    }

    attribs[index++] = NSOpenGLPFAAlphaSize;
    attribs[index++] = ivalues[com_sun_prism_es2_GLPixelFormat_Attributes_ALPHA_SIZE];

    attribs[index++] = NSOpenGLPFAColorSize;
    attribs[index++] =
            ivalues[com_sun_prism_es2_GLPixelFormat_Attributes_RED_SIZE]
            + ivalues[com_sun_prism_es2_GLPixelFormat_Attributes_GREEN_SIZE]
            + ivalues[com_sun_prism_es2_GLPixelFormat_Attributes_BLUE_SIZE]
            + ivalues[com_sun_prism_es2_GLPixelFormat_Attributes_ALPHA_SIZE];

    attribs[index++] = NSOpenGLPFADepthSize;
    attribs[index++] = ivalues[com_sun_prism_es2_GLPixelFormat_Attributes_DEPTH_SIZE];


    // Lets OpenGL know this context is offline renderer aware.
    attribs[index++] = NSOpenGLPFAAllowOfflineRenderers;

    // Zero-terminate
    attribs[index++] = 0;

    NSOpenGLPixelFormat *fmt = [[NSOpenGLPixelFormat alloc] initWithAttributes : attribs];

    [pool release];
    return fmt;
}

void deletePixelFormat(void *pixelFormat) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSOpenGLPixelFormat *fmt = (NSOpenGLPixelFormat *) pixelFormat;
    [fmt release];
    [pool release];
}

void *createContext(void *shareContext, void *view, void *pixelFormat,
        int *viewNotReady) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSView *nsView = NULL;
    NSObject *nsObj = (NSObject *) view;

    if (nsObj != NULL && [nsObj isKindOfClass : [NSView class]]) {
        nsView = (NSView *) nsObj;
    }

    if (nsView != NULL) {
        jboolean viewReady = true;

        if ([nsView lockFocusIfCanDraw] == NO) {
            viewReady = false;
        } else {
            NSRect frame = [nsView frame];
            if ((frame.size.width == 0) || (frame.size.height == 0)) {
                [nsView unlockFocus];
                viewReady = false;
            }
        }

        if (!viewReady) {
            if (viewNotReady != NULL) {
                *viewNotReady = 1;
            }

            // the view is not ready yet
            [pool release];
            return NULL;
        }
    }

    NSOpenGLContext *nsContext = [[NSOpenGLContext alloc]
            initWithFormat : (NSOpenGLPixelFormat *) pixelFormat
            shareContext : (NSOpenGLContext*) shareContext];

    if (nsContext != nil) {
        if (nsView != nil) {
            [nsContext setView : nsView];
            [nsView unlockFocus];
        }
    }

    [pool release];
    return nsContext;
}

void *getCurrentContext() {
    NSOpenGLContext *nsContext = NULL;

    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    nsContext = [NSOpenGLContext currentContext];
    [pool release];
    return nsContext;
}

jboolean makeCurrentContext(void *nsJContext) {
    NSOpenGLContext *nsContext = (NSOpenGLContext *) nsJContext;

    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    [nsContext makeCurrentContext];
    [pool release];
    return JNI_TRUE;
}

jboolean clearCurrentContext(void *nsJContext) {
    NSOpenGLContext *nsContext = (NSOpenGLContext *) nsJContext;

    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSOpenGLContext *currentNSContext = [NSOpenGLContext currentContext];
    if (currentNSContext != nsContext) {
        [nsContext makeCurrentContext];
    }
    [NSOpenGLContext clearCurrentContext];
    [pool release];
    return JNI_TRUE;
}

jboolean deleteContext(void *nsJContext) {
    NSOpenGLContext *nsContext = (NSOpenGLContext *) nsJContext;

    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [nsContext clearDrawable];
    [nsContext release];
    [pool release];
    return JNI_TRUE;
}

jboolean flushBuffer(void *nsJContext) {
    NSOpenGLContext *nsContext = (NSOpenGLContext *) nsJContext;

    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [nsContext flushBuffer];
    [pool release];
    return JNI_TRUE;
}

void setSwapInterval(void *nsJContext, int swapInterval) {
    NSOpenGLContext *nsContext = (NSOpenGLContext *) nsJContext;
    [nsContext setValues : &swapInterval forParameter : NSOpenGLCPSwapInterval];
}
