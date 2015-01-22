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
#import "com_sun_glass_events_DndEvent.h"
#import "com_sun_glass_events_KeyEvent.h"
#import "com_sun_glass_events_MouseEvent.h"
#import "com_sun_glass_ui_View_Capability.h"
#import "com_sun_glass_ui_mac_MacGestureSupport.h"
#import "GlassKey.h"
#import "GlassMacros.h"
#import "GlassView3D.h"
#import "GlassLayer3D.h"
#import "GlassApplication.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

//#define MOUSEVERBOSE
#ifndef MOUSEVERBOSE
    #define MOUSELOG(MSG, ...)
#else
    #define MOUSELOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

//#define KEYVERBOSE
#ifndef KEYVERBOSE
    #define KEYLOG(MSG, ...)
#else
    #define KEYLOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

//#define DNDVERBOSE
#ifndef DNDVERBOSE
    #define DNDLOG(MSG, ...)
#else
    #define DNDLOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

//#define IMVERBOSE
#ifndef IMVERBOSE
    #define IMLOG(MSG, ...)
#else
    #define IMLOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

#define SHARE_GL_CONTEXT
//#define DEBUG_COLORS

// http://developer.apple.com/library/mac/#technotes/tn2085/_index.html
//#define ENABLE_MULTITHREADED_GL

@implementation GlassView3D

- (CGLPixelFormatObj)_createPixelFormatWithDepth:(CGLPixelFormatAttribute)depth
{
    CGLPixelFormatObj pix = NULL;
    {
        const CGLPixelFormatAttribute attributes[] =
        {
            kCGLPFAAccelerated,
            kCGLPFAColorSize, 32,
            kCGLPFAAlphaSize, 8,
            kCGLPFADepthSize, depth,
            (CGLPixelFormatAttribute)0
        };
        GLint npix = 0;
        CGLError err = CGLChoosePixelFormat(attributes, &pix, &npix);
        if (err != kCGLNoError)
        {
            NSLog(@"CGLChoosePixelFormat error: %d", err);
        }
    }
    return pix;
}

- (CGLContextObj)_createContextWithShared:(CGLContextObj)share withFormat:(CGLPixelFormatObj)format
{
    CGLContextObj ctx = NULL;
    {
        CGLError err = CGLCreateContext(format, share, &ctx);
        if (err != kCGLNoError)
        {
            NSLog(@"CGLCreateContext error: %d", err);
        }
    }
    return ctx;
}

