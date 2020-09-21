/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <WebCore/BlobRegistry.h>
#include <WebCore/BlobRegistryImpl.h>

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

class WebBlobRegistry final : public BlobRegistry {
private:
    void registerFileBlobURL(const URL& url, Ref<BlobDataFileReference>&& reference, const String& contentType) final { m_blobRegistry.registerFileBlobURL(url, WTFMove(reference), contentType); }
    void registerBlobURL(const URL& url, Vector<BlobPart>&& parts, const String& contentType) final { m_blobRegistry.registerBlobURL(url, WTFMove(parts), contentType); }
    void registerBlobURL(const URL& url, const URL& srcURL) final { m_blobRegistry.registerBlobURL(url, srcURL); }
    void registerBlobURLOptionallyFileBacked(const URL& url, const URL& srcURL, RefPtr<BlobDataFileReference>&& reference, const String& contentType) final { m_blobRegistry.registerBlobURLOptionallyFileBacked(url, srcURL, WTFMove(reference), contentType); }
    void registerBlobURLForSlice(const URL& url, const URL& srcURL, long long start, long long end) final { m_blobRegistry.registerBlobURLForSlice(url, srcURL, start, end); }
    void unregisterBlobURL(const URL& url) final { m_blobRegistry.unregisterBlobURL(url); }
    unsigned long long blobSize(const URL& url) final { return m_blobRegistry.blobSize(url); }
    void writeBlobsToTemporaryFiles(const Vector<String>& blobURLs, CompletionHandler<void(Vector<String>&& filePaths)>&& completionHandler) final { m_blobRegistry.writeBlobsToTemporaryFiles(blobURLs, WTFMove(completionHandler)); }

    BlobRegistryImpl* blobRegistryImpl() final { return &m_blobRegistry; }

    BlobRegistryImpl m_blobRegistry;
};

WebCore::BlobRegistry* PlatformStrategiesJava::createBlobRegistry()
{
    return new WebBlobRegistry;
}

} // namespace WebCore
