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

#import <Cocoa/Cocoa.h>
#import <jni.h>

@protocol GlassDragSourceDelegate <NSObject>

- (void)startDrag:(NSDragOperation)operation withItems:(NSArray<NSDraggingItem*>*)items;
- (void)draggingEnded:(NSDragOperation)operation;

@end

@interface GlassDragSource : NSObject

+ (void)setDelegate:(NSObject<GlassDragSourceDelegate>*)delegate;
+ (BOOL)isDelegateSet;
+ (void)flushWithMask:(jint)mask withItems:(NSArray<NSDraggingItem*>*)items;

+ (NSDragOperation)mapJavaMaskToNsOperation:(jint)mask;
+ (jint)mapNsOperationToJavaMaskExternal:(NSDragOperation)operation;
+ (jint)mapNsOperationToJavaMaskInternal:(NSDragOperation)operation;
+ (jint)getRecommendedActionForMaskExternal:(NSDragOperation)operation;
+ (jint)getRecommendedActionForMaskInternal:(NSDragOperation)operation;

+ (void)setMask:(jint)mask;
+ (jint)getMask;

+ (jint)getSupportedActions;
+ (void)setSupportedActions:(jint)actions;

@end
