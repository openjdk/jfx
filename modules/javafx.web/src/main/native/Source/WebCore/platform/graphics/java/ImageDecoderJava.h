/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "ImageSource.h"
#include "IntSize.h"
#include "RQRef.h"
#include <wtf/Optional.h>

#include <jni.h>

namespace WebCore {

class ImageDecoder : public RefCounted<ImageDecoder> {
    WTF_MAKE_FAST_ALLOCATED;
public:
    ImageDecoder();
    ~ImageDecoder();

    static Ref<ImageDecoder> create(const SharedBuffer&, AlphaOption, GammaAndColorProfileOption)
    {
        return adoptRef(*new ImageDecoder());
    }

    static size_t bytesDecodedToDetermineProperties();

    String filenameExtension() const;
    bool isSizeAvailable() const;

    // Always original size, without subsampling.
    IntSize size() const;
    size_t frameCount() const;

    RepetitionCount repetitionCount() const;
    std::optional<IntPoint> hotSpot() const;

    IntSize frameSizeAtIndex(size_t, SubsamplingLevel = SubsamplingLevel::Default) const;
    bool frameIsCompleteAtIndex(size_t) const;
    ImageOrientation frameOrientationAtIndex(size_t) const;

    float frameDurationAtIndex(size_t) const;
    bool frameHasAlphaAtIndex(size_t) const;
    bool frameAllowSubsamplingAtIndex(size_t) const;
    unsigned frameBytesAtIndex(size_t, SubsamplingLevel = SubsamplingLevel::Default) const;

    NativeImagePtr createFrameImageAtIndex(size_t, SubsamplingLevel = SubsamplingLevel::Default, const std::optional<IntSize>& sizeForDraw = { });

    void setData(SharedBuffer&, bool allDataReceived);
    bool isAllDataReceived() const { return m_isAllDataReceived; }
    void clearFrameBufferCache(size_t) { }

    JLObject nativeDecoder() const { return m_nativeDecoder; }

protected:
    struct CachedFrameData {
        bool complete {};
        IntSize size;
        float duration {};
        bool hasAlpha {};
    };
    // To store meta data to avoid repeated JNI calls, initial size would be 1.
    Vector<RefPtr<ImageFrameData>> m_frameInfos;

    bool m_isAllDataReceived { false };
    size_t m_receivedDataSize { 0 };
    // Native Handle for Java object.
    JGObject m_nativeDecoder;
    mutable IntSize m_size;

    bool isMetaDataExists(size_t) const;
};

} // namespace WebCore
