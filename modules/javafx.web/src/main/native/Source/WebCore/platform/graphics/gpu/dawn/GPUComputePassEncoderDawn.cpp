/*
 * Copyright (C) 2020 Sony Interactive Entertainment Inc.
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
#include "GPUComputePassEncoder.h"

#if ENABLE(WEBGPU)

namespace WebCore {

RefPtr<GPUComputePassEncoder> GPUComputePassEncoder::tryCreate(Ref<GPUCommandBuffer>&& buffer)
{
    return nullptr;
}

GPUComputePassEncoder::GPUComputePassEncoder(Ref<GPUCommandBuffer>&& buffer, PlatformComputePassEncoderSmartPtr&& encoder)
    : GPUProgrammablePassEncoder(WTFMove(buffer))
    , m_platformComputePassEncoder(WTFMove(encoder))
{
}

void GPUComputePassEncoder::setPipeline(Ref<const GPUComputePipeline>&& pipeline)
{
}

void GPUComputePassEncoder::dispatch(unsigned x, unsigned y, unsigned z)
{
}

const PlatformProgrammablePassEncoder* GPUComputePassEncoder::platformPassEncoder() const
{
    return nullptr;
}

} // namespace WebCore

#endif // ENABLE(WEBGPU)
