/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include <wtf/RefPtr.h>
#include <jni.h>

#include "IntSize.h"
#include "RQRef.h"

namespace WebCore {

class ImageDecoder;

class ImageFrameData : public RefCounted<ImageFrameData> {
public:
    static RefPtr<ImageFrameData> create(const JLObject& frame, const jint* meta) {
        return meta ? adoptRef(new ImageFrameData(frame, meta)) : nullptr;
    }

    static RefPtr<ImageFrameData> create(RefPtr<RQRef> frame, const IntSize& size) {
        return adoptRef(new ImageFrameData(frame, size));
    }

    RefPtr<RQRef> frame() const { return m_frame; }
    IntSize size() const { return m_size; }
    bool hasAlpha() const { return m_hasAlpha; }
    operator RefPtr<RQRef>() const { return m_frame; }

private:
    friend class ImageDecoder;
    ImageFrameData(RefPtr<RQRef> frame, const IntSize& size);
    ImageFrameData(const JLObject& frame, const jint* meta);

    RefPtr<RQRef> m_frame;
    bool m_complete;
    IntSize m_size;
    float m_duration;
    bool m_hasAlpha;

    ImageFrameData() = delete;
};

} // namespace
