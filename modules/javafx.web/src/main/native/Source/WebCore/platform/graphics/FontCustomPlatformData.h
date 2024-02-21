/*
 * Copyright (C) 2023 Sony Interactive Entertainment Inc. All rights reserved.
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

#include "FontPlatformData.h"
#include <wtf/Forward.h>
#include <wtf/Noncopyable.h>

#if PLATFORM(WIN)
#include <wtf/text/WTFString.h>
#elif USE(CORE_TEXT)
#include <CoreFoundation/CFBase.h>
#include <wtf/RetainPtr.h>

typedef struct CGFont* CGFontRef;
typedef const struct __CTFontDescriptor* CTFontDescriptorRef;
#elif PLATFORM(JAVA)
#include "TextFlags.h"
#include "RenderStyleConstants.h"
#include <wtf/java/JavaRef.h> // todo tav remove when building w/ pch
#else
#include "RefPtrCairo.h"

typedef struct FT_FaceRec_*  FT_Face;
#endif

namespace WebCore {

class SharedBuffer;
class FontDescription;
class FontCreationContext;
enum class FontTechnology : uint8_t;

template <typename T> class FontTaggedSettings;
typedef FontTaggedSettings<int> FontFeatureSettings;

struct FontCustomPlatformData : public RefCounted<FontCustomPlatformData> {
    WTF_MAKE_FAST_ALLOCATED;
    WTF_MAKE_NONCOPYABLE(FontCustomPlatformData);
public:
#if PLATFORM(WIN)
    FontCustomPlatformData(const String& name, FontPlatformData::CreationData&&);
#elif USE(CORE_TEXT)
    FontCustomPlatformData(CTFontDescriptorRef fontDescriptor, FontPlatformData::CreationData&& creationData)
        : fontDescriptor(fontDescriptor)
        , creationData(WTFMove(creationData))
        , m_renderingResourceIdentifier(RenderingResourceIdentifier::generate())
    {
    }
#elif PLATFORM(JAVA)
    FontCustomPlatformData(const JLObject& data);
#else
    FontCustomPlatformData(FT_Face, FontPlatformData::CreationData&&);
#endif
    WEBCORE_EXPORT ~FontCustomPlatformData();

    FontPlatformData fontPlatformData(const FontDescription&, bool bold, bool italic, const FontCreationContext&);

    static bool supportsFormat(const String&);
    static bool supportsTechnology(const FontTechnology&);

#if PLATFORM(WIN)
    String name;
    FontPlatformData::CreationData creationData;
#elif USE(CORE_TEXT)
    RetainPtr<CTFontDescriptorRef> fontDescriptor;
    FontPlatformData::CreationData creationData;
#elif PLATFORM(JAVA)
        JGObject m_data;
#else
    RefPtr<cairo_font_face_t> m_fontFace;
#endif

};

WEBCORE_EXPORT RefPtr<FontCustomPlatformData> createFontCustomPlatformData(SharedBuffer&, const String&);

} // namespace WebCore
