/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
#import <Foundation/NSNotification.h>

#include "GlassWindow.h"

#include "com_sun_glass_events_WindowEvent.h"
#include "com_sun_glass_ui_Window_Level.h"
#include "com_sun_glass_ui_Window.h"
#include "GlassViewGL.h"
#include "GlassApplication.h"
#include "GlassViewController.h"

static UIView * s_grabWindow = nil;
static GlassWindow   * focusOwner; // currently focused GlassWindow - i.e. key events receiver


@implementation GlassMainWindow

-(id)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(keyboardDidShow:)
                                                 name:UIKeyboardDidShowNotification
                                               object:nil];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(keyboardDidHide:)
                                                 name:UIKeyboardDidHideNotification
                                               object:nil];
    return self;
}

// multitouch debugging
- (void) sendEvent:(UIEvent *)event
{
    GLASS_LOG("GlassMainWindow received UIEvent: %@", event);
    [super sendEvent:event];
}

- (void) keyboardDidShow:(NSNotification *) notification
{
#if MAT_IOS_DEBUG
    GLASS_LOG("[GlassMainWindow keyboardDidShow]");
    NSDictionary *info = [notification userInfo];
    CGRect keyboardFrame = [[info objectForKey:UIKeyboardFrameEndUserInfoKey] CGRectValue];
    GLASS_LOG("Keyboard frame x = %f, y = %f, width = %f, height = %f", keyboardFrame.origin.x, keyboardFrame.origin.y, keyboardFrame.size.width, keyboardFrame.size.height);
#endif
}

- (void) keyboardDidHide:(NSNotification *) notification
{
    GLASS_LOG("[GlassMainWindow keyboardidHide]");
    [self resignFocusOwner];
}

- (void) resignFocusOwner {
    [focusOwner resignKeyWindow];
}

@end

@implementation GlassMainView

- (id) initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self != nil) {
        [GlassDragDelegate setDragViewParent:self];
    }
    return self;
}

- (void) dealloc {
    [GlassDragDelegate setDragViewParent:nil];
    [GlassDragDelegate cleanup];
    [super dealloc];
}

@end

//Toplevel containers of all GlassWindows
//once we support multiple screens on iOS - there will be one mainWindow/
//mainWindowHost per screen
static GlassMainWindow * mainWindow = nil;
static GlassMainView * mainWindowHost = nil;

@interface GlassWindow (JavaAdditions)
- (void)displaySubviews;
- (void)_setLevel;
- (void)orderBack;
- (void)_orderBack;
- (void)orderFrontRegardless;
- (void)_orderFrontRegardless;

- (void)addChildWindow:(GlassWindow*)child;
- (void)removeChildWindow:(GlassWindow*)child;

- (void)_setAlpha;
- (void)_setBoundsAndPosition;
- (CGSize)_constrainBounds:(CGRect)cFrame;
- (void)_setMinimumSize;
- (void)_setMaximumSize;
- (void)_setVisible;

+ (void)_resetGrab;
- (void)_grabFocus;
- (void)_ungrabFocus;
- (void)_checkUngrab;

- (void)_sendJavaWindowMoveEventForFrame:(CGRect)frame;
- (void)_sendJavaWindowResizeEvent:(int)type forFrame:(CGRect)frame;

- (void)becomeKeyWindow;
- (void)resignKeyWindow;
- (void)windowWillClose;
- (void)sendEvent:(UIEvent *)event;
@end


@interface GlassWindow (Java)
- (id)initWithScreen:(UIScreen *)screen jwindow:(jobject)jwindow;
@end



static inline GlassWindow *getGlassWindow(JNIEnv *env, jlong jPtr)
{
    if (jPtr != 0L)
    {
        return (GlassWindow*)jlong_to_ptr(jPtr);
    }
    else
    {
        return nil;
    }
}


static inline UIView<GlassView>* getiOSView(JNIEnv *env, jobject jview)
{
    if (jview != NULL)
    {
        return (UIView<GlassView>*)jlong_to_ptr((*env)->GetLongField(env, jview, (*env)->GetFieldID(env, mat_jViewClass, "nativePtr", "J")));
    }
    else
    {
        return nil;
    }
}


static inline void setWindowFrame(GlassWindow *window, CGFloat x, CGFloat y, CGFloat w, CGFloat h, jboolean display, jboolean animate)
{
    // set help variables
    window->_setFrameX = x;
    window->_setFrameY = y;
    window->_setFrameWidth = w;
    window->_setFrameHeight = h;
    window->_setFrameDisplay = display;
    window->_setFrameAnimated = animate;

    if ([[NSThread currentThread] isMainThread] == YES)
    {
        [window _setBoundsAndPosition]; // update origin and bounds
        if (display == JNI_TRUE) {
            GLASS_LOG("calling displaySubviews");
            [window displaySubviews];
            GLASS_LOG("called displaySubviews");
        }
        GLASS_LOG("GlassWindow frame after setWindowFrame: %f,%f,%f,%f",[window center].x - [window bounds].size.width / 2,[window center].y - [window bounds].size.height / 2, [window bounds].size.width,[window bounds].size.height);
    }
    else
    {
        [window performSelectorOnMainThread:@selector(_setBoundsAndPosition) withObject:nil waitUntilDone:YES];
        if (display == JNI_TRUE)
            [window performSelectorOnMainThread:@selector(displaySubviews) withObject:nil waitUntilDone:YES];
    }
}


