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

#import "GlassTimer.h"
#import "GlassViewGL.h"
#import "GlassWindow.h"
#import "com_sun_glass_events_KeyEvent.h"
#import "com_sun_glass_events_MouseEvent.h"
#import "com_sun_glass_ui_View_Capability.h"

//shared EAGLContext created in prism-es2 pipeline
//and passed to glass
static EAGLContext *clientContext = nil;

//main UIKit's EAGLContext - same sharegroup as clientContext
static EAGLContext * ctx = nil;


@implementation GLView

+ (Class)layerClass {
    return [CAEAGLLayer class];
}


- (id)initWithFrame:(CGRect)frame withClientContext:(EAGLContext*)clientEAGLContext withJProperties:(jobject)jproperties {

    self = [super initWithFrame:frame];
    if (self) {

        GET_MAIN_JENV;
        jmethodID initMethod = (*env)->GetMethodID(env, mat_jIntegerClass, "<init>", "(I)V");

        self->isHiDPIAware = NO;
        if (jproperties != NULL)
        {
            jobject kHiDPIAwareKey = (*env)->NewObject(env, mat_jIntegerClass, initMethod, com_sun_glass_ui_View_Capability_kHiDPIAwareKeyValue);
            jobject kHiDPIAwareValue = (*env)->CallObjectMethod(env, jproperties, mat_jMapGetMethod, kHiDPIAwareKey);
            if (kHiDPIAwareValue != NULL)
            {
                self->isHiDPIAware = (*env)->CallBooleanMethod(env, kHiDPIAwareValue, mat_jBooleanValueMethod) ? YES : NO;
            }
        }
        [self setContentScaleFactor:(self->isHiDPIAware ?[[UIScreen mainScreen] scale]:1.0)];
        [self setContentMode:UIViewContentModeTopLeft];

        CAEAGLLayer * layer = (CAEAGLLayer*) self.layer;

        layer.opaque = NO;

        //increase clientContext retain count
        clientContext = [clientEAGLContext retain];

        if (ctx == nil) {
            layer.opaque = YES;
            ctx = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2 sharegroup:[clientContext sharegroup]];
        } else {
            ctx = [ctx retain];
        }

        if (![EAGLContext setCurrentContext: ctx]) {
            GLASS_LOG("Failed to set current context");
            return self;
        }

        glGenFramebuffers(1, &frameBuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

        glGenRenderbuffers(1, &renderBuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer);

        [ctx renderbufferStorage:GL_RENDERBUFFER fromDrawable: layer];


        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, renderBuffer);

        glFlush();

        GLASS_LOG("Created GLView - context %@, renderbuffer is %d , framebuffer is %d",
                     ctx, renderBuffer, frameBuffer);

    }
    return self;
}


- (void)dealloc {
    //release OpenGL resources
    glDeleteRenderbuffers(1, &renderBuffer);
    glDeleteFramebuffers(1, &frameBuffer);
    [clientContext release];
    [ctx release];

    [super dealloc];
}

@end


@implementation GlassViewGL : GLView

-(void) doInsertText:(NSString*)myText {
    int unicode = [myText characterAtIndex:0];
    int code = com_sun_glass_events_KeyEvent_VK_UNDEFINED;
    if (unicode == com_sun_glass_events_KeyEvent_VK_ENTER) {
         code = unicode;
    }
    [self->delegate sendJavaKeyEventWithType:com_sun_glass_events_KeyEvent_PRESS
        keyCode:code unicode:unicode modifiers:0];
    [self->delegate sendJavaKeyEventWithType:com_sun_glass_events_KeyEvent_TYPED
        keyCode:code unicode:unicode modifiers:0];
    [self->delegate sendJavaKeyEventWithType:com_sun_glass_events_KeyEvent_RELEASE
        keyCode:code unicode:unicode modifiers:0];
}

-(void) doDeleteBackward {
    int unicode = com_sun_glass_events_KeyEvent_VK_BACKSPACE;
    [self->delegate sendJavaKeyEventWithType:com_sun_glass_events_KeyEvent_PRESS
        keyCode:unicode unicode:unicode modifiers:0];
    [self->delegate sendJavaKeyEventWithType:com_sun_glass_events_KeyEvent_TYPED
        keyCode:unicode unicode:unicode modifiers:0];
    [self->delegate sendJavaKeyEventWithType:com_sun_glass_events_KeyEvent_RELEASE
        keyCode:unicode unicode:unicode modifiers:0];
}

