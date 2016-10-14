/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"

#include "SynchronousLoaderClient.h"

#include "AuthenticationChallenge.h"
#include "NotImplemented.h"
#include "ResourceHandle.h"

namespace WebCore {

void SynchronousLoaderClient::didReceiveAuthenticationChallenge(ResourceHandle*, const AuthenticationChallenge&)
{
    notImplemented();
}

ResourceError SynchronousLoaderClient::platformBadResponseError()
{
    notImplemented();
    return ResourceError();
}

}
