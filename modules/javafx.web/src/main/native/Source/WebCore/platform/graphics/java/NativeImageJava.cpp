/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "NotImplemented.h"

#include "BitmapImage.h"
#include "GraphicsContext.h"
#include "ImageObserver.h"
#include "PlatformJavaClasses.h"
#include "GraphicsContextJava.h"

#include "PlatformContextJava.h"
#include "RenderingQueue.h"
#include "SharedBuffer.h"
#include "Logging.h"

#include "com_sun_webkit_graphics_GraphicsDecoder.h"

namespace WebCore {

IntSize nativeImageSize(const NativeImagePtr& image)
{
    if (!image) {
        return {};
    }

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID midGetSize = env->GetMethodID(
        PG_GetImageFrameClass(env),
        "getSize",
        "()[I");
    ASSERT(midGetSize);
    JLocalRef<jintArray> jsize((jintArray)env->CallObjectMethod(
                        jobject(*image.get()),
                        midGetSize));
    if (!jsize) {
        return {};
    }

    jint* size = (jint*)env->GetPrimitiveArrayCritical((jintArray)jsize, 0);
    IntSize frameSize(size[0], size[1]);
    env->ReleasePrimitiveArrayCritical(jsize, size, 0);
    return frameSize;
}

bool nativeImageHasAlpha(const NativeImagePtr&)
{
    // FIXME-java: Get alpha details from ImageMetadata class
    return true;
}

Color nativeImageSinglePixelSolidColor(const NativeImagePtr&)
{
    return {};
}

float subsamplingScale(GraphicsContext&, const FloatRect&, const FloatRect&)
{
    return 1;
}

void drawNativeImage(const NativeImagePtr& image,
    GraphicsContext& context,
    const FloatRect& destRect,
    const FloatRect& srcRect,
    const IntSize&,
    const ImagePaintingOptions& options)
{
    if (!image) {
        return;
    }
    context.save();

    // Set the compositing operation.
    if (options.compositeOperator() == CompositeOperator::SourceOver && options.blendMode() == BlendMode::Normal && !nativeImageHasAlpha(image))
        context.setCompositeOperation(CompositeOperator::Copy);
    else
        context.setCompositeOperation(options.compositeOperator(), options.blendMode());

#if ENABLE(IMAGE_DECODER_DOWN_SAMPLING)
    IntSize scaledSize = nativeImageSize(image);
    FloatRect adjustedSrcRect = adjustSourceRectForDownSampling(srcRect, scaledSize);
#else
    FloatRect adjustedSrcRect(srcRect);
#endif

    FloatRect adjustedDestRect = destRect;

    if (options.orientation() != ImageOrientation::None) {
        // ImageOrientation expects the origin to be at (0, 0).
        context.translate(destRect.x(), destRect.y());
        adjustedDestRect.setLocation(FloatPoint());
        context.concatCTM(options.orientation().transformFromDefault(adjustedDestRect.size()));
        if (options.orientation().usesWidthAsHeight()) {
            // The destination rectangle will have it's width and height already reversed for the orientation of
            // the image, as it was needed for page layout, so we need to reverse it back here.
            adjustedDestRect.setSize(adjustedDestRect.size().transposedSize());
        }
    }

    context.platformContext()->rq().freeSpace(72)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWIMAGE
        << image
        << adjustedDestRect.x() << adjustedDestRect.y()
        << adjustedDestRect.width() << adjustedDestRect.height()
        << adjustedSrcRect.x() << adjustedSrcRect.y()
        << adjustedSrcRect.width() << adjustedSrcRect.height();
    context.restore();
}


// void drawNativeImage(const NativeImagePtr& image, GraphicsContext& context, const FloatRect& destRect, const FloatRect& srcRect, const IntSize&, CompositeOperator op, BlendMode mode, const ImageOrientation& orientation)
// {
//     if (!image) {
//         return;
//     }
//     context.save();

//     // Set the compositing operation.
//     if (op == CompositeSourceOver && mode == BlendMode::Normal && !nativeImageHasAlpha(image))
//         context.setCompositeOperation(CompositeOperator::Copy);
//     else
//         context.setCompositeOperation(op, mode);

// #if ENABLE(IMAGE_DECODER_DOWN_SAMPLING)
//     IntSize scaledSize = nativeImageSize(image);
//     FloatRect adjustedSrcRect = adjustSourceRectForDownSampling(srcRect, scaledSize);
// #else
//     FloatRect adjustedSrcRect(srcRect);
// #endif

//     FloatRect adjustedDestRect = destRect;

//     if (orientation != ImageOrientation::None) {
//         // ImageOrientation expects the origin to be at (0, 0).
//         context.translate(destRect.x(), destRect.y());
//         adjustedDestRect.setLocation(FloatPoint());
//         context.concatCTM(orientation.transformFromDefault(adjustedDestRect.size()));
//         if (orientation.usesWidthAsHeight()) {
//             // The destination rectangle will have it's width and height already reversed for the orientation of
//             // the image, as it was needed for page layout, so we need to reverse it back here.
//             adjustedDestRect.setSize(adjustedDestRect.size().transposedSize());
//         }
//     }

//     context.platformContext()->rq().freeSpace(72)
//         << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWIMAGE
//         << image
//         << adjustedDestRect.x() << adjustedDestRect.y()
//         << adjustedDestRect.width() << adjustedDestRect.height()
//         << adjustedSrcRect.x() << adjustedSrcRect.y()
//         << adjustedSrcRect.width() << adjustedSrcRect.height();
//     context.restore();
// }

void clearNativeImageSubimages(const NativeImagePtr&)
{
    notImplemented();
}
}