@implementation GlassWindow

+(GlassMainWindow *)  getMainWindow {
    return mainWindow;
}

+(GlassMainView *) getMainWindowHost {
    return mainWindowHost;
}

- (BOOL) canBecomeFirstResponder {return YES;}

- (BOOL)hasText {
        return YES;
}

- (void)insertText:(NSString *)theText {
    const char * inputString = [theText UTF8String];
    for(GlassViewGL * subView in [self->hostView subviews]) {
        if(subView != nil && [subView isKindOfClass:[GlassViewGL class]] == YES) {
            [subView doInsertText:theText];
        }
    }
}

- (void)deleteBackward {
    for(GlassViewGL * subView in [self->hostView subviews]) {
        if(subView != nil && [subView isKindOfClass:[GlassViewGL class]] == YES) {
            [subView doDeleteBackward];
        }
    }
}

JNIEXPORT void JNICALL Java_javafx_scene_control_skin_TextFieldSkinIos_showSoftwareKeyboard
(JNIEnv *env, jobject jTextFieldSkin)
{
    [focusOwner becomeFirstResponder];
}

JNIEXPORT void JNICALL Java_javafx_scene_control_skin_TextFieldSkinIos_hideSoftwareKeyboard
(JNIEnv *env, jobject jTextFieldSkin)
{
    [focusOwner resignFirstResponder];
}

JNIEXPORT void JNICALL Java_javafx_scene_control_skin_TextAreaSkinIos_showSoftwareKeyboard
(JNIEnv *env, jobject jTextAreaSkin)
{
    [focusOwner becomeFirstResponder];
}

JNIEXPORT void JNICALL Java_javafx_scene_control_skin_TextAreaSkinIos_hideSoftwareKeyboard
(JNIEnv *env, jobject jTextAreaSkin)
{
    [focusOwner resignFirstResponder];
}


// request subviews to repaint
- (void) displaySubviews
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    for (UIView * subView in [self->hostView subviews]) {
        if (subView != nil) {
            [subView setNeedsDisplay];
        }
    }
}

#pragma mark ---

// close window (hide and destroy it)
- (void) close {
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    [self _ungrabFocus];
    [self setHidden:YES];

    [self windowWillClose];

    [mainWindowHost release];
    [mainWindow release];//decrease retaincount
}


- (void)setEnabled:(BOOL)enabled
{
    GLASS_LOG("GlassWindow setEnabled");
    self->isEnabled = enabled;
}


#pragma mark --- Java

- (id)initWithScreen:(UIScreen *)screen jwindow:(jobject)jwindow
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    CGRect bounds = CGRectMake(0.0f,0.0f,0.0f,0.0f);
    self = (GlassWindow *)[super initWithFrame: bounds];
    if (self != nil)
    {
        self->jWindow = jwindow;
        self->isFocusable = YES; // can become key window

        self->suppressWindowMoveEvent = NO;
        self->suppressWindowResizeEvent = NO;
        self->isEnabled = YES;

        //default values of min/max frame sizes
        self->minWidth = self->minHeight = 0.0f;
        self->maxWidth = self->maxHeight = CGFLOAT_MAX;

        self->childWindows = [NSMutableArray arrayWithCapacity:(NSUInteger)1];
        self->childWindows = [self->childWindows retain];

        // default to opaque
        [self _setTransparent:NO];

        [self setAutoresizesSubviews:NO];

    }
    return self;
}

- (UIKeyboardType) keyboardType
{
    return UIKeyboardTypeASCIICapable;
}

#pragma mark ---

- (void)_setTransparent:(BOOL)state
{
    GLASS_LOG("GlassWindow _setTransparent called.");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    // should we store original background color?
    // This is only set during window creation so I don't think it's necessary
    self->isTransparent = state;
    if (self->isTransparent == YES)
    {
        [super setBackgroundColor:[UIColor clearColor]];
        [super setOpaque:NO];
    }
    else
    {
        [super setBackgroundColor:[UIColor blackColor]];
        [super setOpaque:YES];
    }
}


#pragma mark --- JavaAdditions

- (void)_setAlpha
{
    GLASS_LOG("GlassWindow _setAlpha called.");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    [self setAlpha:_setAlpha];
}


