/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "NotImplemented.h"

#include "BitmapImage.h"
#include "Image.h"
#include "ImageObserver.h"
#include "ImageBuffer.h"
#include "ImageDecoder.h"
#include "FloatRect.h"
#include "GraphicsContext.h"
#include "TransformationMatrix.h"
#include "JavaEnv.h"
#include "com_sun_webkit_graphics_GraphicsDecoder.h"
#include "GraphicsContextJava.h"
#include "Logging.h"

class ImageBuffer;

namespace WebCore {

void Image::drawPattern(GraphicsContext *gc, const FloatRect& srcRect, const AffineTransform& patternTransform,
                        const FloatPoint& phase, ColorSpace, CompositeOperator, const FloatRect& destRect)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    if (!gc || gc->paintingDisabled() || srcRect.isEmpty()) {
        return;
    }

    NativeImagePtr currFrame = nativeImageForCurrentFrame();
    if (!currFrame) {
        return;
    }

    TransformationMatrix tm = patternTransform.toTransformationMatrix();

    static jmethodID mid = env->GetMethodID(PG_GetGraphicsManagerClass(env),
                "createTransform",
                "(DDDDDD)Lcom/sun/webkit/graphics/WCTransform;");
    ASSERT(mid);
    JLObject transform(env->CallObjectMethod(PL_GetGraphicsManager(env), mid,
                tm.a(), tm.b(), tm.c(), tm.d(), tm.e(), tm.f()));
    ASSERT(transform);
    CheckAndClearException(env);

    gc->platformContext()->rq().freeSpace(13 * 4)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWPATTERN
    << currFrame
    << srcRect.x() << srcRect.y() << srcRect.width() << srcRect.height()
    << RQRef::create(transform)
    << phase.x() << phase.y()
    << destRect.x() << destRect.y() << destRect.width() << destRect.height();

    if (imageObserver())
        imageObserver()->didDraw(this);
}

void Image::drawImage(GraphicsContext *gc, const FloatRect &dstRect, const FloatRect &srcRect,
                       ColorSpace, CompositeOperator)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    if (!gc || gc->paintingDisabled()) {
        return;
    }

    NativeImagePtr currFrame = nativeImageForCurrentFrame();
    if (!currFrame) {
        return;
    }

    gc->platformContext()->rq().freeSpace(72)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWIMAGE
    << currFrame
    << dstRect.x() << dstRect.y()
    << dstRect.width() << dstRect.height()
    << srcRect.x() << srcRect.y()
    << srcRect.width() << srcRect.height();

    if (imageObserver())
        imageObserver()->didDraw(this);
}

PassRefPtr<Image> Image::loadPlatformResource(const char *name)
{
    return BitmapImage::createFromName(name);
}

#if !USE(IMAGEIO)
NativeImagePtr ImageFrame::asNewNativeImage() const
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID s_createWCImage_mID = env->GetMethodID(
            PG_GetGraphicsManagerClass(env), "createFrame",
            "(IILjava/nio/ByteBuffer;)Lcom/sun/webkit/graphics/WCImageFrame;");
    ASSERT(s_createWCImage_mID);

    JLObject data(env->NewDirectByteBuffer(
            m_bytes,
            width() * height() * sizeof(PixelData)));
    ASSERT(data);

    JLObject frame(env->CallObjectMethod(
        PL_GetGraphicsManager(env),
        s_createWCImage_mID,
        width(),
        height(),
        (jobject)data));
    ASSERT(frame);
    CheckAndClearException(env);

    return RQRef::create(frame);
}
#endif
} // namespace WebCore
