/*
 * Copyright (C) 2009 Apple Inc. All rights reserved.
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

#if ENABLE(WEBGL)

#include "WebGLSharedObject.h"
#include <wtf/Vector.h>

namespace WebCore {

class WebGLTexture final : public WebGLSharedObject {
public:

    enum TextureExtensionFlag {
        TextureExtensionsDisabled = 0,
        TextureExtensionFloatLinearEnabled = 1 << 0,
        TextureExtensionHalfFloatLinearEnabled = 2 << 0
    };

    virtual ~WebGLTexture();

    static Ref<WebGLTexture> create(WebGLRenderingContextBase&);

    void setTarget(GC3Denum target, GC3Dint maxLevel);
    void setParameteri(GC3Denum pname, GC3Dint param);
    void setParameterf(GC3Denum pname, GC3Dfloat param);

    GC3Denum getTarget() const { return m_target; }

    int getMinFilter() const { return m_minFilter; }

    void setLevelInfo(GC3Denum target, GC3Dint level, GC3Denum internalFormat, GC3Dsizei width, GC3Dsizei height, GC3Denum type);

    bool canGenerateMipmaps();
    // Generate all level information.
    void generateMipmapLevelInfo();

    GC3Denum getInternalFormat(GC3Denum target, GC3Dint level) const;
    GC3Denum getType(GC3Denum target, GC3Dint level) const;
    GC3Dsizei getWidth(GC3Denum target, GC3Dint level) const;
    GC3Dsizei getHeight(GC3Denum target, GC3Dint level) const;
    bool isValid(GC3Denum target, GC3Dint level) const;
    void markInvalid(GC3Denum target, GC3Dint level);

    // Whether width/height is NotPowerOfTwo.
    static bool isNPOT(GC3Dsizei, GC3Dsizei);

    bool isNPOT() const;
    // Determine if texture sampling should always return [0, 0, 0, 1] (OpenGL ES 2.0 Sec 3.8.2).
    bool needToUseBlackTexture(TextureExtensionFlag) const;

    bool isCompressed() const;
    void setCompressed();

    bool hasEverBeenBound() const { return object() && m_target; }

    static GC3Dint computeLevelCount(GC3Dsizei width, GC3Dsizei height);

    bool immutable() const { return m_immutable; }
    void setImmutable() { m_immutable = true; }

private:
    WebGLTexture(WebGLRenderingContextBase&);

    void deleteObjectImpl(GraphicsContext3D*, Platform3DObject) override;

    class LevelInfo {
    public:
        LevelInfo()
            : valid(false)
            , internalFormat(0)
            , width(0)
            , height(0)
            , type(0)
        {
        }

        void setInfo(GC3Denum internalFmt, GC3Dsizei w, GC3Dsizei h, GC3Denum tp)
        {
            valid = true;
            internalFormat = internalFmt;
            width = w;
            height = h;
            type = tp;
        }

        bool valid;
        GC3Denum internalFormat;
        GC3Dsizei width;
        GC3Dsizei height;
        GC3Denum type;
    };

    bool isTexture() const override { return true; }

    void update();

    int mapTargetToIndex(GC3Denum) const;

    const LevelInfo* getLevelInfo(GC3Denum target, GC3Dint level) const;

    GC3Denum m_target;

    GC3Denum m_minFilter;
    GC3Denum m_magFilter;
    GC3Denum m_wrapS;
    GC3Denum m_wrapT;

    Vector<Vector<LevelInfo>> m_info;

    bool m_isNPOT;
    bool m_isComplete;
    bool m_needToUseBlackTexture;
    bool m_isCompressed;
    bool m_isFloatType;
    bool m_isHalfFloatType;
    bool m_isForWebGL1;
    bool m_immutable { false };
};

} // namespace WebCore

#endif
