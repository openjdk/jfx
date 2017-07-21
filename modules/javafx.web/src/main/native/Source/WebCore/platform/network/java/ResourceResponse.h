/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "HTTPHeaderNames.h"
#include "HTTPParsers.h"

#include "ResourceResponseBase.h"

namespace WebCore {

class ResourceResponse : public ResourceResponseBase {
public:
    ResourceResponse()
    {
    }

    ResourceResponse(const URL& url, const String& mimeType, long long expectedLength, const String& textEncodingName)
        : ResourceResponseBase(url, mimeType, expectedLength, textEncodingName)
    {
    }

private:
    friend class ResourceResponseBase;

    String platformSuggestedFilename() const
    {
        return filenameFromHTTPContentDisposition(httpHeaderField(HTTPHeaderName::ContentDisposition));
    }
};

} // namespace WebCore
