/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
#import "com_sun_glass_events_MouseEvent.h"
#import "com_sun_glass_events_KeyEvent.h"
#import "com_sun_glass_events_TouchEvent.h"
#import "com_sun_glass_events_DndEvent.h"
#import "com_sun_glass_ui_Clipboard.h"

#import "GlassStatics.h"
#import "GlassMacros.h"

#import "GlassViewGL.h"

#import "GlassDragDelegate.h"

//#define VERBOSE_DND
#ifdef VERBOSE_DND

#define DNDLOG NSLog

#else

#define DNDLOG(...)

#endif

static NSObject<GlassDragSourceDelegate> *gDelegate = nil;
static jint gMask = com_sun_glass_ui_Clipboard_ACTION_NONE; // Current DnD clipboard state; allowed operation mask

static BOOL          dragging = NO;// Are we inside drag and drop session?

static UIImageView * dragImage = nil;// Image(s) shown during drag and drop

static UIView      * dragViewParent;// parental UIView of all GlassWindows (aka mainHostView); do not retain
static UIView *      dragSourceView;// GlassView where drag operation started

static jobject lastJavaViewDragTarget = NULL; // last view where mouse occured during dragging session
static CGPoint lastDragPositionInDragTarget;  // position of last dragging event in local coordinates of
                                              // lastJavaViewDragTarget

static CGPoint dragSourceLocation;// Point where drag/drop session started. When session ends without performing
                                  // drop operation, then we animate dragImage back to this position.
static jint operation; // Dragging operation supported in this session

@implementation GlassDragDelegate

+ (void) setDragViewParent:(UIView*)parent
{
    dragViewParent = parent;
}

// try to start new drag session
+ (void) drag:(CGPoint)_dragSourceLocation operation:(jint)_operation glassView:(UIView*)_glassView
{
    // We don't allow new drag/drop session before previous is finished
    if (dragging == NO) {
        DNDLOG(@"Starting drag - operation == %ld", operation);
        dragging = YES;

        operation = _operation;
        dragSourceView = _glassView;

        dragSourceLocation = _dragSourceLocation;

        lastJavaViewDragTarget = NULL;

        // Create and show drag bitmap for better DnD feeling
        if (dragImage == nil) {
            dragImage = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"drag.png"] highlightedImage:[UIImage imageNamed:@"drop.png"]];
        }

        // move drag image a little bit away from under the finger so it is visible
        dragImage.transform = CGAffineTransformMakeTranslation(dragImage.bounds.size.width/3, - dragImage.bounds.size.height/3);

        [dragImage setCenter:dragSourceLocation];

        [GlassDragDelegate showImage:NO];

        DNDLOG(@"[GlassDragDelegate getMask] == %ld",[GlassDragDelegate getMask]);
    }
}

+ (BOOL) isDragging
{
    return dragging;
}

// we are draging already; new touches are not interesting for us at the moment
+ (void) touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event withMouse:(UITouch *)mouse
{
    DNDLOG(@"DRAGGING - touchesBegan");
}

// get GlassView under the finger (possible target)
+ (UIView *) getDragTargetView:(UITouch*)mouse
{
    UIView * topMostView = dragSourceView;
    for (UIView * hostView in [dragViewParent subviews]) {
        for (UIView * gw in [hostView subviews]) {
            CGPoint mousePoint = [mouse locationInView:gw];
            UIView * hitView = [gw hitTest:mousePoint withEvent:nil];

            if ([hitView isKindOfClass:[GlassViewGL class]]) {
                topMostView = hitView;
            }
        }
    }
    return topMostView;
}

+ (void) touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event withMouse:(UITouch *)mouse
{
    DNDLOG(@"DRAGGING - touchesMoved");
    if (dragging == YES) {
        [dragImage setCenter:[mouse locationInView:dragViewParent]];

        GlassViewGL * dragTargetView = (GlassViewGL *)[GlassDragDelegate getDragTargetView:mouse];
        jobject javaDragTargetView = dragTargetView->delegate.jView;
        CGPoint point = [mouse locationInView:dragTargetView.superview];

        if (lastJavaViewDragTarget != javaDragTargetView) { //We are entering View
            if (lastJavaViewDragTarget != NULL) { //... and leaving previous view
                [GlassDragDelegate sendJavaDndEvent:lastDragPositionInDragTarget jView:lastJavaViewDragTarget type:com_sun_glass_events_DndEvent_EXIT];
            }
            [GlassDragDelegate sendJavaDndEvent:point jView:javaDragTargetView type:com_sun_glass_events_DndEvent_ENTER];
        }
        [GlassDragDelegate sendJavaDndEvent:point jView:javaDragTargetView type:com_sun_glass_events_DndEvent_UPDATE];
        //keep last position and View
        lastJavaViewDragTarget = javaDragTargetView;
        lastDragPositionInDragTarget = point;
    }
}

+ (void) touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event withMouse:(UITouch *)mouse
{
    if (dragging == YES) {
        DNDLOG(@"MAIN VIEW IS DRAGGING - touchesEnded");
        [dragImage setCenter:[mouse locationInView:dragViewParent]];

        GlassViewGL * dragTargetView = (GlassViewGL *)[GlassDragDelegate getDragTargetView:mouse];
        jobject javaDragTargetView = dragTargetView->delegate.jView;
        CGPoint point = [mouse locationInView:dragTargetView.superview];

        if (([GlassDragDelegate getMask] & com_sun_glass_ui_Clipboard_ACTION_COPY) != 0) {
            [GlassDragDelegate sendJavaDndEvent:point jView:javaDragTargetView type:com_sun_glass_events_DndEvent_PERFORM];
            [GlassDragDelegate sendJavaDndEvent:point jView:javaDragTargetView type:com_sun_glass_events_DndEvent_END];
            [GlassDragDelegate hideImage];
        } else {
            [GlassDragDelegate sendJavaDndEvent:point jView:javaDragTargetView type:com_sun_glass_events_DndEvent_EXIT];
            [GlassDragDelegate sendJavaDndEvent:point jView:javaDragTargetView type:com_sun_glass_events_DndEvent_END];
            [GlassDragDelegate showImage:NO];
            [GlassDragDelegate animateImage];
        }
        dragging = NO;
    }
}

