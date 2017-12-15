/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "ResourceRequestBase.h"

namespace WebCore {

    class ResourceRequest : public ResourceRequestBase {
    public:
        ResourceRequest(const String& url)
            : ResourceRequestBase(URL(ParsedURLString, url), UseProtocolCachePolicy)
        {
        }

        ResourceRequest(const URL& url)
            : ResourceRequestBase(url, UseProtocolCachePolicy)
        {
        }

        ResourceRequest(const URL& url, const String& referrer, ResourceRequestCachePolicy policy = UseProtocolCachePolicy)
            : ResourceRequestBase(url, policy)
        {
            setHTTPReferrer(referrer);
        }

        ResourceRequest()
            : ResourceRequestBase(URL(), UseProtocolCachePolicy)
        {
        }

    private:
        friend class ResourceRequestBase;

        void doUpdatePlatformRequest() {}
        void doUpdateResourceRequest() {}
        void doUpdatePlatformHTTPBody() { }
        void doUpdateResourceHTTPBody() { }

        void doPlatformSetAsIsolatedCopy(const ResourceRequest&) { }
    };

} // namespace WebCore
