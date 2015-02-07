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

#import <UIKit/UIKit.h>
#import <ImageIO/ImageIO.h>

#include "jni.h"


@interface ImageLoader : NSObject {

@private
    NSMutableData *buffer;
    CGImageSourceRef cgImageSource;
    CGImageRef cgImage;

    int width;
    int height;
    int nComponents;
    int colorSpace;
    int nImages;
    int delayTime;
    int loopCount;
}

@property (nonatomic, retain) NSMutableData* buffer;
@property (nonatomic) CGImageSourceRef cgImageSource;
@property (nonatomic) CGImageRef cgImage;

@property (nonatomic) int width;
@property (nonatomic) int height;
@property (nonatomic) int nComponents;
@property (nonatomic) int colorSpace;
@property (nonatomic) int nImages;
@property (nonatomic) int delayTime;
@property (nonatomic) int loopCount;


- (id) init;

- (void) dealloc;

- (void) addToBuffer: (const void *) bytes
              length: (int) length;

- (BOOL) loadFromBuffer: (JNIEnv *) env;

- (BOOL) loadFromURL: (NSString *) urlString
              JNIEnv: (JNIEnv *) env;

- (void) resize: (int) width
               : (int) height;

- (jbyteArray) getDecompressedBuffer: (JNIEnv *) env
                          imageIndex: (int) imageIndex;


@end
