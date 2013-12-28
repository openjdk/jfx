/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "ClipboardUtilitiesJava.h"

#include <wtf/text/WTFString.h>

namespace WebCore {

#if OS(WINDOWS)
void replaceNewlinesWithWindowsStyleNewlines(String& str)
{
    static const UChar Newline = '\n';
    static const char* const WindowsNewline("\r\n");
    str.replace(Newline, WindowsNewline);
}
#endif

void replaceNBSPWithSpace(String& str)
{
    static const UChar NonBreakingSpaceCharacter = 0xA0;
    static const UChar SpaceCharacter = ' ';
    str.replace(NonBreakingSpaceCharacter, SpaceCharacter);
}

} // namespace WebCore