- (void)_initialize3dWithJproperties:(jobject)jproperties
{
    GET_MAIN_JENV;
    
    int depthBits = 0;
    if (jproperties != NULL)
    {
        jobject k3dDepthKey = (*env)->NewObject(env, jIntegerClass, jIntegerInitMethod, com_sun_glass_ui_View_Capability_k3dDepthKeyValue);
        GLASS_CHECK_EXCEPTION(env);
        jobject k3dDepthKeyValue = (*env)->CallObjectMethod(env, jproperties, jMapGetMethod, k3dDepthKey);
        GLASS_CHECK_EXCEPTION(env);
        if (k3dDepthKeyValue != NULL)
        {
            depthBits = (*env)->CallIntMethod(env, k3dDepthKeyValue, jIntegerValueMethod);
            GLASS_CHECK_EXCEPTION(env);
        }
    }
    
    CGLContextObj sharedCGL = NULL;
    if (jproperties != NULL)
    {
        jobject sharedContextPtrKey = (*env)->NewStringUTF(env, "shareContextPtr");
        jobject sharedContextPtrValue = (*env)->CallObjectMethod(env, jproperties, jMapGetMethod, sharedContextPtrKey);
        GLASS_CHECK_EXCEPTION(env);
        if (sharedContextPtrValue != NULL)
        {
            jlong jsharedContextPtr = (*env)->CallLongMethod(env, sharedContextPtrValue, jLongValueMethod);
            GLASS_CHECK_EXCEPTION(env);
            if (jsharedContextPtr != 0)
            {
                NSOpenGLContext *sharedContextNS = (NSOpenGLContext*)jlong_to_ptr(jsharedContextPtr);
                sharedCGL = [sharedContextNS CGLContextObj];
            }
        }
    }
    
    CGLContextObj clientCGL = NULL;
    if (jproperties != NULL)
    {
        jobject contextPtrKey = (*env)->NewStringUTF(env, "contextPtr");
        jobject contextPtrValue = (*env)->CallObjectMethod(env, jproperties, jMapGetMethod, contextPtrKey);
        GLASS_CHECK_EXCEPTION(env);
        if (contextPtrValue != NULL)
        {
            jlong jcontextPtr = (*env)->CallLongMethod(env, contextPtrValue, jLongValueMethod);
            GLASS_CHECK_EXCEPTION(env);
            if (jcontextPtr != 0)
            {
                NSOpenGLContext *clientContextNS = (NSOpenGLContext*)jlong_to_ptr(jcontextPtr);
                clientCGL = [clientContextNS CGLContextObj];
            }
        }
    }
    if (clientCGL == NULL)
    {
        CGLPixelFormatObj clientPixelFormat = [self _createPixelFormatWithDepth:(CGLPixelFormatAttribute)depthBits];
        clientCGL = [self _createContextWithShared:sharedCGL withFormat:clientPixelFormat];
    }
    if (sharedCGL == NULL)
    {
        // this can happen in Rain or clients other than Prism (ie. device details do not have the shared context set)
        sharedCGL = clientCGL;
    }

    self->isHiDPIAware = NO;
    if (jproperties != NULL)
    {
        jobject kHiDPIAwareKey = (*env)->NewObject(env, jIntegerClass, jIntegerInitMethod, com_sun_glass_ui_View_Capability_kHiDPIAwareKeyValue);
        GLASS_CHECK_EXCEPTION(env);
        jobject kHiDPIAwareValue = (*env)->CallObjectMethod(env, jproperties, jMapGetMethod, kHiDPIAwareKey);
        GLASS_CHECK_EXCEPTION(env);
        if (kHiDPIAwareValue != NULL)
        {
            self->isHiDPIAware = (*env)->CallBooleanMethod(env, kHiDPIAwareValue, jBooleanValueMethod) ? YES : NO;
            GLASS_CHECK_EXCEPTION(env);
        }
    }

    GlassLayer3D *layer = [[GlassLayer3D alloc] initWithSharedContext:sharedCGL andClientContext:clientCGL withHiDPIAware:self->isHiDPIAware];

    // https://developer.apple.com/library/mac/documentation/Cocoa/Reference/ApplicationKit/Classes/nsview_Class/Reference/NSView.html#//apple_ref/occ/instm/NSView/setWantsLayer:
    // the order of the following 2 calls is important: here we indicate we want a layer-hosting view
    {
        [self setLayer:layer];
        [self setWantsLayer:YES];
    }
}

- (id)initWithFrame:(NSRect)frame withJview:(jobject)jView withJproperties:(jobject)jproperties
{
    LOG("GlassView3D initWithFrame:withJview:withJproperties");
    self = [super initWithFrame:frame pixelFormat:[NSOpenGLView defaultPixelFormat]];
    if (self != nil)
    {
        [self _initialize3dWithJproperties:jproperties];
        
        self->_delegate = [[GlassViewDelegate alloc] initWithView:self withJview:jView];
        self->_drawCounter = 0;
        self->_texture = 0;
        
        self->_trackingArea = [[NSTrackingArea alloc] initWithRect:frame
                                                           options:(NSTrackingMouseMoved | NSTrackingActiveAlways | NSTrackingInVisibleRect)
                                                             owner:self userInfo:nil];
        [self addTrackingArea: self->_trackingArea];
        self->nsAttrBuffer = [[NSAttributedString alloc] initWithString:@""];
        self->imEnabled = NO;
        self->shouldProcessKeyEvent = YES;
    }
    return self;
}

- (void)dealloc
{
    if (self->_texture != 0)
    {
        GlassLayer3D *layer = (GlassLayer3D*)[self layer];
        [[layer getPainterOffscreen] bindForWidth:(GLuint)[self bounds].size.width andHeight:(GLuint)[self bounds].size.height];
        {
            glDeleteTextures(1, &self->_texture);
        }
        [[layer getPainterOffscreen] unbind];
    }
    
    [[self layer] release];
    [self->_delegate release];
    self->_delegate = nil;
    
    [self removeTrackingArea: self->_trackingArea];
    [self->_trackingArea release];
    self->_trackingArea = nil;

    [self->nsAttrBuffer release];
    self->nsAttrBuffer = nil;
    
    [super dealloc];
}

