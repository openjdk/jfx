/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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
static jint supportedActions = com_sun_glass_ui_Clipboard_ACTION_NONE;

@implementation GlassDragSource

+ (void)setDelegate:(NSObject<GlassDragSourceDelegate>*)delegate
{
    LOG("GlassDragSource:setDelegate");

    gDelegate = delegate; // notice, there is no retain
}

+ (BOOL)isDelegateSet
{
    return (gDelegate != nil);
}

+ (void)flushWithMask:(jint)mask withItems:(NSArray<NSDraggingItem*>*)items
{
    LOG("GlassDragSource:flushWithMask: %d", mask);

    if ([NSThread isMainThread] == YES)
    {
        supportedActions = mask;
        NSDragOperation operation = [GlassDragSource mapJavaMaskToNsOperation:mask];
        if (operation != NSDragOperationNone)
        {
            LOG("[gDelegate startDrag:operation withItems:items] gDelegate %p", gDelegate);
            [gDelegate startDrag:operation withItems:items];
        }
        else
        {
            LOG("[gDelegate startDrag:operation withItems:items] NOT TAKEN!");
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

    // The upper layers of Java code will turn TransferMode.ANY into a bitwise-OR of COPY,
    // MOVE, and REFERENCE. This condition will never be true which means the only
    // NSDragOperation flags we will set will be Copy, Move, and Link.
    if ((com_sun_glass_ui_Clipboard_ACTION_ANY&mask)==com_sun_glass_ui_Clipboard_ACTION_ANY)
    {
        LOG("   masked com_sun_glass_ui_Clipboard_ACTION_ANY");
        operation |= NSDragOperationEvery;
    }
    return operation;
}

// In this case the NSDragOperation was provided to us by an external
// application.
+ (jint)mapNsOperationToJavaMaskExternal:(NSDragOperation)operation
{
    LOG("GlassDragSource:mapNsOperationToJavaMaskExternal: %d", operation);

    jint mask = com_sun_glass_ui_Clipboard_ACTION_NONE;
    if ((operation&NSDragOperationEvery)==NSDragOperationEvery
       || (operation & NSDragOperationGeneric))
    {
        mask |= com_sun_glass_ui_Clipboard_ACTION_ANY;
    }
    if (operation & NSDragOperationLink)
    {
        mask |= com_sun_glass_ui_Clipboard_ACTION_REFERENCE;
    }
    if (  (operation & NSDragOperationCopy)
       && (operation & NSDragOperationMove))
    {
        mask |= com_sun_glass_ui_Clipboard_ACTION_COPY_OR_MOVE;
    }
    else if (operation & NSDragOperationCopy)
    {
        mask |= com_sun_glass_ui_Clipboard_ACTION_COPY;
    }
    else if (operation & NSDragOperationMove)
    {
        mask |= com_sun_glass_ui_Clipboard_ACTION_MOVE;
    }
    return mask;
}

// The NSDragOperation mas was generated by this JavaFX application so we
// interpret Generic to be a synonym for Move.
+ (jint)mapNsOperationToJavaMaskInternal:(NSDragOperation)operation
{
    LOG("GlassDragSource:mapNsOperationToJavaMaskInternal: %d", operation);

    jint mask = com_sun_glass_ui_Clipboard_ACTION_NONE;
    if (operation & NSDragOperationGeneric)
    {
        mask |= com_sun_glass_ui_Clipboard_ACTION_MOVE;
    }
    if (operation & NSDragOperationLink)
    {
        mask |= com_sun_glass_ui_Clipboard_ACTION_REFERENCE;
    }
    if (operation & NSDragOperationCopy)
    {
        mask |= com_sun_glass_ui_Clipboard_ACTION_COPY;
    }
    if (operation & NSDragOperationMove)
    {
        mask |= com_sun_glass_ui_Clipboard_ACTION_MOVE;
    }
    return mask;
}

// The NSDragOperation was provided to us by an external
// application.
// Return a recommendedAction (one), but the input is a mask
+ (jint)getRecommendedActionForMaskExternal:(NSDragOperation)operation
{
    LOG("GlassDragSource:getRecommendedActionForMaskExternal: %d", operation);

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
        default:
            break;
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

    GLASS_LOG("WARNING: unhandled case in getRecommendedActionForMaskExternal: %d", operation);
    return com_sun_glass_ui_Clipboard_ACTION_NONE;
}

// The NSDragOperation mas was generated by this JavaFX application so we
// interpret Generic to be a synonym for Move.
// To match historical behavior (as embodied in getRecommendedActionForMaskExternal)
// we prefer Copy over Move if both are available.
// Return a recommendedAction (one), but the input is a mask
+ (jint)getRecommendedActionForMaskInternal:(NSDragOperation)operation
{
    LOG("GlassDragSource:getRecommendedActionForMaskInternal: %d", operation);

    if (operation & NSDragOperationCopy) {
        return com_sun_glass_ui_Clipboard_ACTION_COPY;
    }
    if (operation & NSDragOperationGeneric) {
        return com_sun_glass_ui_Clipboard_ACTION_MOVE;
    }
    if (operation & NSDragOperationMove) {
        return com_sun_glass_ui_Clipboard_ACTION_MOVE;
    }
    if (operation & NSDragOperationLink) {
        return com_sun_glass_ui_Clipboard_ACTION_REFERENCE;
    }

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

+ (jint)getSupportedActions
{
    return supportedActions;
}

+ (void)setSupportedActions:(jint)actions
{
    supportedActions = actions;
}

@end
