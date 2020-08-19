/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
#import "GlassWindow+Java.h"
#import "GlassScreen.h"

//#define VERBOSE
#ifndef VERBOSE
#define LOG(MSG, ...)
#else
#define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

static NSWindow *s_grabWindow = nil;
extern NSSize maxScreenDimensions;

@interface NSWindow (External)

- (void)_startLiveResize;
- (void)_endLiveResize;

@end

@implementation GlassWindow (Java)

#pragma mark --- Callbacks

- (void)_sendJavaWindowMoveToAnotherScreenEventIfNeeded
{
    NSScreen *newScreen = [self->nsWindow screen];

    // Update only if the newScreen isn't nil
    if (self->currentScreen != newScreen && newScreen != nil)
    {
        self->currentScreen = newScreen;

        GET_MAIN_JENV;
        (*env)->CallVoidMethod(env, jWindow, jWindowNotifyMoveToAnotherScreen, createJavaScreen(env, newScreen));
    }
}

- (void)_sendJavaWindowNotifyScaleChanged:(CGFloat)newScale
{
    GET_MAIN_JENV;
    (*env)->CallVoidMethod(env, jWindow, jWindowNotifyScaleChanged, 1.0f, 1.0f, newScale, newScale);
}

- (void)_sendJavaWindowMoveEventForFrame:(NSRect)frame
{
    if (self->suppressWindowMoveEvent == NO)
    {
        self->lastReportedLocation = frame.origin;

        GET_MAIN_JENV;

        (*env)->CallVoidMethod(env, jWindow, jWindowNotifyMove,
            (int)frame.origin.x,  (int)frame.origin.y, [self->nsWindow isZoomed]);
        [self _sendJavaWindowMoveToAnotherScreenEventIfNeeded];
    }
}

- (void)_sendJavaWindowResizeEvent:(int)type forFrame:(NSRect)frame
{
    if (self->suppressWindowResizeEvent == NO)
    {
        GET_MAIN_JENV;

        if ([self->nsWindow isMiniaturized]) {
            type = com_sun_glass_events_WindowEvent_MINIMIZE;
        } else if ([self->nsWindow isZoomed]) {
            type = com_sun_glass_events_WindowEvent_MAXIMIZE;
        }

        (*env)->CallVoidMethod(env, jWindow, jWindowNotifyResize, type,
             (int)frame.size.width, (int)frame.size.height);
        [self _sendJavaWindowMoveToAnotherScreenEventIfNeeded];
    }
}

#pragma mark --- Additions

- (id)_initWithContentRect:(NSRect)contentRect styleMask:(NSUInteger)windowStyle screen:(NSScreen *)screen jwindow:(jobject)jwindow jIsChild:(jboolean)jIsChild
{
    self = [super init];
    if (self == nil) {
        return nil;
    }

    if (jIsChild == JNI_FALSE) {
        if (windowStyle & (NSUtilityWindowMask | NSNonactivatingPanelMask)) {
            self->nsWindow = [[GlassWindow_Panel alloc] initWithDelegate:self
                                                               frameRect:contentRect
                                                               styleMask:windowStyle
                                                                  screen:screen];
        } else {
            self->nsWindow = [[GlassWindow_Normal alloc] initWithDelegate:self
                                                                frameRect:contentRect
                                                                styleMask:windowStyle
                                                                   screen:screen];
        }
    } else {
        GlassEmbeddedWindow *ewindow = [[GlassEmbeddedWindow alloc] initWithDelegate:self
                                                             frameRect:contentRect
                                                             styleMask:windowStyle
                                                                screen:screen];
        if (ewindow) {
            ewindow->parent = nil;
            ewindow->child = nil;

            self->nsWindow = ewindow;
        }
    }

    if (self->nsWindow == nil) {
        NSLog(@"Unable to create GlassWindow_Normal or GlassWindow_Panel");
        return nil;
    }

    self->jWindow = jwindow;

    self->isFocusable = YES;
    self->isEnabled = YES;
    self->currentScreen = screen;

    self->suppressWindowMoveEvent = NO;
    self->suppressWindowResizeEvent = NO;

    self->isClosed = NO;

    // This is surely can't be a real location, which indicates
    // we've never sent a MOVE event to Java yet.
    self->lastReportedLocation.x = self->lastReportedLocation.y = FLT_MAX;

    CGFloat x = 0.0f;
    CGFloat y = [screen frame].size.height - [screen visibleFrame].size.height;
    CGFloat w = [self->nsWindow frame].size.width;
    CGFloat h = [self->nsWindow frame].size.height;
    [self _setFlipFrame:NSMakeRect(x, y, w, h) display:YES animate:NO];

    //[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(windowDidMiniaturize:) name:NSWindowDidMiniaturizeNotification object:nil];
    //[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(windowDidDeminiaturize:) name:NSWindowDidMiniaturizeNotification object:nil];

    return self;
}

