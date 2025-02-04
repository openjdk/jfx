/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "PlatformStrategiesJava.h"

#include <WebCore/NotImplemented.h>
#include "WebKitLegacy/WebCoreSupport/WebResourceLoadScheduler.h"
#include <wtf/NeverDestroyed.h>
#include <WebCore/AudioDestination.h>
#include <WebCore/BlobRegistry.h>
#include <WebCore/BlobRegistryImpl.h>
#include <WebCore/MediaStrategy.h>

namespace WebCore {
void PlatformStrategiesJava::initialize()
{
    static NeverDestroyed<PlatformStrategiesJava> platformStrategies;
}

PlatformStrategiesJava::PlatformStrategiesJava()
{
    setPlatformStrategies(this);
}

LoaderStrategy* PlatformStrategiesJava::createLoaderStrategy()
{
    return new WebResourceLoadScheduler;
}

PasteboardStrategy* PlatformStrategiesJava::createPasteboardStrategy()
{
    // This is currently used only by Mac code.
    notImplemented();
    return 0;
}

class WebMediaStrategy final : public MediaStrategy {
private:
#if ENABLE(WEB_AUDIO)
    std::unique_ptr<AudioDestination> createAudioDestination(AudioIOCallback& callback, const String& inputDeviceId,
        unsigned numberOfInputChannels, unsigned numberOfOutputChannels, float sampleRate) override
    {
        return AudioDestination::create(callback, inputDeviceId, numberOfInputChannels, numberOfOutputChannels, sampleRate);
    }
#endif
};

MediaStrategy* PlatformStrategiesJava::createMediaStrategy()
{
    return new WebMediaStrategy;
}

class WebBlobRegistry final : public BlobRegistry {
private:
    void registerInternalFileBlobURL(const URL& url, Ref<BlobDataFileReference>&& reference, const String& path, const String& contentType) override { m_blobRegistry.registerInternalFileBlobURL(url, WTFMove(reference), contentType); }
    void registerInternalBlobURL(const URL& url, Vector<BlobPart>&& parts, const String& contentType) override { m_blobRegistry.registerInternalBlobURL(url, WTFMove(parts), contentType); }
    void registerBlobURL(const URL& url, const URL& srcURL, const PolicyContainer& container, const std::optional<SecurityOriginData>& topOrigin) override { m_blobRegistry.registerBlobURL(url, srcURL, container, topOrigin); }
    void registerInternalBlobURLOptionallyFileBacked(const URL& url, const URL& srcURL, RefPtr<BlobDataFileReference>&& reference, const String& contentType) override { m_blobRegistry.registerBlobURLOptionallyFileBacked(url, srcURL, WTFMove(reference), contentType, { }); }
    void registerInternalBlobURLForSlice(const URL& url, const URL& srcURL, long long start, long long end, const String& contentType) override { m_blobRegistry.registerInternalBlobURLForSlice(url, srcURL, start, end, contentType); }
    void unregisterBlobURL(const URL& url, const std::optional<SecurityOriginData>& topOrigin) override { m_blobRegistry.unregisterBlobURL(url, topOrigin); }
    void registerBlobURLHandle(const URL& url, const std::optional<SecurityOriginData>& topOrigin) override { m_blobRegistry.registerBlobURLHandle(url, topOrigin); };
    void unregisterBlobURLHandle(const URL& url, const std::optional<SecurityOriginData>& topOrigin) override { m_blobRegistry.unregisterBlobURLHandle(url, topOrigin); };
    unsigned long long blobSize(const URL& url) override { return m_blobRegistry.blobSize(url); }
    void writeBlobsToTemporaryFilesForIndexedDB(const Vector<String>& blobURLs, CompletionHandler<void(Vector<String>&& filePaths)>&& completionHandler) override { m_blobRegistry.writeBlobsToTemporaryFilesForIndexedDB(blobURLs, WTFMove(completionHandler)); }

    BlobRegistryImpl* blobRegistryImpl() final { return &m_blobRegistry; }

    BlobRegistryImpl m_blobRegistry;
};

WebCore::BlobRegistry* PlatformStrategiesJava::createBlobRegistry()
{
    return new WebBlobRegistry;
}

} // namespace WebCore
