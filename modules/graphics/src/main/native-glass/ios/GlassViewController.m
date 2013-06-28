/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
#import "GlassViewController.h"
#import "GlassApplication.h"
#import "GlassWindow.h"

// Class responsible for user interface rotation and sending notifications to FX when such
// change happens.
@implementation GlassViewController

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation 
{
    GlassApplication * app =  (GlassApplication*)[[UIApplication sharedApplication] delegate];
    [app GlassApplicationDidChangeScreenParameters];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{  
    return [self supportsOrientation:interfaceOrientation];
}

- (void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    orient = [self interfaceOrientation];
}

-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration 
{
    if ((orient < 3 && toInterfaceOrientation > 2) ||
        (orient > 2 && toInterfaceOrientation < 3)) {
        for(GlassWindow * gw in [self.view  subviews]) {
            if (gw->owner == nil) {//we have found primary stage

                    [UIView beginAnimations:@"View Flip" context:nil];
                    [UIView setAnimationDuration:duration];
                    [UIView setAnimationCurve:UIViewAnimationCurveEaseInOut];

                    gw.bounds = CGRectMake(0.0, 0.0, gw.bounds.size.height, gw.bounds.size.width);
                    gw.center = CGPointMake(gw.center.y, gw.center.x);
                    [UIView commitAnimations];
                
                break;
            }
        }
    }
}

// Returns YES for supported UIInterfaceOrientation. No otherwise. Supported values needs to
// be enumerated in applications info .plist file.
- (BOOL) supportsOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    NSBundle * mainBundle = [NSBundle mainBundle];
    NSDictionary * dict = [mainBundle infoDictionary];
    NSArray * values = (NSArray *)[dict valueForKey:@"UISupportedInterfaceOrientations"];
    NSString * sInterfaceOrientation = nil;
    
    switch (interfaceOrientation) {
        case UIInterfaceOrientationPortrait:
            sInterfaceOrientation = @"UIInterfaceOrientationPortrait";
            break;
        case UIInterfaceOrientationPortraitUpsideDown:
            sInterfaceOrientation = @"UIInterfaceOrientationPortraitUpsideDown";
            break;
        case UIInterfaceOrientationLandscapeLeft:
            sInterfaceOrientation = @"UIInterfaceOrientationLandscapeLeft";
            break;
        case UIInterfaceOrientationLandscapeRight:
            sInterfaceOrientation = @"UIInterfaceOrientationLandscapeRight";
            break;
    }
    
    if (values != nil) {
        for (NSString * value in values) {
            if ([value isEqualToString:sInterfaceOrientation] == YES) {
                return YES;
            }
        }
    }
    
    
    return NO;
}


@end