- (void)_setBoundsAndPosition
{
    GLASS_LOG("_GlassWindow _setFrame called");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    CGRect frameRect = CGRectMake(0.0, 0.0, self->_setFrameWidth, self->_setFrameHeight);

    GLASS_LOG("bounds width, height before constraining %f, %f ", self->_setFrameWidth, self->_setFrameHeight);

    CGSize constrainedSize = [self _constrainBounds:frameRect];
    //if larger than maxSize | smaller than minSize
    if (frameRect.size.width != constrainedSize.width ||
        frameRect.size.height != constrainedSize.height) {
        self->_setFrameWidth = constrainedSize.width;
        self->_setFrameHeight = constrainedSize.height;
        frameRect.size.width = constrainedSize.width;
        frameRect.size.height = constrainedSize.height;
    }

    GLASS_LOG("bounds width, height after constraining %f, %f ", self->_setFrameWidth, self->_setFrameHeight);


    [self setBounds: frameRect];


    CGPoint newCenter = CGPointMake(self->_setFrameX + self->_setFrameWidth / 2, self->_setFrameY + self->_setFrameHeight / 2);

    [self setCenter:newCenter];

    GLASS_LOG("BOUNDS after GlassWindow _setFrame == %f, %f, center == %f %f",[self bounds].size.width,[self bounds].size.height ,[self center].x, [self center].y);

    GLASS_LOG("FRAME after GlassWindow _setFrame == %f, %f, %f, %f",[self frame].size.width,[self frame].size.height ,[self frame].origin.x, [self frame].origin.y);
}

-(void) setBounds:(CGRect)bounds
{
    CGRect frameRect = bounds;
    if (self->owner == nil) { // primary Stage
        GLASS_LOG("primaryStage was asked to resize to %f, %f",bounds.size.width, bounds.size.height);
        frameRect = [[self superview] bounds];
        GLASS_LOG("primaryStage resized to %f, %f",frameRect.size.width, frameRect.size.height);
    }

    [super setBounds:frameRect];

    [self->hostView setFrame:frameRect];//hostView is always same size as GlassWindow

    for(GlassViewGL * subView in [self->hostView subviews]) {
        if(subView != nil && [subView isKindOfClass:[GlassViewGL class]] == YES) {
            [subView setFrame:frameRect];
        }
    }

    [self _sendJavaWindowResizeEvent:com_sun_glass_events_WindowEvent_RESIZE forFrame:[self bounds]];
}

-(void) setCenter:(CGPoint)center
{
    CGPoint newCenter = center;

    if (self->owner == nil) { // primary Stage
        GLASS_LOG("primaryStage was asked to setCenter to %f, %f",center.x, center.y);
        CGRect frameRect = [[self superview] bounds];
        newCenter = CGPointMake(frameRect.size.width/2, frameRect.size.height/2);

        GLASS_LOG("primaryStage setCenter to %f, %f",newCenter.x, newCenter.y);
    }

    [super setCenter:newCenter];

    [self _sendJavaWindowMoveEventForFrame:CGRectMake([self center].x - [self bounds].size.width / 2, [self center].y - [self bounds].size.height / 2, [self bounds].size.width,[self bounds].size.height )];
}


- (CGSize)_constrainBounds:(CGRect)frame
{
    GLASS_LOG("GlassWindow _constrainBounds called");
    CGSize size = frame.size;

    CGSize constrained = CGSizeMake(frame.size.width, frame.size.height);
    {
        if (size.width < self->minWidth)
        {
            constrained.width = self->minWidth;
        }
        else if (size.width > self->maxWidth)
        {
            constrained.width = self->maxWidth;
        }
        if (size.height < self->minHeight)
        {
            constrained.height = self->minHeight;
        }
        else if (size.height > self->maxHeight)
        {
            constrained.height = self->maxHeight;
        }
    }
    return constrained;
}


- (void)_setMinimumSize
{
    GLASS_LOG("GlassWindow _setMinimumSize called. (w %f, h %f)",self->minWidth, self->minHeight);
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    CGSize currentSize = [self bounds].size;
    if (currentSize.width < self->minWidth || currentSize.height < self->minHeight) {
        [self _setBoundsAndPosition];
        [self _sendJavaWindowResizeEvent:com_sun_glass_events_WindowEvent_RESIZE forFrame:[self bounds]];
    }
}


- (void)_setMaximumSize
{
    GLASS_LOG("GlassWindow _setMaximumSize called. (w %f, h %f)",self->maxWidth, self->maxHeight);
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    CGSize currentSize = [self bounds].size;
    if (currentSize.width > self->maxWidth || currentSize.height > self->maxHeight) {
        [self _setBoundsAndPosition];
        [self _sendJavaWindowResizeEvent:com_sun_glass_events_WindowEvent_RESIZE forFrame:[self bounds]];
    }
}


- (void)_setLevel
{
    GLASS_LOG("GlassWindow _setLevel called.");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    UIWindowLevel level = UIWindowLevelNormal;
    switch (self->_setLevel)
    {
        case com_sun_glass_ui_Window_Level_FLOATING:
            level = UIWindowLevelStatusBar;
            break;
        case com_sun_glass_ui_Window_Level_TOPMOST:
            level = UIWindowLevelAlert;
            break;
    }
    //[self setWindowLevel:level];         // implemenation comes here
}


- (void)orderBack
{
    GLASS_LOG("GlassWindow orderBack");
    if ([[NSThread currentThread] isMainThread] == YES) {
        [self _orderBack];
    } else {
        [self performSelectorOnMainThread:@selector(_orderBack) withObject:nil waitUntilDone:YES];
    }
}


- (void)_orderBack
{
    GLASS_LOG("GlassWindow _orderBack");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    if ([self superview] != nil) {
        [[self superview] sendSubviewToBack:self];
    }
}


