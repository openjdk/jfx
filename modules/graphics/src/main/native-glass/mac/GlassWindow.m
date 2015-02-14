/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
    assert(jPtr != 0L);

    NSWindow * nsWindow = (NSWindow*)jlong_to_ptr(jPtr);
    return (GlassWindow*)[nsWindow delegate];
}

static inline NSView<GlassView> *getMacView(JNIEnv *env, jobject jview)
{
    if (jview != NULL)
    {
        jfieldID jfID = (*env)->GetFieldID(env, jViewClass, "ptr", "J");
        GLASS_CHECK_EXCEPTION(env);
        return (NSView<GlassView>*)jlong_to_ptr((*env)->GetLongField(env, jview, jfID));
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
- (void)dealloc                                                                         \
{                                                                                       \
    id window = self->gWindow;                                                          \
    [super dealloc];                                                                    \
    [window release];                                                                   \
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
    /* Local copy of the id keeps the retain/release calls balanced. */                 \
    id view = [self->gWindow->view retain];                                             \
    [self->gWindow sendEvent:event];                                                    \
    [super sendEvent:event];                                                            \
    [view release];                                                                     \
}

@implementation GlassWindow_Normal
GLASS_NS_WINDOW_IMPLEMENTATION
@end

@implementation GlassWindow_Panel
GLASS_NS_WINDOW_IMPLEMENTATION

- (void)setWorksWhenModal:(BOOL)worksWhenModal
{
    [super setWorksWhenModal:NO];
}

- (BOOL)accessibilityIsIgnored
{
    /* In JavaFX NSPanels are used to implement PopupWindows,
     * which are used by ContextMenu.  In Accessibility, for a
     * menu to work as expected, the window has to be ignored.
     * Note that asking the children of the window is
     * very important in this context. It ensures that all
     * descendants created.  Without it, the menu  will
     * not be seen by the assistive technology.
     */
    __block BOOL ignored = [super accessibilityIsIgnored];
    NSArray* children = [self accessibilityAttributeValue: NSAccessibilityChildrenAttribute];
    if (children) {
        [children enumerateObjectsUsingBlock: ^(id child, NSUInteger index, BOOL *stop) {
            NSString* role = [child accessibilityAttributeValue: NSAccessibilityRoleAttribute];
            if ([NSAccessibilityMenuRole isEqualToString: role]) {
                ignored = YES;
                *stop = YES;
            }
            /* Tooltips are exposed by AXHelp attribute and there is no API in Mac
             * to represent the tooltip window.
             * Nonetheless, the window must be ignored to prevent interfering with
             * VoiceOver focus.
             */
            if ([@"AXJFXTOOLTIP" isEqualToString: role]) {
                ignored = YES;
                *stop = YES;
            }
        }];
    }
    return ignored;
}

- (id)accessibilityAttributeValue:(NSString *)attribute
{
    /* 
    * The default value of AXRoleDescription for a NSPanel is 'system dialog'.
    * While this is correct for an average cocoa application it is not appropriate
    * for JFX, where all NSPanels are decoration-less windows used to implement 
    * tooltip, context menus, combo boxes, etc.
    */
    if ([NSAccessibilityRoleDescriptionAttribute isEqualToString: attribute]) {
        return @"";
    }
    return [super accessibilityAttributeValue: attribute];
}

@end
// --------------------------------------------------------------------------------------


@implementation GlassWindow

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
            
            if (((jStyleMask&com_sun_glass_ui_Window_MINIMIZABLE) != 0) ||
                ((jStyleMask&com_sun_glass_ui_Window_MAXIMIZABLE) != 0))
            {
                // on Mac OS X there is one set for min/max buttons,
                // so if clients requests either one, we turn them both on
                styleMask = styleMask|NSMiniaturizableWindowMask;
            }
            
            if ((jStyleMask&com_sun_glass_ui_Window_UNIFIED) != 0) {
                styleMask = styleMask|NSTexturedBackgroundWindowMask;
            }
            
            if ((jStyleMask&com_sun_glass_ui_Window_UTILITY) != 0)
            {
                styleMask = styleMask | NSUtilityWindowMask | NSNonactivatingPanelMask;
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
        
        if ((jStyleMask & com_sun_glass_ui_Window_UNIFIED) != 0) {
            //Prevent the textured effect from disappearing on border thickness recalculation
            [window->nsWindow setAutorecalculatesContentBorderThickness:NO forEdge:NSMaxYEdge];
        }

        if ((jStyleMask & com_sun_glass_ui_Window_UTILITY) != 0) {
            [[window->nsWindow standardWindowButton:NSWindowMiniaturizeButton] setHidden:YES];
            [[window->nsWindow standardWindowButton:NSWindowZoomButton] setHidden:YES];
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

        window->isSizeAssigned = NO;
        window->isLocationAssigned = NO;

        if (jIsChild == JNI_TRUE && jOwnerPtr != 0L && jOwnerPtr != BROWSER_PARENT_ID
            && [window->nsWindow isKindOfClass:[GlassEmbeddedWindow class]])
        {
            GlassEmbeddedWindow* parent = ((GlassEmbeddedWindow*)window->nsWindow)->parent;
            if ([GlassEmbeddedWindow exists:parent])
            {
                window->isLocationAssigned = YES;
                [window _setBounds:(int)round(parent.frame.origin.x)
                                 y:(int)round(parent.frame.origin.y)
                              xSet:YES ySet:YES w:0 h:0 cw:0 ch:0];
            }
        }
    }
    [pool drain];
    
    GLASS_CHECK_EXCEPTION(env);
    
    return ptr_to_jlong(window->nsWindow);
}

static jlong _createWindowCommon
(JNIEnv *env, jobject jWindow, jlong jOwnerPtr, jlong jScreenPtr, jint jStyleMask, jboolean jIsChild)
{
    LOG("_createWindowCommon");
    
    jlong value = 0L;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        jobject jWindowRef = (*env)->NewGlobalRef(env, jWindow);
        value = _createWindowCommonDo(env, jWindowRef, jOwnerPtr, jScreenPtr, jStyleMask, jIsChild);
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
        jMenuBarDelegateClass = (*env)->NewGlobalRef(env, [GlassHelper ClassForName:"com.sun.glass.ui.mac.MacMenuBarDelegate" withEnv:env]);
    }
    
    if (jViewClass == NULL)
    {
        jViewClass = (*env)->NewGlobalRef(env, [GlassHelper ClassForName:"com.sun.glass.ui.View" withEnv:env]);
    }
    
    if (jScreenClass == NULL)
    {
        jScreenClass = (*env)->NewGlobalRef(env, [GlassHelper ClassForName:"com.sun.glass.ui.Screen" withEnv:env]);
    }
    
    if (jWindowNotifyMove == NULL)
    {
        jWindowNotifyMove = (*env)->GetMethodID(env, jWindowClass, "notifyMove", "(II)V");
        if ((*env)->ExceptionCheck(env)) return;
    }
    
    if (jWindowNotifyResize == NULL)
    {
        jWindowNotifyResize = (*env)->GetMethodID(env, jWindowClass, "notifyResize", "(III)V");
        if ((*env)->ExceptionCheck(env)) return;
    }
    
    if (jWindowNotifyMoveToAnotherScreen == NULL)
    {
        jWindowNotifyMoveToAnotherScreen = (*env)->GetMethodID(env, jWindowClass, "notifyMoveToAnotherScreen", "(Lcom/sun/glass/ui/Screen;)V");
        if ((*env)->ExceptionCheck(env)) return;
    }
    
    if (jWindowNotifyClose == NULL)
    {
        jWindowNotifyClose = (*env)->GetMethodID(env, jWindowClass, "notifyClose", "()V");
        if ((*env)->ExceptionCheck(env)) return;
    }
    
    if (jWindowNotifyFocus == NULL)
    {
        jWindowNotifyFocus = (*env)->GetMethodID(env, jWindowClass, "notifyFocus", "(I)V");
        if ((*env)->ExceptionCheck(env)) return;
    }
    
    if (jWindowNotifyFocusUngrab == NULL)
    {
        jWindowNotifyFocusUngrab = (*env)->GetMethodID(env, jWindowClass, "notifyFocusUngrab", "()V");
        if ((*env)->ExceptionCheck(env)) return;
    }
    
    if (jWindowNotifyFocusDisabled == NULL)
    {
        jWindowNotifyFocusDisabled = (*env)->GetMethodID(env, jWindowClass, "notifyFocusDisabled", "()V");
        if ((*env)->ExceptionCheck(env)) return;
    }
    
    if (jWindowNotifyDestroy == NULL)
    {
        jWindowNotifyDestroy = (*env)->GetMethodID(env, jWindowClass, "notifyDestroy", "()V");
        if ((*env)->ExceptionCheck(env)) return;
    }
    
    if (jWindowNotifyDelegatePtr == NULL)
    {
        jWindowNotifyDelegatePtr = (*env)->GetMethodID(env, jWindowClass, "notifyDelegatePtr", "(J)V");
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
    if (!jPtr) return;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        NSInteger level = NSNormalWindowLevel;
        switch (jLevel)
        {
            case com_sun_glass_ui_Window_Level_FLOATING:
                level = NSFloatingWindowLevel;
                break;
            case com_sun_glass_ui_Window_Level_TOPMOST:
                level = NSScreenSaverWindowLevel;
                break;
        }
        [window->nsWindow setLevel:level];
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
    if (!jPtr) return;
    
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
    if (!jPtr) return;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        window->isEnabled = (BOOL)isEnabled;

        if (!window->isEnabled) {
            window->enabledStyleMask = [window->nsWindow styleMask];
            [window->nsWindow setStyleMask: (window->enabledStyleMask & ~(NSUInteger)(NSMiniaturizableWindowMask | NSResizableWindowMask))];

            //XXX: perhaps we could simply enable/disable the buttons w/o playing with the styles at all
            NSButton *zoomButton = [window->nsWindow standardWindowButton:NSWindowZoomButton];
            [zoomButton setEnabled:NO];
        } else {
            [window->nsWindow setStyleMask: window->enabledStyleMask];

            if (window->enabledStyleMask & NSResizableWindowMask) {
                NSButton *zoomButton = [window->nsWindow standardWindowButton:NSWindowZoomButton];
                [zoomButton setEnabled:YES];
            }
        }
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
    if (!jPtr) return;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        [window->nsWindow setAlphaValue:jAlpha];
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
    if (!jPtr) return JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        [window->nsWindow setBackgroundColor:[NSColor colorWithCalibratedRed:r green:g blue:b alpha:1.0f]];
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
    if (!jPtr) return JNI_FALSE;
    
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

        if (oldView && oldView != window->view) {
            [[oldView delegate] resetMouseTracking];
        }

        if (window->view != nil)
        {
            CALayer *layer = [window->view layer];
            if (([layer isKindOfClass:[CAOpenGLLayer class]] == YES) &&
                (([window->nsWindow styleMask] & NSTexturedBackgroundWindowMask) == NO))
            {
                [((CAOpenGLLayer*)layer) setOpaque:[window->nsWindow isOpaque]];
            }
            
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
                
                [window->nsWindow setContentView:[window->view superview]]; // use our superview not ourselves!
                [window->nsWindow setInitialFirstResponder:window->view];
                [window->nsWindow makeFirstResponder:window->view];
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
    if (!jPtr) return JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        window->menubar = (GlassMenubar*)jlong_to_ptr(jMenubarPtr);
        [NSApp setMainMenu:window->menubar->menu];
        [[NSApp mainMenu] update];
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
    if (!jPtr) return JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        // this call will always close the window
        // without calling the windowShouldClose
        [window->nsWindow close];
        // The NSWindow will be automatically released after closing
        // The GlassWindow is released in the [NSWindow dealloc] override        
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
    if (!jPtr) return JNI_FALSE;
    
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
    if (!jPtr) return JNI_FALSE;

    jboolean ret = JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow * window = getGlassWindow(env, jPtr);
        //TODO: full screen
        [window _grabFocus];
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
    if (!jPtr) return;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow * window = getGlassWindow(env, jPtr);
        //TODO; full screen
        [window _ungrabFocus];
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
    if (!jPtr) return JNI_FALSE;
    
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
                [window->nsWindow zoom:nil];
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
            [window _restorePreZoomedRect];
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
    if (!jPtr) return;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        if (xSet || ySet) window->isLocationAssigned = YES;
        if (w > 0 || h > 0 || cw > 0 || ch > 0) window->isSizeAssigned = YES;
        [window _setBounds:x y:y xSet:xSet ySet:ySet w:w h:h cw:cw ch:ch];
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
    if (!jPtr) return JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        [window->nsWindow setMinSize:NSMakeSize(jW, jH)];
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
    if (!jPtr) return JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        [window->nsWindow setMaxSize:NSMakeSize(jW == -1 ? FLT_MAX : (CGFloat)jW,
                                                jH == -1 ? FLT_MAX : (CGFloat)jH)];
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
    if (!jPtr) return JNI_FALSE;
    
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
    if (!jPtr) return JNI_FALSE;
    
    jboolean now = JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        if (jVisible == JNI_TRUE)
        {
            if (!window->isLocationAssigned) {
                [window _setBounds:0 y:0 xSet:JNI_TRUE ySet:JNI_TRUE w:-1 h:-1 cw:-1 ch:-1];
            }
            if (!window->isSizeAssigned) {
                [window _setBounds:0 y:0 xSet:JNI_FALSE ySet:JNI_FALSE w:320 h:200 cw:-1 ch:-1];
            }
            [window _setVisible];
        }
        else
        {
            [window _ungrabFocus];
            if (window->owner != nil)
            {
                [window->owner removeChildWindow:window->nsWindow];
            }
            [window->nsWindow orderOut:window->nsWindow];
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
    if (!jPtr) return JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        
        NSString *title = [GlassHelper nsStringWithJavaString:jTitle withEnv:env];
        LOG("   title: %s", [title UTF8String]);
        [window->nsWindow setTitle:title];
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
    if (!jPtr) return JNI_FALSE;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        
        if (jMiniaturize == JNI_TRUE)
        {
            [window->nsWindow miniaturize:nil];
        }
        else
        {
            [window->nsWindow deminiaturize:nil];
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
    if (!jPtr) return;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassWindow *window = getGlassWindow(env, jPtr);
        if (jPixels != NULL)
        {
            NSImage *image = nil;
            (*env)->CallVoidMethod(env, jPixels, jPixelsAttachData, ptr_to_jlong(&image));
            if (image != nil) {
                // need an explicit window title for the rest of the code to work
                if ([window->nsWindow title] == nil)
                {
                    [window->nsWindow setTitle:@"Untitled"];
                }
                
                // http://www.cocoabuilder.com/archive/cocoa/199554-nswindow-title-bar-icon-without-representedurl.html
                [window->nsWindow setRepresentedURL:[NSURL fileURLWithPath:[window->nsWindow title]]];
                [[window->nsWindow standardWindowButton:NSWindowDocumentIconButton] setImage:image];
                [image release];
            } else {
                [[window->nsWindow standardWindowButton:NSWindowDocumentIconButton] setImage:nil];
            }
        } else {
            [[window->nsWindow standardWindowButton:NSWindowDocumentIconButton] setImage:nil];
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
    if (!jPtr) return;
    
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
    if (!jPtr) return;
    
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
    if (!jPtr) return;
    
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
    if (!jPtr) return;
    
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
    if (!jPtr) return 0;
    
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
    if (!jPtr) return 0;
    
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
