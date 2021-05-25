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
#import "GlassView.h"
#import "GlassDragDelegate.h"

//
@interface GlassMainWindow : UIWindow {

}
-(void) sendEvent:(UIEvent *)event;
@end

@interface GlassMainView : UIView {

}

@end

@interface GlassWindow : UIView<UIKeyInput>
{
    jobject             jWindow; // Glass java Window object

@public

    UIView              *owner;
    UIView<GlassView>   *view; // GlassView owned by this GlassWindow
    UIView              *hostView; // GlassWindow's subview and container of GlassView, Webnode etc.
    GlassWindow         *parentWindow;

    BOOL                isFocusable; // if isFocusable and isEnabled, then can become key window
    BOOL                isTransparent; // YES if window is not opaque
    BOOL                isResizable;
    BOOL                suppressWindowMoveEvent; // don't notify Java about position change
    BOOL                suppressWindowResizeEvent; // don't notify Java about dimensions change

    // temporarily hold new frame origin, size, etc. when updating frame on the main NSThread
    CGFloat             _setFrameX, _setFrameY, _setFrameWidth, _setFrameHeight;
    jboolean            _setFrameDisplay, _setFrameAnimated;

    jint                _setLevel;

    jfloat              _setAlpha; // temporarily hold alpha value while updating it on main NSThread

    BOOL                isEnabled; //see is Focusable
    jfloat              minWidth, minHeight, maxWidth, maxHeight;

    NSMutableArray      *childWindows; // This GlassWindow is parentWindow of its childWindows
}

// Toplevel containers of all GlassWindows
// once we support multiple screens on iOS - there will be one mainWindow/
// mainWindowHost per screen; These windows are not part of FX/Glass window hierarchy, they
// serve us as OS containers. They allow us to easily change orientation for all GlassWindows, etc.
+(GlassMainWindow *)  getMainWindow;
+(GlassMainView *) getMainWindowHost;

- (void)setEnabled:(BOOL)enabled; // see isFocusable
- (void)_setTransparent:(BOOL)state;
- (void)close;

// display system keyboard and editable textfield allowing user to enter/edit text
// here we differentiate from other platforms with accessible key events
- (void)requestInput:(NSString *)text type:(int)type width:(double)width height:(double)height
                 mxx:(double)mxx mxy:(double)mxy mxz:(double)mxz mxt:(double)mxt
                 myx:(double)myx myy:(double)myy myz:(double)myz myt:(double)myt
                 mzx:(double)mzx mzy:(double)mzy mzz:(double)mzz mzt:(double)mzt;
- (void)releaseInput;

- (void)becomeKeyWindow; // window become key window (receives keyboard and other non-touch events)
- (void)resignKeyWindow; // window is not key any more
- (void)makeKeyWindow;   // request window to become key
- (BOOL)isKeyWindow;

@end
