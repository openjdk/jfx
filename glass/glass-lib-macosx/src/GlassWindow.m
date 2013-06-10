/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
#import "GlassEmbeddedWindow+Overrides.h"
#import "GlassView.h"
#import "GlassScreen.h"
#import "GlassTouches.h"
#import "GlassApplication.h"
#import "GlassLayer3D.h"
#import "GlassAccessibleRoot.h"
#import "GlassHelper.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

#define BROWSER_PARENT_ID -1L

#pragma mark --- Internal utilities

static inline GlassWindow *getGlassWindow(JNIEnv *env, jlong jPtr)
{
    if (jPtr != 0L)
    {
        NSWindow * nsWindow = (NSWindow*)jlong_to_ptr(jPtr);
        return (GlassWindow*)[nsWindow delegate];
    }
    else
    {
        return nil;
    }
}

static inline NSView<GlassView> *getMacView(JNIEnv *env, jobject jview)
{
    if (jview != NULL)
    {
        return (NSView<GlassView>*)jlong_to_ptr((*env)->GetLongField(env, jview, (*env)->GetFieldID(env, jViewClass, "ptr", "J")));
    }
    else
    {
        return nil;
    }
}

// --------------------------------------------------------------------------------------
// NSWindow/NSPanel descendants implementation
#define GLASS_NS_WINDOW_IMPLEMENTATION                                                  \
- (id)initWithDelegate:(GlassWindow*)delegate                                           \
             frameRect:(NSRect)rect                                                     \
             styleMask:(NSUInteger)styleMask                                            \
                screen:(NSScreen*)screen                                                \
{                                                                                       \
    self->gWindow = delegate; /* must be done before calling [super init...] */         \
    self = [super initWithContentRect:rect                                              \
                            styleMask:styleMask                                         \
                              backing:NSBackingStoreBuffered                            \
                                defer:NO                                                \
                               screen:screen];                                          \
                                                                                        \
    if (self == nil) {                                                                  \
        return nil;                                                                     \
    }                                                                                   \
                                                                                        \
    [self setDelegate:delegate];                                                        \
    [self setAcceptsMouseMovedEvents:NO];                                               \
    [self setShowsResizeIndicator:NO];                                                  \
    [self setAllowsConcurrentViewDrawing:YES];                                          \
                                                                                        \
    [self setReleasedWhenClosed:YES];                                                   \
                                                                                        \
    return self;                                                                        \
}                                                                                       \
                                                                                        \
- (void)close                                                                           \
{                                                                                       \
    self->gWindow->isClosed = YES;                                                      \
    [self->gWindow close];                                                              \
    [super close];                                                                      \
}                                                                                       \
/* super calls NSWindow on the next run-loop pass when NSWindow could be released */    \
- (BOOL)performKeyEquivalent:(NSEvent *)theEvent                                        \
{                                                                                       \
    [self retain];                                                                      \
    BOOL result = [super performKeyEquivalent:theEvent];                                \
    result = result || self->gWindow->isClosed;                                         \
    [self release];                                                                     \
    return result;                                                                      \
}                                                                                       \
- (BOOL)canBecomeMainWindow                                                             \
{                                                                                       \
    return [self->gWindow canBecomeMainWindow];                                         \
}                                                                                       \
- (BOOL)canBecomeKeyWindow                                                              \
{                                                                                       \
    return [self->gWindow canBecomeKeyWindow];                                          \
}                                                                                       \
- (void)setHidesOnDeactivate:(BOOL)hideOnDeactivate                                     \
{                                                                                       \
    [super setHidesOnDeactivate:NO];                                                    \
}                                                                                       \
- (BOOL)hidesOnDeactivate                                                               \
{                                                                                       \
    return [self->gWindow hidesOnDeactivate];                                           \
}                                                                                       \
- (BOOL)worksWhenModal                                                                  \
{                                                                                       \
    return [self->gWindow worksWhenModal];                                              \
}                                                                                       \
- (void)setBackgroundColor:(NSColor *)color                                             \
{                                                                                       \
    [super setBackgroundColor:[self->gWindow setBackgroundColor:color]];                \
}                                                                                       \
- (NSButton *)standardWindowButton:(NSWindowButton)type                                 \
{                                                                                       \
    NSButton* button = [super standardWindowButton:type];                               \
    switch (type)                                                                       \
    {                                                                                   \
        case NSWindowDocumentIconButton:                                                \
            [button setAcceptsTouchEvents:NO];                                          \
            [button setAction:nil];                                                     \
            [button setEnabled:NO];                                                     \
            break;                                                                      \
    }                                                                                   \
    return button;                                                                      \
}                                                                                       \
- (void)sendEvent:(NSEvent *)event                                                      \
{                                                                                       \
    [self->gWindow sendEvent:event];                                                    \
    [super sendEvent:event];                                                            \
}                                                                                       \
- (NSArray *)accessibilityAttributeNames                                                \
{                                                                                       \
    if( !self->gWindow->isAccessibleInitComplete )                                      \
    {                                                                                   \
        [self->gWindow _initAccessibility];                                             \
    }                                                                                   \
    return [[ [super accessibilityAttributeNames]                                       \
              arrayByAddingObject:NSAccessibilityFocusedUIElementAttribute ] retain];   \
}                                                                                       \
- (id)accessibilityAttributeValue:(NSString *)attribute                                 \
{                                                                                       \
    if ([attribute isEqualToString:NSAccessibilityChildrenAttribute]) {                 \
        return self->gWindow->accChildren; /* return array with one child, the root */  \
    } else if([attribute isEqualToString:NSAccessibilityFocusedUIElementAttribute]) {   \
        LOG( "GlassWindow:accessibilityAttributeValue %p",                              \
             self->gWindow->accFocusElement );                                          \
        return self->gWindow->accFocusElement;                                          \
    } else {                                                                            \
        id idFromSuper = [super accessibilityAttributeValue:attribute];                 \
        return idFromSuper;                                                             \
    }                                                                                   \
}                                                                                       \
- (BOOL)accessibilityIsIgnored                                                          \
{                                                                                       \
    return NO;                                                                          \
}                                                                                       \
- (id)accessibilityHitTest:(NSPoint)point                                               \
{                                                                                       \
    LOG("GlassWindow:accessibilityHitTest:point");                                      \
    return [[self->gWindow->accChildren objectAtIndex:0] accessibilityHitTest:point];   \
}                                                                                       \
-(void)accessibilityPostEvent:(NSString*)event                                          \
                              focusElement:(GlassAccessibleBaseProvider*)focusElement   \
{                                                                                       \
    LOG("GlassWindow:accessibilityPostEvent %s", [event UTF8String]);                   \
    self->gWindow->accFocusElement = focusElement;                                      \
    NSAccessibilityPostNotification(self, event);                                       \
}
//return self->gWindow->accChildren self ;   return NSAccessibilityUnignoredAncestor(self);


