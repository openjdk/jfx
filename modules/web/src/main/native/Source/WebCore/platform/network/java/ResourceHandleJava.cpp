/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

bool ResourceHandle::start(NetworkingContext* context)
{
    ASSERT(!d->m_loader);
    d->m_loader = URLLoader::loadAsynchronously(context, this);
    return d->m_loader;
}

void ResourceHandle::cancel()
{
    if (d->m_loader) {
        d->m_loader->cancel();
    }
}

void ResourceHandle::loadResourceSynchronously(NetworkingContext* context,
                                               const ResourceRequest& request, 
                                               StoredCredentials,
                                               ResourceError& error, 
                                               ResourceResponse& response, 
                                               Vector<char>& data)
{
    URLLoader::loadSynchronously(context, request, error, response, data);
}

/*
static HashSet<String>& allowsAnyHTTPSCertificateHosts()
{
    static HashSet<String> hosts;

    return hosts;
}

void ResourceHandle::setHostAllowsAnyHTTPSCertificate(const String& host)
{
    allowsAnyHTTPSCertificateHosts().add(host.lower());
}

// FIXME:  The CFDataRef will need to be something else when
// building without 
static HashMap<String, RetainPtr<CFDataRef> >& clientCerts()
{
    static HashMap<String, RetainPtr<CFDataRef> > certs;
    return certs;
}

void ResourceHandle::setClientCertificate(const String& host, CFDataRef cert)
{
    clientCerts().set(host.lower(), cert);
}

//stubs needed for windows version
void ResourceHandle::didReceiveAuthenticationChallenge(const AuthenticationChallenge&) 
{
    notImplemented();
}

void ResourceHandle::receivedCredential(const AuthenticationChallenge&, const Credential&) 
{
    notImplemented();
}

void ResourceHandle::receivedCancellation(const AuthenticationChallenge&)
{
    notImplemented();
}

void ResourceHandle::receivedRequestToContinueWithoutCredential(const AuthenticationChallenge&) 
{
    notImplemented();
}


*/

void ResourceHandle::platformSetDefersLoading(bool defers)
{
    notImplemented();
}

bool ResourceHandle::willLoadFromCache(ResourceRequest&, Frame*)
{
    notImplemented();
    return false;
}

bool ResourceHandle::loadsBlocked()
{
    notImplemented();
    return false;
}

} // namespace WebCore
