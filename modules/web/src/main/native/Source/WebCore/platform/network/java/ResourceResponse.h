/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ResourceResponse_h
#define ResourceResponse_h

#include "ResourceResponseBase.h"

namespace WebCore {

class ResourceResponse : public ResourceResponseBase {
public:
    ResourceResponse()
    {
    }

    ResourceResponse(const URL& url, const String& mimeType, long long expectedLength, const String& textEncodingName, const String& filename)
        : ResourceResponseBase(url, mimeType, expectedLength, textEncodingName, filename)
    {
    }

private:
    friend class ResourceResponseBase;

    void doUpdateResourceResponse()
    { }

    PassOwnPtr<CrossThreadResourceResponseData> doPlatformCopyData(PassOwnPtr<CrossThreadResourceResponseData> data) const { return data; }
    void doPlatformAdopt(PassOwnPtr<CrossThreadResourceResponseData>) { }
};

struct CrossThreadResourceResponseData : public CrossThreadResourceResponseDataBase {
};

} // namespace WebCore

#endif // ResourceResponse_h
