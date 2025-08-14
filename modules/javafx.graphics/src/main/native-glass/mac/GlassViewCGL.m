/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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
#import "com_sun_glass_ui_View_Capability.h"
#import "GlassMacros.h"
#import "GlassViewCGL.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

// http://developer.apple.com/library/mac/#technotes/tn2085/_index.html
// #define ENABLE_MULTITHREADED_GL

@implementation GlassViewCGL

- (CGLPixelFormatObj)_createPixelFormatWithDepth:(CGLPixelFormatAttribute)depth
{
    CGLPixelFormatObj pix = NULL;
    {
        const CGLPixelFormatAttribute attributes[] =
        {
            kCGLPFAAccelerated,
            kCGLPFAColorSize, 32,
            kCGLPFAAlphaSize, 8,
            kCGLPFADepthSize, depth,
            kCGLPFAAllowOfflineRenderers, // lets OpenGL know this context is offline renderer aware
            (CGLPixelFormatAttribute)0
        };
        GLint npix = 0;
        CGLError err = CGLChoosePixelFormat(attributes, &pix, &npix);
        if (pix == NULL)
        {
            NSLog(@"CGLChoosePixelFormat: No matching pixel format exists for the requested attributes, trying again with limited capabilities");
            const CGLPixelFormatAttribute attributes2[] =
            {
                kCGLPFAAllowOfflineRenderers,
                (CGLPixelFormatAttribute)0
            };
            err = CGLChoosePixelFormat(attributes2, &pix, &npix);
        }
        if (err != kCGLNoError)
        {
            NSLog(@"CGLChoosePixelFormat error: %d", err);
        }
    }
    return pix;
}

- (CGLContextObj)_createContextWithShared:(CGLContextObj)share
                               withFormat:(CGLPixelFormatObj)format
{
    CGLContextObj ctx = NULL;
    {
        CGLError err = CGLCreateContext(format, share, &ctx);
        if (err != kCGLNoError)
        {
            NSLog(@"CGLCreateContext error: %d", err);
        }
    }
    return ctx;
}