-(BOOL) touchesShouldBegin:(NSSet *)touches withEvent:(UIEvent *)event inContentView:(UIView *)view
{
    return YES;
}

-(BOOL) touchesShouldCancelInContentView:(UIView *)view
{
    return [super touchesShouldCancelInContentView:view];
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    [self->delegate touchesBeganCallback:touches withEvent:event];
}


- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    [self->delegate touchesMovedCallback:touches withEvent:event];
}


- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    [self->delegate touchesEndedCallback:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [self->delegate touchesCancelledCallback:touches withEvent:event];
}


- (id)initWithFrame:(CGRect)frame withJview:(jobject)jView withJproperties:(jobject)jproperties
{
    GET_MAIN_JENV;

    EAGLContext * clientContext = NULL;
    {
        // EAGLContext pointer passed from java
        jobject contextPtrKey = (*env)->NewStringUTF(env, "contextPtr");
        jobject contextPtrValue = (*env)->CallObjectMethod(env, jproperties, mat_jMapGetMethod, contextPtrKey);
        if (contextPtrValue != NULL)
        {
            jlong jcontextPtr = (*env)->CallLongMethod(env, contextPtrValue, mat_jLongValueMethod);
            if (jcontextPtr != 0)
            {
                clientContext = (EAGLContext*)jlong_to_ptr(jcontextPtr);
            }
        }

    }


    self = [super initWithFrame: frame withClientContext:clientContext withJProperties:(jobject)jproperties];
    GLASS_LOG("in GlassViewGL:initWithFrame ... self == %p, frame %@", self, NSStringFromCGRect(frame));

    if (self != nil)
    {
        GET_MAIN_JENV;
        jmethodID initMethod = (*env)->GetMethodID(env, mat_jIntegerClass, "<init>", "(I)V");

        self->delegate = [[GlassViewDelegate alloc] initWithView:self withJview:jView];

        {
            jobject jSyncKey = (*env)->NewObject(env, mat_jIntegerClass, initMethod, com_sun_glass_ui_View_Capability_kSyncKeyValue);
            jobject jSyncKeyValue = (*env)->CallObjectMethod(env, jproperties, mat_jMapGetMethod, jSyncKey);
            if (jSyncKeyValue != NULL)
            {
                (*env)->CallBooleanMethod(env, jSyncKeyValue, mat_jBooleanValueMethod);
            }
        }


        // UIScrollView configuration. We're emulating scrolling, so don't show the
        // scrollbars, and immediately deliver touches to the view.
        [self setShowsHorizontalScrollIndicator:NO];
        [self setShowsVerticalScrollIndicator:NO];
        [self setDelaysContentTouches:NO];
        [self setCanCancelContentTouches:NO];
        [self setDirectionalLockEnabled:NO];

        if (displayLink == NULL) {
            // A system version of 3.1 or greater is required to use CADisplayLink. The NSTimer
            // class is used as fallback when it isn't available.
            NSString *reqSysVer = @"3.1";
            NSString *currSysVer = [[UIDevice currentDevice] systemVersion];
            GLASS_LOG("GlassViewGL: reqSysVer %@ currSysVer %@", reqSysVer, currSysVer);

            if ([currSysVer compare:reqSysVer options:NSNumericSearch] != NSOrderedAscending) {
                displayLink = [[UIScreen mainScreen] displayLinkWithTarget:[GlassTimer getDelegate]
                                                                  selector:@selector(displayLinkUpdate:)];
                [displayLink addToRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
                [displayLink addToRunLoop:[NSRunLoop currentRunLoop] forMode:UITrackingRunLoopMode];
                GLASS_LOG("GlassViewGL: displayLink SET");
            }
        }
        /*
         * This triggers a ViewEvent.REPAINT which triggers the GlassViewEventHandler
         * to perform a live repaint.  All other pulses are triggered from displayLinkUpdate:
         */
        [self setNeedsDisplay];
    }

    return self;
}


- (void)dealloc
{
    [self->delegate release];
     self->delegate = nil;

    if (!nativeView) [nativeView release];
    [super dealloc];
}


- (BOOL)becomeFirstResponder
{
    return YES;
}

- (BOOL)canResignFirstResponder
{
    return YES;
}

- (BOOL)canBecomeFirstResponder
{
    return YES;
}

- (BOOL)isOpaque
{
    return NO;
}

// also called when closing window, when [self window] == nil
- (void)didMoveToWindow
{
    [self->delegate viewDidMoveToWindow];
}

#pragma mark -
#pragma mark Layout

// recenter content periodically to achieve impression of infinite scrolling
- (void)layoutSubviews {
    [super layoutSubviews];
    CGPoint currentOffset = [self contentOffset];
    CGFloat contentWidth = [self contentSize].width;
    CGFloat contentHeight = [self contentSize].height;
    CGFloat centerOffsetX = (contentWidth - [self bounds].size.width) / 2.0;
    CGFloat centerOffsetY = (contentHeight - [self bounds].size.height) / 2.0;
    CGFloat xDistanceFromCenter = fabs(currentOffset.x - centerOffsetX);
    CGFloat yDistanceFromCenter = fabs(currentOffset.y - centerOffsetY);

    if (xDistanceFromCenter > (contentWidth / 4.0) ||
        yDistanceFromCenter > (contentHeight / 4.0)) {
        [self->delegate contentWillRecenter];
        self.contentOffset = CGPointMake(centerOffsetX, centerOffsetY);
    }
}

- (void)_setBounds
{
    [super setFrame:self->_bounds];
    [self->delegate setBounds:self->_bounds];

    CGRect viewFrame = self.frame;
    self.contentSize = CGSizeMake(viewFrame.size.width * 4, viewFrame.size.height * 4);
}


- (void)setFrame:(CGRect)boundsRect
{
    GLASS_LOG("GlassViewGL.setBounds %f,%f,%f, %f ", boundsRect.origin.x, boundsRect.origin.y,boundsRect.size.width,boundsRect.size.height);

    self->_bounds = boundsRect;

    if ([[NSThread currentThread] isMainThread] == YES) {
        [self _setBounds];
    } else {
        [self performSelectorOnMainThread:@selector(_setBounds) withObject:nil waitUntilDone:YES];
    }
}

// Called by the client whenever it draws (View.lock()->Pen.begin()->here)
- (void)begin
{
    // assert([EAGLContext currentContext] == clientContext);
    if ([EAGLContext currentContext] != clientContext) {
        [EAGLContext setCurrentContext:clientContext];
    }

    if (clientContext != nil) {
        GLint currentFrameBuffer, currentRenderBuffer;
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, (GLint *) & currentFrameBuffer);
        glGetIntegerv(GL_RENDERBUFFER_BINDING, (GLint *) & currentRenderBuffer);


        //rebind framebuffer / renderbuffer if neccessary
        if (currentRenderBuffer != renderBuffer) {
            glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer);
        }

        if (currentFrameBuffer != frameBuffer) {
            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        }

        GLint width, height;
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &width);
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &height);

        CAEAGLLayer * layer = (CAEAGLLayer*) self.layer;

        if ((layer.bounds.size.width * layer.contentsScale) != width ||
            (layer.bounds.size.height * layer.contentsScale) != height) {
            GLASS_LOG("Resizing renderBufferStorage (original size == %d,%d) new size == %f,%f ",
                  width, height, layer.bounds.size.width, layer.bounds.size.height);

            [clientContext renderbufferStorage:GL_RENDERBUFFER fromDrawable: layer];
        }
    }

    GLASS_LOG("GlassViewGL.begin for %@, current %@, renderBuffer %d, frameBuffer %d",self,[EAGLContext currentContext], renderBuffer, frameBuffer);
    GLASS_LOG("BEGIN THREAD %@",[NSThread currentThread]);

    // we could clear the surface for the client, but the client should be responsible for drawing
    // and if garbage appears on the screen it's because the client is not drawing in response to system repaints
    // glClear(GL_COLOR_BUFFER_BIT);

    // now we are good to paint
}


