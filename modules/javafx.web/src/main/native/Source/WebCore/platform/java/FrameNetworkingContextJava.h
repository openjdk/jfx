/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "FrameNetworkingContext.h"

#include "FrameLoader.h"
#include "FrameLoaderClient.h"
#include "NetworkStorageSession.h"
#include "Page.h"

namespace WebCore {

class FrameNetworkingContextJava final : public FrameNetworkingContext {
public:
    static Ref<FrameNetworkingContextJava> create(Frame* frame)
    {
        return adoptRef(*new FrameNetworkingContextJava(frame));
    }

    Page* page() const { return frame()->page(); }

    NetworkStorageSession& storageSession() const override
    {
        ASSERT(isMainThread());

        if (frame() && frame()->page()->usesEphemeralSession())
            return *NetworkStorageSession::storageSession(SessionID::legacyPrivateSessionID());

        return NetworkStorageSession::defaultStorageSession();
    }
private:
    FrameNetworkingContextJava(Frame* frame)
        : FrameNetworkingContext(frame)
    {
    }
};

}  // namespace WebCore
