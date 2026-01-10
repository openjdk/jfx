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

#import "MetalTexture.h"
#import "MetalPipelineManager.h"

// ** HELPER METHODS **

static unsigned int getPixelSize(enum MTLPixelFormat pixelFormat)
{
    switch (pixelFormat) {
        case MTLPixelFormatA8Unorm:
            return 1;
        case MTLPixelFormatBGRA8Unorm:
            return 4;
        case MTLPixelFormatRGBA32Float:
            return 16;
        default:
            return 0;
    }
}

static NSMutableDictionary *getBufferAndOffset(MetalContext* context, unsigned int length)
{
    NSMutableDictionary<NSNumber *, id<MTLBuffer>> *bufferOffsetDict = [NSMutableDictionary dictionary];
    id<MTLBuffer> pixelMTLBuf = nil;
    int offset = [[context getDataRingBuffer] reserveBytes:length];
    if (offset < 0) {
        pixelMTLBuf = [context getTransientBufferWithLength:length];
        offset = 0;
    } else {
        pixelMTLBuf = [[context getDataRingBuffer] getBuffer];
    }

    [bufferOffsetDict setObject:pixelMTLBuf forKey:@(offset)];
    return bufferOffsetDict;
}

static NSMutableDictionary *copyPixelDataToRingBuffer(MetalContext* context, void* pixels,
    int srcx, int srcy, int w, int h, int scanStride, MTLPixelFormat pixelFormat)
{
    unsigned int pixelSize = getPixelSize(pixelFormat);
    unsigned int length = pixelSize * w * h;
    NSMutableDictionary<NSNumber *, id<MTLBuffer>> *bufferOffsetDict = getBufferAndOffset(context, length);
    NSNumber *offset = [[bufferOffsetDict allKeys] firstObject];
    id<MTLBuffer> dstBuf = [[bufferOffsetDict allValues] firstObject];

    void *dstBufOffset = dstBuf.contents + [offset intValue];
    unsigned int rowLength = pixelSize * w;
    void *pixelsSrcOffset = pixels + srcy * scanStride + srcx * pixelSize;

    for (int i = 0; i < h; i++) {
        memcpy(dstBufOffset + (rowLength * i), pixelsSrcOffset + (scanStride * i), rowLength);
    }

    return bufferOffsetDict;
}

@implementation MetalTexture

// This method creates a native MTLTexture
- (MetalTexture*) createTexture:(MetalContext*)ctx
                        ofWidth:(NSUInteger)w
                       ofHeight:(NSUInteger)h
                    pixelFormat:(NSUInteger)format
                      useMipMap:(BOOL)useMipMap
{
    self = [super init];
    if (self) {
        width   = w;
        height  = h;
        context = ctx;
        pixelFormat = MTLPixelFormatBGRA8Unorm;

        switch (format) {
            case PFORMAT_BYTE_BGRA_PRE:
            case PFORMAT_INT_ARGB_PRE:
            case PFORMAT_BYTE_RGB:         // Note: this is actually 3-byte RGB
            case PFORMAT_BYTE_GRAY:
                pixelFormat = MTLPixelFormatBGRA8Unorm;
                break;
            case PFORMAT_BYTE_ALPHA:
                pixelFormat = MTLPixelFormatA8Unorm;
                break;
            case PFORMAT_FLOAT_XYZW:
                pixelFormat = MTLPixelFormatRGBA32Float;
                break;
            default:
                break;
        }

        mipmapped = useMipMap;
        // We create 1x1 diffuse map when we have only diffuse
        // color for PhongMaterial, in such a case if generate mipmap
        // it causes assertion error at generateMipMap because
        // mipmapLevelCount will be 1, ignore generating mipmap for
        // texture 1x1
        if (useMipMap &&
            (width == 1 && height == 1)) {
            mipmapped = NO;
        }
        @autoreleasepool {
            MTLTextureDescriptor *texDescriptor =
                [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:pixelFormat
                                                                   width:width
                                                                  height:height
                                                               mipmapped:mipmapped];
            texDescriptor.storageMode = MTLStorageModePrivate;
            // texDescriptor.usage = MTLTextureUsageShaderWrite | MTLTextureUsageShaderRead;
            texDescriptor.usage = MTLTextureUsageUnknown;

            texture = [[context getDevice] newTextureWithDescriptor:texDescriptor];
        }
    }
    return self;
}

