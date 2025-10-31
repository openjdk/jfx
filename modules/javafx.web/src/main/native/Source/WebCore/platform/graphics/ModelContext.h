/*
 * Copyright (C) 2024 Apple Inc. All rights reserved.
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

#include "Color.h"
#include "LayerHostingContextIdentifier.h"
#include "LayoutSize.h"
#include "PlatformLayerIdentifier.h"
#include <wtf/RefCounted.h>

namespace WebCore {

enum class ModelContextDisablePortal : bool { No, Yes };

class ModelContext final : public RefCounted<ModelContext> {
public:
    WEBCORE_EXPORT static Ref<ModelContext> create(const PlatformLayerIdentifier&, const LayerHostingContextIdentifier&, const LayoutSize&, ModelContextDisablePortal, std::optional<Color>);
    WEBCORE_EXPORT ~ModelContext();

    PlatformLayerIdentifier modelLayerIdentifier() const { return m_modelLayerIdentifier; }
    LayerHostingContextIdentifier modelContentsLayerHostingContextIdentifier() const { return m_modelContentsLayerHostingContextIdentifier; }
    LayoutSize modelLayoutSize() const { return m_modelLayoutSize; }
    ModelContextDisablePortal disablePortal() const { return m_disablePortal; }
    std::optional<Color> backgroundColor() const { return m_backgroundColor; }
    void setBackgroundColor(std::optional<Color> backgroundColor) { m_backgroundColor = backgroundColor; }

private:
    explicit ModelContext(const PlatformLayerIdentifier&, const LayerHostingContextIdentifier&, const LayoutSize&, ModelContextDisablePortal, std::optional<Color>);

    PlatformLayerIdentifier m_modelLayerIdentifier;
    LayerHostingContextIdentifier m_modelContentsLayerHostingContextIdentifier;
    LayoutSize m_modelLayoutSize;
    ModelContextDisablePortal m_disablePortal;
    std::optional<Color> m_backgroundColor;
};

} // namespace WebCore