- (void) _orderFrontRegardless
{
    GLASS_LOG("GlassWindow _orderFrontRegardless ");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    if ([self superview] != nil) {
        [[self superview] bringSubviewToFront:self];
    }
}


- (void) orderFrontRegardless
{
    if ([[NSThread currentThread] isMainThread] == YES) {
        [self _orderFrontRegardless];
    } else {
        [self performSelectorOnMainThread:@selector(_orderFrontRegardless) withObject:nil waitUntilDone:YES];
    }
}


- (void)addChildWindow:(GlassWindow*)child
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    if (child != nil) {
        child->parentWindow = self;
        [self->childWindows addObject:child];

        [child _setBoundsAndPosition];
    }
}


- (void)removeChildWindow:(GlassWindow*)child
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    if (child != nil) {
        child->parentWindow = nil;
        [self->childWindows removeObject:child];
    }
}


+ (void)_resetGrab
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    if (s_grabWindow && [s_grabWindow isKindOfClass:[GlassWindow class]]) {
        GlassWindow * window = (GlassWindow*)s_grabWindow;
        [window _ungrabFocus];
    }
    s_grabWindow = nil; // unconditionally
}


- (void)_ungrabFocus
{
    if (s_grabWindow != self) {
        return;
    }

    GET_MAIN_JENV;
    (*env)->CallVoidMethod(env, self->jWindow, jWindowNotifyFocusUngrab);

    s_grabWindow = nil;
}


- (void)_checkUngrab
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    if (!s_grabWindow) {
        return;
    }

    // If this window doesn't belong to an owned windows hierarchy that
    // holds the grab currently, then the grab should be released.
    for (GlassWindow * window = self; window; window = window->parentWindow) {
        if (window == s_grabWindow) {
            return;
        }
    }

    [GlassWindow _resetGrab];
}


- (void)_grabFocus
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    if (s_grabWindow == self) {
        return;
    }

    [GlassWindow _resetGrab];
    s_grabWindow = self;
}


- (void)_setVisible
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    if (self->isEnabled == YES)
    {
        GLASS_LOG("making GlassWindow Visible %p",self);
        [self setHidden:NO];

        if (self->isFocusable == YES) {
            GLASS_LOG("making GlassWindow key %p",self);
            [self makeKeyWindow];
        }

        [self orderFrontRegardless];
    } else {
        [self orderFrontRegardless];
    }

    if ((self->owner != nil && self->parentWindow == nil))
    {
        [(GlassWindow *)self->owner addChildWindow:self];
    }
}


- (void)_sendJavaWindowMoveEventForFrame:(CGRect)frame
{
    if (self->suppressWindowMoveEvent == NO)
    {
        GET_MAIN_JENV;
        (*env)->CallVoidMethod(env, jWindow, mat_jWindowNotifyMove, (int)frame.origin.x,  (int)frame.origin.y);
    }
}


- (void)_sendJavaWindowResizeEvent:(int)type forFrame:(CGRect)frame
{
    if (self->suppressWindowResizeEvent == NO)
    {
        GET_MAIN_JENV;
        (*env)->CallVoidMethod(env, jWindow, mat_jWindowNotifyResize, type, (int)frame.size.width, (int)frame.size.height);
    }
}


#pragma mark --- UIView

- (void) setBackgroundColor:(UIColor *)color
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    if (self->isTransparent == NO)
    {
        // allow color if we're opaque
        [super setBackgroundColor:color];
    }
    else
    {
        // for transparent window, ignore and set to clear color
        // do we want to store the background color in case we switch to non-transparent mode?
        [super setBackgroundColor:[UIColor clearColor]];
    }
}

- (void) makeKeyWindow
{
    if (self->isEnabled && self->isFocusable && focusOwner != self) {

        [focusOwner resignKeyWindow];

        [self becomeKeyWindow];
    }
}

- (BOOL) isKeyWindow
{
    return self == focusOwner;
}

- (void)becomeKeyWindow
{
    GLASS_LOG("Window did become key");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );


    GET_MAIN_JENV;
    if (!self->isEnabled)
    {
        (*env)->CallVoidMethod(env, self->jWindow, mat_jWindowNotifyFocusDisabled);
        return;
    }

    focusOwner = self;

    (*env)->CallVoidMethod(env, self->jWindow, mat_jWindowNotifyFocus, com_sun_glass_events_WindowEvent_FOCUS_GAINED);
}


- (void)resignKeyWindow
{
    GLASS_LOG("Window did resign key");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );

    if (focusOwner == self) {
        focusOwner = nil;
    }

    [self _ungrabFocus];

    GET_MAIN_JENV;
    (*env)->CallVoidMethod(env, self->jWindow, mat_jWindowNotifyFocus, com_sun_glass_events_WindowEvent_FOCUS_LOST);
}


