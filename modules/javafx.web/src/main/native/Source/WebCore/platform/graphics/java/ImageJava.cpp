/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "Image.h"
#include "ImageObserver.h"
#include "ImageBuffer.h"
#include "FloatRect.h"
#include "GraphicsContext.h"
#include "TransformationMatrix.h"
#include "PlatformJavaClasses.h"
#include "com_sun_webkit_graphics_GraphicsDecoder.h"
#include "GraphicsContextJava.h"
#include "PlatformContextJava.h"
#include "Logging.h"

class ImageBuffer;

namespace WebCore {

void Image::drawImage(GraphicsContext& gc, const FloatRect &dstRect, const FloatRect &srcRect,
                       CompositeOperator compositeOperator, BlendMode)
{
    if (gc.paintingDisabled()) {
        return;
    }

    auto nativeImage = nativeImageForCurrentFrame();
    if (!nativeImage) {
        return;
    }

    CompositeOperator oldCompositeOperator = gc.compositeOperation();
    gc.setCompositeOperation(compositeOperator);

    gc.platformContext()->rq().freeSpace(72)
    << (jint)com_sun_webkit_graphics_GraphicsDecoder_DRAWIMAGE
    << nativeImage->platformImage()->getImage()
    << dstRect.x() << dstRect.y()
    << dstRect.width() << dstRect.height()
    << srcRect.x() << srcRect.y()
    << srcRect.width() << srcRect.height();

    gc.setCompositeOperation(oldCompositeOperator);

    if (imageObserver())
        imageObserver()->didDraw(*this);
}

Ref<Image> ImageAdapter::loadPlatformResource(const char *name)
{
    return BitmapImage::createFromName(name);
}

void ImageAdapter::invalidate()
{
}
#if !USE(IMAGEIO)
NativeImagePtr ImageFrame::asNewNativeImage() const
{
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID s_createWCImage_mID = env->GetMethodID(
            PG_GetGraphicsManagerClass(env), "createFrame",
            "(IILjava/nio/ByteBuffer;)Lcom/sun/webkit/graphics/WCImageFrame;");
    ASSERT(s_createWCImage_mID);

    JLObject data(env->NewDirectByteBuffer(
            m_bytes,
            width() * height() * sizeof(PixelData)));
    ASSERT(data);
    if (!data) {
        return nullptr;
    }

    JLObject frame(env->CallObjectMethod(
        PL_GetGraphicsManager(env),
        s_createWCImage_mID,
        width(),
        height(),
        (jobject)data));
    ASSERT(frame);
    if (WTF::CheckAndClearException(env) || !frame) {
        return nullptr;
    }

    return RQRef::create(frame);
}
#endif
} // namespace WebCore
