/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "Logging.h"

#if !LOG_DISABLED
namespace WebCore {

void initializeLoggingChannelsIfNecessary()
{
    static bool haveInitializedLoggingChannels = false;
    if (haveInitializedLoggingChannels)
        return;

    haveInitializedLoggingChannels = true;

#if defined(NDEBUG)
    //Warning("This is a release build. Setting QT_WEBKIT_LOG will have no effect.");
#else
    // By default we log calls to notImplemented(). This can be turned
    // off by setting the environment variable DISABLE_NI_WARNING to 1
    LogNotYetImplemented.state = WTFLogChannelOn;
#endif
}

} // namespace WebCore
#endif