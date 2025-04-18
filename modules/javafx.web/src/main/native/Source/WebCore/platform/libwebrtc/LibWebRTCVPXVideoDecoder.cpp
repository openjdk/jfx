/*
 * Copyright (C) 2022 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "LibWebRTCVPXVideoDecoder.h"

#if USE(LIBWEBRTC) && PLATFORM(COCOA)

#include "CVUtilities.h"
#include "IOSurface.h"
#include "LibWebRTCDav1dDecoder.h"
#include "Logging.h"
#include "VideoFrameLibWebRTC.h"
#include <wtf/FastMalloc.h>
#include <wtf/NeverDestroyed.h>
#include <wtf/ThreadSafeRefCounted.h>
#include <wtf/WorkQueue.h>
#include <wtf/text/MakeString.h>

ALLOW_UNUSED_PARAMETERS_BEGIN
ALLOW_COMMA_BEGIN

#include <webrtc/api/environment/environment_factory.h>
#include <webrtc/modules/video_coding/codecs/vp8/include/vp8.h>
#include <webrtc/modules/video_coding/codecs/vp9/include/vp9.h>
#include <webrtc/system_wrappers/include/cpu_info.h>
#include <webrtc/webkit_sdk/WebKit/WebKitDecoder.h>

ALLOW_COMMA_END
ALLOW_UNUSED_PARAMETERS_END

#include "CoreVideoSoftLink.h"

namespace WebCore {

static WorkQueue& vpxDecoderQueue()
{
    static std::once_flag onceKey;
    static LazyNeverDestroyed<Ref<WorkQueue>> queue;
    std::call_once(onceKey, [] {
        queue.construct(WorkQueue::create("VPx VideoDecoder Queue"_s));
    });
    return queue.get();
}

class LibWebRTCVPXInternalVideoDecoder : public ThreadSafeRefCounted<LibWebRTCVPXInternalVideoDecoder> , public webrtc::DecodedImageCallback {
public:
    static Ref<LibWebRTCVPXInternalVideoDecoder> create(LibWebRTCVPXVideoDecoder::Type type, const VideoDecoder::Config& config, VideoDecoder::OutputCallback&& outputCallback, VideoDecoder::PostTaskCallback&& postTaskCallback) { return adoptRef(*new LibWebRTCVPXInternalVideoDecoder(type, config, WTFMove(outputCallback), WTFMove(postTaskCallback))); }
    ~LibWebRTCVPXInternalVideoDecoder() = default;

    void postTask(Function<void()>&& task) { m_postTaskCallback(WTFMove(task)); }
    void decode(std::span<const uint8_t>, bool isKeyFrame, int64_t timestamp, std::optional<uint64_t> duration,  VideoDecoder::DecodeCallback&&);
    void close() { m_isClosed = true; }
private:
    LibWebRTCVPXInternalVideoDecoder(LibWebRTCVPXVideoDecoder::Type, const VideoDecoder::Config&, VideoDecoder::OutputCallback&&, VideoDecoder::PostTaskCallback&&);
    int32_t Decoded(webrtc::VideoFrame&) final;
    CVPixelBufferPoolRef pixelBufferPool(size_t width, size_t height, OSType) WTF_REQUIRES_LOCK(m_pixelBufferPoolLock);
    CVPixelBufferRef createPixelBuffer(size_t width, size_t height, webrtc::BufferType);

    VideoDecoder::OutputCallback m_outputCallback;
    VideoDecoder::PostTaskCallback m_postTaskCallback;
    UniqueRef<webrtc::VideoDecoder> m_internalDecoder;
    int64_t m_timestamp { 0 };
    std::optional<uint64_t> m_duration;
    bool m_isClosed { false };
    bool m_useIOSurface { false };
    ProcessIdentity m_resourceOwner;
    RetainPtr<CVPixelBufferPoolRef> m_pixelBufferPool WTF_GUARDED_BY_LOCK(m_pixelBufferPoolLock);
    Lock m_pixelBufferPoolLock;
    size_t m_pixelBufferPoolWidth { 0 };
    size_t m_pixelBufferPoolHeight { 0 };
    OSType m_pixelBufferPoolType;
};

void LibWebRTCVPXVideoDecoder::create(Type type, const Config& config, CreateCallback&& callback, OutputCallback&& outputCallback, PostTaskCallback&& postTaskCallback)
{
    auto decoder = makeUniqueRef<LibWebRTCVPXVideoDecoder>(type, config, WTFMove(outputCallback), WTFMove(postTaskCallback));
    vpxDecoderQueue().dispatch([callback = WTFMove(callback), decoder = WTFMove(decoder)]() mutable {
        auto internalDecoder = decoder->m_internalDecoder;
        internalDecoder->postTask([callback = WTFMove(callback), decoder = WTFMove(decoder)]() mutable {
            callback(UniqueRef<VideoDecoder> { WTFMove(decoder) });
        });
    });
}

LibWebRTCVPXVideoDecoder::LibWebRTCVPXVideoDecoder(Type type, const Config& config, OutputCallback&& outputCallback, PostTaskCallback&& postTaskCallback)
    : m_internalDecoder(LibWebRTCVPXInternalVideoDecoder::create(type, config, WTFMove(outputCallback), WTFMove(postTaskCallback)))
{
}

LibWebRTCVPXVideoDecoder::~LibWebRTCVPXVideoDecoder()
{
}

void LibWebRTCVPXVideoDecoder::decode(EncodedFrame&& frame, DecodeCallback&& callback)
{
    vpxDecoderQueue().dispatch([value = Vector<uint8_t> { frame.data }, isKeyFrame = frame.isKeyFrame, timestamp = frame.timestamp, duration = frame.duration, decoder = m_internalDecoder, callback = WTFMove(callback)]() mutable {
        decoder->decode({ value.data(), value.size() }, isKeyFrame, timestamp, duration, WTFMove(callback));
    });
}

void LibWebRTCVPXVideoDecoder::flush(Function<void()>&& callback)
{
    vpxDecoderQueue().dispatch([decoder = m_internalDecoder, callback = WTFMove(callback)]() mutable {
        decoder->postTask(WTFMove(callback));
    });
}

void LibWebRTCVPXVideoDecoder::reset()
{
    m_internalDecoder->close();
}

void LibWebRTCVPXVideoDecoder::close()
{
    m_internalDecoder->close();
}


void LibWebRTCVPXInternalVideoDecoder::decode(std::span<const uint8_t> data, bool isKeyFrame, int64_t timestamp, std::optional<uint64_t> duration,  VideoDecoder::DecodeCallback&& callback)
{
    m_timestamp = timestamp;
    m_duration = duration;

    webrtc::EncodedImage image;
    image.SetEncodedData(webrtc::WebKitEncodedImageBufferWrapper::create(const_cast<uint8_t*>(data.data()), data.size()));
    image._frameType = isKeyFrame ? webrtc::VideoFrameType::kVideoFrameKey : webrtc::VideoFrameType::kVideoFrameDelta;

    auto error = m_internalDecoder->Decode(image, false, 0);

    m_postTaskCallback([protectedThis = Ref { *this }, error, callback = WTFMove(callback)]() mutable {
        if (protectedThis->m_isClosed)
            return;

        if (error)
            protectedThis->m_outputCallback(makeUnexpected(makeString("VPx decoding failed with error "_s, error)));

        callback({ });
    });
}

static UniqueRef<webrtc::VideoDecoder> createInternalDecoder(LibWebRTCVPXVideoDecoder::Type type)
{
    switch (type) {
    case LibWebRTCVPXVideoDecoder::Type::VP8:
        return makeUniqueRefFromNonNullUniquePtr(webrtc::CreateVp8Decoder(webrtc::EnvironmentFactory().Create()));
    case LibWebRTCVPXVideoDecoder::Type::VP9:
        return makeUniqueRefFromNonNullUniquePtr(webrtc::VP9Decoder::Create());
    case LibWebRTCVPXVideoDecoder::Type::VP9_P2:
        return makeUniqueRefFromNonNullUniquePtr(webrtc::VP9Decoder::Create());
#if ENABLE(AV1)
    case LibWebRTCVPXVideoDecoder::Type::AV1:
        return createLibWebRTCDav1dDecoder();
#endif
    }
}

LibWebRTCVPXInternalVideoDecoder::LibWebRTCVPXInternalVideoDecoder(LibWebRTCVPXVideoDecoder::Type type, const VideoDecoder::Config& config, VideoDecoder::OutputCallback&& outputCallback, VideoDecoder::PostTaskCallback&& postTaskCallback)
    : m_outputCallback(WTFMove(outputCallback))
    , m_postTaskCallback(WTFMove(postTaskCallback))
    , m_internalDecoder(createInternalDecoder(type))
    , m_useIOSurface(config.pixelBuffer == VideoDecoder::HardwareBuffer::Yes)
    , m_resourceOwner(config.resourceOwner)
{
    m_internalDecoder->RegisterDecodeCompleteCallback(this);
    webrtc::VideoDecoder::Settings settings;
    settings.set_number_of_cores(webrtc::CpuInfo::DetectNumberOfCores());
    m_internalDecoder->Configure(settings);
}

CVPixelBufferPoolRef LibWebRTCVPXInternalVideoDecoder::pixelBufferPool(size_t width, size_t height, OSType type)
{
    if (!m_pixelBufferPool || m_pixelBufferPoolWidth != width || m_pixelBufferPoolHeight != height || m_pixelBufferPoolType != type) {
        auto result = createCVPixelBufferPool(width, height, type, 0u, false, m_useIOSurface);
        if (!result) {
            RELEASE_LOG_ERROR(Media, "LibWebRTCVPXInternalVideoDecoder failed creating buffer pool with error %d", result.error());
            return nullptr;
        }

        m_pixelBufferPool = WTFMove(*result);
        m_pixelBufferPoolWidth = width;
        m_pixelBufferPoolHeight = height;
        m_pixelBufferPoolType = type;
    }
    return m_pixelBufferPool.get();
}

CVPixelBufferRef LibWebRTCVPXInternalVideoDecoder::createPixelBuffer(size_t width, size_t height, webrtc::BufferType bufferType)
{
                OSType pixelBufferType;
                switch (bufferType) {
                case webrtc::BufferType::I420:
                    pixelBufferType = kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange;
                    break;
                case webrtc::BufferType::I010:
                    pixelBufferType = kCVPixelFormatType_420YpCbCr10BiPlanarVideoRange;
                    break;
                default:
                    return nullptr;
                }

                CVPixelBufferRef pixelBuffer = nullptr;
                CVReturn status = kCVReturnError;

    {
        Locker locker(m_pixelBufferPoolLock);
        if (auto bufferPool = pixelBufferPool(width, height, pixelBufferType))
                    status = CVPixelBufferPoolCreatePixelBuffer(nullptr, bufferPool, &pixelBuffer);
    }

                if (status != kCVReturnSuccess || !pixelBuffer) {
                    RELEASE_LOG_ERROR(Media, "Failed creating a pixel buffer for converting a VPX frame with error %d", status);
                    return nullptr;
                }

    if (m_resourceOwner) {
        if (auto surface = CVPixelBufferGetIOSurface(pixelBuffer))
            IOSurface::setOwnershipIdentity(surface, m_resourceOwner);
    }

                return pixelBuffer;
}

int32_t LibWebRTCVPXInternalVideoDecoder::Decoded(webrtc::VideoFrame& frame)
{
    m_postTaskCallback([protectedThis = Ref { *this }, colorSpace = VideoFrameLibWebRTC::colorSpaceFromFrame(frame), buffer = frame.video_frame_buffer(), timestamp = m_timestamp, duration = m_duration]() mutable {
        if (protectedThis->m_isClosed)
            return;

        auto videoFrame = VideoFrameLibWebRTC::create({ }, false, VideoFrame::Rotation::None, WTFMove(colorSpace), WTFMove(buffer), [protectedThis] (auto& buffer) {
            return adoptCF(webrtc::createPixelBufferFromFrameBuffer(buffer, [protectedThis] (size_t width, size_t height, webrtc::BufferType bufferType) -> CVPixelBufferRef {
                return protectedThis->createPixelBuffer(width, height, bufferType);
            }));
        });

        protectedThis->m_outputCallback(VideoDecoder::DecodedFrame { WTFMove(videoFrame), timestamp, duration });
    });
    return 0;
}

}

#endif // USE(LIBWEBRTC)
