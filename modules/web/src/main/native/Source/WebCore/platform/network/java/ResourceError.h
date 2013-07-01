/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ResourceError_h
#define ResourceError_h

#include "ResourceErrorBase.h"

namespace WebCore {

class ResourceError : public ResourceErrorBase {
public:
    ResourceError() {}

    ResourceError(const String& domain, int errorCode, 
                  const String& failingURL, const String& localizedDescription)
        : ResourceErrorBase(domain, errorCode, failingURL, localizedDescription)
    {
    }
};

}

#endif // ResourceError_h_