- (id<MTLBuffer>) getPixelBuffer
{
    [context endCurrentRenderEncoder];

    id<MTLCommandBuffer> commandBuffer = [context getCurrentCommandBuffer];
    @autoreleasepool {
        id<MTLBlitCommandEncoder> blitEncoder = [commandBuffer blitCommandEncoder];

        if (texture.usage == MTLTextureUsageRenderTarget) {
            [blitEncoder synchronizeTexture:texture slice:0 level:0];
        }

        [blitEncoder copyFromTexture:texture
                         sourceSlice:(NSUInteger)0
                         sourceLevel:(NSUInteger)0
                        sourceOrigin:MTLOriginMake(0, 0, 0)
                          sourceSize:MTLSizeMake(texture.width, texture.height, texture.depth)
                            toBuffer:[context getPixelBuffer]
                   destinationOffset:(NSUInteger)0
              destinationBytesPerRow:(NSUInteger)texture.width * getPixelSize(pixelFormat)
            destinationBytesPerImage:(NSUInteger)texture.width * texture.height * getPixelSize(pixelFormat)];

        [blitEncoder endEncoding];
    }
    [context commitCurrentCommandBufferAndWait];

    return [context getPixelBuffer];
}

- (void) updateTexture:(void*)pixels
                  dstX:(int)dstX
                  dstY:(int)dstY
                  srcX:(int)srcX
                  srcY:(int)srcY
                 width:(int)w
                height:(int)h
            scanStride:(int)scanStride
{
    NSMutableDictionary* bufferOffsetDict = copyPixelDataToRingBuffer(context, pixels, srcX, srcY,
                                                                    w, h, scanStride, pixelFormat);
    int offset = [[[bufferOffsetDict allKeys] firstObject] intValue];
    id<MTLBuffer> pixelMTLBuf = [[bufferOffsetDict allValues] firstObject];

    [context endCurrentRenderEncoder];
    id<MTLCommandBuffer> commandBuffer = [context getCurrentCommandBuffer];
    @autoreleasepool {
        id<MTLBlitCommandEncoder> blitEncoder = [commandBuffer blitCommandEncoder];

        [blitEncoder copyFromBuffer:pixelMTLBuf
                       sourceOffset:(NSUInteger)offset
                  sourceBytesPerRow:(NSUInteger)w * getPixelSize(pixelFormat)
                sourceBytesPerImage:(NSUInteger)0 // 0 for 2D image
                         sourceSize:MTLSizeMake(w, h, 1)
                          toTexture:texture
                   destinationSlice:(NSUInteger)0
                   destinationLevel:(NSUInteger)0
                  destinationOrigin:MTLOriginMake(dstX, dstY, 0)];

        if (texture.usage == MTLTextureUsageRenderTarget) {
            [blitEncoder synchronizeTexture:texture slice:0 level:0];
        }

        if ([self isMipmapped]) {
            [blitEncoder generateMipmapsForTexture:texture];
        }

        [blitEncoder endEncoding];
    }
}

- (void) updateTextureYUV422:(char*)pixels
                        dstX:(int)dstX
                        dstY:(int)dstY
                        srcX:(int)srcX
                        srcY:(int)srcY
                       width:(int)w
                      height:(int)h
                  scanStride:(int)scanStride
{
    id<MTLTexture> tex = [self getTexture];
    @autoreleasepool {
        id<MTLDevice> device = [context getDevice];

        id<MTLBuffer> srcBuff = [[device newBufferWithLength:(w * h * 2)
                                                     options:MTLResourceStorageModeManaged] autorelease];
        for (int row = 0; row < h; row++) {
            // Copy each row in srcBuff
            memcpy(srcBuff.contents + (row * w * 2), pixels, w * 2);
            pixels += (w * 2);
            pixels += scanStride - (w * 2);
        }

        [srcBuff didModifyRange:NSMakeRange(0, srcBuff.length)];

        [context endCurrentRenderEncoder];

        MTLSize threadgroupSize = MTLSizeMake(2, 1, 1);

        MTLSize threadgroupCount;
        threadgroupCount.width  = w / threadgroupSize.width;
        threadgroupCount.height = h / threadgroupSize.height;
        threadgroupCount.depth  = 1;

        id<MTLComputePipelineState> computePipelineState =
            [[context getPipelineManager] getComputePipelineStateWithFunc:@"uyvy422_to_rgba"];

        id<MTLCommandBuffer> commandBuffer = [context getCurrentCommandBuffer];

        id<MTLComputeCommandEncoder> computeEncoder = [commandBuffer computeCommandEncoder];

        [computeEncoder setComputePipelineState:computePipelineState];

        [computeEncoder setBuffer:srcBuff
                           offset:0
                          atIndex:0];

        [computeEncoder setTexture:tex
                           atIndex:0];

        [computeEncoder dispatchThreadgroups:threadgroupCount
                       threadsPerThreadgroup:threadgroupSize];

        [computeEncoder endEncoding];

        [context commitCurrentCommandBuffer];
    }
}

- (id<MTLTexture>) getTexture
{
    return texture;
}

- (MTLPixelFormat) getPixelFormat
{
    return pixelFormat;
}

- (BOOL) isMipmapped
{
    return mipmapped;
}

- (void) dealloc
{
    if (texture != nil) {
        [texture release];
        texture = nil;
    }
    [super dealloc];
}

@end // MetalTexture


// ** JNI METHODS **

