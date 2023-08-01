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

#import "GlassTitleBar.h"

static const CGFloat kMinTitleBarHeight = 14.0;
static const CGFloat kDefaultTitleBarHeight = 28.0;

// A view that encapsulates the close/minimize/maximize buttons
@interface StoplightView : NSView
{
@private
    BOOL mouseInside;
}
-(void)redrawButtons;
@end

@implementation StoplightView
-(instancetype)initWithFrame:(NSRect)frame
{
    self = [super initWithFrame: frame];
    if (self)
    {
        NSTrackingArea* tracking = [[NSTrackingArea alloc] initWithRect: self.bounds
                                                                options: NSTrackingActiveAlways | NSTrackingInVisibleRect | NSTrackingMouseEnteredAndExited
                                                                  owner: self
                                                               userInfo: nil];
        [self addTrackingArea: tracking];
        mouseInside = NO;
    }
    return self;
}

-(void)viewDidMoveToWindow
{
    [NSNotificationCenter.defaultCenter addObserver: self
                                           selector: @selector(redrawButtons)
                                               name: NSWindowDidBecomeKeyNotification
                                             object: self.window];
    [NSNotificationCenter.defaultCenter addObserver: self
                                           selector: @selector(redrawButtons)
                                               name: NSWindowDidResignKeyNotification
                                             object: self.window];
}

-(void)viewWillMoveToWindow:(NSWindow *)newWindow
{
    [NSNotificationCenter.defaultCenter removeObserver: self];
}

-(void)redrawButtons
{
    for (NSView* view in self.subviews)
    {
        [view setNeedsDisplay: YES];
    }
}

-(void)mouseEntered:(NSEvent *)event
{
    mouseInside = YES;
    [self redrawButtons];
}

-(void)mouseExited:(NSEvent *)event
{
    mouseInside = NO;
    [self redrawButtons];
}

// An undocumented but well-known API. When the mouse enters
// the area encompassing the spotlight buttons they all show
// their icons at the same time. Their superview needs to
// implement this method to make that magic work.
-(BOOL)_mouseInGroup:(NSButton*)button
{
    return mouseInside;
}
@end

@interface GlassTitleBar ()
@property (readwrite) NSEdgeInsets insets;
@end

@implementation GlassTitleBar
{
@private
    NSWindow*                   window;

    // The jfxView contains the surface that JavaFX draws on. The hostView
    // manages other views below and above the jfxView.
    NSView*                     hostView;
    NSView*                     jfxView;

    CGFloat                     titleBarHeight;

    // We cannot draw on top of the platform title bar (we can only draw beneath
    // it and that content would get blurred). We set the window's title bar to
    // be invisible and recreate the title bar effect manually by placing this
    // view below the JFX view.
    NSVisualEffectView*         titleBarBackground;

    // We cannot control the position of the stoplight buttons. Instead we hide
    // the original stoplight buttons and create duplicates that we can control.
    // This view is positioned above the jfx view.
    StoplightView*              stoplightView;

    BOOL                        useOriginals;
    NSMutableDictionary<NSNumber*, NSButton*>* buttons;
    NSMutableDictionary<NSNumber*, NSButton*>* originals;
}

- (instancetype)initWithWindow:(NSWindow*)w
{
    // The window must have the NSWindowStyleMaskFullSizeContentView
    // flag set.
    self = [super init];
    if (self != nil)
    {
        window = w;
        window.titleVisibility = NSWindowTitleHidden;
        window.titlebarAppearsTransparent = YES;

        hostView = nil;
        jfxView = nil;

        titleBarHeight = kDefaultTitleBarHeight;
        _insets = NSEdgeInsetsZero;

        titleBarBackground = [[NSVisualEffectView alloc] initWithFrame: NSZeroRect];

        // Currently this is producing a darker title bar than expected. The
        // title bar effect blends window state (such as whether the window has
        // focus or not) with the content underneath. This includes the NSWindow
        // background which JavaFX defaults to black.
        titleBarBackground.material = NSVisualEffectMaterialTitlebar;
        titleBarBackground.blendingMode = NSVisualEffectBlendingModeWithinWindow;

        stoplightView = [[StoplightView alloc] initWithFrame: NSZeroRect];
        useOriginals = NO;
        buttons = [[NSMutableDictionary dictionary] retain];
        originals = [[NSMutableDictionary dictionary] retain];

        [self addButton: NSWindowCloseButton];
        [self addButton: NSWindowMiniaturizeButton];
        [self addButton: NSWindowZoomButton];

        [self positionStoplightView];

        [NSNotificationCenter.defaultCenter addObserver: self
                                               selector: @selector(restoreOriginalButtons)
                                                   name: NSWindowWillEnterFullScreenNotification
                                                 object: window];

        // Before we exit fullscreen we ensure the layout insets are broadcast
        // The buttons themselves cannot be restored until after we've left
        // fullscreen.
        [NSNotificationCenter.defaultCenter addObserver: self
                                               selector: @selector(leavingFullscreen)
                                                   name: NSWindowWillExitFullScreenNotification
                                                 object: window];

        [NSNotificationCenter.defaultCenter addObserver: self
                                               selector: @selector(hideOriginalButtons)
                                                   name: NSWindowDidExitFullScreenNotification
                                                 object: window];
    }
    return self;
}