@implementation GlassWindow_Normal
GLASS_NS_WINDOW_IMPLEMENTATION
@end

@implementation GlassWindow_Panel
GLASS_NS_WINDOW_IMPLEMENTATION

- (void)setWorksWhenModal:(BOOL)worksWhenModal
{
    [super setWorksWhenModal:NO];
}

@end
// --------------------------------------------------------------------------------------


@implementation GlassWindow

-(void) accessibilityPostEvent:(NSString*)event
                               focusElement:(GlassAccessibleBaseProvider*)focusElement
{
    LOG("GlassWindow:Base:accessibilityPostEvent NSAccessibilityFocusedUIElementAttribute");
    // [self->nsWindow accessibilityPostEvent];
    // NSAccessibilityPostNotification(self->nsWindow, NSAccessibilityFocusedUIElementAttribute);
}


- (void)setFullscreenWindow:(NSWindow*)fsWindow
{
    if (self->fullscreenWindow == fsWindow) {
        return;
    }
    
    [self _ungrabFocus];
    
    NSWindow *from, *to;
    from = self->fullscreenWindow ? self->fullscreenWindow : self->nsWindow;
    to = fsWindow ? fsWindow : self->nsWindow;
    
    NSArray * children = [from childWindows];
    for (NSUInteger i=0; i<[children count]; i++)
    {
        NSWindow *child = (NSWindow*)[children objectAtIndex:i];
        if ([[child delegate] isKindOfClass: [GlassWindow class]]) {
            [from removeChildWindow: child];
            [to addChildWindow:child ordered:NSWindowAbove];
        }
    }
    
    self->fullscreenWindow = fsWindow;
    
    GET_MAIN_JENV;
    (*env)->CallVoidMethod(env, self->jWindow, jWindowNotifyDelegatePtr, ptr_to_jlong(fsWindow));
    GLASS_CHECK_EXCEPTION(env);
}

- (void)close
{
    [self _ungrabFocus];
}

- (void)sendEvent:(NSEvent *)event
{
    if ([event type] == NSLeftMouseDown || [event type] == NSRightMouseDown || [event type] == NSOtherMouseDown)
    {
        NSPoint p = [NSEvent mouseLocation];
        NSRect frame = [self->nsWindow frame];
        NSRect contentRect = [self->nsWindow contentRectForFrameRect:frame];
        
        if (p.y >= (frame.origin.y + contentRect.size.height))
        {
            // Click to the titlebar
            [self _ungrabFocus];
        }
        
        [self _checkUngrab];
    }
}

// Window vs Panel API
- (BOOL)canBecomeMainWindow
{
    if (!self->isEnabled)
    {
        // We'll send FOCUS_DISABLED
        return YES;
    }
    return self->isFocusable;
}

// Window vs Panel API
- (BOOL)hidesOnDeactivate
{
    return NO;
}

// Window vs Panel API
- (BOOL)worksWhenModal
{
    return NO;
}

- (BOOL)canBecomeKeyWindow
{
    if (!self->isEnabled)
    {
        // We'll send FOCUS_DISABLED
        return YES;
    }
    return self->isFocusable;
}

- (NSColor*)setBackgroundColor:(NSColor *)color
{
    if (self->isTransparent == NO)
    {
        // allow color if we're opaque
        return color;
    }
    else
    {
        // for transparent window, ignore and set to clear color
        // FIXME: do we want to store the background color in case we switch to non-transparent mode?
        return [NSColor clearColor];
    }
}

- (void)setAccessibilityInitIsComplete:(GlassAccessibleRoot *)acc
{
    // TODO: When should the retained reference be released?
    accChildren = [[NSArray arrayWithObject:acc] retain];
    isAccessibleInitComplete = YES;  // set the flag so init isn't called again
    acc->parent = nsWindow;  // this is the parent of the root
}

@end

#pragma mark --- GlassEmbeddedWindow

static NSMutableArray * embeddedWindowsList = nil;

@implementation GlassEmbeddedWindow

