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

#import <Foundation/Foundation.h>
#import <JavaVM/jni.h>

// A place to store native peers to Java objects
@interface JObjectPeers : NSObject
{
    NSMutableArray *peerList;
}

// does not cache JNIEnv! It does use the JavaVM for comparison though
- (void) setPeer:(id)peer forJObject:(jobject)jo javaEnv:(JNIEnv*)env;

- (void) removePeersForJObject:(jobject)jo javaEnv:(JNIEnv*)env;
- (void) removePeer:(id)peer; // removes all associations with the given ObjC object

- (id) peerForJObject:(jobject)obj javaEnv:(JNIEnv*)env;

- (jobject) jobjectForPeer:(id)peer javaEnv:(JNIEnv*)env;

/*
 * Use this if you do not already have a JNIEnv. Bear in mind you may need to attach
 * the current thread to get a valid env
 * Ugly, but atomic
 */
- (jobject) jobjectForPeer:(id)peer andVM:(JavaVM**)vm;

@end
