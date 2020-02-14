/*
 * Copyright (C) 2018 Apple Inc. All rights reserved.
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

#pragma once

#if ENABLE(WEBGPU)

#include "GPUBuffer.h"
#include "GPUOrigin3D.h"
#include "GPUTexture.h"
#include <wtf/HashSet.h>
#include <wtf/RefCounted.h>
#include <wtf/RefPtr.h>
#include <wtf/RetainPtr.h>

OBJC_PROTOCOL(MTLBlitCommandEncoder);
OBJC_PROTOCOL(MTLCommandBuffer);

namespace WebCore {

class GPUDevice;

struct GPUExtent3D;

using PlatformCommandBuffer = MTLCommandBuffer;
using PlatformCommandBufferSmartPtr = RetainPtr<MTLCommandBuffer>;

struct GPUBufferCopyViewBase {
    uint64_t offset;
    unsigned rowPitch;
    unsigned imageHeight;
};

struct GPUBufferCopyView final : GPUBufferCopyViewBase {
    GPUBufferCopyView(Ref<GPUBuffer>&& bufferCopy, const GPUBufferCopyViewBase& base)
        : GPUBufferCopyViewBase(base)
        , buffer(WTFMove(bufferCopy))
    {
    }

    Ref<GPUBuffer> buffer;
};

struct GPUTextureCopyViewBase {
    unsigned mipLevel;
    unsigned arrayLayer;
    GPUOrigin3D origin;
};

struct GPUTextureCopyView final : GPUTextureCopyViewBase {
    GPUTextureCopyView(Ref<GPUTexture>&& textureCopy, const GPUTextureCopyViewBase& base)
        : GPUTextureCopyViewBase(base)
        , texture(WTFMove(textureCopy))
    {
    }

    Ref<GPUTexture> texture;
};

class GPUCommandBuffer : public RefCounted<GPUCommandBuffer> {
public:
    static RefPtr<GPUCommandBuffer> tryCreate(const GPUDevice&);

    PlatformCommandBuffer* platformCommandBuffer() const { return m_platformCommandBuffer.get(); }
    const HashSet<Ref<GPUBuffer>>& usedBuffers() const { return m_usedBuffers; }
    const HashSet<Ref<GPUTexture>>& usedTextures() const { return m_usedTextures; }
    bool isEncodingPass() const { return m_isEncodingPass; }

    void setIsEncodingPass(bool isEncoding) { m_isEncodingPass = isEncoding; }
#if USE(METAL)
    ~GPUCommandBuffer();
    void endBlitEncoding();
#endif

    void copyBufferToBuffer(Ref<GPUBuffer>&&, uint64_t srcOffset, Ref<GPUBuffer>&&, uint64_t dstOffset, uint64_t size);
    void copyBufferToTexture(GPUBufferCopyView&&, GPUTextureCopyView&&, const GPUExtent3D&);
    void copyTextureToBuffer(GPUTextureCopyView&&, GPUBufferCopyView&&, const GPUExtent3D&);
    void copyTextureToTexture(GPUTextureCopyView&&, GPUTextureCopyView&&, const GPUExtent3D&);

    void useBuffer(Ref<GPUBuffer>&& buffer) { m_usedBuffers.addVoid(WTFMove(buffer)); }
    void useTexture(Ref<GPUTexture>&& texture) { m_usedTextures.addVoid(WTFMove(texture)); }

private:
    GPUCommandBuffer(PlatformCommandBufferSmartPtr&&);

    PlatformCommandBufferSmartPtr m_platformCommandBuffer;
    HashSet<Ref<GPUBuffer>> m_usedBuffers;
    HashSet<Ref<GPUTexture>> m_usedTextures;
    bool m_isEncodingPass { false };
#if USE(METAL)
    MTLBlitCommandEncoder *blitEncoder() const;
    mutable RetainPtr<MTLBlitCommandEncoder> m_blitEncoder;
#endif
};

} // namespace WebCore

#endif // ENABLE(WEBGPU)
