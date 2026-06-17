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

#import "common.h"
#import "com_sun_glass_events_ViewEvent.h"
#import "com_sun_glass_events_WindowEvent.h"
#import "com_sun_glass_ui_Window.h"
#import "com_sun_glass_ui_Window_Level.h"
#import "com_sun_glass_ui_mac_MacWindow.h"

#import "GlassMacros.h"
#import "GlassWindow.h"
#import "GlassWindow+Java.h"
#import "GlassWindow+Overrides.h"
#import "GlassViewDelegate.h"
#import "GlassApplication.h"
#import "GlassScreen.h"

#import <AppKit/NSGraphics.h> // NSBeep();

@implementation GlassWindow (Overrides)

- (void)dealloc
{
    assert(pthread_main_np() == 1);
    JNIEnv *env = jEnv;
    if (env != NULL)
    {
        (*env)->DeleteGlobalRef(env, self->jWindow);
        GLASS_CHECK_EXCEPTION(env);
    }

    self->jWindow = NULL;

    [super dealloc];
}

#pragma mark --- Delegate

- (void)windowDidChangeScreen:(NSNotification *)notification
{
    //NSLog(@"windowDidChangeScreen: %p  screen: %p", self, [self->nsWindow screen]);

    // Fix up window stacking order
    [self reorderChildWindows];
}

- (void)windowDidBecomeKey:(NSNotification *)notification
{
    //NSLog(@"windowDidBecomeKey: %p", self);

    // store host menu if running embedded, otherwise we
    // just store a default menu
    self->hostMenu = [NSApp mainMenu];

    GET_MAIN_JENV;
    if (!self->isEnabled)
    {
        NSBeep();
        (*env)->CallVoidMethod(env, self->jWindow, jWindowNotifyFocusDisabled);
        return;
    }

    (*env)->CallVoidMethod(env, self->jWindow, jWindowNotifyFocus, com_sun_glass_events_WindowEvent_FOCUS_GAINED);

    if (self->menubar != nil)
    {
        [NSApp setMainMenu:self->menubar->menu];
    }
    [[NSApp mainMenu] update];

    // Fix up window stacking order
    [self reorderChildWindows];
}

- (void)windowDidResignKey:(NSNotification *)notification
{
    //NSLog(@"windowDidResignKey: %p", self);

    NSMenu* menu = nil;

    if (self->menubar != nil) {
        menu = self->menubar->menu;
    }

    // restore menu of host application if running embedded,
    // otherwise we just restore a default menu
    if ([NSApp mainMenu] == menu) {
        [NSApp setMainMenu:self->hostMenu];
        [[NSApp mainMenu] update];
    }

    [self _ungrabFocus];

    GET_MAIN_JENV_NOWARN;
    if (env != NULL) {
        (*env)->CallVoidMethod(env, self->jWindow, jWindowNotifyFocus, com_sun_glass_events_WindowEvent_FOCUS_LOST);
    }
}

- (void)windowWillClose:(NSNotification *)notification
{
    //NSLog(@"windowWillClose");
    // Remove self from list of owner's child windows
    if (self->owner != nil) {
        [self->owner removeChildWindow:self];
    }

    // Finally, close owned windows to mimic MS Windows behavior
    if (self->childWindows != nil) {
        // Iterate over an immutable copy
        NSArray *children = [[NSArray alloc] initWithArray:self->childWindows];
        for (GlassWindow *child in children) {
            [child->nsWindow close];
        }
        [children release];
    }

    // If we have an owner, reorder its remaining children
    if (self->owner != nil) {
        [self->owner reorderChildWindows];
    }

    // Call the notification method
    assert(pthread_main_np() == 1);
    JNIEnv *env = jEnv;
    if (env != NULL) {
        (*env)->CallVoidMethod(env, self->jWindow, jWindowNotifyDestroy);
    }
}

- (void)windowWillMove:(NSNotification *)notification
{
    //NSLog(@"windowWillMove");
}

- (void)windowDidMove:(NSNotification *)notification
{
    //NSLog(@"windowDidMove");
    [self _sendJavaWindowMoveEventForFrame:[self _flipFrame]];
}

- (void)windowDidChangeBackingProperties:(NSNotification *)notification
{
    // The spec for [NSWindow backingScaleFactor] only mentions 1.0 and 2.0
    // whereas NSScreen's one is more generic. So use the latter.
    if ([self->nsWindow screen]) {
        CGFloat scale = GetScreenScaleFactor([self->nsWindow screen]);
        [self->view notifyScaleFactorChanged:scale];
        [self _sendJavaWindowNotifyScaleChanged:scale];
    }

    // Screen.notifySettingsChanged() calls Window.setScreen(), and the latter
    // generates the Window.EventHandler.handleScreenChangedEvent notification.
}

- (NSSize)windowWillResize:(NSWindow *)window toSize:(NSSize)proposedFrameSize
{
    //NSLog(@"windowWillResize");
    return proposedFrameSize;
}

