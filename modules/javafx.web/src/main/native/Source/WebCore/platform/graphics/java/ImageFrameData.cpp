/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"

#include "ImageFrameData.h"

namespace WebCore {

ImageFrameData::ImageFrameData(RefPtr<RQRef> frame, const IntSize& size)
  : m_frame(frame),
    m_complete(true),
    m_size(size),
    m_duration(0),
    m_hasAlpha(false)
{
}

ImageFrameData::ImageFrameData(const JLObject& frame, const jint* meta)
  : m_frame(RQRef::create(frame)),
    m_complete(meta[0]),
    m_size(IntSize(meta[1], meta[2])),
    m_duration(meta[3] / 1000.0f),
    m_hasAlpha(meta[4])
{
}
} // namespace