- (void)windowWillClose
{
    GLASS_LOG("GlassWindow windowWillClose");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    // Unparent self
    if (self->parentWindow != nil)
    {
        [self->parentWindow removeChildWindow:self];
    }

    // Call the notification method
    GET_MAIN_JENV;
    (*env)->CallVoidMethod(env, self->jWindow, mat_jWindowNotifyDestroy);

    if (childWindows != NULL) {

        // Finally, close owned windows to mimic MS Windows behavior
        for (GlassWindow * child in self->childWindows)
        {
            [child close];
        }

        [childWindows release];
    }

    [self->hostView removeFromSuperview];
    [self->hostView release];

    if ([self superview] != nil) {
        [self removeFromSuperview];
    }

    if (focusOwner == self) {
        focusOwner = nil;
    }

    (*jEnv)->DeleteGlobalRef(jEnv, self->jWindow);
    GLASS_CHECK_EXCEPTION(jEnv);

    self->jWindow = NULL;
}


- (void) requestInput:(NSString *)text type:(int)type width:(double)width height:(double)height
                  mxx:(double)mxx mxy:(double)mxy mxz:(double)mxz mxt:(double)mxt
                  myx:(double)myx myy:(double)myy myz:(double)myz myt:(double)myt
                  mzx:(double)mzx mzy:(double)mzy mzz:(double)mzz mzt:(double)mzt

{
    [view requestInput:text type:type width:width height:height
                   mxx:mxx mxy:mxy mxz:mxz mxt:mxt
                   myx:myx myy:myy myz:myz myt:myt
                   mzx:mzx mzy:mzy mzz:mzz mzt:mzt];
}


- (void) releaseInput
{
    [view releaseInput];
}

@end



jlong _1createWindow(JNIEnv *env, jobject jWindow, jlong jOwnerPtr, jlong jScreenPtr, jint jStyleMask)
{
    [[NSThread currentThread] isMainThread];
    if ([[NSThread currentThread] isMainThread] == NO) NSLog(@"[1] must be on main thread");
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    GlassWindow *window;

    {
        UIScreen *screen = (UIScreen*)jlong_to_ptr(jScreenPtr);
        BOOL hidden = YES;
        if (jOwnerPtr == 0L) {
            // no owner means it is the primary stage; Decorated primary stage shows status bar by default
            hidden = ((jStyleMask & com_sun_glass_ui_Window_TITLED) == 0);

            NSObject * values = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UIStatusBarHidden"];
            //we prefer explicit settings from .plist
            if (values != nil) {
                hidden = (values == @(YES))?YES:NO;
            }

            [UIApplication sharedApplication].statusBarHidden = hidden;
        }


        if (mainWindow == nil) {
            //We have to remove rootViewController of splashscreen UIWindow in order to avoid
            //StatusBar orientation change ...
            UIWindow *splashScreen = [[UIApplication sharedApplication] keyWindow];
            splashScreen.rootViewController = nil;

            GLASS_LOG("SCREEN: %@", screen);
            CGRect applicationFrame = [screen bounds];
            GLASS_LOG("FRAME: %@", applicationFrame);

            mainWindow = [[GlassMainWindow alloc] initWithFrame:applicationFrame];
            mainWindowHost = [[GlassMainView alloc] initWithFrame:CGRectMake(0.0, 0.0, applicationFrame.size.width, applicationFrame.size.height)];

            // Set GlassViewController - responsible for orientation change, etc.
            GlassViewController *rvc = [[GlassViewController alloc] init];
            [rvc setView:mainWindowHost];
            [mainWindow setRootViewController:rvc];
            [rvc release];

            [mainWindow setHidden:NO];
            [mainWindowHost setHidden:NO];
        } else {
            mainWindow = [mainWindow retain];//increase retain count per each GlassWindow
            mainWindowHost = [mainWindowHost retain];
        }

        [mainWindow setAutoresizesSubviews:YES];
        [mainWindowHost setAutoresizesSubviews:NO];

        [mainWindow makeKeyWindow];

        GLASS_LOG("GlassWindow _1createWindow");
        window = [[GlassWindow alloc] initWithScreen:screen jwindow:jWindow];

        window->isResizable = NO;

        window->hostView = [[UIView alloc] init];
        [window->hostView setAutoresizesSubviews:NO];

        [window addSubview:window->hostView];

        window.backgroundColor = [UIColor whiteColor];

        if ((jStyleMask & com_sun_glass_ui_Window_TRANSPARENT) != 0)
        {
            [window _setTransparent:YES];
        }
        else
        {
            [window _setTransparent:NO];
        }

        [mainWindowHost addSubview:window];

        if (jOwnerPtr != 0L)
        {
            GLASS_LOG("Adding %p window as usbview of owner window %lld", window, jOwnerPtr);
            window->owner = (UIWindow*)jlong_to_ptr(jOwnerPtr);
        } else {
            NSArray *views = [mainWindowHost subviews];
            // if there exists any secondary stage, its owner is primary stage internally if
            // not set explicitly
            if ([views count] > 1) {
                window->owner = [views objectAtIndex:0];
            }
        }
    }
    [pool drain];

    GLASS_CHECK_EXCEPTION(env);

    return ptr_to_jlong(window);
}



@interface GlassWindowDispatcher : NSObject
{
@public
    jobject     jWindow;
    jlong       jOwnerPtr;
    jlong       jScreenPtr;
    jint        jStyleMask;
    jlong       jlongReturn;
}
@end



@implementation GlassWindowDispatcher

