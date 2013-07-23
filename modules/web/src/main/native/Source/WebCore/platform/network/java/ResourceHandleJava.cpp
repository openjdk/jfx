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

bool ResourceHandle::start()
{
    ASSERT(!d->m_loader);
    d->m_loader = URLLoader::loadAsynchronously(context(), this);
    return d->m_loader;
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
                                               StoredCredentials,
                                               ResourceError& error, 
                                               ResourceResponse& response, 
                                               Vector<char>& data)
{
    URLLoader::loadSynchronously(context, request, error, response, data);
}


void ResourceHandle::platformSetDefersLoading(bool defers)
{
    notImplemented();
}


bool ResourceHandle::loadsBlocked()
{
    notImplemented();
    return false;
}

} // namespace WebCore
