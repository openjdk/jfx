/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

#import "GlassMenu.h"
#import "GlassView.h"

// normal Glass window delegate
@interface GlassWindow : NSObject <NSWindowDelegate>
{
    jobject             jWindow;

@public
    // A reference to an NSWindow or NSPanel descendant - the native window
    NSWindow            *nsWindow;

    NSWindow            *owner;
    NSView<GlassView>   *view;
    NSScreen            *currentScreen;
    GlassMenubar        *menubar;
    NSRect              preZoomedRect;
    NSWindow            *fullscreenWindow;

    BOOL                isFocusable;
    BOOL                isEnabled;
    NSUInteger          enabledStyleMask; // valid while the window is disabled
    BOOL                isTransparent;
    BOOL                isDecorated;
    BOOL                isResizable;
    BOOL                suppressWindowMoveEvent;
    BOOL                suppressWindowResizeEvent;

    NSPoint             lastReportedLocation; // which was sent to Java

    BOOL                isClosed;

    // We track whether an explicit size/location have been assigned to the window
    // in order to differentiate between an explicitly assigned zero bounds, and the
    // deafult bounds (which are also zeros - see a comment in _createWindowCommon().)
    BOOL                isSizeAssigned;
    BOOL                isLocationAssigned;

@private
    BOOL                isWindowResizable;
}

- (void)setFullscreenWindow:(NSWindow *)fsWindow;

// NSWindow overrides delegate methods
- (void)close;
- (void)sendEvent:(NSEvent *)event;
- (BOOL)canBecomeMainWindow;
- (BOOL)canBecomeKeyWindow;
- (BOOL)hidesOnDeactivate;
- (BOOL)worksWhenModal;
- (NSColor*)setBackgroundColor:(NSColor *)color;
@end

@interface GlassWindow_Normal : NSWindow
{
@public
    GlassWindow* gWindow;
}

- (id)initWithDelegate:(GlassWindow*)delegate
             frameRect:(NSRect)rect
             styleMask:(NSUInteger)styleMask
                screen:(NSScreen*)screen;
@end

@interface GlassWindow_Panel : NSPanel
{
@public
    GlassWindow* gWindow;
}

- (id)initWithDelegate:(GlassWindow*)delegate
             frameRect:(NSRect)rect
             styleMask:(NSUInteger)styleMask
                screen:(NSScreen*)screen;
@end


extern NSImage* getImage(u_int8_t* data, int jWidth, int jHeight, int jOffset);