/*
 * Class:     com_sun_prism_mtl_MTLTexture
 * Method:    nUpdate
 * Signature: (JLjava/nio/ByteBuffer;[BIIIIIII)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLTexture_nUpdate
    (JNIEnv *env, jclass jClass, jlong nTexturePtr, jobject buf,
    jbyteArray pixData, jint dstx, jint dsty, jint srcx, jint srcy,
    jint width, jint height, jint scanStride)
{
    MetalTexture* mtlTex  = (MetalTexture*)jlong_to_ptr(nTexturePtr);

    jint length = pixData?
        (*env)->GetArrayLength(env, pixData) :
        (jint)((*env)->GetDirectBufferCapacity(env, buf));
    length *= sizeof(jbyte);

    jbyte* pixels = (jbyte*)((pixData != NULL) ?
        (*env)->GetPrimitiveArrayCritical(env, pixData, NULL) :
        (*env)->GetDirectBufferAddress(env, buf));

    [mtlTex updateTexture:pixels
                     dstX:dstx
                     dstY:dsty
                     srcX:srcx
                     srcY:srcy
                    width:width
                   height:height
               scanStride:scanStride];

    if (pixData != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, pixData, pixels, 0);
    }

    // TODO: MTL: add error detection and return appropriate jlong
    return 0;
}

/*
 * Class:     com_sun_prism_mtl_MTLTexture
 * Method:    nUpdateFloat
 * Signature: (JLjava/nio/FloatBuffer;[FIIIIIII)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLTexture_nUpdateFloat
    (JNIEnv *env, jclass jClass, jlong nTexturePtr, jobject buf,
    jfloatArray pixData, jint dstx, jint dsty, jint srcx, jint srcy,
    jint width, jint height, jint scanStride)
{
    MetalTexture* mtlTex  = (MetalTexture*)jlong_to_ptr(nTexturePtr);

    jint length = pixData ?
        (*env)->GetArrayLength(env, pixData) :
        (jint)((*env)->GetDirectBufferCapacity(env, buf));
    length *= sizeof(jfloat);

    jfloat *pixels = (jfloat*)((pixData != NULL) ?
        (*env)->GetPrimitiveArrayCritical(env, pixData, NULL) :
        (*env)->GetDirectBufferAddress(env, buf));

    [mtlTex updateTexture:pixels
                     dstX:dstx
                     dstY:dsty
                     srcX:srcx
                     srcY:srcy
                    width:width
                   height:height
               scanStride:scanStride];

    if (pixData != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, pixData, pixels, 0);
    }
    // TODO: MTL: add error detection and return appropriate jlong
    return 0;
}

/*
 * Class:     com_sun_prism_mtl_MTLTexture
 * Method:    nUpdateInt
 * Signature: (JLjava/nio/IntBuffer;[IIIIIIII)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLTexture_nUpdateInt
    (JNIEnv *env, jclass jClass, jlong nTexturePtr, jobject buf,
    jintArray pixData, jint dstx, jint dsty, jint srcx, jint srcy,
    jint width, jint height, jint scanStride)
{
    MetalTexture* mtlTex  = (MetalTexture*)jlong_to_ptr(nTexturePtr);

    jint length = pixData ?
        (*env)->GetArrayLength(env, pixData) :
        (jint)((*env)->GetDirectBufferCapacity(env, buf));
    length *= sizeof(jint);

    jint *pixels = (jint*)((pixData != NULL) ?
        (*env)->GetPrimitiveArrayCritical(env, pixData, NULL) :
        (*env)->GetDirectBufferAddress(env, buf));

    [mtlTex updateTexture:pixels
                     dstX:dstx
                     dstY:dsty
                     srcX:srcx
                     srcY:srcy
                    width:width
                   height:height
               scanStride:scanStride];

    if (pixData != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, pixData, pixels, 0);
    }// TODO: MTL: add error detection and return appropriate jlong
    return 0;
}

/*
 * Class:     com_sun_prism_mtl_MTLTexture
 * Method:    nUpdateInt
 * Signature: (J[BIIIIIII)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_mtl_MTLTexture_nUpdateYUV422
    (JNIEnv *env, jclass jClass, jlong nTexturePtr, jbyteArray pixData,
    jint dstx, jint dsty, jint srcx, jint srcy, jint w, jint h, jint scanStride)
{
    MetalTexture* mtlTex  = (MetalTexture*)jlong_to_ptr(nTexturePtr);
    jbyte* pixels = (*env)->GetByteArrayElements(env, pixData, 0);

    [mtlTex updateTextureYUV422:(char*)pixels
                           dstX:dstx
                           dstY:dsty
                           srcX:srcx
                           srcY:srcy
                          width:w
                         height:h
                     scanStride:scanStride];

    (*env)->ReleaseByteArrayElements(env, pixData, pixels, 0);

    // TODO: MTL: add error detection and return appropriate jlong
    return 0;
}
