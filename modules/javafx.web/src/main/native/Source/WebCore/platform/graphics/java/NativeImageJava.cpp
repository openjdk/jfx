/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"

#include "NotImplemented.h"

#include "BitmapImage.h"
#include "GraphicsContext.h"
#include "ImageObserver.h"
#include <wtf/java/JavaEnv.h>
#include "GraphicsContextJava.h"

#include "PlatformContextJava.h"
#include "RenderingQueue.h"
#include "SharedBuffer.h"
#include "Logging.h"

#include "com_sun_webkit_graphics_GraphicsDecoder.h"

namespace WebCore {

IntSize nativeImageSize(const NativeImagePtr& image)
{
    return image && image->frame() ? image->size() : IntSize();
}

bool nativeImageHasAlpha(const NativeImagePtr& image)
{
    return image && image->frame() && image->hasAlpha();
}

Color nativeImageSinglePixelSolidColor(const NativeImagePtr&)
{
    return {};
}

float subsamplingScale(GraphicsContext&, const FloatRect&, const FloatRect&)
{
    return 1;
}

void drawNativeImage(const NativeImagePtr& image, GraphicsContext& context, const FloatRect& destRect, const FloatRect& srcRect, const IntSize&, CompositeOperator op, BlendMode mode, const ImageOrientation& orientation)
{
    if (!image || !image->frame()) {
        return;
    }
    context.save();

    // Set the compositing operation.
    if (op == CompositeSourceOver && mode == BlendModeNormal && !nativeImageHasAlpha(image))
        context.setCompositeOperation(CompositeCopy);
    else
        context.setCompositeOperation(op, mode);

#if ENABLE(IMAGE_DECODER_DOWN_SAMPLING)
    IntSize scaledSize = nativeImageSize(image);
    FloatRect adjustedSrcRect = adjustSourceRectForDownSampling(srcRect, scaledSize);
#else
    FloatRect adjustedSrcRect(srcRect);
#endif

    FloatRect adjustedDestRect = destRect;

    if (orientation != DefaultImageOrientation) {
        // ImageOrientation expects the origin to be at (0, 0).
        context.translate(destRect.x(), destRect.y());
        adjustedDestRect.setLocation(FloatPoint());
        context.concatCTM(orientation.transformFromDefault(adjustedDestRect.size()));
        if (orientation.usesWidthAsHeight()) {
            // The destination rectangle will have it's width and height already reversed for the orientation of
            // the image, as it was needed for page layout, so we need to reverse it back here.
            adjustedDestRect.setSize(adjustedDestRect.size().transposedSize());
        }
    }

    context.platformContext()->rq().freeSpace(72)
        << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWIMAGE
        << image->frame()
        << adjustedDestRect.x() << adjustedDestRect.y()
        << adjustedDestRect.width() << adjustedDestRect.height()
        << adjustedSrcRect.x() << adjustedSrcRect.y()
        << adjustedSrcRect.width() << adjustedSrcRect.height();
    context.restore();
}

void clearNativeImageSubimages(const NativeImagePtr&)
{
    notImplemented();
}
}
