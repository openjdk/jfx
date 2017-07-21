/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"

#include <wtf/text/WTFString.h>
#include "Logging.h"

#if !LOG_DISABLED
namespace WebCore {

String logLevelString()
{
#if defined(NDEBUG)
    return emptyString();
#else
    return String("all");
#endif
}
} // namespace WebCore
#endif
