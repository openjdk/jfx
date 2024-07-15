/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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
#import "GlassTouches.h"
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

- (void)windowDidBecomeKey:(NSNotification *)notification
{
    if (self->fullscreenWindow)
    {
        return;
    }

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
}

- (void)windowDidResignKey:(NSNotification *)notification
{
    if (self->fullscreenWindow)
    {
        return;
    }

    [self _ungrabFocus];

    GET_MAIN_JENV_NOWARN;
    if (env != NULL) {
        (*env)->CallVoidMethod(env, self->jWindow, jWindowNotifyFocus, com_sun_glass_events_WindowEvent_FOCUS_LOST);
    }

    // FIXME: KCR -- DEBUG
    if (!env) {
        NSLog(@"KCR: *** windowDidResignKey: Java has been detached -- we just saved a crash!!!");
    }
}

- (void)windowWillClose:(NSNotification *)notification
{
    // Unparent self. Otherwise the code hangs
    if ([self->nsWindow parentWindow])
    {
        [[self->nsWindow parentWindow] removeChildWindow:self->nsWindow];
    }

    // Finally, close owned windows to mimic MS Windows behavior
    NSArray *children = [self->nsWindow childWindows];
    for (NSUInteger i=0; i<[children count]; i++)
    {
        NSWindow *child = (NSWindow*)[children objectAtIndex:i];
        [child close];
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

- (void)windowDidMiniaturize:(NSNotification *)notification
{
    [self _sendJavaWindowResizeEvent:com_sun_glass_events_WindowEvent_MINIMIZE forFrame:[self _flipFrame]];
}

- (void)windowDidDeminiaturize:(NSNotification *)notification
{
    [self _sendJavaWindowResizeEvent:com_sun_glass_events_WindowEvent_RESTORE forFrame:[self _flipFrame]];
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

    (*env)->CallVoidMethod(env, jWindow, jWindowNotifyResize, com_sun_glass_events_WindowEvent_MAXIMIZE, (int)newFrame.size.width, (int)newFrame.size.height);

    return YES;
}

- (void)windowWillEnterFullScreen:(NSNotification *)notification
{
    //NSLog(@"windowWillEnterFullScreen");

    NSUInteger mask = [self->nsWindow styleMask];
    self->isWindowResizable = ((mask & NSWindowStyleMaskResizable) != 0);
    [[self->view delegate] setResizableForFullscreen:YES];
}

- (void)windowDidEnterFullScreen:(NSNotification *)notification
{
    //NSLog(@"windowDidEnterFullScreen");
    [(GlassViewDelegate*)[self->view delegate] sendJavaFullScreenEvent:YES withNativeWidget:YES];
    [GlassApplication leaveFullScreenExitingLoopIfNeeded];
}

- (void)windowWillExitFullScreen:(NSNotification *)notification
{
    //NSLog(@"windowWillExitFullScreen");
}

- (void)windowDidExitFullScreen:(NSNotification *)notification
{
    //NSLog(@"windowDidExitFullScreen");

    GlassViewDelegate* delegate = (GlassViewDelegate*)[self->view delegate];
    [delegate setResizableForFullscreen:self->isWindowResizable];

    [delegate sendJavaFullScreenEvent:NO withNativeWidget:YES];
    [GlassApplication leaveFullScreenExitingLoopIfNeeded];
}

- (BOOL)windowShouldClose:(NSNotification *)notification
{
    if (self->isEnabled)
    {
        GET_MAIN_JENV_NOWARN;
        if (env != NULL) {
            (*env)->CallVoidMethod(env, jWindow, jWindowNotifyClose);
        }

        // FIXME: KCR -- DEBUG
        if (!env) {
            NSLog(@"KCR: *** windowShouldClose: Java has been detached -- we just saved a crash!!!");
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
