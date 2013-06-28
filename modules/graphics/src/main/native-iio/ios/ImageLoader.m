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

#import "ImageLoader.h"

#include "jni_utils.h"
#include "com_sun_javafx_iio_ios_IosImageLoader.h"

#include "debug.h"

// For incremental loading see:
//http://developer.apple.com/library/ios/#documentation/GraphicsImaging/Conceptual/ImageIOGuide/imageio_intro/ikpg_intro.html


@implementation ImageLoader


@synthesize buffer;
@synthesize width;
@synthesize height;
@synthesize nComponents;
@synthesize colorSpace;
@synthesize nImages;
@synthesize delayTime;
@synthesize cgImageSource;
@synthesize cgImage;

-(id) init {
    self = [super init];
    if (self) {
        [self setBuffer : [NSMutableData data]];
    }
    return self;
}

-(void) dealloc {
    IIOLog(@"ImageLoader::dealloc");
    CFRelease(cgImageSource);
    [buffer release];
    [super dealloc];
}

-(void) addToBuffer : (const void *) bytes
             length : (int) length {
    [buffer appendBytes : bytes
                 length : length];
}

-(BOOL) hasAlpha : (CGImageAlphaInfo) aInfo {
    return aInfo != kCGImageAlphaNone;
}

-(BOOL) isAlphaPremultiplied : (CGImageAlphaInfo) aInfo {
    return aInfo == kCGImageAlphaPremultipliedLast ||
    aInfo == kCGImageAlphaPremultipliedFirst;
}

-(int) resolveJavaColorSpace : (CGColorSpaceModel) nativeModel
                   alphaInfo : (CGImageAlphaInfo) aInfo {

    int jColorSpace = -1;

    if (nativeModel == kCGColorSpaceModelMonochrome) {
        if ([self hasAlpha : aInfo]) {
            if ([self isAlphaPremultiplied : aInfo]) {
                jColorSpace = com_sun_javafx_iio_ios_IosImageLoader_GRAY_ALPHA_PRE;
            } else {
                jColorSpace = com_sun_javafx_iio_ios_IosImageLoader_GRAY_ALPHA;
            }
        } else {
            jColorSpace = com_sun_javafx_iio_ios_IosImageLoader_GRAY;
        }
    } else if (nativeModel == kCGColorSpaceModelIndexed) {
        if ([self hasAlpha : aInfo]) {
            if ([self isAlphaPremultiplied : aInfo]) {
                jColorSpace = com_sun_javafx_iio_ios_IosImageLoader_PALETTE_ALPHA_PRE;
            } else {
                jColorSpace = com_sun_javafx_iio_ios_IosImageLoader_PALETTE_ALPHA;
            }
        } else {
            jColorSpace = com_sun_javafx_iio_ios_IosImageLoader_PALETTE;
        }
        // NOTE: what about com_sun_javafx_iio_ios_IosImageLoader_PALETTE_TRANS ???
    } else if (nativeModel == kCGColorSpaceModelRGB) {
        if ([self hasAlpha : aInfo]) {
            if ([self isAlphaPremultiplied : aInfo]) {
                jColorSpace = com_sun_javafx_iio_ios_IosImageLoader_RGBA_PRE;
            } else {
                jColorSpace = com_sun_javafx_iio_ios_IosImageLoader_RGBA;
            }
        } else {
            jColorSpace = com_sun_javafx_iio_ios_IosImageLoader_RGB;
        }
    }

    /*

     NOTE: what about the rest of color space models? The Java enum ImageType has no equivalents!

     kCGColorSpaceModelUnknown,
     kCGColorSpaceModelCMYK,
     kCGColorSpaceModelLab,
     kCGColorSpaceModelDeviceN,
     kCGColorSpaceModelPattern

     */

    return jColorSpace;
}

-(void) reportImageProperties {
    IIOLog(@"Image size:                 [ %d x %d ]", [self width], [self height]);
    IIOLog(@"Image color space:          %d", [self colorSpace]);
    IIOLog(@"Image number of components: %d", [self nComponents]);
    IIOLog(@"Image number of images:     %d", [self nImages]);
    IIOLog(@"Image duration:             %d", [self delayTime]);
}

