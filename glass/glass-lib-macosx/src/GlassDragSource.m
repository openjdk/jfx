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

#import "common.h"
#import "com_sun_glass_ui_Clipboard.h"

#import "GlassMacros.h"
#import "GlassDragSource.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

static NSObject<GlassDragSourceDelegate> *gDelegate = nil;
static jint gMask = com_sun_glass_ui_Clipboard_ACTION_NONE;

@implementation GlassDragSource

+ (void)setDelegate:(NSObject<GlassDragSourceDelegate>*)delegate
{
    LOG("GlassDragSource:setDelegate");
    
    gDelegate = delegate; // notice, there is no retain
}

+ (void)flushWithMask:(jint)mask
{
    LOG("GlassDragSource:flushWithMask: %d", mask);
    
    if ([NSThread isMainThread] == YES)
    {
        NSDragOperation operation = [GlassDragSource mapJavaMaskToNsOperation:mask];        
        if (operation != NSDragOperationNone)
        {
            LOG("[gDelegate startDrag:operation]");
                [gDelegate startDrag:operation];
        }
        else
        {
            LOG("[gDelegate startDrag:operation] NOT TAKEN!");
        }
    }
    else
    {
        NSLog(@"GlassDragSource flush called on nonmain thread!");
    }
}

+ (NSDragOperation)mapJavaMaskToNsOperation:(jint)mask
{
    LOG("GlassDragSource:mapJavaMaskToNsOperation: %d", mask);
    
    NSDragOperation operation = NSDragOperationNone;
    if (mask == com_sun_glass_ui_Clipboard_ACTION_NONE)
    {
        LOG("   com_sun_glass_ui_Clipboard_ACTION_NONE");
        operation = NSDragOperationNone;
    }
    else if (mask == com_sun_glass_ui_Clipboard_ACTION_COPY)
    {
        LOG("   com_sun_glass_ui_Clipboard_ACTION_COPY");
        operation = NSDragOperationCopy;
    }
    else if (mask == com_sun_glass_ui_Clipboard_ACTION_MOVE)
    {
        LOG("   com_sun_glass_ui_Clipboard_ACTION_MOVE");
        operation = NSDragOperationMove;
    }
    else if (mask == com_sun_glass_ui_Clipboard_ACTION_REFERENCE)
    {
        LOG("   com_sun_glass_ui_Clipboard_ACTION_REFERENCE");
        operation = NSDragOperationLink;
    }
    else if (mask == com_sun_glass_ui_Clipboard_ACTION_COPY_OR_MOVE)
    {
        LOG("   com_sun_glass_ui_Clipboard_ACTION_COPY_OR_MOVE");
        operation = NSDragOperationCopy|NSDragOperationMove;
    }
    else if (mask == com_sun_glass_ui_Clipboard_ACTION_ANY)
    {
        LOG("   com_sun_glass_ui_Clipboard_ACTION_ANY");
        operation = NSDragOperationEvery;
    }
    else
    {
        if ((com_sun_glass_ui_Clipboard_ACTION_COPY&mask)==com_sun_glass_ui_Clipboard_ACTION_COPY)
        {
            LOG("   masked com_sun_glass_ui_Clipboard_ACTION_COPY");
            operation |= NSDragOperationCopy;
        }
        
        if ((com_sun_glass_ui_Clipboard_ACTION_MOVE&mask)==com_sun_glass_ui_Clipboard_ACTION_MOVE)
        {
            LOG("   masked com_sun_glass_ui_Clipboard_ACTION_MOVE");
            operation |= NSDragOperationMove;
        }
        
        if ((com_sun_glass_ui_Clipboard_ACTION_COPY_OR_MOVE&mask)==com_sun_glass_ui_Clipboard_ACTION_COPY_OR_MOVE)
        {
            LOG("   masked com_sun_glass_ui_Clipboard_ACTION_COPY_OR_MOVE");
            operation |= NSDragOperationCopy|NSDragOperationMove;
        }
        
        if ((com_sun_glass_ui_Clipboard_ACTION_REFERENCE&mask)==com_sun_glass_ui_Clipboard_ACTION_REFERENCE)
        {
            LOG("   masked com_sun_glass_ui_Clipboard_ACTION_REFERENCE");
            operation |= NSDragOperationLink;
        }
        
        if ((com_sun_glass_ui_Clipboard_ACTION_ANY&mask)==com_sun_glass_ui_Clipboard_ACTION_ANY)
        {
            LOG("   masked com_sun_glass_ui_Clipboard_ACTION_ANY");
            operation |= NSDragOperationEvery;
        }
    }
    return operation;
}

// Return a recommendedAction (one), but the input is a mask
+ (jint)mapNsOperationToJavaMask:(NSDragOperation)operation
{
    LOG("GlassDragSource:mapNsOperationToJavaMask: %d", operation);
    
    switch (operation) {
        case NSDragOperationNone: 
            return com_sun_glass_ui_Clipboard_ACTION_NONE;
        case NSDragOperationCopy: 
        case NSDragOperationGeneric:
        case NSDragOperationEvery:
            return com_sun_glass_ui_Clipboard_ACTION_COPY;
        case NSDragOperationMove:
            return com_sun_glass_ui_Clipboard_ACTION_MOVE;
        case NSDragOperationLink:
            return com_sun_glass_ui_Clipboard_ACTION_REFERENCE;
    }

    if (operation & (NSDragOperationCopy | NSDragOperationGeneric)) {
        return com_sun_glass_ui_Clipboard_ACTION_COPY;
    }
    if (operation & NSDragOperationMove) {
        return com_sun_glass_ui_Clipboard_ACTION_MOVE;
    }
    if (operation & NSDragOperationLink) {
        return com_sun_glass_ui_Clipboard_ACTION_REFERENCE;
    }

    GLASS_LOG("WARNING: unhandled case in mapNsOperationToJavaMask: %d", operation);
    return com_sun_glass_ui_Clipboard_ACTION_NONE;
}

+ (void)setMask:(jint)mask
{
    LOG("GlassDragSource:mask");
    
    gMask = mask;
}

+ (jint)getMask
{
    LOG("GlassDragSource:getMask");
    
    return gMask;
}

@end
