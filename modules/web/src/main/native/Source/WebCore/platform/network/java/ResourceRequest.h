/*
 * Copyright (c) 2012-2013, Oracle and/or its affiliates. All rights reserved.
 */

#ifndef ResourceRequest_h
#define ResourceRequest_h

#include "ResourceRequestBase.h"

namespace WebCore {

    class ResourceRequest : public ResourceRequestBase {
    public:
        ResourceRequest(const String& url)
            : ResourceRequestBase(KURL(ParsedURLString, url), UseProtocolCachePolicy)
        {
        }

        ResourceRequest(const KURL& url) 
            : ResourceRequestBase(url, UseProtocolCachePolicy)
        {
        }

        ResourceRequest(const KURL& url, const String& referrer, ResourceRequestCachePolicy policy = UseProtocolCachePolicy) 
            : ResourceRequestBase(url, policy)
        {
            setHTTPReferrer(referrer);
        }
        
        ResourceRequest()
            : ResourceRequestBase(KURL(), UseProtocolCachePolicy)
        {
        }

        void doUpdatePlatformRequest() {}
        void doUpdateResourceRequest() {}
        void doUpdatePlatformHTTPBody() { }
        void doUpdateResourceHTTPBody() { }

    private:
        friend class ResourceRequestBase;

        PassOwnPtr<CrossThreadResourceRequestData> doPlatformCopyData(PassOwnPtr<CrossThreadResourceRequestData> data) const { return data; }
        void doPlatformAdopt(PassOwnPtr<CrossThreadResourceRequestData>) { }
    };

    struct CrossThreadResourceRequestData : public CrossThreadResourceRequestDataBase {
    };

} // namespace WebCore

#endif // ResourceRequest_h
