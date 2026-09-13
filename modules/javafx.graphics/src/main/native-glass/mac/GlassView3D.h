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

#import "GlassView.h"
#import "GlassLayer.h"

// GlassView3D is subView of GlassHostView and it performs event
// handling tasks related to both OpenGL and Metal pipeline
@interface GlassView3D : NSView <GlassView, NSTextInputClient>
{
    GlassViewDelegate   *_delegate;
    NSTrackingArea      *_trackingArea;
    GlassLayer *layer;

    NSView *subView;

    NSAttributedString *nsAttrBuffer;
    BOOL imEnabled;
    BOOL handlingKeyEvent;
    BOOL didCommitText;

    BOOL isHiDPIAware;
    NSEvent *lastKeyEvent;

    // These fields track state for the Keyman input method.
    BOOL keymanActive;
    BOOL sendKeyEvent;
    unichar insertTextChar;
}

- (GlassViewDelegate*)delegate;
- (id)initWithFrame:(NSRect)frame withJview:(jobject)jView withJproperties:(jobject)jproperties;
- (void)setFrameOrigin:(NSPoint)newOrigin;
- (CALayer*)getLayer;

@end