- (NSWindow*)_getCurrentWindow
{
    return self->fullscreenWindow ? self->fullscreenWindow : self->nsWindow;
}

- (void)_ungrabFocus
{
    NSWindow *window = [self _getCurrentWindow];

    if (s_grabWindow != window) {
        return;
    }

    GET_MAIN_JENV;
    (*env)->CallVoidMethod(env, self->jWindow, jWindowNotifyFocusUngrab);

    s_grabWindow = nil;
}

+ (void)_resetGrab
{
    if (s_grabWindow && [[s_grabWindow delegate] isKindOfClass:[GlassWindow class]]) {
        GlassWindow * window = (GlassWindow*)[s_grabWindow delegate];
        [window _ungrabFocus];
    }
    s_grabWindow = nil; // unconditionally
}

- (void)_checkUngrab
{
    if (!s_grabWindow) {
        return;
    }

    // If this window doesn't belong to an owned windows hierarchy that
    // holds the grab currently, then the grab should be released.
    for (NSWindow * window = self->nsWindow; window; window = [window parentWindow]) {
        if (window == s_grabWindow) {
            return;
        }
    }

    [GlassWindow _resetGrab];
}

- (void)_grabFocus
{
    NSWindow *window = [self _getCurrentWindow];

    if (s_grabWindow == window) {
        return;
    }

    [GlassWindow _resetGrab];
    s_grabWindow = window;
}

- (void)_setResizable
{
    NSUInteger mask = [self->nsWindow styleMask];
    if ((mask & NSResizableWindowMask) != 0)
    {
        if (self->isDecorated == YES)
        {
            mask &= ~(NSUInteger)NSResizableWindowMask;
            [self->nsWindow setStyleMask: mask];
            [self->nsWindow setShowsResizeIndicator:NO];

            NSButton *zoomButton = [self->nsWindow standardWindowButton:NSWindowZoomButton];
            [zoomButton setEnabled:NO];
        }
        self->isResizable = NO;
    }
    else
    {
        if (self->isDecorated == YES)
        {
            mask |= NSResizableWindowMask;
            [self->nsWindow setStyleMask: mask];
            [self->nsWindow setShowsResizeIndicator:YES];

            NSButton *zoomButton = [self->nsWindow standardWindowButton:NSWindowZoomButton];
            [zoomButton setEnabled:YES];
        }
        self->isResizable = YES;
    }
}

- (NSRect)_constrainFrame:(NSRect)frame
{
    NSSize minSize = [self->nsWindow minSize];
    NSSize maxSize = [self->nsWindow maxSize];
    NSSize size = frame.size;

    NSRect constrained = frame;
    {
        if (size.width < minSize.width)
        {
            constrained.size.width = minSize.width;
        }
        else if (size.width > maxSize.width)
        {
            constrained.size.width = maxSize.width;
        }

        if (size.height < minSize.height)
        {
            constrained.size.height = minSize.height;
        }
        else if (size.height > maxSize.height)
        {
            constrained.size.height = maxSize.height;
        }
    }
    return constrained;
}

- (void)_setVisible
{
    LOG("_setVisible: focusable %d enabled %d", self->isFocusable, self->isEnabled);

    if (self->isFocusable == YES && self->isEnabled == YES)
    {
        [self->nsWindow makeMainWindow];
        [self->nsWindow makeKeyAndOrderFront:nil];
    }
    else
    {
        [self->nsWindow orderFront:nil];
    }

    if ((self->owner != nil) && ([self->nsWindow parentWindow] == nil))
    {
        [self->owner addChildWindow:self->nsWindow ordered:NSWindowAbove];
    }
    // Make sure we synchronize scale factors which could have changed while
    // we were not visible without invoking the overrides we watch.
    if ([self->nsWindow screen] && (self->view != nil)) {
        [self->view notifyScaleFactorChanged:GetScreenScaleFactor([self->nsWindow screen])];
    }
}

- (void)_setWindowFrameWithRect:(NSRect)rect withDisplay:(jboolean)display withAnimate:(jboolean)animate
{
    NSRect frame = [self _constrainFrame:rect];
    NSString *const constantRestorePreZoomRect = @"_restorePreZoomedRect";
    NSArray *syms = [NSThread  callStackSymbols];
    NSString *callerMethod;

    bool callFlipFrame = true;
    if ([syms count] > 1) {
        callerMethod = [syms objectAtIndex:1];
        if([callerMethod rangeOfString:constantRestorePreZoomRect].location != NSNotFound){
            callFlipFrame = false;
        }
    }
    if (callFlipFrame) {
        [self _setFlipFrame:frame display:(BOOL)display animate:(BOOL)animate];
    }
    else {
        [self->nsWindow setFrame:frame display:(BOOL)display animate:(BOOL)animate];
    }
}

