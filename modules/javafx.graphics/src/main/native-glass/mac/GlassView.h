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

#import "GlassHostView.h"
#import "GlassFullscreenWindow.h"
#import "GlassViewDelegate.h"

// main GlassView protocol
// TODO: now that we removed GlassView2D, we should collapse the delegate back into GlassView3D
// and use Obj-C catgegories to partition the implementation (just like GlassWindow)
@protocol GlassView <NSObject>

- (id)initWithFrame:(NSRect)frame withJview:(jobject)jview withJproperties:(jobject)jproperties;

- (void)enterFullscreenWithAnimate:(BOOL)animate withKeepRatio:(BOOL)keepRatio withHideCursor:(BOOL)hideCursor;
- (void)exitFullscreenWithAnimate:(BOOL)animate;

// the graphics specifics APIs
- (void)begin;
- (void)end;
- (void)pushPixels:(void*)pixels withWidth:(GLuint)width withHeight:(GLuint)height withScaleX:(GLfloat)scaleX withScaleY:(GLfloat)scaleY withEnv:(JNIEnv *)env;

- (GlassViewDelegate*)delegate;
- (void)setInputMethodEnabled:(BOOL)enabled;
- (void)finishInputMethodComposition;

- (void)notifyScaleFactorChanged:(CGFloat)scale;

@end