- (BOOL)becomeFirstResponder
{
    return YES;
}

- (BOOL)acceptsFirstResponder
{
    return YES;
}

- (BOOL)canBecomeKeyView
{
    return YES;
}

- (BOOL)postsBoundsChangedNotifications
{
    return NO;
}

- (BOOL)postsFrameChangedNotifications
{
    return NO;
}

- (BOOL)acceptsFirstMouse:(NSEvent *)theEvent
{
    return YES;
}

- (BOOL)isFlipped
{
    return YES;
}

- (BOOL)isOpaque
{
    return NO;
}

- (BOOL)mouseDownCanMoveWindow
{
    return NO;
}

// also called when closing window, when [self window] == nil
- (void)viewDidMoveToWindow
{
    if ([self window] != nil)
    {
        GlassLayer3D *layer = (GlassLayer3D*)[self layer];
        [[layer getPainterOffscreen] setBackgroundColor:[[[self window] backgroundColor] colorUsingColorSpaceName:NSDeviceRGBColorSpace]];
    }

    [self->_delegate viewDidMoveToWindow];
}

- (void)setFrameOrigin:(NSPoint)newOrigin
{
    [super setFrameOrigin:newOrigin];
    [self->_delegate setFrameOrigin:newOrigin];
}

- (void)setFrameSize:(NSSize)newSize
{
    [super setFrameSize:newSize];
    [self->_delegate setFrameSize:newSize];
}

- (void)setFrame:(NSRect)frameRect
{
    [super setFrame:frameRect];
    [self->_delegate setFrame:frameRect];
}

- (void)updateTrackingAreas
{
    [super updateTrackingAreas];
    [self->_delegate updateTrackingAreas];
}

- (void)mouseEntered:(NSEvent *)theEvent
{
    MOUSELOG("mouseEntered");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)mouseMoved:(NSEvent *)theEvent
{
    MOUSELOG("mouseMoved");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)mouseExited:(NSEvent *)theEvent
{
    MOUSELOG("mouseExited");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)mouseDown:(NSEvent *)theEvent
{
    MOUSELOG("mouseDown");
    // First check if system Input Method Engine needs to handle this event
    NSInputManager *inputManager = [NSInputManager currentInputManager];
    if ([inputManager wantsToHandleMouseEvents]) {
        if ([inputManager handleMouseEvent:theEvent]) {
            return;
        }
    }
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)mouseDragged:(NSEvent *)theEvent
{
    MOUSELOG("mouseDragged");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)mouseUp:(NSEvent *)theEvent
{
    MOUSELOG("mouseUp");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)rightMouseDown:(NSEvent *)theEvent
{
    MOUSELOG("rightMouseDown");
    [self->_delegate sendJavaMouseEvent:theEvent];
    // NOTE: menuForEvent: is invoked differently for right-click
    // and Ctrl+Click actions. So instead we always synthesize
    // the menu event in Glass. See sendJavaMouseEvent for details.
}