+ (void) touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event withMouse:(UITouch *)mouse
{
    if (dragging == YES) {
        DNDLOG(@"MAIN VIEW IS DRAGGING - touchesCancelled");
        [GlassDragDelegate touchesEnded:touches withEvent:event withMouse:mouse];
    }
}

+ (void) showImage:(BOOL)highlight
{
    [dragImage setHighlighted:highlight];
    if (dragImage.superview != dragViewParent) {
        [dragViewParent addSubview:dragImage];
    }
    [dragImage setHidden:NO];

}


+ (void) hideImage
{
    dragImage.hidden = YES;
    [dragImage removeFromSuperview];
}

// Animate DnD bitmap back to coords where draging session started
+ (void) animateImage
{
    [UIView animateWithDuration:0.3
                     animations:^{dragImage.center = CGPointMake(dragSourceLocation.x - dragImage.bounds.size.width/3, dragSourceLocation.y + dragImage.bounds.size.height/3) ;}
                     completion:^(BOOL finished){ [GlassDragDelegate hideImage]; }];
}

+ (void)sendJavaDndEvent:(CGPoint)draggingLocation jView:(jobject)javaView type:(jint)type
{
    GET_MAIN_JENV;
    DNDLOG(@"sendJavaDndEvent called x,y %f, %f, %p",draggingLocation.x, draggingLocation.y, javaView );
    int x = draggingLocation.x;
    int y = draggingLocation.y;
    int xAbs = x;
    int yAbs = y;
    int mask;
    jint recommendedActions = com_sun_glass_ui_Clipboard_ACTION_COPY;

    DNDLOG(@"dragging source operation %d, recommendedActions %d", (int)operation, (int)recommendedActions);
    [GlassDragDelegate setMask:recommendedActions];
    switch (type)
    {
        case com_sun_glass_events_DndEvent_ENTER:
            DNDLOG(@"com_sun_glass_events_DndEvent_ENTER");
            mask = (*env)->CallIntMethod(env, javaView , jViewNotifyDragEnter, x, y, xAbs, yAbs, recommendedActions);
            [GlassDragDelegate setMask:mask];
            DNDLOG(@"mask == %d", mask);
            break;
        case com_sun_glass_events_DndEvent_UPDATE:
            DNDLOG(@"com_sun_glass_events_DndEvent_UPDATE");
            mask = (*env)->CallIntMethod(env, javaView, jViewNotifyDragOver, x, y, xAbs, yAbs, recommendedActions);
            [GlassDragDelegate setMask:mask];
            if (mask == 0) {
                [GlassDragDelegate showImage:NO];
            } else {
                [GlassDragDelegate showImage:YES];
            }
            DNDLOG(@"mask == %d", mask);
            break;
        case com_sun_glass_events_DndEvent_PERFORM:
            DNDLOG(@"com_sun_glass_events_DndEvent_PERFORM");
            mask = (*env)->CallIntMethod(env, javaView, jViewNotifyDragDrop, x, y, xAbs, yAbs, recommendedActions);
            [GlassDragDelegate setMask:mask];
            DNDLOG(@"mask == %d", mask);
            break;
        case com_sun_glass_events_DndEvent_END:
            DNDLOG(@"com_sun_glass_events_DndEvent_END");
            (*env)->CallVoidMethod(env, javaView, jViewNotifyDragEnd, recommendedActions);
            [GlassDragDelegate setMask:com_sun_glass_ui_Clipboard_ACTION_NONE];
            DNDLOG(@"mask == %d", mask);
            break;
        case com_sun_glass_events_DndEvent_EXIT:
            DNDLOG(@"com_sun_glass_events_DndEvent_EXIT");
            (*env)->CallVoidMethod(env, javaView, jViewNotifyDragLeave);
            [GlassDragDelegate setMask:com_sun_glass_ui_Clipboard_ACTION_NONE];
            DNDLOG(@"mask == %d", mask);
            break;
        default:
            [GlassDragDelegate setMask:com_sun_glass_ui_Clipboard_ACTION_NONE];
            break;
    }

    GLASS_CHECK_EXCEPTION(env);
}

+ (void)setDelegate:(NSObject<GlassDragSourceDelegate>*)delegate
{
    DNDLOG(@"GlassDragDelegate:setDelegate");

    gDelegate = delegate; // notice, there is no retain
}

+ (void)flushWithMask:(jint)mask
{
    DNDLOG(@"GlassDragDelegate:flushWithMask: %ld", mask);

    if ([NSThread isMainThread] == YES)
    {
        if (mask != com_sun_glass_ui_Clipboard_ACTION_NONE)
        {
            DNDLOG(@"[gDelegate startDrag:%ld]",mask);
            [gDelegate startDrag:mask];
        }
        else
        {
            DNDLOG(@"[gDelegate startDrag:operation] NOT TAKEN!");
        }
    }
    else
    {
        DNDLOG(@"GlassDragDelegate flush called on nonmain thread!");
    }
}


+ (void)setMask:(jint)mask
{
    DNDLOG(@"GlassDragDelegate:mask");

    gMask = mask;
}

+ (jint)getMask
{
    DNDLOG(@"GlassDragDelegate:getMask");

    return gMask;
}


+ (void) cleanup
{
    [dragImage release];
    dragImage = nil;
}

@end
