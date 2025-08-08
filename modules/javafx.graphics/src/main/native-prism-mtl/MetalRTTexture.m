/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

#import "MetalRTTexture.h"
#import "com_sun_prism_mtl_MTLRTTexture.h"

@implementation MetalRTTexture

- (MetalRTTexture*) createTexture:(MetalContext*)ctx
                  ofPhysicalWidth:(NSUInteger)pw
                 ofPhysicalHeight:(NSUInteger)ph
                   ofContentWidth:(NSUInteger)cw
                  ofContentHeight:(NSUInteger)ch
                           isMsaa:(BOOL)isMsaa
{
    self = [super init];
    if (self) {
        physicalWidth  = pw;
        physicalHeight = ph;
        contentWidth   = cw;
        contentHeight  = ch;

        width   = pw;
        height  = ph;
        context = ctx;
        isMSAA  = isMsaa;

        pixelFormat = MTLPixelFormatBGRA8Unorm;
        mipmapped = NO;

        @autoreleasepool {
            MTLTextureDescriptor *texDescriptor = [MTLTextureDescriptor new];
            texDescriptor.storageMode = MTLStorageModeManaged;
            texDescriptor.usage  = MTLTextureUsageRenderTarget;
            texDescriptor.width  = width;
            texDescriptor.height = height;
            texDescriptor.textureType = MTLTextureType2D;
            texDescriptor.pixelFormat = pixelFormat;
            texDescriptor.sampleCount = 1;
            texDescriptor.hazardTrackingMode = MTLHazardTrackingModeTracked;

            id<MTLDevice> device = [context getDevice];

            texture = [device newTextureWithDescriptor:texDescriptor];

            if (isMSAA) {
                MTLTextureDescriptor *msaaTexDescriptor = [MTLTextureDescriptor new];
                msaaTexDescriptor.storageMode = MTLStorageModePrivate;
                msaaTexDescriptor.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead | MTLTextureUsageShaderWrite;
                msaaTexDescriptor.width  = width;
                msaaTexDescriptor.height = height;
                msaaTexDescriptor.textureType = MTLTextureType2DMultisample;
                msaaTexDescriptor.pixelFormat = pixelFormat;
                //By default all SoC's on macOS support 4 sample count
                msaaTexDescriptor.sampleCount = 4;
                msaaTexture = [device newTextureWithDescriptor:msaaTexDescriptor];
            } else {
                msaaTexture = nil;
            }

            depthTexture = nil;
            depthMSAATexture = nil;
        }
    }
    return self;
}

- (MetalRTTexture*) createTexture:(MetalContext*)ctx
                  ofPhysicalWidth:(NSUInteger)pw
                 ofPhysicalHeight:(NSUInteger)ph
                           mtlTex:(long)pTex
{
    self = [super init];
    if (self) {
        width = physicalWidth = pw;
        height = physicalHeight = ph;
        context = ctx;
        pixelFormat = MTLPixelFormatBGRA8Unorm;
        mipmapped = NO;
        id <MTLTexture> tex = (__bridge id<MTLTexture>)(jlong_to_ptr(pTex));
        texture = tex;
    }
    return self;
}

- (void) createDepthTexture
{
    id<MTLDevice> device = [context getDevice];
    if (depthTexture.width != width ||
        depthTexture.height != height ||
        lastDepthMSAA != isMSAA) {
        lastDepthMSAA = isMSAA;
        @autoreleasepool {
            MTLTextureDescriptor *depthDesc = [MTLTextureDescriptor new];
            depthDesc.width  = width;
            depthDesc.height = height;
            depthDesc.pixelFormat = MTLPixelFormatDepth32Float;
            depthDesc.textureType = MTLTextureType2D;
            depthDesc.sampleCount = 1;
            depthDesc.usage = MTLTextureUsageRenderTarget;
            depthDesc.storageMode = MTLStorageModePrivate;
            depthTexture = [device newTextureWithDescriptor:depthDesc];
            if (isMSAA) {
                depthDesc.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead | MTLTextureUsageShaderWrite;
                depthDesc.textureType = MTLTextureType2DMultisample;
                // By default all SoC's on macOS support 4 sample count
                depthDesc.sampleCount = 4;
                depthMSAATexture = [device newTextureWithDescriptor:depthDesc];
            }
        }
    }
}

- (void) initRTT:(int*)arr
{
    id<MTLTexture> tex = [self getTexture];
    MTLRegion region = {{0, 0, 0}, {tex.width, tex.height, 1}};
    [tex replaceRegion:region
           mipmapLevel:0
             withBytes:arr
           bytesPerRow:tex.width * 4];
}