-(void) retrieveDelayTime {

    NSDictionary *dict = (NSDictionary *) CGImageSourceCopyPropertiesAtIndex(cgImageSource, 0, NULL);
    NSDictionary *gifDict = (NSDictionary *) [dict objectForKey : (id) kCGImagePropertyGIFDictionary];

    int delay = 100; // 100ms default if no time interval is retrieved
    if (gifDict) {
        NSNumber *delayValue = [gifDict objectForKey : (id) kCGImagePropertyGIFDelayTime];
        delay = (int) ([delayValue doubleValue] * 1000);
    }

    [self setDelayTime : delay];
}

-(CGImageRef) createImageAtIndex : (int) index {

    CGImageRef cgImageRef = CGImageSourceCreateImageAtIndex(cgImageSource, index, NULL);
    return cgImageRef;
}

-(BOOL) loadFromSource : (CGImageSourceRef) sourceRef
                JNIEnv : (JNIEnv *) env {

    [self setCgImageSource : sourceRef];

    size_t imageCount = CGImageSourceGetCount(sourceRef);
    [self setNImages : (int) imageCount];

    if (imageCount > 1) {
        [self retrieveDelayTime];
    }

    BOOL success = FALSE;

    CGImageRef firstImage = [self createImageAtIndex : 0];

    if (firstImage) {
        [self setCgImage : firstImage];
        [self setWidth : (int) CGImageGetWidth(firstImage)];
        [self setHeight : (int) CGImageGetHeight(firstImage)];

        CGColorSpaceRef cgColorSpace = CGImageGetColorSpace(firstImage);
        CGImageAlphaInfo alphaInfo = CGImageGetAlphaInfo(firstImage);

        int nComp = (int) CGColorSpaceGetNumberOfComponents(cgColorSpace);
        if ([self hasAlpha : alphaInfo]) {
            nComp++;
        }

        [self setNComponents : nComp];

        CGColorSpaceModel spaceModel = CGColorSpaceGetModel(cgColorSpace);
        [self setColorSpace :
         [self resolveJavaColorSpace : spaceModel
                           alphaInfo : alphaInfo]];

        [self reportImageProperties];

        success = TRUE;
    } else {
        // NOTE: see what went wrong (RT-27439)
        //CGImageSourceStatus status = CGImageSourceGetStatus(sourceRef);
        /*enum CGImageSourceStatus {
         kCGImageStatusUnexpectedEOF = -5,
         kCGImageStatusInvalidData = -4,
         kCGImageStatusUnknownType = -3,
         kCGImageStatusReadingHeader = -2,
         kCGImageStatusIncomplete = -1,
         kCGImageStatusComplete = 0
         };*/

        throwException(env, JAVA_IO_IOEXCEPTION, "Unable to decode image");
    }

    return success;
}

-(BOOL) loadFromURL : (NSString *) urlString
             JNIEnv : (JNIEnv *) env {

    NSURL *url = [NSURL URLWithString : urlString];

    BOOL success = FALSE;

    if ([url isFileURL]) {
        NSError *error;
        BOOL isReachable = [url checkResourceIsReachableAndReturnError : &error];

        if (!isReachable) {
            NSString *nsErrorMessage = [NSString stringWithFormat : @"%@ (%@), Recovery suggestion: %@",
                                        [error localizedFailureReason],
                                        [error localizedDescription],
                                        [error localizedRecoverySuggestion]];

            const char *errorMessage = [nsErrorMessage UTF8String];
            throwException(env, JAVA_IO_IOEXCEPTION, errorMessage);
            return FALSE;
        }
    }

    CGImageSourceRef sourceRef = CGImageSourceCreateWithURL((CFURLRef) url, NULL);
    if (sourceRef == NULL) {
        throwException(env, JAVA_IO_IOEXCEPTION, "Failed to create CGImageSource");
    } else {
        success = [self loadFromSource : sourceRef
                                JNIEnv : env];
    }

    return success;
}