- (void)end
{
    GLASS_LOG("END THREAD %@",[NSThread currentThread]);
    GLASS_LOG("GlassViewGL.end for %@, current %@, clientContext %@",self,[EAGLContext currentContext], clientContext);
    assert([EAGLContext currentContext] == clientContext);
}

// send also font size and font family, bg color, text color, baseline, ...?
- (void)requestInput:(NSString *)text type:(int)type width:(double)width height:(double)height
                 mxx:(double)mxx mxy:(double)mxy mxz:(double)mxz mxt:(double)mxt
                 myx:(double)myx myy:(double)myy myz:(double)myz myt:(double)myt
                 mzx:(double)mzx mzy:(double)mzy mzz:(double)mzz mzt:(double)mzt
{

    if (type == 0 || type == 1) { // TextField or PasswordField

        UITextField* textField = [[UITextField alloc] initWithFrame:CGRectMake(mxt + 1, myt + 1, width - 2, height - 2)];

        textField.text = text;

        [self setUpKeyboardForText:(id)textField];

        if (type == 1) {
            textField.secureTextEntry = YES; // Password field behavior
        }

        [self setUpLayerForText:(id)textField];

        textField.font = [UIFont systemFontOfSize:15];
        textField.inputAccessoryView = inputAccessoryView;
        textField.contentVerticalAlignment = UIControlContentVerticalAlignmentCenter;
        textField.borderStyle = UITextBorderStyleNone;
        textField.layer.borderColor =[[UIColor clearColor] CGColor];
        // textField.backgroundColor = [UIColor clearColor];

        textField.delegate = self->delegate;

        nativeView = textField;

    } else if (type == 3) { // TextArea

        UITextView* textView = [[UITextView alloc] initWithFrame:CGRectMake(mxt + 1, myt + 1, width - 2, height - 2)];

        textView.text = text;

        [self setUpKeyboardForText:(id)textView];

        [self setUpLayerForText:(id)textView];

        textView.font = [UIFont systemFontOfSize:15];
        textView.inputAccessoryView = inputAccessoryView;

        nativeView = textView;

    }

    if (![[self.superview subviews] containsObject:nativeView]) {

        [self.superview addSubview:nativeView];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(textChanged:)
                                                     name:UITextViewTextDidChangeNotification
                                                   object:nativeView];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(textChanged:)
                                                     name:UITextFieldTextDidChangeNotification
                                                   object:nativeView];

        [nativeView becomeFirstResponder];
    }
}