- (void)rightMouseDragged:(NSEvent *)theEvent
{
    MOUSELOG("rightMouseDragged");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)rightMouseUp:(NSEvent *)theEvent
{
    MOUSELOG("rightMouseUp");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)otherMouseDown:(NSEvent *)theEvent
{
    MOUSELOG("otherMouseDown");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)otherMouseDragged:(NSEvent *)theEvent
{
    MOUSELOG("otherMouseDragged");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)otherMouseUp:(NSEvent *)theEvent
{
    MOUSELOG("otherMouseUp");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (void)rotateWithEvent:(NSEvent *)theEvent
{
    [self->_delegate sendJavaGestureEvent:theEvent type:com_sun_glass_ui_mac_MacGestureSupport_GESTURE_ROTATE];
}

- (void)swipeWithEvent:(NSEvent *)theEvent
{
    [self->_delegate sendJavaGestureEvent:theEvent type:com_sun_glass_ui_mac_MacGestureSupport_GESTURE_SWIPE];
}

- (void)magnifyWithEvent:(NSEvent *)theEvent
{
    [self->_delegate sendJavaGestureEvent:theEvent type:com_sun_glass_ui_mac_MacGestureSupport_GESTURE_MAGNIFY];
}

- (void)endGestureWithEvent:(NSEvent *)theEvent
{
    [self->_delegate sendJavaGestureEndEvent:theEvent];
}

- (void)beginGestureWithEvent:(NSEvent *)theEvent
{
    [self->_delegate sendJavaGestureBeginEvent:theEvent];
}

- (void)scrollWheel:(NSEvent *)theEvent
{
    MOUSELOG("scrollWheel");
    [self->_delegate sendJavaMouseEvent:theEvent];
}

- (BOOL)performKeyEquivalent:(NSEvent *)theEvent
{
    KEYLOG("performKeyEquivalent");
    [GlassApplication registerKeyEvent:theEvent];

    // Crash if the FS window is released while performing a key equivalent
    // Local copy of the id keeps the retain/release calls balanced.
    id fsWindow = [self->_delegate->fullscreenWindow retain];

    // RT-37093, RT-37399 Command-EQUALS and Command-DOT needs special casing on Mac
    // as it is passed through as two calls to performKeyEquivalent, which in turn
    // create extra KeyEvents.
    //
    NSString *chars = [theEvent charactersIgnoringModifiers];
    if ([theEvent type] == NSKeyDown && [chars length] > 0)
    {
        unichar uch = [chars characterAtIndex:0];
        if ([theEvent modifierFlags] & NSCommandKeyMask &&
            (uch == com_sun_glass_events_KeyEvent_VK_PERIOD ||
             uch == com_sun_glass_events_KeyEvent_VK_EQUALS))
        {
            GET_MAIN_JENV;
            
            jcharArray jKeyChars = GetJavaKeyChars(env, theEvent);
            jint jModifiers = GetJavaModifiers(theEvent);
            
            (*env)->CallVoidMethod(env, self->_delegate->jView, jViewNotifyKey,
                                   com_sun_glass_events_KeyEvent_PRESS,
                                   uch, jKeyChars, jModifiers);
            (*env)->CallVoidMethod(env, self->_delegate->jView, jViewNotifyKey,
                                   com_sun_glass_events_KeyEvent_TYPED,
                                   uch, jKeyChars, jModifiers);
            (*env)->CallVoidMethod(env, self->_delegate->jView, jViewNotifyKey,
                                   com_sun_glass_events_KeyEvent_RELEASE,
                                   uch, jKeyChars, jModifiers);
            (*env)->DeleteLocalRef(env, jKeyChars);
            
            GLASS_CHECK_EXCEPTION(env);
            [fsWindow release];
            return YES;
        }
    }
    [self->_delegate sendJavaKeyEvent:theEvent isDown:YES];
    [fsWindow release];
    return NO; // return NO to allow system-default processing of Cmd+Q, etc.
}

- (void)keyDown:(NSEvent *)theEvent
{
    KEYLOG("keyDown");
    [GlassApplication registerKeyEvent:theEvent];

    if (![[self inputContext] handleEvent:theEvent] || shouldProcessKeyEvent) {
        [self->_delegate sendJavaKeyEvent:theEvent isDown:YES]; 
    }
    shouldProcessKeyEvent = YES;
}

- (void)keyUp:(NSEvent *)theEvent
{
    KEYLOG("keyUp");
    [self->_delegate sendJavaKeyEvent:theEvent isDown:NO];
}

- (void)flagsChanged:(NSEvent *)theEvent
{
    KEYLOG("flagsChanged");
    [self->_delegate sendJavaModifierKeyEvent:theEvent];
}

- (BOOL)wantsPeriodicDraggingUpdates
{
    // we only want want updated drag operations when the mouse position changes
    return NO;
}

- (BOOL)prepareForDragOperation:(id <NSDraggingInfo>)sender
{
    DNDLOG("prepareForDragOperation");
    return YES;
}

- (BOOL)performDragOperation:(id <NSDraggingInfo>)sender
{
    DNDLOG("performDragOperation");
    [self->_delegate sendJavaDndEvent:sender type:com_sun_glass_events_DndEvent_PERFORM];
    
    return YES;
}

- (void)concludeDragOperation:(id <NSDraggingInfo>)sender
{
    DNDLOG("concludeDragOperation");
}

- (NSDragOperation)draggingEntered:(id <NSDraggingInfo>)sender
{
    DNDLOG("draggingEntered");
    return [self->_delegate sendJavaDndEvent:sender type:com_sun_glass_events_DndEvent_ENTER];
}

- (NSDragOperation)draggingUpdated:(id <NSDraggingInfo>)sender
{
    DNDLOG("draggingUpdated");
    return [self->_delegate sendJavaDndEvent:sender type:com_sun_glass_events_DndEvent_UPDATE];
}

- (void)draggingEnded:(id <NSDraggingInfo>)sender
{
    DNDLOG("draggingEnded");
    [self->_delegate draggingEnded];
}

- (void)draggingExited:(id <NSDraggingInfo>)sender
{
    DNDLOG("draggingExited");
    [self->_delegate sendJavaDndEvent:sender type:com_sun_glass_events_DndEvent_EXIT];
}

- (NSDragOperation)draggingSourceOperationMaskForLocal:(BOOL)isLocal
{
    // Deprecated for 10.7
    // use NSDraggingSession - (NSDragOperation)draggingSession:(NSDraggingSession *)session sourceOperationMaskForDraggingContext:(NSDraggingContext)context
    DNDLOG("draggingSourceOperationMaskForLocal");
    return [self->_delegate draggingSourceOperationMaskForLocal:isLocal];
}

#pragma mark --- Callbacks

- (void)enterFullscreenWithAnimate:(BOOL)animate withKeepRatio:(BOOL)keepRatio withHideCursor:(BOOL)hideCursor
{
    [self->_delegate enterFullscreenWithAnimate:animate withKeepRatio:keepRatio withHideCursor:hideCursor];
}

- (void)exitFullscreenWithAnimate:(BOOL)animate
{
    [self->_delegate exitFullscreenWithAnimate:animate];
}

- (void)begin
{
    LOG("begin");
    assert(self->_drawCounter >= 0);
    
    if (self->_drawCounter == 0)
    {
        GlassLayer3D *layer = (GlassLayer3D*)[self layer];
        NSRect bounds = (self->isHiDPIAware && [self respondsToSelector:@selector(convertRectToBacking:)]) ?
            [self convertRectToBacking:[self bounds]] : [self bounds];
        [[layer getPainterOffscreen] bindForWidth:(GLuint)bounds.size.width andHeight:(GLuint)bounds.size.height];
    }
    self->_drawCounter++;
}

- (void)end
{
    assert(self->_drawCounter > 0);
    
    self->_drawCounter--;
    if (self->_drawCounter == 0)
    {
        GlassLayer3D *layer = (GlassLayer3D*)[self layer];
        [[layer getPainterOffscreen] unbind];
        [layer flush];
    }
    LOG("end");
}

- (void)drawRect:(NSRect)dirtyRect
{
    [self->_delegate drawRect:dirtyRect];
}

- (void)pushPixels:(void*)pixels withWidth:(GLuint)width withHeight:(GLuint)height withEnv:(JNIEnv *)env
{
    assert(self->_drawCounter > 0);
    
    if (self->_texture == 0)
    {
        glGenTextures(1, &self->_texture);
    }
    
    BOOL uploaded = NO;
    if ((self->_textureWidth != width) || (self->_textureHeight != height))
    {
        uploaded = YES;
        
        self->_textureWidth = width;
        self->_textureHeight = height;
        
        // GL_EXT_texture_rectangle is defined in OS X 10.6 GL headers, so we can depend on GL_TEXTURE_RECTANGLE_EXT being available
        glBindTexture(GL_TEXTURE_RECTANGLE_EXT, self->_texture);
        glTexParameteri(GL_TEXTURE_RECTANGLE_EXT, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_RECTANGLE_EXT, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_RECTANGLE_EXT, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_RECTANGLE_EXT, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexImage2D(GL_TEXTURE_RECTANGLE_EXT, 0, GL_RGBA8, (GLsizei)self->_textureWidth, (GLsizei)self->_textureHeight, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
    }
    
    glEnable(GL_TEXTURE_RECTANGLE_EXT);
    glBindTexture(GL_TEXTURE_RECTANGLE_EXT, self->_texture);
    {
        if (uploaded == NO)
        {
            glTexSubImage2D(GL_TEXTURE_RECTANGLE_EXT, 0, 0, 0, (GLsizei)self->_textureWidth, (GLsizei)self->_textureHeight, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
        }
        
        GLfloat w = self->_textureWidth;
        GLfloat h = self->_textureHeight;
        
        NSSize size = [self bounds].size;
        if ((size.width != w) || (size.height != h))
        {
            // This could happen on live resize, clear the FBO to avoid rendering garbage
            glClear(GL_COLOR_BUFFER_BIT);
        }
        
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0f, size.width, size.height, 0.0f, -1.0f, 1.0f);
        {
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();
            {
                glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE); // copy
                
                glBegin(GL_QUADS);
                {
                    glTexCoord2f(0.0f, 0.0f); glVertex2f(0.0f, 0.0f);
                    glTexCoord2f(   w, 0.0f); glVertex2f(   w, 0.0f);
                    glTexCoord2f(   w,    h); glVertex2f(   w,    h);
                    glTexCoord2f(0.0f,    h); glVertex2f(0.0f,    h);
                }
                glEnd();
            }
            glMatrixMode(GL_MODELVIEW);
            glPopMatrix();
        }
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
    }
    glBindTexture(GL_TEXTURE_RECTANGLE_EXT, 0);
    glDisable(GL_TEXTURE_RECTANGLE_EXT);
    
    glFinish();

    // The layer will be notified about redraw in _end()
}

- (GlassViewDelegate*)delegate
{
    return self->_delegate;
}

- (void)setInputMethodEnabled:(BOOL)enabled
{
    IMLOG("setInputMethodEnabled called with arg is %s", (enabled ? "YES" : "NO") );
    [self unmarkText];
    self->imEnabled = enabled;
}

/*
 NSTextInputClient protocol implementation follows here.
 */

- (void)doCommandBySelector:(SEL)aSelector
{
    IMLOG("doCommandBySelector called ");
    // In case the IM was stopped with a mouse and the next typed key
    // is a special command key (backspace, tab, etc.)
    self->shouldProcessKeyEvent = YES;
}

- (void) insertText:(id)aString replacementRange:(NSRange)replacementRange
{
    IMLOG("insertText called with string: %s", [aString UTF8String]);
    if ([self->nsAttrBuffer length] > 0 || [aString length] > 1) { 
        [self->_delegate notifyInputMethod:aString attr:4 length:(int)[aString length] cursor:(int)[aString length] selectedRange: NSMakeRange(NSNotFound, 0)];
        self->shouldProcessKeyEvent = NO;
    } else {
        self->shouldProcessKeyEvent = YES;
    }
    self->nsAttrBuffer = [self->nsAttrBuffer initWithString:@""];
}

- (void) setMarkedText:(id)aString selectedRange:(NSRange)selectionRange replacementRange:(NSRange)replacementRange
{
    if (!self->imEnabled) {
        self->shouldProcessKeyEvent = YES;
        return;
    }
    BOOL isAttributedString = [aString isKindOfClass:[NSAttributedString class]];
    NSAttributedString *attrString = (isAttributedString ? (NSAttributedString *)aString : nil);
    NSString *incomingString = (isAttributedString ? [aString string] : aString);
    IMLOG("setMarkedText called, attempt to set string to %s", [incomingString UTF8String]);
    [self->_delegate notifyInputMethod:incomingString attr:1 length:0 cursor:(int)[incomingString length] selectedRange:selectionRange ];
    self->nsAttrBuffer = (attrString == nil ? [self->nsAttrBuffer initWithString:incomingString] 
                                            : [self->nsAttrBuffer initWithAttributedString: attrString]);
    self->shouldProcessKeyEvent = NO;
}

- (void) unmarkText
{
    IMLOG("unmarkText called\n");
    if (self->nsAttrBuffer != nil && self->nsAttrBuffer.length != 0) {
        self->nsAttrBuffer = [self->nsAttrBuffer initWithString:@""];
        [self->_delegate notifyInputMethod:@"" attr:4 length:0 cursor:0 selectedRange: NSMakeRange(NSNotFound, 0)];
    }
    self->shouldProcessKeyEvent = YES;
}

- (BOOL) hasMarkedText
{
    BOOL hmText = (self->imEnabled ? (self->nsAttrBuffer.length == 0 ? FALSE : TRUE) : FALSE);
    IMLOG("hasMarkedText called return %s ", (hmText ? "true" : "false"));
    return hmText;
}

- (NSRange) markedRange
{
    IMLOG("markedRange called, return range in attributed string");
    if (self->imEnabled) {
        return NSMakeRange(0, self->nsAttrBuffer.length);
    } else {
        return NSMakeRange(NSNotFound, 0);
    }
}

- (NSAttributedString *) attributedSubstringForProposedRange:(NSRange)theRange actualRange:(NSRangePointer)actualRange
{
    IMLOG("attributedSubstringFromRange called: location=%lu, length=%lu", 
            (unsigned long)theRange.location, (unsigned long)theRange.length);
    if (self->imEnabled) {
        return self->nsAttrBuffer;
    } else {
        return NULL;
    }
}

- (NSRange) selectedRange
{
    IMLOG("selectedRange called");
    if (self->imEnabled) {
        return NSMakeRange(0, [[self->nsAttrBuffer string] length]);
    } else {
        return NSMakeRange(NSNotFound, 0);
    }
}

- (NSRect) firstRectForCharacterRange:(NSRange)theRange actualRange:(NSRangePointer)actualRange
{
    IMLOG("firstRectForCharacterRange called %lu %lu",
          (unsigned long)theRange.location, (unsigned long)theRange.length);
    NSRect result = [self->_delegate getInputMethodCandidatePosRequest:0];
    NSRect screenFrame = [[NSScreen mainScreen] frame];
    result.origin.y = screenFrame.size.height - result.origin.y;
    return result;
}

- (NSUInteger)characterIndexForPoint:(NSPoint)thePoint
{
    IMLOG("characterIndexForPoint (%f, %f) called", thePoint.x, thePoint.y);
    return 0;
}

- (NSArray*) validAttributesForMarkedText
{
    return [NSArray array];
}

- (void)notifyScaleFactorChanged:(CGFloat)scale
{
    GlassLayer3D *layer = (GlassLayer3D*)[self layer];
    [layer notifyScaleFactorChanged:scale];
}

/* Accessibility support */

- (NSArray *)accessibilityAttributeNames
{
    NSArray *names = NULL;
    GlassAccessible *accessible = [self->_delegate getAccessible];
    if (accessible) {
        names = [accessible accessibilityAttributeNames];
    }
    if (names == NULL) {
        names = [super accessibilityAttributeNames];
    }
    return names;
}

- (id)accessibilityAttributeValue:(NSString *)attribute
{
    id value = NULL;
    GlassAccessible *accessible = [self->_delegate getAccessible];
    if (accessible) {
        value = [accessible accessibilityAttributeValue: attribute];
    }
    if (value == NULL) {
        value = [super accessibilityAttributeValue: attribute];
    }
    return value;
}

- (BOOL)accessibilityIsIgnored
{
    BOOL value = TRUE; /* This default value in the OS */
    GlassAccessible *accessible = [self->_delegate getAccessible];
    if (accessible) {
        value = [accessible accessibilityIsIgnored];
    }
    return value;
}

- (id)accessibilityHitTest:(NSPoint)point
{
    id value = NULL;
    GlassAccessible *accessible = [self->_delegate getAccessible];
    if (accessible) {
        value = [accessible accessibilityHitTest: point];
    }
    if (value == NULL) {
        value = [super accessibilityHitTest: point];
    }
    return value;
}

- (id)accessibilityFocusedUIElement
{
    id value = NULL;
    GlassAccessible *accessible = [self->_delegate getAccessible];
    if (accessible) {
        value = [accessible accessibilityFocusedUIElement];
    }
    if (value == NULL) {
        value = [super accessibilityFocusedUIElement];
    }
    return value;
}


@end
