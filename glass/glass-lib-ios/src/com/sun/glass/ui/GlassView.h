/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
#import <QuartzCore/QuartzCore.h>

#import "GlassMacros.h"
#import "GlassViewDelegate.h"

#include "common.h"



// main GlassView protocol
@protocol GlassView <NSObject>

- (id)initWithFrame:(CGRect)frame withJview:(jobject)jview withJproperties:(jobject)jproperties;

// the graphics specifics APIs
- (void)begin;
- (void)end;

- (void)requestInput:(NSString *)text type:(int)type width:(double)width height:(double)height 
                 mxx:(double)mxx mxy:(double)mxy mxz:(double)mxz mxt:(double)mxt 
                 myx:(double)myx myy:(double)myy myz:(double)myz myt:(double)myt
                 mzx:(double)mzx mzy:(double)mzy mzz:(double)mzz mzt:(double)mzt;
- (void)releaseInput;

@end