- (void) _createWindow
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread");
    GET_MAIN_JENV;
    self->jlongReturn = _1createWindow(env, self->jWindow, self->jOwnerPtr, self->jScreenPtr, self->jStyleMask);
}

@end



/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _createWindow
 * Signature: (JJZI)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosWindow__1createWindow
(JNIEnv *env, jobject jwindow, jlong jownerPtr, jlong jscreenPtr, jint jstyleMask)
{
    jlong value;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1createWindow");
        jobject jWindowRef = (*env)->NewGlobalRef(env, jwindow);
        if ([[NSThread currentThread] isMainThread] == YES)
        {
            value = _1createWindow(env, jWindowRef, jownerPtr, jscreenPtr, jstyleMask);
            GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1createWindow  from NSMainThread called");

        }
        else
        {
            GlassWindowDispatcher *dispatcher = [[GlassWindowDispatcher alloc] autorelease];
            dispatcher->jWindow = jWindowRef;
            dispatcher->jOwnerPtr = jownerPtr;
            dispatcher->jScreenPtr = jscreenPtr;
            dispatcher->jStyleMask = jstyleMask;
            [dispatcher performSelectorOnMainThread:@selector(_createWindow) withObject:dispatcher waitUntilDone:YES];
            value = dispatcher->jlongReturn;
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return value;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _createChildWindow
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosWindow__1createChildWindow
(JNIEnv *env, jobject jwindow, jlong parent) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1createChildWindow");
    // implementation comes here
    return 0L;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _close
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1close
(JNIEnv *env, jclass jwindow, jlong ptr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1close");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = (GlassWindow*)jlong_to_ptr(ptr);
        if ([[NSThread currentThread] isMainThread] == YES)
        {
            // this call will always close the window
            // without calling the windowShouldClose
            [window close];
            [window release];
        }
        else
        {
            // this call will always close the window
            // without calling the windowShouldClose
            [window performSelectorOnMainThread:@selector(close) withObject:nil waitUntilDone:YES];
            [window performSelectorOnMainThread:@selector(release) withObject:nil waitUntilDone:YES];
        }
        // The window is released here since we retain it - different from Mac OS X
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setView
 * Signature: (JLcom/sun/glass/ui/View;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setView
(JNIEnv *env, jobject jwindow, jlong windowPtr, jobject jview) {

    if ([[NSThread currentThread] isMainThread] == NO) NSLog(@"[2] must be on main thread");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setView");
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);

        window->view = getiOSView(env, jview);


        GLASS_LOG("window: %@", window);
        GLASS_LOG("frame: %.2f,%.2f %.2fx%.2f", [window frame].origin.x, [window frame].origin.y, [window frame].size.width, [window frame].size.height);
        GLASS_LOG("view: %@", window->view);
        GLASS_LOG("frame: %.2f,%.2f %.2fx%.2f", [window->view frame].origin.x, [window->view frame].origin.y, [window->view frame].size.width, [window->view frame].size.height);

        if (window->view != nil)
        {
            window->suppressWindowMoveEvent = YES; // RT-11215
            {
                CGRect viewFrame = [window->view bounds];
                if ((viewFrame.size.width != 0.0f) && (viewFrame.size.height != 0.0f))
                {
                    CGRect windowFrame = CGRectMake(0.0, 0.0, viewFrame.size.width, viewFrame.size.height);
                    windowFrame.origin.x = [window center].x - viewFrame.size.width / 2 ;
                    windowFrame.origin.y = [window center].y - viewFrame.size.height / 2;
                    setWindowFrame(window, windowFrame.origin.x, windowFrame.origin.y, windowFrame.size.width, windowFrame.size.height, JNI_TRUE, JNI_FALSE);
                }

                if ([[NSThread currentThread] isMainThread] == YES)
                {
                    [window->hostView addSubview: window->view];
                }
                else
                {
                    [window->hostView performSelectorOnMainThread:@selector(addSubview:) withObject:window->view waitUntilDone:YES];
                }
            }
            window->suppressWindowMoveEvent = NO;
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setMenubar
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setMenubar
(JNIEnv *env, jobject jwindow, jlong windowPtr, jlong menubarPtr) {

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setMenubar - setMenuBar called.");
        // implementation comes here
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _minimize
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1minimize
(JNIEnv *env, jobject jwindow, jlong windowPtr, jboolean minimize) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1minimize called.");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        // implementation comes here
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _maximize
 * Signature: (JZZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1maximize
(JNIEnv *env, jobject jwindow, jlong windowPtr, jboolean maximize, jboolean wasMaximized) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1maximize called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        // implementation comes here
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setBoundsAndPosition
 * Signature: (JIIZZIIIIFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setBounds
(JNIEnv *env, jobject jWindow, jlong jPtr,
 jint x, jint y, jboolean xSet, jboolean ySet,
 jint w, jint h, jint cw, jint ch, jfloat xGravity, jfloat yGravity)
{
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setBounds");
    if ([[NSThread currentThread] isMainThread] == NO) NSLog(@"[3] must be on main thread");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GLASS_LOG("Called setBounds with x %ld,y %ld,xSet %d,ySet %d,w %ld,h %ld,cw %ld,ch %ld",x,y,xSet,ySet,w,h,cw,ch);

        GlassWindow *window = (GlassWindow *)jlong_to_ptr(jPtr);

        CGPoint origin = CGPointMake([window center].x - [window bounds].size.width / 2, [window center].y - [window bounds].size.height / 2);
        GLASS_LOG("window original position x,y %f, %f",origin.x,origin.y);

        CGSize size = [window bounds].size;
        GLASS_LOG("window size w,h %f,%f",size.width,size.height);

        CGSize sizeForClient = CGRectMake(0, 0, cw > 0 ? cw : 0, ch > 0 ? ch : 0).size;
        GLASS_LOG("sizeForClient %f, %f", sizeForClient.width, sizeForClient.height);

        CGFloat newX = xSet == JNI_TRUE ? x : origin.x;
        CGFloat newY = ySet == JNI_TRUE ? y : origin.y;
        CGFloat newW = (w > 0) ? w :
        (cw > 0) ? sizeForClient.width : size.width;
        CGFloat newH = (h > 0) ? h :
        (ch > 0) ? sizeForClient.height : size.height;
        GLASS_LOG("FRAME: x,y,w,h - %f, %f, %f %f",newX, newY, newW, newH);

        setWindowFrame(window, newX, newY, newW, newH, JNI_TRUE, JNI_FALSE);

        //Let's notify JavaFX about move,size change (as we don't have window's size,position) Notifications on iOS
        if(xSet == JNI_TRUE || ySet == JNI_TRUE) {
            [window _sendJavaWindowMoveEventForFrame:CGRectMake([window center].x - [window bounds].size.width / 2, [window center].y - [window bounds].size.height / 2, [window bounds].size.width,[window bounds].size.height )];
        } else {
            [window _sendJavaWindowResizeEvent:com_sun_glass_events_WindowEvent_RESIZE forFrame:[window bounds]];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setVisible
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setVisible
(JNIEnv *env, jobject jwindow, jlong windowPtr, jboolean visible) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setVisible called.");
    jboolean now;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);
        if (visible == JNI_TRUE)
        {
            if ([[NSThread currentThread] isMainThread] == YES)
            {
                [window _setVisible];
            }
            else
            {
                [window performSelectorOnMainThread:@selector(_setVisible) withObject:nil waitUntilDone:YES];
            }
        }
        else
        {
            if ([[NSThread currentThread] isMainThread] == YES)
            {
                [window _ungrabFocus];
                if (window->owner != nil)
                {
                    [(GlassWindow *)window->owner removeChildWindow: window];
                }
                //[window orderOut:window];
            }
            else
            {
                [window performSelectorOnMainThread:@selector(_ungrabFocus) withObject:nil waitUntilDone:YES];
                if (window->owner != nil)
                {
                    [(GlassWindow *)(window->owner) performSelectorOnMainThread:@selector(removeChildWindow:) withObject:window waitUntilDone:YES];
                }
                //[window performSelectorOnMainThread:@selector(orderOut:) withObject:window waitUntilDone:YES];
            }
        }
        if ([[NSThread currentThread] isMainThread] == NO) NSLog(@"[4] must be on main thread");
        now = ([window isHidden] == NO) ? JNI_TRUE : JNI_FALSE;
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return now;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _requestFocus
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1requestFocus
(JNIEnv *env, jobject jwindow, jlong windowPtr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1requestFocus  called.");
    jboolean focused;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);
        if (window->isFocusable == YES) {
            if ([[NSThread currentThread] isMainThread] == YES)
            {
                [window makeKeyWindow];//for iOS
                [window orderFrontRegardless];
            }
            else
            {
                [window performSelectorOnMainThread:@selector(makeKeyWindow) withObject:nil waitUntilDone:YES];
                [window performSelectorOnMainThread:@selector(orderFrontRegardless) withObject:window waitUntilDone:YES];
            }
        }
        if ([[NSThread currentThread] isMainThread] == NO) NSLog(@"[5] must be on main thread");
        focused = [window isKeyWindow] ? JNI_TRUE : JNI_FALSE;
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return focused;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _grabFocus
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1grabFocus
(JNIEnv *env, jobject jwindow, jlong windowPtr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1grabFocus");
    jboolean ret;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow * window = getGlassWindow(env, windowPtr);
        if ([[NSThread currentThread] isMainThread] == YES)
        {
            [window _grabFocus];
        }
        else
        {
            [window performSelectorOnMainThread:@selector(_grabFocus) withObject:nil waitUntilDone:YES];
        }
        ret = JNI_TRUE;
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return ret;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _ungrabFocus
 * Signature: (J)
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1ungrabFocus
(JNIEnv *env, jobject jwindow, jlong windowPtr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1ungrabFocus");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow * window = getGlassWindow(env, windowPtr);

        if ([[NSThread currentThread] isMainThread] == YES)
        {
            [window _ungrabFocus];
        } else {
            [window performSelectorOnMainThread:@selector(_ungrabFocus) withObject:nil waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setTitle
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setTitle
(JNIEnv *env, jobject jwindow, jlong windowPtr, jstring title) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setTitle called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        // implementation comes here when feature is requested on iOS
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setLevel
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setLevel
(JNIEnv *env, jobject jwindow, jlong windowPtr, jint level) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setLevel called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);
        window->_setLevel = level;

        if ([[NSThread currentThread] isMainThread] == YES)
        {
            [window _setLevel];
        }
        else
        {
            [window performSelectorOnMainThread:@selector(setLevel) withObject:nil waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setResizable
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setResizable
(JNIEnv *env, jobject jwindow, jlong windowPtr, jboolean resizeable) {
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setResizable called.");
        GlassWindow *window = getGlassWindow(env, windowPtr);
        if (window->isResizable != resizeable)
        {
            window->isResizable = resizeable;
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setFocusable
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setFocusable
(JNIEnv *env, jobject jwindow, jlong windowPtr, jboolean isFocusable) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setFocusable called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);
        window->isFocusable = isFocusable;
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setAlpha
 * Signature: (JF)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setAlpha
(JNIEnv *env, jobject jwindow, jlong windowPtr, jfloat alpha) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setAlpha(%f)",alpha);
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);
        window->_setAlpha = alpha;

        if ([[NSThread currentThread] isMainThread] == YES)
        {
            [window _setAlpha];
        }
        else
        {
            [window performSelectorOnMainThread:@selector(_setAlpha) withObject:nil waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setBackground
 * Signature: (JFFF)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setBackground
(JNIEnv *env, jobject jwindow, jlong windowPtr, jfloat r, jfloat g, jfloat b) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setBackground");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);
        if ([[NSThread currentThread] isMainThread] == YES)
        {
            [window setBackgroundColor:[UIColor colorWithRed:r green:g blue:b alpha:1.0f]];
        }
        else
        {
            [window performSelectorOnMainThread:@selector(setBackgroundColor:) withObject:[UIColor colorWithRed:r green:g blue:b alpha:1.0f] waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setMinimumSize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setMinimumSize
(JNIEnv *env, jobject jwindow, jlong windowPtr, jint width, jint height) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setMinimumSize called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);
        window->minWidth = (jfloat) width;
        window->minHeight = (jfloat) height;

        if ([[NSThread currentThread] isMainThread] == YES)
        {
            [window _setMinimumSize];
        }
        else
        {
            [window performSelectorOnMainThread:@selector(_setMinimumSize) withObject:nil waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setMaximumSize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setMaximumSize
(JNIEnv *env, jobject jwindow, jlong windowPtr, jint width, jint height) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setMaximumSize called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);
        window->maxWidth = (jfloat)(width >= 0 ? width : CGFLOAT_MAX);
        window->maxHeight = (jfloat)(height >= 0 ? height : CGFLOAT_MAX);

        if ([[NSThread currentThread] isMainThread] == YES)
        {
            [window _setMaximumSize];
        }
        else
        {
            [window performSelectorOnMainThread:@selector(_setMaximumSize) withObject:nil waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return JNI_TRUE;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setIcon
 * Signature: (JIILjava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setIcon
(JNIEnv *env, jobject jwindow, jlong windowPtr, jobject pixels) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setIcon called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        // implemenation comes here
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _toFront
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1toFront
(JNIEnv *env, jobject jwindow, jlong windowPtr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1toFront called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);
        [window orderFrontRegardless];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _toBack
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1toBack
(JNIEnv *env, jobject jwindow, jlong windowPtr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1toBack called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, windowPtr);
        [window orderBack];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _enterModal
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1enterModal
(JNIEnv *env, jobject jwindow, jlong windowPtr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1enterModal called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        // implementation omes here
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _enterModalWithWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1enterModalWithWindow
(JNIEnv *env, jobject jwindow, jlong windowPtr, jlong window) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1enterModalWithWindow called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        // implemenation comes here
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _exitModal
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1exitModal
(JNIEnv *env, jobject jwindow, jlong windowPtr) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1exitModal called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        // implementation comes here
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _setEnabled
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1setEnabled
(JNIEnv *env, jobject jwindow, jlong windowPtr, jboolean enabled) {
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosWindow__1setEnabled called.");
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;

    GlassWindow *window = (GlassWindow*)jlong_to_ptr(windowPtr);
    [window setEnabled:(BOOL)enabled];

    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _requestInput
 * Signature: (JLjava/lang/String;IDDDDDDDDDDDDDD)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1requestInput
(JNIEnv *env, jobject jwin, jlong ptr, jstring text, jint type, jdouble width, jdouble height,
    jdouble mxx, jdouble mxy, jdouble mxz, jdouble mxt,
    jdouble myx, jdouble myy, jdouble myz, jdouble myt,
    jdouble mzx, jdouble mzy, jdouble mzz, jdouble mzt)
{
    fprintf(stderr, "We should never be here!\n");
    return;
}


/*
 * Class:     com_sun_glass_ui_ios_IosWindow
 * Method:    _releaseInput
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosWindow__1releaseInput (JNIEnv *env, jobject jwin, jlong ptr)
{
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;

    GlassWindow *window = getGlassWindow(env, ptr);
    [window releaseInput];

    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}