- (void)_setBounds:(jint)x y:(jint)y xSet:(jboolean)xSet ySet:(jboolean)ySet w:(jint)w h:(jint)h cw:(jint)cw ch:(jint)ch
{
    NSPoint origin = [self _flipFrame].origin;
    NSSize size = [self->nsWindow frame].size;
    NSSize sizeForClient = [NSWindow frameRectForContentRect:NSMakeRect(0.0, 0.0, cw > 0 ? cw : 0.0, ch > 0 ? ch : 0.0) styleMask:[self->nsWindow styleMask]].size;

    jint newX = xSet == JNI_TRUE ? x : (jint)origin.x;
    jint newY = ySet == JNI_TRUE ? y : (jint)origin.y;

    jint newW = (w > 0) ? w : (cw > 0) ? (jint)sizeForClient.width : (jint)size.width;
    jint newH = (h > 0) ? h : (ch > 0) ? (jint)sizeForClient.height : (jint)size.height;

    [self _setWindowFrameWithRect:NSMakeRect(newX, newY, newW, newH) withDisplay:JNI_TRUE withAnimate:JNI_FALSE];
}

- (void)_restorePreZoomedRect
{
    [self _setWindowFrameWithRect:NSMakeRect(self->preZoomedRect.origin.x, self->preZoomedRect.origin.y, self->preZoomedRect.size.width, self->preZoomedRect.size.height) withDisplay:JNI_TRUE withAnimate:JNI_TRUE];
    [self _sendJavaWindowMoveEventForFrame:[self _flipFrame]];
    [self _sendJavaWindowResizeEvent:com_sun_glass_events_WindowEvent_RESTORE forFrame:[self _flipFrame]];
}

- (NSScreen*)_getScreen
{
    NSScreen *screen = [self->nsWindow screen];
    if (screen == nil)
    {
        screen = self->currentScreen;
    }
    if (screen == nil)
    {
        screen = [[NSScreen screens] objectAtIndex: 0];
    }
    return screen;
}

#pragma mark --- Flip

- (void)_setFlipFrame:(NSRect)frameRect display:(BOOL)displayFlag animate:(BOOL)animateFlag
{
    //NSLog(@"_setFlipFrame:   %.2f,%.2f %.2fx%.2f", frameRect.origin.x, frameRect.origin.y, frameRect.size.width, frameRect.size.height);
    NSScreen * screen = [[NSScreen screens] objectAtIndex: 0];
    NSRect screenFrame = screen.frame;
    //NSLog(@"            screenFrame: %.2f,%.2f %.2fx%.2f", screenFrame.origin.x, screenFrame.origin.y, screenFrame.size.width, screenFrame.size.height);

    frameRect.origin.y = screenFrame.size.height - frameRect.size.height - frameRect.origin.y;
    //NSLog(@"            set to frameRect:%.2f,%.2f %.2fx%.2f", frameRect.origin.x, frameRect.origin.y, frameRect.size.width, frameRect.size.height);

    if (frameRect.size.width > maxScreenDimensions.width) {
        frameRect.size.width = maxScreenDimensions.width;
    }

    if (frameRect.size.height > maxScreenDimensions.height) {
        frameRect.size.height = maxScreenDimensions.height;
    }

    [self->nsWindow setFrame:frameRect display:displayFlag animate:animateFlag];

    //frameRect = [self _flipFrame];
    //NSLog(@"            _flipFrame:%.2f,%.2f %.2fx%.2f", frameRect.origin.x, frameRect.origin.y, frameRect.size.width, frameRect.size.height);
    //frameRect = [super frame];
    //NSLog(@"            frame:%.2f,%.2f %.2fx%.2f", frameRect.origin.x, frameRect.origin.y, frameRect.size.width, frameRect.size.height);
}

- (NSRect)_flipFrame
{
    NSScreen * screen = [[NSScreen screens] objectAtIndex: 0];
    NSRect screenFrame = screen.frame;

    NSRect frame = [self->nsWindow frame];
    //NSLog(@"_flipFrame: v.s.h=%.2f f.s.h=%.2f f.o.y=%.2f", screenFrame.size.height, frame.size.height, frame.origin.y);
    frame.origin.y = screenFrame.size.height - frame.size.height - frame.origin.y;
    //NSLog(@"                            result: f.o.y=%.2f", frame.origin.y);

    //NSLog(@"_flipFrame:   %.2f,%.2f %.2fx%.2f", frame.origin.x, frame.origin.y, frame.size.width, frame.size.height);
    return frame;
}

@end