- (id)initWithDelegate:(GlassWindow*)delegate
             frameRect:(NSRect)rect
             styleMask:(NSUInteger)styleMask
                screen:(NSScreen*)screen
{
    self = [super initWithDelegate:delegate frameRect:rect styleMask:styleMask screen:screen];
    if (self == nil) {
        return nil;
    }

    if (embeddedWindowsList == nil) {
        embeddedWindowsList = [[NSMutableArray alloc] initWithCapacity: 4];
    }
    [embeddedWindowsList addObject:self]; // retains 'self'

    return self;
}

- (void)close
{
    if (embeddedWindowsList) {
        [embeddedWindowsList removeObject:self]; // releases 'self'
        if ([embeddedWindowsList count] == 0) {
            [embeddedWindowsList release];
            embeddedWindowsList = nil;
        }
    }
    [super close];
}

+ (BOOL)exists:(GlassEmbeddedWindow*)window
{
    if (embeddedWindowsList && window) {
        return [embeddedWindowsList indexOfObjectIdenticalTo:window] != NSNotFound;
    }
    return NO;
}

- (void)setFullscreenWindow:(NSWindow*)fsWindow
{
    if (self->parent != nil)
    {
        BOOL fullscreen = (fsWindow != nil);
        
        CALayer *layer = [self->gWindow->view layer];
        if ([layer isKindOfClass:[GlassLayer3D class]] == YES)
        {
            [((CAOpenGLLayer*)layer) setAsynchronous:fullscreen];
            
            layer = [self->parent->gWindow->view layer];
            if ([layer isKindOfClass:[GlassLayer3D class]] == YES)
            {
                [((CAOpenGLLayer*)layer) setAsynchronous:!fullscreen];
            }
        }
        
        self->fullscreenWindow = fsWindow;
    }
}

- (void)sendEvent:(NSEvent *)theEvent
{
    BOOL fullscreen = (self->fullscreenWindow != nil);
    if (fullscreen == NO)
    {
        [super sendEvent:theEvent];
    }
    else
    {
        [self->fullscreenWindow sendEvent:theEvent];
    }
}


@end

#pragma mark --- Dispatcher

// TODO: re-implement using Obj-C blocks ?
static jlong _createWindowCommonDo(JNIEnv *env, jobject jWindow, jlong jOwnerPtr, jlong jScreenPtr, jint jStyleMask, jboolean jIsChild)
{
    GlassWindow *window = nil;
    
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        NSUInteger styleMask = NSBorderlessWindowMask;
        // only titled windows get title
        if ((jStyleMask&com_sun_glass_ui_Window_TITLED) != 0)
        {
            styleMask = styleMask|NSTitledWindowMask;
        }
        // only nontransparent windows get decorations
        if ((jStyleMask&com_sun_glass_ui_Window_TRANSPARENT) == 0)
        {
            if ((jStyleMask&com_sun_glass_ui_Window_CLOSABLE) != 0)
            {
                styleMask = styleMask|NSClosableWindowMask;
            }
            
            if (((jStyleMask&com_sun_glass_ui_Window_MINIMIZABLE) != 0) || ((jStyleMask&com_sun_glass_ui_Window_MAXIMIZABLE) != 0))
            {
                // on Mac OS X there is one set for min/max buttons, so if clients requests either one, we turn them both on
                styleMask = styleMask|NSMiniaturizableWindowMask;
            }
            
            if ((jStyleMask&com_sun_glass_ui_Window_UNIFIED) != 0) {
                styleMask = styleMask|NSTexturedBackgroundWindowMask;
            }
            
            if ((jStyleMask&com_sun_glass_ui_Window_UTILITY) != 0)
            {
                styleMask = styleMask|NSUtilityWindowMask;
            }
        }

        if ((jStyleMask&com_sun_glass_ui_Window_POPUP) != 0)
        {
            // can receive keyboard input without activating the owning application
            styleMask = styleMask|NSNonactivatingPanelMask;
        }

        // initial size must be 0x0 otherwise we don't get resize update if the initial size happens to be the exact same size as the later programatical one!
        CGFloat x = 0.0f;
        CGFloat y = 0.0f;
        CGFloat w = 0.0f;
        CGFloat h = 0.0f;
        
        NSScreen *screen = (NSScreen*)jlong_to_ptr(jScreenPtr);
        window = [[GlassWindow alloc] _initWithContentRect:NSMakeRect(x, y, w, h) styleMask:styleMask screen:screen jwindow:jWindow jIsChild:jIsChild];
        
        if ((jStyleMask&com_sun_glass_ui_Window_UNIFIED) != 0) {
            //Prevent the textured effect from disappearing on border thickness recalculation
            [window->nsWindow setAutorecalculatesContentBorderThickness:NO forEdge:NSMaxYEdge];
        }

        if (jIsChild == JNI_FALSE)
        {
            if (jOwnerPtr != 0L)
            {
                window->owner = getGlassWindow(env, jOwnerPtr)->nsWindow; // not retained (use weak reference?)
            }
        }
        else
        {
            if ((jOwnerPtr != 0L) && (jOwnerPtr != BROWSER_PARENT_ID))
            {
                GlassEmbeddedWindow *parent = getGlassEmbeddedWindow(env, jOwnerPtr);
                GlassEmbeddedWindow *ewindow = (GlassEmbeddedWindow*)window->nsWindow;
                parent->child = ewindow; // not retained (use weak reference?)
                
                ewindow->parent = parent; // not retained (use weak reference?)
            }
        }
        window->isResizable = NO;
        window->isDecorated = (jStyleMask&com_sun_glass_ui_Window_TITLED) != 0;
        /* 10.7 full screen window support */ 
        if ([NSWindow instancesRespondToSelector:@selector(toggleFullScreen:)]) {
            NSWindowCollectionBehavior behavior = [window->nsWindow collectionBehavior];
            if (window->isDecorated && !window->owner)
            {
                // Only titled ownerless windows should have the Full Screen Toggle control
                behavior |= (1 << 7) /* NSWindowCollectionBehaviorFullScreenPrimary */;
            }
            else
            {
                // Other windows are only allowed to be shown together with a primary
                // full screen window
                behavior |= (1 << 8) /* NSWindowCollectionBehaviorFullScreenAuxiliary */;
            }
            [window->nsWindow setCollectionBehavior: behavior];
        }
        
        window->isTransparent = (jStyleMask & com_sun_glass_ui_Window_TRANSPARENT) != 0;
        if (window->isTransparent == YES)
        {
            [window->nsWindow setBackgroundColor:[NSColor clearColor]];
            [window->nsWindow setHasShadow:NO];
            [window->nsWindow setOpaque:NO];
        }
        else
        {
            [window->nsWindow setHasShadow:YES];
            [window->nsWindow setOpaque:YES];
        }
        
        window->fullscreenWindow = nil;
        window->isAccessibleInitComplete = NO;
    }
    [pool drain];
    
    GLASS_CHECK_EXCEPTION(env);
    
    return ptr_to_jlong(window->nsWindow);
}



