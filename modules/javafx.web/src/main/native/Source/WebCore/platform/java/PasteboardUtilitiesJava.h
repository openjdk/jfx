/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef PasteboardUtilitiesJava_h
#define PasteboardUtilitiesJava_h

#include <wtf/Forward.h>

namespace WebCore {

#if OS(WINDOWS)
void replaceNewlinesWithWindowsStyleNewlines(String&);
#endif
void replaceNBSPWithSpace(String&);

} // namespace WebCore

#endif // PasteboardUtilitiesJava_h
