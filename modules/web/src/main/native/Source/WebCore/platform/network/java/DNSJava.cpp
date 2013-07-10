/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 */

#ifndef DNSJava_H
#define DNSJava_H

#include "config.h"
#include "DNS.h"

#include "DNSResolveQueue.h"
#include "NotImplemented.h"
#include <wtf/text/WTFString.h>

namespace WebCore {

bool DNSResolveQueue::platformProxyIsEnabledInSystemPreferences()
{
    notImplemented();
    return false;
}

void DNSResolveQueue::platformResolve(const String&)
{
    notImplemented();
}

void prefetchDNS(const String& host)
{
    if (host.isEmpty())
        return;
    notImplemented();
}

}
#endif
