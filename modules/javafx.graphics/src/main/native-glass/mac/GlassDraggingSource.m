/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
#include <AppKit/AppKit.h>
#import "GlassDraggingSource.h"
#import "GlassMacros.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif


@implementation GlassDraggingSource

- (NSDragOperation)draggingSession:(NSDraggingSession *)session sourceOperationMaskForDraggingContext:(NSDraggingContext)context
{
    // The Command key masks out every operation other than NSDragOperationGeneric. We want
    // internal Move events to get through this filtering so we copy the Move bit into the
    // Generic bit and treat Generic as a synonym for Move.
    if (context == NSDraggingContextWithinApplication)
    {
        NSDragOperation result = self->dragOperation;
        if (result & NSDragOperationMove)
            result |= NSDragOperationGeneric;
        return result;
    }
    return self->dragOperation;
}

- (GlassDraggingSource*)initWithOperation:(NSDragOperation)operation delegate:(id<GlassDragSourceDelegate>)delegate
{
    dragOperation = operation;
    // The delegate retains this object
    dragDelegate = delegate;
    return self;
}

- (void)draggingSession:(NSDraggingSession *)session willBeginAtPoint:(NSPoint)screenPoint
{
}

- (void)draggingSession:(NSDraggingSession *)session movedToPoint:(NSPoint)screenPoint
{
}

- (void)draggingSession:(NSDraggingSession *)session endedAtPoint:(NSPoint)screenPoint operation:(NSDragOperation)operation
{
    LOG("Dragging session ended");
    [dragDelegate draggingEnded: operation];
}

- (BOOL)ignoreModifierKeysForDraggingSession:(NSDraggingSession *)session
{
    return NO;
}

@end
