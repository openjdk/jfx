/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "BitmapImage.h"
#include "NotImplemented.h"
#include "GraphicsContext.h"
#include "ImageObserver.h"
#include "GraphicsContextJava.h"
#include "PlatformContextJava.h"
#include "ImageDecoderJava.h"
#include "RenderingQueue.h"
#include "SharedBuffer.h"
#include "WKJPlatformJava.h"

namespace WebCore {

Ref<Image> BitmapImage::createFromName(const char* name)
{
    Ref<BitmapImage> img(create());

#if USE(IMAGEIO)
    // This is the branch the build compiles, and it has been inert for a long time: the only
    // JNI it contained was a method-id lookup for WCImageDecoder.loadFromResource whose call
    // was commented out below, so createFromName has been returning an empty BitmapImage.
    // The lookup went out with the id cache; the commented-out call is left exactly as found,
    // because reviving it is a behaviour change and not this commit's business.
    SharedBufferBuilder bufferBuilder;
    //RefPtr<SharedBuffer> dataBuffer(SharedBuffer::create());
    //img->m_source->ensureDecoderAvailable(dataBuffer.get());
    //img->m_source->ensureDecoderAvailable(bufferBuilder.take().ptr());    //revisit
  /*  WCImageDecoder.loadFromResource(name), on
        static_cast<ImageDecoderJava*>(img->m_source->m_decoder.get())->nativeDecoder().
        There is no host slot for it: nothing live has ever made this call. */

    // we have to make this call in order to initialize
    // internal flags that indicates the image readiness
   // img->encodedDataStatus();

    // Absence if the image size indicates some problem with
    // the availability of the resource referred by the name.
    // It should never happen if resources are set up correctly,
    // however it does happen after OOME
//    ASSERT(isSizeAvailable);
#else
    // Not compiled: USE(IMAGEIO) is unconditionally on. Converted rather than deleted so the
    // branch stays translatable. NOTE, pre-existing and left alone: `dataBuffer` below is
    // undeclared - only `bufferBuilder` is - so this branch has not compiled for some time.
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->load_from_resource)
        return WTF::move(img);

    SharedBufferBuilder bufferBuilder;
    WKJStringArg resourceName(String::fromLatin1(name));
    ASSERT(resourceName.data());

    cb->load_from_resource(resourceName.data(), resourceName.length(),
                           wkj_from_ptr((bufferBuilder.get()).get()));
    wkjCheckAndClearException();
    //From the upper call we got a callback [wkj_shared_buffer_builder_append]
    //that fills the buffer.
    img->setData(WTF::move(dataBuffer), true);
#endif
    return WTF::move(img);
}


} // namespace WebCore

extern "C" {

WKJ_EXPORT void wkj_shared_buffer_builder_append(int64_t builder,
                                                 const uint8_t* data, int32_t count)
{
    using namespace WebCore;

    ASSERT(builder);
    SharedBufferBuilder* pBuffer = static_cast<SharedBufferBuilder*>(wkj_to_ptr(builder));

    pBuffer->append(std::span<const uint8_t>(data, count));
}

}//extern "C"
