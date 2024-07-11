/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

#import <Cocoa/Cocoa.h>

@interface NSApplication (NiblessAdditions)

-(void)setAppleMenu:(NSMenu *)aMenu;

@end


/*
 * NSApplicationFX is a subclass of NSApplication that we use when we
 * initialize the application.
 * We need to subclass NSApplication in order to stop AWT from installing
 * their NSApplicationDelegate delegate, overwriting the one we install.
 *
 * We don't override anything in NSApplication. All work is done in our
 * NSApplicationDelegate as recommended by Apple.
 */
@interface NSApplicationFX : NSApplication
@end

@interface GlassApplication : NSObject <NSApplicationDelegate>
{
    BOOL            started;

    jobject         jApplication;
    jobject         jLaunchable;
    jboolean        jTaskBarApp;
    jlong           jshareContextPtr;

    // local and intra-app event monitoring
    //
    // id              localMonitor;
    // id              globalMonitor;
}

- (void)runLoop:(id)selector;
- (BOOL)started;

+ (jobject)enterNestedEventLoopWithEnv:(JNIEnv*)env;
+ (void)leaveNestedEventLoopWithEnv:(JNIEnv*)env retValue:(jobject)retValue;

+ (void)enterFullScreenExitingLoop;
+ (void)leaveFullScreenExitingLoopIfNeeded;

+ (void)registerKeyEvent:(NSEvent*)event;
+ (jint)getKeyCodeForChar:(jchar)c;

+ (BOOL)syncRenderingDisabled;

@end