@interface GlassWindowDispatcher : NSObject
{
@public
    jobject         jWindow;
    jobject         jView;
    jlong                jOwnerPtr;
    jlong                jScreenPtr;
    jint                jStyleMask;
    jboolean    jIsChild;
    jlong                jlongReturn;
}
@end

@implementation GlassWindowDispatcher

- (void)_createWindowCommonDispatch
{
    GET_MAIN_JENV;
    self->jlongReturn = _createWindowCommonDo(env, self->jWindow, self->jOwnerPtr, self->jScreenPtr, self->jStyleMask, self->jIsChild);
}

@end

static jlong _createWindowCommon
(JNIEnv *env, jobject jWindow, jlong jOwnerPtr, jlong jScreenPtr, jint jStyleMask, jboolean jIsChild)
{
    LOG("_createWindowCommon");
    
    jlong value = 0L;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        jobject jWindowRef = (*env)->NewGlobalRef(env, jWindow);
        if ([NSThread isMainThread] == YES)
        {
            value = _createWindowCommonDo(env, jWindowRef, jOwnerPtr, jScreenPtr, jStyleMask, jIsChild);
        }
        else
        {
            GlassWindowDispatcher *dispatcher = [[GlassWindowDispatcher alloc] autorelease];
            dispatcher->jWindow = jWindowRef;
            dispatcher->jOwnerPtr = jOwnerPtr;
            dispatcher->jScreenPtr = jScreenPtr;
            dispatcher->jStyleMask = jStyleMask;
            dispatcher->jIsChild = jIsChild;
            [dispatcher performSelectorOnMainThread:@selector(_createWindowCommonDispatch) withObject:dispatcher waitUntilDone:YES]; // gznote: need to wait for return value
            value = dispatcher->jlongReturn;
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    LOG("   window: %p", value);
    return value;
}

#pragma mark --- JNI

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1initIDs
(JNIEnv *env, jclass jClass)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1initIDs");
    
    if (jWindowClass == NULL)
    {
        jWindowClass = (*env)->NewGlobalRef(env, jClass);
    }
    
    if (jMenuBarDelegateClass == NULL)
    {
        jMenuBarDelegateClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/mac/MacMenuBarDelegate"));
    }
    
    if (jViewClass == NULL)
    {
        jViewClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/View"));
    }
    
    if (jScreenClass == NULL)
    {
        jScreenClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/Screen"));
    }
    
    if (jWindowNotifyMove == NULL)
    {
        jWindowNotifyMove = (*env)->GetMethodID(env, jWindowClass, "notifyMove", "(II)V");
    }
    
    if (jWindowNotifyResize == NULL)
    {
        jWindowNotifyResize = (*env)->GetMethodID(env, jWindowClass, "notifyResize", "(III)V");
    }
    
    if (jWindowNotifyMoveToAnotherScreen == NULL)
    {
        jWindowNotifyMoveToAnotherScreen = (*env)->GetMethodID(env, jWindowClass, "notifyMoveToAnotherScreen", "(JJ)V");
    }
    
    if (jWindowNotifyClose == NULL)
    {
        jWindowNotifyClose = (*env)->GetMethodID(env, jWindowClass, "notifyClose", "()V");
    }
    
    if (jWindowNotifyFocus == NULL)
    {
        jWindowNotifyFocus = (*env)->GetMethodID(env, jWindowClass, "notifyFocus", "(I)V");
    }
    
    if (jWindowNotifyFocusUngrab == NULL)
    {
        jWindowNotifyFocusUngrab = (*env)->GetMethodID(env, jWindowClass, "notifyFocusUngrab", "()V");
    }
    
    if (jWindowNotifyFocusDisabled == NULL)
    {
        jWindowNotifyFocusDisabled = (*env)->GetMethodID(env, jWindowClass, "notifyFocusDisabled", "()V");
    }
    
    if (jWindowNotifyDestroy == NULL)
    {
        jWindowNotifyDestroy = (*env)->GetMethodID(env, jWindowClass, "notifyDestroy", "()V");
    }
    
    if (jWindowNotifyDelegatePtr == NULL)
    {
        jWindowNotifyDelegatePtr = (*env)->GetMethodID(env, jWindowClass, "notifyDelegatePtr", "(J)V");
    }

    if (jWindowNotifyInitAccessibilityPtr == NULL)
    {
        jWindowNotifyInitAccessibilityPtr = (*env)->GetMethodID(env, jWindowClass, "notifyInitAccessibility", "()V");
    }
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _createWindow
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacWindow__1createWindow
(JNIEnv *env, jobject jWindow, jlong jOwnerPtr, jlong jScreenPtr, jint jStyleMask)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1createWindow");
    
    return _createWindowCommon(env, jWindow, jOwnerPtr, jScreenPtr, jStyleMask, JNI_FALSE);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _createChildWindow
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacWindow__1createChildWindow
(JNIEnv *env, jobject jWindow, jlong jOwnerPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1createChildWindow");
    LOG("   owner: %p", jOwnerPtr);
    
    jlong jScreenPtr = 0L;
    jint jStyleMask = NSBorderlessWindowMask;
    if (jOwnerPtr == BROWSER_PARENT_ID)
    {
        LOG("       case PARENT (PLUGIN)");
        // special case: embedded window for plugin (the container which will hold the child window)
    }
    else
    {
        LOG("       case CHILD (EMBEDDED)");
        // special case: embedded window for plugin (the actual plugin window with remote layer)
        // jOwnerPtr must be a valid GlassEmbeddedWindow instance
        if (![GlassEmbeddedWindow exists:(GlassEmbeddedWindow*)jlong_to_ptr(jOwnerPtr)]) {
            return (jlong)0;
        }
    }
    
    return _createWindowCommon(env, jWindow, jOwnerPtr, jScreenPtr, jStyleMask, JNI_TRUE);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _setLevel
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setLevel
(JNIEnv *env, jobject jWindow, jlong jPtr, jint jLevel)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setLevel");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        window->_setLevel = jLevel;
        
        if ([NSThread isMainThread] == YES)
        {
            [window _setLevel];
        }
        else
        {
            [window performSelectorOnMainThread:@selector(_setLevel) withObject:nil waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _setFocusable
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setFocusable
(JNIEnv *env, jobject jWindow, jlong jPtr, jboolean isFocusable)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setCanBecomeActive");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        window->isFocusable = (isFocusable==JNI_TRUE);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _setEnabled
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setEnabled
(JNIEnv *env, jobject jwindow, jlong jPtr, jboolean isEnabled)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setEnabled");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        
        GLASS_PERFORM_WITH_ARG(window, _setEnabled, [NSNumber numberWithBool: (BOOL)isEnabled], YES);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*                                                                                                     
 * Class:     com_sun_glass_ui_mac_MacWindow                                                     
 * Method:    _setAlpha                                                                                
 * Signature: (F)V                                                                                     
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setAlpha
(JNIEnv *env, jobject jWindow, jlong jPtr, jfloat jAlpha)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setAlpha");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        window->_setAlpha = jAlpha;
        
        if ([NSThread isMainThread] == YES)
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
 * Class:     com_sun_glass_ui_mac_MacWindow                                                     
 * Method:    _setBackground
 * Signature: (JFFF)Z                                                                           
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setBackground
(JNIEnv *env, jobject jWindow, jlong jPtr, jfloat r, jfloat g, jfloat b)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setBackground");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        if ([NSThread isMainThread] == YES)
        {
            [window->nsWindow setBackgroundColor:[NSColor colorWithCalibratedRed:r green:g blue:b alpha:1.0f]];
        }
        else
        {
            [window->nsWindow performSelectorOnMainThread:@selector(setBackgroundColor:) withObject:[NSColor colorWithCalibratedRed:r green:g blue:b alpha:1.0f] waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return JNI_TRUE; // gznote: remove this return value if unused
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow                                                     
 * Method:    _setView                                                                                 
 * Signature: (J)Z                                        
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setView
(JNIEnv *env, jobject jWindow, jlong jPtr, jobject jview)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setView");
    LOG("   window: %p", jPtr);
    LOG("   view: %p", getMacView(env, jview));
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);

        // We don't support changing views in the FS mode because
        // by Glass design the FS functionality belongs to the View.
        // Also, this leads to a crash on the Mac
        if ([window->nsWindow styleMask] & (1 << 14)/*NSFullScreenWindowMask*/) {
            [window->nsWindow performSelector:@selector(toggleFullScreen:) withObject:nil];

            // Wait until the FS mode has really exited
            [GlassApplication enterFullScreenExitingLoop];
        }

        NSView<GlassView> *oldView = window->view;
        window->view = getMacView(env, jview);
        //NSLog(@"        window: %@", window);
        //NSLog(@"                frame: %.2f,%.2f %.2fx%.2f", [window frame].origin.x, [window frame].origin.y, [window frame].size.width, [window frame].size.height);
        //NSLog(@"        view: %@", window->view);
        //NSLog(@"                frame: %.2f,%.2f %.2fx%.2f", [window->view frame].origin.x, [window->view frame].origin.y, [window->view frame].size.width, [window->view frame].size.height);

        // RT-24864: notify multi touch handling code that 
        // view delegate has changed
        [GlassTouches updateTracking:(oldView ? [oldView delegate] : nil) 
                         newDelegate:(window->view ? [window->view delegate] : nil)];

        if (oldView && oldView != window->view) {
            [[oldView delegate] resetMouseTracking];
        }

        if (window->view != nil)
        {
            window->suppressWindowMoveEvent = YES; // RT-11215
            {
                NSRect viewFrame = [window->view frame];
                if ((viewFrame.size.width != 0.0f) && (viewFrame.size.height != 0.0f))
                {
                    NSRect windowFrame = [window->nsWindow frameRectForContentRect:viewFrame];
                    windowFrame.origin.x = [window->nsWindow frame].origin.x;
                    windowFrame.origin.y = [window->nsWindow frame].origin.y;
                    [window _setWindowFrameWithRect:NSMakeRect(windowFrame.origin.x, windowFrame.origin.y, windowFrame.size.width, windowFrame.size.height) withDisplay:JNI_TRUE withAnimate:JNI_FALSE];
                }
                
                if ([NSThread isMainThread] == YES)
                {
                    [window->nsWindow setContentView:[window->view superview]]; // use our superview not ourselves!
                    [window->nsWindow setInitialFirstResponder:window->view];
                    [window->nsWindow makeFirstResponder:window->view];
                }
                else
                {
                    [window->nsWindow performSelectorOnMainThread:@selector(setContentView:) withObject:[window->view superview] waitUntilDone:YES];
                    [window->nsWindow performSelectorOnMainThread:@selector(setInitialFirstResponder:) withObject:window->view waitUntilDone:YES];
                    [window->nsWindow performSelectorOnMainThread:@selector(makeFirstResponder:) withObject:window->view waitUntilDone:YES];
                }
            }
            window->suppressWindowMoveEvent = NO;
        }
        else
        {
            [window->nsWindow performSelectorOnMainThread:@selector(setContentView:) withObject:nil waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return JNI_TRUE; // gznote: remove this return value if unused
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _setMenubar
 * Signature: (Lcom/sun/glass/ui/mac/MacMenubarDelegate;)V
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setMenubar
(JNIEnv *env, jobject jWindow, jlong jPtr, jlong jMenubarPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setMenubar");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        window->menubar = (GlassMenubar*)jlong_to_ptr(jMenubarPtr);
        if ([NSThread isMainThread] == YES)
        {
            [NSApp setMainMenu:window->menubar->menu];
            [[NSApp mainMenu] update];
        }
        else
        {
            [NSApp performSelectorOnMainThread:@selector(setMainMenu:) withObject:window->menubar->menu waitUntilDone:YES];
            [[NSApp mainMenu] performSelectorOnMainThread:@selector(update) withObject:nil waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return JNI_TRUE; // gznote: remove this return value if unused
}

/*                                                                                                     
 * Class:     com_sun_glass_ui_mac_MacWindow                                                     
 * Method:    _close                                                                                   
 * Signature: ()V                                                                                      
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1close
(JNIEnv *env, jclass cls, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1close");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        if ([NSThread isMainThread] == YES)
        {
            // this call will always close the window
            // without calling the windowShouldClose
            [window->nsWindow close];
        }
        else
        {
            // this call will always close the window
            // without calling the windowShouldClose
            [window->nsWindow performSelectorOnMainThread:@selector(close) withObject:nil waitUntilDone:YES];
        }
        // The nsWindow is released automatically since we don't retain it
        // The window is released in the nsWindow -close override method
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return JNI_TRUE; // gznote: remove this return value if unused
}

/*                                                                                                     
 * Class:     com_sun_glass_ui_mac_MacWindow                                                     
 * Method:    _requestFocus
 * Signature: (J)Z                                                                                      
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1requestFocus
(JNIEnv *env, jobject jWindow, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1requestFocus");
    
    jboolean focused = JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);

        if ([window->nsWindow isVisible])
        {
            [window->nsWindow makeMainWindow];
            [window->nsWindow makeKeyAndOrderFront:window->nsWindow];
            [window->nsWindow orderFrontRegardless];
        }

        focused = [window->nsWindow isKeyWindow] ? JNI_TRUE : JNI_FALSE;
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return focused;
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _grabFocus
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1grabFocus
(JNIEnv *env, jobject jThis, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1grabFocus");
    jboolean ret = JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow * window = getGlassWindow(env, jPtr);
        //TODO: full screen
        if ([NSThread isMainThread] == YES)
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
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _ungrabFocus
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1ungrabFocus
(JNIEnv *env, jobject jThis, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1ungrabFocus");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow * window = getGlassWindow(env, jPtr);
        //TODO; full screen
        if ([NSThread isMainThread] == YES)
        {
            [window _ungrabFocus];
        }
        else
        {
            [window performSelectorOnMainThread:@selector(_ungrabFocus) withObject:nil waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _maximize
 * Signature: (JZZ)V
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1maximize
(JNIEnv *env, jobject jWindow, jlong jPtr, jboolean maximize, jboolean isZoomed)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1maximize");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        window->suppressWindowResizeEvent = YES;
        
        if ((maximize == JNI_TRUE) && (isZoomed == JNI_FALSE))
        {
            window->preZoomedRect = [window->nsWindow frame];
            
            if ([window->nsWindow styleMask] != NSBorderlessWindowMask)
            { 
                if ([NSThread isMainThread] == YES)
                {
                    [window->nsWindow zoom:nil];
                }
                else
                {
                    [window->nsWindow performSelectorOnMainThread:@selector(zoom:) withObject:nil waitUntilDone:YES];
                }
                
                // windowShouldZoom will be called automatically in this case
            }
            else
            {
                NSRect visibleRect = [[window _getScreen] visibleFrame];
                [window _setWindowFrameWithRect:NSMakeRect(visibleRect.origin.x, visibleRect.origin.y, visibleRect.size.width, visibleRect.size.height) withDisplay:JNI_TRUE withAnimate:JNI_TRUE];
                
                // calling windowShouldZoom will send Java maximize event
                [window windowShouldZoom:window->nsWindow toFrame:[window->nsWindow frame]];
            }
        }
        else if ((maximize == JNI_FALSE) && (isZoomed == JNI_TRUE))
        {
            if ([NSThread isMainThread] == YES)
            {
                [window _restorePreZoomedRect];
            }
            else
            {
                [window performSelectorOnMainThread:@selector(_restorePreZoomedRect) withObject:nil waitUntilDone:YES];
            }
        }
        
        window->suppressWindowResizeEvent = NO;
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return JNI_TRUE; // gznote: remove this return value if unused
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _setBounds
 * Signature: (JIIZZIIIIFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setBounds
(JNIEnv *env, jobject jWindow, jlong jPtr,
 jint x, jint y, jboolean xSet, jboolean ySet,
 jint w, jint h, jint cw, jint ch, jfloat xGravity, jfloat yGravity)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setBounds");
    LOG("   x,y: %d,%d", x, y);
    LOG("   xSet,ySet: %d,%d", xSet, ySet);
    LOG("   xGravity,yGravity: %.2f,%.2f", xGravity, yGravity);
    LOG("   w x h: %dx%d", w, h);
    LOG("   cw x ch: %dx%d", cw, ch);
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        NSPoint origin = [window _flipFrame].origin;
        NSSize size = [window->nsWindow frame].size;
        NSSize sizeForClient = [NSWindow frameRectForContentRect:NSMakeRect(0.0, 0.0, cw > 0 ? cw : 0.0, ch > 0 ? ch : 0.0) styleMask:[window->nsWindow styleMask]].size;
        
        jint newX = xSet == JNI_TRUE ? x : (jint)origin.x;
        jint newY = ySet == JNI_TRUE ? y : (jint)origin.y;
        jint newW = (w > 0) ? w : (cw > 0) ? (jint)sizeForClient.width : (jint)size.width;
        jint newH = (h > 0) ? h : (ch > 0) ? (jint)sizeForClient.height : (jint)size.height;
        
        [window _setWindowFrameWithRect:NSMakeRect(newX, newY, newW, newH) withDisplay:JNI_TRUE withAnimate:JNI_FALSE];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _setMinimumSize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setMinimumSize
(JNIEnv *env, jobject jWindow, jlong jPtr, jint jW, jint jH)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setMinimumSize");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        window->_setMinimumSizeW = jW;
        window->_setMinimumSizeH = jH;
        
        if ([NSThread isMainThread] == YES)
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
    
    return JNI_TRUE; // gznote: remove this return value if unused
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _setMaximumSize
 * Signature: (JII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setMaximumSize
(JNIEnv *env, jobject jWindow, jlong jPtr, jint jW, jint jH)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setMaximumSize");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        window->_setMaximumSizeW = jW == -1 ? FLT_MAX : (CGFloat)jW;
        window->_setMaximumSizeH = jH == -1 ? FLT_MAX : (CGFloat)jH;
        
        if ([NSThread isMainThread] == YES)
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
    
    return JNI_TRUE; // gznote: remove this return value if unused
}

/*                                                                                                     
 * Class:     com_sun_glass_ui_mac_MacWindow                                                     
 * Method:    _setResizable                                                                              
 * Signature: (Z)Z                                                                                     
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setResizable
(JNIEnv *env, jobject jWindow, jlong jPtr, jboolean jResizable)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setResizable");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        if (window->isResizable != jResizable)
        {
            [window performSelectorOnMainThread:@selector(_setResizable) withObject:nil waitUntilDone:YES];
        }
    }        
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return JNI_TRUE;
}

/*                                                                                                     
 * Class:     com_sun_glass_ui_mac_MacWindow                                                     
 * Method:    _setVisible                                                                              
 * Signature: (Z)Z                                                                                     
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setVisible
(JNIEnv *env, jobject jWindow, jlong jPtr, jboolean jVisible)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setVisible");
    
    jboolean now = JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        if (jVisible == JNI_TRUE)
        {
            if ([NSThread isMainThread] == YES)
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
            if ([NSThread isMainThread] == YES)
            {
                [window _ungrabFocus];
                if (window->owner != nil)
                {
                    [window->owner removeChildWindow:window->nsWindow];
                }
                [window->nsWindow orderOut:window->nsWindow];
            }
            else
            {
                [window performSelectorOnMainThread:@selector(_ungrabFocus) withObject:nil waitUntilDone:YES];
                if (window->owner != nil)
                {
                    [window->owner performSelectorOnMainThread:@selector(removeChildWindow:) withObject:window->nsWindow waitUntilDone:YES];
                }
                [window->nsWindow performSelectorOnMainThread:@selector(orderOut:) withObject:window->nsWindow waitUntilDone:YES];
            }
        }
        now = [window->nsWindow isVisible] ? JNI_TRUE : JNI_FALSE;
        
        // RT-22502 temp workaround: bring plugin window in front of a browser 
        if (now == YES)
        {
            static BOOL isBackgroundOnlyAppChecked = NO;
            static BOOL isBackgroundOnlyApp = NO;
            if (isBackgroundOnlyAppChecked == NO)
            {
                isBackgroundOnlyAppChecked = YES;
                
                ProcessSerialNumber psn;
                if (GetCurrentProcess(&psn) == noErr)
                {
                    ProcessInfoRec info;
                    memset(&info, 0x00, sizeof(ProcessInfoRec));
                    GetProcessInformation(&psn, &info);
                    isBackgroundOnlyApp = ((modeOnlyBackground&info.processMode) == modeOnlyBackground);
                }
            }
            if (isBackgroundOnlyApp == YES)
            {
                [window->nsWindow performSelectorOnMainThread:@selector(orderFrontRegardless) withObject:nil waitUntilDone:YES];
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return now;
}

/*                                                                                                     
 * Class:     com_sun_glass_ui_mac_MacWindow                                                     
 * Method:    _setTitle                                                                                
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setTitle
(JNIEnv *env, jobject jWindow, jlong jPtr, jstring jTitle)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setTitle");
    LOG("   window: %p", jPtr);
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        
        NSString *title = [GlassHelper nsStringWithJavaString:jTitle withEnv:env];
        LOG("   title: %s", [title UTF8String]);
        if ([NSThread isMainThread] == YES)
        {
            [window->nsWindow setTitle:title];
        }
        else
        {
            [window->nsWindow performSelectorOnMainThread:@selector(setTitle:) withObject:title waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return JNI_TRUE; // gnote: remove this return value if unused
}

/*                                                                                                     
 * Class:     com_sun_glass_ui_mac_MacWindow                                                     
 * Method:    _minimize                                                                                
 * Signature: (Z)V                                                                                     
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacWindow__1minimize
(JNIEnv *env, jobject jWindow, jlong jPtr, jboolean jMiniaturize)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1minimize");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        
        if (jMiniaturize == JNI_TRUE)
        {
            if ([NSThread isMainThread] == YES)
            {
                [window->nsWindow miniaturize:nil];
            }
            else
            {
                [window->nsWindow performSelectorOnMainThread:@selector(miniaturize:) withObject:nil waitUntilDone:YES];
            }
        }
        else
        {
            if ([NSThread isMainThread] == YES)
            {
                [window->nsWindow deminiaturize:nil];
            }
            else
            {
                [window->nsWindow performSelectorOnMainThread:@selector(deminiaturize:) withObject:nil waitUntilDone:YES];
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return JNI_TRUE; // gnote: remove this return value if unused
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _setIcon
 * Signature: (JLcom/sun/glass/ui/Pixels;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1setIcon
(JNIEnv *env, jobject jWindow, jlong jPtr, jobject jPixels)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1setIcon");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        if (jPixels != NULL)
        {
            NSImage *image = nil;
            (*env)->CallVoidMethod(env, jPixels, jPixelsAttachData, ptr_to_jlong(&image));
            if (image != nil)
            {
                GlassWindow *window = getGlassWindow(env, jPtr);
                
                // need an explicit window title for the rest of the code to work
                if ([window->nsWindow title] == nil)
                {
                    if ([NSThread isMainThread] == YES)
                    {
                        [window->nsWindow setTitle:@"Untitled"];
                    }
                    else
                    {
                        [window->nsWindow performSelectorOnMainThread:@selector(setTitle:) withObject:@"Untitled" waitUntilDone:YES];
                    }
                }
                
                // http://www.cocoabuilder.com/archive/cocoa/199554-nswindow-title-bar-icon-without-representedurl.html
                if ([NSThread isMainThread] == YES)
                {
                    [window->nsWindow setRepresentedURL:[NSURL fileURLWithPath:[window->nsWindow title]]];
                    [[window->nsWindow standardWindowButton:NSWindowDocumentIconButton] setImage:image];
                }
                else
                {
                    [window->nsWindow performSelectorOnMainThread:@selector(setRepresentedURL:) withObject:[NSURL fileURLWithPath:[window->nsWindow title]] waitUntilDone:YES];
                    [[window->nsWindow standardWindowButton:NSWindowDocumentIconButton] performSelectorOnMainThread:@selector(setImage:) withObject:image waitUntilDone:YES];
                }
                
                [image release];
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _toFront
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1toFront
(JNIEnv *env, jobject jWindow, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1toFront");
    LOG("   window: %p", jPtr);
    
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        [window->nsWindow orderFrontRegardless];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _toBack
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1toBack
(JNIEnv *env, jobject jWindow, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1toBack");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        [window->nsWindow orderBack:nil];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}


/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _enterModal
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1enterModal
(JNIEnv *env, jobject jWindow, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1enterModal");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        [NSApp runModalForWindow:window->nsWindow];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _enterModalWithWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1enterModalWithWindow
(JNIEnv *env, jobject jWindow, jlong jDialogPtr, jlong jWindowPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1enterModalWithWindow");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        //GlassWindow *window = getGlassWindow(env, jDialogPtr);
        // TODO: implement _enterModalWithWindow
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _exitModal
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacWindow__1exitModal
(JNIEnv *env, jobject jWindow, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1exitModal");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        [NSApp stop:window->nsWindow];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _getEmbeddedX
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacWindow__1getEmbeddedX
(JNIEnv *env, jobject jWindow, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1getEmbeddedX");
    
    jint x = 0;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassEmbeddedWindow *window = getGlassEmbeddedWindow(env, jPtr);
        x = (int)round([window frame].origin.x);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return x;
}

/*
 * Class:     com_sun_glass_ui_mac_MacWindow
 * Method:    _getEmbeddedY
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacWindow__1getEmbeddedY
(JNIEnv *env, jobject jWindow, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacWindow__1getEmbeddedX");
    
    jint y = 0;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassEmbeddedWindow *window = getGlassEmbeddedWindow(env, jPtr);
        NSRect frameRect = [window frame];
        
        // flip y coorindate
        NSScreen *screen = [[NSScreen screens] objectAtIndex:0];
        NSRect screenFrame = screen.frame;
        y = (int)round(screenFrame.size.height - frameRect.size.height - frameRect.origin.y);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return y;
}
