/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "ResourceErrorBase.h"

namespace WebCore {

class ResourceError : public ResourceErrorBase {
public:
    ResourceError(Type type = Type::Null)
        : ResourceErrorBase(type)
    {
    }

    ResourceError(const String& domain, int errorCode,
                  const URL& failingURL, const String& localizedDescription, Type type = Type::General)
        : ResourceErrorBase(domain, errorCode, failingURL, localizedDescription, type)
    {
    }

private:
    friend class ResourceErrorBase;
    void doPlatformIsolatedCopy(const ResourceError&) { }
};

} // namespace WebCore
