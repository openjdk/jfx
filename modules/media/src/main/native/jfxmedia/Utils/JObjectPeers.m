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

#import "JObjectPeers.h"
#import "JavaUtils.h"

#define USE_WEAK_REFS 0

/************************************************************************
    Never cache a JNIEnv! They are different for each calling thread.
    Instead we'll use the underlying JavaVM for comparison.
 ************************************************************************/

@interface PeerListEntry : NSObject
{
    id peer;
    jobject javaObject;
    JavaVM *jvm;
}

@property (nonatomic,readonly,assign) id peer;
@property (nonatomic,readonly,assign) jobject javaObject;
@property (nonatomic,readonly,assign) JavaVM *jvm;

- (id) initWithPeer:(id)inPeer javaObject:(jobject)inJavaObject javaVM:(JavaVM*)objectVM;
- (BOOL) isSameObject:(jobject)other withVM:(JavaVM*)otherVM inEnv:(JNIEnv*)env;

@end

@implementation PeerListEntry

@synthesize peer;
@synthesize javaObject;
@synthesize jvm;

- (id) initWithPeer:(id)inPeer javaObject:(jobject)inJavaObject javaVM:(JavaVM *)objectVM
{
    if ((self = [super init]) != nil) {
        peer = [inPeer retain];
        javaObject = inJavaObject;
        jvm = objectVM;
    }
    return self;
}

- (void) dealloc
{
    [peer release];
    javaObject = 0;
    jvm = NULL;
    [super dealloc];
}

- (BOOL) isSameObject:(jobject)other withVM:(JavaVM*)otherVM inEnv:(JNIEnv*)env
{
    if ((jvm == otherVM) && (env != NULL)) {
        return (*env)->IsSameObject(env, other, javaObject);
    }
    return NO;
}

@end

// FIXME: may need to @synchronized (peerList)

@implementation JObjectPeers

- (id) init
{
    if ((self = [super init]) != nil) {
        peerList = [[NSMutableArray alloc] init];
    }
    return self;
}

- (void) dealloc
{
    [peerList release];
    peerList = nil;
    [super dealloc];
}

// call within a synchronized block for protection
- (PeerListEntry*) entryForPeer:(id)peer
{
    for (PeerListEntry *candidate in peerList) {
        if (candidate.peer == peer) {
            return candidate;
        }
    }
    return nil;
}

- (PeerListEntry*) entryForJavaObject:(jobject)jo javaEnv:(JNIEnv*)env
{
    JavaVM *jvm;
    if ((*env)->GetJavaVM(env, &jvm) == JNI_OK) {
        for (PeerListEntry *candidate in peerList) {
            if ([candidate isSameObject:jo withVM:jvm inEnv:env]) {
                return candidate;
            }
        }
    }
    return nil;
}

- (void) setPeer:(id)peer forJObject:(jobject)jo javaEnv:(JNIEnv *)env
{
    JavaVM *jvm;
    if ((*env)->GetJavaVM(env, &jvm) == JNI_OK) {
        @synchronized (peerList) {
            // check for existing association
            PeerListEntry *entry = nil;
            for (PeerListEntry *candidate in peerList) {
                if (candidate.peer == peer && [candidate isSameObject:jo withVM:jvm inEnv:env]) {
                    // association already exists
                    return;
                }
            }
             
            entry = [[PeerListEntry alloc] initWithPeer:peer javaObject:jo javaVM:jvm];
            [peerList addObject:entry];
            [entry release]; // array retains
        }
    }
}

- (void) removePeersForJObject:(jobject)jo javaEnv:(JNIEnv *)env
{
    JavaVM *jvm;
    if ((*env)->GetJavaVM(env, &jvm) == JNI_OK) {
        @synchronized (peerList) {
            PeerListEntry *entry = [self entryForJavaObject:jo javaEnv:env];
            if (entry) {
                [peerList removeObject:entry];
                // entry will be released by array
            }
        }
    }
}

- (void) removePeer:(id)peer
{
    @synchronized (peerList) {
        PeerListEntry *entry = [self entryForPeer:peer];
        if (entry) {
            [peerList removeObject:entry];
        }
    }
}

- (id) peerForJObject:(jobject)obj javaEnv:(JNIEnv *)env
{
    JavaVM *jvm;
    if ((*env)->GetJavaVM(env, &jvm) == JNI_OK) {
        @synchronized (peerList) {
            PeerListEntry *entry = [self entryForJavaObject:obj javaEnv:env];
            if (entry) {
                return [[entry.peer retain] autorelease];
            }
        }
    }
    return nil;
}

- (jobject) jobjectForPeer:(id)peer javaEnv:(JNIEnv*)env
{
    JavaVM *jvm;
    if ((*env)->GetJavaVM(env, &jvm) == JNI_OK) {
        @synchronized (peerList) {
            PeerListEntry *entry = [self entryForPeer:peer];
            if (entry) {
                return entry.javaObject;
            }
        }
    }
    return nil;
}

- (jobject) jobjectForPeer:(id)peer andVM:(JavaVM **)vm
{
    @synchronized (peerList) {
        PeerListEntry *entry = [self entryForPeer:peer];
        if (entry) {
            *vm = entry.jvm;
            return entry.javaObject;
        }
    }
    return 0;
}

@end
