/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

@interface GlassTitleBar : NSObject

// When an instance is created the NSWindow's standardWindowButton: method needs
// to return the default buttons so we can hide them. After this is created the
// NSWindow should defer to this object when asked for a standard window button.
-(instancetype)initWithWindow:(NSWindow*)window;
// When the window is transitioning to a traditional title bar
-(void)detachFromWindow;

// Called by the NSWindow's standardWindowButton: method.
-(NSButton*)standardWindowButton:(NSWindowButton)type;

// Sets the view which contains the JFX content and the host view that contains
// it. The GlassTitleBar will add additional views above and below the content
// to produce the title bar effect and provide the stoplight controls.
-(void)setHostView:(NSView*)hostView jfxView:(NSView*)jfxView;

// The JFX view must implement hitTest: to test whether a point hits a JavaFX
// control or not. It must also implement mouseDown. If the hitTest returns nil
// the mouse click will fall through to the host view which will forward it
// here.
-(void)handleMouseDown:(NSEvent*)event;

// The height of the title bar
@property CGFloat height;

// The insets (left and right only) which allow
// clients to avoid the platform decorations.
@property (readonly) NSEdgeInsets insets;
@end
