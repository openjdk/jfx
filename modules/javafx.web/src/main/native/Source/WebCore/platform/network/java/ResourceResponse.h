/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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


    ResourceResponse(const URL& url, const String& mimeType, long long expectedLength, const String& textEncodingName) //XXX:, const String& filename)
        : ResourceResponseBase(url, mimeType, expectedLength, textEncodingName) //XXX: , filename)
    {
    }

private:
    friend class ResourceResponseBase;

    void doUpdateResourceResponse()
    { }

    std::unique_ptr<CrossThreadResourceResponseData> doPlatformCopyData(std::unique_ptr<CrossThreadResourceResponseData> data) const { return data; }
    void doPlatformAdopt(std::unique_ptr<CrossThreadResourceResponseData>) { }

    String platformSuggestedFilename() const { return String(); } //XXX: check implementation
};


struct CrossThreadResourceResponseData : public CrossThreadResourceResponseDataBase {
};

} // namespace WebCore

#endif // ResourceResponse_h