- (void) readPixels:(int*)pDst
{
    int* texContent = (int*)[[self getPixelBuffer] contents];
    for (NSUInteger i = 0; i < contentHeight; i++) {
        for (NSUInteger j = 0; j < contentWidth; j++) {
            pDst[i * contentWidth + j] = texContent[i * physicalWidth + j];
        }
    }
}

- (void) readPixelsFromRTT:(int*)pDst
{
    int *texContent = (int*)[[self getPixelBuffer] contents];
    memcpy(pDst, texContent, contentWidth * contentHeight * 4);
}

- (id<MTLTexture>) getTexture { return [super getTexture]; }
- (id<MTLTexture>) getDepthTexture { return depthTexture; }
- (BOOL) isMSAAEnabled { return isMSAA; }
- (id<MTLTexture>) getMSAATexture { return msaaTexture; }
- (id<MTLTexture>) getDepthMSAATexture { return depthMSAATexture; }

- (void) dealloc {
    if (depthTexture != nil) {
        [depthTexture release];
        depthTexture = nil;
    }
    if (depthMSAATexture != nil) {
        [depthMSAATexture release];
        depthMSAATexture = nil;
    }
    if (msaaTexture != nil) {
        [msaaTexture release];
        msaaTexture = nil;
    }
    [super dealloc];
}
@end // MetalRTTexture


// ** JNI METHODS **

/*
 * Class:     com_sun_prism_mtl_MTLRTTexture
 * Method:    nCreateRT
 * Signature: (JIIIILcom/sun/prism/Texture/WrapMode;Z)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLRTTexture_nCreateRT
    (JNIEnv *env, jclass jClass, jlong ctx, jint pw, jint ph, jint cw,
    jint ch, jobject wrapMode, jboolean isMsaa)
{
    MetalContext* context = (MetalContext*)jlong_to_ptr(ctx);
    MetalRTTexture* rtt = [[MetalRTTexture alloc] createTexture:context
                                                ofPhysicalWidth:pw
                                               ofPhysicalHeight:ph
                                                 ofContentWidth:cw
                                                ofContentHeight:ch
                                                         isMsaa:isMsaa];
    return ptr_to_jlong(rtt);
}

/*
 * Class:     com_sun_prism_mtl_MTLRTTexture
 * Method:    nCreateRT2
 * Signature: (JJII)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLRTTexture_nCreateRT2
    (JNIEnv *env, jclass jClass, jlong ctx, jlong pTex, jint pw, jint ph)
{
    MetalContext* context = (MetalContext*)jlong_to_ptr(ctx);
    MetalRTTexture* rtt = [[MetalRTTexture alloc] createTexture:context
                                                ofPhysicalWidth:pw
                                               ofPhysicalHeight:ph
                                                         mtlTex:pTex];
    return ptr_to_jlong(rtt);
}

/*
 * Class:     com_sun_prism_mtl_MTLRTTexture
 * Method:    nInitRTT
 * Signature: (J[I)V
 */
// This method initializes underlying native MTLTexture with passed in pixData
// This texure replaceRegion is executed on CPU
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLRTTexture_nInitRTT
    (JNIEnv *env, jclass class, jlong jTexPtr, jintArray pixData)
{
    MetalRTTexture* rtt = (MetalRTTexture*) jlong_to_ptr(jTexPtr);
    int* arr = (*env)->GetIntArrayElements(env, pixData, NULL);

    [rtt initRTT:arr];

    (*env)->ReleaseIntArrayElements(env, pixData, arr, 0);
}

/*
 * Class:     com_sun_prism_mtl_MTLRTTexture
 * Method:    nReadPixelsFromRTT
 * Signature: (JLjava/nio/IntBuffer;)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLRTTexture_nReadPixelsFromRTT
    (JNIEnv *env, jclass class, jlong jTexPtr, jobject pixData)
{
    MetalRTTexture* rtt = (MetalRTTexture*) jlong_to_ptr(jTexPtr);
    int* pDst = (int*) (*env)->GetDirectBufferAddress(env, pixData);
    [rtt readPixelsFromRTT:pDst];
}

/*
 * Class:     com_sun_prism_mtl_MTLRTTexture
 * Method:    nReadPixels
 * Signature: (J[I)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_mtl_MTLRTTexture_nReadPixels
    (JNIEnv *env, jclass class, jlong jTexPtr, jintArray pixData)
{
    MetalRTTexture* rtt = (MetalRTTexture*) jlong_to_ptr(jTexPtr);
    int* pDst = (*env)->GetIntArrayElements(env, pixData, nil);

    [rtt readPixels:pDst];

    (*env)->ReleaseIntArrayElements(env, pixData, pDst, 0);
}