- (void)_initialize3dWithJproperties:(jobject)jproperties
                       useMTLForBlit:(BOOL)useMTLInGlass
{
    GET_MAIN_JENV;

    int depthBits = 0;
    if (jproperties != NULL)
    {
        jobject k3dDepthKey = (*env)->NewObject(env, jIntegerClass, jIntegerInitMethod, com_sun_glass_ui_View_Capability_k3dDepthKeyValue);
        GLASS_CHECK_EXCEPTION(env);
        jobject k3dDepthKeyValue = (*env)->CallObjectMethod(env, jproperties, jMapGetMethod, k3dDepthKey);
        GLASS_CHECK_EXCEPTION(env);
        if (k3dDepthKeyValue != NULL)
        {
            depthBits = (*env)->CallIntMethod(env, k3dDepthKeyValue, jIntegerValueMethod);
            GLASS_CHECK_EXCEPTION(env);
        }
    }

    CGLContextObj sharedCGL = NULL;
    if (jproperties != NULL)
    {
        jobject sharedContextPtrKey = (*env)->NewStringUTF(env, "shareContextPtr");
        jobject sharedContextPtrValue = (*env)->CallObjectMethod(env, jproperties, jMapGetMethod, sharedContextPtrKey);
        GLASS_CHECK_EXCEPTION(env);
        if (sharedContextPtrValue != NULL)
        {
            jlong jsharedContextPtr = (*env)->CallLongMethod(env, sharedContextPtrValue, jLongValueMethod);
            GLASS_CHECK_EXCEPTION(env);
            if (jsharedContextPtr != 0)
            {
                NSOpenGLContext *sharedContextNS = (NSOpenGLContext*)jlong_to_ptr(jsharedContextPtr);
                sharedCGL = [sharedContextNS CGLContextObj];
            }
        }
    }

    CGLContextObj clientCGL = NULL;
    BOOL isSwPipe = NO;

    if (jproperties != NULL)
    {
        jobject contextPtrKey = (*env)->NewStringUTF(env, "contextPtr");
        jobject contextPtrValue = (*env)->CallObjectMethod(env, jproperties, jMapGetMethod, contextPtrKey);
        GLASS_CHECK_EXCEPTION(env);
        if (contextPtrValue != NULL)
        {
            jlong jcontextPtr = (*env)->CallLongMethod(env, contextPtrValue, jLongValueMethod);
            GLASS_CHECK_EXCEPTION(env);
            if (jcontextPtr != 0)
            {
                NSOpenGLContext *clientContextNS = (NSOpenGLContext*)jlong_to_ptr(jcontextPtr);
                clientCGL = [clientContextNS CGLContextObj];
            }
        }
    }
    if (clientCGL == NULL)
    {
        CGLPixelFormatObj clientPixelFormat = [self _createPixelFormatWithDepth:(CGLPixelFormatAttribute)depthBits];
        clientCGL = [self _createContextWithShared:sharedCGL
                                        withFormat:clientPixelFormat];
    }
    if (sharedCGL == NULL)
    {
        // this can happen in Rain or clients other than Prism (ie. device details do not have the shared context set)
        sharedCGL = clientCGL;
        isSwPipe = YES;
    }

    BOOL isHiDPIAware = NO;
    if (jproperties != NULL)
    {
        jobject kHiDPIAwareKey = (*env)->NewObject(env, jIntegerClass, jIntegerInitMethod, com_sun_glass_ui_View_Capability_kHiDPIAwareKeyValue);
        GLASS_CHECK_EXCEPTION(env);
        jobject kHiDPIAwareValue = (*env)->CallObjectMethod(env, jproperties, jMapGetMethod, kHiDPIAwareKey);
        GLASS_CHECK_EXCEPTION(env);
        if (kHiDPIAwareValue != NULL)
        {
            isHiDPIAware = (*env)->CallBooleanMethod(env, kHiDPIAwareValue, jBooleanValueMethod) ? YES : NO;
            GLASS_CHECK_EXCEPTION(env);
        }
    }

    self->layer = [[GlassLayer alloc] initGlassLayer:(NSObject*)sharedCGL
                                    andClientContext:(NSObject*)clientCGL
                                         mtlQueuePtr:0l
                                      withHiDPIAware:isHiDPIAware
                                        withIsSwPipe:isSwPipe
                                       useMTLForBlit:useMTLInGlass];
    // https://developer.apple.com/library/mac/documentation/Cocoa/Reference/ApplicationKit/Classes/nsview_Class/Reference/NSView.html#//apple_ref/occ/instm/NSView/setWantsLayer:
    // the order of the following 2 calls is important: here we indicate we want a layer-hosting view
    [self setLayer:self->layer];
    [self setWantsLayer:YES];
}

- (id)initWithFrame:(NSRect)frame
          withJview:(jobject)jView
    withJproperties:(jobject)jproperties
      useMTLForBlit:(BOOL)useMTLInGlass;
{
    LOG("GlassViewCGL initWithFrame:withJview:withJproperties");

    NSOpenGLPixelFormatAttribute pixelFormatAttributes[] =
    {
        NSOpenGLPFAAllowOfflineRenderers, // Lets OpenGL know this context is offline renderer aware
        (NSOpenGLPixelFormatAttribute)0
    };
    NSOpenGLPixelFormat *pFormat = [[[NSOpenGLPixelFormat alloc] initWithAttributes:pixelFormatAttributes] autorelease];
    if (!pFormat)
    {
        pFormat = [NSOpenGLView defaultPixelFormat];
        LOG("GlassViewCGL initWithFrame: initWithAttributes failed! Set pixel format to default pixel format");
    }
    self = [super initWithFrame:frame pixelFormat:pFormat];
    if (self != nil)
    {
        [self _initialize3dWithJproperties:jproperties
            useMTLForBlit:(BOOL)useMTLInGlass];
    }
    return self;
}

- (void)dealloc
{
    [self->layer release];
    [super dealloc];
}

// also called when closing window, when [self window] == nil
- (void)viewDidMoveToWindow
{
    if ([self window] != nil)
    {
        [[self->layer getPainterOffscreen] setBackgroundColor:[[[self window] backgroundColor] colorUsingColorSpace:NSColorSpace.sRGBColorSpace]];
    }
}

- (GlassLayer*)getLayer
{
    return self->layer;
}

- (BOOL)acceptsFirstMouse:(NSEvent *)theEvent
{
    return YES;
}

@end
