/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
#import "com_sun_prism_es2_GLPixelFormat.h"
#ifdef GRADLE_BUILD
// This is a temporary build hack. When we're off ant and using Gradle, we can
// get rid of this ifdef
#import "com_sun_prism_es2_GLPixelFormat_Attributes.h"
#endif

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

    // Zero-terminate
    attribs[index++] = 0;

    NSOpenGLPixelFormat *fmt = [[NSOpenGLPixelFormat alloc] initWithAttributes : attribs];
    if (fmt == nil) {
        // should we fallback to defaults or not?
        fmt = [NSOpenGLView defaultPixelFormat];
    }

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


#import <mach-o/dyld.h>
#import <stdlib.h>
#import <string.h>

/*
 *
 * Here's what the code does:
 *
 *
 * 1) Allocates storage for the symbol name plus an underscore character ('_').
 * The underscore character is part of the UNIX C symbol-mangling convention,
 * so make sure that you provide storage for it.
 *
 * 2) Copies the symbol name into the string variable, starting at the second
 *  character, to leave room for prefixing the underscore character.
 *
 * 3) Copies the underscore character into the first character of the symbol
 *  name string.
 *
 * 4) Checks to make sure that the symbol name is defined, and if it is, looks
 *  up the symbol.
 *
 * 5) Frees the symbol name string because it is no longer needed.
 *
 * 6) Returns the appropriate pointer if successful, or NULL if not successful.
 *  Before using this pointer, you should make sure that is it valid.
 *
 */
void *getProcAddress(const char *name) {
    NSSymbol symbol;

    char *symbolName;

    symbolName = malloc(strlen(name) + 2); // 1
    strcpy(symbolName + 1, name); // 2
    symbolName[0] = '_'; // 3
    symbol = NULL;

    if (NSIsSymbolNameDefined(symbolName)) {// 4
        symbol = NSLookupAndBindSymbol(symbolName);
    }
    free(symbolName); // 5

    return symbol ? NSAddressOfSymbol(symbol) : NULL;
}