- (void)windowDidResize:(NSNotification *)notification
{
    //NSLog(@"windowDidResize");
    NSRect frame = [self _flipFrame];

    if ((int)self->lastReportedLocation.x != (int)frame.origin.x ||
            (int)self->lastReportedLocation.y != (int)frame.origin.y)
    {
        [self _sendJavaWindowMoveEventForFrame:frame];

        // Refetch the frame since it might've been modified due to the previous call
        frame = [self _flipFrame];
    }

    [self _sendJavaWindowResizeEvent:com_sun_glass_events_WindowEvent_RESIZE forFrame:frame];
}

- (void)windowWillMiniaturize:(NSNotification *)notification
{
    //NSLog(@"windowWillMiniaturize: %p", self);
}

- (void)windowDidMiniaturize:(NSNotification *)notification
{
    //NSLog(@"windowDidMiniaturize: %p", self);
    [self _sendJavaWindowResizeEvent:com_sun_glass_events_WindowEvent_MINIMIZE forFrame:[self _flipFrame]];
    [self minimizeChildWindows:YES];
}

- (void)windowDidDeminiaturize:(NSNotification *)notification
{
    //NSLog(@"windowDidDeminiaturize: %p", self);
    [self _sendJavaWindowResizeEvent:com_sun_glass_events_WindowEvent_RESTORE forFrame:[self _flipFrame]];
    [self minimizeChildWindows:NO];
    [self reorderChildWindows];
}

- (BOOL)windowShouldZoom:(NSWindow *)window toFrame:(NSRect)newFrame
{
    GET_MAIN_JENV;

    // This is called both to zoom a window and to unzoom it. If the window was
    // zoomed when it was initially created the OS might try to unzoom it to a
    // very small size.
    if (newFrame.size.width <= 1 || newFrame.size.height <= 1) {
        return NO;
    }

    return YES;
}

- (void)windowWillEnterFullScreen:(NSNotification *)notification
{
    //NSLog(@"windowWillEnterFullScreen");

    NSUInteger mask = [self->nsWindow styleMask];
    self->isWindowResizable = ((mask & NSWindowStyleMaskResizable) != 0);
    [[self->view delegate] setResizableForFullscreen:YES];

    // When we switch to full-screen mode, we always need the standard window buttons to be shown.
    [[self->nsWindow standardWindowButton:NSWindowCloseButton] setHidden:NO];
    [[self->nsWindow standardWindowButton:NSWindowMiniaturizeButton] setHidden:NO];
    [[self->nsWindow standardWindowButton:NSWindowZoomButton] setHidden:NO];

    if (nsWindow.toolbar != nil) {
        nsWindow.toolbar.visible = NO;
    }
    // Allow child windows to move to the same space as this full-screen window
    [self setMoveToActiveSpaceChildWindows:YES];
}

- (void)windowDidEnterFullScreen:(NSNotification *)notification
{
    //NSLog(@"windowDidEnterFullScreen");
    [(GlassViewDelegate*)[self->view delegate] sendJavaFullScreenEvent:YES withNativeWidget:YES];
    [GlassApplication leaveFullScreenExitingLoopIfNeeded];

    // Fix up window stacking order then disable moving child windows to active space
    [self reorderChildWindows];
    [self setMoveToActiveSpaceChildWindows:NO];
}

- (void)windowWillExitFullScreen:(NSNotification *)notification
{
    //NSLog(@"windowWillExitFullScreen");

    // When we exit full-screen mode, hide the standard window buttons if they were previously hidden.
    if (!self->isStandardButtonsVisible) {
        [[self->nsWindow standardWindowButton:NSWindowCloseButton] setHidden:YES];
        [[self->nsWindow standardWindowButton:NSWindowMiniaturizeButton] setHidden:YES];
        [[self->nsWindow standardWindowButton:NSWindowZoomButton] setHidden:YES];
    }
}

- (void)windowDidExitFullScreen:(NSNotification *)notification
{
    //NSLog(@"windowDidExitFullScreen");

    if (nsWindow.toolbar != nil) {
        nsWindow.toolbar.visible = YES;
    }

    GlassViewDelegate* delegate = (GlassViewDelegate*)[self->view delegate];
    [delegate setResizableForFullscreen:self->isWindowResizable];

    [delegate sendJavaFullScreenEvent:NO withNativeWidget:YES];
    [GlassApplication leaveFullScreenExitingLoopIfNeeded];

    // Fix up window stacking order
    [self reorderChildWindows];
}

- (BOOL)windowShouldClose:(NSNotification *)notification
{
    if (self->isEnabled)
    {
        GET_MAIN_JENV_NOWARN;
        if (env != NULL) {
            (*env)->CallVoidMethod(env, jWindow, jWindowNotifyClose);
        }
    }

    // it's up to app to decide if the window should be closed
    return FALSE;
}

#pragma mark --- Title Icon

- (BOOL)window:(NSWindow *)window shouldPopUpDocumentPathMenu:(NSMenu *)menu
{
    return NO;
}

- (BOOL)window:(NSWindow *)window shouldDragDocumentWithEvent:(NSEvent *)event from:(NSPoint)dragImageLocation withPasteboard:(NSPasteboard *)pasteboard
{
    return NO;
}

@end