- (void)setUpKeyboardForText:(id) view
{
    if ([view isMemberOfClass:[UITextField class]] || [view isMemberOfClass:[UITextView class]]) {
        [view setAutocorrectionType:UITextAutocorrectionTypeNo];
        [view setAutocapitalizationType:UITextAutocapitalizationTypeNone];
        [view setSpellCheckingType:UITextSpellCheckingTypeNo];
        [view setReturnKeyType:UIReturnKeyDefault];
        [view setKeyboardType:UIKeyboardTypeASCIICapable];
    }
}

- (void)setUpLayerForText:(id) view
{
    if ([view isMemberOfClass:[UITextField class]] || [view isMemberOfClass:[UITextView class]]) {
        [[view layer] setBackgroundColor:[[UIColor whiteColor] CGColor]];
        [[view layer] setBorderColor:[[UIColor colorWithRed:0.8 green:0.8 blue:0.8 alpha:1.0] CGColor]];
        [[view layer] setBorderWidth:1.0f];
        [[view layer] setCornerRadius:3.0f];
        [[view layer] setMasksToBounds:YES];
    }
}

- (void)releaseInput
{
    if (nativeView) {
        [[NSNotificationCenter defaultCenter] removeObserver:self name:UITextViewTextDidChangeNotification object:nativeView];
        [[NSNotificationCenter defaultCenter] removeObserver:self name:UITextFieldTextDidChangeNotification object:nativeView];
        [nativeView resignFirstResponder];
        [nativeView removeFromSuperview];

        [nativeView release];
        nativeView = nil;
    }
}

- (UIView *)inputAccessoryView
{
    if (!inputAccessoryView) {

        UIToolbar *tlbr = [[UIToolbar alloc] init];
        tlbr.barStyle = UIBarStyleBlackTranslucent;
        [tlbr sizeToFit];

        UIBarButtonItem *cancelBtn =[[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemCancel target:self action:@selector(cancelClicked)];
        UIBarButtonItem *flexible = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
        UIBarButtonItem *doneBtn =[[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone target:self action:@selector(doneClicked)];

        [tlbr setItems:[NSArray arrayWithObjects:cancelBtn, flexible, doneBtn, nil]];

        [cancelBtn release];
        [doneBtn release];
        [flexible release];

        inputAccessoryView = tlbr;

    }
    return inputAccessoryView;
}

- (void)textChanged:(NSNotification *) notification
{
    if ([notification object] != nativeView) return;

    NSString *str = [[notification object] text];

    [self->delegate sendJavaInputMethodEvent:str
                              clauseBoundary:nil
                                attrBoundary:nil
                                   attrValue:nil
                         committedTextLength:[str length]
                                    caretPos:0
                                  visiblePos:0];
}

- (void) cancelClicked
{
    GLASS_LOG("User canceled entering text.");
    [self releaseInput];
}

- (void) doneClicked
{
    [self releaseInput];
}

@end