-(void)dealloc
{
    [buttons release];
    [originals release];
    [titleBarBackground release];
    [stoplightView release];
    [super dealloc];
}

-(void)addButton:(NSWindowButton)b
{
    NSButton* button = [NSWindow standardWindowButton: b forStyleMask: window.styleMask];
    NSButton* original = [window standardWindowButton: b];
    button.enabled = original.enabled;
    original.hidden = YES;

    NSRect frame = NSOffsetRect(button.frame, NSWidth(stoplightView.frame), 0);
    if (!NSIsEmptyRect(stoplightView.frame))
        frame = NSOffsetRect(frame, 6, 0);
    stoplightView.frame = NSUnionRect(stoplightView.frame, frame);
    button.frame = frame;
    [stoplightView addSubview: button];

    buttons[@(b)] = button;
    originals[@(b)] = original;
}

-(void)restoreOriginalButtons
{
    stoplightView.hidden = YES;

    for (NSNumber* key in originals.allKeys)
    {
        NSButton* original = originals[key];
        NSButton* button = buttons[key];
        original.enabled = button.enabled;
        original.hidden = NO;
    }

    useOriginals = YES;

    [self positionStoplightView];
}

-(void)hideOriginalButtons
{
    for (NSNumber* key in originals.allKeys)
    {
        NSButton* original = originals[key];
        NSButton* button = buttons[key];
        button.enabled = original.enabled;
        original.hidden = YES;
    }

    stoplightView.hidden = NO;
    useOriginals = NO;

    [self positionStoplightView];
}

-(void)leavingFullscreen
{
    stoplightView.hidden = NO;
    [self positionStoplightView];
}

-(NSButton*)standardWindowButton:(NSWindowButton)slot
{
    if (useOriginals)
        return originals[@(slot)];
    return buttons[@(slot)];;
}

-(void)detachFromHostView
{
    [titleBarBackground removeFromSuperview];
    [stoplightView removeFromSuperview];

    hostView = nil;
    jfxView = nil;
}

-(void)detachFromWindow
{
    [self detachFromHostView];
    if (!useOriginals)
        [self restoreOriginalButtons];
    window.titleVisibility = NSWindowTitleVisible;
    window.titlebarAppearsTransparent = NO;
    window = nil;
}

-(void)setHostView:(NSView*)newHostView jfxView:(NSView*)newJFX
{
    [self detachFromHostView];
    hostView = newHostView;
    jfxView = newJFX;

    if (hostView)
    {
        // The title bar effect needs to be below the
        // JFX content to avoid blurring.
        NSRect titleBarFrame = hostView.bounds;
        titleBarFrame.size.height = titleBarHeight;
        titleBarBackground.frame = titleBarFrame;
        titleBarBackground.autoresizingMask = NSViewWidthSizable;

        [hostView addSubview: titleBarBackground
                  positioned: NSWindowBelow
                  relativeTo: jfxView];

        // The buttons need to be above
        [hostView addSubview: stoplightView
                  positioned: NSWindowAbove
                  relativeTo: jfxView];

        [self positionStoplightView];
        [stoplightView redrawButtons];
    }
}

-(CGFloat)height
{
    return titleBarHeight;
}

-(void)setHeight:(CGFloat)height
{
    if (height < kMinTitleBarHeight)
        height = kMinTitleBarHeight;

    if (height != titleBarHeight)
    {
        titleBarHeight = height;
        if (titleBarBackground) {
            NSRect frame = titleBarBackground.frame;
            frame.size.height = titleBarHeight;
            titleBarBackground.frame = frame;
        }
    }
    [self positionStoplightView];
}

-(void)positionStoplightView
{
    NSPoint closeCenter;
    closeCenter.x = titleBarHeight / 2.0;
    closeCenter.y = titleBarHeight / 2.0;

    NSButton* closeButton = buttons[@(NSWindowCloseButton)];
    NSSize centeringOffset = NSMakeSize(NSWidth(closeButton.frame) / 2.0, NSHeight(closeButton.frame) / 2.0);

    NSRect frame = stoplightView.frame;
    frame.origin.x = closeCenter.x - centeringOffset.width;
    frame.origin.y = closeCenter.y - centeringOffset.height;
    stoplightView.frame = frame;

    NSEdgeInsets insets;
    insets.top = insets.bottom = 0;
    insets.left = NSMaxX(frame) + 6;
    insets.right = 6;

    if (stoplightView.hidden)
    {
        insets.left = 6;
    }

    if (!NSEdgeInsetsEqual(insets, _insets))
    {
        self.insets = insets;
    }
}

// It is expected that the JFX view will implement hitTest: and
// mouseDown: to intercept and process events for the controls it
// draws. If the mouse down didn't hit a control it should fall
// through to the host view which will call this routine.
-(void)handleMouseDown:(NSEvent *)event
{
    NSPoint point = [hostView convertPoint: event.locationInWindow fromView: nil];
    if (point.y > titleBarHeight)
        return;

    if (event.clickCount == 1)
    {
        [window performWindowDragWithEvent: event];
    }
    else if (event.clickCount == 2)
    {
        NSString* action = [NSUserDefaults.standardUserDefaults stringForKey: @"AppleActionOnDoubleClick"];
        if ([action isEqualToString: @"Minimize"])
            [window performMiniaturize: nil];
        else if ([action isEqualToString: @"Maximize"])
            [window performZoom: nil];
    }
}
@end

