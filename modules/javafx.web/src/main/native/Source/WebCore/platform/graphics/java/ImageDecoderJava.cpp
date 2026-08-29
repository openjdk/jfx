/*
 * Copyright (c) 2017, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "ImageDecoderJava.h"

#include "NotImplemented.h"
#include "SharedBuffer.h"
#include "Logging.h"
#include "WKJPlatformJava.h"

namespace WebCore {

WTF_MAKE_TZONE_ALLOCATED_IMPL(ImageDecoderJava);

#ifndef NDEBUG
  struct ImageDecoderCounter {
    static int created;
    static int deleted;

    ~ImageDecoderCounter() {
      if ((created - deleted) != 0) {
          fprintf(stderr, "LEAK: %d image sources (%d - %d)\n",
                (created - deleted), created, deleted);
      }
    }
  };
  int ImageDecoderCounter::created = 0;
  int ImageDecoderCounter::deleted = 0;
  static ImageDecoderCounter sourceCounter;
#endif

ImageDecoderJava::ImageDecoderJava()
{
#ifndef NDEBUG
    ++ImageDecoderCounter::created;
#endif

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->get_image_decoder) {
        return;
    }

    m_nativeDecoder = WKJHandle(cb->get_image_decoder());

    wkjCheckAndClearException();
}

ImageDecoderJava::~ImageDecoderJava()
{
#ifndef NDEBUG
    ++ImageDecoderCounter::deleted;
#endif
    // The host table could be absent here, in case of deallocation of static BitmapImage
    // objects after the VM has gone; that is what the null environment check meant.
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_decoder_destroy || !m_nativeDecoder) {
        return;
    }

    cb->image_decoder_destroy(m_nativeDecoder.get());
    wkjCheckAndClearException();
}

void ImageDecoderJava::setData(const FragmentedSharedBuffer& data, bool allDataReceived)
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_decoder_add_image_data || !m_nativeDecoder) {
        return;
    }

    while (m_receivedDataSize < data.size()) {
        const auto& someData = data.getSomeData(m_receivedDataSize);
        unsigned length = someData.size();

        // The JNI version allocated a byte[] per chunk and skipped the chunk when the
        // allocation threw; there is no allocation to fail now, so every chunk is delivered.
        cb->image_decoder_add_image_data(m_nativeDecoder.get(), someData.span().data(),
                                         static_cast<int32_t>(length));
        wkjCheckAndClearException();

        m_receivedDataSize += length;
    }

    if (allDataReceived) {
        m_isAllDataReceived = true;
        // The end-of-stream call: a null array before, a null pointer with length 0 now.
        cb->image_decoder_add_image_data(m_nativeDecoder.get(), nullptr, 0);
        wkjCheckAndClearException();
    }
}

bool ImageDecoderJava::isSizeAvailable() const
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_decoder_get_image_size || !m_nativeDecoder) {
        return { };
    }

    // The JNI version passed the int[] straight to GetPrimitiveArrayCritical without testing
    // it for null, so a null return crashed. The slot reports it instead; m_size is then left
    // untouched, which is the only difference and it replaces undefined behaviour.
    int32_t wh[2] = { 0, 0 };
    if (!cb->image_decoder_get_image_size(m_nativeDecoder.get(), wh)) {
        wkjCheckAndClearException();
        return m_size.width();
    }
    wkjCheckAndClearException();

    m_size.setWidth(wh[0]);
    m_size.setHeight(wh[1]);

    return m_size.width();
}

size_t ImageDecoderJava::frameCount() const
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_decoder_get_frame_count || !m_nativeDecoder) {
        return { };
    }

    int32_t count = cb->image_decoder_get_frame_count(m_nativeDecoder.get());
    wkjCheckAndClearException();

    return count < 1
        ? 1
        : count;
}

PlatformImagePtr ImageDecoderJava::createFrameImageAtIndex(size_t idx, SubsamplingLevel, const DecodingOptions&)
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_decoder_get_frame || !m_nativeDecoder) {
        return { };
    }

    WKJHandle frame { cb->image_decoder_get_frame(m_nativeDecoder.get(), static_cast<int32_t>(idx)) };
    wkjCheckAndClearException();

    if (!frame)
        return nullptr;

    int32_t wh[2] = { 0, 0 };
    if (!cb->image_frame_get_size || !cb->image_frame_get_size(frame.get(), wh)) {
        return ImageJava::create(RQRef::create(frame.get()), nullptr, 0, 0);
    }

    IntSize frameSize(wh[0], wh[1]);

    return ImageJava::create(RQRef::create(frame.get()), nullptr, frameSize.width(), frameSize.height());
}

WTF::Seconds ImageDecoderJava::frameDurationAtIndex(size_t idx) const
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_decoder_get_frame_duration || !m_nativeDecoder) {
        return { };
    }

    int32_t duration = cb->image_decoder_get_frame_duration(m_nativeDecoder.get(),
                                                            static_cast<int32_t>(idx));
    return WTF::Seconds::fromMilliseconds(duration);
}

EncodedDataStatus ImageDecoderJava::encodedDataStatus() const
{
    if (m_encodedDataStatus == EncodedDataStatus::Complete)
    {
        return m_encodedDataStatus;
    }

    if (m_isAllDataReceived)
    {
        m_encodedDataStatus = EncodedDataStatus::Complete;
    }
    else if (isSizeAvailable())
    {
        m_encodedDataStatus = EncodedDataStatus::SizeAvailable;
    }

    return m_encodedDataStatus;
}

IntSize ImageDecoderJava::size() const
{
    return m_size;
}

IntSize ImageDecoderJava::frameSizeAtIndex(size_t idx, SubsamplingLevel) const
IntSize ImageDecoderJava::frameSizeAtIndex(size_t idx, SubsamplingLevel) const
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_decoder_get_frame_size || !m_nativeDecoder) {
        return { };
    }

    int32_t wh[2] = { 0, 0 };
    if (!cb->image_decoder_get_frame_size(m_nativeDecoder.get(), static_cast<int32_t>(idx), wh)) {
        return m_size;
    }

    return IntSize(wh[0], wh[1]);
}

bool ImageDecoderJava::frameAllowSubsamplingAtIndex(size_t) const
{
    notImplemented();
    return true;
}

bool ImageDecoderJava::frameHasAlphaAtIndex(size_t) const
{
    // FIXME-java: Read it from ImageMetadata
    return true;
}

bool ImageDecoderJava::frameIsCompleteAtIndex(size_t idx) const
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_decoder_get_frame_complete || !m_nativeDecoder) {
        return false;
    }

    return cb->image_decoder_get_frame_complete(m_nativeDecoder.get(),
                                                static_cast<int32_t>(idx)) != 0;
}

unsigned ImageDecoderJava::frameBytesAtIndex(size_t idx, SubsamplingLevel samplingLevel) const
{
    auto frameSize = frameSizeAtIndex(idx, samplingLevel);
    return (frameSize.area() * 4);
}

RepetitionCount ImageDecoderJava::repetitionCount() const
{
    return RepetitionCountInfinite;
}

String ImageDecoderJava::filenameExtension() const
{
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->image_decoder_get_filename_extension || !m_nativeDecoder) {
        return { };
    }

    String ext = wkjFetchString([&](uint16_t* buf, int32_t cap, int32_t* length) {
        return cb->image_decoder_get_filename_extension(m_nativeDecoder.get(), buf, cap, length);
    });
    wkjCheckAndClearException();

    // The JNI String constructor collapsed a null Java string to the empty String; keep that.
    return ext.isNull() ? emptyString() : ext;
}

std::optional<IntPoint> ImageDecoderJava::hotSpot() const
{
    notImplemented();
    return { };
}

size_t ImageDecoderJava::bytesDecodedToDetermineProperties() const
{
    // Set to match value used for CoreGraphics.
    return 13088;
}
} // namespace WebCore