-(BOOL) loadFromBuffer : (JNIEnv *) env {

    BOOL success = FALSE;

    CGImageSourceRef sourceRef = CGImageSourceCreateWithData((CFTypeRef) buffer, NULL);

    if (sourceRef == NULL) {
        throwException(env, JAVA_IO_IOEXCEPTION, "Failed to create CGImageSource");
    } else {
        success = [self loadFromSource : sourceRef
                                JNIEnv : env];
    }

    return success;
}

-(BOOL) hasLessThan8BitsPerChannel {
    return CGImageGetBitsPerPixel([self cgImage]) < 24;
}

/*

 In order to resize an image it is necessary to create a new bitmap context. However, not all
 color spaces are supported for new bitmap contexts. Therefore all non-alpha images are
 converted to

 - kCGImageAlphaNoneSkipLast

 and all images with an alpha component are converted to

 - kCGImageAlphaPremultipliedLast

 (Not premultiplied RGB is not supported)

 For the list of all supported pixel formats, see

 https://developer.apple.com/library/ios/#documentation/GraphicsImaging/Conceptual/drawingwithquartz2d/dq_context/dq_context.html

 (Supported Pixel Formats section)

 */

-(void) resize : (int) newWidth : (int) newHeight {

    // in case a different size is requested or the original color model uses less than 8 bits per component, create a new bitmap context
    if ([self width] != newWidth ||
        [self height] != newHeight ||
        [self hasLessThan8BitsPerChannel]) {

        const CGImageAlphaInfo alphaInfo = CGImageGetAlphaInfo(cgImage);

        const CGBitmapInfo bitmapInfo = [self hasAlpha : alphaInfo] ?
        kCGImageAlphaPremultipliedLast : kCGImageAlphaNoneSkipLast;

        const int nComp = 4;
        CGColorSpaceRef cgColorSpace = CGColorSpaceCreateDeviceRGB();
        CGContextRef cgContext = CGBitmapContextCreate(
                                                       NULL,
                                                       newWidth,
                                                       newHeight,
                                                       8,
                                                       nComp * newWidth,
                                                       cgColorSpace,
                                                       bitmapInfo);

        CGContextDrawImage(cgContext,
                           CGRectMake(0.0f, 0.0f,
                                      (CGFloat) newWidth,
                                      (CGFloat) newHeight),
                           cgImage);

        CGImageRef cgImageNew = CGBitmapContextCreateImage(cgContext);

        [self setCgImage : cgImageNew];
        [self setNComponents : nComp];
        [self setColorSpace : [self resolveJavaColorSpace : CGColorSpaceGetModel(cgColorSpace)
                                                alphaInfo : bitmapInfo]];

        CGContextRelease(cgContext);

        IIOLog(@"Image was resized to %dx%d", newWidth, newHeight);
    }
}

-(CFDataRef) copyImagePixels : (CGImageRef) image {
    return CGDataProviderCopyData(CGImageGetDataProvider(image));
}

-(jbyteArray) getDecompressedBuffer : (JNIEnv *) env
                         imageIndex : (int) imageIndex {

    jboolean iscopy = FALSE;

    int bufferSize = [self width] * [self height] * [self nComponents];
    jbyteArray outBuffer = (*env)->NewByteArray(env, bufferSize);
    if (outBuffer == NULL) {
        throwException(env,
                       JAVA_OOM_ERROR,
                       "Cannot initilialize memory buffer for decoded image data");
    } else {
        jbyte *jByteBuffer = (*env)->GetPrimitiveArrayCritical(env,
                                                               outBuffer,
                                                               &iscopy);

        CFDataRef cfDataRef = [self copyImagePixels : cgImage];
        size_t dataLength = (size_t) CFDataGetLength(cfDataRef);
        memcpy(jByteBuffer, (UInt8 *) CFDataGetBytePtr(cfDataRef), dataLength);
        CFRelease(cfDataRef);

        (*env)->ReleasePrimitiveArrayCritical(env,
                                              outBuffer,
                                              jByteBuffer,
                                              JNI_ABORT);

        CGImageRelease(cgImage);

        // prepare the next image of an animated image
        if (imageIndex + 1 < nImages) {
            CGImageRef image = [self createImageAtIndex : imageIndex];
            [self setCgImage : image];
        }

        IIOLog(@"ImageLoader: sent %lu data to Java", dataLength);
    }

    return outBuffer;
}

@end
