/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"
#include "ResourceHandle.h"
#include "ResourceHandleInternal.h"

#include "NotImplemented.h"

namespace WebCore {

ResourceHandleInternal::~ResourceHandleInternal()
{
}

ResourceHandle::~ResourceHandle()
{
}

bool ResourceHandle::start()
{
    ASSERT(!d->m_loader);
    d->m_loader = URLLoader::loadAsynchronously(context(), this);
    return d->m_loader != nullptr;
}

void ResourceHandle::cancel()
{
    if (d->m_loader) {
        d->m_loader->cancel();
    }
}

//utatodo: merge artifact
void ResourceHandle::platformLoadResourceSynchronously(NetworkingContext* context,
                                               const ResourceRequest& request,
                                               StoredCredentialsPolicy,
                                               ResourceError& error,
                                               ResourceResponse& response,
                                               Vector<char>& data)
{
    URLLoader::loadSynchronously(context, request, error, response, data);
}


void ResourceHandle::platformSetDefersLoading(bool)
{
    notImplemented();
}

void ResourceHandle::receivedCredential(const AuthenticationChallenge&, const Credential&)
{
    // Implement like ResourceHandleCurl
    notImplemented();
}

void ResourceHandle::receivedRequestToContinueWithoutCredential(const AuthenticationChallenge&)
{
    // Implement like ResourceHandleCurl
    notImplemented();
}

void ResourceHandle::receivedCancellation(const AuthenticationChallenge& challenge)
{
    if (challenge != d->m_currentWebChallenge)
        return;

    if (client())
        client()->receivedCancellation(this, challenge);
}

void ResourceHandle::receivedRequestToPerformDefaultHandling(const AuthenticationChallenge&)
{
    ASSERT_NOT_REACHED();
}

void ResourceHandle::receivedChallengeRejection(const AuthenticationChallenge&)
{
    ASSERT_NOT_REACHED();
}

} // namespace WebCore
