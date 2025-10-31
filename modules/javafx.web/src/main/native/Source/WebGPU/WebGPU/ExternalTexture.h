/*
 * Copyright (c) 2023 Apple Inc. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#import "Device.h"
#import <wtf/Ref.h>
#import <wtf/RefCountedAndCanMakeWeakPtr.h>
#import <wtf/TZoneMalloc.h>
#import <wtf/WeakHashSet.h>
#import <wtf/WeakPtr.h>

using CVPixelBufferRef = struct __CVBuffer*;

struct WGPUExternalTextureImpl {
};

namespace WebGPU {

class CommandEncoder;

class ExternalTexture : public RefCountedAndCanMakeWeakPtr<ExternalTexture>, public WGPUExternalTextureImpl {
    WTF_MAKE_TZONE_ALLOCATED(ExternalTexture);
public:
    static Ref<ExternalTexture> create(CVPixelBufferRef pixelBuffer, WGPUColorSpace colorSpace, Device& device)
    {
        return adoptRef(*new ExternalTexture(pixelBuffer, colorSpace, device));
    }
    static Ref<ExternalTexture> createInvalid(Device& device)
    {
        return adoptRef(*new ExternalTexture(device));
    }

    ~ExternalTexture();

    CVPixelBufferRef pixelBuffer() const { return m_pixelBuffer.get(); }
    WGPUColorSpace colorSpace() const { return m_colorSpace; }

    void destroy();
    void undestroy();
    void setCommandEncoder(CommandEncoder&) const;
    bool isDestroyed() const;

    bool isValid() const;
    void update(CVPixelBufferRef);
    size_t openCommandEncoderCount() const;
    void updateExternalTextures(id<MTLTexture>, id<MTLTexture>);

private:
    ExternalTexture(CVPixelBufferRef, WGPUColorSpace, Device&);
    ExternalTexture(Device&);

    Ref<Device> protectedDevice() const { return m_device; }

    RetainPtr<CVPixelBufferRef> m_pixelBuffer;
    WGPUColorSpace m_colorSpace;
    const Ref<Device> m_device;
    bool m_destroyed { false };
    id<MTLTexture> m_texture0 { nil };
    id<MTLTexture> m_texture1 { nil };
    mutable WeakHashSet<CommandEncoder> m_commandEncoders;
};

} // namespace WebGPU

