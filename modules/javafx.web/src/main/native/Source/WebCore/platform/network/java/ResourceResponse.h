/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

    bool isMovedPermanently() const
    {
        return httpStatusCode() == 301;
    }

    bool isFound() const
    {
        return httpStatusCode() == 302;
    }

    bool isSeeOther() const
    {
        return httpStatusCode() == 303;
    }

    bool isNotModified() const
    {
        return httpStatusCode() == 304;
    }

    bool isUnauthorized() const
    {
        return httpStatusCode() == 401;
    }

private:
    friend class ResourceResponseBase;

    String platformSuggestedFilename() const
    {
        return filenameFromHTTPContentDisposition(httpHeaderField(HTTPHeaderName::ContentDisposition));
    }
};

} // namespace WebCore
