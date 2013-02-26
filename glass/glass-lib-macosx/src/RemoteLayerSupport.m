/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import "RemoteLayerSupport.h"

@interface NSObject (JRSRenderServer)

@property(readonly) uint32_t layerID;

- (NSObject*)createRemoteLayerBoundTo:(mach_port_t)serverPort;
- (void)hostRemoteLayer:(uint32_t)layerID;
+ (mach_port_t)startRenderServer;
+ (NSString *)sendRenderServer:(mach_port_t)serverPort;
+ (mach_port_t)recieveRenderServer:(NSString *)serverName;

@end

mach_port_t RemoteLayerStartServer()
{
    mach_port_t theResult = MACH_PORT_NULL;
    {
        // Use reflection here so we don't have a dependency on JRSRemoteLayer.h
        Class JRSRemoteLayer_class = objc_getClass("JRSRenderServer");
        SEL startRenderServer_SEL = @selector(startRenderServer);
        NSMethodSignature *startRenderServer_sig = [JRSRemoteLayer_class methodSignatureForSelector:startRenderServer_SEL];
        NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:startRenderServer_sig];
        [invocation setSelector:startRenderServer_SEL];
        [invocation setTarget:JRSRemoteLayer_class];
        [invocation invoke];
        [invocation getReturnValue:&theResult];
    }
    return theResult;
}

NSString* RemoteLayerGetServerName(mach_port_t serverPort)
{
    NSString *name = nil;

    // If there's no serverPort JRSRenderServer is handling communications with
    // the Safari remote CA server. We still need a name, so don't call sendRenderServer:
    // and instead create a placeholder name.
    if (serverPort) {
        // Use reflection here so we don't have a dependency on JRSRemoteLayer.h
        Class JRSRemoteLayer_class = objc_getClass("JRSRenderServer");
        SEL sendRenderServer_SEL = @selector(sendRenderServer:);
        NSMethodSignature *sendRenderServer_sig = [JRSRemoteLayer_class methodSignatureForSelector:sendRenderServer_SEL];
        NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:sendRenderServer_sig];
        [invocation setSelector:sendRenderServer_SEL];
        [invocation setTarget:JRSRemoteLayer_class];
        [invocation setArgument:&serverPort atIndex:2];
        [invocation invoke];
        [invocation getReturnValue:&name];
    } else {
        name = [NSString stringWithFormat:@"PlaceHolderServerName-%d", getpid()];
    }

    return name;
}

mach_port_t RemoteLayerGetServerPort(NSString *serverName)
{
    mach_port_t port = MACH_PORT_NULL;
    {
        // Use reflection here so we don't have a dependency on JRSRemoteLayer.h
        Class JRSRemoteLayer_class = objc_getClass("JRSRenderServer");
        SEL recieveRenderServer_SEL = @selector(recieveRenderServer:);
        NSMethodSignature *receiveRenderServer_sig = [JRSRemoteLayer_class methodSignatureForSelector:recieveRenderServer_SEL];
        NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:receiveRenderServer_sig];
        [invocation setSelector:recieveRenderServer_SEL];
        [invocation setTarget:JRSRemoteLayer_class];
        [invocation setArgument:&serverName atIndex:2];
        [invocation invoke];
        [invocation getReturnValue:&port];
    }
    return port;
}

id RemoteLayerGetRemoteFromLocal(mach_port_t serverPort, id localLayer)
{
    id remoteLayer = nil;
    {
        // Use reflection here so we don't have a dependency on JRSRemoteLayer.h
        // remoteLayer = [localLayer reateRemoteLayerBoundTo:(mach_port_t)serverPort];
        SEL createRemoteLayer_SEL = @selector(createRemoteLayerBoundTo:);
        NSMethodSignature *createRemoteLayer_sig = [localLayer methodSignatureForSelector:createRemoteLayer_SEL];
        NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:createRemoteLayer_sig];
        [invocation setSelector:createRemoteLayer_SEL];
        [invocation setArgument:&serverPort atIndex:2];
        [invocation invokeWithTarget:localLayer];
        [invocation getReturnValue:&remoteLayer];
    }
    return remoteLayer;
}

uint32_t RemoteLayerGetIdForRemote(id remoteLayer)
{
    uint32_t layerID = 0;
    {
        // Use reflection here so we don't have a dependency on JRSRemoteLayer.h
        // layerID = [remoteLayer layerID];
        SEL layerID_SEL = @selector(layerID);
        NSMethodSignature *layerID_sig = [remoteLayer methodSignatureForSelector:layerID_SEL];
        NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:layerID_sig];
        [invocation setSelector:layerID_SEL];
        [invocation invokeWithTarget:remoteLayer];
        [invocation getReturnValue:&layerID];
        
        // what is the purpose of these 2 calls below?
        CFRetain(remoteLayer);
        [remoteLayer release]; // GC
    }
    return layerID;
}

void RemoteLayerHostRemoteIdInLocal(uint32_t remoteId, id localLayer)
{
    // Use reflection here so we don't have a dependency on JRSRemoteLayer.h
    // - (void) hostRemoteLayer:(uint32_t)layerID;
    SEL createRemoteLayer_SEL = @selector(hostRemoteLayer:);
    NSMethodSignature *hostRemoteLayer_sig = [localLayer methodSignatureForSelector:createRemoteLayer_SEL];
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:hostRemoteLayer_sig];
    [invocation setSelector:createRemoteLayer_SEL];
    [invocation setArgument:&remoteId atIndex:2];
    [invocation invokeWithTarget:localLayer];
}
