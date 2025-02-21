/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#include "ImageDecoder.h"
#include "ImageSource.h"
#include "IntSize.h"
#include "SharedBuffer.h"
#include "RQRef.h"

#include <jni.h>

namespace WebCore {

class ImageDecoderJava : public ImageDecoder {
    WTF_MAKE_FAST_ALLOCATED;
public:
    ImageDecoderJava();
    ~ImageDecoderJava();

    static bool supportsMediaType(MediaType type) { return type == MediaType::Image; }

    static Ref<ImageDecoder> create(const FragmentedSharedBuffer&, AlphaOption, GammaAndColorProfileOption)
    {
        return adoptRef(*new ImageDecoderJava());
    }

    size_t bytesDecodedToDetermineProperties() const final;

    String filenameExtension() const final;
    bool isSizeAvailable() const final;

    EncodedDataStatus encodedDataStatus() const final;
    // Always original size, without subsampling.
    IntSize size() const final;
    size_t frameCount() const final;

    RepetitionCount repetitionCount() const final;
    std::optional<IntPoint> hotSpot() const final;

    IntSize frameSizeAtIndex(size_t, SubsamplingLevel = SubsamplingLevel::Default) const final;
    bool frameIsCompleteAtIndex(size_t) const final;

    WTF::Seconds frameDurationAtIndex(size_t) const final;
    bool frameHasAlphaAtIndex(size_t) const final;
    bool frameAllowSubsamplingAtIndex(size_t) const;
    unsigned frameBytesAtIndex(size_t, SubsamplingLevel = SubsamplingLevel::Default) const final;

    PlatformImagePtr createFrameImageAtIndex(size_t, SubsamplingLevel = SubsamplingLevel::Default, const DecodingOptions& = DecodingOptions(DecodingMode::Synchronous)) final;

    void setData(const FragmentedSharedBuffer&, bool allDataReceived) final;
    bool isAllDataReceived() const final { return m_isAllDataReceived;}
    void clearFrameBufferCache(size_t) final {  }

    JLObject nativeDecoder() const { return m_nativeDecoder; }

protected:
    bool m_isAllDataReceived { false };
    size_t m_receivedDataSize { 0 };
    mutable EncodedDataStatus m_encodedDataStatus { EncodedDataStatus::Unknown };
    // Native Handle for Java object.
    JGObject m_nativeDecoder;
    mutable IntSize m_size;
};

} // namespace WebCore
