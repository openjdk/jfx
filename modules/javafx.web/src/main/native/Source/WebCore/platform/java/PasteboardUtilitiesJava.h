/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include <wtf/Forward.h>

namespace WebCore {

#if OS(WINDOWS)
void replaceNewlinesWithWindowsStyleNewlines(String&);
#endif
void replaceNBSPWithSpace(String&);

} // namespace WebCore
