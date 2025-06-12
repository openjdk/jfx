/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
#import "com_sun_glass_events_DndEvent.h"
#import "com_sun_glass_events_KeyEvent.h"
#import "com_sun_glass_events_MouseEvent.h"
#import "com_sun_glass_ui_View_Capability.h"
#import "com_sun_glass_ui_mac_MacGestureSupport.h"
#import "GlassKey.h"
#import "GlassMacros.h"
#import "GlassViewMTL3D.h"
#import "GlassApplication.h"
#import "GlassScreen.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

//#define MOUSEVERBOSE
#ifndef MOUSEVERBOSE
    #define MOUSELOG(MSG, ...)
#else
    #define MOUSELOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

//#define KEYVERBOSE
#ifndef KEYVERBOSE
    #define KEYLOG(MSG, ...)
#else
    #define KEYLOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

//#define DNDVERBOSE
#ifndef DNDVERBOSE
    #define DNDLOG(MSG, ...)
#else
    #define DNDLOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

//#define IMVERBOSE
#ifndef IMVERBOSE
    #define IMLOG(MSG, ...)
#else
    #define IMLOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

#define SHARE_GL_CONTEXT
//#define DEBUG_COLORS

@implementation GlassViewMTL3D

- (void)_initialize3dWithJproperties:(jobject)jproperties
{
    GET_MAIN_JENV;
    long mtlCommandQueuePtr = 0l;

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

    BOOL isSwPipe = NO;

    if (jproperties != NULL)
    {
        jobject mtlCommandQueueKey = (*env)->NewStringUTF(env, "mtlCommandQueue");
        jobject mtlCommandQueueValue = (*env)->CallObjectMethod(env, jproperties, jMapGetMethod, mtlCommandQueueKey);
        //NSLog(@"---- mtlCommandQueueKey = %p", mtlCommandQueueKey);
        //NSLog(@"---- mtlCommandQueueValue = %p", mtlCommandQueueValue);
        GLASS_CHECK_EXCEPTION(env);
        if (mtlCommandQueueValue != NULL)
        {
            jlong jmtlQueuePtr = (*env)->CallLongMethod(env, mtlCommandQueueValue, jLongValueMethod);
            GLASS_CHECK_EXCEPTION(env);
            if (jmtlQueuePtr != 0)
            {
                //NSLog(@"--- GLASS metal command queue ptr = %ld", jmtlQueuePtr);

                //TODO: MTL: This enables sharing of MTLCommandQueue between PRISM and GLASS, if needed.
                //Note : Currently, PRISM and GLASS create their own dedicated MTLCommandQueue
                mtlCommandQueuePtr = jmtlQueuePtr;
            }
        }
    }

    if (mtlCommandQueuePtr == 0l) {
        LOG("GlassViewMTL3D _initialize3dWithJproperties : using software pipeline");
        isSwPipe = YES;
    }

    self->layer = [[GlassLayer3D alloc] initGlassLayer:nil
        andClientContext:nil mtlQueuePtr:mtlCommandQueuePtr
        withHiDPIAware:YES withIsSwPipe:isSwPipe];

    // https://developer.apple.com/library/mac/documentation/Cocoa/Reference/ApplicationKit/Classes/nsview_Class/Reference/NSView.html#//apple_ref/occ/instm/NSView/setWantsLayer:
    // the order of the following 2 calls is important: here we indicate we want a layer-hosting view
    {
        [self setLayerContentsRedrawPolicy: NSViewLayerContentsRedrawOnSetNeedsDisplay];
        [self setLayer:self->layer];
        [self setWantsLayer:YES];
        //[self setWantsUpdateLayer:YES];
    }
}

- (BOOL) wantsUpdateLayer {
    return TRUE;
}

- (id)initWithFrame:(NSRect)frame withJview:(jobject)jView withJproperties:(jobject)jproperties
{
    LOG("GlassViewMTL3D initWithFrame:withJview:withJproperties");

    self = [super initWithFrame: frame];
    if (self != nil)
    {
        [self _initialize3dWithJproperties:jproperties];
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
    //[self->_delegate viewDidMoveToWindow];
}

- (GlassLayer3D*)getLayer
{
    return self->layer;
}

- (BOOL)acceptsFirstMouse:(NSEvent *)theEvent
{
    return YES;
}

@end
