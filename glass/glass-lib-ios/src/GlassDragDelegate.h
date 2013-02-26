/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

@protocol GlassDragSourceDelegate <NSObject>

- (void)startDrag:(int)operation;

@end


@interface GlassDragDelegate : NSObject
{
    
}

+ (void) setDragViewParent:(UIView *)parent;

// try to start new drag session
+ (void) drag:(CGPoint)_dragSourceLocation operation:(jint)_operation glassView:(UIView*)_glassView;

+ (BOOL) isDragging; // drag and drop session is in progress; there can be always zero or one Drag and Drop in progress

// during drag and drop session, all touch events are delivered to GlassDragDelegate
+ (void) touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event withMouse:(UITouch *)mouse;
+ (void) touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event withMouse:(UITouch *)mouse;
+ (void) touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event withMouse:(UITouch *)mouse;
+ (void) touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event withMouse:(UITouch *)mouse;

// release resources
+ (void) cleanup;

+ (void)setDelegate:(NSObject<GlassDragSourceDelegate>*)delegate;
// force drop and/or other allowed operation
+ (void)flushWithMask:(jint)mask;

// set get clipboard mask; based on this state variable we know what operation is currently supported
+ (void)setMask:(jint)mask;
+ (jint)getMask;

@end
